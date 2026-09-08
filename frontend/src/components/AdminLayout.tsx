import { useState } from 'react';
import { Link, Navigate, Outlet, useLocation } from 'react-router-dom';
import { useAuthStore, useRagStore } from '../stores';

const NAV = [
  { id: 'dashboard', label: 'Dashboard', path: '/admin' },
  { id: 'front-office', label: 'Front Office', path: '/admin?tab=front-office' },
  { id: 'inventory', label: 'Inventory', path: '/admin?tab=inventory' },
  { id: 'reservations', label: 'Reservations', path: '/admin?tab=reservations' },
  { id: 'housekeeping', label: 'Housekeeping', path: '/admin?tab=housekeeping' },
  { id: 'maintenance', label: 'Maintenance', path: '/admin?tab=maintenance' },
  { id: 'folios', label: 'Folios', path: '/admin?tab=folios' },
  { id: 'blackouts', label: 'Blackouts', path: '/admin?tab=blackouts' },
  { id: 'assistant', label: 'Assistant', path: '/admin?tab=assistant' },
] as const;

export function AdminLayout() {
  const { user, isStaff, logout } = useAuthStore();
  const { clearStaffMessages } = useRagStore();
  const location = useLocation();
  const [sidebarOpen, setSidebarOpen] = useState(false);

  if (!user || !isStaff()) {
    return <Navigate to="/login" replace />;
  }

  const params = new URLSearchParams(location.search);
  const activeTab = params.get('tab') ?? 'dashboard';

  function handleLogout() {
    clearStaffMessages();
    logout();
  }

  const sidebar = (
    <aside className="flex h-full w-64 shrink-0 flex-col border-r border-white/10 bg-forest text-sand">
      <div className="border-b border-white/10 px-5 py-5">
        <Link to="/" className="font-display text-2xl font-bold tracking-tight text-white">
          Booka
        </Link>
        <p className="mt-1 text-[11px] font-semibold uppercase tracking-[0.16em] text-gold/90">
          Operations
        </p>
        <p className="mt-3 truncate text-sm text-sand/75">
          {user.hotelId ? `Property #${user.hotelId}` : 'All properties'}
        </p>
      </div>

      <nav className="flex-1 space-y-0.5 overflow-y-auto px-3 py-4">
        {NAV.map((item) => {
          const active =
            item.id === 'dashboard' ? activeTab === 'dashboard' : activeTab === item.id;
          return (
            <Link
              key={item.id}
              to={item.path}
              onClick={() => setSidebarOpen(false)}
              className={`block rounded-xl px-3 py-2.5 text-sm font-medium transition ${
                active
                  ? 'bg-white/15 text-white'
                  : 'text-sand/75 hover:bg-white/8 hover:text-white'
              }`}
            >
              {item.label}
            </Link>
          );
        })}
      </nav>

      <div className="border-t border-white/10 px-4 py-4">
        <p className="truncate text-sm font-medium text-white">{user.fullName}</p>
        <p className="truncate text-xs text-sand/55">{user.email}</p>
        <div className="mt-3 flex gap-2">
          <Link to="/" className="btn-ghost !px-2 !py-1.5 text-xs text-sand/80 hover:bg-white/10">
            Guest site
          </Link>
          <button
            type="button"
            onClick={handleLogout}
            className="btn-ghost !px-2 !py-1.5 text-xs text-gold hover:bg-white/10"
          >
            Sign out
          </button>
        </div>
      </div>
    </aside>
  );

  return (
    <div className="flex min-h-screen ops-canvas">
      {/* Desktop sidebar — sticky so all NAV items stay reachable via overflow scroll */}
      <div className="sticky top-0 hidden h-screen shrink-0 lg:block">{sidebar}</div>

      {/* Mobile drawer */}
      {sidebarOpen && (
        <div className="fixed inset-0 z-50 flex lg:hidden">
          <button
            type="button"
            className="absolute inset-0 bg-ink/50"
            aria-label="Close menu"
            onClick={() => setSidebarOpen(false)}
          />
          <div className="relative z-10 h-full animate-panel shadow-2xl">{sidebar}</div>
        </div>
      )}

      <div className="flex min-w-0 flex-1 flex-col">
        <header className="sticky top-0 z-30 flex items-center gap-3 border-b border-sand/70 bg-mist/90 px-4 py-3 backdrop-blur-md lg:hidden">
          <button
            type="button"
            onClick={() => setSidebarOpen(true)}
            className="btn-secondary !px-3 !py-2 text-xs"
          >
            Menu
          </button>
          <div className="min-w-0">
            <p className="font-display text-lg font-bold text-forest">Operations</p>
            <p className="truncate text-xs text-ink/50">{user.fullName}</p>
          </div>
        </header>
        <main className="flex-1 overflow-x-hidden p-4 sm:p-6 lg:p-8">
          <Outlet />
        </main>
      </div>
    </div>
  );
}
