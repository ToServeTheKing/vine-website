import { useEffect } from 'react';
import { Route, Routes, useLocation } from 'react-router-dom';
import Header from '@/components/Header';
import Footer from '@/components/Footer';
import HomePage from '@/pages/Home';
import ProductsPage from '@/pages/Products';
import HistoryPage from '@/pages/History';
import ContactPage from '@/pages/Contact';
import NotFoundPage from '@/pages/NotFound';

/**
 * Client-side navigation keeps the previous scroll position, which lands you halfway down a page you
 * just opened. Anchors like /#visit still need to work, so only reset when there isn't one.
 */
const ScrollToTop = () => {
  const { pathname, hash } = useLocation();
  useEffect(() => {
    if (!hash) window.scrollTo(0, 0);
  }, [pathname, hash]);
  return null;
};

const App = () => (
  <div className="min-h-screen bg-bakery-50 flex flex-col">
    <ScrollToTop />
    <Header />
    <main className="flex-grow">
      <Routes>
        <Route path="/" element={<HomePage />} />
        <Route path="/products" element={<ProductsPage />} />
        <Route path="/history" element={<HistoryPage />} />
        <Route path="/contact" element={<ContactPage />} />
        <Route path="*" element={<NotFoundPage />} />
      </Routes>
    </main>
    <Footer />
  </div>
);

export default App;
