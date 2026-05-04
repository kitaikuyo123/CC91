import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import CommentSection from '../components/CommentSection';
import * as commentApi from '../api/comment';
import { AuthProvider } from '../context/AuthContext';

// Mock the API
vi.mock('../api/comment');

describe('CommentSection', () => {
  const mockComments = [
    {
      id: 1,
      postId: 1,
      authorId: 1,
      authorUsername: 'user1',
      content: 'First comment',
      parentId: null,
      createdAt: '2024-01-01T10:00:00',
      status: 'ACTIVE',
      replies: [
        {
          id: 2,
          postId: 1,
          authorId: 2,
          authorUsername: 'user2',
          content: 'Reply to first comment',
          parentId: 1,
          createdAt: '2024-01-01T11:00:00',
          status: 'ACTIVE',
          replies: [],
        },
      ],
    },
    {
      id: 3,
      postId: 1,
      authorId: 3,
      authorUsername: 'user3',
      content: 'Second comment',
      parentId: null,
      createdAt: '2024-01-01T12:00:00',
      status: 'ACTIVE',
      replies: [],
    },
  ];

  beforeEach(() => {
    vi.clearAllMocks();
  });

  afterEach(() => {
    localStorage.clear();
  });

  const wrapper = ({ children }: { children: React.ReactNode }) => (
    <AuthProvider>{children}</AuthProvider>
  );

  describe('加载状态', () => {
    it('应该显示加载中状态', () => {
      vi.mocked(commentApi.getCommentsByPostId).mockImplementation(
        () => new Promise(() => {})
      );

      render(<CommentSection postId={1} />, { wrapper });

      expect(screen.getByText('加载评论中...')).toBeInTheDocument();
    });
  });

  describe('加载成功 - 有评论', () => {
    it('应该显示评论列表', async () => {
      vi.mocked(commentApi.getCommentsByPostId).mockResolvedValue(mockComments);

      render(<CommentSection postId={1} />, { wrapper });

      await waitFor(() => {
        expect(screen.getByText('评论 (2)')).toBeInTheDocument();
        expect(screen.getByText('First comment')).toBeInTheDocument();
        expect(screen.getByText('Second comment')).toBeInTheDocument();
      });
    });

    it('应该显示嵌套回复', async () => {
      vi.mocked(commentApi.getCommentsByPostId).mockResolvedValue(mockComments);

      render(<CommentSection postId={1} />, { wrapper });

      await waitFor(() => {
        expect(screen.getByText('Reply to first comment')).toBeInTheDocument();
        expect(screen.getByText('user1')).toBeInTheDocument();
        expect(screen.getByText('user2')).toBeInTheDocument();
      });
    });

    it('应该显示评论作者和创建时间', async () => {
      vi.mocked(commentApi.getCommentsByPostId).mockResolvedValue(mockComments);

      render(<CommentSection postId={1} />, { wrapper });

      await waitFor(() => {
        expect(screen.getByText('user1')).toBeInTheDocument();
        expect(screen.getByText('user3')).toBeInTheDocument();
        // Date formatting check - use getAllByText for multiple matches
        expect(screen.getAllByText(/2024/).length).toBeGreaterThan(0);
      });
    });
  });

  describe('加载成功 - 空评论', () => {
    it('应该显示空状态提示', async () => {
      vi.mocked(commentApi.getCommentsByPostId).mockResolvedValue([]);

      render(<CommentSection postId={1} />, { wrapper });

      await waitFor(() => {
        expect(screen.getByText('评论 (0)')).toBeInTheDocument();
        expect(screen.getByText('还没有评论，快来抢沙发吧！')).toBeInTheDocument();
      });
    });
  });

  describe('加载失败', () => {
    it('应该显示错误信息', async () => {
      vi.mocked(commentApi.getCommentsByPostId).mockRejectedValue({
        response: { data: { message: '加载评论失败' } },
      });

      render(<CommentSection postId={1} />, { wrapper });

      await waitFor(() => {
        expect(screen.getByText('加载评论失败')).toBeInTheDocument();
      });
    });

    it('没有错误消息时应该显示默认错误', async () => {
      vi.mocked(commentApi.getCommentsByPostId).mockRejectedValue({});

      render(<CommentSection postId={1} />, { wrapper });

      await waitFor(() => {
        expect(screen.getByText('加载评论失败')).toBeInTheDocument();
      });
    });
  });

  describe('未登录状态', () => {
    it('应该显示登录提示', async () => {
      vi.mocked(commentApi.getCommentsByPostId).mockResolvedValue([]);

      render(<CommentSection postId={1} />, { wrapper });

      await waitFor(() => {
        // Check that login link is present
        const loginLink = screen.getByText('登录');
        expect(loginLink).toBeInTheDocument();
        expect(loginLink.closest('a')).toHaveAttribute('href', '/login');
      });
    });

    it('不应该显示发表评论表单', async () => {
      vi.mocked(commentApi.getCommentsByPostId).mockResolvedValue([]);

      render(<CommentSection postId={1} />, { wrapper });

      await waitFor(() => {
        expect(screen.queryByPlaceholderText('发表你的看法...')).not.toBeInTheDocument();
      });
    });

    it('未登录时不应该显示回复按钮', async () => {
      vi.mocked(commentApi.getCommentsByPostId).mockResolvedValue(mockComments);

      render(<CommentSection postId={1} />, { wrapper });

      await waitFor(() => {
        expect(screen.queryByText('回复')).not.toBeInTheDocument();
      });
    });
  });

  describe('已登录状态 - 发表评论', () => {
    beforeEach(() => {
      localStorage.setItem('access_token', 'test-token');
      localStorage.setItem('user', JSON.stringify({ username: 'testuser', email: 'test@test.com' }));
    });

    it('应该显示发表评论表单', async () => {
      vi.mocked(commentApi.getCommentsByPostId).mockResolvedValue([]);

      render(<CommentSection postId={1} />, { wrapper });

      await waitFor(() => {
        expect(screen.getByPlaceholderText('发表你的看法...')).toBeInTheDocument();
        expect(screen.getByRole('button', { name: '发表评论' })).toBeInTheDocument();
      });
    });

    it('发表空评论不应该提交', async () => {
      const user = userEvent.setup();
      vi.mocked(commentApi.getCommentsByPostId).mockResolvedValue([]);

      render(<CommentSection postId={1} />, { wrapper });

      await waitFor(() => {
        expect(screen.getByRole('button', { name: '发表评论' })).toBeInTheDocument();
      });

      const submitButton = screen.getByRole('button', { name: '发表评论' });
      expect(submitButton).toBeDisabled();
    });

    it('成功发表评论后应该更新列表', async () => {
      const user = userEvent.setup();
      vi.mocked(commentApi.getCommentsByPostId).mockResolvedValue([]);

      const newComment = {
        id: 10,
        postId: 1,
        authorId: 1,
        authorUsername: 'testuser',
        content: 'New comment',
        parentId: null,
        createdAt: '2024-01-01T13:00:00',
        status: 'ACTIVE',
        replies: [],
      };
      vi.mocked(commentApi.createComment).mockResolvedValue(newComment);

      render(<CommentSection postId={1} />, { wrapper });

      await waitFor(() => {
        expect(screen.getByPlaceholderText('发表你的看法...')).toBeInTheDocument();
      });

      const textarea = screen.getByPlaceholderText('发表你的看法...');
      await user.type(textarea, 'New comment');

      const submitButton = screen.getByRole('button', { name: '发表评论' });
      await user.click(submitButton);

      await waitFor(() => {
        expect(commentApi.createComment).toHaveBeenCalledWith(1, { content: 'New comment' });
        expect(screen.getByText('New comment')).toBeInTheDocument();
      });
    });

    it('发表评论失败应该显示错误信息', async () => {
      const user = userEvent.setup();
      vi.mocked(commentApi.getCommentsByPostId).mockResolvedValue([]);
      vi.mocked(commentApi.createComment).mockRejectedValue({
        response: { data: { message: '发表评论失败' } },
      });

      render(<CommentSection postId={1} />, { wrapper });

      await waitFor(() => {
        expect(screen.getByPlaceholderText('发表你的看法...')).toBeInTheDocument();
      });

      const textarea = screen.getByPlaceholderText('发表你的看法...');
      await user.type(textarea, 'New comment');

      const submitButton = screen.getByRole('button', { name: '发表评论' });
      await user.click(submitButton);

      await waitFor(() => {
        expect(screen.getByText('发表评论失败')).toBeInTheDocument();
      });
    });

    it('提交中按钮应该显示加载状态', async () => {
      const user = userEvent.setup();
      vi.mocked(commentApi.getCommentsByPostId).mockResolvedValue([]);
      vi.mocked(commentApi.createComment).mockImplementation(
        () => new Promise(() => {})
      );

      render(<CommentSection postId={1} />, { wrapper });

      await waitFor(() => {
        expect(screen.getByPlaceholderText('发表你的看法...')).toBeInTheDocument();
      });

      const textarea = screen.getByPlaceholderText('发表你的看法...');
      await user.type(textarea, 'New comment');

      const submitButton = screen.getByRole('button', { name: '发表评论' });
      await user.click(submitButton);

      await waitFor(() => {
        expect(submitButton).toBeDisabled();
        expect(screen.getByText('发表中...')).toBeInTheDocument();
      });
    });
  });

  describe('已登录状态 - 回复评论', () => {
    beforeEach(() => {
      localStorage.setItem('access_token', 'test-token');
      localStorage.setItem('user', JSON.stringify({ username: 'testuser', email: 'test@test.com' }));
    });

    it('应该显示回复按钮', async () => {
      vi.mocked(commentApi.getCommentsByPostId).mockResolvedValue(mockComments);

      render(<CommentSection postId={1} />, { wrapper });

      await waitFor(() => {
        expect(screen.getAllByText('回复').length).toBeGreaterThan(0);
      });
    });

    it('点击回复按钮应该显示回复表单', async () => {
      const user = userEvent.setup();
      vi.mocked(commentApi.getCommentsByPostId).mockResolvedValue(mockComments);

      render(<CommentSection postId={1} />, { wrapper });

      await waitFor(() => {
        expect(screen.getByText('First comment')).toBeInTheDocument();
      });

      const replyButtons = screen.getAllByText('回复');
      await user.click(replyButtons[0]);

      expect(screen.getByPlaceholderText('写下你的回复...')).toBeInTheDocument();
    });

    it('再次点击回复按钮应该隐藏回复表单', async () => {
      const user = userEvent.setup();
      vi.mocked(commentApi.getCommentsByPostId).mockResolvedValue(mockComments);

      render(<CommentSection postId={1} />, { wrapper });

      await waitFor(() => {
        expect(screen.getByText('First comment')).toBeInTheDocument();
      });

      const replyButtons = screen.getAllByText('回复');
      await user.click(replyButtons[0]);
      await user.click(replyButtons[0]);

      expect(screen.queryByPlaceholderText('写下你的回复...')).not.toBeInTheDocument();
    });

    it('提交回复应该更新评论树', async () => {
      const user = userEvent.setup();
      vi.mocked(commentApi.getCommentsByPostId).mockResolvedValue(mockComments);

      const newReply = {
        id: 10,
        postId: 1,
        authorId: 1,
        authorUsername: 'testuser',
        content: 'New reply',
        parentId: 1,
        createdAt: '2024-01-01T13:00:00',
        status: 'ACTIVE',
        replies: [],
      };
      vi.mocked(commentApi.replyToComment).mockResolvedValue(newReply);

      render(<CommentSection postId={1} />, { wrapper });

      await waitFor(() => {
        expect(screen.getByText('First comment')).toBeInTheDocument();
      });

      const replyButtons = screen.getAllByText('回复');
      await user.click(replyButtons[0]);

      const textarea = screen.getByPlaceholderText('写下你的回复...');
      await user.type(textarea, 'New reply');

      const submitButton = screen.getByRole('button', { name: '发送回复' });
      await user.click(submitButton);

      await waitFor(() => {
        expect(commentApi.replyToComment).toHaveBeenCalledWith(1, { content: 'New reply' });
        expect(screen.getByText('New reply')).toBeInTheDocument();
      });
    });

    it('回复失败应该显示错误信息', async () => {
      const user = userEvent.setup();
      vi.mocked(commentApi.getCommentsByPostId).mockResolvedValue(mockComments);
      vi.mocked(commentApi.replyToComment).mockRejectedValue({
        response: { data: { message: '回复失败' } },
      });

      render(<CommentSection postId={1} />, { wrapper });

      await waitFor(() => {
        expect(screen.getByText('First comment')).toBeInTheDocument();
      });

      const replyButtons = screen.getAllByText('回复');
      await user.click(replyButtons[0]);

      const textarea = screen.getByPlaceholderText('写下你的回复...');
      await user.type(textarea, 'New reply');

      const submitButton = screen.getByRole('button', { name: '发送回复' });
      await user.click(submitButton);

      await waitFor(() => {
        expect(screen.getByText('回复失败')).toBeInTheDocument();
      });
    });

    it('回复表单提交时空内容不应该提交', async () => {
      const user = userEvent.setup();
      vi.mocked(commentApi.getCommentsByPostId).mockResolvedValue(mockComments);

      render(<CommentSection postId={1} />, { wrapper });

      await waitFor(() => {
        expect(screen.getByText('First comment')).toBeInTheDocument();
      });

      const replyButtons = screen.getAllByText('回复');
      await user.click(replyButtons[0]);

      const submitButton = screen.getByRole('button', { name: '发送回复' });
      expect(submitButton).toBeDisabled();
    });
  });

  describe('已登录状态 - 删除评论', () => {
    beforeEach(() => {
      localStorage.setItem('access_token', 'test-token');
      localStorage.setItem('user', JSON.stringify({ username: 'testuser', email: 'test@test.com' }));
    });

    it('作者应该看到删除按钮', async () => {
      const userComments = [
        {
          id: 1,
          postId: 1,
          authorId: 1,
          authorUsername: 'testuser',
          content: 'My comment',
          parentId: null,
          createdAt: '2024-01-01T10:00:00',
          status: 'ACTIVE',
          replies: [],
        },
      ];
      vi.mocked(commentApi.getCommentsByPostId).mockResolvedValue(userComments);

      render(<CommentSection postId={1} />, { wrapper });

      await waitFor(() => {
        expect(screen.getByText('My comment')).toBeInTheDocument();
        expect(screen.getByText('删除')).toBeInTheDocument();
      });
    });

    it('非作者不应该看到删除按钮', async () => {
      vi.mocked(commentApi.getCommentsByPostId).mockResolvedValue(mockComments);

      render(<CommentSection postId={1} />, { wrapper });

      await waitFor(() => {
        expect(screen.getByText('First comment')).toBeInTheDocument();
        // testuser is not user1 or user3, so no delete buttons
        expect(screen.queryByText('删除')).not.toBeInTheDocument();
      });
    });

    it('点击删除并确认应该删除评论', async () => {
      const user = userEvent.setup();
      const userComments = [
        {
          id: 1,
          postId: 1,
          authorId: 1,
          authorUsername: 'testuser',
          content: 'My comment',
          parentId: null,
          createdAt: '2024-01-01T10:00:00',
          status: 'ACTIVE',
          replies: [],
        },
      ];
      vi.mocked(commentApi.getCommentsByPostId).mockResolvedValue(userComments);
      vi.mocked(commentApi.deleteComment).mockResolvedValue(undefined);
      global.confirm = vi.fn(() => true);

      render(<CommentSection postId={1} />, { wrapper });

      await waitFor(() => {
        expect(screen.getByText('删除')).toBeInTheDocument();
      });

      const deleteButton = screen.getByText('删除');
      await user.click(deleteButton);

      expect(global.confirm).toHaveBeenCalledWith('确定要删除这条评论吗？');
      expect(commentApi.deleteComment).toHaveBeenCalledWith(1);

      await waitFor(() => {
        expect(screen.queryByText('My comment')).not.toBeInTheDocument();
      });
    });

    it('取消删除不应该删除评论', async () => {
      const user = userEvent.setup();
      const userComments = [
        {
          id: 1,
          postId: 1,
          authorId: 1,
          authorUsername: 'testuser',
          content: 'My comment',
          parentId: null,
          createdAt: '2024-01-01T10:00:00',
          status: 'ACTIVE',
          replies: [],
        },
      ];
      vi.mocked(commentApi.getCommentsByPostId).mockResolvedValue(userComments);
      vi.mocked(commentApi.deleteComment).mockResolvedValue(undefined);
      global.confirm = vi.fn(() => false);

      render(<CommentSection postId={1} />, { wrapper });

      await waitFor(() => {
        expect(screen.getByText('删除')).toBeInTheDocument();
      });

      const deleteButton = screen.getByText('删除');
      await user.click(deleteButton);

      expect(global.confirm).toHaveBeenCalled();
      expect(commentApi.deleteComment).not.toHaveBeenCalled();
      expect(screen.getByText('My comment')).toBeInTheDocument();
    });

    it('删除失败应该显示错误信息', async () => {
      const user = userEvent.setup();
      const userComments = [
        {
          id: 1,
          postId: 1,
          authorId: 1,
          authorUsername: 'testuser',
          content: 'My comment',
          parentId: null,
          createdAt: '2024-01-01T10:00:00',
          status: 'ACTIVE',
          replies: [],
        },
      ];
      vi.mocked(commentApi.getCommentsByPostId).mockResolvedValue(userComments);
      vi.mocked(commentApi.deleteComment).mockRejectedValue({
        response: { data: { message: '删除评论失败' } },
      });
      global.confirm = vi.fn(() => true);

      render(<CommentSection postId={1} />, { wrapper });

      await waitFor(() => {
        expect(screen.getByText('删除')).toBeInTheDocument();
      });

      const deleteButton = screen.getByText('删除');
      await user.click(deleteButton);

      await waitFor(() => {
        expect(screen.getByText('删除评论失败')).toBeInTheDocument();
      });
    });

    it('删除嵌套回复应该更新评论树', async () => {
      const user = userEvent.setup();
      const userComments = [
        {
          id: 1,
          postId: 1,
          authorId: 1,
          authorUsername: 'testuser',
          content: 'Parent comment',
          parentId: null,
          createdAt: '2024-01-01T10:00:00',
          status: 'ACTIVE',
          replies: [
            {
              id: 2,
              postId: 1,
              authorId: 1,
              authorUsername: 'testuser',
              content: 'Reply to delete',
              parentId: 1,
              createdAt: '2024-01-01T11:00:00',
              status: 'ACTIVE',
              replies: [],
            },
          ],
        },
      ];
      vi.mocked(commentApi.getCommentsByPostId).mockResolvedValue(userComments);
      vi.mocked(commentApi.deleteComment).mockResolvedValue(undefined);
      global.confirm = vi.fn(() => true);

      render(<CommentSection postId={1} />, { wrapper });

      await waitFor(() => {
        expect(screen.getByText('Reply to delete')).toBeInTheDocument();
      });

      const deleteButtons = screen.getAllByText('删除');
      await user.click(deleteButtons[1]); // Click the delete button for the reply

      await waitFor(() => {
        expect(commentApi.deleteComment).toHaveBeenCalledWith(2);
        expect(screen.queryByText('Reply to delete')).not.toBeInTheDocument();
      });
    });
  });
});
