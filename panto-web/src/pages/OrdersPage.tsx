import { useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useCustomers } from '../api/customers';
import { useOrders } from '../api/orders';
import { Pagination } from '../components/Pagination';
import type { OrderStatus } from '../types/order';
import { extractErrorMessage } from '../utils/error';

const inputCls =
  'w-full rounded-xl border border-white/10 bg-white/5 px-4 py-2.5 text-sm text-stone-100 outline-none transition focus:border-amber-300/50 focus:ring-2 focus:ring-amber-300/20 placeholder:text-stone-500';

const selectCls =
  'rounded-xl border border-white/10 bg-stone-900 px-4 py-2.5 text-sm text-stone-100 outline-none focus:border-amber-300/50';

function formatCurrency(value: number) {
  return `$${value.toFixed(2)}`;
}

function formatDateTime(value: string) {
  return new Intl.DateTimeFormat('en-NZ', {
    dateStyle: 'medium',
    timeStyle: 'short',
  }).format(new Date(value));
}

export function OrdersPage() {
  const navigate = useNavigate();

  const [page, setPage] = useState(0);
  const [customerId, setCustomerId] = useState<number | undefined>(undefined);
  const [status, setStatus] = useState<OrderStatus | undefined>(undefined);
  const [dateFrom, setDateFrom] = useState('');
  const [dateTo, setDateTo] = useState('');

  const customersQuery = useCustomers({
    active: true,
    page: 0,
    size: 100,
  });

  const ordersQuery = useOrders({
    customerId,
    status,
    dateFrom: dateFrom || undefined,
    dateTo: dateTo || undefined,
    page,
    size: 20,
  });

  const customerOptions = customersQuery.data?.items ?? [];
  const ordersErrorMessage = useMemo(
    () => (ordersQuery.error ? extractErrorMessage(ordersQuery.error) : null),
    [ordersQuery.error],
  );

  return (
    <div className="mx-auto max-w-6xl space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <p className="text-xs font-semibold tracking-[0.18em] text-amber-300 uppercase">
            Sales
          </p>
          <h1 className="mt-1 text-3xl font-black tracking-tight">Orders</h1>
        </div>
        <button
          type="button"
          onClick={() => navigate('/orders/new')}
          className="rounded-xl bg-amber-300 px-5 py-2.5 text-sm font-semibold text-stone-900 transition hover:bg-amber-200"
        >
          + New Order
        </button>
      </div>

      <div className="grid gap-3 md:grid-cols-4">
        <select
          value={customerId ?? ''}
          onChange={(e) => {
            setCustomerId(e.target.value ? Number(e.target.value) : undefined);
            setPage(0);
          }}
          className={selectCls}
          disabled={customersQuery.isLoading || !!customersQuery.error}
        >
          <option value="">All customers</option>
          {customerOptions.map((customer) => (
            <option key={customer.id} value={customer.id}>
              {customer.companyName}
            </option>
          ))}
        </select>
        <select
          value={status ?? ''}
          onChange={(e) => {
            setStatus((e.target.value || undefined) as OrderStatus | undefined);
            setPage(0);
          }}
          className={selectCls}
        >
          <option value="">All status</option>
          <option value="ACTIVE">Active</option>
          <option value="ROLLED_BACK">Rolled Back</option>
        </select>
        <input
          type="date"
          value={dateFrom}
          onChange={(e) => {
            setDateFrom(e.target.value);
            setPage(0);
          }}
          className={inputCls}
        />
        <input
          type="date"
          value={dateTo}
          onChange={(e) => {
            setDateTo(e.target.value);
            setPage(0);
          }}
          className={inputCls}
        />
      </div>

      {customersQuery.error && (
        <div className="rounded-2xl border border-amber-300/20 bg-amber-300/10 px-5 py-4 text-sm leading-6 text-amber-100">
          Customer filter options are temporarily unavailable. You can still review orders and try refreshing the page.
        </div>
      )}

      {ordersErrorMessage && (
        <p className="rounded-xl bg-red-900/30 px-4 py-3 text-sm text-red-400">{ordersErrorMessage}</p>
      )}

      <div className="overflow-hidden rounded-[1.75rem] border border-white/10 bg-white/5">
        {ordersQuery.isLoading ? (
          <div className="p-12 text-center text-sm text-stone-500">Loading...</div>
        ) : ordersQuery.data?.items.length === 0 ? (
          <div className="p-12 text-center text-sm text-stone-500">No orders found.</div>
        ) : (
          <table className="w-full text-sm">
            <thead>
              <tr className="border-b border-white/10 text-left text-xs font-semibold tracking-wider text-stone-400 uppercase">
                <th className="px-6 py-4">Order No.</th>
                <th className="px-6 py-4">Customer</th>
                <th className="px-6 py-4">Status</th>
                <th className="px-6 py-4 text-right">Items</th>
                <th className="px-6 py-4 text-right">Total</th>
                <th className="px-6 py-4">Created</th>
                <th className="px-6 py-4 text-right">Actions</th>
              </tr>
            </thead>
            <tbody>
              {ordersQuery.data?.items.map((order) => (
                <tr key={order.id} className="border-b border-white/5 transition hover:bg-white/[0.03]">
                  <td className="px-6 py-4 font-mono text-xs text-amber-300">{order.orderNumber}</td>
                  <td className="px-6 py-4 font-medium text-stone-100">{order.customerCompanyName}</td>
                  <td className="px-6 py-4">
                    <span
                      className={`rounded-full px-3 py-1 text-xs font-semibold ${
                        order.status === 'ACTIVE'
                          ? 'bg-emerald-400/15 text-emerald-400'
                          : 'bg-stone-700 text-stone-300'
                      }`}
                    >
                      {order.status === 'ACTIVE' ? 'Active' : 'Rolled Back'}
                    </span>
                  </td>
                  <td className="px-6 py-4 text-right text-stone-300">{order.itemCount}</td>
                  <td className="px-6 py-4 text-right font-medium text-stone-100">
                    {formatCurrency(order.totalAmount)}
                  </td>
                  <td className="px-6 py-4 text-stone-400">{formatDateTime(order.createdAt)}</td>
                  <td className="px-6 py-4 text-right">
                    <button
                      type="button"
                      onClick={() => navigate(`/orders/${order.id}`)}
                      className="rounded-lg border border-white/10 px-3 py-1.5 text-xs font-medium text-stone-300 transition hover:bg-white/10 hover:text-stone-100"
                    >
                      View Details
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>

      {ordersQuery.data && ordersQuery.data.totalPages > 1 && (
        <Pagination
          page={ordersQuery.data.page}
          totalPages={ordersQuery.data.totalPages}
          totalElements={ordersQuery.data.totalElements}
          size={ordersQuery.data.size}
          onPageChange={setPage}
        />
      )}
    </div>
  );
}
