import Link from 'next/link';
import Image from 'next/image';

const Header = () => {
  const navItems = [
    { label: 'Accueil', href: '/' },
    { label: 'Nos Produits', href: '/produits' },
    { label: 'Notre Histoire', href: '/notre-histoire' },
    { label: 'Commander', href: '/commander' },
    { label: 'Contact', href: '/contact' },
  ];

  return (
    <header className="bg-white shadow-sm">
      <div className="container mx-auto px-4">
        <div className="flex items-center justify-between h-20">
          {/* Logo */}
          <Link href="/" className="flex items-center">
            <Image
              src="/images/ressources/logo.png"
              alt="Boulangerie Artisanale"
              width={50}
              height={50}
              className="h-12 w-auto"
            />
            <span className="ml-3 font-serif text-xl text-bakery-800">
              Boulangerie Artisanale
            </span>
          </Link>

          {/* Navigation */}
          <nav className="hidden md:flex space-x-8">
            {navItems.map((item) => (
              <Link
                key={item.href}
                href={item.href}
                className="text-bakery-700 hover:text-bakery-900 transition"
              >
                {item.label}
              </Link>
            ))}
          </nav>

          {/* Mobile menu button */}
          <button className="md:hidden p-2">
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
      </div>
    </header>
  );
};

export default Header; 