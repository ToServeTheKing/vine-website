import { Link } from 'react-router-dom';
import Logo from '@/components/Logo';
import { photo } from '@/lib/assets';

const HomePage = () => {
  return (
    <div>
      {/* Hero — centred lockup on a sage wash. */}
      <section className="relative flex items-center min-h-[78svh] py-20 md:py-28 bg-bakery-900 text-white overflow-hidden">
        {/* Softened on purpose: the storefront's own painted sign sits right behind
            the logo, so a sharp photo makes you read the name twice. scale-110 hides
            the blur's feathered edges. */}
        <img
          src={photo('gallery/Outside.webp')}
          alt=""
          fetchPriority="high"
          className="absolute inset-0 w-full h-full object-cover object-bottom blur-[3px] scale-110"
        />
        {/* Sage wash rather than a neutral black scrim — the tint is the identity. */}
        <div className="absolute inset-0 bg-bakery-900/80" />
        <div className="absolute inset-0 bg-gradient-to-t from-bakery-900 via-bakery-800/60 to-bakery-900/80" />

        {/* w-full/min-w-0 keep this flex item from sizing to its max-content
            width and blowing out the page on narrow screens. */}
        <div className="relative container mx-auto px-4 w-full min-w-0">
          <div className="max-w-3xl mx-auto text-center">
            <Logo size="lg" linked={false} className="text-bakery-50 mb-10 justify-center" />
            <p className="text-lg sm:text-xl text-bakery-100 mb-10 text-balance leading-relaxed">
              A coffeehouse and bakery in downtown Princeville, Illinois.
            </p>
            <div className="flex flex-col sm:flex-row gap-3 justify-center">
              <Link
                to="/products"
                className="bg-bakery-50 hover:bg-white text-bakery-900 px-8 py-3.5 rounded-full font-medium tracking-wide inline-block transition shadow-lg"
              >
                See the menu
              </Link>
              <a
                href="#visit"
                className="border border-bakery-200/60 hover:bg-bakery-50/10 text-bakery-50 px-8 py-3.5 rounded-full font-medium tracking-wide inline-block transition"
              >
                Visit us
              </a>
            </div>
          </div>
        </div>
      </section>

      {/* Our Specialties */}
      <section className="py-16 md:py-24 bg-bakery-50">
        <div className="container mx-auto px-4">
          <h2 className="font-adbhashitha text-3xl md:text-4xl text-center text-bakery-900 mb-12">What people come in for</h2>
          <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 gap-6 md:gap-8">
            {['Cinnamon Rolls', 'Sugar Cookies', 'Cakes'].map((item) => (
              <div
                key={item}
                className="group text-center bg-white rounded-3xl overflow-hidden shadow-sm hover:shadow-lg hover:-translate-y-1 transition duration-300"
              >
                <div className="overflow-hidden">
                  <img
                    src={photo(`gallery/${item}.webp`)}
                    alt={item}
                    width={600}
                    height={400}
                    loading="lazy"
                    className="w-full h-56 object-cover transition-transform duration-500 group-hover:scale-105"
                  />
                </div>
                <h3 className="font-adbhashitha text-xl md:text-2xl text-bakery-800 py-6 tracking-wide">{item}</h3>
              </div>
            ))}
          </div>
        </div>
      </section>

      {/* About Us */}
      <section className="py-16 md:py-24 bg-bakery-800 text-white">
        <div className="container mx-auto px-4">
          <div className="max-w-3xl mx-auto text-center">
            <h2 className="font-adbhashitha text-3xl md:text-4xl mb-8" style={{ letterSpacing: '0.01em' }}>Our story</h2>
            <p className="text-base md:text-lg text-bakery-100 mb-8 leading-relaxed">
              Morissa Bennett opened The Vine in 2024. We bake in our own kitchen on Main Street:
              cinnamon rolls, cookies, custom cakes, sandwiches, paninis, and coffee.
            </p>
            <Link to="/history" className="text-white hover:text-bakery-200 font-semibold underline underline-offset-4">
              Read our story
            </Link>
          </div>
        </div>
      </section>

      {/* Hours & Contact */}
      <section id="visit" className="py-16 md:py-24 bg-bakery-50 scroll-mt-24">
        <div className="container mx-auto px-4">
          <h2 className="font-adbhashitha text-3xl md:text-4xl text-center text-bakery-900 mb-12">Visit us</h2>
          <div className="grid grid-cols-1 md:grid-cols-2 gap-8 md:gap-12 max-w-4xl mx-auto">
            <div className="bg-white rounded-3xl p-6 md:p-8">
              <h2 className="font-adbhashitha text-2xl md:text-3xl text-bakery-900 mb-6">Our hours</h2>
              <ul className="space-y-3 text-bakery-800">
                <li className="flex justify-between gap-4">
                  <span>Tuesday – Friday</span>
                  <span className="font-medium whitespace-nowrap">7:00am – 2:00pm</span>
                </li>
                <li className="flex justify-between gap-4">
                  <span>Saturday</span>
                  <span className="font-medium whitespace-nowrap">7:00am – 12:00pm</span>
                </li>
                <li className="flex justify-between gap-4">
                  <span>Sunday – Monday</span>
                  <span className="font-medium">Closed</span>
                </li>
              </ul>
            </div>
            <div className="bg-white rounded-3xl p-6 md:p-8">
              <h2 className="font-adbhashitha text-2xl md:text-3xl text-bakery-900 mb-6">Find us</h2>
              <address className="not-italic space-y-3 text-bakery-800">
                <p>215 E Main Street<br />Princeville, IL 61559</p>
                <p>
                  <a href="tel:+13097010660" className="hover:text-bakery-600 underline underline-offset-4">(309) 701-0660</a>
                </p>
                <p className="break-words">
                  <a href="mailto:contact@itsthevine.com" className="hover:text-bakery-600 underline underline-offset-4">contact@itsthevine.com</a>
                </p>
              </address>
            </div>
          </div>
        </div>
      </section>
    </div>
  );
};

export default HomePage;
