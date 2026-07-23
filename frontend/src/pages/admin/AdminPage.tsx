import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import {
  deleteProduct, fetchAdminProducts, fetchEnquiries,
  type AdminEnquiry, type AdminProduct,
} from '@/lib/api';
import { useAuth, signIn } from '@/lib/auth';

type Tab = 'products' | 'enquiries';

export default function AdminPage() {
  const { me, loading } = useAuth();
  const [tab, setTab] = useState<Tab>('products');
  const [products, setProducts] = useState<AdminProduct[]>([]);
  const [enquiries, setEnquiries] = useState<AdminEnquiry[]>([]);
  const [error, setError] = useState('');

  const load = () => {
    // Only ask for admin data once we know there is a session; otherwise every anonymous visitor
    // who guesses this URL gets bounced to the identity provider.
    if (!me.admin) return;
    fetchAdminProducts().then(setProducts).catch((e) => setError(String(e.message ?? e)));
    fetchEnquiries().then(setEnquiries).catch((e) => setError(String(e.message ?? e)));
  };

  useEffect(load, [me.admin]);

  if (loading) {
    return <p className="container mx-auto px-4 py-20 text-bakery-700">Loading…</p>;
  }

  if (!me.admin) {
    return (
      <div className="container mx-auto px-4 py-20 text-center">
        <h1 className="font-adbhashitha text-3xl text-bakery-900 mb-6">Staff only</h1>
        <button
          onClick={signIn}
          className="px-8 py-3.5 bg-bakery-600 text-white rounded-full tracking-wide hover:bg-bakery-700 transition-colors"
        >
          Sign in
        </button>
      </div>
    );
  }

  async function remove(p: AdminProduct) {
    if (!window.confirm(`Remove “${p.name}” from the menu? The photos stay in storage.`)) return;
    try {
      await deleteProduct(p.id);
      setProducts((list) => list.filter((x) => x.id !== p.id));
    } catch (e) {
      setError(String((e as Error).message));
    }
  }

  return (
    <div className="container mx-auto px-4 py-12">
      <div className="flex flex-wrap items-baseline justify-between gap-4 mb-8">
        <h1 className="font-adbhashitha text-4xl text-bakery-900">Manage</h1>
        <span className="text-sm text-bakery-600">signed in as {me.name}</span>
      </div>

      {error && (
        <p role="alert" className="mb-6 rounded-xl bg-red-50 px-4 py-3 text-red-800">{error}</p>
      )}

      <div className="flex gap-3 mb-8">
        {(['products', 'enquiries'] as Tab[]).map((t) => (
          <button
            key={t}
            onClick={() => setTab(t)}
            className={`px-6 py-2 rounded-full border text-sm uppercase tracking-[0.12em] transition-colors ${
              tab === t
                ? 'bg-bakery-600 text-white border-bakery-600'
                : 'bg-white border-bakery-300 text-bakery-700 hover:bg-bakery-100'
            }`}
          >
            {t === 'products' ? `Products (${products.length})` : `Enquiries (${enquiries.length})`}
          </button>
        ))}
      </div>

      {tab === 'products' ? (
        <>
          <Link
            to="/admin/products/new"
            className="inline-block mb-6 px-6 py-2.5 bg-bakery-600 text-white rounded-full text-sm tracking-wide hover:bg-bakery-700 transition-colors"
          >
            Add a product
          </Link>
          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-6">
            {products.map((p) => (
              <div key={p.id} className="bg-white rounded-3xl overflow-hidden shadow-xs">
                {p.imageUrls[0] && (
                  <img src={p.imageUrls[0]} alt="" className="w-full aspect-square object-cover" />
                )}
                <div className="p-5">
                  <div className="font-adbhashitha text-lg text-bakery-900">{p.name}</div>
                  <div className="text-xs uppercase tracking-[0.15em] text-bakery-600 mb-4">
                    {p.category} · {p.imageKeys.length} photo{p.imageKeys.length === 1 ? '' : 's'}
                  </div>
                  <div className="flex gap-3 text-sm">
                    <Link
                      to={`/admin/products/${p.id}`}
                      className="underline underline-offset-4 text-bakery-700 hover:text-bakery-900"
                    >
                      Edit
                    </Link>
                    <button
                      onClick={() => remove(p)}
                      className="underline underline-offset-4 text-red-700 hover:text-red-900"
                    >
                      Remove
                    </button>
                  </div>
                </div>
              </div>
            ))}
            {products.length === 0 && <p className="text-bakery-700">Nothing on the menu yet.</p>}
          </div>
        </>
      ) : (
        <div className="flex flex-col gap-4">
          {enquiries.map((e) => (
            <div key={e.id} className="bg-white rounded-3xl p-6 shadow-xs">
              <div className="flex flex-wrap items-baseline justify-between gap-2">
                <div className="font-medium text-bakery-900">
                  {e.name} <a href={`mailto:${e.email}`} className="font-normal text-bakery-600 underline underline-offset-4">{e.email}</a>
                </div>
                <div className="text-sm text-bakery-600">
                  {new Date(e.receivedAt).toLocaleString()}
                  {!e.delivered && (
                    // The enquiry was saved but the relay refused it — nobody got an email.
                    <span className="ml-2 rounded-full bg-amber-100 px-2 py-0.5 text-xs text-amber-800">
                      not emailed
                    </span>
                  )}
                </div>
              </div>
              <p className="mt-3 whitespace-pre-wrap text-bakery-800">{e.message}</p>
            </div>
          ))}
          {enquiries.length === 0 && <p className="text-bakery-700">No enquiries yet.</p>}
        </div>
      )}
    </div>
  );
}
