/**
 * 页面底部组件
 */
export default function Footer() {
  return (
    <footer className="footer" role="contentinfo">
      <div className="container footer-content">
        <p>&copy; {new Date().getFullYear()} CC91 论坛. All rights reserved.</p>
      </div>
    </footer>
  );
}
