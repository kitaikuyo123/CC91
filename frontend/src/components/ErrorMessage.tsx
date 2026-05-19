/**
 * 统一消息提示组件
 * 用于标准化错误/成功/信息提示的展示
 */

type MessageType = 'error' | 'success' | 'info' | 'warning';

interface ErrorMessageProps {
  type?: MessageType;
  message: string;
  onDismiss?: () => void;
}

const typeStyles: Record<MessageType, { bg: string; border: string; color: string }> = {
  error: { bg: '#fef2f2', border: '#fecaca', color: '#dc2626' },
  success: { bg: '#f0fdf4', border: '#bbf7d0', color: '#16a34a' },
  info: { bg: '#eff6ff', border: '#bfdbfe', color: '#2563eb' },
  warning: { bg: '#fffbeb', border: '#fde68a', color: '#d97706' },
};

export default function ErrorMessage({ type = 'error', message, onDismiss }: ErrorMessageProps) {
  if (!message) return null;

  const style = typeStyles[type];

  return (
    <div
      role="alert"
      style={{
        padding: '0.75rem 1rem',
        borderRadius: 'var(--radius-sm, 6px)',
        fontSize: '0.9rem',
        backgroundColor: style.bg,
        border: `1px solid ${style.border}`,
        color: style.color,
        display: 'flex',
        justifyContent: 'space-between',
        alignItems: 'center',
        gap: '0.5rem',
        marginBottom: '1rem',
      }}
    >
      <span>{message}</span>
      {onDismiss && (
        <button
          onClick={onDismiss}
          style={{
            background: 'none',
            border: 'none',
            cursor: 'pointer',
            color: style.color,
            fontSize: '1.1rem',
            padding: '0 0.25rem',
            lineHeight: 1,
            flexShrink: 0,
          }}
          aria-label="关闭提示"
        >
          &times;
        </button>
      )}
    </div>
  );
}
