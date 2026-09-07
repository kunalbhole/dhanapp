const inrFormatter = new Intl.NumberFormat('en-IN', { maximumFractionDigits: 0 });

export function formatINR(n: number): string {
  const sign = n < 0 ? '−' : '';
  return `${sign}₹${inrFormatter.format(Math.abs(n))}`;
}

export function monthKey(date: Date = new Date()): string {
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}`;
}

export function monthRange(date: Date = new Date()): [number, number] {
  const start = new Date(date.getFullYear(), date.getMonth(), 1).getTime();
  const end = new Date(date.getFullYear(), date.getMonth() + 1, 1).getTime();
  return [start, end];
}

export function dayGroupLabel(millis: number): string {
  const d = new Date(millis);
  const today = new Date();
  const yesterday = new Date();
  yesterday.setDate(today.getDate() - 1);
  if (isSameDay(d, today)) return 'Today';
  if (isSameDay(d, yesterday)) return 'Yesterday';
  return d.toLocaleDateString('en-IN', { weekday: 'short', month: 'short', day: 'numeric' });
}

export function dateLabel(millis: number): string {
  return new Date(millis).toLocaleDateString('en-IN', { month: 'short', day: 'numeric', year: 'numeric' });
}

export function timeLabel(millis: number): string {
  return new Date(millis).toLocaleTimeString('en-IN', { hour: 'numeric', minute: '2-digit' });
}

export function formatBytes(bytes: number): string {
  if (bytes < 1024) return `${bytes} B`;
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
}

function isSameDay(a: Date, b: Date): boolean {
  return a.getFullYear() === b.getFullYear() && a.getMonth() === b.getMonth() && a.getDate() === b.getDate();
}
