import { describe, it, expect, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import { MemoryRouter, Routes, Route } from 'react-router-dom';
import AdminLayout from '../../components/AdminLayout';

describe('AdminLayout', () => {
  const renderWithRouter = (initialPath = '/admin') => {
    return render(
      <MemoryRouter initialEntries={[initialPath]}>
        <Routes>
          <Route path="/admin" element={<AdminLayout />}>
            <Route index element={<div>Admin Dashboard</div>} />
          </Route>
          <Route path="/admin/categories" element={<AdminLayout />}>
            <Route index element={<div>Category Management</div>} />
          </Route>
          <Route path="/" element={<div>Forum Home</div>} />
        </Routes>
      </MemoryRouter>
    );
  };

  it('should render sidebar navigation links', () => {
    renderWithRouter();
    expect(screen.getByText('首页')).toBeInTheDocument();
    expect(screen.getByText('版块管理')).toBeInTheDocument();
    expect(screen.getByText('内容审核')).toBeInTheDocument();
    expect(screen.getByText('用户管理')).toBeInTheDocument();
    expect(screen.getByText('公告管理')).toBeInTheDocument();
  });

  it('should render the back to forum link', () => {
    renderWithRouter();
    expect(screen.getByText('← 返回论坛')).toBeInTheDocument();
  });

  it('should render Outlet content', () => {
    renderWithRouter();
    expect(screen.getByText('Admin Dashboard')).toBeInTheDocument();
  });

  it('should render sidebar header', () => {
    renderWithRouter();
    // There are two "管理后台" texts: mobile header and sidebar header
    const headings = screen.getAllByText('管理后台');
    expect(headings.length).toBeGreaterThanOrEqual(1);
  });

  it('should mark active nav link with aria-current', () => {
    renderWithRouter('/admin');
    const activeLink = screen.getByText('首页').closest('a');
    expect(activeLink).toHaveAttribute('aria-current', 'page');
  });

  it('should render the menu toggle button', () => {
    renderWithRouter();
    expect(screen.getByLabelText('打开菜单')).toBeInTheDocument();
  });
});
