import { Link, Outlet, useLocation } from 'react-router-dom';
import { useAuthStore } from '../stores';

export function Layout() {
  const { user, logout, isStaff } = useAuthStore();
  const location = useLocation();
  const isHome = location.pathname === '/';

  return (
    <div className="flex min-h-screen flex-col">
      <header
        className={`sticky top-0 z-40 transition-colors duration-300 ${
          isHome
            ? 'border-b border-white/15 bg-ink/30 text-white backdrop-blur-xl'
            : 'border-b border-sand/90 bg-white/90 text-ink shadow-[0_8px_24px_-20px_rgba(28,50,44,0.35)] backdrop-blur-xl'
        }`}
      >
        <div className="mx-auto flex max-w-6xl items-center justify-between gap-4 px-4 py-3.5 sm:py-4">
          <Link to="/" className="group flex items-baseline gap-2">
            <span
              className={`font-display text-2xl font-bold tracking-tight sm:text-[1.65rem] ${
                isHome ? 'text-white' : 'text-forest'
              }`}
            >
              Booka
            </span>
            <span
              className={`hidden text-[10px] font-semibold uppercase tracking-[0.18em] sm:inline ${
                isHome ? 'text-gold/95' : 'text-moss'
              }`}
            >
              Stays
            </span>
          </Link>
          <nav
            className={`flex items-center gap-4 text-sm font-medium sm:gap-6 ${
              isHome ? 'text-white' : 'text-ink/75'
            }`}
          >
            <Link
              to="/#stays"
              className={`transition ${isHome ? 'opacity-90 hover:opacity-100' : 'hover:text-forest'}`}
            >
              Stays
            </Link>
            {isStaff() && (
              <Link
                to="/admin"
                className={`transition ${isHome ? 'opacity-90 hover:opacity-100' : 'hover:text-forest'}`}
              >
                Operations
              </Link>
            )}
            {user ? (
              <div className="flex items-center gap-3">
                <span className={`hidden max-w-[10rem] truncate sm:inline ${isHome ? 'opacity-75' : 'text-ink/55'}`}>
                  {user.fullName}
                </span>
                <button
                  type="button"
                  onClick={logout}
                  className={`rounded-xl px-3 py-1.5 transition ${
                    isHome
                      ? 'border border-white/25 hover:bg-white/10'
                      : 'border border-sand bg-fog/80 text-forest hover:border-lagoon/40'
                  }`}
                >
                  Logout
                </button>
              </div>
            ) : (
              <Link
                to="/login"
                className={`rounded-xl px-3.5 py-1.5 font-semibold shadow-sm transition ${
                  isHome
                    ? 'bg-white text-forest hover:bg-fog'
                    : 'bg-moss text-white hover:brightness-105'
                }`}
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

      <footer className="mt-auto border-t border-sand bg-fog text-ink">
        <div className="mx-auto grid max-w-6xl gap-8 px-4 py-12 sm:grid-cols-[1.2fr_1fr] sm:items-end">
          <div>
            <p className="font-display text-3xl font-bold text-forest">Booka</p>
            <p className="mt-3 max-w-md text-sm leading-relaxed text-ink/60">
              Hotel booking and property operations in one calm surface — live inventory, clear
              rates, and desk-ready workflows.
            </p>
          </div>
          <div className="flex flex-col gap-2 text-sm sm:items-end sm:text-right">
            <p className="text-[11px] font-semibold uppercase tracking-[0.16em] text-moss">
              Guest promise
            </p>
            <p className="text-ink/65">Confirm now · Pay at property · Concierge on every stay</p>
            <Link to="/login" className="mt-2 font-medium text-forest underline-offset-4 hover:underline">
              Staff operations →
            </Link>
          </div>
        </div>
      </footer>
    </div>
  );
}
