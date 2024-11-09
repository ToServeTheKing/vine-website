import Image from 'next/image';

interface Product {
  id: number;
  name: string;
  description: string;
  price: string;
  category: string;
  image: string;
}

const products: Product[] = [
  {
    id: 1,
    name: "Pagnot Traditionnel",
    description: "Pain de campagne à la croûte dorée et croustillante, façonné à la main selon la tradition.",
    price: "1.50€",
    category: "Pains",
    image: "/images/produits/pagnot.jpeg"
  },
  {
    id: 2,
    name: "Baguette Tradition",
    description: "La classique baguette française, croustillante à l'extérieur et moelleuse à l'intérieur.",
    price: "0.90€",
    category: "Pains",
    image: "/images/produits/baguette.jpeg"
  },
  {
    id: 3,
    name: "Cupcake Vanille",
    description: "Délicieux cupcake à la vanille de Madagascar, surmonté d'une crème légère.",
    price: "3.00€",
    category: "Pâtisseries",
    image: "/images/produits/cupcake.jpg"
  },
  {
    id: 4,
    name: "Gâteau Royal Chocolat",
    description: "Gâteau au chocolat noir intense, avec ses trois couches de mousse au chocolat.",
    price: "12.50€",
    category: "Pâtisseries",
    image: "/images/produits/gateau-royal.jpg"
  },
  {
    id: 5,
    name: "Croissant Pur Beurre",
    description: "Croissant artisanal au beurre AOP, feuilleté et croustillant.",
    price: "1.45€",
    category: "Viennoiseries",
    image: "/images/produits/croissant.jpeg"
  },
  {
    id: 6,
    name: "Chocolatine",
    description: "Viennoiserie au chocolat noir, feuilletée et généreusement garnie.",
    price: "1.75€",
    category: "Viennoiseries",
    image: "/images/produits/chocolatine.jpg"
  }
];

const ProductsPage = () => {
  return (
    <div className="min-h-screen bg-bakery-50">
      {/* Hero Section */}
      <div className="relative h-[50vh] bg-bakery-900">
        <Image
          src="/images/ressources/produits-boutique.jpeg"
          alt="Boutique de produits"
          fill
          className="object-cover"
        />
        <div className="absolute inset-0 bg-black/40" />
        <div className="relative container mx-auto px-4 h-full flex items-center justify-center text-center">
          <div>
            <h1 className="font-serif text-4xl md:text-5xl text-white mb-4">
              Nos Produits
            </h1>
            <p className="text-white/90 text-lg max-w-2xl">
              Découvrez notre sélection de produits artisanaux, préparés chaque jour avec passion
            </p>
          </div>
        </div>
      </div>

      {/* Products Section */}
      <div className="container mx-auto px-4 py-16">
        {/* Categories */}
        <div className="flex flex-wrap justify-center gap-4 mb-12">
          {['Tous', 'Pains', 'Viennoiseries', 'Pâtisseries'].map((category) => (
            <button
              key={category}
              className="px-6 py-2 rounded-full border-2 border-bakery-600 text-bakery-600 hover:bg-bakery-600 hover:text-white transition-colors"
            >
              {category}
            </button>
          ))}
        </div>

        {/* Products Grid */}
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-8">
          {products.map((product) => (
            <div 
              key={product.id}
              className="group bg-white rounded-lg overflow-hidden shadow-md hover:shadow-xl transition-shadow duration-300"
            >
              {/* Image Container */}
              <div className="relative h-64 overflow-hidden">
                <div className="absolute inset-0 bg-bakery-200/20" />
                <Image
                  src={product.image}
                  alt={product.name}
                  fill
                  className="object-cover transform group-hover:scale-105 transition-transform duration-300"
                />
              </div>

              {/* Content */}
              <div className="p-6">
                <div className="flex justify-between items-start mb-2">
                  <h3 className="font-serif text-xl text-bakery-800">
                    {product.name}
                  </h3>
                  <span className="text-bakery-600 font-semibold">
                    {product.price}
                  </span>
                </div>
                <p className="text-bakery-600/80 text-sm mb-4">
                  {product.description}
                </p>
                <div className="flex justify-between items-center">
                  <span className="text-sm text-bakery-500">
                    {product.category}
                  </span>
                  <button className="text-bakery-600 hover:text-bakery-800 text-sm font-medium transition-colors">
                    En savoir plus →
                  </button>
                </div>
              </div>
            </div>
          ))}
        </div>
      </div>
    </div>
  );
};

export default ProductsPage; 