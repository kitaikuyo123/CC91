import { Link } from 'react-router-dom';

interface BreadcrumbItem {
  label: string;
  href?: string;
}

interface BreadcrumbsProps {
  items: BreadcrumbItem[];
}

/**
 * CC98 经典面包屑导航组件
 */
export default function Breadcrumbs({ items }: BreadcrumbsProps) {
  return (
    <div className="cc98-breadcrumbs">
      <Link to="/">
        <i className="fa fa-home"></i> 首页
      </Link>
      
      {items.map((item, index) => (
        <span key={index} style={{ display: 'inline-flex', alignItems: 'center', gap: '0.5rem' }}>
          <span className="cc98-breadcrumb-sep">
            <i className="fa fa-angle-right"></i>
          </span>
          {item.href ? (
            <Link to={item.href}>{item.label}</Link>
          ) : (
            <span className="cc98-breadcrumb-current">{item.label}</span>
          )}
        </span>
      ))}

      <style>{`
        .cc98-breadcrumbs {
          display: flex;
          align-items: center;
          flex-wrap: wrap;
          gap: 0.5rem;
          font-size: 0.85rem;
          color: var(--text-muted);
          margin-bottom: 1.25rem;
          background: var(--card-bg);
          padding: 0.6rem 1rem;
          border-radius: var(--cc98-radius);
          border: 1px solid var(--border-color);
        }
        .cc98-breadcrumbs a {
          color: var(--text-muted);
          text-decoration: none;
          display: inline-flex;
          align-items: center;
          gap: 0.25rem;
          transition: var(--cc98-transition);
        }
        .cc98-breadcrumbs a:hover {
          color: var(--link-color);
          text-decoration: underline;
        }
        .cc98-breadcrumb-sep {
          color: var(--border-color);
          font-weight: bold;
        }
        .cc98-breadcrumb-current {
          color: var(--text-main);
          font-weight: 500;
        }
      `}</style>
    </div>
  );
}
