'use client';
import { motion } from 'framer-motion';

const HistoryPage = () => {
  return (
    <div className="min-h-screen bg-bakery-50">
      {/* Hero Section */}
      <motion.div 
        initial={{ opacity: 0 }}
        animate={{ opacity: 1 }}
        className="relative h-[40vh] bg-bakery-600"
      >
        <div className="absolute inset-0 bg-black/40 bg-cover bg-center" style={{ backgroundImage: 'url(/images/ressources/history-bg.jpeg)', opacity: '0.6' }} />
        <div className="relative container mx-auto px-4 h-[45vh] flex items-center justify-center text-center">
          <div>
            <h1 className="font-sans text-4xl md:text-5xl text-white mb-4">
              Our History
            </h1>
            <p className="text-white/90 text-lg max-w-2xl">
              Discover the story behind our bakery.
            </p>
          </div>
        </div>
      </motion.div>

      {/* History Content Section */}
      <div className="container mx-auto px-4 py-16">
        <div className="max-w-3xl mx-auto bg-white p-8 rounded-lg shadow-md">
          <h2 className="text-2xl font-semibold mb-6">Our Journey</h2>
          <p className="mb-4">
            Our bakery was founded in 1920 by John Doe, a passionate baker with a dream to bring the finest baked goods to the community. Over the years, our bakery has grown and evolved, but our commitment to quality and tradition has remained the same.
          </p>
          <p className="mb-4">
            In the early days, John would wake up before dawn to prepare fresh bread and pastries for the day. His dedication and hard work quickly earned him a loyal customer base. As the bakery's reputation grew, so did its offerings. Today, we offer a wide variety of baked goods, from classic breads and pastries to modern cakes and desserts.
          </p>
          <p className="mb-4">
            Throughout the decades, our bakery has remained a family-owned business. Each generation has brought new ideas and innovations, while staying true to the values and traditions that John established. We are proud to be a part of this community and to continue serving our customers with the same passion and dedication that started it all.
          </p>
          <p className="mb-4">
            Thank you for being a part of our journey. We look forward to many more years of baking for you and your family.
          </p>
        </div>
      </div>
    </div>
  );
};

export default HistoryPage;
