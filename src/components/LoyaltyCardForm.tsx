'use client'
import { useState, useEffect, useRef } from 'react';

interface LoyaltyCardFormProps {
  isOpen: boolean;
  onClose: () => void;
}

const LoyaltyCardForm = ({ isOpen, onClose }: LoyaltyCardFormProps) => {
  const modalRef = useRef<HTMLDivElement>(null);
  const [formData, setFormData] = useState({
    firstName: '',
    lastName: '',
    age: '',
    address: '',
    postalCode: '',
    city: '',
    email: '',
  });

  useEffect(() => {
    if (isOpen) {
      document.body.style.overflow = 'hidden';
    } else {
      document.body.style.overflow = 'unset';
    }
    return () => {
      document.body.style.overflow = 'unset';
    };
  }, [isOpen]);

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    console.log('Form submitted:', formData);
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
    <div className="fixed inset-0 z-50 overflow-y-auto bg-black/50 backdrop-blur-sm">
      <div className="min-h-screen px-4 text-center">
        {/* This element centers the modal */}
        <span
          className="inline-block h-screen align-middle"
          aria-hidden="true"
        >
          &#8203;
        </span>

        {/* Modal */}
        <div 
          ref={modalRef}
          className="inline-block w-full max-w-md p-6 my-8 text-left align-middle bg-white rounded-lg shadow-xl transform transition-all"
          onClick={(e) => e.stopPropagation()}
        >
          <div className="flex justify-between items-center mb-6">
            <h2 className="font-serif text-2xl text-bakery-800">
              Demande de Carte de Fidélité
            </h2>
            <button
              onClick={onClose}
              className="text-bakery-600 hover:text-bakery-800"
            >
              <svg className="h-6 w-6" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
              </svg>
            </button>
          </div>

          <form onSubmit={handleSubmit} className="space-y-4">
            <div className="grid grid-cols-2 gap-4">
              <div>
                <label className="block text-sm font-medium text-bakery-700 mb-1">
                  Prénom
                </label>
                <input
                  type="text"
                  required
                  className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-bakery-500"
                  value={formData.firstName}
                  onChange={(e) => setFormData({ ...formData, firstName: e.target.value })}
                />
              </div>
              <div>
                <label className="block text-sm font-medium text-bakery-700 mb-1">
                  Nom
                </label>
                <input
                  type="text"
                  required
                  className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-bakery-500"
                  value={formData.lastName}
                  onChange={(e) => setFormData({ ...formData, lastName: e.target.value })}
                />
              </div>
            </div>

            <div>
              <label className="block text-sm font-medium text-bakery-700 mb-1">
                Âge
              </label>
              <input
                type="number"
                required
                min="0"
                className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-bakery-500"
                value={formData.age}
                onChange={(e) => setFormData({ ...formData, age: e.target.value })}
              />
            </div>

            <div>
              <label className="block text-sm font-medium text-bakery-700 mb-1">
                Adresse
              </label>
              <input
                type="text"
                required
                className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-bakery-500"
                value={formData.address}
                onChange={(e) => setFormData({ ...formData, address: e.target.value })}
              />
            </div>

            <div className="grid grid-cols-2 gap-4">
              <div>
                <label className="block text-sm font-medium text-bakery-700 mb-1">
                  Code Postal
                </label>
                <input
                  type="text"
                  required
                  className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-bakery-500"
                  value={formData.postalCode}
                  onChange={(e) => setFormData({ ...formData, postalCode: e.target.value })}
                />
              </div>
              <div>
                <label className="block text-sm font-medium text-bakery-700 mb-1">
                  Ville
                </label>
                <input
                  type="text"
                  required
                  className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-bakery-500"
                  value={formData.city}
                  onChange={(e) => setFormData({ ...formData, city: e.target.value })}
                />
              </div>
            </div>

            <div>
              <label className="block text-sm font-medium text-bakery-700 mb-1">
                Email
              </label>
              <input
                type="email"
                required
                className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-bakery-500"
                value={formData.email}
                onChange={(e) => setFormData({ ...formData, email: e.target.value })}
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
      </div>
    </div>
  );
};

export default LoyaltyCardForm; 