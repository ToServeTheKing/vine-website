/**
 * The small shared pieces of the admin screens: one icon set, one set of button and field looks.
 *
 * Extracted from the catalogue editor when the catering tables arrived, so the two screens can't
 * drift into looking like two different products.
 */

export const Icon = ({ d, className = '' }: { d: string; className?: string }) => (
  <svg
    viewBox="0 0 24 24"
    fill="none"
    stroke="currentColor"
    strokeWidth={2}
    strokeLinecap="round"
    strokeLinejoin="round"
    className={`w-4 h-4 ${className}`}
    aria-hidden="true"
  >
    <path d={d} />
  </svg>
);

export const ARROW_UP = 'M12 19V5M5 12l7-7 7 7';
export const ARROW_DOWN = 'M12 5v14M19 12l-7 7-7-7';
export const ARROW_LEFT = 'M19 12H5M12 19l-7-7 7-7';
export const ARROW_RIGHT = 'M5 12h14M12 5l7 7-7 7';
export const TRASH = 'M3 6h18M8 6V4h8v2M19 6l-1 14H6L5 6';
export const PLUS = 'M12 5v14M5 12h14';
export const CHECK = 'M20 6L9 17l-5-5';
export const X = 'M18 6L6 18M6 6l12 12';

const button =
  'inline-flex items-center justify-center gap-1.5 rounded-md px-3 py-1.5 text-sm font-medium ' +
  'transition-colors disabled:opacity-40 disabled:cursor-not-allowed';
export const primary = `${button} bg-bakery-600 text-white hover:bg-bakery-700`;
export const secondary = `${button} border border-bakery-300 text-bakery-800 hover:bg-bakery-100`;
export const danger = `${button} text-red-700 hover:bg-red-50`;
export const iconButton =
  'inline-flex items-center justify-center w-7 h-7 rounded-md border border-bakery-300 ' +
  'text-bakery-700 hover:bg-bakery-100 transition-colors disabled:opacity-30 disabled:cursor-not-allowed';
export const field =
  'w-full rounded-md border border-bakery-300 bg-white px-3 py-2 text-sm ' +
  'focus:border-bakery-500 focus:outline-none focus:ring-1 focus:ring-bakery-500';

/** Moves one entry of a list by `delta`, or returns the list untouched if that would fall off an end. */
export function shift<T>(items: T[], index: number, delta: number): T[] {
  const target = index + delta;
  if (target < 0 || target >= items.length) return items;
  const next = [...items];
  [next[index], next[target]] = [next[target], next[index]];
  return next;
}
