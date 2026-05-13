import { useLocation, useNavigate } from 'react-router-dom';

/**
 * 404 页面 - 未匹配路由的兜底展示
 */
export default function NotFoundPage() {
  const location = useLocation();
  const navigate = useNavigate();

  return (
    <div className="container" style={{ marginTop: '2rem', maxWidth: '800px' }}>
      <div className="card empty-state" role="alert" aria-live="polite">
        <div
          aria-hidden="true"
          style={{
            fontSize: '3rem',
            lineHeight: 1,
            marginBottom: '0.75rem',
            color: 'var(--color-text-muted)',
          }}
        >
          404
        </div>
        <h1 style={{ marginBottom: '0.5rem' }}>页面不存在</h1>
        <p style={{ marginBottom: '0.25rem' }}>
          你访问的地址未找到。
        </p>
        <p style={{ fontSize: '0.9rem', color: 'var(--color-text-muted)' }}>
          路径：<span style={{ fontFamily: 'var(--font-mono)' }}>{location.pathname}</span>
        </p>

        <div style={{ display: 'flex', gap: '0.75rem', marginTop: '1.25rem', flexWrap: 'wrap', justifyContent: 'center' }}>
          <button
            type="button"
            className="btn"
            onClick={() => navigate(-1)}
          >
            返回上一页
          </button>
          <button
            type="button"
            className="btn btn-primary"
            onClick={() => navigate('/')}
          >
            回到首页
          </button>
        </div>
      </div>
    </div>
  );
}
