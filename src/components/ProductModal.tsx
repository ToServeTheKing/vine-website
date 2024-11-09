import { motion, AnimatePresence } from 'framer-motion';
import Image from 'next/image';
import type { Product } from '@/types/product';

interface ProductModalProps {
  product: Product | null;
  onClose: () => void;
}

const ProductModal = ({ product, onClose }: ProductModalProps) => {
  if (!product) return null;

  return (
    <AnimatePresence>
      <motion.div
        initial={{ opacity: 0 }}
        animate={{ opacity: 1 }}
        exit={{ opacity: 0 }}
        className="fixed inset-0 z-50 overflow-y-auto bg-black bg-opacity-50 backdrop-blur-sm"
        onClick={onClose}
      >
        <div className="min-h-screen px-4 text-center">
          <motion.div
            initial={{ scale: 0.95, opacity: 0 }}
            animate={{ scale: 1, opacity: 1 }}
            exit={{ scale: 0.95, opacity: 0 }}
            className="inline-block w-full max-w-2xl p-6 my-8 text-left align-middle bg-white rounded-lg shadow-xl"
            onClick={(e) => e.stopPropagation()}
          >
            <div className="relative h-80 mb-6">
              <Image
                src={product.image}
                alt={product.name}
                fill
                className="object-cover rounded-lg"
              />
            </div>
            
            <div className="flex justify-between items-start mb-4">
              <h3 className="font-serif text-2xl text-bakery-800">{product.name}</h3>
              <span className="text-bakery-600 text-xl font-semibold">{product.price}</span>
            </div>

            <p className="text-bakery-600/80 mb-6">{product.description}</p>

            <div className="space-y-4">
              <div>
                <h4 className="font-medium text-bakery-800 mb-2">Ingrédients</h4>
                <p className="text-bakery-600/80">{product.ingredients}</p>
              </div>
              
              <div>
                <h4 className="font-medium text-bakery-800 mb-2">Conservation</h4>
                <p className="text-bakery-600/80">{product.storage}</p>
              </div>
            </div>

            <button
              onClick={onClose}
              className="mt-6 w-full bg-bakery-600 text-white py-3 rounded-md hover:bg-bakery-700 transition-colors"
            >
              Fermer
            </button>
          </motion.div>
        </div>
      </motion.div>
    </AnimatePresence>
  );
};

export default ProductModal; 