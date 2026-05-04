import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import PostDetailPage from '../pages/PostDetailPage';
import CommentSection from '../components/CommentSection';
import * as postApi from '../api/post';
import { AuthProvider } from '../context/AuthContext';

// Mock the API
vi.mock('../api/post');
vi.mock('../components/CommentSection');

// Mock react-router-dom
const mockNavigate = vi.fn();
vi.mock('react-router-dom', async () => ({
  ...(await vi.importActual('react-router-dom')),
  useNavigate: () => mockNavigate,
  useParams: () => ({ id: '1' }),
}));

describe('PostDetailPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mockNavigate.mockClear();
    // Mock CommentSection
    vi.mocked(CommentSection).mockReturnValue(<div data-testid="comment-section">Comments</div>);
  });

  const mockPost = {
    id: 1,
    title: 'Test Post Title',
    content: 'This is the post content with multiple lines.\nLine 2\nLine 3',
    authorId: 1,
    authorUsername: 'testuser',
    createdAt: '2024-01-01T10:00:00',
    updatedAt: '2024-01-01T10:00:00',
    viewCount: 100,
  };

  const renderWithAuth = (username: string | null) => {
    const TestWrapper = ({ children }: { children: React.ReactNode }) => (
      <MemoryRouter initialEntries={['/posts/1']}>
        <AuthProvider>
          <Routes>
            <Route path="/posts/:id" element={children} />
          </Routes>
        </AuthProvider>
      </MemoryRouter>
    );

    // Set up auth state
    if (username) {
      localStorage.setItem('access_token', 'test-token');
      localStorage.setItem('user', JSON.stringify({ username, email: `${username}@test.com` }));
    }

    const result = render(<PostDetailPage />, { wrapper: TestWrapper });
    return result;
  };

  const cleanupAuth = () => {
    localStorage.clear();
  };

  describe('加载状态', () => {
    afterEach(cleanupAuth);

    it('应该显示加载中状态', () => {
      vi.mocked(postApi.getPostById).mockImplementation(
        () => new Promise(() => {})
      );

      renderWithAuth('testuser');

      expect(screen.getByText('加载中...')).toBeInTheDocument();
    });
  });

  describe('加载成功', () => {
    afterEach(cleanupAuth);

    it('应该显示帖子详情', async () => {
      vi.mocked(postApi.getPostById).mockResolvedValue(mockPost);

      renderWithAuth('otheruser');

      // Wait for loading to complete first
      await waitFor(() => {
        expect(screen.queryByText('加载中...')).not.toBeInTheDocument();
      });

      expect(screen.getByText('Test Post Title')).toBeInTheDocument();
      // Check for content using a more specific text that appears in the rendered output
      expect(screen.getByText(/multiple lines/)).toBeInTheDocument();
      expect(screen.getByText('testuser')).toBeInTheDocument();
      expect(screen.getByText('浏览: 100')).toBeInTheDocument();
    });

    it('非作者不应该看到编辑删除按钮', async () => {
      vi.mocked(postApi.getPostById).mockResolvedValue(mockPost);

      renderWithAuth('otheruser');

      await waitFor(() => {
        expect(screen.getByText('Test Post Title')).toBeInTheDocument();
      });

      expect(screen.queryByText('编辑')).not.toBeInTheDocument();
      expect(screen.queryByText('删除')).not.toBeInTheDocument();
    });

    it('作者应该看到编辑删除按钮', async () => {
      vi.mocked(postApi.getPostById).mockResolvedValue(mockPost);

      renderWithAuth('testuser');

      await waitFor(() => {
        expect(screen.getByText('Test Post Title')).toBeInTheDocument();
      });

      expect(screen.getByText('编辑')).toBeInTheDocument();
      expect(screen.getByText('删除')).toBeInTheDocument();
    });

    it('应该显示评论区域', async () => {
      vi.mocked(postApi.getPostById).mockResolvedValue(mockPost);

      renderWithAuth('testuser');

      await waitFor(() => {
        expect(screen.getByTestId('comment-section')).toBeInTheDocument();
      });
    });

    it('点击返回列表应该导航到帖子列表', async () => {
      const user = userEvent.setup();
      vi.mocked(postApi.getPostById).mockResolvedValue(mockPost);

      renderWithAuth('testuser');

      await waitFor(() => {
        expect(screen.getByText('Test Post Title')).toBeInTheDocument();
      });

      // Find the button by role since text includes arrow character
      const buttons = screen.getAllByRole('button');
      const backButton = buttons.find(btn => btn.textContent?.includes('返回列表'));
      await user.click(backButton!);

      expect(mockNavigate).toHaveBeenCalledWith('/posts');
    });

    it('点击编辑按钮应该导航到编辑页面', async () => {
      const user = userEvent.setup();
      vi.mocked(postApi.getPostById).mockResolvedValue(mockPost);

      renderWithAuth('testuser');

      await waitFor(() => {
        expect(screen.getByText('编辑')).toBeInTheDocument();
      });

      const editButton = screen.getByText('编辑');
      await user.click(editButton);

      expect(mockNavigate).toHaveBeenCalledWith('/posts/1/edit');
    });
  });

  describe('删除帖子', () => {
    afterEach(cleanupAuth);

    it('作者点击删除并确认应该删除帖子并导航', async () => {
      const user = userEvent.setup();
      vi.mocked(postApi.getPostById).mockResolvedValue(mockPost);
      vi.mocked(postApi.deletePost).mockResolvedValue(undefined);
      global.confirm = vi.fn(() => true);

      renderWithAuth('testuser');

      await waitFor(() => {
        expect(screen.getByText('删除')).toBeInTheDocument();
      });

      const deleteButton = screen.getByText('删除');
      await user.click(deleteButton);

      expect(global.confirm).toHaveBeenCalledWith('确定要删除这篇帖子吗？此操作不可恢复。');
      expect(postApi.deletePost).toHaveBeenCalledWith(1);
      expect(mockNavigate).toHaveBeenCalledWith('/posts');
    });

    it('删除时取消确认不应该删除', async () => {
      const user = userEvent.setup();
      vi.mocked(postApi.getPostById).mockResolvedValue(mockPost);
      vi.mocked(postApi.deletePost).mockResolvedValue(undefined);
      global.confirm = vi.fn(() => false);

      renderWithAuth('testuser');

      await waitFor(() => {
        expect(screen.getByText('删除')).toBeInTheDocument();
      });

      const deleteButton = screen.getByText('删除');
      await user.click(deleteButton);

      expect(global.confirm).toHaveBeenCalled();
      expect(postApi.deletePost).not.toHaveBeenCalled();
    });

    it('删除失败应该显示错误信息', async () => {
      const user = userEvent.setup();
      vi.mocked(postApi.getPostById).mockResolvedValue(mockPost);
      vi.mocked(postApi.deletePost).mockRejectedValue({
        response: { data: { message: '删除失败' } },
      });
      global.confirm = vi.fn(() => true);

      renderWithAuth('testuser');

      await waitFor(() => {
        expect(screen.getByText('删除')).toBeInTheDocument();
      });

      const deleteButton = screen.getByText('删除');
      await user.click(deleteButton);

      await waitFor(() => {
        expect(screen.getByText('删除失败')).toBeInTheDocument();
      });

      expect(mockNavigate).not.toHaveBeenCalledWith('/posts');
    });

    it('删除中应该禁用删除按钮', async () => {
      const user = userEvent.setup();
      vi.mocked(postApi.getPostById).mockResolvedValue(mockPost);
      vi.mocked(postApi.deletePost).mockImplementation(
        () => new Promise(() => {})
      );
      global.confirm = vi.fn(() => true);

      renderWithAuth('testuser');

      await waitFor(() => {
        expect(screen.getByText('删除')).toBeInTheDocument();
      });

      const deleteButton = screen.getByText('删除');
      await user.click(deleteButton);

      await waitFor(() => {
        expect(deleteButton).toBeDisabled();
      });
    });
  });

  describe('加载失败', () => {
    afterEach(cleanupAuth);

    it('应该显示错误信息', async () => {
      vi.mocked(postApi.getPostById).mockRejectedValue({
        response: { data: { message: '帖子不存在' }, status: 404 },
      });

      renderWithAuth('testuser');

      await waitFor(() => {
        expect(screen.getByText('帖子不存在')).toBeInTheDocument();
      });
    });

    it('404错误应该显示特定错误信息', async () => {
      vi.mocked(postApi.getPostById).mockRejectedValue({
        response: { data: { message: 'Not found' }, status: 404 },
      });

      renderWithAuth('testuser');

      await waitFor(() => {
        expect(screen.getByText('帖子不存在')).toBeInTheDocument();
      });
    });

    it('错误状态下点击返回列表应该导航到帖子列表', async () => {
      const user = userEvent.setup();
      vi.mocked(postApi.getPostById).mockRejectedValue({});

      renderWithAuth('testuser');

      await waitFor(() => {
        expect(screen.getByText('返回列表')).toBeInTheDocument();
      });

      const backButton = screen.getByText('返回列表');
      await user.click(backButton);

      expect(mockNavigate).toHaveBeenCalledWith('/posts');
    });
  });

  describe('帖子时间显示', () => {
    afterEach(cleanupAuth);

    it('更新时间与创建时间不同时应该显示更新时间', async () => {
      const updatedPost = {
        ...mockPost,
        updatedAt: '2024-01-02T12:00:00',
      };
      vi.mocked(postApi.getPostById).mockResolvedValue(updatedPost);

      renderWithAuth('testuser');

      await waitFor(() => {
        expect(screen.getByText('Test Post Title')).toBeInTheDocument();
      });

      expect(screen.getByText(/发布于/)).toBeInTheDocument();
      expect(screen.getByText(/更新于/)).toBeInTheDocument();
    });

    it('更新时间与创建时间相同时不应该显示更新时间', async () => {
      vi.mocked(postApi.getPostById).mockResolvedValue(mockPost);

      renderWithAuth('testuser');

      await waitFor(() => {
        expect(screen.getByText('Test Post Title')).toBeInTheDocument();
      });

      expect(screen.getByText(/发布于/)).toBeInTheDocument();
      expect(screen.queryByText(/更新于/)).not.toBeInTheDocument();
    });
  });
});
