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

/** What the admin screens get back: the public shape plus where it sits in the order. */
export interface AdminProduct extends Product {
  position: number;
  /** The same photos as `images`, in the same order — these are what arrangePhotos names them by. */
  keys: string[];
}

export interface AdminCategory {
  id: number;
  name: string;
  position: number;
  /** How many products are filed under it — deleting one that's in use is refused. */
  used: number;
}

async function get<T>(path: string): Promise<T> {
  const res = await fetch(path, { headers: { Accept: 'application/json' } });
  if (!res.ok) throw new Error(`${path} responded ${res.status}`);
  return res.json() as Promise<T>;
}

export const fetchProducts = (category?: string) =>
  get<Product[]>(category && category !== 'All' ? `/api/products?category=${encodeURIComponent(category)}` : '/api/products');

export const fetchCategories = () => get<string[]>('/api/categories');

/**
 * Spring hands the SPA a CSRF token in a cookie and wants it echoed on anything that writes. Read
 * per request rather than cached: it rotates on sign-in, and a stale token fails exactly like a
 * missing one. Returns nothing when security is off, which is why the contact form still posts
 * happily on a deployment with no identity provider.
 */
export function csrfHeader(): Record<string, string> {
  const token = document.cookie
    .split('; ')
    .find((c) => c.startsWith('XSRF-TOKEN='))
    ?.slice('XSRF-TOKEN='.length);
  return token ? { 'X-XSRF-TOKEN': decodeURIComponent(token) } : {};
}

/**
 * Every admin write funnels through here so one place understands the server's failure shapes: a
 * 401/403 means the session lapsed (the OIDC chain answers /api with a status rather than bouncing
 * you to a login page), and anything else carries a ProblemDetail whose `detail` is the sentence
 * the server wants the editor to read.
 */
async function send<T>(path: string, method: string, body?: unknown, form?: FormData): Promise<T> {
  const res = await fetch(path, {
    method,
    headers: {
      Accept: 'application/json',
      ...(form ? {} : { 'Content-Type': 'application/json' }),
      ...csrfHeader(),
    },
    body: form ?? (body === undefined ? undefined : JSON.stringify(body)),
  });

  if (res.status === 401 || res.status === 403) {
    throw new Error('Your sign-in has expired — refresh the page to sign in again.');
  }
  if (!res.ok) {
    const problem = await res.json().catch(() => null);
    throw new Error(problem?.detail || problem?.error || 'That did not save. Please try again.');
  }
  return (res.status === 204 ? undefined : await res.json()) as T;
}

// --- products ---------------------------------------------------------------

export const adminProducts = () => get<AdminProduct[]>('/api/admin/products');

export function createProduct(name: string, category: string, photos: File[]) {
  const form = new FormData();
  form.append('name', name);
  form.append('category', category);
  photos.forEach((p) => form.append('photos', p));
  return send<AdminProduct>('/api/admin/products', 'POST', undefined, form);
}

export const describeProduct = (id: number, name: string, category: string) =>
  send<AdminProduct>(`/api/admin/products/${id}`, 'PUT', { name, category });

export const deleteProduct = (id: number) =>
  send<{ ok: boolean }>(`/api/admin/products/${id}`, 'DELETE');

export function addPhotos(id: number, photos: File[]) {
  const form = new FormData();
  photos.forEach((p) => form.append('photos', p));
  return send<AdminProduct>(`/api/admin/products/${id}/photos`, 'POST', undefined, form);
}

/** The full arrangement the editor is looking at — removing a photo is just an omission. */
export const arrangePhotos = (id: number, keys: string[]) =>
  send<AdminProduct>(`/api/admin/products/${id}/photos`, 'PUT', keys);

export const reorderProducts = (ids: number[]) =>
  send<AdminProduct[]>('/api/admin/products/order', 'PUT', { ids });

// --- categories -------------------------------------------------------------

export const adminCategories = () => get<AdminCategory[]>('/api/admin/categories');

export const createCategory = (name: string) =>
  send<AdminCategory>('/api/admin/categories', 'POST', { name });

export const renameCategory = (id: number, name: string) =>
  send<AdminCategory>(`/api/admin/categories/${id}`, 'PUT', { name });

export const reorderCategories = (ids: number[]) =>
  send<AdminCategory[]>('/api/admin/categories/order', 'PUT', { ids });

export const deleteCategory = (id: number) =>
  send<{ ok: boolean }>(`/api/admin/categories/${id}`, 'DELETE');
