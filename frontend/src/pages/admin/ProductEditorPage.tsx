import { useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import {
  createProduct, fetchAdminProducts, updateProduct, uploadPhoto,
  type AdminProduct,
} from '@/lib/api';
import { useAuth, signIn } from '@/lib/auth';

export default function ProductEditorPage() {
  const { id } = useParams();
  const editing = id !== undefined;
  const navigate = useNavigate();
  const { me, loading } = useAuth();

  const [name, setName] = useState('');
  const [category, setCategory] = useState('');
  const [position, setPosition] = useState<number | null>(null);
  const [images, setImages] = useState<{ key: string; url: string }[]>([]);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');

  useEffect(() => {
    if (!editing || !me.admin) return;
    fetchAdminProducts()
      .then((all) => {
        const p = all.find((x: AdminProduct) => String(x.id) === id);
        if (!p) { setError('That product no longer exists.'); return; }
        setName(p.name);
        setCategory(p.category);
        setPosition(p.position);
        setImages(p.imageKeys.map((k, i) => ({ key: k, url: p.imageUrls[i] })));
      })
      .catch((e) => setError(String(e.message ?? e)));
  }, [editing, id, me.admin]);

  if (loading) return <p className="container mx-auto px-4 py-20 text-bakery-700">Loading…</p>;
  if (!me.admin) {
    return (
      <div className="container mx-auto px-4 py-20 text-center">
        <button onClick={signIn} className="px-8 py-3.5 bg-bakery-600 text-white rounded-full">Sign in</button>
      </div>
    );
  }

  async function onFiles(files: FileList | null) {
    if (!files?.length) return;
    setBusy(true); setError('');
    try {
      // Sequentially, so the order the photos are chosen is the order they appear on the card.
      for (const file of Array.from(files)) {
        const t = await uploadPhoto(file);
        setImages((list) => [...list, { key: t.key, url: t.publicUrl }]);
      }
    } catch (e) {
      setError(String((e as Error).message));
    } finally {
      setBusy(false);
    }
  }

  async function save(e: React.FormEvent) {
    e.preventDefault();
    setBusy(true); setError('');
    try {
      const form = { name, category, position, imageKeys: images.map((i) => i.key) };
      if (editing) await updateProduct(Number(id), form);
      else await createProduct(form);
      navigate('/admin');
    } catch (err) {
      setError(String((err as Error).message));
      setBusy(false);
    }
  }

  const move = (from: number, to: number) => {
    if (to < 0 || to >= images.length) return;
    setImages((list) => {
      const next = [...list];
      const [it] = next.splice(from, 1);
      next.splice(to, 0, it);
      return next;
    });
  };

  return (
    <div className="container mx-auto px-4 py-12 max-w-2xl">
      <h1 className="font-adbhashitha text-4xl text-bakery-900 mb-8">
        {editing ? 'Edit product' : 'Add a product'}
      </h1>

      {error && <p role="alert" className="mb-6 rounded-xl bg-red-50 px-4 py-3 text-red-800">{error}</p>}

      <form onSubmit={save} className="bg-white rounded-3xl p-8 shadow-xs">
        <label className="block text-sm font-medium text-bakery-800 mb-2" htmlFor="name">Name</label>
        <input
          id="name" value={name} onChange={(e) => setName(e.target.value)} required
          className="mb-5 w-full px-4 py-2.5 bg-bakery-50 border border-bakery-200 rounded-xl focus:outline-none focus:ring-2 focus:ring-bakery-500"
        />

        <label className="block text-sm font-medium text-bakery-800 mb-2" htmlFor="category">Category</label>
        <input
          id="category" value={category} onChange={(e) => setCategory(e.target.value)} required
          placeholder="Cakes, Cookies, Rolls, Pie, Brownies, Pastries"
          className="mb-1 w-full px-4 py-2.5 bg-bakery-50 border border-bakery-200 rounded-xl focus:outline-none focus:ring-2 focus:ring-bakery-500"
        />
        <p className="mb-5 text-xs text-bakery-600">
          A new category appears as its own filter button, after the ones the site already knows.
        </p>

        <label className="block text-sm font-medium text-bakery-800 mb-2" htmlFor="photos">Photos</label>
        <input
          id="photos" type="file" accept="image/*" multiple disabled={busy}
          onChange={(e) => onFiles(e.target.files)}
          className="mb-4 block w-full text-sm text-bakery-700 file:mr-4 file:rounded-full file:border-0 file:bg-bakery-600 file:px-5 file:py-2 file:text-white"
        />

        {images.length > 0 && (
          <div className="mb-6 grid grid-cols-3 gap-3">
            {images.map((img, i) => (
              <div key={img.key} className="relative">
                <img src={img.url} alt="" className="aspect-square w-full rounded-xl object-cover" />
                {i === 0 && (
                  <span className="absolute top-1 left-1 rounded-full bg-bakery-900/70 px-2 py-0.5 text-[10px] uppercase tracking-wide text-white">
                    card
                  </span>
                )}
                <div className="mt-1 flex justify-between text-xs text-bakery-700">
                  <button type="button" onClick={() => move(i, i - 1)} aria-label="Move earlier">←</button>
                  <button
                    type="button"
                    onClick={() => setImages((l) => l.filter((_, j) => j !== i))}
                    className="text-red-700"
                  >
                    remove
                  </button>
                  <button type="button" onClick={() => move(i, i + 1)} aria-label="Move later">→</button>
                </div>
              </div>
            ))}
          </div>
        )}

        <div className="flex items-center gap-4">
          <button
            type="submit"
            disabled={busy || images.length === 0}
            className="px-8 py-3 bg-bakery-600 text-white rounded-full tracking-wide hover:bg-bakery-700 transition-colors disabled:opacity-60"
          >
            {busy ? 'Working…' : 'Save'}
          </button>
          <button type="button" onClick={() => navigate('/admin')} className="text-bakery-700 underline underline-offset-4">
            Cancel
          </button>
          {images.length === 0 && <span className="text-sm text-bakery-600">Add at least one photo.</span>}
        </div>
      </form>
    </div>
  );
}
