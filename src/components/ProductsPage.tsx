'use client';
import { useState } from 'react';
import Image from 'next/image';
import { motion, AnimatePresence } from 'framer-motion';
import ProductModal from './ProductModal';
import { products, categories } from '@/data/products';
import type { Product } from '@/types/product';
import LoadingSpinner from './LoadingSpinner';

const ProductsPage = () => {
  const [selectedCategory, setSelectedCategory] = useState('Tous');
  const [selectedProduct, setSelectedProduct] = useState<Product | null>(null);
  const [isLoading, setIsLoading] = useState(false);

  const filteredProducts = selectedCategory === 'Tous'
    ? products
    : products.filter(product => product.category === selectedCategory);

  const handleCategoryChange = (category: string) => {
    setIsLoading(true);
    setSelectedCategory(category);
    // Simulate loading state
    setTimeout(() => setIsLoading(false), 500);
  };

  return (
    <div className="min-h-screen bg-bakery-50">
      {/* Hero Section */}
      <motion.div 
        initial={{ opacity: 0 }}
        animate={{ opacity: 1 }}
        className="relative h-[40vh] bg-bakery-900"
      >
        <div className="absolute inset-0 bg-black/40 bg-cover bg-center" style={{ backgroundImage: 'url(/images/ressources/produits-boutique.jpeg)', opacity: '0.6' }} />
        <div className="relative container mx-auto px-4 h-[45vh] flex items-center justify-center text-center">
          <div>
            <h1 className="font-serif text-4xl md:text-5xl text-white mb-4">
              Nos Produits
            </h1>
            <p className="text-white/90 text-lg max-w-2xl">
              Découvrez notre sélection de produits artisanaux, préparés chaque jour avec passion
            </p>
          </div>
        </div>
      </motion.div>

      {/* Products Section */}
      <div className="container mx-auto px-4 py-16">
        {/* Categories */}
        <div className="flex flex-wrap justify-center gap-4 mb-12">
          {categories.map((category) => (
            <motion.button
              key={category}
              whileHover={{ scale: 1.05 }}
              whileTap={{ scale: 0.95 }}
              onClick={() => handleCategoryChange(category)}
              className={`px-6 py-2 rounded-full border-2 transition-colors ${
                selectedCategory === category
                  ? 'bg-bakery-600 text-white border-bakery-600'
                  : 'border-bakery-600 text-bakery-600 hover:bg-bakery-600 hover:text-white'
              }`}
            >
              {category}
            </motion.button>
          ))}
        </div>

        {/* Products Grid */}
        {isLoading ? (
          <div className="flex justify-center items-center h-96">
            <LoadingSpinner />
          </div>
        ) : (
          <motion.div 
            layout
            className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-8"
          >
            <AnimatePresence>
              {filteredProducts.map((product) => (
                <motion.div
                  key={product.id}
                  layout
                  initial={{ opacity: 0 }}
                  animate={{ opacity: 1 }}
                  exit={{ opacity: 0 }}
                  className="group bg-white rounded-lg overflow-hidden shadow-md hover:shadow-xl transition-shadow duration-300"
                >
                  {/* Image Container */}
                  <div 
                    className="relative h-64 overflow-hidden cursor-pointer"
                    onClick={() => setSelectedProduct(product)}
                  >
                    <div className="absolute inset-0 bg-bakery-200/20" />
                    <Image
                      src={product.image}
                      alt={product.name}
                      fill
                      className="object-cover transform group-hover:scale-105 transition-transform duration-300"
                    />
                  </div>

                  {/* Content */}
                  <div className="p-6">
                    <div className="flex justify-between items-start mb-2">
                      <h3 className="font-serif text-xl text-bakery-800">
                        {product.name}
                      </h3>
                      <span className="text-bakery-600 font-semibold">
                        {product.price}
                      </span>
                    </div>
                    <p className="text-bakery-600/80 text-sm mb-4">
                      {product.description}
                    </p>
                    <div className="flex justify-between items-center">
                      <span className="text-sm text-bakery-500">
                        {product.category}
                      </span>
                      <button 
                        onClick={() => setSelectedProduct(product)}
                        className="text-bakery-600 hover:text-bakery-800 text-sm font-medium transition-colors"
                      >
                        En savoir plus →
                      </button>
                    </div>
                  </div>
                </motion.div>
              ))}
            </AnimatePresence>
          </motion.div>
        )}
      </div>

      {/* Product Modal */}
      <ProductModal 
        product={selectedProduct} 
        onClose={() => setSelectedProduct(null)} 
      />
    </div>
  );
};

export default ProductsPage; 