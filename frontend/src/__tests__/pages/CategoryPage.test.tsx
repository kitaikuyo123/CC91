import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter, Routes, Route } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import CategoryPage from '../../pages/CategoryPage';
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
  useParams: () => ({ id: '1' }),
}));

describe('CategoryPage', () => {
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
        <MemoryRouter initialEntries={['/category/1']}>
          <Routes>
            <Route path="/category/:id" element={children} />
          </Routes>
        </MemoryRouter>
      </QueryClientProvider>
    );
  };

  const mockCategory = {
    id: 1,
    name: 'Tech Discussion',
    description: 'Talk about technology',
    sortOrder: 1,
    createdAt: '2024-01-01T10:00:00',
  };

  const mockPosts = [
    {
      id: 1,
      title: 'Post 1',
      content: 'Content of post 1',
      authorId: 1,
      authorUsername: 'user1',
      categoryId: 1,
      categoryName: 'Tech Discussion',
      createdAt: '2024-01-01T10:00:00',
      updatedAt: '2024-01-01T10:00:00',
      viewCount: 100,
    },
    {
      id: 2,
      title: 'Post 2',
      content: 'Content of post 2',
      authorId: 2,
      authorUsername: 'user2',
      categoryId: 1,
      categoryName: 'Tech Discussion',
      createdAt: '2024-01-02T10:00:00',
      updatedAt: '2024-01-02T10:00:00',
      viewCount: 50,
    },
  ];

  describe('Loading state', () => {
    it('should show loading state', () => {
      vi.mocked(categoryApi.getCategoryById).mockImplementation(
        () => new Promise(() => {})
      );
      vi.mocked(postApi.getPostsByCategory).mockImplementation(
        () => new Promise(() => {})
      );

      render(<CategoryPage />, { wrapper: createWrapper() });

      expect(screen.getByText('加载中...')).toBeInTheDocument();
    });
  });

  describe('Success - with posts', () => {
    it('should display category name and description', async () => {
      vi.mocked(categoryApi.getCategoryById).mockResolvedValue(mockCategory);
      vi.mocked(postApi.getPostsByCategory).mockResolvedValue({
        content: mockPosts,
        totalPages: 1,
        totalElements: 2,
      } as any);

      render(<CategoryPage />, { wrapper: createWrapper() });

      await waitFor(() => {
        expect(screen.getByText('Tech Discussion')).toBeInTheDocument();
        expect(screen.getByText('Talk about technology')).toBeInTheDocument();
      });
    });

    it('should display posts list', async () => {
      vi.mocked(categoryApi.getCategoryById).mockResolvedValue(mockCategory);
      vi.mocked(postApi.getPostsByCategory).mockResolvedValue({
        content: mockPosts,
        totalPages: 1,
        totalElements: 2,
      } as any);

      render(<CategoryPage />, { wrapper: createWrapper() });

      await waitFor(() => {
        expect(screen.getByText('Post 1')).toBeInTheDocument();
        expect(screen.getByText('Post 2')).toBeInTheDocument();
      });
    });

    it('should navigate to post detail when clicking post', async () => {
      const user = userEvent.setup();
      vi.mocked(categoryApi.getCategoryById).mockResolvedValue(mockCategory);
      vi.mocked(postApi.getPostsByCategory).mockResolvedValue({
        content: [mockPosts[0]],
        totalPages: 1,
        totalElements: 1,
      } as any);

      render(<CategoryPage />, { wrapper: createWrapper() });

      await waitFor(() => {
        expect(screen.getByText('Post 1')).toBeInTheDocument();
      });

      const postCard = screen.getByText('Post 1').closest('.card');
      await user.click(postCard!);

      expect(mockNavigate).toHaveBeenCalledWith('/posts/1');
    });
  });

  describe('Success - empty posts', () => {
    it('should show empty state', async () => {
      vi.mocked(categoryApi.getCategoryById).mockResolvedValue(mockCategory);
      vi.mocked(postApi.getPostsByCategory).mockResolvedValue({
        content: [],
        totalPages: 0,
        totalElements: 0,
      } as any);

      render(<CategoryPage />, { wrapper: createWrapper() });

      await waitFor(() => {
        expect(screen.getByText('暂无帖子')).toBeInTheDocument();
      });
    });
  });

  describe('Error state', () => {
    it('should show error message', async () => {
      vi.mocked(categoryApi.getCategoryById).mockRejectedValue({
        response: { data: { message: 'Category not found' } },
      });
      vi.mocked(postApi.getPostsByCategory).mockResolvedValue({
        content: [],
        totalPages: 0,
        totalElements: 0,
      } as any);

      render(<CategoryPage />, { wrapper: createWrapper() });

      await waitFor(() => {
        expect(screen.getByText('Category not found')).toBeInTheDocument();
      });
    });

    it('should show default error message', async () => {
      vi.mocked(categoryApi.getCategoryById).mockRejectedValue({});
      vi.mocked(postApi.getPostsByCategory).mockResolvedValue({
        content: [],
        totalPages: 0,
        totalElements: 0,
      } as any);

      render(<CategoryPage />, { wrapper: createWrapper() });

      await waitFor(() => {
        expect(screen.getByText('加载失败')).toBeInTheDocument();
      });
    });
  });

  describe('Pagination', () => {
    it('should show pagination controls', async () => {
      vi.mocked(categoryApi.getCategoryById).mockResolvedValue(mockCategory);
      vi.mocked(postApi.getPostsByCategory).mockResolvedValue({
        content: mockPosts,
        totalPages: 3,
        totalElements: 25,
      } as any);

      render(<CategoryPage />, { wrapper: createWrapper() });

      await waitFor(() => {
        expect(screen.getByText('上一页')).toBeInTheDocument();
        expect(screen.getByText('下一页')).toBeInTheDocument();
        expect(screen.getByText('第 1 / 3 页')).toBeInTheDocument();
      });
    });

    it('should disable prev button on first page', async () => {
      vi.mocked(categoryApi.getCategoryById).mockResolvedValue(mockCategory);
      vi.mocked(postApi.getPostsByCategory).mockResolvedValue({
        content: mockPosts,
        totalPages: 3,
        totalElements: 25,
      } as any);

      render(<CategoryPage />, { wrapper: createWrapper() });

      await waitFor(() => {
        const prevButton = screen.getByText('上一页');
        expect(prevButton).toBeDisabled();
      });
    });

    it('should disable next button on last page', async () => {
      vi.mocked(categoryApi.getCategoryById).mockResolvedValue(mockCategory);
      vi.mocked(postApi.getPostsByCategory).mockResolvedValue({
        content: mockPosts,
        totalPages: 1,
        totalElements: 2,
      } as any);

      render(<CategoryPage />, { wrapper: createWrapper() });

      await waitFor(() => {
        expect(screen.getByText('Post 1')).toBeInTheDocument();
      });
      // Single page should not have pagination buttons
      expect(screen.queryByText('下一页')).not.toBeInTheDocument();
    });

    it('should change page when clicking next', async () => {
      const user = userEvent.setup();
      vi.mocked(categoryApi.getCategoryById).mockResolvedValue(mockCategory);
      vi.mocked(postApi.getPostsByCategory)
        .mockResolvedValueOnce({
          content: mockPosts,
          totalPages: 3,
          totalElements: 25,
        } as any)
        .mockResolvedValueOnce({
          content: [mockPosts[0]],
          totalPages: 3,
          totalElements: 25,
        } as any);

      render(<CategoryPage />, { wrapper: createWrapper() });

      await waitFor(() => {
        expect(screen.getByText('下一页')).toBeInTheDocument();
      });

      const nextButton = screen.getByText('下一页');
      await user.click(nextButton);

      await waitFor(() => {
        expect(postApi.getPostsByCategory).toHaveBeenCalledWith(1, 1, 10);
      });
    });

    it('should not show pagination for single page', async () => {
      vi.mocked(categoryApi.getCategoryById).mockResolvedValue(mockCategory);
      vi.mocked(postApi.getPostsByCategory).mockResolvedValue({
        content: mockPosts,
        totalPages: 1,
        totalElements: 2,
      } as any);

      render(<CategoryPage />, { wrapper: createWrapper() });

      await waitFor(() => {
        expect(screen.queryByText('上一页')).not.toBeInTheDocument();
        expect(screen.queryByText('下一页')).not.toBeInTheDocument();
      });
    });
  });
});
