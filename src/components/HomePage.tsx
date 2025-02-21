'use client'
import Link from 'next/link';
import Image from 'next/image';
// import { useState } from 'react';

const HomePage = () => {
  // const [showLoyaltyForm, setShowLoyaltyForm] = useState(false);

  return (
    <div className="min-h-screen">
      {/* Hero Section */}
      <section className="relative py-16 bg-bakery-600 text-white">
        {/* Background Image */}
        {/* <Image
          src="/images/gallery/Outside.jpg"
          alt="The Vine Coffeehouse & Bakery"
          fill
          priority
          className="object-cover"
          quality={90}
        /> */}
        {/* Overlay */}
        <div className="absolute inset-0"/>
        
        {/* Content */}
        <div className="relative container mx-auto px-4 h-full flex items-center">
            <div className="text-white max-w-2xl">
              <div className="w-full">
                <Image
                src="/images/vine_logo/linen_mist.png"
                alt="The Vine Coffeehouse & Bakery"
                width={2}
                height={1}
                layout="responsive"
                className="object-cover"
                quality={90}
                />
              </div>
              <p className="text-xl mb-8">
                Discover our homemade treats and cakes made with passion.
              </p>
              <Link 
                href="/products" 
                className="bg-bakery-50 hover:bg-bakery-100 text-bakery-800 px-8 py-3 rounded-md inline-block transition"
              >
              See Our Products
              </Link>
            </div>
        </div>
      </section>



      {/* Our Specialties */}
      <section className="py-16 bg-white">
        <div className="container mx-auto px-4">
          <h2 className="font-adbhashitha text-4xl text-center mb-12">Our Specialties</h2>
          <div className="grid grid-cols-1 md:grid-cols-3 gap-8">
            {['Cinnamon Rolls', 'Sugar Cookies', 'Cakes'].map((item, index) => (
              <div key={index} className="text-center p-6 bg-bakery-50 rounded-lg">
                <h3 className="font-sans text-2xl mb-6">{item}</h3>
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
                {/* <p className="text-bakery-700">Discover our selection of {item.toLowerCase()} prepared daily with love.</p> */}
              </div>
            ))}
          </div>
        </div>
      </section>

      {/* About Us */}
      <section className="py-16 bg-bakery-600 text-white">
        <div className="container mx-auto px-4">
          <div className="max-w-3xl mx-auto text-center">
            <h2 className="font-adbhashitha text-4xl mb-8" style={{ letterSpacing: '0.01em' }}>About Us</h2>
            <p className="text-lg mb-6">
            Welcome to The Vine Bakery, where the warmth of home meets the heart of Princeville, IL. We are passionate about baking delicious, fresh goods that bring people together. Our cozy space invites you to gather with friends and family, sharing in the simple joy of homemade treats made with love and the finest ingredients. Come join us at The Vine and feel right at home.
            </p>
            <Link href="/notre-histoire" className="text-white hover:text-bakery-700 font-semibold">
              Learn More →
            </Link>
          </div>
        </div>
      </section>

      {/* Hours & Contact */}
      <section className="py-16 bg-white">
        <div className="container mx-auto px-4">
          <div className="grid grid-cols-1 md:grid-cols-2 gap-12">
            <div>
              <h2 className="font-adbhashitha text-3xl mb-6">Our Hours</h2>
              <ul className="space-y-3">
                <li className="flex justify-between">
                  <span>Monday - Saturday</span>
                  <span>7:00am - 2:00pm</span>
                </li>
                <li className="flex justify-between">
                  <span>Sunday - Monday</span>
                  <span>Closed</span>
                </li>
              </ul>
            </div>
            <div>
              <h2 className="font-adbhashitha text-3xl mb-6">Contact</h2>
              <address className="not-italic">
                <p className="mb-2">215 E Main Street</p>
                <p className="mb-2">61559 Princeville, IL</p>
                <p className="mb-2">Phone: (309) 701-0660</p>
                <p>Email: contact@itsthevine.com</p>
              </address>
            </div>
          </div>
        </div>
      </section>
    </div>
  );
};

export default HomePage;