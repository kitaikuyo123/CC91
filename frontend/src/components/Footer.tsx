/**
 * 页面底部组件
 */
export default function Footer() {
  return (
    <footer className="footer">
      <div className="container">
        <p>&copy; 2024 CC91 论坛. All rights reserved.</p>
      </div>

      <style>{`
        .footer {
          background-color: #34495e;
          color: white;
          text-align: center;
          padding: 1.5rem 0;
          margin-top: auto;
        }
        .container {
          max-width: 1200px;
          margin: 0 auto;
          padding: 0 1rem;
        }
      `}</style>
    </footer>
  );
}
