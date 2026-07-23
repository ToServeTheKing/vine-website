import { useRef, useState } from 'react';

interface ProductGalleryProps {
  images: string[];
  alt: string;
}

const Chevron = ({ direction }: { direction: 'left' | 'right' }) => (
  <svg
    className="h-5 w-5"
    viewBox="0 0 24 24"
    fill="none"
    stroke="currentColor"
    strokeWidth={2}
    strokeLinecap="round"
    strokeLinejoin="round"
    aria-hidden="true"
  >
    <path d={direction === 'left' ? 'M15 18l-6-6 6-6' : 'M9 18l6-6-6-6'} />
  </svg>
);

/**
 * The square photo on a product card, with arrows and dots when there's more than one shot.
 *
 * Replaces react-awesome-slider, which hasn't been published since 2020 and pins peer deps to
 * React 16 — the same job in a fraction of the code, and one less unmaintained dependency in a
 * build we gate on CVEs. Behaviour is what the old cards did: one image at a time, square crop,
 * arrows only when they'd do something.
 */
/** Past this many pixels a horizontal drag counts as a swipe rather than a tap or a page scroll. */
const SWIPE_THRESHOLD = 40;

const ProductGallery: React.FC<ProductGalleryProps> = ({ images, alt }) => {
  const [index, setIndex] = useState(0);

  // Filtering swaps the product under a reused component instance, so a stale index can point
  // past the new list — every frame then renders at opacity-0 and the card goes blank. Reset
  // during render (the React-sanctioned way to derive state from props) rather than in an effect,
  // so the correct frame paints on the first pass instead of flashing an empty square.
  const [renderedFor, setRenderedFor] = useState(images);
  if (renderedFor !== images) {
    setRenderedFor(images);
    setIndex(0);
  }

  const many = images.length > 1;
  const active = index < images.length ? index : 0;

  const step = (delta: number) => setIndex((i) => (i + delta + images.length) % images.length);

  // Swipe on touch devices and arrow keys — the react-awesome-slider this replaced had swipe, and
  // the products page is browsed mostly on phones. Vertical drags are left alone so the page still
  // scrolls through the card.
  const touchStart = useRef<{ x: number; y: number } | null>(null);
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
    if (Math.abs(dx) < SWIPE_THRESHOLD || Math.abs(dx) <= Math.abs(dy)) return;
    step(dx < 0 ? 1 : -1);
  };

  const arrowClass =
    'absolute top-1/2 -translate-y-1/2 grid place-items-center h-10 w-10 rounded-full bg-bakery-900/40 text-white backdrop-blur-sm transition hover:bg-bakery-900/60 focus:outline-none focus-visible:ring-2 focus-visible:ring-white';

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
          className={`absolute inset-0 w-full h-full object-cover transition-opacity duration-300 motion-reduce:transition-none ${
            i === active ? 'opacity-100' : 'opacity-0 pointer-events-none'
          }`}
          aria-hidden={i === active ? undefined : true}
        />
      ))}

      {many && (
        <>
          <button
            type="button"
            onClick={() => step(-1)}
            aria-label={`Previous photo of ${alt}`}
            className={`${arrowClass} left-2`}
          >
            <Chevron direction="left" />
          </button>
          <button
            type="button"
            onClick={() => step(1)}
            aria-label={`Next photo of ${alt}`}
            className={`${arrowClass} right-2`}
          >
            <Chevron direction="right" />
          </button>

          {/* Dots: how many photos there are, and which one you're on. */}
          <div className="absolute inset-x-0 bottom-3 flex justify-center gap-1.5">
            {images.map((src, i) => (
              <button
                key={src}
                type="button"
                onClick={() => setIndex(i)}
                aria-label={`Show photo ${i + 1} of ${images.length} of ${alt}`}
                aria-current={i === active ? 'true' : undefined}
                className={`h-1.5 rounded-full shadow-xs transition-all motion-reduce:transition-none focus:outline-none focus-visible:ring-2 focus-visible:ring-white ${
                  i === active ? 'w-4 bg-white' : 'w-1.5 bg-white/60 hover:bg-white/80'
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
