import Link from 'next/link';
import Logo_R from 'public/images/resources/logo_R.svg';
import Logo_L from 'public/images/resources/logo_L.svg';

interface LogoProps {

    logoColor: string;
  
}

const Logo: React.FC<LogoProps> = ({ logoColor }) => {
    return (
        <Link href="/" className="flex items-center">
            <Logo_R fill={logoColor} width={60} height={60}/>
            {/* <Image
              src="/images/resources/logo_L.png"
              alt="The Vine Coffeehouse & Bakery"
              width={60}
              height={60}
              className="h-12 w-auto"
            /> */}
            <span className="ml-3 text-2xl text-bakery-800" style={{color: logoColor}}>
                <span className="font-lejour" style={{ letterSpacing: '0.01em'}}>The Vine  </span><span className="font-adbhashitha">Coffeehouse & Bakery</span>
            </span>
            <Logo_L fill={logoColor} width={60} height={60}/>
        </Link>
    );
}

export default Logo;