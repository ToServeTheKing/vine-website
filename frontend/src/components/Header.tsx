import { useEffect, useState } from 'react';
import { motion, AnimatePresence } from 'motion/react';
import { Link, useLocation } from 'react-router-dom';
import Logo from './Logo';

const navItems = [
  { label: 'Our Products', href: '/products' },
  { label: 'Our Story', href: '/history' },
  { label: 'Contact', href: '/contact' },
];

const Header = () => {
  const [isMobileMenuOpen, setIsMobileMenuOpen] = useState(false);
  const { pathname } = useLocation();

  // Close on navigation — without this the panel stays up over the page you just opened.
  useEffect(() => setIsMobileMenuOpen(false), [pathname]);

  // Escape closes it, and the page behind it doesn't scroll while it's up.
  useEffect(() => {
    if (!isMobileMenuOpen) return;
    const onKey = (e: KeyboardEvent) => { if (e.key === 'Escape') setIsMobileMenuOpen(false); };
    const previousOverflow = document.body.style.overflow;
    document.body.style.overflow = 'hidden';
    window.addEventListener('keydown', onKey);
    return () => {
      document.body.style.overflow = previousOverflow;
      window.removeEventListener('keydown', onKey);
    };
  }, [isMobileMenuOpen]);

  return (
    <>
      <header className="sticky top-0 z-40 bg-bakery-50/90 backdrop-blur border-b border-bakery-200">
        <div className="container mx-auto px-4">
          <div className="flex items-center justify-between gap-2 h-20 md:h-24">
            {/* Logo */}
            <Logo className="text-bakery-700" />
            {/* Desktop Navigation */}
            <nav className="hidden md:flex items-center gap-8">
              {navItems.map((item) => (
                <Link
                  key={item.href}
                  to={item.href}
                  className="text-sm uppercase tracking-[0.15em] text-bakery-700 hover:text-bakery-900 transition"
                >
                  {item.label}
                </Link>
              ))}
            </nav>

            {/* Mobile menu button — the same control opens and closes, so the bar never
                disappears out from under your thumb. */}
            <button
              className="md:hidden p-2 shrink-0"
              onClick={() => setIsMobileMenuOpen((open) => !open)}
              aria-label={isMobileMenuOpen ? 'Close menu' : 'Open menu'}
              aria-expanded={isMobileMenuOpen}
            >
              <svg
                className="h-6 w-6 text-bakery-700"
                fill="none"
                strokeLinecap="round"
                strokeLinejoin="round"
                strokeWidth="2"
                viewBox="0 0 24 24"
                stroke="currentColor"
                aria-hidden="true"
              >
                {isMobileMenuOpen ? <path d="M6 18L18 6M6 6l12 12" /> : <path d="M4 6h16M4 12h16M4 18h16" />}
              </svg>
            </button>
          </div>
        </div>
      </header>

      {/* Mobile navigation. Three things here are load-bearing:

          It lives OUTSIDE <header>. The header carries `backdrop-blur`, and a backdrop-filter
          makes an element a containing block for fixed-position descendants — so a `fixed` panel
          nested inside it resolves against the 80px header box, not the viewport, and gets
          clipped to a sliver.

          It starts BELOW the bar (`top-20`) instead of covering it, so the logo and the toggle
          stay put and the panel needs no second copy of either. One logo, one position, every
          breakpoint.

          It must UNMOUNT when closed: a panel parked off-screen still extends the scrollable
          area, which is what used to let you scroll sideways and find the menu. */}
      <AnimatePresence>
        {isMobileMenuOpen && (
          <motion.div
            initial={{ opacity: 0, y: -8 }}
            animate={{ opacity: 1, y: 0 }}
            exit={{ opacity: 0, y: -8 }}
            transition={{ duration: 0.2, ease: 'easeOut' }}
            className="md:hidden fixed inset-x-0 top-20 bottom-0 z-30 bg-bakery-50"
          >
            <nav className="container mx-auto px-4 flex flex-col">
              {navItems.map((item) => (
                <Link
                  key={item.href}
                  to={item.href}
                  className="text-bakery-800 hover:text-bakery-600 transition py-4 text-lg border-b border-bakery-100"
                  onClick={() => setIsMobileMenuOpen(false)}
                >
                  {item.label}
                </Link>
              ))}
            </nav>
          </motion.div>
        )}
      </AnimatePresence>
    </>
  );
};

export default Header;
