import { Metadata } from 'next';
import HistoryPage from '@/components/HistoryPage';

export const metadata: Metadata = {
  title: 'History | The Vine',
  description: 'Découvrez notre sélection de Breads traditionnels, Pastries et Cakes artisanales. products frais préparés chaque jour avec passion.',
  keywords: 'boulangerie, pâtisserie, pain artisanal, Pastries, croissant, chocolatine, pagnot, baguette tradition',
  openGraph: {
    title: 'Contact | The Vine',
    description: 'Découvrez nos products artisanaux frais',
    images: [
      {
        url: '/images/og/products.jpg',
        width: 1200,
        height: 630,
        alt: 'History of The Vine'
      },
    ],
  },
};

export default function Page() {
  return <HistoryPage />;
} 