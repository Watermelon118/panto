import { useEffect, useState } from 'react';
import { NavLink, Outlet, useNavigate } from 'react-router-dom';
import { useDashboardSummary } from '../api/dashboard';
import { WarningBadge } from '../components/WarningBadge';
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
  { to: '/orders', label: 'Orders', roles: ['ADMIN', 'WAREHOUSE', 'MARKETING'] },
  { to: '/inbound', label: 'Inbound', roles: ['ADMIN', 'WAREHOUSE', 'MARKETING', 'ACCOUNTANT'] },
  { to: '/inventory/stock', label: 'Stock Summary', roles: ['ADMIN', 'WAREHOUSE', 'MARKETING', 'ACCOUNTANT'] },
  { to: '/inventory/batches', label: 'Batches', roles: ['ADMIN', 'WAREHOUSE', 'MARKETING', 'ACCOUNTANT'] },
  { to: '/inventory/transactions', label: 'Transactions', roles: ['ADMIN', 'WAREHOUSE', 'MARKETING', 'ACCOUNTANT'] },
  { to: '/destructions', label: 'Destructions', roles: ['ADMIN', 'WAREHOUSE', 'ACCOUNTANT'] },
  { to: '/reports', label: 'Reports', roles: ['ADMIN', 'ACCOUNTANT'] },
  { to: '/settings', label: 'Settings', roles: ['ADMIN'] },
  { to: '/users', label: 'Users', roles: ['ADMIN'] },
];

function getWarningTarget(
  expiringSoonCount: number,
  expiredCount: number,
  lowStockCount: number,
) {
  if (expiringSoonCount > 0) {
    return '/inventory/batches?status=EXPIRING_SOON';
  }
  if (expiredCount > 0) {
    return '/destructions';
  }
  if (lowStockCount > 0) {
    return '/inventory/stock';
  }
  return '/dashboard';
}

export function AppLayout() {
  const navigate = useNavigate();
  const user = useAuthStore((s) => s.user);
  const logout = useAuthStore((s) => s.logout);
  const dashboardQuery = useDashboardSummary();
  const [warningToastOpen, setWarningToastOpen] = useState(false);

  const visibleNav = NAV_ITEMS.filter(
    (item) => user?.role && item.roles.includes(user.role),
  );
  const expiryWarningCount = dashboardQuery.data?.warnings.expiringSoonCount ?? 0;
  const expiredCount = dashboardQuery.data?.warnings.expiredCount ?? 0;
  const lowStockCount = dashboardQuery.data?.warnings.lowStockCount ?? 0;
  const totalWarningCount = expiryWarningCount + expiredCount + lowStockCount;
  const warningTarget = getWarningTarget(expiryWarningCount, expiredCount, lowStockCount);

  const handleLogout = async () => {
    await logout();
    navigate('/login', { replace: true });
  };

  useEffect(() => {
    if (!user?.username || !dashboardQuery.data || totalWarningCount <= 0) {
      return;
    }

    const storageKey = `panto-warning-toast:${user.username}`;
    const hasShown = sessionStorage.getItem(storageKey);

    if (!hasShown) {
      sessionStorage.setItem(storageKey, 'shown');
      setWarningToastOpen(true);
    }
  }, [dashboardQuery.data, totalWarningCount, user?.username]);

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
            <WarningBadge
              count={expiryWarningCount}
              onClick={() => navigate('/inventory/batches?status=EXPIRING_SOON')}
            />
            <span>{user?.username} | {user?.role}</span>
            {/*
              {user?.username} · {user?.role}
            */}
            <button
              type="button"
              onClick={() => void handleLogout()}
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

      {warningToastOpen && (
        <div className="fixed right-6 top-20 z-50 w-full max-w-sm rounded-[1.5rem] border border-amber-300/20 bg-stone-900/95 p-5 shadow-[0_24px_60px_rgba(0,0,0,0.45)] backdrop-blur">
          <div className="flex items-start justify-between gap-4">
            <div>
              <p className="text-xs font-semibold tracking-[0.14em] text-amber-300 uppercase">
                Login Warning
              </p>
              <h2 className="mt-2 text-lg font-semibold text-white">
                {totalWarningCount} active warning{totalWarningCount > 1 ? 's' : ''}
              </h2>
            </div>
            <button
              type="button"
              onClick={() => setWarningToastOpen(false)}
              className="rounded-lg p-1.5 text-stone-500 transition hover:bg-white/10 hover:text-stone-200"
            >
              X
            </button>
          </div>

          <div className="mt-4 space-y-2 text-sm text-stone-300">
            <p>Low stock: {lowStockCount}</p>
            <p>Expiring soon: {expiryWarningCount}</p>
            <p>Expired: {expiredCount}</p>
          </div>

          <div className="mt-5 flex gap-2">
            <button
              type="button"
              onClick={() => {
                navigate(warningTarget);
                setWarningToastOpen(false);
              }}
              className="rounded-xl bg-amber-300 px-4 py-2 text-sm font-semibold text-stone-900 transition hover:bg-amber-200"
            >
              Review now
            </button>
            <button
              type="button"
              onClick={() => setWarningToastOpen(false)}
              className="rounded-xl border border-white/10 px-4 py-2 text-sm font-medium text-stone-400 transition hover:bg-white/10 hover:text-stone-100"
            >
              Dismiss
            </button>
          </div>
        </div>
      )}
    </div>
  );
}
