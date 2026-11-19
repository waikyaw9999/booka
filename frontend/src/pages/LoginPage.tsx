import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { api, getErrorMessage } from '../lib/api';
import { useAuthStore } from '../stores';
import type { UserRole } from '../types';
import { InlineAlert } from '../components/ui';

const DEMO_PROPERTIES = [
  { property: 'Booka Grand Hotel', manager: 'admin@booka.com', desk: 'staff@booka.com' },
  { property: 'Harbourview Suites', manager: 'harbour.admin@booka.com', desk: 'harbour.staff@booka.com' },
  { property: 'Inle Lake Lodge', manager: 'inle.admin@booka.com', desk: 'inle.staff@booka.com' },
  { property: 'Bagan Sandstone Inn', manager: 'bagan.admin@booka.com', desk: 'bagan.staff@booka.com' },
  { property: 'Mandalay Heritage House', manager: 'mandalay.admin@booka.com', desk: 'mandalay.staff@booka.com' },
] as const;

export function LoginPage() {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);
  const { setUser } = useAuthStore();
  const navigate = useNavigate();

  async function signIn(nextEmail: string, nextPassword: string) {
    setEmail(nextEmail);
    setPassword(nextPassword);
    setLoading(true);
    setError('');
    try {
      const res = await api.login(nextEmail, nextPassword);
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

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    await signIn(email, password);
  }

  return (
    <div className="relative min-h-[70vh] overflow-hidden">
      <div className="pointer-events-none absolute inset-0 bg-[radial-gradient(ellipse_60%_50%_at_80%_0%,rgba(58,133,118,0.16),transparent)]" />
      <div className="relative mx-auto grid max-w-5xl gap-10 px-4 py-16 lg:grid-cols-[1.1fr_0.9fr] lg:items-center lg:py-24">
        <div className="animate-rise space-y-4">
          <p className="font-display text-5xl font-bold tracking-tight text-forest">Booka</p>
          <h1 className="font-display text-3xl font-semibold text-ink sm:text-4xl">
            Operations portal
          </h1>
          <p className="max-w-md leading-relaxed text-ink/60">
            Front office, housekeeping, folios, and the staff assistant — separate from the guest
            booking experience.
          </p>
          {import.meta.env.DEV && (
            <div className="max-w-md space-y-2 text-sm text-ink/55">
              <p>
                Demo password for every property:{' '}
                <span className="font-medium text-ink/70">password</span>
              </p>
              <ul className="space-y-1.5">
                {DEMO_PROPERTIES.map((entry) => (
                  <li key={entry.property} className="flex flex-wrap items-baseline gap-x-3 gap-y-1">
                    <span className="font-medium text-ink/75">{entry.property}</span>
                    <button
                      type="button"
                      className="text-forest underline-offset-4 hover:underline"
                      onClick={() => signIn(entry.manager, 'password')}
                    >
                      Manager
                    </button>
                    <button
                      type="button"
                      className="text-forest underline-offset-4 hover:underline"
                      onClick={() => signIn(entry.desk, 'password')}
                    >
                      Front desk
                    </button>
                  </li>
                ))}
              </ul>
            </div>
          )}
          <div className="flex flex-wrap gap-x-4 gap-y-2 text-sm">
            <Link to="/register-property" className="text-forest underline-offset-4 hover:underline">
              List your property
            </Link>
            <Link to="/" className="btn-ghost inline-flex !px-0">
              ← Back to guest site
            </Link>
          </div>
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
