'use client'
import { useState } from 'react';

interface LoyaltyCardFormProps {
  isOpen: boolean;
  onClose: () => void;
}

const LoyaltyCardForm = ({ isOpen, onClose }: LoyaltyCardFormProps) => {
  const [formData, setFormData] = useState({
    firstName: '',
    lastName: '',
    age: '',
    address: '',
    postalCode: '',
    city: '',
    email: '',
  });

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    // Handle form submission here
    console.log('Form submitted:', formData);
    // Reset form and close overlay
    setFormData({
      firstName: '',
      lastName: '',
      age: '',
      address: '',
      postalCode: '',
      city: '',
      email: '',
    });
    onClose();
  };

  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 bg-black bg-opacity-50 z-50 flex items-center justify-center p-4">
      <div className="bg-white rounded-lg max-w-md w-full max-h-[90vh] overflow-y-auto">
        <div className="p-6">
          <div className="flex justify-between items-center mb-6">
            <h2 className="font-serif text-2xl text-bakery-800">Demande de Carte de Fidélité</h2>
            <button
              onClick={onClose}
              className="text-bakery-600 hover:text-bakery-800"
              aria-label="Fermer"
            >
              <svg className="h-6 w-6" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
              </svg>
            </button>
          </div>

          <form onSubmit={handleSubmit} className="space-y-4">
            <div>
              <label htmlFor="firstName" className="block text-sm font-medium text-bakery-700">Prénom</label>
              <input
                type="text"
                id="firstName"
                required
                className="mt-1 block w-full rounded-md border-gray-300 shadow-sm focus:border-bakery-500 focus:ring-bakery-500"
                value={formData.firstName}
                onChange={(e) => setFormData({ ...formData, firstName: e.target.value })}
              />
            </div>

            <div>
              <label htmlFor="lastName" className="block text-sm font-medium text-bakery-700">Nom</label>
              <input
                type="text"
                id="lastName"
                required
                className="mt-1 block w-full rounded-md border-gray-300 shadow-sm focus:border-bakery-500 focus:ring-bakery-500"
                value={formData.lastName}
                onChange={(e) => setFormData({ ...formData, lastName: e.target.value })}
              />
            </div>

            <div>
              <label htmlFor="age" className="block text-sm font-medium text-bakery-700">Âge</label>
              <input
                type="number"
                id="age"
                required
                min="0"
                className="mt-1 block w-full rounded-md border-gray-300 shadow-sm focus:border-bakery-500 focus:ring-bakery-500"
                value={formData.age}
                onChange={(e) => setFormData({ ...formData, age: e.target.value })}
              />
            </div>

            <div>
              <label htmlFor="address" className="block text-sm font-medium text-bakery-700">Adresse</label>
              <input
                type="text"
                id="address"
                required
                className="mt-1 block w-full rounded-md border-gray-300 shadow-sm focus:border-bakery-500 focus:ring-bakery-500"
                value={formData.address}
                onChange={(e) => setFormData({ ...formData, address: e.target.value })}
              />
            </div>

            <div>
              <label htmlFor="postalCode" className="block text-sm font-medium text-bakery-700">Code Postal</label>
              <input
                type="text"
                id="postalCode"
                required
                className="mt-1 block w-full rounded-md border-gray-300 shadow-sm focus:border-bakery-500 focus:ring-bakery-500"
                value={formData.postalCode}
                onChange={(e) => setFormData({ ...formData, postalCode: e.target.value })}
              />
            </div>

            <div>
              <label htmlFor="city" className="block text-sm font-medium text-bakery-700">Ville</label>
              <input
                type="text"
                id="city"
                required
                className="mt-1 block w-full rounded-md border-gray-300 shadow-sm focus:border-bakery-500 focus:ring-bakery-500"
                value={formData.city}
                onChange={(e) => setFormData({ ...formData, city: e.target.value })}
              />
            </div>

            <div>
              <label htmlFor="email" className="block text-sm font-medium text-bakery-700">Email</label>
              <input
                type="email"
                id="email"
                required
                className="mt-1 block w-full rounded-md border-gray-300 shadow-sm focus:border-bakery-500 focus:ring-bakery-500"
                value={formData.email}
                onChange={(e) => setFormData({ ...formData, email: e.target.value })}
              />
            </div>

            <button
              type="submit"
              className="w-full bg-bakery-600 text-white py-2 px-4 rounded-md hover:bg-bakery-700 transition duration-200"
            >
              Demander ma carte
            </button>
          </form>
        </div>
      </div>
    </div>
  );
};

export default LoyaltyCardForm; 