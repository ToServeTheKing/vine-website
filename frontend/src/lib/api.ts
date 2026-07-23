/**
 * The catalogue, its ordering, its category filter and its image URLs are all decided by the
 * backend — this file just fetches them. Same origin, so no base URL and no CORS.
 */

export interface Product {
  id: number;
  name: string;
  category: string;
  /** Absolute, ready to put in a src. Built server-side from the bucket config. */
  images: string[];
}

async function get<T>(path: string): Promise<T> {
  const res = await fetch(path, { headers: { Accept: 'application/json' } });
  if (!res.ok) throw new Error(`${path} responded ${res.status}`);
  return res.json() as Promise<T>;
}

export const fetchProducts = (category?: string) =>
  get<Product[]>(category && category !== 'All' ? `/api/products?category=${encodeURIComponent(category)}` : '/api/products');

export const fetchCategories = () => get<string[]>('/api/categories');
