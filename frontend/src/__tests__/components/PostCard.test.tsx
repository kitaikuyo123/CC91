import { describe, it, expect, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import PostCard from '../../components/PostCard';

// Mock dompurify: pass-through for normal tags but strip <script> (模拟真实 XSS 净化)
vi.mock('dompurify', () => ({
  default: {
    sanitize: (html: string) => html.replace(/<script[\s\S]*?<\/script>/gi, ''),
  },
}));

// Mock image imports
vi.mock('../../assets/cc98_avatar_cat.png', () => ({
  default: 'cat-avatar.png',
}));
vi.mock('../../assets/cc98_avatar_student.png', () => ({
  default: 'student-avatar.png',
}));

const renderPostCard = (overrides: Record<string, any> = {}) => {
  const defaultProps = {
    id: 1,
    authorUsername: 'testuser',
    floor: 1,
    content: 'Hello World',
    createdAt: '2024-01-15T10:00:00',
  };

  return render(
    <MemoryRouter>
      <PostCard {...defaultProps} {...overrides} />
    </MemoryRouter>
  );
};

describe('PostCard', () => {
  it('should render content, author, and floor', () => {
    renderPostCard();

    expect(screen.getByText('testuser')).toBeInTheDocument();
    expect(screen.getByText(/Hello World/)).toBeInTheDocument();
    expect(screen.getByText('1 楼')).toBeInTheDocument();
  });

  it('should call onToggleLike when clicking the like button', async () => {
    const user = userEvent.setup();
    const onToggleLike = vi.fn();
    renderPostCard({ onToggleLike, likeCount: 5 });

    const likeButton = screen.getByTitle('赞同此楼发言');
    await user.click(likeButton);

    expect(onToggleLike).toHaveBeenCalledTimes(1);
  });

  it('should show bookmark button when onToggleBookmark is provided', () => {
    const onToggleBookmark = vi.fn();
    renderPostCard({ onToggleBookmark, isBookmarkedByCurrentUser: false });

    expect(screen.getByTitle('收藏此贴')).toBeInTheDocument();
  });

  it('should show "已收藏" when bookmarked', () => {
    const onToggleBookmark = vi.fn();
    renderPostCard({ onToggleBookmark, isBookmarkedByCurrentUser: true });

    expect(screen.getByTitle('从收藏夹中移除')).toBeInTheDocument();
    expect(screen.getByText('已收藏')).toBeInTheDocument();
  });

  it('should not show bookmark button when onToggleBookmark is not provided', () => {
    renderPostCard();

    expect(screen.queryByTitle('收藏此贴')).not.toBeInTheDocument();
    expect(screen.queryByTitle('从收藏夹中移除')).not.toBeInTheDocument();
  });

  it('should render markdown images as img tags', () => {
    renderPostCard({ content: 'Check this out: ![my-image](http://example.com/test.png)' });

    const img = screen.getByRole('img', { name: 'my-image' });
    expect(img).toBeInTheDocument();
    expect(img).toHaveAttribute('src', 'http://example.com/test.png');
  });

  it('should not render <script> in markdown image alt text (XSS protection)', () => {
    // alt 已被 HTML 转义为 &lt;script&gt;...&gt;，DOMPurify 会再次过滤
    const { container } = renderPostCard({
      content: '![<script>alert(1)</script>](/uploads/x.png)',
    });
    expect(container.querySelector('script')).toBeNull();
    // 应仍渲染 img 标签
    const img = container.querySelector('img.cc98-post-image');
    expect(img).not.toBeNull();
  });
});
