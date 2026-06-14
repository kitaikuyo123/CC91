import DOMPurify from 'dompurify';

export function sanitizeHtml(dirty: string): string {
  return DOMPurify.sanitize(dirty, {
    ALLOWED_TAGS: ['b', 'i', 'em', 'strong', 'a', 'p', 'br', 'ul', 'ol', 'li', 'code', 'pre', 'blockquote', 'img', 'h1', 'h2', 'h3', 'h4', 'h5', 'h6', 'span'],
    ALLOWED_ATTR: ['href', 'src', 'alt', 'class', 'target', 'rel'],
  });
}

export function escapeHtml(text: string): string {
  const div = document.createElement('div');
  div.textContent = text;
  return div.innerHTML;
}
