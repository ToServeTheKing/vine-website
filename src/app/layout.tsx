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
  description: "Boulangerie artisanale proposant des Breads traditionnels, Pastries et Cakes faits maison avec des products de qualité.",
  keywords: "boulangerie, pâtisserie, pain artisanal, Pastries, gâteaux, Cakes françaises",
  metadataBase: new URL('https://votre-domaine.fr'),
  openGraph: {
    title: 'The Vine Coffeehouse & Bakery',
    description: 'Découvrez nos Breads et Cakes artisanales',
    locale: 'en_US',
    type: 'website',
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