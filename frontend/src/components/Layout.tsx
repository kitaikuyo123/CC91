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
    <div className="app">
      <Header isLoggedIn={isAuthenticated} />
      <main className="main">
        {children}
      </main>
      <Footer />

      <style>{`
        .app {
          display: flex;
          flex-direction: column;
          min-height: 100vh;
        }
        .main {
          flex: 1;
          padding: 2rem 0;
        }
      `}</style>
    </div>
  );
}
