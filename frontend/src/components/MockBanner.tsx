import { useState, useEffect } from 'react';

/**
 * MockBanner Component
 * Renders a sticky notification banner at the top of the app layout when the application
 * is running in Mock Mode (backend offline / VITE_USE_MOCK enabled).
 */
export default function MockBanner() {
  const [isMock, setIsMock] = useState(localStorage.getItem('use_mock') === 'true');
  const [isVisible, setIsVisible] = useState(true);

  useEffect(() => {
    const handleStorageChange = () => {
      setIsMock(localStorage.getItem('use_mock') === 'true');
    };
    window.addEventListener('storage', handleStorageChange);
    window.addEventListener('mock-mode-changed', handleStorageChange);
    return () => {
      window.removeEventListener('storage', handleStorageChange);
      window.removeEventListener('mock-mode-changed', handleStorageChange);
    };
  }, []);

  const handleSwitchToLive = () => {
    localStorage.removeItem('use_mock');
    window.location.reload();
  };

  const handleSwitchToMock = () => {
    localStorage.setItem('use_mock', 'true');
    window.location.reload();
  };

  if (!isMock) {
    // Show a very subtle floating button in the bottom right corner to allow developer manual override
    return (
      <div 
        style={{
          position: 'fixed',
          bottom: '20px',
          right: '20px',
          zIndex: 9999,
          backgroundColor: 'rgba(57, 70, 118, 0.85)',
          backdropFilter: 'blur(5px)',
          color: '#fff',
          padding: '8px 12px',
          borderRadius: '20px',
          fontSize: '12px',
          cursor: 'pointer',
          boxShadow: '0 4px 12px rgba(0,0,0,0.15)',
          border: '1px solid rgba(255,255,255,0.2)',
          transition: 'all 0.3s ease',
          display: 'flex',
          alignItems: 'center',
          gap: '6px',
        }}
        onClick={handleSwitchToMock}
        title="点击切换至 Mock 模式进行离线开发"
      >
        <span style={{ display: 'inline-block', width: '8px', height: '8px', borderRadius: '50%', backgroundColor: '#2ecc71' }}></span>
        <span>连接真实后端 (点击切换 Mock)</span>
      </div>
    );
  }

  if (!isVisible) {
    // Banner is minimized, show a minimized sticky indicator
    return (
      <div 
        style={{
          position: 'fixed',
          bottom: '20px',
          right: '20px',
          zIndex: 9999,
          backgroundColor: 'rgba(231, 76, 60, 0.9)',
          backdropFilter: 'blur(5px)',
          color: '#fff',
          padding: '8px 12px',
          borderRadius: '20px',
          fontSize: '12px',
          cursor: 'pointer',
          boxShadow: '0 4px 12px rgba(0,0,0,0.15)',
          border: '1px solid rgba(255,255,255,0.2)',
          transition: 'all 0.3s ease',
          display: 'flex',
          alignItems: 'center',
          gap: '6px',
        }}
        onClick={() => setIsVisible(true)}
        title="点击展开 Mock 提示栏"
      >
        <span style={{ display: 'inline-block', width: '8px', height: '8px', borderRadius: '50%', backgroundColor: '#f1c40f' }}></span>
        <span>已进入 Mock 模式 (点击展开提示)</span>
      </div>
    );
  }

  return (
    <div style={{
      width: '100%',
      backgroundColor: '#e74c3c',
      backgroundImage: 'linear-gradient(90deg, #e74c3c, #c0392b)',
      color: '#fff',
      fontSize: '13px',
      padding: '8px 16px',
      boxSizing: 'border-box',
      display: 'flex',
      justifyContent: 'space-between',
      alignItems: 'center',
      boxShadow: '0 2px 8px rgba(0,0,0,0.15)',
      zIndex: 10000,
      position: 'relative',
      backdropFilter: 'blur(10px)',
      fontFamily: 'system-ui, -apple-system, sans-serif'
    }}>
      <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
        <span style={{ fontSize: '15px' }}>⚠️</span>
        <span>
          <strong>当前正使用 Mock 数据模式</strong> (后端服务未启动或连接异常)。您进行的所有修改都将保留在本地浏览器缓存中。
        </span>
      </div>
      <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
        <button 
          onClick={handleSwitchToLive}
          style={{
            backgroundColor: '#fff',
            color: '#c0392b',
            border: 'none',
            padding: '4px 12px',
            borderRadius: '4px',
            fontWeight: 'bold',
            cursor: 'pointer',
            fontSize: '12px',
            boxShadow: '0 2px 4px rgba(0,0,0,0.1)',
            transition: 'background-color 0.2s',
          }}
          onMouseOver={(e) => (e.currentTarget.style.backgroundColor = '#f5f5f5')}
          onMouseOut={(e) => (e.currentTarget.style.backgroundColor = '#fff')}
        >
          ⚡ 连接真实后端
        </button>
        <button 
          onClick={() => setIsVisible(false)}
          style={{
            backgroundColor: 'transparent',
            color: 'rgba(255,255,255,0.7)',
            border: 'none',
            cursor: 'pointer',
            fontSize: '16px',
            padding: '0 4px',
            lineHeight: 1,
          }}
          title="收起提示栏"
        >
          ×
        </button>
      </div>
    </div>
  );
}
