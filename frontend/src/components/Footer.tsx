/**
 * CC98 风格经典页脚组件 - 包含论坛数据统计、友情链接与版权信息
 */
export default function Footer() {
  return (
    <footer className="cc98-footer">
      <div className="container">
        {/* 1. Helpful Links */}
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
          <p>&copy; {new Date().getFullYear()} CC91 论坛. 版权所有.</p>
          <p style={{ marginTop: '0.25rem', fontSize: '0.75rem', opacity: 0.6 }}>
            Powered by React + Spring Boot.
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

        @media (max-width: 768px) {
          .cc98-footer-stats {
            gap: 0.75rem 1rem;
            font-size: 0.78rem;
            justify-content: flex-start;
          }

          .cc98-footer-links {
            flex-wrap: wrap;
            gap: 0.3rem 0.6rem;
          }

          .cc98-footer {
            padding: 1.25rem 0;
          }
        }

        @media (max-width: 375px) {
          .cc98-footer-stats {
            gap: 0.4rem 0.6rem;
            font-size: 0.72rem;
          }
        }
      `}</style>
    </footer>
  );
}
