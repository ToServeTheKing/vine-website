import { useEffect, useState } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { fetchCategories, fetchProducts, type Product } from '@/lib/api';
import ProductGallery from '@/components/ProductGallery';

const ProductsPage = () => {
  const [categories, setCategories] = useState<string[]>(['All']);
  const [selectedCategory, setSelectedCategory] = useState('All');
  const [products, setProducts] = useState<Product[]>([]);
  const [failed, setFailed] = useState(false);

  useEffect(() => {
    fetchCategories().then(setCategories).catch(() => setFailed(true));
  }, []);

  // The filter is applied by the API, not in the browser — one source of truth for what's in a
  // category. `ignore` drops a slow response that lost the race to a newer click.
  useEffect(() => {
    let ignore = false;
    setFailed(false);
    fetchProducts(selectedCategory)
      .then((p) => { if (!ignore) setProducts(p); })
      .catch(() => { if (!ignore) setFailed(true); });
    return () => { ignore = true; };
  }, [selectedCategory]);

  return (
    <div className="min-h-screen bg-bakery-50">
      {/* Page header */}
      <header className="container mx-auto px-4 pt-14 pb-10 md:pt-20 md:pb-12 text-center">
        <h1 className="font-adbhashitha text-4xl md:text-5xl text-bakery-900">
          Our products
        </h1>
      </header>

      {/* Products Section */}
      <div className="container mx-auto px-4 pb-16">
        {/* Categories */}
        <div className="flex flex-wrap justify-center gap-4 mb-12">
          {categories.map((category) => (
            <motion.button
              key={category}
              whileHover={{ scale: 1.05 }}
              whileTap={{ scale: 0.95 }}
              onClick={() => setSelectedCategory(category)}
              className={`px-6 py-2 rounded-full border text-sm uppercase tracking-[0.12em] transition-colors ${
                selectedCategory === category
                  ? 'bg-bakery-600 text-white border-bakery-600'
                  : 'bg-white border-bakery-300 text-bakery-700 hover:bg-bakery-100'
              }`}
            >
              {category}
            </motion.button>
          ))}
        </div>

        {failed && (
          <p role="alert" className="text-center text-bakery-800">
            We could not load the menu just now. Please refresh, or call us on{' '}
            <a href="tel:+13097010660" className="underline underline-offset-4">(309) 701-0660</a>.
          </p>
        )}

        {/* Products Grid */}
        <motion.div
          layout
          className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-8"
        >
          <AnimatePresence>
            {products.map((product) => (
              <motion.div
                key={product.id}
                layout
                initial={{ opacity: 0 }}
                animate={{ opacity: 1 }}
                exit={{ opacity: 0 }}
                className="group bg-white rounded-3xl overflow-hidden shadow-sm hover:shadow-lg hover:-translate-y-1 transition duration-300"
              >
                {/* Image Container */}
                <div className="relative w-full overflow-hidden">
                  <ProductGallery images={product.images} alt={product.name} />
                </div>

                {/* Content */}
                <div className="p-6 text-center">
                  <h3 className="font-adbhashitha text-xl text-bakery-900 mb-2 tracking-wide">
                    {product.name}
                  </h3>
                  <span className="text-xs uppercase tracking-[0.15em] text-bakery-600">
                    {product.category}
                  </span>
                </div>
              </motion.div>
            ))}
          </AnimatePresence>
        </motion.div>
      </div>
    </div>
  );
};

export default ProductsPage;
