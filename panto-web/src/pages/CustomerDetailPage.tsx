import type { ReactNode } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { useCustomer } from '../api/customers';
import { extractErrorMessage } from '../utils/error';

function formatCurrency(value: number) {
  return `$${value.toFixed(2)}`;
}

function formatDateTime(value: string) {
  return new Intl.DateTimeFormat('en-NZ', {
    dateStyle: 'medium',
    timeStyle: 'short',
  }).format(new Date(value));
}

function Field({ label, value }: { label: string; value: ReactNode }) {
  return (
    <div>
      <p className="text-xs font-medium text-stone-400">{label}</p>
      <div className="mt-1 text-sm text-stone-200">{value}</div>
    </div>
  );
}

export function CustomerDetailPage() {
  const navigate = useNavigate();
  const { id } = useParams<{ id: string }>();
  const customerId = Number(id);

  const customerQuery = useCustomer(Number.isFinite(customerId) ? customerId : null);
  const customer = customerQuery.data;
  const errorMessage = customerQuery.error ? extractErrorMessage(customerQuery.error) : null;

  if (customerQuery.isLoading) {
    return <div className="p-12 text-center text-sm text-stone-500">Loading...</div>;
  }

  if (!customer) {
    return (
      <div className="space-y-4 p-12 text-center text-sm text-stone-500">
        <p>{errorMessage ?? 'Customer not found.'}</p>
        <button
          type="button"
          onClick={() => navigate('/customers')}
          className="rounded-xl border border-white/10 px-4 py-2.5 text-stone-300 transition hover:bg-white/5 hover:text-stone-100"
        >
          Back to Customers
        </button>
      </div>
    );
  }

  return (
    <div className="mx-auto max-w-6xl space-y-8">
      <div className="flex flex-wrap items-start justify-between gap-4">
        <div className="flex items-center gap-4">
          <button
            type="button"
            onClick={() => navigate('/customers')}
            className="rounded-xl border border-white/10 px-4 py-2.5 text-sm text-stone-400 transition hover:bg-white/5 hover:text-stone-100"
          >
            Back
          </button>
          <div>
            <p className="text-xs font-semibold tracking-[0.18em] text-amber-300 uppercase">
              Customers
            </p>
            <h1 className="mt-1 text-3xl font-black tracking-tight">{customer.companyName}</h1>
            <p className="mt-2 text-sm text-stone-400">
              Updated {formatDateTime(customer.updatedAt)}
            </p>
          </div>
        </div>
        <span
          className={`rounded-full px-4 py-2 text-sm font-semibold ${
            customer.active
              ? 'bg-emerald-400/15 text-emerald-400'
              : 'bg-stone-700 text-stone-300'
          }`}
        >
          {customer.active ? 'Active' : 'Inactive'}
        </span>
      </div>

      {errorMessage && (
        <p className="rounded-xl bg-red-900/30 px-4 py-3 text-sm text-red-400">{errorMessage}</p>
      )}

      <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-4">
        <div className="rounded-[1.75rem] border border-white/10 bg-white/5 p-6">
          <p className="text-xs font-semibold tracking-[0.14em] text-stone-400 uppercase">
            Cumulative Spend
          </p>
          <p className="mt-4 text-3xl font-black text-white">{formatCurrency(customer.cumulativeSpend)}</p>
          <p className="mt-2 text-sm text-stone-500">Active orders only</p>
        </div>
        <div className="rounded-[1.75rem] border border-white/10 bg-white/5 p-6">
          <p className="text-xs font-semibold tracking-[0.14em] text-stone-400 uppercase">
            Total Orders
          </p>
          <p className="mt-4 text-3xl font-black text-white">{customer.totalOrderCount}</p>
          <p className="mt-2 text-sm text-stone-500">Including rolled back orders</p>
        </div>
        <div className="rounded-[1.75rem] border border-white/10 bg-white/5 p-6">
          <p className="text-xs font-semibold tracking-[0.14em] text-stone-400 uppercase">
            Contact
          </p>
          <p className="mt-4 text-lg font-semibold text-white">{customer.contactPerson ?? '—'}</p>
          <p className="mt-2 text-sm text-stone-500">{customer.phone ?? 'No phone provided'}</p>
        </div>
        <div className="rounded-[1.75rem] border border-white/10 bg-white/5 p-6">
          <p className="text-xs font-semibold tracking-[0.14em] text-stone-400 uppercase">
            GST Number
          </p>
          <p className="mt-4 text-lg font-semibold text-white">{customer.gstNumber ?? '—'}</p>
          <p className="mt-2 text-sm text-stone-500">{customer.email ?? 'No email provided'}</p>
        </div>
      </div>

      <div className="grid gap-6 lg:grid-cols-[minmax(0,0.95fr)_minmax(0,1.05fr)]">
        <section className="rounded-[1.75rem] border border-white/10 bg-white/5 p-6">
          <h2 className="text-sm font-semibold text-stone-300">Customer Profile</h2>
          <div className="mt-5 grid gap-4 md:grid-cols-2">
            <Field label="Company Name" value={customer.companyName} />
            <Field label="Contact Person" value={customer.contactPerson ?? '—'} />
            <Field label="Phone" value={customer.phone ?? '—'} />
            <Field label="Email" value={customer.email ?? '—'} />
            <Field label="Address" value={customer.address ?? '—'} />
            <Field label="GST Number" value={customer.gstNumber ?? '—'} />
            <div className="md:col-span-2">
              <Field label="Remarks" value={customer.remarks ?? '—'} />
            </div>
          </div>
        </section>

        <section className="rounded-[1.75rem] border border-white/10 bg-white/5 p-6">
          <div className="flex items-center justify-between gap-4">
            <h2 className="text-sm font-semibold text-stone-300">Order History</h2>
            <p className="text-xs text-stone-500">
              Showing {customer.orderHistory.length} of {customer.totalOrderCount}
            </p>
          </div>

          {customer.orderHistory.length === 0 ? (
            <div className="mt-8 rounded-2xl border border-dashed border-white/10 px-6 py-10 text-center text-sm text-stone-500">
              No order history yet.
            </div>
          ) : (
            <div className="mt-5 overflow-hidden rounded-2xl border border-white/10">
              <table className="w-full text-sm">
                <thead>
                  <tr className="border-b border-white/10 text-left text-xs font-semibold tracking-wider text-stone-400 uppercase">
                    <th className="px-4 py-3">Order No.</th>
                    <th className="px-4 py-3">Status</th>
                    <th className="px-4 py-3 text-right">Total</th>
                    <th className="px-4 py-3">Created</th>
                    <th className="px-4 py-3 text-right">Action</th>
                  </tr>
                </thead>
                <tbody>
                  {customer.orderHistory.map((order) => (
                    <tr key={order.id} className="border-b border-white/5 last:border-b-0">
                      <td className="px-4 py-3 font-mono text-xs text-amber-300">{order.orderNumber}</td>
                      <td className="px-4 py-3">
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
                      <td className="px-4 py-3 text-right font-medium text-stone-100">
                        {formatCurrency(order.totalAmount)}
                      </td>
                      <td className="px-4 py-3 text-stone-400">{formatDateTime(order.createdAt)}</td>
                      <td className="px-4 py-3 text-right">
                        <button
                          type="button"
                          onClick={() => navigate(`/orders/${order.id}`)}
                          className="rounded-lg border border-white/10 px-3 py-1.5 text-xs font-medium text-stone-300 transition hover:bg-white/10 hover:text-stone-100"
                        >
                          View Order
                        </button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </section>
      </div>
    </div>
  );
}
