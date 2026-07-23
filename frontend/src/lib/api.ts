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

/**
 * Spring Security protects every mutating request with a CSRF token, and the platform's security
 * starter writes it to a readable XSRF-TOKEN cookie. Without this header a POST is rejected 403 —
 * including the public contact form, which is not obvious until the form stops working.
 */
function csrfHeaders(): Record<string, string> {
  const token = document.cookie.split('; ').find((c) => c.startsWith('XSRF-TOKEN='))?.split('=')[1];
  return token ? { 'X-XSRF-TOKEN': decodeURIComponent(token) } : {};
}

async function get<T>(path: string): Promise<T> {
  const res = await fetch(path, { headers: { Accept: 'application/json' } });
  if (!res.ok) throw new Error(`${path} responded ${res.status}`);
  return res.json() as Promise<T>;
}

export const fetchProducts = (category?: string) =>
  get<Product[]>(category && category !== 'All' ? `/api/products?category=${encodeURIComponent(category)}` : '/api/products');

export const fetchCategories = () => get<string[]>('/api/categories');

// ---- who is signed in (public: the SPA asks on every page load) ----
export interface Me { authenticated: boolean; admin: boolean; name: string | null }
export const fetchMe = () => get<Me>('/api/me');

// ---- admin (everything below needs an Authentik login) ----
export interface AdminProduct {
  id: number;
  name: string;
  category: string;
  position: number;
  imageKeys: string[];
  imageUrls: string[];
}
export interface AdminEnquiry {
  id: number;
  name: string;
  email: string;
  message: string;
  delivered: boolean;
  receivedAt: string;
}
export interface ProductForm {
  name: string;
  category: string;
  position: number | null;
  imageKeys: string[];
}

async function send<T>(path: string, method: string, body?: unknown): Promise<T> {
  const res = await fetch(path, {
    method,
    headers: { 'Content-Type': 'application/json', Accept: 'application/json', ...csrfHeaders() },
    body: body === undefined ? undefined : JSON.stringify(body),
  });
  if (!res.ok) {
    // The backend puts a human-readable reason in `detail`; show that rather than a status code.
    let detail = `${method} ${path} responded ${res.status}`;
    try { detail = (await res.json()).detail ?? detail; } catch { /* not JSON */ }
    throw new Error(detail);
  }
  return res.status === 204 ? (undefined as T) : (res.json() as Promise<T>);
}

export const fetchAdminProducts = () => get<AdminProduct[]>('/api/admin/products');
export const createProduct = (f: ProductForm) => send<AdminProduct>('/api/admin/products', 'POST', f);
export const updateProduct = (id: number, f: ProductForm) =>
  send<AdminProduct>(`/api/admin/products/${id}`, 'PUT', f);
export const deleteProduct = (id: number) => send<void>(`/api/admin/products/${id}`, 'DELETE');
export const fetchEnquiries = () => get<AdminEnquiry[]>('/api/admin/enquiries');

export interface UploadTarget { key: string; uploadUrl: string; publicUrl: string }

/** Presign, then PUT the file straight to the bucket — the photo never passes through the app. */
export async function uploadPhoto(file: File): Promise<UploadTarget> {
  const target = await send<UploadTarget>(
    `/api/admin/images/presign-upload?filename=${encodeURIComponent(file.name)}`
      + `&contentType=${encodeURIComponent(file.type || 'application/octet-stream')}`,
    'POST');
  // Straight to the bucket, so no CSRF header here — it is a different origin and a presigned URL.
  const put = await fetch(target.uploadUrl, {
    method: 'PUT',
    headers: { 'Content-Type': file.type || 'application/octet-stream' },
    body: file,
  });
  if (!put.ok) throw new Error(`the bucket rejected the upload (${put.status})`);
  return target;
}

/** The public contact form. Mutating, so it needs the CSRF token too. */
export async function submitContact(input: { name: string; email: string; message: string }) {
  const res = await fetch('/api/contact', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json', ...csrfHeaders() },
    body: JSON.stringify(input),
  });
  const data = await res.json().catch(() => ({}));
  if (!res.ok) throw new Error(data.error || 'Could not send the message.');
  return data;
}
