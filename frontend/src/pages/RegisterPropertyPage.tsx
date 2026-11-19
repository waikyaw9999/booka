import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { api, getErrorMessage } from '../lib/api';
import { useAuthStore } from '../stores';
import type { UserRole } from '../types';
import { InlineAlert } from '../components/ui';

export function RegisterPropertyPage() {
  const navigate = useNavigate();
  const { setUser } = useAuthStore();
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);
  const [form, setForm] = useState({
    hotelName: '',
    description: '',
    address: '',
    city: '',
    country: '',
    amenities: '',
    cancellationPolicy: '',
    managerFullName: '',
    managerEmail: '',
    managerPassword: '',
  });

  function update<K extends keyof typeof form>(key: K, value: string) {
    setForm((prev) => ({ ...prev, [key]: value }));
  }

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    setLoading(true);
    setError('');
    try {
      const res = await api.registerProperty({
        hotelName: form.hotelName.trim(),
        description: form.description.trim() || undefined,
        address: form.address.trim(),
        city: form.city.trim(),
        country: form.country.trim(),
        amenities: form.amenities.trim() || undefined,
        cancellationPolicy: form.cancellationPolicy.trim() || undefined,
        managerEmail: form.managerEmail.trim(),
        managerPassword: form.managerPassword,
        managerFullName: form.managerFullName.trim(),
      });
      setUser({
        userId: res.userId,
        email: res.email,
        fullName: res.fullName,
        role: res.role as UserRole,
        hotelId: res.hotelId,
        token: res.token,
      });
      navigate('/admin?tab=inventory');
    } catch (err) {
      setError(getErrorMessage(err, 'Unable to create your property. Please try again.'));
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="relative min-h-[70vh] overflow-hidden">
      <div className="pointer-events-none absolute inset-0 bg-[radial-gradient(ellipse_60%_50%_at_20%_0%,rgba(58,133,118,0.14),transparent)]" />
      <div className="relative mx-auto max-w-3xl px-4 py-14 sm:py-20">
        <div className="mb-8 space-y-3">
          <p className="font-display text-4xl font-bold tracking-tight text-forest">Booka</p>
          <h1 className="font-display text-3xl font-semibold text-ink">List your property</h1>
          <p className="max-w-xl text-ink/60">
            Create a hotel account for your team. Guests will find you on the public Booka site; your
            manager login opens Operations for this property only.
          </p>
        </div>

        <form onSubmit={handleSubmit} className="surface-elevated space-y-8 p-6 sm:p-8">
          {error && <InlineAlert tone="error">{error}</InlineAlert>}

          <section className="space-y-4">
            <p className="text-[11px] font-semibold uppercase tracking-[0.14em] text-moss">Property</p>
            <div>
              <label className="label-caps">Hotel name</label>
              <input
                required
                className="input-field"
                value={form.hotelName}
                onChange={(e) => update('hotelName', e.target.value)}
              />
            </div>
            <div className="grid gap-4 sm:grid-cols-2">
              <div>
                <label className="label-caps">City</label>
                <input
                  required
                  className="input-field"
                  value={form.city}
                  onChange={(e) => update('city', e.target.value)}
                />
              </div>
              <div>
                <label className="label-caps">Country</label>
                <input
                  required
                  className="input-field"
                  value={form.country}
                  onChange={(e) => update('country', e.target.value)}
                />
              </div>
            </div>
            <div>
              <label className="label-caps">Address</label>
              <input
                required
                className="input-field"
                value={form.address}
                onChange={(e) => update('address', e.target.value)}
              />
            </div>
            <div>
              <label className="label-caps">Short description</label>
              <textarea
                className="input-field min-h-24"
                value={form.description}
                onChange={(e) => update('description', e.target.value)}
              />
            </div>
            <div>
              <label className="label-caps">Amenities (comma-separated)</label>
              <input
                className="input-field"
                value={form.amenities}
                onChange={(e) => update('amenities', e.target.value)}
                placeholder="Wi-Fi, Pool, Breakfast"
              />
            </div>
            <div>
              <label className="label-caps">Cancellation policy</label>
              <textarea
                className="input-field min-h-20"
                value={form.cancellationPolicy}
                onChange={(e) => update('cancellationPolicy', e.target.value)}
              />
            </div>
          </section>

          <section className="space-y-4">
            <p className="text-[11px] font-semibold uppercase tracking-[0.14em] text-moss">
              Manager account
            </p>
            <div>
              <label className="label-caps">Full name</label>
              <input
                required
                className="input-field"
                value={form.managerFullName}
                onChange={(e) => update('managerFullName', e.target.value)}
              />
            </div>
            <div>
              <label className="label-caps">Work email</label>
              <input
                type="email"
                required
                className="input-field"
                value={form.managerEmail}
                onChange={(e) => update('managerEmail', e.target.value)}
                autoComplete="username"
              />
            </div>
            <div>
              <label className="label-caps">Password (min 6 characters)</label>
              <input
                type="password"
                required
                minLength={6}
                className="input-field"
                value={form.managerPassword}
                onChange={(e) => update('managerPassword', e.target.value)}
                autoComplete="new-password"
              />
            </div>
          </section>

          <div className="flex flex-wrap items-center gap-4">
            <button type="submit" className="btn-primary" disabled={loading}>
              {loading ? 'Creating…' : 'Create property & sign in'}
            </button>
            <Link to="/login" className="btn-ghost !px-0 text-sm">
              Already have a property login?
            </Link>
          </div>
        </form>
      </div>
    </div>
  );
}
