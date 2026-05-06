/**
 * Format a date string into a human-readable format.
 *
 * Uses the longest format found across the codebase, which includes
 * year, month, day, hour, and minute.
 */
export function formatDate(dateString: string): string {
  return new Date(dateString).toLocaleString('zh-CN', {
    year: 'numeric',
    month: 'long',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  });
}
