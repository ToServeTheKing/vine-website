import type { Metadata } from "next";
import { Playfair_Display, Raleway } from "next/font/google";
import localFont from 'next/font/local';
import "./globals.css";
import Header from '@/components/Header';
import Footer from '@/components/Footer';

const playfair = Playfair_Display({ 
  subsets: ["latin"],
  variable: '--font-playfair',
  display: 'swap',
});

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
  title: "The Vine Coffeehouse & Bakery",
  description: "Artisanal bakery and coffeehouse in Princeville, IL offering traditional breads, pastries, and cakes made with quality ingredients.",
  keywords: "bakery, coffeehouse, artisanal bread, pastries, cakes, Princeville IL",
  metadataBase: new URL('https://itsthevine.com'),
  openGraph: {
    title: 'The Vine Coffeehouse & Bakery',
    description: 'Discover The Vine Coffeehouse and Bakery in Princeville, IL',
    locale: 'en_US',
    type: 'website',
    images: [
      {
        url: '/images/og/gallery.jpg',
        width: 1200,
        height: 630,
        alt: 'Products of The Vine'
      },
    ],
  },
};

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html lang="en" className={`${playfair.variable} ${raleway.variable} ${adbhashitha.variable} ${lejour.variable}`}>
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