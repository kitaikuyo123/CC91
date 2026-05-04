import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import PostListPage from '../pages/PostListPage';
import * as postApi from '../api/post';
import { AuthProvider } from '../context/AuthContext';

// Mock the API
vi.mock('../api/post');

// Mock react-router-dom
const mockNavigate = vi.fn();
vi.mock('react-router-dom', async () => ({
  ...(await vi.importActual('react-router-dom')),
  useNavigate: () => mockNavigate,
}));

describe('PostListPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  const wrapper = ({ children }: { children: React.ReactNode }) => (
    <MemoryRouter>
      <AuthProvider>{children}</AuthProvider>
    </MemoryRouter>
  );

  const mockPosts = [
    {
      id: 1,
      title: 'Test Post 1',
      content: 'This is the content of test post 1',
      authorId: 1,
      authorUsername: 'user1',
      createdAt: '2024-01-01T10:00:00',
      updatedAt: '2024-01-01T10:00:00',
      viewCount: 100,
    },
    {
      id: 2,
      title: 'Test Post 2',
      content: 'This is the content of test post 2',
      authorId: 2,
      authorUsername: 'user2',
      createdAt: '2024-01-02T10:00:00',
      updatedAt: '2024-01-02T10:00:00',
      viewCount: 50,
    },
  ];

  describe('加载状态', () => {
    it('应该显示加载中状态', () => {
      vi.mocked(postApi.getPostList).mockImplementation(
        () => new Promise(() => {})
      );

      render(<PostListPage />, { wrapper });

      expect(screen.getByText('加载中...')).toBeInTheDocument();
    });
  });

  describe('加载成功 - 有帖子', () => {
    it('应该显示帖子列表', async () => {
      vi.mocked(postApi.getPostList).mockResolvedValue({
        content: mockPosts,
        pageable: { pageNumber: 0, pageSize: 10 },
        totalElements: 2,
        totalPages: 1,
        last: true,
        first: true,
        empty: false,
      });

      render(<PostListPage />, { wrapper });

      await waitFor(() => {
        expect(screen.getByText('Test Post 1')).toBeInTheDocument();
        expect(screen.getByText('Test Post 2')).toBeInTheDocument();
      });

      expect(screen.getByText('共 2 篇帖子')).toBeInTheDocument();
    });

    it('应该显示帖子元信息', async () => {
      vi.mocked(postApi.getPostList).mockResolvedValue({
        content: mockPosts,
        pageable: { pageNumber: 0, pageSize: 10 },
        totalElements: 1,
        totalPages: 1,
        last: true,
        first: true,
        empty: false,
      });

      render(<PostListPage />, { wrapper });

      await waitFor(() => {
        expect(screen.getAllByText('作者:').length).toBeGreaterThan(0);
        expect(screen.getByText('user1')).toBeInTheDocument();
        expect(screen.getByText('浏览: 100')).toBeInTheDocument();
      });
    });

    it('点击帖子卡片应该导航到详情页', async () => {
      const user = userEvent.setup();
      vi.mocked(postApi.getPostList).mockResolvedValue({
        content: [mockPosts[0]],
        pageable: { pageNumber: 0, pageSize: 10 },
        totalElements: 1,
        totalPages: 1,
        last: true,
        first: true,
        empty: false,
      });

      render(<PostListPage />, { wrapper });

      await waitFor(() => {
        expect(screen.getByText('Test Post 1')).toBeInTheDocument();
      });

      const postCard = screen.getByText('Test Post 1').closest('.card');
      await user.click(postCard!);

      expect(mockNavigate).toHaveBeenCalledWith('/posts/1');
    });

    it('点击发布新帖按钮应该导航到创建页面', async () => {
      const user = userEvent.setup();
      vi.mocked(postApi.getPostList).mockResolvedValue({
        content: mockPosts,
        pageable: { pageNumber: 0, pageSize: 10 },
        totalElements: 2,
        totalPages: 1,
        last: true,
        first: true,
        empty: false,
      });

      render(<PostListPage />, { wrapper });

      await waitFor(() => {
        expect(screen.getByText('发布新帖')).toBeInTheDocument();
      });

      const newPostButtons = screen.getAllByText('发布新帖');
      await user.click(newPostButtons[0]);

      expect(mockNavigate).toHaveBeenCalledWith('/posts/new');
    });
  });

  describe('加载成功 - 空列表', () => {
    it('应该显示空状态提示', async () => {
      vi.mocked(postApi.getPostList).mockResolvedValue({
        content: [],
        pageable: { pageNumber: 0, pageSize: 10 },
        totalElements: 0,
        totalPages: 1,
        last: true,
        first: true,
        empty: true,
      });

      render(<PostListPage />, { wrapper });

      await waitFor(() => {
        expect(screen.getByText('还没有帖子')).toBeInTheDocument();
      });

      expect(screen.getByText('发布第一篇帖子')).toBeInTheDocument();
    });

    it('空状态下点击发布按钮应该导航到创建页面', async () => {
      const user = userEvent.setup();
      vi.mocked(postApi.getPostList).mockResolvedValue({
        content: [],
        pageable: { pageNumber: 0, pageSize: 10 },
        totalElements: 0,
        totalPages: 1,
        last: true,
        first: true,
        empty: true,
      });

      render(<PostListPage />, { wrapper });

      await waitFor(() => {
        expect(screen.getByText('发布第一篇帖子')).toBeInTheDocument();
      });

      const publishButton = screen.getByText('发布第一篇帖子');
      await user.click(publishButton);

      expect(mockNavigate).toHaveBeenCalledWith('/posts/new');
    });
  });

  describe('加载失败', () => {
    it('应该显示错误信息', async () => {
      vi.mocked(postApi.getPostList).mockRejectedValue({
        response: { data: { message: '网络错误' } },
      });

      render(<PostListPage />, { wrapper });

      await waitFor(() => {
        expect(screen.getByText('网络错误')).toBeInTheDocument();
      });
    });

    it('没有错误消息时应该显示默认错误', async () => {
      vi.mocked(postApi.getPostList).mockRejectedValue({});

      render(<PostListPage />, { wrapper });

      await waitFor(() => {
        expect(screen.getByText('加载帖子列表失败')).toBeInTheDocument();
      });
    });
  });

  describe('分页功能', () => {
    it('应该显示分页控件', async () => {
      vi.mocked(postApi.getPostList).mockResolvedValue({
        content: mockPosts,
        pageable: { pageNumber: 0, pageSize: 10 },
        totalElements: 25,
        totalPages: 3,
        last: false,
        first: true,
        empty: false,
      });

      render(<PostListPage />, { wrapper });

      await waitFor(() => {
        expect(screen.getByText('上一页')).toBeInTheDocument();
        expect(screen.getByText('下一页')).toBeInTheDocument();
        expect(screen.getByText('1')).toBeInTheDocument();
        expect(screen.getByText('2')).toBeInTheDocument();
        expect(screen.getByText('3')).toBeInTheDocument();
      });
    });

    it('第一页时上一页按钮应该禁用', async () => {
      vi.mocked(postApi.getPostList).mockResolvedValue({
        content: mockPosts,
        pageable: { pageNumber: 0, pageSize: 10 },
        totalElements: 25,
        totalPages: 3,
        last: false,
        first: true,
        empty: false,
      });

      render(<PostListPage />, { wrapper });

      await waitFor(() => {
        const prevButton = screen.getByText('上一页');
        expect(prevButton).toBeDisabled();
      });
    });

    it('点击页码应该切换页面', async () => {
      const user = userEvent.setup();
      vi.mocked(postApi.getPostList)
        .mockResolvedValueOnce({
          content: mockPosts,
          pageable: { pageNumber: 0, pageSize: 10 },
          totalElements: 25,
          totalPages: 3,
          last: false,
          first: true,
          empty: false,
        })
        .mockResolvedValueOnce({
          content: [mockPosts[0]],
          pageable: { pageNumber: 1, pageSize: 10 },
          totalElements: 25,
          totalPages: 3,
          last: false,
          first: false,
          empty: false,
        });

      render(<PostListPage />, { wrapper });

      await waitFor(() => {
        expect(screen.getByText('上一页')).toBeInTheDocument();
      });

      const page2Button = screen.getByText('2');
      await user.click(page2Button);

      await waitFor(() => {
        expect(postApi.getPostList).toHaveBeenCalledWith(1, 10);
      });
    });

    it('点击下一页应该切换页面', async () => {
      const user = userEvent.setup();
      vi.mocked(postApi.getPostList)
        .mockResolvedValueOnce({
          content: mockPosts,
          pageable: { pageNumber: 0, pageSize: 10 },
          totalElements: 25,
          totalPages: 3,
          last: false,
          first: true,
          empty: false,
        })
        .mockResolvedValueOnce({
          content: [mockPosts[0]],
          pageable: { pageNumber: 1, pageSize: 10 },
          totalElements: 25,
          totalPages: 3,
          last: false,
          first: false,
          empty: false,
        });

      render(<PostListPage />, { wrapper });

      await waitFor(() => {
        expect(screen.getByText('下一页')).toBeInTheDocument();
      });

      const nextButton = screen.getByText('下一页');
      await user.click(nextButton);

      await waitFor(() => {
        expect(postApi.getPostList).toHaveBeenCalledWith(1, 10);
      });
    });

    it('最后一页时下一页按钮应该禁用', async () => {
      const user = userEvent.setup();
      vi.mocked(postApi.getPostList)
        .mockResolvedValueOnce({
          content: mockPosts,
          pageable: { pageNumber: 0, pageSize: 10 },
          totalElements: 25,
          totalPages: 3,
          last: false,
          first: true,
          empty: false,
        })
        .mockResolvedValueOnce({
          content: mockPosts,
          pageable: { pageNumber: 1, pageSize: 10 },
          totalElements: 25,
          totalPages: 3,
          last: false,
          first: false,
          empty: false,
        })
        .mockResolvedValueOnce({
          content: [mockPosts[0]],
          pageable: { pageNumber: 2, pageSize: 10 },
          totalElements: 25,
          totalPages: 3,
          last: true,
          first: false,
          empty: false,
        });

      render(<PostListPage />, { wrapper });

      // Navigate to last page by clicking page 3
      await waitFor(() => {
        expect(screen.getByText('3')).toBeInTheDocument();
      });

      const page3Button = screen.getByText('3');
      await user.click(page3Button);

      await waitFor(() => {
        const nextButton = screen.getByText('下一页');
        expect(nextButton).toBeDisabled();
      });
    });

    it('只有一页时不显示分页控件', async () => {
      vi.mocked(postApi.getPostList).mockResolvedValue({
        content: mockPosts,
        pageable: { pageNumber: 0, pageSize: 10 },
        totalElements: 2,
        totalPages: 1,
        last: true,
        first: true,
        empty: false,
      });

      render(<PostListPage />, { wrapper });

      await waitFor(() => {
        expect(screen.queryByText('上一页')).not.toBeInTheDocument();
        expect(screen.queryByText('下一页')).not.toBeInTheDocument();
      });
    });
  });
});
