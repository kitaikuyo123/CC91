import { BrowserRouter, Routes, Route, Outlet } from 'react-router-dom';
import { QueryClientProvider } from '@tanstack/react-query';
import { AuthProvider, useAuth } from './context/AuthContext';
import ErrorBoundary from './components/ErrorBoundary';
import ProtectedRoute from './components/ProtectedRoute';
import AdminRoute from './components/AdminRoute';
import Layout from './components/Layout';
import AdminLayout from './components/AdminLayout';
import HomePage from './pages/HomePage';
import LoginPage from './pages/LoginPage';
import RegisterPage from './pages/RegisterPage';
import DashboardPage from './pages/DashboardPage';
import MyPostsPage from './pages/MyPostsPage';
import MyCommentsPage from './pages/MyCommentsPage';
import MyDraftsPage from './pages/MyDraftsPage';
import ProfilePage from './pages/ProfilePage';
import ProfileEditPage from './pages/ProfileEditPage';
import PostListPage from './pages/PostListPage';
import PostDetailPage from './pages/PostDetailPage';
import CreatePostPage from './pages/CreatePostPage';
import EditPostPage from './pages/EditPostPage';
import CategoryPage from './pages/CategoryPage';
import ForgotPasswordPage from './pages/ForgotPasswordPage';
import ResetPasswordPage from './pages/ResetPasswordPage';
import SearchPage from './pages/SearchPage';
import NotificationsPage from './pages/NotificationsPage';
import NotFoundPage from './pages/NotFoundPage';
import AdminDashboard from './pages/admin/AdminDashboard';
import CategoryManage from './pages/admin/CategoryManage';
import ContentModeration from './pages/admin/ContentModeration';
import UserManage from './pages/admin/UserManage';
import { queryClient } from './lib/queryClient';

function AdminRoutes() {
  const { isAdmin } = useAuth();
  return (
    <AdminRoute isAdmin={isAdmin}>
      <AdminLayout />
    </AdminRoute>
  );
}

function App() {
  return (
    <QueryClientProvider client={queryClient}>
      <BrowserRouter>
        <AuthProvider>
          <ErrorBoundary>
          <Routes>
            <Route path="/" element={<Layout><Outlet /></Layout>}>
              <Route index element={<HomePage />} />
              <Route path="login" element={<LoginPage />} />
              <Route path="register" element={<RegisterPage />} />
              <Route path="forgot-password" element={<ForgotPasswordPage />} />
              <Route path="reset-password" element={<ResetPasswordPage />} />
              <Route path="profile/:username" element={<ProfilePage />} />
              <Route
                path="profile/edit"
                element={
                  <ProtectedRoute>
                    <ProfileEditPage />
                  </ProtectedRoute>
                }
              />
              <Route
                path="dashboard"
                element={
                  <ProtectedRoute>
                    <DashboardPage />
                  </ProtectedRoute>
                }
              />

              <Route
                path="dashboard/posts"
                element={
                  <ProtectedRoute>
                    <MyPostsPage />
                  </ProtectedRoute>
                }
              />

              <Route
                path="dashboard/comments"
                element={
                  <ProtectedRoute>
                    <MyCommentsPage />
                  </ProtectedRoute>
                }
              />

              <Route
                path="dashboard/drafts"
                element={
                  <ProtectedRoute>
                    <MyDraftsPage />
                  </ProtectedRoute>
                }
              />

              {/* 版块相关路由 */}
              <Route path="category/:id" element={<CategoryPage />} />

              {/* 帖子相关路由 */}
              <Route path="posts" element={<PostListPage />} />
              <Route path="posts/:id" element={<PostDetailPage />} />
              <Route
                path="posts/new"
                element={
                  <ProtectedRoute>
                    <CreatePostPage />
                  </ProtectedRoute>
                }
              />
              <Route
                path="posts/:id/edit"
                element={
                  <ProtectedRoute>
                    <EditPostPage />
                  </ProtectedRoute>
                }
              />

              {/* 搜索和通知路由 */}
              <Route path="search" element={<SearchPage />} />
              <Route
                path="notifications"
                element={
                  <ProtectedRoute>
                    <NotificationsPage />
                  </ProtectedRoute>
                }
              />

              {/* 404 */}
              <Route path="*" element={<NotFoundPage />} />
            </Route>

            {/* 管理后台路由 */}
            <Route path="/admin" element={
              <ProtectedRoute>
                <AdminRoutes />
              </ProtectedRoute>
            }>
              <Route index element={<AdminDashboard />} />
              <Route path="categories" element={<CategoryManage />} />
              <Route path="content" element={<ContentModeration />} />
              <Route path="users" element={<UserManage />} />

              {/* 404 */}
              <Route path="*" element={<NotFoundPage />} />
            </Route>
          </Routes>
          </ErrorBoundary>
        </AuthProvider>
      </BrowserRouter>
    </QueryClientProvider>
  );
}

export default App;
