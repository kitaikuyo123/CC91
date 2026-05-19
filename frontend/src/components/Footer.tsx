/**
 * CC98 风格经典页脚组件 - 包含论坛数据统计、友情链接与版权信息
 */
export default function Footer() {
  return (
    <footer className="cc98-footer">
      <div className="container">
        {/* 1. Statistics Bar */}
        <div className="cc98-footer-stats">
          <div className="cc98-stat-item">
            <span className="stat-label">今日帖数：</span>
            <span className="stat-value">124</span>
          </div>
          <div className="cc98-stat-item">
            <span className="stat-label">昨日帖数：</span>
            <span className="stat-value">482</span>
          </div>
          <div className="cc98-stat-item">
            <span className="stat-label">最高日帖数：</span>
            <span className="stat-value">1,539</span>
          </div>
          <div className="cc98-stat-item">
            <span className="stat-label">论坛主题：</span>
            <span className="stat-value">1,829</span>
          </div>
          <div className="cc98-stat-item">
            <span className="stat-label">总帖数：</span>
            <span className="stat-value">38,193</span>
          </div>
          <div className="cc98-stat-item">
            <span className="stat-label">注册会员：</span>
            <span className="stat-value">9,852 人</span>
          </div>
        </div>

        {/* 2. Helpful Links */}
        <div className="cc98-footer-links">
          <a href="#about">关于我们</a>
          <span className="cc98-footer-divider">|</span>
          <a href="https://www.zju.edu.cn" target="_blank" rel="noopener noreferrer">浙江大学</a>
          <span className="cc98-footer-divider">|</span>
          <a href="#contact">联系管理员</a>
          <span className="cc98-footer-divider">|</span>
          <a href="#rules">论坛条例</a>
        </div>

        {/* 3. Copyright info */}
        <div className="cc98-footer-copyright">
          <p>&copy; {new Date().getFullYear()} CC91 论坛 - CC98 美术重构版. 版权所有.</p>
          <p style={{ marginTop: '0.25rem', fontSize: '0.75rem', opacity: 0.6 }}>
            Powered by React + Spring Boot. 求是学子的精神家园.
          </p>
        </div>
      </div>

      <style>{`
        .cc98-footer {
          background-color: var(--primary-color);
          color: rgba(255, 255, 255, 0.85);
          padding: 2rem 0;
          margin-top: auto;
          border-top: 3px solid var(--accent-color);
          transition: var(--cc98-transition);
          font-size: 0.85rem;
        }
        
        .theme-dark .cc98-footer {
          border-top-color: var(--border-color);
        }

        .cc98-footer-stats {
          display: flex;
          flex-wrap: wrap;
          justify-content: center;
          gap: 1.5rem;
          margin-bottom: 1.5rem;
          padding-bottom: 1.25rem;
          border-bottom: 1px solid rgba(255, 255, 255, 0.15);
        }

        .cc98-stat-item {
          display: flex;
          align-items: center;
        }

        .stat-label {
          color: rgba(255, 255, 255, 0.65);
        }

        .stat-value {
          font-weight: bold;
          color: var(--accent-color);
        }
        
        .theme-dark .stat-value {
          color: var(--primary-text);
        }

        .cc98-footer-links {
          display: flex;
          justify-content: center;
          align-items: center;
          gap: 0.5rem;
          margin-bottom: 1.25rem;
        }

        .cc98-footer-links a {
          color: rgba(255, 255, 255, 0.85) !important;
          text-decoration: none;
          font-weight: 500;
          transition: var(--cc98-transition);
        }

        .cc98-footer-links a:hover {
          color: white !important;
          text-shadow: 0 0 6px rgba(255, 255, 255, 0.8);
          text-decoration: underline;
        }

        .cc98-footer-divider {
          color: rgba(255, 255, 255, 0.3);
          font-size: 0.8rem;
        }

        .cc98-footer-copyright {
          text-align: center;
          color: rgba(255, 255, 255, 0.7);
        }
      `}</style>
    </footer>
  );
}
