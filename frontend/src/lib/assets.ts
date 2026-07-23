/**
 * Photos live in the public MinIO bucket, not in the app image — 50 MB of JPEGs has no business
 * inside a container we redeploy on every commit, and the bucket serves them with a year-long
 * cache. Small brand assets (logo marks, favicons) stay local so first paint needs nothing external.
 */
const BASE = (import.meta.env.VITE_ASSET_BASE ?? 'https://s3.thebennett.net/itsthevine').replace(/\/$/, '');

/**
 * `photo('products/scones.webp')` -> absolute bucket URL.
 *
 * Segments are encoded individually: some gallery files have spaces in their names ("Cinnamon
 * Rolls.webp") and a raw space in a URL doesn't fetch.
 */
export function photo(key: string): string {
  const path = key.replace(/^\//, '').split('/').map(encodeURIComponent).join('/');
  return `${BASE}/images/${path}`;
}
