import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import CreatePostPage from '../pages/CreatePostPage';
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

describe('CreatePostPage', () => {
  const setupAuth = () => {
    localStorage.setItem('access_token', 'test-token');
    localStorage.setItem('user', JSON.stringify({ username: 'testuser', email: 'test@test.com' }));
  };

  beforeEach(() => {
    vi.clearAllMocks();
    mockNavigate.mockClear();
  });

  afterEach(() => {
    localStorage.clear();
  });

  const wrapper = ({ children }: { children: React.ReactNode }) => (
    <MemoryRouter>
      <AuthProvider>{children}</AuthProvider>
    </MemoryRouter>
  );

  describe('未登录状态', () => {
    it('未登录应该导航到登录页', () => {
      render(<CreatePostPage />, { wrapper });

      expect(mockNavigate).toHaveBeenCalledWith('/login');
    });

    it('未登录应该不渲染内容', () => {
      const { container } = render(<CreatePostPage />, { wrapper });

      expect(container.firstChild).toBeNull();
    });
  });

  describe('已登录状态', () => {
    beforeEach(setupAuth);

    it('应该渲染创建帖子表单', () => {
      render(<CreatePostPage />, { wrapper });

      expect(screen.getByText('发布新帖')).toBeInTheDocument();
      expect(screen.getByPlaceholderText('请输入帖子标题')).toBeInTheDocument();
      expect(screen.getByPlaceholderText('请输入帖子内容...')).toBeInTheDocument();
      expect(screen.getByRole('button', { name: '发布帖子' })).toBeInTheDocument();
      expect(screen.getByRole('button', { name: '取消' })).toBeInTheDocument();
    });

    it('应该显示标题字符计数', () => {
      render(<CreatePostPage />, { wrapper });

      expect(screen.getByText('(0/200)')).toBeInTheDocument();
    });

    it('输入标题时字符计数应该更新', async () => {
      const user = userEvent.setup();
      render(<CreatePostPage />, { wrapper });

      const titleInput = screen.getByPlaceholderText('请输入帖子标题');
      await user.type(titleInput, 'Test Title');

      expect(screen.getByText('(10/200)')).toBeInTheDocument();
    });
  });

  describe('表单验证', () => {
    beforeEach(setupAuth);

    it('提交空标题应该显示验证错误', async () => {
      const user = userEvent.setup();
      render(<CreatePostPage />, { wrapper });

      const titleInput = screen.getByPlaceholderText('请输入帖子标题');
      const contentInput = screen.getByPlaceholderText('请输入帖子内容...');

      await user.type(contentInput, 'Some content');

      const submitButton = screen.getByRole('button', { name: '发布帖子' });
      await user.click(submitButton);

      await waitFor(() => {
        expect(screen.getByText('标题不能为空')).toBeInTheDocument();
      });

      // Check that navigate was not called with a posts path (only /login might have been called in useEffect)
      expect(mockNavigate).not.toHaveBeenCalledWith('/posts/123');
    });

    it('提交空内容应该显示验证错误', async () => {
      const user = userEvent.setup();
      render(<CreatePostPage />, { wrapper });

      const titleInput = screen.getByPlaceholderText('请输入帖子标题');
      await user.type(titleInput, 'Test Title');

      const submitButton = screen.getByRole('button', { name: '发布帖子' });
      await user.click(submitButton);

      await waitFor(() => {
        expect(screen.getByText('内容不能为空')).toBeInTheDocument();
      });

      expect(mockNavigate).not.toHaveBeenCalledWith('/posts/123');
    });

    it('只有空格的标题应该显示验证错误', async () => {
      const user = userEvent.setup();
      render(<CreatePostPage />, { wrapper });

      const titleInput = screen.getByPlaceholderText('请输入帖子标题');
      const contentInput = screen.getByPlaceholderText('请输入帖子内容...');

      await user.type(titleInput, '   ');
      await user.type(contentInput, 'Some content');

      const submitButton = screen.getByRole('button', { name: '发布帖子' });
      await user.click(submitButton);

      await waitFor(() => {
        expect(screen.getByText('标题不能为空')).toBeInTheDocument();
      });

      expect(mockNavigate).not.toHaveBeenCalledWith('/posts/123');
    });

    it('只有空格的内容应该显示验证错误', async () => {
      const user = userEvent.setup();
      render(<CreatePostPage />, { wrapper });

      const titleInput = screen.getByPlaceholderText('请输入帖子标题');
      const contentInput = screen.getByPlaceholderText('请输入帖子内容...');

      await user.type(titleInput, 'Test Title');
      await user.type(contentInput, '   ');

      const submitButton = screen.getByRole('button', { name: '发布帖子' });
      await user.click(submitButton);

      await waitFor(() => {
        expect(screen.getByText('内容不能为空')).toBeInTheDocument();
      });

      expect(mockNavigate).not.toHaveBeenCalledWith('/posts/123');
    });
  });

  describe('提交成功', () => {
    beforeEach(setupAuth);

    it('成功提交后应该导航到新帖子详情页', async () => {
      const user = userEvent.setup();
      const mockCreatedPost = { id: 123, title: 'Test Title', content: 'Test content' };
      vi.mocked(postApi.createPost).mockResolvedValue(mockCreatedPost as any);

      render(<CreatePostPage />, { wrapper });

      const titleInput = screen.getByPlaceholderText('请输入帖子标题');
      const contentInput = screen.getByPlaceholderText('请输入帖子内容...');

      await user.type(titleInput, 'Test Title');
      await user.type(contentInput, 'Test content');

      const submitButton = screen.getByRole('button', { name: '发布帖子' });
      await user.click(submitButton);

      await waitFor(() => {
        expect(postApi.createPost).toHaveBeenCalledWith({
          title: 'Test Title',
          content: 'Test content',
        });
      });

      expect(mockNavigate).toHaveBeenCalledWith('/posts/123');
    });

    it('应该trim标题和内容', async () => {
      const user = userEvent.setup();
      const mockCreatedPost = { id: 123, title: 'Test Title', content: 'Test content' };
      vi.mocked(postApi.createPost).mockResolvedValue(mockCreatedPost as any);

      render(<CreatePostPage />, { wrapper });

      const titleInput = screen.getByPlaceholderText('请输入帖子标题');
      const contentInput = screen.getByPlaceholderText('请输入帖子内容...');

      await user.type(titleInput, '  Test Title  ');
      await user.type(contentInput, '  Test content  ');

      const submitButton = screen.getByRole('button', { name: '发布帖子' });
      await user.click(submitButton);

      await waitFor(() => {
        expect(postApi.createPost).toHaveBeenCalledWith({
          title: 'Test Title',
          content: 'Test content',
        });
      });
    });

    it('提交中按钮应该显示加载状态并禁用', async () => {
      const user = userEvent.setup();
      vi.mocked(postApi.createPost).mockImplementation(
        () => new Promise(() => {})
      );

      render(<CreatePostPage />, { wrapper });

      const titleInput = screen.getByPlaceholderText('请输入帖子标题');
      const contentInput = screen.getByPlaceholderText('请输入帖子内容...');

      await user.type(titleInput, 'Test Title');
      await user.type(contentInput, 'Test content');

      const submitButton = screen.getByRole('button', { name: '发布帖子' });
      await user.click(submitButton);

      await waitFor(() => {
        expect(submitButton).toBeDisabled();
        expect(screen.getByText('发布中...')).toBeInTheDocument();
      });
    });
  });

  describe('提交失败', () => {
    beforeEach(setupAuth);

    it('API错误应该显示错误信息', async () => {
      const user = userEvent.setup();
      vi.mocked(postApi.createPost).mockRejectedValue({
        response: { data: { message: '创建失败，请重试' } },
      });

      render(<CreatePostPage />, { wrapper });

      const titleInput = screen.getByPlaceholderText('请输入帖子标题');
      const contentInput = screen.getByPlaceholderText('请输入帖子内容...');

      await user.type(titleInput, 'Test Title');
      await user.type(contentInput, 'Test content');

      const submitButton = screen.getByRole('button', { name: '发布帖子' });
      await user.click(submitButton);

      await waitFor(() => {
        expect(screen.getByText('创建失败，请重试')).toBeInTheDocument();
      });

      expect(mockNavigate).not.toHaveBeenCalledWith('/posts/123');
    });

    it('没有错误消息时应该显示默认错误', async () => {
      const user = userEvent.setup();
      vi.mocked(postApi.createPost).mockRejectedValue({});

      render(<CreatePostPage />, { wrapper });

      const titleInput = screen.getByPlaceholderText('请输入帖子标题');
      const contentInput = screen.getByPlaceholderText('请输入帖子内容...');

      await user.type(titleInput, 'Test Title');
      await user.type(contentInput, 'Test content');

      const submitButton = screen.getByRole('button', { name: '发布帖子' });
      await user.click(submitButton);

      await waitFor(() => {
        expect(screen.getByText('创建帖子失败')).toBeInTheDocument();
      });
    });
  });

  describe('取消按钮', () => {
    beforeEach(setupAuth);

    it('点击取消应该导航到帖子列表', async () => {
      const user = userEvent.setup();
      render(<CreatePostPage />, { wrapper });

      const cancelButton = screen.getByRole('button', { name: '取消' });
      await user.click(cancelButton);

      expect(mockNavigate).toHaveBeenCalledWith('/posts');
    });

    it('提交中时取消按钮应该禁用', async () => {
      const user = userEvent.setup();
      vi.mocked(postApi.createPost).mockImplementation(
        () => new Promise(() => {})
      );

      render(<CreatePostPage />, { wrapper });

      const titleInput = screen.getByPlaceholderText('请输入帖子标题');
      const contentInput = screen.getByPlaceholderText('请输入帖子内容...');

      await user.type(titleInput, 'Test Title');
      await user.type(contentInput, 'Test content');

      const submitButton = screen.getByRole('button', { name: '发布帖子' });
      await user.click(submitButton);

      await waitFor(() => {
        const cancelButton = screen.getByRole('button', { name: '取消' });
        expect(cancelButton).toBeDisabled();
      });
    });
  });

  describe('输入限制', () => {
    beforeEach(setupAuth);

    it('标题输入应该限制最多200字符', async () => {
      const user = userEvent.setup();
      render(<CreatePostPage />, { wrapper });

      const titleInput = screen.getByPlaceholderText('请输入帖子标题') as HTMLInputElement;
      expect(titleInput.maxLength).toBe(200);
    });
  });
});
