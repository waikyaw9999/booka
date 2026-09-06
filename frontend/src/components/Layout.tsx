import { Link, Outlet, useLocation } from 'react-router-dom';
import { useAuthStore } from '../stores';

export function Layout() {
  const { user, logout, isStaff } = useAuthStore();
  const location = useLocation();
  const isHome = location.pathname === '/';

  return (
    <div className="min-h-screen flex flex-col">
      <header
        className={`sticky top-0 z-40 transition-colors ${
          isHome
            ? 'bg-ink/40 text-white backdrop-blur-md border-b border-white/10'
            : 'bg-forest/95 text-white backdrop-blur-md border-b border-white/10'
        }`}
      >
        <div className="max-w-6xl mx-auto px-4 py-4 flex items-center justify-between gap-4">
          <Link to="/" className="font-display text-2xl font-bold tracking-tight">
            Booka
          </Link>
          <nav className="flex items-center gap-5 text-sm font-medium">
            <Link to="/" className="opacity-90 hover:opacity-100 transition">
              Stays
            </Link>
            {isStaff() && (
              <Link to="/admin" className="opacity-90 hover:opacity-100 transition">
                Operations
              </Link>
            )}
            {user ? (
              <div className="flex items-center gap-3">
                <span className="hidden sm:inline opacity-75">{user.fullName}</span>
                <button
                  onClick={logout}
                  className="rounded-xl border border-white/25 px-3 py-1.5 hover:bg-white/10 transition"
                >
                  Logout
                </button>
              </div>
            ) : (
              <Link
                to="/login"
                className="rounded-xl bg-white text-forest px-3 py-1.5 font-semibold hover:bg-sand transition"
              >
                Staff
              </Link>
            )}
          </nav>
        </div>
      </header>

      <main className="flex-1">
        <Outlet />
      </main>

      <footer className="mt-auto border-t border-sand bg-forest text-sand/90">
        <div className="max-w-6xl mx-auto px-4 py-8 flex flex-col sm:flex-row sm:items-center sm:justify-between gap-3">
          <p className="font-display text-xl text-white">Booka</p>
          <p className="text-sm opacity-80">Quiet stays. Clear booking. Real-time help.</p>
        </div>
      </footer>
    </div>
  );
}
