interface PaginationProps {
  currentPage: number; // 0-indexed
  totalPages: number;
  onPageChange: (page: number) => void;
}

/**
 * CC98 经典分页器组件
 */
export default function Pagination({ currentPage, totalPages, onPageChange }: PaginationProps) {
  if (totalPages <= 1) return null;

  // 生成可见的页码列表
  const getPages = () => {
    const pages: (number | string)[] = [];
    const delta = 2; // 当前页左右显示多少个页码
    
    for (let i = 0; i < totalPages; i++) {
      if (
        i === 0 || 
        i === totalPages - 1 || 
        (i >= currentPage - delta && i <= currentPage + delta)
      ) {
        pages.push(i);
      } else if (
        (i === currentPage - delta - 1 && i > 0) ||
        (i === currentPage + delta + 1 && i < totalPages - 1)
      ) {
        pages.push('...');
      }
    }
    
    // 去重相邻的省略号
    return pages.filter((item, index, self) => {
      if (item === '...') {
        return self.indexOf('...') === index;
      }
      return true;
    });
  };

  const pages = getPages();

  return (
    <div className="cc98-pagination">
      {/* 首页 */}
      <button 
        className="cc98-page-item" 
        onClick={() => onPageChange(0)} 
        disabled={currentPage === 0}
        title="第一页"
      >
        <i className="fa fa-angle-double-left"></i>
      </button>

      {/* 上一页 */}
      <button 
        className="cc98-page-item" 
        onClick={() => onPageChange(currentPage - 1)} 
        disabled={currentPage === 0}
        title="上一页"
      >
        上一页
      </button>

      {/* 页码 */}
      {pages.map((page, index) => {
        if (page === '...') {
          return <span key={`ellipsis-${index}`} className="cc98-page-ellipsis">...</span>;
        }
        
        return (
          <button
            key={`page-${page}`}
            className={`cc98-page-item ${currentPage === page ? 'active' : ''}`}
            onClick={() => onPageChange(page as number)}
          >
            {(page as number) + 1}
          </button>
        );
      })}

      {/* 下一页 */}
      <button 
        className="cc98-page-item" 
        onClick={() => onPageChange(currentPage + 1)} 
        disabled={currentPage === totalPages - 1}
        title="下一页"
      >
        下一页
      </button>

      {/* 末页 */}
      <button 
        className="cc98-page-item" 
        onClick={() => onPageChange(totalPages - 1)} 
        disabled={currentPage === totalPages - 1}
        title="最后一页"
      >
        <i className="fa fa-angle-double-right"></i>
      </button>

      <span className="cc98-page-info">
        第 {currentPage + 1} / {totalPages} 页
      </span>

      <style>{`
        .cc98-pagination {
          display: flex;
          justify-content: flex-end;
          align-items: center;
          gap: 0.3rem;
          margin: 1.5rem 0;
          flex-wrap: wrap;
        }
        .cc98-page-item {
          min-width: 32px;
          height: 32px;
          display: inline-flex;
          align-items: center;
          justify-content: center;
          border: 1px solid var(--border-color);
          background: var(--card-bg);
          color: var(--text-main);
          font-size: 0.85rem;
          font-weight: bold;
          border-radius: 4px;
          cursor: pointer;
          transition: var(--cc98-transition);
          padding: 0 0.5rem;
        }
        .cc98-page-item:hover:not(:disabled) {
          border-color: var(--primary-color);
          color: var(--primary-text);
          background-color: var(--quote-bg);
        }
        .cc98-page-item.active {
          background-color: var(--primary-color);
          color: white;
          border-color: var(--primary-color);
        }
        .cc98-page-item:disabled {
          opacity: 0.4;
          cursor: not-allowed;
        }
        .cc98-page-ellipsis {
          color: var(--text-muted);
          padding: 0 0.5rem;
          user-select: none;
        }
        .cc98-page-info {
          font-size: 0.85rem;
          color: var(--text-muted);
          margin-left: 0.5rem;
          user-select: none;
        }
      `}</style>
    </div>
  );
}
