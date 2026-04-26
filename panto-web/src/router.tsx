import { createBrowserRouter, Navigate } from 'react-router-dom';
import { ProtectedRoute } from './components/ProtectedRoute';
import { AppLayout } from './layouts/AppLayout';
import { AuditLogsPage } from './pages/AuditLogsPage';
import { BatchListPage } from './pages/BatchListPage';
import { ChangePasswordPage } from './pages/ChangePasswordPage';
import { CustomerDetailPage } from './pages/CustomerDetailPage';
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
import type { UserRole } from './types/auth';

const ALL_ROLES: UserRole[] = ['ADMIN', 'WAREHOUSE', 'MARKETING', 'ACCOUNTANT'];
const ADMIN_ONLY: UserRole[] = ['ADMIN'];
const ADMIN_WAREHOUSE: UserRole[] = ['ADMIN', 'WAREHOUSE'];
const ADMIN_MARKETING: UserRole[] = ['ADMIN', 'MARKETING'];
const ORDER_ROLES: UserRole[] = ['ADMIN', 'WAREHOUSE', 'MARKETING'];
const DESTRUCTION_VIEW_ROLES: UserRole[] = ['ADMIN', 'WAREHOUSE', 'ACCOUNTANT'];
const DESTRUCTION_CREATE_ROLES: UserRole[] = ['ADMIN', 'WAREHOUSE'];
const REPORT_ROLES: UserRole[] = ['ADMIN', 'ACCOUNTANT'];

export const router = createBrowserRouter([
  {
    path: '/login',
    element: <LoginPage />,
  },
  {
    element: <ProtectedRoute />,
    children: [
      { path: '/change-password', element: <ChangePasswordPage /> },
      {
        element: <AppLayout />,
        children: [
          { index: true, element: <Navigate to="/dashboard" replace /> },
          {
            element: <ProtectedRoute roles={ALL_ROLES} />,
            children: [
              { path: '/dashboard', element: <DashboardPage /> },
              { path: '/products', element: <ProductsPage /> },
              { path: '/inventory/stock', element: <StockSummaryPage /> },
              { path: '/inventory/batches', element: <BatchListPage /> },
              { path: '/inventory/transactions', element: <TransactionListPage /> },
            ],
          },
          {
            element: <ProtectedRoute roles={ADMIN_ONLY} />,
            children: [
              { path: '/audit-logs', element: <AuditLogsPage /> },
              { path: '/settings', element: <SettingsPage /> },
              { path: '/users', element: <UsersPage /> },
            ],
          },
          {
            element: <ProtectedRoute roles={ADMIN_MARKETING} />,
            children: [
              { path: '/customers', element: <CustomersPage /> },
              { path: '/customers/:id', element: <CustomerDetailPage /> },
            ],
          },
          {
            element: <ProtectedRoute roles={ORDER_ROLES} />,
            children: [
              { path: '/orders', element: <OrdersPage /> },
              { path: '/orders/new', element: <OrderCreatePage /> },
              { path: '/orders/:id', element: <OrderDetailPage /> },
            ],
          },
          {
            element: <ProtectedRoute roles={ADMIN_WAREHOUSE} />,
            children: [
              { path: '/inbound', element: <InboundListPage /> },
              { path: '/inbound/new', element: <InboundCreatePage /> },
              { path: '/inbound/:id', element: <InboundDetailPage /> },
            ],
          },
          {
            element: <ProtectedRoute roles={DESTRUCTION_VIEW_ROLES} />,
            children: [
              { path: '/destructions', element: <DestructionListPage /> },
              { path: '/destructions/:id', element: <DestructionDetailPage /> },
            ],
          },
          {
            element: <ProtectedRoute roles={DESTRUCTION_CREATE_ROLES} />,
            children: [
              { path: '/destructions/new', element: <DestructionCreatePage /> },
            ],
          },
          {
            element: <ProtectedRoute roles={REPORT_ROLES} />,
            children: [
              { path: '/reports', element: <ReportsPage /> },
            ],
          },
        ],
      },
    ],
  },
  {
    path: '*',
    element: <Navigate to="/dashboard" replace />,
  },
]);
