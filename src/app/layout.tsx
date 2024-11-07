import type { Metadata } from "next";
import { Playfair_Display, Raleway } from "next/font/google";
import "./globals.css";

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

export const metadata: Metadata = {
  title: "Boulangerie Artisanale - Pains & Délices",
  description: "Boulangerie artisanale proposant des pains traditionnels, viennoiseries et pâtisseries faits maison avec des produits de qualité.",
  keywords: "boulangerie, pâtisserie, pain artisanal, viennoiseries, gâteaux, pâtisseries françaises",
  metadataBase: new URL('https://votre-domaine.fr'),
  openGraph: {
    title: 'Boulangerie Artisanale - Pains & Délices',
    description: 'Découvrez nos pains et pâtisseries artisanales',
    locale: 'fr_FR',
    type: 'website',
  },
};

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html lang="fr" className={`${playfair.variable} ${raleway.variable}`}>
      <body className="min-h-screen bg-bakery-50">{children}</body>
    </html>
  );
}