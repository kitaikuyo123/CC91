import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import EditPostPage from '../pages/EditPostPage';
import * as postApi from '../api/post';
import { AuthProvider } from '../context/AuthContext';

// Mock the API
vi.mock('../api/post');

// Mock react-router-dom
const mockNavigate = vi.fn();
vi.mock('react-router-dom', async () => ({
  ...(await vi.importActual('react-router-dom')),
  useNavigate: () => mockNavigate,
  useParams: () => ({ id: '1' }),
}));

describe('EditPostPage', () => {
  const mockPost = {
    id: 1,
    title: 'Original Title',
    content: 'Original content',
    authorId: 1,
    authorUsername: 'testuser',
    createdAt: '2024-01-01T10:00:00',
    updatedAt: '2024-01-01T10:00:00',
    viewCount: 100,
  };

  beforeEach(() => {
    vi.clearAllMocks();
    mockNavigate.mockClear();
  });

  afterEach(() => {
    localStorage.clear();
  });

  const renderWithAuth = (username: string | null) => {
    const TestWrapper = ({ children }: { children: React.ReactNode }) => (
      <MemoryRouter initialEntries={['/posts/1/edit']}>
        <AuthProvider>
          <Routes>
            <Route path="/posts/:id/edit" element={children} />
          </Routes>
        </AuthProvider>
      </MemoryRouter>
    );

    if (username) {
      localStorage.setItem('access_token', 'test-token');
      localStorage.setItem('user', JSON.stringify({ username, email: `${username}@test.com` }));
    }

    return render(<EditPostPage />, { wrapper: TestWrapper });
  };

  describe('未登录状态', () => {
    it('未登录应该导航到登录页', () => {
      renderWithAuth(null);

      expect(mockNavigate).toHaveBeenCalledWith('/login');
    });

    it('未登录应该不渲染内容', () => {
      const { container } = renderWithAuth(null);

      expect(container.firstChild).toBeNull();
    });
  });

  describe('加载状态', () => {
    beforeEach(() => {
      localStorage.setItem('access_token', 'test-token');
      localStorage.setItem('user', JSON.stringify({ username: 'testuser', email: 'test@test.com' }));
    });

    it('应该显示加载中状态', () => {
      vi.mocked(postApi.getPostById).mockImplementation(
        () => new Promise(() => {})
      );

      renderWithAuth('testuser');

      expect(screen.getByText('加载中...')).toBeInTheDocument();
    });
  });

  describe('加载失败', () => {
    beforeEach(() => {
      localStorage.setItem('access_token', 'test-token');
      localStorage.setItem('user', JSON.stringify({ username: 'testuser', email: 'test@test.com' }));
    });

    it('应该显示错误信息', async () => {
      vi.mocked(postApi.getPostById).mockRejectedValue({
        response: { data: { message: '帖子不存在' } },
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

      const backButton = screen.getByRole('button', { name: '返回列表' });
      await user.click(backButton);

      expect(mockNavigate).toHaveBeenCalledWith('/posts');
    });
  });

  describe('权限检查', () => {
    beforeEach(() => {
      localStorage.setItem('access_token', 'test-token');
      localStorage.setItem('user', JSON.stringify({ username: 'otheruser', email: 'other@test.com' }));
    });

    it('非作者应该显示权限错误', async () => {
      vi.mocked(postApi.getPostById).mockResolvedValue(mockPost);

      renderWithAuth('otheruser');

      await waitFor(() => {
        expect(screen.getByText('你没有权限编辑这篇帖子')).toBeInTheDocument();
      });
    });

    it('非作者点击返回帖子应该导航到帖子详情', async () => {
      const user = userEvent.setup();
      vi.mocked(postApi.getPostById).mockResolvedValue(mockPost);

      renderWithAuth('otheruser');

      await waitFor(() => {
        expect(screen.getByText('返回帖子')).toBeInTheDocument();
      });

      const backButton = screen.getByRole('button', { name: '返回帖子' });
      await user.click(backButton);

      expect(mockNavigate).toHaveBeenCalledWith('/posts/1');
    });
  });

  describe('加载成功 - 作者', () => {
    beforeEach(() => {
      localStorage.setItem('access_token', 'test-token');
      localStorage.setItem('user', JSON.stringify({ username: 'testuser', email: 'test@test.com' }));
    });

    it('应该预填充帖子数据', async () => {
      vi.mocked(postApi.getPostById).mockResolvedValue(mockPost);

      renderWithAuth('testuser');

      await waitFor(() => {
        const titleInput = screen.getByPlaceholderText('请输入帖子标题') as HTMLInputElement;
        const contentInput = screen.getByPlaceholderText('请输入帖子内容...') as HTMLTextAreaElement;

        expect(titleInput.value).toBe('Original Title');
        expect(contentInput.value).toBe('Original content');
      });
    });

    it('应该显示编辑帖子表单', async () => {
      vi.mocked(postApi.getPostById).mockResolvedValue(mockPost);

      renderWithAuth('testuser');

      await waitFor(() => {
        expect(screen.getByText('编辑帖子')).toBeInTheDocument();
        expect(screen.getByPlaceholderText('请输入帖子标题')).toBeInTheDocument();
        expect(screen.getByPlaceholderText('请输入帖子内容...')).toBeInTheDocument();
        expect(screen.getByRole('button', { name: '保存修改' })).toBeInTheDocument();
        expect(screen.getByRole('button', { name: '取消' })).toBeInTheDocument();
      });
    });

    it('应该显示标题字符计数', async () => {
      vi.mocked(postApi.getPostById).mockResolvedValue(mockPost);

      renderWithAuth('testuser');

      await waitFor(() => {
        expect(screen.getByText('(14/200)')).toBeInTheDocument();
      });
    });
  });

  describe('表单验证', () => {
    beforeEach(() => {
      localStorage.setItem('access_token', 'test-token');
      localStorage.setItem('user', JSON.stringify({ username: 'testuser', email: 'test@test.com' }));
    });

    it('提交空标题应该显示验证错误', async () => {
      const user = userEvent.setup();
      vi.mocked(postApi.getPostById).mockResolvedValue(mockPost);

      renderWithAuth('testuser');

      await waitFor(() => {
        expect(screen.getByPlaceholderText('请输入帖子标题')).toBeInTheDocument();
      });

      const titleInput = screen.getByPlaceholderText('请输入帖子标题');
      await user.clear(titleInput);

      const submitButton = screen.getByRole('button', { name: '保存修改' });
      await user.click(submitButton);

      await waitFor(() => {
        expect(screen.getByText('标题不能为空')).toBeInTheDocument();
      });

      expect(mockNavigate).not.toHaveBeenCalledWith('/posts/1');
    });

    it('提交空内容应该显示验证错误', async () => {
      const user = userEvent.setup();
      vi.mocked(postApi.getPostById).mockResolvedValue(mockPost);

      renderWithAuth('testuser');

      await waitFor(() => {
        expect(screen.getByPlaceholderText('请输入帖子内容...')).toBeInTheDocument();
      });

      const contentInput = screen.getByPlaceholderText('请输入帖子内容...');
      await user.clear(contentInput);

      const submitButton = screen.getByRole('button', { name: '保存修改' });
      await user.click(submitButton);

      await waitFor(() => {
        expect(screen.getByText('内容不能为空')).toBeInTheDocument();
      });

      expect(mockNavigate).not.toHaveBeenCalledWith('/posts/1');
    });
  });

  describe('提交成功', () => {
    beforeEach(() => {
      localStorage.setItem('access_token', 'test-token');
      localStorage.setItem('user', JSON.stringify({ username: 'testuser', email: 'test@test.com' }));
    });

    it('成功更新后应该导航到帖子详情页', async () => {
      const user = userEvent.setup();
      vi.mocked(postApi.getPostById).mockResolvedValue(mockPost);
      vi.mocked(postApi.updatePost).mockResolvedValue(mockPost as any);

      renderWithAuth('testuser');

      await waitFor(() => {
        expect(screen.getByPlaceholderText('请输入帖子标题')).toBeInTheDocument();
      });

      const titleInput = screen.getByPlaceholderText('请输入帖子标题');
      await user.clear(titleInput);
      await user.type(titleInput, 'Updated Title');

      const submitButton = screen.getByRole('button', { name: '保存修改' });
      await user.click(submitButton);

      await waitFor(() => {
        expect(postApi.updatePost).toHaveBeenCalledWith(1, {
          title: 'Updated Title',
          content: 'Original content',
        });
      });

      expect(mockNavigate).toHaveBeenCalledWith('/posts/1');
    });

    it('应该trim标题和内容', async () => {
      const user = userEvent.setup();
      vi.mocked(postApi.getPostById).mockResolvedValue(mockPost);
      vi.mocked(postApi.updatePost).mockResolvedValue(mockPost as any);

      renderWithAuth('testuser');

      await waitFor(() => {
        expect(screen.getByPlaceholderText('请输入帖子标题')).toBeInTheDocument();
      });

      const titleInput = screen.getByPlaceholderText('请输入帖子标题');
      const contentInput = screen.getByPlaceholderText('请输入帖子内容...');

      await user.clear(titleInput);
      await user.clear(contentInput);
      await user.type(titleInput, '  Updated Title  ');
      await user.type(contentInput, '  Updated content  ');

      const submitButton = screen.getByRole('button', { name: '保存修改' });
      await user.click(submitButton);

      await waitFor(() => {
        expect(postApi.updatePost).toHaveBeenCalledWith(1, {
          title: 'Updated Title',
          content: 'Updated content',
        });
      });
    });

    it('提交中按钮应该显示加载状态并禁用', async () => {
      const user = userEvent.setup();
      vi.mocked(postApi.getPostById).mockResolvedValue(mockPost);
      vi.mocked(postApi.updatePost).mockImplementation(
        () => new Promise(() => {})
      );

      renderWithAuth('testuser');

      await waitFor(() => {
        expect(screen.getByPlaceholderText('请输入帖子标题')).toBeInTheDocument();
      });

      const submitButton = screen.getByRole('button', { name: '保存修改' });
      await user.click(submitButton);

      await waitFor(() => {
        expect(submitButton).toBeDisabled();
        expect(screen.getByText('保存中...')).toBeInTheDocument();
      });
    });
  });

  describe('提交失败', () => {
    beforeEach(() => {
      localStorage.setItem('access_token', 'test-token');
      localStorage.setItem('user', JSON.stringify({ username: 'testuser', email: 'test@test.com' }));
    });

    it('API错误应该显示错误信息', async () => {
      const user = userEvent.setup();
      vi.mocked(postApi.getPostById).mockResolvedValue(mockPost);
      vi.mocked(postApi.updatePost).mockRejectedValue({
        response: { data: { message: '更新失败' } },
      });

      renderWithAuth('testuser');

      await waitFor(() => {
        expect(screen.getByPlaceholderText('请输入帖子标题')).toBeInTheDocument();
      });

      const submitButton = screen.getByRole('button', { name: '保存修改' });
      await user.click(submitButton);

      await waitFor(() => {
        expect(screen.getByText('更新失败')).toBeInTheDocument();
      });

      expect(mockNavigate).not.toHaveBeenCalledWith('/posts/1');
    });
  });

  describe('取消按钮', () => {
    beforeEach(() => {
      localStorage.setItem('access_token', 'test-token');
      localStorage.setItem('user', JSON.stringify({ username: 'testuser', email: 'test@test.com' }));
    });

    it('点击取消应该导航到帖子详情', async () => {
      const user = userEvent.setup();
      vi.mocked(postApi.getPostById).mockResolvedValue(mockPost);

      renderWithAuth('testuser');

      await waitFor(() => {
        expect(screen.getByRole('button', { name: '取消' })).toBeInTheDocument();
      });

      const cancelButton = screen.getByRole('button', { name: '取消' });
      await user.click(cancelButton);

      expect(mockNavigate).toHaveBeenCalledWith('/posts/1');
    });

    it('提交中时取消按钮应该禁用', async () => {
      const user = userEvent.setup();
      vi.mocked(postApi.getPostById).mockResolvedValue(mockPost);
      vi.mocked(postApi.updatePost).mockImplementation(
        () => new Promise(() => {})
      );

      renderWithAuth('testuser');

      await waitFor(() => {
        expect(screen.getByPlaceholderText('请输入帖子标题')).toBeInTheDocument();
      });

      const submitButton = screen.getByRole('button', { name: '保存修改' });
      await user.click(submitButton);

      await waitFor(() => {
        const cancelButton = screen.getByRole('button', { name: '取消' });
        expect(cancelButton).toBeDisabled();
      });
    });
  });
});
