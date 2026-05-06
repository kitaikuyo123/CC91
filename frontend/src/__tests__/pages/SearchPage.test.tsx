import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import SearchPage from '../../pages/SearchPage';
import * as postApi from '../../api/post';

// Mock API
vi.mock('../../api/post');
vi.mock('../../hooks/useDebounce', () => ({
  useDebounce: (value: string) => value,
}));

// Mock react-router-dom
const mockNavigate = vi.fn();
const mockSetSearchParams = vi.fn();
vi.mock('react-router-dom', async () => ({
  ...(await vi.importActual('react-router-dom')),
  useNavigate: () => mockNavigate,
  useSearchParams: () => [
    new URLSearchParams('q=test'),
    mockSetSearchParams,
  ],
}));

describe('SearchPage', () => {
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

  const mockPosts = [
    {
      id: 1,
      title: 'Test Post 1',
      content: 'Content of test post 1',
      authorId: 1,
      authorUsername: 'user1',
      categoryId: 1,
      categoryName: 'Tech',
      createdAt: '2024-01-01T10:00:00',
      updatedAt: '2024-01-01T10:00:00',
      viewCount: 100,
    },
  ];

  describe('Initial state', () => {
    it('should render search form', async () => {
      // 预先 mock searchPosts 返回空结果，避免 undefined 错误
      vi.mocked(postApi.searchPosts).mockResolvedValue({
        content: [],
        totalElements: 0,
        totalPages: 0,
        pageable: { pageNumber: 0, pageSize: 10 },
        last: true,
        first: true,
        empty: true,
      } as any);

      render(<SearchPage />, { wrapper: createWrapper() });

      // 等待初始搜索完成（URL 参数中有 'test'）
      await waitFor(() => {
        expect(screen.getByPlaceholderText('输入关键词搜索标题或内容...')).toBeInTheDocument();
      });
      // 等待加载完成后再检查按钮文本
      await waitFor(() => {
        expect(screen.getByText('搜索')).toBeInTheDocument();
      });
    });

    it('should load keyword from URL params', async () => {
      vi.mocked(postApi.searchPosts).mockResolvedValue({
        content: mockPosts,
        totalElements: 1,
        totalPages: 1,
        pageable: { pageNumber: 0, pageSize: 10 },
        last: true,
        first: true,
        empty: false,
      } as any);

      render(<SearchPage />, { wrapper: createWrapper() });

      await waitFor(() => {
        expect(screen.getByDisplayValue('test')).toBeInTheDocument();
      });
    });
  });

  describe('Search functionality', () => {
    it('should perform search on form submit', async () => {
      const user = userEvent.setup();
      vi.mocked(postApi.searchPosts).mockResolvedValue({
        content: mockPosts,
        totalElements: 1,
        totalPages: 1,
        pageable: { pageNumber: 0, pageSize: 10 },
        last: true,
        first: true,
        empty: false,
      } as any);

      render(<SearchPage />, { wrapper: createWrapper() });

      const searchInput = screen.getByPlaceholderText('输入关键词搜索标题或内容...');
      await user.clear(searchInput);
      await user.type(searchInput, 'keyword');
      await user.click(screen.getByText('搜索'));

      await waitFor(() => {
        expect(mockSetSearchParams).toHaveBeenCalledWith({ q: 'keyword' });
        expect(postApi.searchPosts).toHaveBeenCalledWith('keyword', 0, 10);
      });
    });

    it('should display search results', async () => {
      vi.mocked(postApi.searchPosts).mockResolvedValue({
        content: mockPosts,
        totalElements: 1,
        totalPages: 1,
        pageable: { pageNumber: 0, pageSize: 10 },
        last: true,
        first: true,
        empty: false,
      } as any);

      render(<SearchPage />, { wrapper: createWrapper() });

      await waitFor(() => {
        expect(screen.getByText('Test Post 1')).toBeInTheDocument();
      });

      expect(screen.getByText('找到 1 条结果')).toBeInTheDocument();
    });

    it('should show empty state when no results', async () => {
      vi.mocked(postApi.searchPosts).mockResolvedValue({
        content: [],
        totalElements: 0,
        totalPages: 0,
        pageable: { pageNumber: 0, pageSize: 10 },
        last: true,
        first: true,
        empty: true,
      } as any);

      render(<SearchPage />, { wrapper: createWrapper() });

      await waitFor(() => {
        expect(screen.getByText('没有找到匹配的帖子')).toBeInTheDocument();
      });
    });

    it('should show loading state during search', () => {
      vi.mocked(postApi.searchPosts).mockImplementation(
        () => new Promise(() => {})
      );

      render(<SearchPage />, { wrapper: createWrapper() });

      expect(screen.getByText('搜索中...')).toBeInTheDocument();
    });
  });

  describe('Navigation', () => {
    it('should navigate to post detail when clicking post', async () => {
      const user = userEvent.setup();
      vi.mocked(postApi.searchPosts).mockResolvedValue({
        content: [mockPosts[0]],
        totalElements: 1,
        totalPages: 1,
        pageable: { pageNumber: 0, pageSize: 10 },
        last: true,
        first: true,
        empty: false,
      } as any);

      render(<SearchPage />, { wrapper: createWrapper() });

      await waitFor(() => {
        expect(screen.getByText('Test Post 1')).toBeInTheDocument();
      });

      const postCard = screen.getByText('Test Post 1').closest('.card');
      await user.click(postCard!);

      expect(mockNavigate).toHaveBeenCalledWith('/posts/1');
    });
  });

  describe('Pagination', () => {
    it('should show pagination controls for multiple pages', async () => {
      vi.mocked(postApi.searchPosts).mockResolvedValue({
        content: mockPosts,
        totalElements: 25,
        totalPages: 3,
        pageable: { pageNumber: 0, pageSize: 10 },
        last: false,
        first: true,
        empty: false,
      } as any);

      render(<SearchPage />, { wrapper: createWrapper() });

      await waitFor(() => {
        expect(screen.getByText('上一页')).toBeInTheDocument();
        expect(screen.getByText('下一页')).toBeInTheDocument();
        expect(screen.getByText('第 1 / 3 页')).toBeInTheDocument();
      });
    });

    it('should not show pagination for single page', async () => {
      vi.mocked(postApi.searchPosts).mockResolvedValue({
        content: mockPosts,
        totalElements: 1,
        totalPages: 1,
        pageable: { pageNumber: 0, pageSize: 10 },
        last: true,
        first: true,
        empty: false,
      } as any);

      render(<SearchPage />, { wrapper: createWrapper() });

      await waitFor(() => {
        expect(screen.queryByText('上一页')).not.toBeInTheDocument();
        expect(screen.queryByText('下一页')).not.toBeInTheDocument();
      });
    });
  });
});
