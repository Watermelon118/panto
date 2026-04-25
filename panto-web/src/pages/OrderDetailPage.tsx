import { useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { useInvoice, useOrder, useRollbackOrder } from '../api/orders';
import { Modal } from '../components/Modal';
import { extractErrorMessage } from '../utils/error';

const textareaCls =
  'w-full rounded-xl border border-white/10 bg-white/5 px-4 py-3 text-sm text-stone-100 outline-none transition focus:border-amber-300/50 focus:ring-2 focus:ring-amber-300/20 placeholder:text-stone-500';

function formatCurrency(value: number) {
  return `$${value.toFixed(2)}`;
}

function formatDateTime(value: string) {
  return new Intl.DateTimeFormat('en-NZ', {
    dateStyle: 'medium',
    timeStyle: 'short',
  }).format(new Date(value));
}

function formatDate(value: string) {
  return new Intl.DateTimeFormat('en-NZ', {
    dateStyle: 'medium',
  }).format(new Date(value));
}

function StatusBadge({ status }: { status: 'ACTIVE' | 'ROLLED_BACK' }) {
  return (
    <span
      className={`rounded-full px-3 py-1 text-xs font-semibold ${
        status === 'ACTIVE'
          ? 'bg-emerald-400/15 text-emerald-400'
          : 'bg-stone-700 text-stone-300'
      }`}
    >
      {status === 'ACTIVE' ? 'Active' : 'Rolled Back'}
    </span>
  );
}

export function OrderDetailPage() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const orderId = Number(id);

  const orderQuery = useOrder(orderId);
  const invoiceQuery = useInvoice(orderId);
  const rollbackOrder = useRollbackOrder();

  const [rollbackOpen, setRollbackOpen] = useState(false);
  const [rollbackReason, setRollbackReason] = useState('');
  const [error, setError] = useState<string | null>(null);

  const order = orderQuery.data;
  const invoice = invoiceQuery.data;

  const handleRollback = async () => {
    const reason = rollbackReason.trim();
    if (!reason) {
      setError('Please provide a rollback reason.');
      return;
    }

    setError(null);
    try {
      await rollbackOrder.mutateAsync({
        id: orderId,
        request: { reason },
      });
      setRollbackReason('');
      setRollbackOpen(false);
    } catch (rollbackError) {
      setError(extractErrorMessage(rollbackError));
    }
  };

  if (orderQuery.isLoading) {
    return <div className="p-12 text-center text-sm text-stone-500">Loading...</div>;
  }

  if (!order) {
    return (
      <div className="space-y-4 p-12 text-center text-sm text-stone-500">
        <p>Order not found.</p>
        <button
          type="button"
          onClick={() => navigate('/orders')}
          className="rounded-xl border border-white/10 px-4 py-2.5 text-stone-300 transition hover:bg-white/5 hover:text-stone-100"
        >
          Back to Orders
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
            onClick={() => navigate('/orders')}
            className="rounded-xl border border-white/10 px-4 py-2.5 text-sm text-stone-400 transition hover:bg-white/5 hover:text-stone-100"
          >
            Back
          </button>
          <div>
            <p className="text-xs font-semibold tracking-[0.18em] text-amber-300 uppercase">
              Sales
            </p>
            <h1 className="mt-1 font-mono text-2xl font-black tracking-tight">{order.orderNumber}</h1>
          </div>
        </div>

        <div className="flex flex-wrap items-center gap-3">
          <StatusBadge status={order.status} />
          <button
            type="button"
            onClick={() => window.print()}
            className="rounded-xl border border-white/10 px-4 py-2.5 text-sm text-stone-300 transition hover:bg-white/5 hover:text-stone-100"
          >
            Print Invoice
          </button>
          <button
            type="button"
            onClick={() => {
              setError(null);
              setRollbackOpen(true);
            }}
            disabled={order.status !== 'ACTIVE' || rollbackOrder.isPending}
            className="rounded-xl bg-red-500/90 px-4 py-2.5 text-sm font-semibold text-white transition hover:bg-red-500 disabled:cursor-not-allowed disabled:opacity-40"
          >
            {rollbackOrder.isPending ? 'Rolling Back...' : 'Rollback Order'}
          </button>
        </div>
      </div>

      {(orderQuery.error || invoiceQuery.error || error) && (
        <p className="rounded-xl bg-red-900/30 px-4 py-3 text-sm text-red-400">
          {error ?? extractErrorMessage(orderQuery.error ?? invoiceQuery.error)}
        </p>
      )}

      <div className="grid gap-6 lg:grid-cols-[minmax(0,1.15fr)_minmax(320px,0.85fr)]">
        <section className="space-y-6">
          <div className="space-y-4 rounded-[1.75rem] border border-white/10 bg-white/5 p-6">
            <div className="flex items-center justify-between">
              <h2 className="text-sm font-semibold text-stone-300">Order Details</h2>
              <p className="text-xs text-stone-500">Created {formatDateTime(order.createdAt)}</p>
            </div>

            <div className="grid gap-4 md:grid-cols-2">
              <div>
                <p className="text-xs font-medium text-stone-400">Customer</p>
                <p className="mt-1 text-sm font-medium text-stone-100">{order.customerCompanyName}</p>
              </div>
              <div>
                <p className="text-xs font-medium text-stone-400">Last Updated</p>
                <p className="mt-1 text-sm text-stone-300">{formatDateTime(order.updatedAt)}</p>
              </div>
              <div className="md:col-span-2">
                <p className="text-xs font-medium text-stone-400">Remarks</p>
                <p className="mt-1 text-sm text-stone-300">{order.remarks || 'No remarks.'}</p>
              </div>
            </div>
          </div>

          <div className="space-y-4 rounded-[1.75rem] border border-white/10 bg-white/5 p-6">
            <h2 className="text-sm font-semibold text-stone-300">
              Order Items <span className="ml-1 text-stone-500">({order.items.length})</span>
            </h2>

            <div className="overflow-x-auto">
              <table className="w-full text-sm">
                <thead>
                  <tr className="border-b border-white/10 text-left text-xs font-semibold tracking-wider text-stone-400 uppercase">
                    <th className="pb-3 pr-4">Product</th>
                    <th className="pb-3 pr-4">Batch</th>
                    <th className="pb-3 pr-4">Expiry</th>
                    <th className="pb-3 pr-4 text-right">Qty</th>
                    <th className="pb-3 pr-4 text-right">Unit Price</th>
                    <th className="pb-3 pr-4 text-right">GST</th>
                    <th className="pb-3 text-right">Subtotal</th>
                  </tr>
                </thead>
                <tbody>
                  {order.items.map((item) => (
                    <tr key={item.id} className="border-b border-white/5">
                      <td className="py-3 pr-4">
                        <div className="font-medium text-stone-100">{item.productName}</div>
                        <div className="text-xs text-stone-500">
                          {item.productSku} | {item.productUnit}
                          {item.productSpecification ? ` | ${item.productSpecification}` : ''}
                        </div>
                      </td>
                      <td className="py-3 pr-4 font-mono text-xs text-amber-300">
                        {item.batchNumber ?? '-'}
                      </td>
                      <td className="py-3 pr-4 text-stone-300">
                        {item.batchExpiryDate ? formatDate(item.batchExpiryDate) : '-'}
                      </td>
                      <td className="py-3 pr-4 text-right text-stone-300">{item.quantity}</td>
                      <td className="py-3 pr-4 text-right text-stone-300">
                        {formatCurrency(item.unitPrice)}
                      </td>
                      <td className="py-3 pr-4 text-right text-stone-300">
                        {formatCurrency(item.gstAmount)}
                      </td>
                      <td className="py-3 text-right font-medium text-stone-100">
                        {formatCurrency(item.subtotal + item.gstAmount)}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>
        </section>

        <aside className="space-y-6">
          <div className="rounded-[1.75rem] border border-white/10 bg-stone-900/80 p-6">
            <p className="text-xs font-semibold tracking-[0.14em] text-stone-400 uppercase">
              Totals
            </p>
            <div className="mt-5 space-y-3 text-sm">
              <div className="flex items-center justify-between text-stone-300">
                <span>Subtotal</span>
                <span>{formatCurrency(order.subtotalAmount)}</span>
              </div>
              <div className="flex items-center justify-between text-stone-300">
                <span>GST</span>
                <span>{formatCurrency(order.gstAmount)}</span>
              </div>
              <div className="flex items-center justify-between border-t border-white/10 pt-3 text-base font-semibold text-white">
                <span>Total</span>
                <span>{formatCurrency(order.totalAmount)}</span>
              </div>
            </div>
          </div>

          <div className="space-y-4 rounded-[1.75rem] border border-white/10 bg-white/5 p-6">
            <div className="flex items-center justify-between">
              <h2 className="text-sm font-semibold text-stone-300">Invoice Preview</h2>
              {invoice && <p className="text-xs text-stone-500">{invoice.invoiceNumber}</p>}
            </div>

            {invoiceQuery.isLoading ? (
              <div className="rounded-xl bg-white/5 px-4 py-8 text-center text-sm text-stone-500">
                Loading invoice...
              </div>
            ) : !invoice ? (
              <div className="rounded-xl bg-white/5 px-4 py-8 text-center text-sm text-stone-500">
                Invoice data is unavailable.
              </div>
            ) : (
              <div className="space-y-5 rounded-[1.5rem] border border-dashed border-white/10 bg-stone-950/60 p-5">
                <div className="flex items-start justify-between gap-4">
                  <div>
                    <p className="text-lg font-bold text-stone-100">Tax Invoice</p>
                    <p className="mt-1 text-xs text-stone-500">{invoice.invoiceNumber}</p>
                  </div>
                  <div className="text-right text-xs text-stone-500">
                    <p>{formatDateTime(invoice.invoiceDate)}</p>
                    <p className="mt-1">
                      <StatusBadge status={invoice.status} />
                    </p>
                  </div>
                </div>

                <div className="grid gap-4 rounded-2xl bg-white/5 p-4 text-sm md:grid-cols-2">
                  <div>
                    <p className="text-xs font-medium text-stone-400">Bill To</p>
                    <p className="mt-1 font-medium text-stone-100">{invoice.customer.companyName}</p>
                    <p className="mt-1 text-stone-300">{invoice.customer.contactPerson || 'No contact'}</p>
                    <p className="text-stone-300">{invoice.customer.phone || 'No phone'}</p>
                    <p className="text-stone-300">{invoice.customer.address || 'No address'}</p>
                  </div>
                  <div>
                    <p className="text-xs font-medium text-stone-400">Notes</p>
                    <p className="mt-1 text-stone-300">{invoice.remarks || 'No remarks.'}</p>
                    <p className="mt-3 text-xs font-medium text-stone-400">Payment Instructions</p>
                    <p className="mt-1 text-stone-300">
                      {invoice.paymentInstructions || 'Refer to office payment terms.'}
                    </p>
                    <p className="mt-3 text-xs font-medium text-stone-400">GST Number</p>
                    <p className="mt-1 text-stone-300">{invoice.customer.gstNumber || 'Not provided'}</p>
                  </div>
                </div>

                <div className="overflow-x-auto">
                  <table className="w-full text-sm">
                    <thead>
                      <tr className="border-b border-white/10 text-left text-xs font-semibold tracking-wider text-stone-400 uppercase">
                        <th className="pb-3 pr-4">Item</th>
                        <th className="pb-3 pr-4 text-right">Qty</th>
                        <th className="pb-3 pr-4 text-right">Unit Price</th>
                        <th className="pb-3 pr-4 text-right">GST</th>
                        <th className="pb-3 text-right">Line Total</th>
                      </tr>
                    </thead>
                    <tbody>
                      {invoice.items.map((item, index) => (
                        <tr key={`${item.productSku}-${index}`} className="border-b border-white/5">
                          <td className="py-3 pr-4">
                            <div className="font-medium text-stone-100">{item.productName}</div>
                            <div className="text-xs text-stone-500">
                              {item.productSku} | {item.productUnit}
                              {item.productSpecification ? ` | ${item.productSpecification}` : ''}
                            </div>
                          </td>
                          <td className="py-3 pr-4 text-right text-stone-300">{item.quantity}</td>
                          <td className="py-3 pr-4 text-right text-stone-300">
                            {formatCurrency(item.unitPrice)}
                          </td>
                          <td className="py-3 pr-4 text-right text-stone-300">
                            {formatCurrency(item.gstAmount)}
                          </td>
                          <td className="py-3 text-right font-medium text-stone-100">
                            {formatCurrency(item.subtotal + item.gstAmount)}
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>

                <div className="ml-auto max-w-xs space-y-2 text-sm">
                  <div className="flex items-center justify-between text-stone-300">
                    <span>Subtotal</span>
                    <span>{formatCurrency(invoice.subtotalAmount)}</span>
                  </div>
                  <div className="flex items-center justify-between text-stone-300">
                    <span>GST</span>
                    <span>{formatCurrency(invoice.gstAmount)}</span>
                  </div>
                  <div className="flex items-center justify-between border-t border-white/10 pt-2 text-base font-semibold text-white">
                    <span>Total</span>
                    <span>{formatCurrency(invoice.totalAmount)}</span>
                  </div>
                </div>
              </div>
            )}
          </div>
        </aside>
      </div>

      {rollbackOpen && (
        <Modal
          title="Rollback Order"
          onClose={() => {
            setRollbackOpen(false);
            setRollbackReason('');
            setError(null);
          }}
        >
          <div className="space-y-4">
            <p className="text-sm leading-6 text-stone-300">
              Rolling back this order will return stock to the original batches and mark the order as rolled back.
            </p>
            <label className="block space-y-1.5">
              <span className="text-xs font-medium text-stone-400">
                Reason <span className="ml-0.5 text-amber-300">*</span>
              </span>
              <textarea
                value={rollbackReason}
                onChange={(e) => setRollbackReason(e.target.value)}
                rows={5}
                className={textareaCls}
                placeholder="Explain why this order is being rolled back"
              />
            </label>
            {error && (
              <p className="rounded-xl bg-red-900/30 px-4 py-3 text-sm text-red-400">{error}</p>
            )}
            <div className="space-y-2">
              <button
                type="button"
                onClick={() => void handleRollback()}
                disabled={rollbackOrder.isPending}
                className="w-full rounded-xl bg-red-500/90 px-5 py-2.5 text-sm font-semibold text-white transition hover:bg-red-500 disabled:opacity-50"
              >
                {rollbackOrder.isPending ? 'Rolling Back...' : 'Confirm Rollback'}
              </button>
              <button
                type="button"
                onClick={() => {
                  setRollbackOpen(false);
                  setRollbackReason('');
                  setError(null);
                }}
                className="w-full rounded-xl border border-white/10 px-5 py-2.5 text-sm font-medium text-stone-400 transition hover:bg-white/10"
              >
                Cancel
              </button>
            </div>
          </div>
        </Modal>
      )}
    </div>
  );
}
