import { Product } from '@/types/product';

export const categories = ['Tous', 'Pains', 'Viennoiseries', 'Pâtisseries'];

export const products: Product[] = [
  {
    id: 1,
    name: "Pagnot Traditionnel",
    description: "Pain de campagne à la croûte dorée et croustillante, façonné à la main selon la tradition.",
    price: "3.50€",
    category: "Pains",
    image: "/images/produits/pagnot.jpeg",
    ingredients: "Farine de blé, eau, levain, sel marin",
    storage: "À conserver dans un endroit frais et sec, consommer de préférence dans les 2-3 jours"
  },
  {
    id: 2,
    name: "Baguette",
    description: "Pain traditionnel français, croustillant à l'extérieur et moelleux à l'intérieur.",
    price: "2.00€",
    category: "Pains",
    image: "/images/produits/baguette.jpeg",
    ingredients: "Farine de blé, eau, levain, sel",
    storage: "À conserver dans un endroit frais et sec, consommer de préférence dans les 2-3 jours"
  },
  {
    id: 3,
    name: "Chocolatine",
    description: "Viennoiserie feuilletée au beurre, garnie de chocolat.",
    price: "1.50€",
    category: "Viennoiseries",
    image: "/images/produits/chocolatine.jpg",
    ingredients: "Farine de blé, beurre, sucre, chocolat",
    storage: "À conserver dans un endroit frais et sec, consommer de préférence dans les 2-3 jours"
  },
  {
    id: 4,
    name: "Croissant",
    description: "Viennoiserie feuilletée au beurre, idéale pour le petit-déjeuner.",
    price: "1.50€",
    category: "Viennoiseries",
    image: "/images/produits/croissant.jpeg",
    ingredients: "Farine de blé, beurre, sucre",
    storage: "À conserver dans un endroit frais et sec, consommer de préférence dans les 2-3 jours"
  },
  {
    id: 5,
    name: "Cupcake",
    description: "Petit gâteau moelleux, idéal pour une pause sucrée.",
    price: "2.00€",
    category: "Pâtisseries",
    image: "/images/produits/cupcake.jpg",
    ingredients: "Farine de blé, beurre, sucre, oeufs",
    storage: "À conserver dans un endroit frais et sec, consommer de préférence dans les 2-3 jours"
  },
  {
    id: 6,
    name: "Gâteau Royal Chocolat",
    description: "Gâteau au chocolat fondant, idéal pour les amateurs de chocolat.",
    price: "4.00€",
    category: "Pâtisseries",
    image: "/images/produits/gateau-royal.jpg",
    ingredients: "Farine de blé, beurre, sucre, chocolat, oeufs",
    storage: "À conserver dans un endroit frais et sec, consommer de préférence dans les 2-3 jours"
  },
]; 