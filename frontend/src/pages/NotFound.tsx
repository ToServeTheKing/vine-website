import { Link } from 'react-router-dom';

const NotFoundPage = () => (
  <div className="bg-bakery-50">
    <div className="container mx-auto px-4 py-24 md:py-32 text-center">
      <h1 className="font-adbhashitha text-4xl md:text-5xl text-bakery-900 mb-6">
        We could not find that page
      </h1>
      <p className="text-bakery-800 mb-10">
        It may have moved. The menu, our story, and how to reach us are all still here.
      </p>
      <div className="flex flex-col sm:flex-row gap-3 justify-center">
        <Link
          to="/"
          className="bg-bakery-600 hover:bg-bakery-700 text-white px-8 py-3.5 rounded-full font-medium tracking-wide inline-block transition"
        >
          Back home
        </Link>
        <Link
          to="/products"
          className="border border-bakery-300 hover:bg-bakery-100 text-bakery-700 px-8 py-3.5 rounded-full font-medium tracking-wide inline-block transition"
        >
          See the menu
        </Link>
      </div>
    </div>
  </div>
);

export default NotFoundPage;
