import { useRef, useState } from 'react';

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
 * arrows only when they'd do something — plus swipe and keyboard, which the old slider had on touch
 * devices and the first version of this did not.
 */

/** Past this many pixels a horizontal drag counts as a swipe rather than a tap or a page scroll. */
const SWIPE_THRESHOLD = 40;

const ProductGallery: React.FC<ProductGalleryProps> = ({ images, alt }) => {
  const [index, setIndex] = useState(0);
  const many = images.length > 1;
  const touchStart = useRef<{ x: number; y: number } | null>(null);

  const step = (delta: number) => setIndex((i) => (i + delta + images.length) % images.length);

  const onTouchStart = (e: React.TouchEvent) => {
    const t = e.touches[0];
    touchStart.current = { x: t.clientX, y: t.clientY };
  };

  const onTouchEnd = (e: React.TouchEvent) => {
    const start = touchStart.current;
    touchStart.current = null;
    if (!start || !many) return;
    const t = e.changedTouches[0];
    const dx = t.clientX - start.x;
    const dy = t.clientY - start.y;
    // Ignore anything more vertical than horizontal — that is the page being scrolled, not a swipe.
    if (Math.abs(dx) < SWIPE_THRESHOLD || Math.abs(dx) <= Math.abs(dy)) return;
    step(dx < 0 ? 1 : -1);
  };

  return (
    <div
      className="relative aspect-square bg-bakery-100"
      onTouchStart={onTouchStart}
      onTouchEnd={onTouchEnd}
      onKeyDown={many ? (e) => {
        if (e.key === 'ArrowLeft') { e.preventDefault(); step(-1); }
        if (e.key === 'ArrowRight') { e.preventDefault(); step(1); }
      } : undefined}
      tabIndex={many ? 0 : undefined}
      role={many ? 'group' : undefined}
      aria-roledescription={many ? 'carousel' : undefined}
      aria-label={many ? `${alt} — ${images.length} photos` : undefined}
    >
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
          {/* Which of how many. The old slider ran with bullets off, but once a card can be swiped
              there is otherwise nothing to say it holds more than one photo. */}
          <div className="absolute bottom-2 left-1/2 -translate-x-1/2 flex gap-1.5" aria-hidden="true">
            {images.map((src, i) => (
              <span
                key={src}
                className={`h-1.5 w-1.5 rounded-full transition ${
                  i === index ? 'bg-white' : 'bg-white/40'
                }`}
              />
            ))}
          </div>
        </>
      )}
    </div>
  );
};

export default ProductGallery;
