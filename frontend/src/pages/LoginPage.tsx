import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { api, getErrorMessage } from '../lib/api';
import { useAuthStore } from '../stores';
import type { UserRole } from '../types';
import { InlineAlert } from '../components/ui';

export function LoginPage() {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);
  const { setUser } = useAuthStore();
  const navigate = useNavigate();

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    setLoading(true);
    setError('');
    try {
      const res = await api.login(email, password);
      setUser({
        userId: res.userId,
        email: res.email,
        fullName: res.fullName,
        role: res.role as UserRole,
        hotelId: res.hotelId,
        token: res.token,
      });
      navigate(res.role === 'GUEST' ? '/' : '/admin');
    } catch (err) {
      setError(getErrorMessage(err, 'Unable to sign in. Please try again.'));
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="relative min-h-[70vh] overflow-hidden">
      <div className="pointer-events-none absolute inset-0 bg-[radial-gradient(ellipse_60%_50%_at_80%_0%,rgba(61,139,122,0.14),transparent)]" />
      <div className="relative mx-auto grid max-w-5xl gap-10 px-4 py-16 lg:grid-cols-[1.1fr_0.9fr] lg:items-center lg:py-24">
        <div className="animate-rise space-y-4">
          <p className="font-display text-5xl font-bold text-forest">Booka</p>
          <h1 className="font-display text-3xl font-semibold text-ink sm:text-4xl">
            Operations portal
          </h1>
          <p className="max-w-md text-ink/60">
            Sign in for front office, housekeeping, folios, and the staff assistant — separate from
            the guest booking experience.
          </p>
          <p className="text-sm text-ink/45">
            Demo credentials: <span className="font-medium text-ink/70">admin@booka.com</span> /{' '}
            <span className="font-medium text-ink/70">password</span>
          </p>
          <Link to="/" className="btn-ghost inline-flex !px-0 text-sm">
            ← Back to guest site
          </Link>
        </div>

        <form onSubmit={handleSubmit} className="surface-elevated animate-rise-delay space-y-4 p-6 sm:p-8">
          <p className="text-[11px] font-semibold uppercase tracking-[0.14em] text-moss">
            Staff sign-in
          </p>
          <div>
            <label className="label-caps">Email</label>
            <input
              type="email"
              required
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              className="input-field"
              autoComplete="username"
            />
          </div>
          <div>
            <label className="label-caps">Password</label>
            <input
              type="password"
              required
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              className="input-field"
              autoComplete="current-password"
            />
          </div>
          {error && <InlineAlert tone="error">{error}</InlineAlert>}
          <button type="submit" disabled={loading} className="btn-primary w-full">
            {loading ? 'Signing in…' : 'Enter operations'}
          </button>
        </form>
      </div>
    </div>
  );
}
