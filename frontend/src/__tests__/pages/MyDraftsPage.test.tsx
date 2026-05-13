import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { AuthProvider } from '../../context/AuthContext';
import MyDraftsPage from '../../pages/MyDraftsPage';
import * as userApi from '../../api/user';

vi.mock('../../api/user');

const mockNavigate = vi.fn();
vi.mock('react-router-dom', async () => ({
  ...(await vi.importActual('react-router-dom')),
  useNavigate: () => mockNavigate,
}));

const mockDraft = {
  id: 1,
  title: 'My Draft',
  content: 'Draft content.',
  authorId: 1,
  authorUsername: 'testuser',
  categoryName: '技术交流',
  createdAt: '2024-01-02T10:00:00',
  updatedAt: '2024-01-02T10:00:00',
  viewCount: 0,
  commentCount: 0,
  status: 'DRAFT',
};

const mockUntitledDraft = {
  id: 2,
  title: '',
  content: '',
  authorId: 1,
  authorUsername: 'testuser',
  createdAt: '2024-01-03T10:00:00',
  updatedAt: '2024-01-03T10:00:00',
  viewCount: 0,
  commentCount: 0,
  status: 'DRAFT',
};

describe('MyDraftsPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    localStorage.clear();
    localStorage.setItem('access_token', 'test-token');
    localStorage.setItem('user', JSON.stringify({ username: 'testuser', email: 'test@example.com' }));
  });

  const createWrapper = (initialEntries: string[] = ['/dashboard/drafts']) => {
    const queryClient = new QueryClient({
      defaultOptions: {
        queries: { retry: false, gcTime: 0 },
        mutations: { retry: false },
      },
    });

    return function Wrapper({ children }: { children: React.ReactNode }) {
      return (
        <QueryClientProvider client={queryClient}>
          <MemoryRouter initialEntries={initialEntries}>
            <AuthProvider>{children}</AuthProvider>
          </MemoryRouter>
        </QueryClientProvider>
      );
    };
  };

  it('应该渲染草稿列表', async () => {
    vi.mocked(userApi.getMyDrafts).mockResolvedValue([mockDraft]);

    render(<MyDraftsPage />, { wrapper: createWrapper() });

    await waitFor(() => {
      expect(screen.getByText('My Draft')).toBeInTheDocument();
      expect(screen.getByText('草稿')).toBeInTheDocument();
    });
    expect(screen.getByText('共 1 篇草稿')).toBeInTheDocument();
  });

  it('无标题草稿应显示"(无标题)"', async () => {
    vi.mocked(userApi.getMyDrafts).mockResolvedValue([mockUntitledDraft]);

    render(<MyDraftsPage />, { wrapper: createWrapper() });

    await waitFor(() => {
      expect(screen.getByText('(无标题)')).toBeInTheDocument();
      expect(screen.getByText('(空内容)')).toBeInTheDocument();
    });
  });

  it('点击草稿应跳转到编辑页面', async () => {
    const user = userEvent.setup();
    vi.mocked(userApi.getMyDrafts).mockResolvedValue([{ ...mockDraft, id: 99 }]);

    render(<MyDraftsPage />, { wrapper: createWrapper() });

    const title = await screen.findByText('My Draft');
    const card = title.closest('.card');
    await user.click(card!);

    expect(mockNavigate).toHaveBeenCalledWith('/posts/new?draftId=99');
  });

  it('空列表应显示空状态', async () => {
    vi.mocked(userApi.getMyDrafts).mockResolvedValue([]);

    render(<MyDraftsPage />, { wrapper: createWrapper() });

    await waitFor(() => {
      expect(screen.getByText('暂无草稿')).toBeInTheDocument();
    });
  });

  it('点击返回 Dashboard 应导航到 /dashboard', async () => {
    const user = userEvent.setup();
    vi.mocked(userApi.getMyDrafts).mockResolvedValue([]);

    render(<MyDraftsPage />, { wrapper: createWrapper() });

    const btn = await screen.findByRole('button', { name: /返回 Dashboard/ });
    await user.click(btn);

    expect(mockNavigate).toHaveBeenCalledWith('/dashboard');
  });
});
