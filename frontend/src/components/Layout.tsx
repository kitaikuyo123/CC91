import { useState, useEffect, type ReactNode } from 'react';
import Header from './Header';
import Footer from './Footer';
import MockBanner from './MockBanner';
import { useAuth } from '../context/AuthContext';

interface LayoutProps {
  children: ReactNode;
}

/**
 * 主布局组件 - 包含头部、内容区、底部，支持全局主题切换和动态 Font Awesome 加载
 */
export default function Layout({ children }: LayoutProps) {
  const { isAuthenticated } = useAuth();
  const [theme, setTheme] = useState(() => localStorage.getItem('cc98-theme') || 'classic');

  // 1. 动态加载 Font Awesome 4.7.0
  useEffect(() => {
    const linkId = 'font-awesome-470';
    if (!document.getElementById(linkId)) {
      const link = document.createElement('link');
      link.id = linkId;
      link.rel = 'stylesheet';
      link.href = 'https://cdnjs.cloudflare.com/ajax/libs/font-awesome/4.7.0/css/font-awesome.min.css';
      document.head.appendChild(link);
    }
  }, []);

  // 2. 监听并应用全局主题到 body
  useEffect(() => {
    const themes = ['theme-classic', 'theme-warm', 'theme-sakura', 'theme-dark'];
    themes.forEach(t => document.body.classList.remove(t));
    document.body.classList.add(`theme-${theme}`);
    localStorage.setItem('cc98-theme', theme);
  }, [theme]);

  // 3. 监听全局自定义主题切换事件
  useEffect(() => {
    const handleThemeChange = (e: CustomEvent<string>) => {
      setTheme(e.detail);
    };
    window.addEventListener('cc98-theme-change', handleThemeChange as EventListener);
    return () => {
      window.removeEventListener('cc98-theme-change', handleThemeChange as EventListener);
    };
  }, []);

  return (
    <div className={`app theme-${theme}`}>
      <MockBanner />
      <a href="#main-content" className="skip-link">
        跳转到主要内容
      </a>
      <Header />
      <main id="main-content" className="main-content container" role="main">
        {children}
      </main>
      <Footer />

      <style>{`
        .app {
          display: flex;
          flex-direction: column;
          min-height: 100vh;
          background-color: var(--page-bg);
          transition: var(--cc98-transition);
        }
        .main-content {
          flex: 1;
          padding: 1.5rem 0;
          min-height: calc(100vh - 280px);
        }
      `}</style>
    </div>
  );
}
