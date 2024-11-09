import { Metadata } from 'next';
import ProductsPage from '@/components/ProductsPage';

export const metadata: Metadata = {
  title: 'Nos Produits | Au Blé d\'Or - Boulangerie Artisanale',
  description: 'Découvrez notre sélection de pains traditionnels, viennoiseries et pâtisseries artisanales. Produits frais préparés chaque jour avec passion.',
  keywords: 'boulangerie, pâtisserie, pain artisanal, viennoiseries, croissant, chocolatine, pagnot, baguette tradition',
  openGraph: {
    title: 'Nos Produits | Au Blé d\'Or',
    description: 'Découvrez nos produits artisanaux frais',
    images: [
      {
        url: '/images/og/products.jpg',
        width: 1200,
        height: 630,
        alt: 'Produits de la boulangerie Au Blé d\'Or',
      },
    ],
  },
};

export default function Page() {
  return <ProductsPage />;
} 