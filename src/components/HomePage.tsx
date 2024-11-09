'use client'
import Link from 'next/link';
import Image from 'next/image';
import { useState } from 'react';

const HomePage = () => {
  const [showLoyaltyForm, setShowLoyaltyForm] = useState(false);

  return (
    <div className="min-h-screen">
      {/* Hero Section */}
      <section className="relative h-[80vh]">
        {/* Background Image */}
        <Image
          src="/images/ressources/main-banner.jpeg"
          alt="Au Blé d'Or - Boulangerie Artisanale"
          fill
          priority
          className="object-cover"
          quality={90}
        />
        {/* Overlay */}
        <div className="absolute inset-0 bg-black/40" />
        
        {/* Content */}
        <div className="relative container mx-auto px-4 h-full flex items-center">
          <div className="text-white max-w-2xl">
            <h1 className="font-serif text-5xl md:text-6xl mb-4">
              Boulangerie Artisanale
            </h1>
            <p className="text-xl mb-8">
              Découvrez nos pains et pâtisseries faits maison avec passion depuis 1987
            </p>
            <Link 
              href="/produits" 
              className="bg-bakery-600 hover:bg-bakery-700 text-white px-8 py-3 rounded-md inline-block transition"
            >
              Commander en ligne
            </Link>
          </div>
        </div>
      </section>



      {/* Nos Spécialités */}
      <section className="py-16 bg-white">
        <div className="container mx-auto px-4">
          <h2 className="font-serif text-4xl text-center mb-12">Nos Spécialités</h2>
          <div className="grid grid-cols-1 md:grid-cols-3 gap-8">
            {['Pains Traditionnels', 'Viennoiseries', 'Pâtisseries'].map((item, index) => (
              <div key={index} className="text-center p-6 bg-bakery-50 rounded-lg">
                <h3 className="font-serif text-2xl mb-6">{item}</h3>
                <div className="flex justify-center items-center mb-4">
                  <Image
                    src={`/images/gallery/${item}.png`}
                    alt={item}
                    width={300}
                    height={200}
                    className="object-cover rounded-[20px] border border-black/20 shadow-lg"
                    priority={index < 3}
                  />
                </div>
                <p className="text-bakery-700">Découvrez notre sélection de {item.toLowerCase()} préparés chaque jour avec amour.</p>
              </div>
            ))}
          </div>
        </div>
      </section>

      {/* Notre Histoire */}
      <section className="py-16 bg-bakery-50">
        <div className="container mx-auto px-4">
          <div className="max-w-3xl mx-auto text-center">
            <h2 className="font-serif text-4xl mb-8">Notre Histoire</h2>
            <p className="text-lg mb-6">
              Depuis plus de 35 ans, notre boulangerie familiale perpétue la tradition du bon pain artisanal. 
              Chaque jour, nos artisans boulangers se lèvent tôt pour vous proposer des produits frais et authentiques.
            </p>
            <Link href="/notre-histoire" className="text-bakery-600 hover:text-bakery-700 font-semibold">
              En savoir plus →
            </Link>
          </div>
        </div>
      </section>

      {/* Loyalty Card Section */}
      <section className="py-16 bg-bakery-600 text-white">
        <div className="container mx-auto px-4">
          <div className="text-center mb-8">
            <h2 className="font-serif text-3xl mb-4">Carte de Fidélité</h2>
            <p className="text-lg mb-8">
              Profitez de nombreux avantages avec notre carte de fidélité
            </p>
          </div>

          {/* Loyalty Card with Flip Effect */}
          <div className="card-container max-w-2xl mx-auto aspect-[1.586/1] mb-8">
            <div className="card relative w-full h-full">
              {/* Front of the card */}
              <div className="card-side card-front absolute w-full h-full rounded-[24px] overflow-hidden bg-bakery-600">
                <Image
                  src="/images/ressources/card-front.png"
                  alt="Carte de fidélité - Recto"
                  fill
                  className="object-contain p-1"
                  priority
                  sizes="(max-width: 768px) 100vw, (max-width: 1200px) 50vw, 33vw"
                />
              </div>
              {/* Back of the card */}
              <div className="card-side card-back absolute w-full h-full rounded-[24px] overflow-hidden bg-bakery-600">
                <Image
                  src="/images/ressources/card-back.png"
                  alt="Carte de fidélité - Verso"
                  fill
                  className="object-contain p-1"
                  priority
                  sizes="(max-width: 768px) 100vw, (max-width: 1200px) 50vw, 33vw"
                />
              </div>
            </div>
          </div>

          {/* Button and Form Section */}
          <div className="max-w-2xl mx-auto">
            <button 
              onClick={() => setShowLoyaltyForm(!showLoyaltyForm)}
              className="bg-white text-bakery-600 px-8 py-3 rounded-md hover:bg-bakery-50 transition mb-8 mx-auto block"
            >
              {showLoyaltyForm ? 'Masquer le formulaire' : 'Obtenir ma carte'}
            </button>

            {/* Form */}
            {showLoyaltyForm && (
              <div className="bg-white text-bakery-800 rounded-lg p-6 mt-8 shadow-lg">
                <h3 className="font-serif text-2xl mb-6">Demande de Carte de Fidélité</h3>
                
                <form className="space-y-4">
                  <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                    <div>
                      <label className="block text-sm font-medium mb-1">
                        Prénom
                      </label>
                      <input
                        type="text"
                        required
                        className="w-full px-3 py-2 border border-bakery-200 rounded-md focus:outline-none focus:ring-2 focus:ring-bakery-500"
                      />
                    </div>
                    <div>
                      <label className="block text-sm font-medium mb-1">
                        Nom
                      </label>
                      <input
                        type="text"
                        required
                        className="w-full px-3 py-2 border border-bakery-200 rounded-md focus:outline-none focus:ring-2 focus:ring-bakery-500"
                      />
                    </div>
                  </div>

                  <div>
                    <label className="block text-sm font-medium mb-1">
                      Âge
                    </label>
                    <input
                      type="number"
                      required
                      min="0"
                      className="w-full px-3 py-2 border border-bakery-200 rounded-md focus:outline-none focus:ring-2 focus:ring-bakery-500"
                    />
                  </div>

                  <div>
                    <label className="block text-sm font-medium mb-1">
                      Adresse
                    </label>
                    <input
                      type="text"
                      required
                      className="w-full px-3 py-2 border border-bakery-200 rounded-md focus:outline-none focus:ring-2 focus:ring-bakery-500"
                    />
                  </div>

                  <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                    <div>
                      <label className="block text-sm font-medium mb-1">
                        Code Postal
                      </label>
                      <input
                        type="text"
                        required
                        className="w-full px-3 py-2 border border-bakery-200 rounded-md focus:outline-none focus:ring-2 focus:ring-bakery-500"
                      />
                    </div>
                    <div>
                      <label className="block text-sm font-medium mb-1">
                        Ville
                      </label>
                      <input
                        type="text"
                        required
                        className="w-full px-3 py-2 border border-bakery-200 rounded-md focus:outline-none focus:ring-2 focus:ring-bakery-500"
                      />
                    </div>
                  </div>

                  <div>
                    <label className="block text-sm font-medium mb-1">
                      Email
                    </label>
                    <input
                      type="email"
                      required
                      className="w-full px-3 py-2 border border-bakery-200 rounded-md focus:outline-none focus:ring-2 focus:ring-bakery-500"
                    />
                  </div>

                  <button
                    type="submit"
                    className="w-full bg-bakery-600 text-white py-3 px-4 rounded-md hover:bg-bakery-700 transition duration-200 mt-6"
                  >
                    Demander ma carte
                  </button>
                </form>
              </div>
            )}
          </div>
        </div>
      </section>

      {/* Horaires et Contact */}
      <section className="py-16 bg-white">
        <div className="container mx-auto px-4">
          <div className="grid grid-cols-1 md:grid-cols-2 gap-12">
            <div>
              <h2 className="font-serif text-3xl mb-6">Nos Horaires</h2>
              <ul className="space-y-3">
                <li className="flex justify-between">
                  <span>Lundi - Vendredi</span>
                  <span>6h30 - 19h30</span>
                </li>
                <li className="flex justify-between">
                  <span>Samedi</span>
                  <span>6h30 - 20h00</span>
                </li>
                <li className="flex justify-between">
                  <span>Dimanche</span>
                  <span>7h00 - 13h00</span>
                </li>
              </ul>
            </div>
            <div>
              <h2 className="font-serif text-3xl mb-6">Contact</h2>
              <address className="not-italic">
                <p className="mb-2">123 Rue du Pain</p>
                <p className="mb-2">75001 Paris</p>
                <p className="mb-2">Tél: 01 23 45 67 89</p>
                <p>Email: contact@blédor-artisanale.fr</p>
              </address>
            </div>
          </div>
        </div>
      </section>
    </div>
  );
};

export default HomePage;