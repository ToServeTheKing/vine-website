import { motion, AnimatePresence } from 'framer-motion';
import type { Product } from '@/types/product';
import AwesomeSlider from 'react-awesome-slider';
import "keen-slider/keen-slider.min.css"

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
        className="fixed inset-0 z-50 overflow-y-auto bg-black bg-opacity-50 backdrop-blur-sm flex items-center justify-center"
        onClick={onClose}
      >
        <motion.div
          initial={{ scale: 0.95, opacity: 0 }}
          animate={{ scale: 1, opacity: 1 }}
          exit={{ scale: 0.95, opacity: 0 }}
          className="inline-block w-full max-w-2xl p-6 my-8 text-left align-middle bg-white rounded-lg shadow-xl"
          onClick={(e) => e.stopPropagation()}
        >
          <div className="relative h-80 mb-6">
            <AwesomeSlider cssModule={AwesomeSlider}>
              {product.images.map((imgUrl, index) => (
                <div
                  key={index}
                  data-src={imgUrl}
                  className="w-full h-full bg-cover bg-center"
                />
              ))}
            </AwesomeSlider>
          </div>
          
          <div className="flex justify-between items-start mb-4">
            <h3 className="font-sans text-2xl text-bakery-800">{product.name}</h3>
            {/* <span className="text-bakery-600 text-xl font-semibold">{product.price}</span> */}
          </div>

          <button
            onClick={onClose}
            className="mt-6 w-full bg-bakery-600 text-white py-3 rounded-md hover:bg-bakery-700 transition-colors"
          >
            Close
          </button>
        </motion.div>
      </motion.div>
    </AnimatePresence>
  );
};

export default ProductModal;
