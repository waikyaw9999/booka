import { useState } from 'react';
import { Link, Navigate, Outlet, useLocation } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import { api } from '../lib/api';
import { useAuthStore, useRagStore } from '../stores';

const NAV = [
  { id: 'dashboard', label: 'Dashboard', path: '/admin' },
  { id: 'front-office', label: 'Front Office', path: '/admin?tab=front-office' },
  { id: 'inventory', label: 'Inventory', path: '/admin?tab=inventory' },
  { id: 'reservations', label: 'Reservations', path: '/admin?tab=reservations' },
  { id: 'guests', label: 'Guests', path: '/admin?tab=guests' },
  { id: 'requests', label: 'Requests', path: '/admin?tab=requests' },
  { id: 'housekeeping', label: 'Housekeeping', path: '/admin?tab=housekeeping' },
  { id: 'maintenance', label: 'Maintenance', path: '/admin?tab=maintenance' },
  { id: 'folios', label: 'Folios', path: '/admin?tab=folios' },
  { id: 'night-audit', label: 'Night audit', path: '/admin?tab=night-audit' },
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

  const { data: hotel } = useQuery({
    queryKey: ['hotel', user.hotelId],
    queryFn: () => api.getHotel(user.hotelId!),
    enabled: user.hotelId != null,
  });

  const params = new URLSearchParams(location.search);
  const activeTab = params.get('tab') ?? 'dashboard';
  const propertyLabel = hotel?.name ?? (user.hotelId ? `Property #${user.hotelId}` : 'All properties');

  function handleLogout() {
    clearStaffMessages();
    logout();
  }

  const sidebar = (
    <aside className="flex h-full w-64 shrink-0 flex-col border-r border-sand/90 bg-white text-ink shadow-[4px_0_24px_-20px_rgba(28,50,44,0.35)]">
      <div className="border-b border-sand/80 px-5 py-5">
        <Link to="/" className="font-display text-2xl font-bold tracking-tight text-forest">
          Booka
        </Link>
        <p className="mt-1 text-[11px] font-semibold uppercase tracking-[0.16em] text-moss">
          Operations
        </p>
        <p className="mt-3 truncate text-sm text-ink/60">{propertyLabel}</p>
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
                  ? 'bg-moss/12 text-forest shadow-sm ring-1 ring-moss/20'
                  : 'text-ink/65 hover:bg-fog hover:text-forest'
              }`}
            >
              {item.label}
            </Link>
          );
        })}
      </nav>

      <div className="border-t border-sand/80 px-4 py-4">
        <p className="truncate text-sm font-medium text-ink">{user.fullName}</p>
        <p className="truncate text-xs text-ink/50">{user.email}</p>
        <div className="mt-3 flex gap-2">
          <Link to="/" className="btn-ghost !px-2 !py-1.5 text-xs">
            Guest site
          </Link>
          <button
            type="button"
            onClick={handleLogout}
            className="btn-ghost !px-2 !py-1.5 text-xs text-ink/55"
          >
            Sign out
          </button>
        </div>
      </div>
    </aside>
  );

  return (
    <div className="ops-canvas flex min-h-screen">
      <div className="sticky top-0 hidden h-screen shrink-0 lg:block">{sidebar}</div>

      {sidebarOpen && (
        <div className="fixed inset-0 z-50 flex lg:hidden">
          <button
            type="button"
            className="absolute inset-0 bg-ink/25 backdrop-blur-[2px]"
            aria-label="Close menu"
            onClick={() => setSidebarOpen(false)}
          />
          <div className="relative z-10 h-full animate-panel shadow-2xl">{sidebar}</div>
        </div>
      )}

      <div className="flex min-w-0 flex-1 flex-col">
        <header className="sticky top-0 z-30 flex items-center gap-3 border-b border-sand/80 bg-white/90 px-4 py-3 backdrop-blur-md lg:hidden">
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
