import { createBrowserRouter, Navigate } from 'react-router-dom';
import { ProtectedRoute } from './components/ProtectedRoute';
import { AppLayout } from './layouts/AppLayout';
import { BatchListPage } from './pages/BatchListPage';
import { CustomersPage } from './pages/CustomersPage';
import { DashboardPage } from './pages/DashboardPage';
import { DestructionCreatePage } from './pages/DestructionCreatePage';
import { DestructionDetailPage } from './pages/DestructionDetailPage';
import { DestructionListPage } from './pages/DestructionListPage';
import { InboundCreatePage } from './pages/InboundCreatePage';
import { InboundDetailPage } from './pages/InboundDetailPage';
import { InboundListPage } from './pages/InboundListPage';
import { LoginPage } from './pages/LoginPage';
import { OrderCreatePage } from './pages/OrderCreatePage';
import { OrderDetailPage } from './pages/OrderDetailPage';
import { OrdersPage } from './pages/OrdersPage';
import { ProductsPage } from './pages/ProductsPage';
import { ReportsPage } from './pages/ReportsPage';
import { SettingsPage } from './pages/SettingsPage';
import { StockSummaryPage } from './pages/StockSummaryPage';
import { TransactionListPage } from './pages/TransactionListPage';
import { UsersPage } from './pages/UsersPage';

export const router = createBrowserRouter([
  {
    path: '/login',
    element: <LoginPage />,
  },
  {
    element: <ProtectedRoute />,
    children: [
      {
        element: <AppLayout />,
        children: [
          { index: true, element: <Navigate to="/dashboard" replace /> },
          { path: '/dashboard', element: <DashboardPage /> },
          { path: '/products', element: <ProductsPage /> },
          { path: '/customers', element: <CustomersPage /> },
          { path: '/orders', element: <OrdersPage /> },
          { path: '/orders/new', element: <OrderCreatePage /> },
          { path: '/orders/:id', element: <OrderDetailPage /> },
          { path: '/inbound', element: <InboundListPage /> },
          { path: '/inbound/new', element: <InboundCreatePage /> },
          { path: '/inbound/:id', element: <InboundDetailPage /> },
          { path: '/inventory/stock', element: <StockSummaryPage /> },
          { path: '/inventory/batches', element: <BatchListPage /> },
          { path: '/inventory/transactions', element: <TransactionListPage /> },
          { path: '/destructions', element: <DestructionListPage /> },
          { path: '/destructions/new', element: <DestructionCreatePage /> },
          { path: '/destructions/:id', element: <DestructionDetailPage /> },
          { path: '/reports', element: <ReportsPage /> },
          { path: '/settings', element: <SettingsPage /> },
          { path: '/users', element: <UsersPage /> },
        ],
      },
    ],
  },
  {
    path: '*',
    element: <Navigate to="/dashboard" replace />,
  },
]);
