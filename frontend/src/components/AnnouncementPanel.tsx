interface Announcement {
  id: number;
  title: string;
  date: string;
  isRed?: boolean;
  isBold?: boolean;
}

interface AnnouncementPanelProps {
  announcements: Announcement[];
}

/**
 * CC98 经典公告栏
 */
export default function AnnouncementPanel({ announcements }: AnnouncementPanelProps) {
  return (
    <div className="cc98-ann-container">
      {/* 外部大标题 */}
      <div className="cc98-ann-title-external">
        <i className="fa fa-volume-up" style={{ color: 'var(--cc98-alt-color-a)', marginRight: '0.5rem' }}></i>
        全站公告
      </div>

      {/* 复古金色粗顶部边框卡片 */}
      <div className="cc98-announcement-box-classic">
        <div className="cc98-announcement-list-classic">
          {announcements.length === 0 ? (
            <div className="cc98-ann-empty">暂无系统公告</div>
          ) : (
            announcements.map((ann) => (
              <div key={ann.id} className="cc98-announcement-item-classic">
                <a 
                  href={`#announcement-${ann.id}`}
                  onClick={(e) => { e.preventDefault(); alert(`公告详情：\n${ann.title}`); }}
                  className={`cc98-ann-link-classic ${ann.isRed ? 'red' : ''} ${ann.isBold ? 'bold' : ''}`}
                >
                  <span className="cc98-ann-date-classic">[{ann.date}]</span>
                  <span>{ann.title}</span>
                </a>
              </div>
            ))
          )}
        </div>
      </div>

      <style>{`
        .cc98-ann-container {
          width: 100%;
          margin-bottom: 2rem;
          display: flex;
          flex-direction: column;
          align-items: flex-start; /* 外部标题靠左对齐 */
        }

        .cc98-ann-title-external {
          font-size: 1.3rem;
          font-weight: bold;
          color: var(--cc98-alt-color-a); /* 绑定主题色 A */
          display: flex;
          align-items: center;
          margin-bottom: 0.85rem;
          padding-left: 0.25rem;
        }

        .cc98-announcement-box-classic {
          border: 1px solid var(--border-color); /* 绑定通用卡片微弱边框 */
          border-top: 8px solid var(--cc98-alt-color-a); /* 顶部粗边框绑定主题色 A */
          border-radius: 4px;
          background-color: var(--card-bg);
          overflow: hidden;
          box-shadow: var(--cc98-shadow);
          width: 100%;
        }

        .cc98-announcement-list-classic {
          padding: 1rem 0;
          display: flex;
          flex-direction: column;
          min-height: 100px;
        }

        .cc98-announcement-item-classic {
          padding: 0.65rem 1.5rem;
          display: flex;
          align-items: center;
          font-size: 0.92rem;
          line-height: 1.6;
          transition: var(--cc98-transition);
        }

        .cc98-announcement-item-classic:hover {
          background-color: var(--quote-bg);
        }

        .cc98-ann-link-classic {
          text-decoration: none;
          color: var(--text-main);
          font-weight: 500;
          white-space: nowrap;
          overflow: hidden;
          text-overflow: ellipsis;
          width: 100%;
          display: block;
        }

        .cc98-ann-date-classic {
          color: var(--text-muted);
          margin-right: 0.5rem;
          font-weight: normal;
        }

        .cc98-ann-link-classic.bold { 
          font-weight: bold; 
        }
        
        .cc98-ann-link-classic.red { 
          color: #fb6165; 
        }
        
        .cc98-ann-link-classic:hover { 
          text-decoration: underline; 
          color: var(--link-color);
        }

        .cc98-ann-empty {
          padding: 3rem 1rem;
          text-align: center;
          color: var(--text-muted);
          font-size: 0.88rem;
          margin: auto;
        }
      `}</style>
    </div>
  );
}
