'use client';
import { useState } from 'react';
import { motion } from 'framer-motion';
import LoadingSpinner from './LoadingSpinner';

const ContactPage = () => {
  const [isLoading, setIsLoading] = useState(false);
  const [formData, setFormData] = useState({
    name: '',
    email: '',
    message: ''
  });

  const handleChange = (e: React.ChangeEvent<HTMLInputElement | HTMLTextAreaElement>) => {
    setFormData({
      ...formData,
      [e.target.name]: e.target.value
    });
  };

  // TODO: Implement form submission
  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    setIsLoading(true);
    // Simulate form submission
    setTimeout(() => {
      setIsLoading(false);
      alert('Message sent!');
      setFormData({ name: '', email: '', message: '' });
    }, 1000);
  };

  return (
    <div className="min-h-screen bg-bakery-50">
      {/* Hero Section */}
      <motion.div 
        initial={{ opacity: 0 }}
        animate={{ opacity: 1 }}
        className="relative h-[40vh] bg-bakery-600"
      >
        <div className="absolute inset-0 bg-black/40 bg-cover bg-center" style={{ backgroundImage: 'url(/images/ressources/contact-bg.jpeg)', opacity: '0.6' }} />
        <div className="relative container mx-auto px-4 h-[45vh] flex items-center justify-center text-center">
          <div>
            <h1 className="font-sans text-4xl md:text-5xl text-white mb-4">
              Contact Us
            </h1>
            <p className="text-white/90 text-lg max-w-2xl">
              We would love to hear from you! Please fill out the form below to get in touch.
            </p>
          </div>
        </div>
      </motion.div>

      {/* Contact Form Section */}
      <div className="container mx-auto px-4 py-16">
        <div className="max-w-2xl mx-auto bg-white p-8 rounded-lg shadow-md">
          <h2 className="text-2xl font-semibold mb-6">Get in Touch</h2>
          <form onSubmit={handleSubmit}>
            <div className="mb-4">
              <label className="block text-sm font-medium text-gray-700 mb-2" htmlFor="name">Name</label>
              <input 
                type="text" 
                id="name" 
                name="name" 
                value={formData.name} 
                onChange={handleChange} 
                className="w-full px-4 py-2 border rounded-lg focus:outline-none focus:ring-2 focus:ring-bakery-600" 
                required 
              />
            </div>
            <div className="mb-4">
              <label className="block text-sm font-medium text-gray-700 mb-2" htmlFor="email">Email</label>
              <input 
                type="email" 
                id="email" 
                name="email" 
                value={formData.email} 
                onChange={handleChange} 
                className="w-full px-4 py-2 border rounded-lg focus:outline-none focus:ring-2 focus:ring-bakery-600" 
                required 
              />
            </div>
            <div className="mb-4">
              <label className="block text-sm font-medium text-gray-700 mb-2" htmlFor="message">Message</label>
              <textarea 
                id="message" 
                name="message" 
                value={formData.message} 
                onChange={handleChange} 
                className="w-full px-4 py-2 border rounded-lg focus:outline-none focus:ring-2 focus:ring-bakery-600" 
                rows={5} 
                required 
              />
            </div>
            <div className="flex justify-center">
              <button 
                type="submit" 
                className="px-6 py-2 bg-bakery-600 text-white rounded-full hover:bg-bakery-700 transition-colors"
              >
                {isLoading ? <LoadingSpinner /> : 'Send Message'}
              </button>
            </div>
          </form>
        </div>
      </div>
    </div>
  );
};

export default ContactPage;
