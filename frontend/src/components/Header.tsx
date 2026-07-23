import { useState } from 'react';
import { motion, AnimatePresence } from 'motion/react';
import { Link } from 'react-router-dom';
import Logo from './Logo';

const Header = () => {
  const [isMobileMenuOpen, setIsMobileMenuOpen] = useState(false);

  const navItems = [
    { label: 'Our Products', href: '/products' },
    { label: 'Our Story', href: '/history' },
    { label: 'Contact', href: '/contact' },
  ];

  return (
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

          {/* Mobile menu button */}
          <button
            className="md:hidden p-2 shrink-0"
            onClick={() => setIsMobileMenuOpen(!isMobileMenuOpen)}
            aria-label="Toggle menu"
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
            >
              <path d="M4 6h16M4 12h16M4 18h16"></path>
            </svg>
          </button>
        </div>

        {/* Mobile Navigation — must UNMOUNT when closed. A panel parked off-screen
            at translate-x-full still extends the scrollable area, which is what let
            you scroll sideways and find the menu. */}
        <AnimatePresence>
          {isMobileMenuOpen && (
            <motion.div
              initial={{ x: '100%' }}
              animate={{ x: 0 }}
              exit={{ x: '100%' }}
              transition={{ type: 'tween', duration: 0.25, ease: 'easeOut' }}
              className="md:hidden fixed inset-0 bg-bakery-50 z-50"
            >
              <div className="p-4">
                <div className="flex justify-between items-center gap-2 mb-8 h-16">
                  <Logo className="text-bakery-800" />
                  <button
                    onClick={() => setIsMobileMenuOpen(false)}
                    className="p-2 shrink-0"
                    aria-label="Close menu"
                  >
                    <svg
                      className="h-6 w-6 text-bakery-700"
                      fill="none"
                      viewBox="0 0 24 24"
                      stroke="currentColor"
                    >
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
                    </svg>
                  </button>
                </div>
                <nav className="flex flex-col">
                  {navItems.map((item) => (
                    <Link
                      key={item.href}
                      to={item.href}
                      className="text-bakery-800 hover:text-bakery-600 transition py-4 text-lg"
                      onClick={() => setIsMobileMenuOpen(false)}
                    >
                      {item.label}
                    </Link>
                  ))}
                </nav>
              </div>
            </motion.div>
          )}
        </AnimatePresence>
      </div>
    </header>
  );
};

export default Header;
