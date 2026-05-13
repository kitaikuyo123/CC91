import { describe, it, expect, beforeEach } from 'vitest';
import { render, screen } from '@testing-library/react';
import App from '../../App';

describe('NotFoundPage (routing)', () => {
  beforeEach(() => {
    localStorage.clear();
    window.history.pushState({}, '', '/this-route-does-not-exist');
  });

  it('should render 404 page for unknown routes', () => {
    render(<App />);

    expect(screen.getByRole('heading', { name: '页面不存在' })).toBeInTheDocument();
    expect(screen.getByText('404')).toBeInTheDocument();
    expect(screen.getByText(/路径：/)).toBeInTheDocument();
    expect(screen.getByRole('button', { name: '回到首页' })).toBeInTheDocument();
  });
});
