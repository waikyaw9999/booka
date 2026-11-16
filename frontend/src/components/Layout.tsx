import { Link, Outlet, useLocation } from 'react-router-dom';
import { useAuthStore } from '../stores';

export function Layout() {
  const { user, logout, isStaff } = useAuthStore();
  const location = useLocation();
  const isHome = location.pathname === '/';

  return (
    <div className="min-h-screen flex flex-col">
      <header
        className={`sticky top-0 z-40 transition-colors duration-300 ${
          isHome
            ? 'bg-ink/35 text-white backdrop-blur-xl border-b border-white/10'
            : 'bg-forest/95 text-white backdrop-blur-xl border-b border-white/10'
        }`}
      >
        <div className="mx-auto flex max-w-6xl items-center justify-between gap-4 px-4 py-3.5 sm:py-4">
          <Link to="/" className="group flex items-baseline gap-2">
            <span className="font-display text-2xl font-bold tracking-tight sm:text-[1.65rem]">
              Booka
            </span>
            <span className="hidden text-[10px] font-semibold uppercase tracking-[0.18em] text-gold/90 sm:inline">
              Stays
            </span>
          </Link>
          <nav className="flex items-center gap-4 text-sm font-medium sm:gap-6">
            <Link to="/#stays" className="opacity-90 transition hover:opacity-100">
              Stays
            </Link>
            {isStaff() && (
              <Link to="/admin" className="opacity-90 transition hover:opacity-100">
                Operations
              </Link>
            )}
            {user ? (
              <div className="flex items-center gap-3">
                <span className="hidden max-w-[10rem] truncate opacity-75 sm:inline">
                  {user.fullName}
                </span>
                <button
                  type="button"
                  onClick={logout}
                  className="rounded-xl border border-white/25 px-3 py-1.5 transition hover:bg-white/10"
                >
                  Logout
                </button>
              </div>
            ) : (
              <Link
                to="/login"
                className="rounded-xl bg-white px-3.5 py-1.5 font-semibold text-forest shadow-sm transition hover:bg-fog"
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

      <footer className="mt-auto border-t border-white/10 bg-forest text-sand/90">
        <div className="mx-auto grid max-w-6xl gap-8 px-4 py-12 sm:grid-cols-[1.2fr_1fr] sm:items-end">
          <div>
            <p className="font-display text-3xl font-bold text-white">Booka</p>
            <p className="mt-3 max-w-md text-sm leading-relaxed text-sand/75">
              Hotel booking and property operations in one calm surface — live inventory, clear
              rates, and desk-ready workflows.
            </p>
          </div>
          <div className="flex flex-col gap-2 text-sm sm:items-end sm:text-right">
            <p className="text-[11px] font-semibold uppercase tracking-[0.16em] text-gold/90">
              Guest promise
            </p>
            <p className="text-sand/80">Confirm now · Pay at property · Concierge on every stay</p>
            <Link to="/login" className="mt-2 text-white/90 underline-offset-4 hover:underline">
              Staff operations →
            </Link>
          </div>
        </div>
      </footer>
    </div>
  );
}
