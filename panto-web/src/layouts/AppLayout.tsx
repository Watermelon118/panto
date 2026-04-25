import { NavLink, Outlet } from 'react-router-dom';
import { useAuthStore } from '../store/auth-store';
import type { UserRole } from '../types/auth';

interface NavItem {
  to: string;
  label: string;
  roles: UserRole[];
}

const NAV_ITEMS: NavItem[] = [
  { to: '/dashboard', label: 'Dashboard', roles: ['ADMIN', 'WAREHOUSE', 'MARKETING', 'ACCOUNTANT'] },
  { to: '/products', label: 'Products', roles: ['ADMIN', 'WAREHOUSE', 'MARKETING', 'ACCOUNTANT'] },
  { to: '/customers', label: 'Customers', roles: ['ADMIN', 'MARKETING'] },
  { to: '/inbound', label: 'Inbound', roles: ['ADMIN', 'WAREHOUSE', 'MARKETING', 'ACCOUNTANT'] },
  { to: '/inventory/stock', label: 'Stock Summary', roles: ['ADMIN', 'WAREHOUSE', 'MARKETING', 'ACCOUNTANT'] },
  { to: '/inventory/batches', label: 'Batches', roles: ['ADMIN', 'WAREHOUSE', 'MARKETING', 'ACCOUNTANT'] },
  { to: '/inventory/transactions', label: 'Transactions', roles: ['ADMIN', 'WAREHOUSE', 'MARKETING', 'ACCOUNTANT'] },
  { to: '/users', label: 'Users', roles: ['ADMIN'] },
];

export function AppLayout() {
  const user = useAuthStore((s) => s.user);
  const logout = useAuthStore((s) => s.logout);

  const visibleNav = NAV_ITEMS.filter(
    (item) => user?.role && item.roles.includes(user.role),
  );

  return (
    <div className="flex min-h-screen bg-stone-950 text-stone-100">
      <aside className="fixed inset-y-0 left-0 flex w-56 flex-col border-r border-white/10 bg-stone-900">
        <div className="border-b border-white/10 px-6 py-5">
          <p className="text-xs font-semibold tracking-[0.2em] text-amber-300 uppercase">
            Panto
          </p>
          <p className="mt-0.5 text-sm text-stone-400">Warehouse System</p>
        </div>

        <nav className="flex-1 space-y-1 px-3 py-4">
          {visibleNav.map((item) => (
            <NavLink
              key={item.to}
              to={item.to}
              className={({ isActive }) =>
                `block rounded-xl px-4 py-2.5 text-sm font-medium transition ${
                  isActive
                    ? 'bg-amber-300/15 text-amber-300'
                    : 'text-stone-400 hover:bg-white/5 hover:text-stone-100'
                }`
              }
            >
              {item.label}
            </NavLink>
          ))}
        </nav>
      </aside>

      <div className="ml-56 flex flex-1 flex-col">
        <header className="sticky top-0 z-10 flex h-14 items-center justify-end border-b border-white/10 bg-stone-900/60 px-6 backdrop-blur">
          <div className="flex items-center gap-3 text-sm text-stone-300">
            <span>
              {user?.username} · {user?.role}
            </span>
            <button
              type="button"
              onClick={logout}
              className="rounded-lg bg-white/10 px-3 py-1.5 text-xs font-semibold transition hover:bg-white/20"
            >
              Sign Out
            </button>
          </div>
        </header>

        <main className="flex-1 px-8 py-8">
          <Outlet />
        </main>
      </div>
    </div>
  );
}
