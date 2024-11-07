import Link from 'next/link';
import Image from 'next/image';

const Footer = () => {
  return (
    <footer className="bg-bakery-800 text-white">
      <div className="container mx-auto px-4 py-12">
        <div className="grid grid-cols-1 md:grid-cols-4 gap-8">
          {/* Logo and Description */}
          <div className="col-span-1 md:col-span-2 flex items-center">
            <Image
              src="/images/ressources/logo.png"
              alt="Au Blé d'Or - Boulangerie Artisanale"
              width={60}
              height={60}
              className="mb-4 mr-4"
            />
            <div>
              <h2 className="text-bakery-400 font-semibold mb-2 text-lg">Au Blé d&apos;Or</h2>
              <p className="text-bakery-200 max-w-md">
                Artisans boulangers depuis 1987, nous perpétuons la tradition
                du bon pain et des pâtisseries artisanales.
              </p>
            </div>
          </div>

          {/* Navigation Links */}
          <div>
            <h3 className="font-serif text-xl mb-4">Navigation</h3>
            <ul className="space-y-2">
              <li>
                <Link href="/produits" className="text-bakery-200 hover:text-white transition">
                  Nos Produits
                </Link>
              </li>
              <li>
                <Link href="/notre-histoire" className="text-bakery-200 hover:text-white transition">
                  Notre Histoire
                </Link>
              </li>
              <li>
                <Link href="/commander" className="text-bakery-200 hover:text-white transition">
                  Commander
                </Link>
              </li>
            </ul>
          </div>

          {/* Contact Info */}
          <div>
            <h3 className="font-serif text-xl mb-4">Contact</h3>
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
          <p>© 2024 Au Blé d&apos;Or - Boulangerie Artisanale. Tous droits réservés.</p>
        </div>
      </div>
    </footer>
  );
};

export default Footer; 