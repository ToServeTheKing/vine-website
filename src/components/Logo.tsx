import Link from 'next/link';
import Logo_R from 'public/images/resources/logo_R.svg';
import Logo_L from 'public/images/resources/logo_L.svg';

interface LogoProps {
  /** Tailwind text-* class. The SVG marks fill with currentColor, so this colors the whole lockup. */
  className?: string;
  size?: 'sm' | 'lg';
  /** The hero sits on the homepage, where a link back to "/" is pointless. */
  linked?: boolean;
}

// The lockup: branch · "The Vine" over "Coffeehouse + Bakery" · branch.
// Branch heights track the two-line wordmark so the marks read as part of it.
const SIZES = {
  sm: {
    branch: 'w-12 h-12 sm:w-14 sm:h-14',
    name: 'text-xl sm:text-2xl md:text-3xl',
    tag: 'text-[0.6rem] sm:text-xs tracking-[0.18em]',
    gap: 'gap-1.5 sm:gap-2',
  },
  lg: {
    branch: 'w-20 h-20 sm:w-28 sm:h-28',
    name: 'text-4xl sm:text-5xl md:text-6xl',
    tag: 'text-xs sm:text-base tracking-[0.2em]',
    gap: 'gap-2 sm:gap-4',
  },
};

const Logo: React.FC<LogoProps> = ({ className = 'text-bakery-700', size = 'sm', linked = true }) => {
  const s = SIZES[size];
  const inner = (
    <>
      <Logo_R className={`${s.branch} shrink-0`} />
      <span className="flex flex-col items-center leading-none min-w-0">
        <span className={`font-lejour ${s.name}`} style={{ letterSpacing: '0.01em' }}>
          The Vine
        </span>
        <span className={`font-adbhashitha ${s.tag} uppercase mt-1.5 whitespace-nowrap`}>
          Coffeehouse + Bakery
        </span>
      </span>
      <Logo_L className={`${s.branch} shrink-0`} />
    </>
  );

  const classes = `flex items-center ${s.gap} min-w-0 shrink ${className}`;

  if (!linked) {
    return (
      <div className={`${classes} justify-center`} role="img" aria-label="The Vine Coffeehouse + Bakery">
        {inner}
      </div>
    );
  }

  return (
    <Link href="/" className={classes} aria-label="The Vine Coffeehouse + Bakery, home">
      {inner}
    </Link>
  );
};

export default Logo;
