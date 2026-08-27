import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { api } from '../lib/api';
import { useAuthStore } from '../stores';
import type { UserRole } from '../types';

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
      setError(err instanceof Error ? err.message : 'Login failed');
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="mx-auto max-w-md px-4 py-16">
      <div className="mb-8 text-center">
        <p className="font-display text-4xl font-bold text-forest">Booka</p>
        <h1 className="mt-3 font-display text-2xl font-semibold text-ink">Staff access</h1>
        <p className="mt-2 text-sm text-ink/55">Demo: admin@booka.com / password</p>
      </div>
      <form onSubmit={handleSubmit} className="surface space-y-4 p-6">
        <div>
          <label className="mb-1 block text-xs font-semibold uppercase tracking-wide text-ink/55">
            Email
          </label>
          <input
            type="email"
            required
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            className="input-field"
          />
        </div>
        <div>
          <label className="mb-1 block text-xs font-semibold uppercase tracking-wide text-ink/55">
            Password
          </label>
          <input
            type="password"
            required
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            className="input-field"
          />
        </div>
        {error && <p className="text-sm text-red-700">{error}</p>}
        <button type="submit" disabled={loading} className="btn-primary w-full">
          {loading ? 'Signing in...' : 'Sign in'}
        </button>
      </form>
    </div>
  );
}
