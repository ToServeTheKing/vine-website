'use client';
import { useState } from 'react';
import Image from 'next/image';
import { motion, AnimatePresence } from 'framer-motion';
import ProductModal from './ProductModal';
import { products, categories } from '@/data/products';
import type { Product } from '@/types/product';
import LoadingSpinner from './LoadingSpinner';
import 'react-awesome-slider/dist/styles.css';
import AwesomeSlider from 'react-awesome-slider';
  
const ProductsPage = () => {
  const [selectedCategory, setSelectedCategory] = useState('All');
  const [selectedProduct, setSelectedProduct] = useState<Product | null>(null);
  const [isLoading, setIsLoading] = useState(false);

  const filteredProducts = selectedCategory === 'All'
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
        className="relative h-[40vh] bg-bakery-600"
      >
        <div className="absolute inset-0 bg-black/40 bg-cover bg-center" style={{ backgroundImage: 'url(/images/ressources/contact-bg.jpeg)', opacity: '0.6' }} />
        <div className="relative container mx-auto px-4 h-[45vh] flex items-center justify-center text-center">
          <div>
            <h1 className="font-sans text-4xl md:text-5xl text-white mb-4">
              Products
            </h1>
            <p className="text-white/90 text-lg max-w-2xl">
              Explore our wide range of products crafted with the finest ingredients. Whether you're looking for something sweet or savory, we have something for everyone. Browse through our categories to find your perfect treat.
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
                    className="relative w-full overflow-hidden cursor-pointer"
                    onClick={() => setSelectedProduct(product)}
                    >
                    <AwesomeSlider 
                      bullets={false} 
                      style={{ aspectRatio: '1 / 1' }} 
                      organicArrows={product.images.length > 1}
                      customContent={true}
                      buttonContentLeft={product.images.length > 1 ? <span className="text-white text-2xl">{'<'}</span> : null}
                      buttonContentRight={product.images.length > 1 ? <span className="text-white text-2xl">{'>'}</span> : null}
                    >
                      {product.images.map((image, index) => (
                      <div key={index} data-src={image} />
                      ))}
                    </AwesomeSlider>
                    </div>

                  {/* Content */}
                  <div className="p-6">
                    <div className="flex justify-between items-start mb-2">
                      <h3 className="font-sans text-xl text-bakery-800">
                        {product.name}
                      </h3>
                    </div>
                    <div className="flex justify-between items-center">
                      <span className="text-sm text-bakery-500">
                        {product.category}
                      </span>
                    </div>
                  </div>
                </motion.div>
              ))}
            </AnimatePresence>
          </motion.div>
        )}
      </div>

      {/* Product Modal */}
      {/* <ProductModal 
        product={selectedProduct} 
        onClose={() => setSelectedProduct(null)} 
      /> */}
    </div>
  );
};

export default ProductsPage; 