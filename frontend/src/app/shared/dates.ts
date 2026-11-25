/** Date helpers working on yyyy-MM-dd strings in local time (what <input type="date"> and the API use). */

const pad = (n: number) => String(n).padStart(2, '0');

export function toIso(date: Date): string {
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`;
}

export function parseIso(iso: string): Date {
  const [y, m, d] = iso.split('-').map(Number);
  return new Date(y, m - 1, d);
}

export function todayIso(): string {
  return toIso(new Date());
}

export function addDays(iso: string, days: number): string {
  const date = parseIso(iso);
  date.setDate(date.getDate() + days);
  return toIso(date);
}

export function nightsBetween(checkIn: string, checkOut: string): number {
  return Math.round((parseIso(checkOut).getTime() - parseIso(checkIn).getTime()) / 86_400_000);
}

const weekdayFmt = new Intl.DateTimeFormat('en-IN', { weekday: 'short' });
const weekdayLongFmt = new Intl.DateTimeFormat('en-IN', { weekday: 'long' });
const dayMonthFmt = new Intl.DateTimeFormat('en-IN', { day: 'numeric', month: 'short' });
const fullFmt = new Intl.DateTimeFormat('en-IN', { day: 'numeric', month: 'short', year: 'numeric' });
const stampFmt = new Intl.DateTimeFormat('en-IN', {
  day: 'numeric',
  month: 'short',
  year: 'numeric',
  hour: 'numeric',
  minute: '2-digit',
});

export const weekday = (iso: string) => weekdayFmt.format(parseIso(iso));
export const weekdayLong = (iso: string) => weekdayLongFmt.format(parseIso(iso));
export const dayMonth = (iso: string) => dayMonthFmt.format(parseIso(iso));
export const fullDate = (iso: string) => fullFmt.format(parseIso(iso));
export const dateTime = (isoDateTime: string) => stampFmt.format(new Date(isoDateTime));

export const MAX_STAY_NIGHTS = 30;

/** Mirrors the backend rules so people get instant feedback; the backend still validates everything. */
export function stayProblem(checkIn: string, checkOut: string): string | null {
  if (checkIn < todayIso()) {
    return 'Check-in must be today or a later date.';
  }
  if (checkOut <= checkIn) {
    return 'Check-out must be after check-in.';
  }
  if (nightsBetween(checkIn, checkOut) > MAX_STAY_NIGHTS) {
    return `Stays are limited to ${MAX_STAY_NIGHTS} nights.`;
  }
  return null;
}
