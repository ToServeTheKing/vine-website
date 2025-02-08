import Link from 'next/link';
import Image from 'next/image';
import Logo from './Logo';

const Footer = () => {
  return (
    <footer className="bg-bakery-800 text-white">
      <div className="container mx-auto px-4 py-12">
        <div className="grid grid-cols-1 md:grid-cols-4 gap-8">
          {/* Logo and Description */}
          <div className="col-span-1 md:col-span-2 flex items-center">
            <Logo logoColor="#f2f3f2"/>
          </div>

          {/* Navigation Links */}
          <div>
            <h3 className="font-adbhashitha text-xl mb-4">Navigation</h3>
            <ul className="space-y-2">
              <li>
                <Link href="/products" className="text-bakery-200 hover:text-white transition">
                   Our Products
                </Link>
              </li>
              <li>
                <Link href="/history" className="text-bakery-200 hover:text-white transition">
                  History
                </Link>
              </li>
              <li>
                <Link href="/contact" className="text-bakery-200 hover:text-white transition">
                  Contact
                </Link>
              </li>
            </ul>
          </div>

          {/* Contact Info */}
          <div>
            <h3 className="font-adbhashitha text-xl mb-4">Contact</h3>
            <ul className="space-y-2 text-bakery-200">
              <li>123 Rue du Pain</li>
              <li>75001 Paris</li>
              <li>01 23 45 67 89</li>
              <li>contact@boulangerie-artisanale.fr</li>
            </ul>
          </div>
        </div>

        {/* Bottom Bar */}
        <div className="border-t border-bakery-700 mt-8 pt-8 text-center text-bakery-300">
          <p>© 2025 The Vine Cofeehouse & Bakery</p>
        </div>
      </div>
    </footer>
  );
};

export default Footer; 