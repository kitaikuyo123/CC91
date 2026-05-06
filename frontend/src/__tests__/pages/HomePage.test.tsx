import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import HomePage from '../../pages/HomePage';
import * as categoryApi from '../../api/category';
import * as postApi from '../../api/post';

// Mock APIs
vi.mock('../../api/category');
vi.mock('../../api/post');

// Mock react-router-dom
const mockNavigate = vi.fn();
vi.mock('react-router-dom', async () => ({
  ...(await vi.importActual('react-router-dom')),
  useNavigate: () => mockNavigate,
}));

// Mock window.open
const mockWindowOpen = vi.fn();
vi.stubGlobal('open', mockWindowOpen);

describe('HomePage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  const createWrapper = () => {
    const queryClient = new QueryClient({
      defaultOptions: {
        queries: { retry: false, gcTime: 0 },
        mutations: { retry: false },
      },
    });

    return ({ children }: { children: React.ReactNode }) => (
      <QueryClientProvider client={queryClient}>
        <MemoryRouter>{children}</MemoryRouter>
      </QueryClientProvider>
    );
  };

  const mockCategories = [
    { id: 1, name: 'Tech', description: 'Technology discussions', sortOrder: 1, createdAt: '2024-01-01T10:00:00' },
    { id: 2, name: 'Life', description: 'Life chat', sortOrder: 2, createdAt: '2024-01-01T10:00:00' },
  ];

  const mockPosts = [
    {
      id: 1,
      title: 'Post 1',
      content: 'Content 1',
      authorId: 1,
      authorUsername: 'user1',
      categoryId: 1,
      categoryName: 'Tech',
      createdAt: '2024-01-01T10:00:00',
      updatedAt: '2024-01-01T10:00:00',
      viewCount: 100,
    },
    {
      id: 2,
      title: 'Post 2',
      content: 'Content 2',
      authorId: 2,
      authorUsername: 'user2',
      categoryId: 2,
      categoryName: 'Life',
      createdAt: '2024-01-02T10:00:00',
      updatedAt: '2024-01-02T10:00:00',
      viewCount: 200,
    },
  ];

  describe('Page header', () => {
    it('should render forum title and description', () => {
      vi.mocked(categoryApi.getCategories).mockResolvedValue(mockCategories);
      vi.mocked(postApi.getPostList).mockResolvedValue({
        content: mockPosts,
        totalElements: 2,
        totalPages: 1,
      } as any);

      render(<HomePage />, { wrapper: createWrapper() });

      expect(screen.getByText('CC91 论坛')).toBeInTheDocument();
      expect(screen.getByText('一个现代化的技术交流社区')).toBeInTheDocument();
    });
  });

  describe('Categories section', () => {
    it('should display categories list', async () => {
      vi.mocked(categoryApi.getCategories).mockResolvedValue(mockCategories);
      vi.mocked(postApi.getPostList).mockResolvedValue({
        content: mockPosts,
        totalElements: 2,
        totalPages: 1,
      } as any);

      render(<HomePage />, { wrapper: createWrapper() });

      await waitFor(() => {
        expect(screen.getByText('讨论版块')).toBeInTheDocument();
        expect(screen.getByText('Tech')).toBeInTheDocument();
        expect(screen.getByText('Technology discussions')).toBeInTheDocument();
        expect(screen.getByText('Life')).toBeInTheDocument();
        expect(screen.getByText('Life chat')).toBeInTheDocument();
      });
    });

    it('should navigate to category page when clicking category', async () => {
      const user = userEvent.setup();
      vi.mocked(categoryApi.getCategories).mockResolvedValue(mockCategories);
      vi.mocked(postApi.getPostList).mockResolvedValue({
        content: mockPosts,
        totalElements: 2,
        totalPages: 1,
      } as any);

      render(<HomePage />, { wrapper: createWrapper() });

      await waitFor(() => {
        expect(screen.getByText('Tech')).toBeInTheDocument();
      });

      const categoryCard = screen.getByText('Tech').closest('.card');
      await user.click(categoryCard!);

      expect(mockNavigate).toHaveBeenCalledWith('/category/1');
    });
  });

  describe('Recent posts section', () => {
    it('should display recent posts', async () => {
      vi.mocked(categoryApi.getCategories).mockResolvedValue(mockCategories);
      vi.mocked(postApi.getPostList).mockImplementation((_page: any, size: any) => {
        if (size >= 50) return Promise.resolve({ content: [], totalElements: 0, totalPages: 0 } as any);
        return Promise.resolve({ content: mockPosts, totalElements: 2, totalPages: 1 } as any);
      });

      render(<HomePage />, { wrapper: createWrapper() });

      await waitFor(() => {
        expect(screen.getByText('最新帖子')).toBeInTheDocument();
        expect(screen.getByText('Post 1')).toBeInTheDocument();
        expect(screen.getByText('Post 2')).toBeInTheDocument();
      });
    });

    it('should show empty state when no posts', async () => {
      vi.mocked(categoryApi.getCategories).mockResolvedValue(mockCategories);
      vi.mocked(postApi.getPostList).mockResolvedValue({
        content: [],
        totalElements: 0,
        totalPages: 0,
      } as any);

      render(<HomePage />, { wrapper: createWrapper() });

      await waitFor(() => {
        expect(screen.getByText('暂无帖子')).toBeInTheDocument();
      });
    });

    it('should navigate to post detail when clicking post', async () => {
      const user = userEvent.setup();
      vi.mocked(categoryApi.getCategories).mockResolvedValue(mockCategories);
      vi.mocked(postApi.getPostList).mockImplementation((_page: any, size: any) => {
        if (size >= 50) return Promise.resolve({ content: [], totalElements: 0, totalPages: 0 } as any);
        return Promise.resolve({ content: [mockPosts[0]], totalElements: 1, totalPages: 1 } as any);
      });

      render(<HomePage />, { wrapper: createWrapper() });

      await waitFor(() => {
        expect(screen.getByText('Post 1')).toBeInTheDocument();
      });

      const postCard = screen.getByText('Post 1').closest('.card');
      await user.click(postCard!);

      expect(mockWindowOpen).toHaveBeenCalledWith('/posts/1', '_blank');
    });
  });

  describe('Hot posts section', () => {
    it('should display hot posts ordered by view count', async () => {
      vi.mocked(categoryApi.getCategories).mockResolvedValue(mockCategories);
      vi.mocked(postApi.getPostList).mockImplementation((_page: any, size: any) => {
        if (size >= 50) return Promise.resolve({ content: mockPosts, totalElements: 2, totalPages: 1 } as any);
        return Promise.resolve({ content: [], totalElements: 0, totalPages: 0 } as any);
      });

      render(<HomePage />, { wrapper: createWrapper() });

      await waitFor(() => {
        expect(screen.getByText('🔥 热门帖子')).toBeInTheDocument();
        expect(screen.getByText('Post 2')).toBeInTheDocument();
        expect(screen.getByText('Post 1')).toBeInTheDocument();
      });
    });

    it('should show numbered rankings', async () => {
      vi.mocked(categoryApi.getCategories).mockResolvedValue(mockCategories);
      vi.mocked(postApi.getPostList).mockImplementation((_page: any, size: any) => {
        if (size >= 50) return Promise.resolve({ content: mockPosts, totalElements: 2, totalPages: 1 } as any);
        return Promise.resolve({ content: [], totalElements: 0, totalPages: 0 } as any);
      });

      render(<HomePage />, { wrapper: createWrapper() });

      await waitFor(() => {
        expect(screen.getByText('🔥 热门帖子')).toBeInTheDocument();
      });

      // Should show ranking numbers
      const rankings = screen.getAllByText(/\d+/);
      expect(rankings.length).toBeGreaterThan(0);
    });

    it('should navigate to post when clicking hot post', async () => {
      const user = userEvent.setup();
      vi.mocked(categoryApi.getCategories).mockResolvedValue(mockCategories);
      vi.mocked(postApi.getPostList).mockImplementation((_page: any, size: any) => {
        if (size >= 50) return Promise.resolve({ content: mockPosts, totalElements: 2, totalPages: 1 } as any);
        return Promise.resolve({ content: [], totalElements: 0, totalPages: 0 } as any);
      });

      render(<HomePage />, { wrapper: createWrapper() });

      await waitFor(() => {
        expect(screen.getByText('🔥 热门帖子')).toBeInTheDocument();
        expect(screen.getByText('Post 2')).toBeInTheDocument();
      });

      await user.click(screen.getByText('Post 2'));

      expect(mockWindowOpen).toHaveBeenCalledWith('/posts/2', '_blank');
    });
  });
});
