import { useEffect } from 'react';
import { Outlet, Route, Routes, useLocation } from 'react-router-dom';
import Header from '@/components/Header';
import Footer from '@/components/Footer';
import HomePage from '@/pages/Home';
import ProductsPage from '@/pages/Products';
import HistoryPage from '@/pages/History';
import ContactPage from '@/pages/Contact';
import NotFoundPage from '@/pages/NotFound';
import AdminPage from '@/pages/Admin';

/**
 * Client-side navigation keeps the previous scroll position, which lands you halfway down a page you
 * just opened. Anchors like /#visit still need to work, so only reset when there isn't one.
 */
const ScrollToTop = () => {
  const { pathname, hash } = useLocation();
  useEffect(() => {
    // 'instant' overrides the page's scroll-behavior:smooth, which is meant for the #visit
    // anchor, not for landing on a new page.
    if (!hash) window.scrollTo({ top: 0, behavior: 'instant' });
  }, [pathname, hash]);
  return null;
};

/** The shop front: the nav, the footer, and the pages a customer sees. */
const PublicLayout = () => (
  <div className="min-h-screen bg-bakery-50 flex flex-col">
    <Header />
    <main className="flex-grow">
      <Outlet />
    </main>
    <Footer />
  </div>
);

/**
 * The admin sits outside the public chrome deliberately. It isn't a page you'd browse to — the nav
 * would offer a signed-in editor links away from unsaved work, and the opening hours in the footer
 * are noise on a screen whose whole job is the catalogue.
 */
const AdminLayout = () => (
  <div className="min-h-screen bg-bakery-50">
    <Outlet />
  </div>
);

const App = () => (
  <>
    <ScrollToTop />
    <Routes>
      <Route element={<PublicLayout />}>
        <Route path="/" element={<HomePage />} />
        <Route path="/products" element={<ProductsPage />} />
        <Route path="/history" element={<HistoryPage />} />
        <Route path="/contact" element={<ContactPage />} />
        <Route path="*" element={<NotFoundPage />} />
      </Route>
      <Route element={<AdminLayout />}>
        <Route path="/admin" element={<AdminPage />} />
      </Route>
    </Routes>
  </>
);

export default App;
