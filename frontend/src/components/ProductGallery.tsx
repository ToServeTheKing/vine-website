import { useState } from 'react';

interface ProductGalleryProps {
  images: string[];
  alt: string;
}

/**
 * The square photo on a product card, with arrows when there's more than one shot.
 *
 * Replaces react-awesome-slider, which hasn't been published since 2020 and pins peer deps to
 * React 16 — the same job in a fraction of the code, and one less unmaintained dependency in a
 * build we gate on CVEs. Behaviour is what the old cards did: one image at a time, square crop,
 * arrows only when they'd do something.
 */
const ProductGallery: React.FC<ProductGalleryProps> = ({ images, alt }) => {
  const [index, setIndex] = useState(0);
  const many = images.length > 1;

  const step = (delta: number) => setIndex((i) => (i + delta + images.length) % images.length);

  return (
    <div className="relative aspect-square bg-bakery-100">
      {images.map((src, i) => (
        <img
          key={src}
          src={src}
          // Only the visible frame gets described; the rest are decorative duplicates of the same item.
          alt={i === 0 ? alt : ''}
          loading="lazy"
          decoding="async"
          className={`absolute inset-0 w-full h-full object-cover transition-opacity duration-300 ${
            i === index ? 'opacity-100' : 'opacity-0 pointer-events-none'
          }`}
          aria-hidden={i === index ? undefined : true}
        />
      ))}

      {many && (
        <>
          <button
            type="button"
            onClick={() => step(-1)}
            aria-label={`Previous photo of ${alt}`}
            className="absolute left-2 top-1/2 -translate-y-1/2 grid place-items-center h-10 w-10 rounded-full bg-bakery-900/40 text-white text-2xl leading-none backdrop-blur-sm transition hover:bg-bakery-900/60 focus:outline-none focus-visible:ring-2 focus-visible:ring-white"
          >
            <span aria-hidden="true">{'<'}</span>
          </button>
          <button
            type="button"
            onClick={() => step(1)}
            aria-label={`Next photo of ${alt}`}
            className="absolute right-2 top-1/2 -translate-y-1/2 grid place-items-center h-10 w-10 rounded-full bg-bakery-900/40 text-white text-2xl leading-none backdrop-blur-sm transition hover:bg-bakery-900/60 focus:outline-none focus-visible:ring-2 focus-visible:ring-white"
          >
            <span aria-hidden="true">{'>'}</span>
          </button>
        </>
      )}
    </div>
  );
};

export default ProductGallery;
