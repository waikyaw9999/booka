import { Link, Outlet } from 'react-router-dom';
import { useQueryClient } from '@tanstack/react-query';
import { useAuthStore, useRagStore } from '../stores';

export function Layout() {
  const { user, logout, isStaff } = useAuthStore();
  const clearStaffMessages = useRagStore((s) => s.clearStaffMessages);
  const queryClient = useQueryClient();

  function handleLogout() {
    clearStaffMessages();
    queryClient.clear();
    logout();
  }

  return (
    <div className="flex min-h-screen flex-col">
      <header className="sticky top-0 z-40 border-b border-sand/90 bg-white/90 text-ink shadow-[0_8px_24px_-20px_rgba(28,50,44,0.35)] backdrop-blur-xl transition-colors duration-300">
        <div className="mx-auto flex max-w-6xl items-center justify-between gap-4 px-4 py-3.5 sm:py-4">
          <Link to="/" className="group flex items-baseline gap-2">
            <span className="font-display text-2xl font-bold tracking-tight text-forest sm:text-[1.65rem]">
              Booka
            </span>
            <span className="hidden text-[10px] font-semibold uppercase tracking-[0.18em] text-moss sm:inline">
              Stays
            </span>
          </Link>
          <nav className="flex items-center gap-4 text-sm font-medium text-ink/75 sm:gap-6">
            <Link to="/#stays" className="transition hover:text-forest">
              Stays
            </Link>
            {isStaff() && (
              <Link to="/admin" className="transition hover:text-forest">
                Operations
              </Link>
            )}
            {user ? (
              <div className="flex items-center gap-3">
                <span className="hidden max-w-[10rem] truncate text-ink/55 sm:inline">{user.fullName}</span>
                <button
                  type="button"
                  onClick={handleLogout}
                  className="rounded-xl border border-sand bg-fog/80 px-3 py-1.5 text-forest transition hover:border-lagoon/40"
                >
                  Logout
                </button>
              </div>
            ) : (
              <Link
                to="/login"
                className="rounded-xl bg-moss px-3.5 py-1.5 font-semibold text-white shadow-sm transition hover:brightness-105"
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
            <Link
              to="/register-property"
              className="font-medium text-forest/80 underline-offset-4 hover:underline"
            >
              List your property
            </Link>
          </div>
        </div>
      </footer>
    </div>
  );
}
