import { type ReactNode } from 'react';
import { useAuth } from '../context/AuthContext';
import Header from './Header';
import Footer from './Footer';

interface LayoutProps {
  children: ReactNode;
}

/**
 * 主布局组件 - 包含头部、内容区、底部
 */
export default function Layout({ children }: LayoutProps) {
  const { isAuthenticated } = useAuth();
  return (
    <div className="app-layout">
      <a href="#main-content" className="skip-link">
        跳转到主要内容
      </a>
      <Header isLoggedIn={isAuthenticated} />
      <main id="main-content" className="main-content" role="main">
        {children}
      </main>
      <Footer />
    </div>
  );
}
