'use client';
import { useState } from 'react';

type Status = 'idle' | 'sending' | 'sent' | 'error';

const ContactPage = () => {
  const [formData, setFormData] = useState({
    name: '',
    email: '',
    message: ''
  });
  const [status, setStatus] = useState<Status>('idle');
  const [error, setError] = useState('');

  const handleChange = (e: React.ChangeEvent<HTMLInputElement | HTMLTextAreaElement>) => {
    setFormData({
      ...formData,
      [e.target.name]: e.target.value
    });
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setStatus('sending');
    setError('');
    try {
      const res = await fetch('/api/contact', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(formData),
      });
      const data = await res.json();
      if (!res.ok) throw new Error(data.error || 'Could not send the message.');
      setStatus('sent');
      setFormData({ name: '', email: '', message: '' });
    } catch (err) {
      // Never claim success we did not get. Tell them, and give them the phone number.
      setStatus('error');
      setError(err instanceof Error ? err.message : 'Could not send the message.');
    }
  };

  return (
    <div className="min-h-screen bg-bakery-50">
      {/* Page header */}
      <header className="container mx-auto px-4 pt-14 pb-10 md:pt-20 md:pb-12 text-center">
        <h1 className="font-adbhashitha text-4xl md:text-5xl text-bakery-900">
          Contact us
        </h1>
      </header>

      {/* Contact Form Section */}
      <div className="container mx-auto px-4 pb-16 md:pb-24">
        <div className="max-w-2xl mx-auto bg-white p-8 md:p-10 rounded-3xl shadow-sm">
          <h2 className="font-adbhashitha text-2xl md:text-3xl text-bakery-900 mb-2">Get in touch</h2>
          <p className="text-bakery-700 mb-8">
            Or call us: <a href="tel:+13097010660" className="underline underline-offset-4 hover:text-bakery-600">(309) 701-0660</a>
          </p>
          <form onSubmit={handleSubmit}>
            <div className="mb-4">
              <label className="block text-sm font-medium text-bakery-800 mb-2" htmlFor="name">Name</label>
              <input 
                type="text" 
                id="name" 
                name="name" 
                value={formData.name} 
                onChange={handleChange} 
                className="w-full px-4 py-2.5 bg-bakery-50 border border-bakery-200 rounded-xl focus:outline-none focus:ring-2 focus:ring-bakery-500 focus:border-bakery-500" 
                required 
              />
            </div>
            <div className="mb-4">
              <label className="block text-sm font-medium text-bakery-800 mb-2" htmlFor="email">Email</label>
              <input 
                type="email" 
                id="email" 
                name="email" 
                value={formData.email} 
                onChange={handleChange} 
                className="w-full px-4 py-2.5 bg-bakery-50 border border-bakery-200 rounded-xl focus:outline-none focus:ring-2 focus:ring-bakery-500 focus:border-bakery-500" 
                required 
              />
            </div>
            <div className="mb-4">
              <label className="block text-sm font-medium text-bakery-800 mb-2" htmlFor="message">Message</label>
              <textarea 
                id="message" 
                name="message" 
                value={formData.message} 
                onChange={handleChange} 
                className="w-full px-4 py-2.5 bg-bakery-50 border border-bakery-200 rounded-xl focus:outline-none focus:ring-2 focus:ring-bakery-500 focus:border-bakery-500" 
                rows={5} 
                required 
              />
            </div>
            <div className="flex flex-col items-center gap-4">
              <button
                type="submit"
                disabled={status === 'sending'}
                className="px-8 py-3 bg-bakery-600 text-white rounded-full tracking-wide hover:bg-bakery-700 transition-colors disabled:opacity-60"
              >
                {status === 'sending' ? 'Sending...' : 'Send message'}
              </button>

              {status === 'sent' && (
                <p role="status" className="text-bakery-700">
                  Thanks. Your message is on its way, and we will get back to you.
                </p>
              )}
              {status === 'error' && (
                <p role="alert" className="text-center text-red-700">
                  {error} Please call us on{' '}
                  <a href="tel:+13097010660" className="underline underline-offset-4">(309) 701-0660</a>.
                </p>
              )}
            </div>
          </form>
        </div>
      </div>
    </div>
  );
};

export default ContactPage;
