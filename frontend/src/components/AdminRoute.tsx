import { Navigate } from 'react-router-dom';

interface AdminRouteProps {
  children: React.ReactNode;
  isAdmin: boolean;
}

/**
 * 管理员路由保护组件
 * 非 ADMIN 用户显示 403 或重定向
 */
export default function AdminRoute({ children, isAdmin }: AdminRouteProps) {
  if (!isAdmin) {
    return (
      <div style={{
        display: 'flex',
        flexDirection: 'column',
        alignItems: 'center',
        justifyContent: 'center',
        minHeight: '50vh',
        textAlign: 'center'
      }}>
        <div style={{ fontSize: '4rem', marginBottom: '1rem' }}>🚫</div>
        <h1>403 - 禁止访问</h1>
        <p style={{ color: '#666', marginTop: '1rem' }}>
          您没有权限访问管理后台
        </p>
      </div>
    );
  }

  return <>{children}</>;
}
