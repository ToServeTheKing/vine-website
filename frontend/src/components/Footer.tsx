import { Link } from 'react-router-dom';
import Logo from './Logo';

const Footer = () => {
  return (
    <footer className="bg-bakery-900 text-bakery-100">
      <div className="container mx-auto px-4 py-14">
        <div className="grid grid-cols-1 md:grid-cols-4 gap-10">
          {/* Logo */}
          <div className="col-span-1 md:col-span-2 flex items-start">
            <Logo className="text-bakery-50" />
          </div>

          {/* Navigation Links */}
          <div>
            <h3 className="font-adbhashitha text-sm uppercase tracking-[0.18em] text-bakery-300 mb-4">Navigation</h3>
            <ul className="space-y-2">
              <li>
                <Link to="/products" className="hover:text-white transition">Our Products</Link>
              </li>
              <li>
                <Link to="/history" className="hover:text-white transition">Our Story</Link>
              </li>
              <li>
                <Link to="/contact" className="hover:text-white transition">Contact</Link>
              </li>
            </ul>
          </div>

          {/* Contact Info */}
          <div>
            <h3 className="font-adbhashitha text-sm uppercase tracking-[0.18em] text-bakery-300 mb-4">Visit</h3>
            <address className="not-italic space-y-2">
              <p>215 E Main Street<br />Princeville, IL 61559</p>
              <p><a href="tel:+13097010660" className="hover:text-white transition">(309) 701-0660</a></p>
              <p className="break-words">
                <a href="mailto:contact@itsthevine.com" className="hover:text-white transition">contact@itsthevine.com</a>
              </p>
            </address>
          </div>
        </div>

        {/* Bottom Bar */}
        <div className="mt-12 text-center text-sm text-bakery-300">
          <p>© {new Date().getFullYear()} The Vine Coffeehouse + Bakery</p>
        </div>
      </div>
    </footer>
  );
};

export default Footer;
