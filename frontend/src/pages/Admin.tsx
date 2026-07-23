import { useCallback, useEffect, useRef, useState } from 'react';
import {
  addPhotos,
  adminCategories,
  adminProducts,
  arrangePhotos,
  createCategory,
  createProduct,
  deleteCategory,
  deleteProduct,
  describeProduct,
  renameCategory,
  reorderCategories,
  reorderProducts,
  type AdminCategory,
  type AdminProduct,
} from '@/lib/api';

/**
 * The catalogue, editable by the person who bakes it.
 *
 * The whole screen is built around one rule: nothing waits on the network to look like it happened.
 * Reordering and deleting apply to the list on screen first and reconcile afterwards, because an
 * editor tidying twenty items shouldn't be typing into a page that freezes between every click.
 * Uploads are the exception — they genuinely take a moment (resize, convert, send), so they say so.
 *
 * Getting here at all means signing in: /admin is an authenticated path, so an unknown visitor is
 * sent to the identity provider before this ever loads.
 */

// --- little pieces ----------------------------------------------------------

const Icon = ({ d, className = '' }: { d: string; className?: string }) => (
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

const ARROW_UP = 'M12 19V5M5 12l7-7 7 7';
const ARROW_DOWN = 'M12 5v14M19 12l-7 7-7-7';
const ARROW_LEFT = 'M19 12H5M12 19l-7-7 7-7';
const ARROW_RIGHT = 'M5 12h14M12 5l7 7-7 7';
const TRASH = 'M3 6h18M8 6V4h8v2M19 6l-1 14H6L5 6';
const PLUS = 'M12 5v14M5 12h14';
const CHECK = 'M20 6L9 17l-5-5';
const X = 'M18 6L6 18M6 6l12 12';

const button =
  'inline-flex items-center justify-center gap-1.5 rounded-md px-3 py-1.5 text-sm font-medium ' +
  'transition-colors disabled:opacity-40 disabled:cursor-not-allowed';
const primary = `${button} bg-bakery-600 text-white hover:bg-bakery-700`;
const secondary = `${button} border border-bakery-300 text-bakery-800 hover:bg-bakery-100`;
const danger = `${button} text-red-700 hover:bg-red-50`;
const iconButton =
  'inline-flex items-center justify-center w-7 h-7 rounded-md border border-bakery-300 ' +
  'text-bakery-700 hover:bg-bakery-100 transition-colors disabled:opacity-30 disabled:cursor-not-allowed';
const field =
  'w-full rounded-md border border-bakery-300 bg-white px-3 py-2 text-sm ' +
  'focus:border-bakery-500 focus:outline-none focus:ring-1 focus:ring-bakery-500';

/** Moves one entry of a list by `delta`, or returns the list untouched if that would fall off an end. */
function shift<T>(items: T[], index: number, delta: number): T[] {
  const target = index + delta;
  if (target < 0 || target >= items.length) return items;
  const next = [...items];
  [next[index], next[target]] = [next[target], next[index]];
  return next;
}

// --- photos -----------------------------------------------------------------

/**
 * The photos on one item. Order matters — the first is the one the products page leads with — so
 * arranging is left/right rather than a drag target, which is far easier to hit on a phone.
 */
const Photos = ({
  product,
  onArrange,
  busy,
}: {
  product: AdminProduct;
  onArrange: (keys: string[]) => void;
  busy: boolean;
}) => (
  <div className="flex flex-wrap gap-3">
    {product.images.map((url, i) => (
      <figure key={product.keys[i] ?? url} className="w-28">
        <img
          src={url}
          alt=""
          loading="lazy"
          className="w-28 h-28 rounded-md object-cover border border-bakery-200 bg-bakery-100"
        />
        <figcaption className="mt-1 flex items-center justify-between gap-1">
          <div className="flex gap-1">
            <button
              type="button"
              className={iconButton}
              disabled={busy || i === 0}
              onClick={() => onArrange(shift(product.keys, i, -1))}
              aria-label="Move photo earlier"
            >
              <Icon d={ARROW_LEFT} />
            </button>
            <button
              type="button"
              className={iconButton}
              disabled={busy || i === product.images.length - 1}
              onClick={() => onArrange(shift(product.keys, i, 1))}
              aria-label="Move photo later"
            >
              <Icon d={ARROW_RIGHT} />
            </button>
          </div>
          <button
            type="button"
            className={iconButton}
            // The server refuses an empty arrangement; saying so up front beats an error message.
            disabled={busy || product.images.length === 1}
            onClick={() => onArrange(product.keys.filter((_, at) => at !== i))}
            aria-label="Remove photo"
            title={product.images.length === 1 ? 'An item needs at least one photo' : 'Remove photo'}
          >
            <Icon d={TRASH} />
          </button>
        </figcaption>
      </figure>
    ))}
  </div>
);

/** A file picker styled as a button, resetting itself so the same file can be chosen twice. */
const PhotoPicker = ({
  label,
  onPick,
  disabled,
  className = secondary,
}: {
  label: string;
  onPick: (files: File[]) => void;
  disabled?: boolean;
  className?: string;
}) => {
  const input = useRef<HTMLInputElement>(null);
  return (
    <>
      <input
        ref={input}
        type="file"
        accept="image/*"
        multiple
        className="hidden"
        onChange={(e) => {
          const files = Array.from(e.target.files ?? []);
          e.target.value = '';
          if (files.length) onPick(files);
        }}
      />
      <button type="button" className={className} disabled={disabled} onClick={() => input.current?.click()}>
        <Icon d={PLUS} />
        {label}
      </button>
    </>
  );
};

// --- one item ---------------------------------------------------------------

const ProductCard = ({
  product,
  categories,
  first,
  last,
  onChange,
  onMove,
  onDelete,
  onError,
}: {
  product: AdminProduct;
  categories: string[];
  first: boolean;
  last: boolean;
  onChange: (updated: AdminProduct) => void;
  onMove: (delta: number) => void;
  onDelete: () => void;
  onError: (message: string) => void;
}) => {
  const [name, setName] = useState(product.name);
  const [category, setCategory] = useState(product.category);
  const [busy, setBusy] = useState(false);

  // A reorder or an upload re-fetches this product; the fields should follow unless they're being
  // edited, which is what the dirty check below decides.
  const dirty = name !== product.name || category !== product.category;
  const [synced, setSynced] = useState(product);
  if (synced !== product) {
    setSynced(product);
    if (!dirty) {
      setName(product.name);
      setCategory(product.category);
    }
  }

  const run = async (work: () => Promise<AdminProduct>) => {
    setBusy(true);
    try {
      onChange(await work());
    } catch (e) {
      onError(e instanceof Error ? e.message : 'That did not save.');
    } finally {
      setBusy(false);
    }
  };

  return (
    <li className="rounded-lg border border-bakery-200 bg-white p-4 shadow-sm">
      <div className="flex flex-col gap-4 sm:flex-row sm:items-start">
        <div className="flex sm:flex-col gap-1 sm:pt-1">
          <button
            type="button"
            className={iconButton}
            disabled={first}
            onClick={() => onMove(-1)}
            aria-label={`Move ${product.name} up`}
          >
            <Icon d={ARROW_UP} />
          </button>
          <button
            type="button"
            className={iconButton}
            disabled={last}
            onClick={() => onMove(1)}
            aria-label={`Move ${product.name} down`}
          >
            <Icon d={ARROW_DOWN} />
          </button>
        </div>

        <div className="flex-1 min-w-0 space-y-3">
          <div className="grid gap-2 sm:grid-cols-[1fr_12rem]">
            <label className="block">
              <span className="sr-only">Name</span>
              <input
                className={field}
                value={name}
                onChange={(e) => setName(e.target.value)}
                placeholder="Name"
              />
            </label>
            <label className="block">
              <span className="sr-only">Category</span>
              <select className={field} value={category} onChange={(e) => setCategory(e.target.value)}>
                {/* A product can sit in a category nobody defined; don't silently retype it. */}
                {!categories.includes(category) && <option value={category}>{category}</option>}
                {categories.map((c) => (
                  <option key={c} value={c}>
                    {c}
                  </option>
                ))}
              </select>
            </label>
          </div>

          <Photos
            product={product}
            busy={busy}
            onArrange={(keys) => run(() => arrangePhotos(product.id, keys))}
          />

          <div className="flex flex-wrap items-center gap-2">
            <PhotoPicker
              label="Add photos"
              disabled={busy}
              onPick={(files) => run(() => addPhotos(product.id, files))}
            />
            {dirty && (
              <>
                <button
                  type="button"
                  className={primary}
                  disabled={busy}
                  onClick={() => run(() => describeProduct(product.id, name.trim(), category))}
                >
                  <Icon d={CHECK} />
                  Save
                </button>
                <button
                  type="button"
                  className={secondary}
                  disabled={busy}
                  onClick={() => {
                    setName(product.name);
                    setCategory(product.category);
                  }}
                >
                  <Icon d={X} />
                  Cancel
                </button>
              </>
            )}
            <button type="button" className={`${danger} ml-auto`} disabled={busy} onClick={onDelete}>
              <Icon d={TRASH} />
              Delete
            </button>
          </div>
          {busy && <p className="text-sm text-bakery-600">Working…</p>}
        </div>
      </div>
    </li>
  );
};

// --- adding an item ---------------------------------------------------------

const NewItem = ({
  categories,
  onAdded,
  onError,
}: {
  categories: string[];
  onAdded: (product: AdminProduct) => void;
  onError: (message: string) => void;
}) => {
  const [name, setName] = useState('');
  const [category, setCategory] = useState(categories[0] ?? '');
  const [files, setFiles] = useState<File[]>([]);
  const [busy, setBusy] = useState(false);

  // The category list arrives after the first render, so the default has to catch up once.
  useEffect(() => {
    setCategory((c) => (c || categories[0] || ''));
  }, [categories]);

  const submit = async () => {
    setBusy(true);
    try {
      onAdded(await createProduct(name.trim(), category, files));
      setName('');
      setFiles([]);
    } catch (e) {
      onError(e instanceof Error ? e.message : 'That did not save.');
    } finally {
      setBusy(false);
    }
  };

  return (
    <section className="rounded-lg border border-bakery-200 bg-white p-4 shadow-sm">
      <h2 className="font-adbhashitha text-xl text-bakery-800">Add something new</h2>
      <p className="mt-1 text-sm text-bakery-600">New items go to the top of the products page.</p>

      <div className="mt-3 grid gap-2 sm:grid-cols-[1fr_12rem]">
        <input
          className={field}
          value={name}
          onChange={(e) => setName(e.target.value)}
          placeholder="What is it? e.g. Chocolate drip cake"
        />
        <select className={field} value={category} onChange={(e) => setCategory(e.target.value)}>
          {categories.map((c) => (
            <option key={c} value={c}>
              {c}
            </option>
          ))}
        </select>
      </div>

      {files.length > 0 && (
        <div className="mt-3 flex flex-wrap gap-2">
          {files.map((file, i) => (
            <div key={`${file.name}-${i}`} className="relative">
              <img
                src={URL.createObjectURL(file)}
                alt=""
                className="w-20 h-20 rounded-md object-cover border border-bakery-200"
                // Revoking once it's painted keeps the preview from holding the file in memory.
                onLoad={(e) => URL.revokeObjectURL(e.currentTarget.src)}
              />
              <button
                type="button"
                className="absolute -top-2 -right-2 w-6 h-6 rounded-full bg-white border border-bakery-300 text-bakery-700 flex items-center justify-center"
                onClick={() => setFiles(files.filter((_, at) => at !== i))}
                aria-label={`Remove ${file.name}`}
              >
                <Icon d={X} className="w-3 h-3" />
              </button>
            </div>
          ))}
        </div>
      )}

      <div className="mt-3 flex flex-wrap items-center gap-2">
        <PhotoPicker
          label={files.length ? 'Add more photos' : 'Choose photos'}
          disabled={busy}
          onPick={(picked) => setFiles((current) => [...current, ...picked])}
        />
        <button
          type="button"
          className={primary}
          disabled={busy || !name.trim() || !category || files.length === 0}
          onClick={submit}
        >
          <Icon d={PLUS} />
          {busy ? 'Uploading…' : 'Add to the page'}
        </button>
        {busy && <span className="text-sm text-bakery-600">Photos can take a few seconds each.</span>}
      </div>
    </section>
  );
};

// --- categories -------------------------------------------------------------

const Categories = ({
  categories,
  setCategories,
  onError,
  onChanged,
}: {
  categories: AdminCategory[];
  setCategories: (next: AdminCategory[]) => void;
  onError: (message: string) => void;
  onChanged: () => void;
}) => {
  const [fresh, setFresh] = useState('');
  const [editing, setEditing] = useState<number | null>(null);
  const [draft, setDraft] = useState('');
  const [busy, setBusy] = useState(false);

  const guard = async (work: () => Promise<unknown>, optimistic?: AdminCategory[]) => {
    const before = categories;
    if (optimistic) setCategories(optimistic);
    setBusy(true);
    try {
      await work();
      onChanged();
    } catch (e) {
      if (optimistic) setCategories(before);
      onError(e instanceof Error ? e.message : 'That did not save.');
    } finally {
      setBusy(false);
    }
  };

  return (
    <section className="rounded-lg border border-bakery-200 bg-white p-4 shadow-sm">
      <h2 className="font-adbhashitha text-xl text-bakery-800">Categories</h2>
      <p className="mt-1 text-sm text-bakery-600">
        These are the filter buttons on the products page, in this order. Renaming one moves
        everything filed under it too.
      </p>

      <ul className="mt-3 divide-y divide-bakery-100">
        {categories.map((category, i) => (
          <li key={category.id} className="flex items-center gap-2 py-2">
            <div className="flex gap-1">
              <button
                type="button"
                className={iconButton}
                disabled={busy || i === 0}
                onClick={() =>
                  guard(
                    () => reorderCategories(shift(categories, i, -1).map((c) => c.id)),
                    shift(categories, i, -1),
                  )
                }
                aria-label={`Move ${category.name} up`}
              >
                <Icon d={ARROW_UP} />
              </button>
              <button
                type="button"
                className={iconButton}
                disabled={busy || i === categories.length - 1}
                onClick={() =>
                  guard(
                    () => reorderCategories(shift(categories, i, 1).map((c) => c.id)),
                    shift(categories, i, 1),
                  )
                }
                aria-label={`Move ${category.name} down`}
              >
                <Icon d={ARROW_DOWN} />
              </button>
            </div>

            {editing === category.id ? (
              <>
                <input
                  className={field}
                  value={draft}
                  autoFocus
                  onChange={(e) => setDraft(e.target.value)}
                  onKeyDown={(e) => e.key === 'Escape' && setEditing(null)}
                />
                <button
                  type="button"
                  className={primary}
                  disabled={busy || !draft.trim()}
                  onClick={() =>
                    guard(async () => {
                      await renameCategory(category.id, draft.trim());
                      setEditing(null);
                    })
                  }
                >
                  <Icon d={CHECK} />
                  Save
                </button>
                <button type="button" className={secondary} onClick={() => setEditing(null)}>
                  <Icon d={X} />
                </button>
              </>
            ) : (
              <>
                <span className="flex-1 text-bakery-900">{category.name}</span>
                <span className="text-sm text-bakery-500">
                  {category.used} item{category.used === 1 ? '' : 's'}
                </span>
                <button
                  type="button"
                  className={secondary}
                  onClick={() => {
                    setEditing(category.id);
                    setDraft(category.name);
                  }}
                >
                  Rename
                </button>
                <button
                  type="button"
                  className={danger}
                  disabled={busy || category.used > 0}
                  title={category.used > 0 ? 'Move its items somewhere else first' : 'Delete'}
                  onClick={() =>
                    guard(
                      () => deleteCategory(category.id),
                      categories.filter((c) => c.id !== category.id),
                    )
                  }
                >
                  <Icon d={TRASH} />
                </button>
              </>
            )}
          </li>
        ))}
      </ul>

      <div className="mt-3 flex gap-2">
        <input
          className={field}
          value={fresh}
          onChange={(e) => setFresh(e.target.value)}
          placeholder="New category"
          onKeyDown={(e) => e.key === 'Enter' && fresh.trim() && guard(async () => {
            await createCategory(fresh.trim());
            setFresh('');
          })}
        />
        <button
          type="button"
          className={primary}
          disabled={busy || !fresh.trim()}
          onClick={() =>
            guard(async () => {
              await createCategory(fresh.trim());
              setFresh('');
            })
          }
        >
          <Icon d={PLUS} />
          Add
        </button>
      </div>
    </section>
  );
};

// --- the page ---------------------------------------------------------------

const AdminPage = () => {
  const [products, setProducts] = useState<AdminProduct[] | null>(null);
  const [categories, setCategories] = useState<AdminCategory[]>([]);
  const [error, setError] = useState('');

  const load = useCallback(async () => {
    try {
      const [items, cats] = await Promise.all([adminProducts(), adminCategories()]);
      setProducts(items);
      setCategories(cats);
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Could not load the catalogue.');
      setProducts([]);
    }
  }, []);

  useEffect(() => {
    void load();
  }, [load]);

  const names = categories.map((c) => c.name);

  /** Reorder and delete both apply on screen first — the point of this page is that it keeps up. */
  const settle = async (optimistic: AdminProduct[], work: () => Promise<unknown>) => {
    const before = products ?? [];
    setProducts(optimistic);
    try {
      await work();
    } catch (e) {
      setProducts(before);
      setError(e instanceof Error ? e.message : 'That did not save.');
    }
  };

  return (
    <div className="mx-auto max-w-4xl px-4 py-10 sm:px-6">
      <header className="flex flex-wrap items-center justify-between gap-3">
        <div>
          <h1 className="font-lejour text-4xl text-bakery-700">The Vine</h1>
          <p className="text-bakery-600">Everything on the products page lives here.</p>
        </div>
        <div className="flex items-center gap-2">
          <a href="/products" className={secondary}>
            View the page
          </a>
          {/* A real form post: the platform's logout expects one, and it also ends the Authentik session. */}
          <form method="post" action="/logout">
            <button type="submit" className={secondary}>
              Sign out
            </button>
          </form>
        </div>
      </header>

      {error && (
        <div
          role="alert"
          className="mt-6 flex items-start gap-3 rounded-md border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-800"
        >
          <span className="flex-1">{error}</span>
          <button type="button" onClick={() => setError('')} aria-label="Dismiss">
            <Icon d={X} />
          </button>
        </div>
      )}

      {products === null ? (
        <p className="mt-10 text-bakery-600">Loading…</p>
      ) : (
        <div className="mt-6 space-y-6">
          <Categories
            categories={categories}
            setCategories={setCategories}
            onError={setError}
            // A rename rewrites the products filed under it, so the list has to come back fresh.
            onChanged={() => void load()}
          />

          <NewItem
            categories={names}
            onError={setError}
            onAdded={(product) => setProducts([product, ...products])}
          />

          <section>
            <h2 className="font-adbhashitha text-xl text-bakery-800">
              On the page ({products.length})
            </h2>
            <ul className="mt-3 space-y-3">
              {products.map((product, i) => (
                <ProductCard
                  key={product.id}
                  product={product}
                  categories={names}
                  first={i === 0}
                  last={i === products.length - 1}
                  onError={setError}
                  onChange={(updated) =>
                    setProducts(products.map((p) => (p.id === updated.id ? updated : p)))
                  }
                  onMove={(delta) => {
                    const moved = shift(products, i, delta);
                    void settle(moved, () => reorderProducts(moved.map((p) => p.id)));
                  }}
                  onDelete={() => {
                    if (!confirm(`Remove ${product.name} from the products page?`)) return;
                    void settle(
                      products.filter((p) => p.id !== product.id),
                      () => deleteProduct(product.id),
                    );
                  }}
                />
              ))}
            </ul>
            {products.length === 0 && (
              <p className="mt-3 text-bakery-600">Nothing here yet — add something above.</p>
            )}
          </section>
        </div>
      )}
    </div>
  );
};

export default AdminPage;
