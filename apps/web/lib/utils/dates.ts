export function formatDate(value?: string | null): string {
  if (!value) return "-";
  return new Date(value).toLocaleString("en-TZ");
}

export const formatDateTime = formatDate;

export function countdown(target?: string | null): string {
  if (!target) return "-";
  const diff = new Date(target).getTime() - Date.now();
  if (diff <= 0) return "due";
  const days = Math.floor(diff / (1000 * 60 * 60 * 24));
  const hours = Math.floor((diff / (1000 * 60 * 60)) % 24);
  return `${days}d ${hours}h`;
}
