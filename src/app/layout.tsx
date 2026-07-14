import type { Metadata } from "next";
import { Raleway } from "next/font/google";
import localFont from 'next/font/local';
import "./globals.css";
import Header from '@/components/Header';
import Footer from '@/components/Footer';

const raleway = Raleway({
  subsets: ["latin"],
  variable: '--font-raleway',
  display: 'swap',
});

const adbhashitha = localFont({
  src: './fonts/AdBhashitha.woff',
  variable: '--font-adbhashitha',
  display: 'swap',
});

const lejour = localFont({
  src: './fonts/LeJour-Script.woff',
  variable: '--font-lejour',
  display: 'swap',
});

export const metadata: Metadata = {
  title: "The Vine Coffeehouse + Bakery",
  description: "A locally owned coffeehouse and bakery in downtown Princeville, Illinois. We bake pastries, custom cakes, cookies, and cinnamon rolls, and serve sandwiches, paninis, and coffee.",
  keywords: "bakery, coffeehouse, pastries, custom cakes, cinnamon rolls, paninis, Princeville IL",
  metadataBase: new URL('https://itsthevine.com'),
  openGraph: {
    title: 'The Vine Coffeehouse + Bakery',
    description: 'A locally owned coffeehouse and bakery in downtown Princeville, IL.',
    locale: 'en_US',
    type: 'website',
  },
  icons: {
    icon: [
      {
        media: '(prefers-color-scheme: light)',
        url: '/images/resources/logo_L.png',
        href: '/images/resources/logo_L.png',
      },
      {
        media: '(prefers-color-scheme: dark)',
        url: '/images/resources/logo_dark.png',
        href: '/images/resources/logo_dark.png',
      },
    ],
  }
};

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html lang="en" className={`${raleway.variable} ${adbhashitha.variable} ${lejour.variable}`}>
      <body className="min-h-screen bg-bakery-50 flex flex-col">
        <Header />
        <main className="flex-grow">
          {children}
        </main>
        <Footer />
      </body>
    </html>
  );
}