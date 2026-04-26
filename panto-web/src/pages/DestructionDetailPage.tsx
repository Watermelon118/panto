import type { ReactNode } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { useDestruction } from '../api/destruction';
import type { ExpiryStatus } from '../types/inventory';
import { extractErrorMessage } from '../utils/error';

const EXPIRY_STATUS_LABELS: Record<ExpiryStatus, string> = {
  NORMAL: 'Normal',
  EXPIRING_SOON: 'Expiring Soon',
  EXPIRED: 'Expired',
};

const EXPIRY_STATUS_CLS: Record<ExpiryStatus, string> = {
  NORMAL: 'bg-emerald-900/40 text-emerald-400',
  EXPIRING_SOON: 'bg-amber-900/40 text-amber-300',
  EXPIRED: 'bg-red-900/40 text-red-400',
};

function formatCurrency(value: number) {
  return `$${value.toFixed(2)}`;
}

function formatDate(value: string) {
  return new Intl.DateTimeFormat('en-NZ', {
    dateStyle: 'medium',
  }).format(new Date(value));
}

function formatDateTime(value: string) {
  return new Intl.DateTimeFormat('en-NZ', {
    dateStyle: 'medium',
    timeStyle: 'short',
  }).format(new Date(value));
}

function Field({
  label,
  value,
  children,
}: {
  label: string;
  value?: ReactNode;
  children?: ReactNode;
}) {
  return (
    <div>
      <p className="text-xs font-medium text-stone-400">{label}</p>
      <div className="mt-1 text-sm text-stone-200">{children ?? value}</div>
    </div>
  );
}

export function DestructionDetailPage() {
  const navigate = useNavigate();
  const { id } = useParams<{ id: string }>();
  const destructionId = Number(id);

  const destructionQuery = useDestruction(destructionId);
  const destruction = destructionQuery.data;
  const errorMessage = destructionQuery.error
    ? extractErrorMessage(destructionQuery.error)
    : null;

  if (destructionQuery.isLoading) {
    return <div className="p-12 text-center text-sm text-stone-500">Loading...</div>;
  }

  if (!destruction) {
    return (
      <div className="space-y-4 p-12 text-center text-sm text-stone-500">
        <p>{errorMessage ?? 'Destruction record not found.'}</p>
        <button
          type="button"
          onClick={() => navigate('/destructions')}
          className="rounded-xl border border-white/10 px-4 py-2.5 text-stone-300 transition hover:bg-white/5 hover:text-stone-100"
        >
          Back to Destructions
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
            onClick={() => navigate('/destructions')}
            className="rounded-xl border border-white/10 px-4 py-2.5 text-sm text-stone-400 transition hover:bg-white/5 hover:text-stone-100"
          >
            Back
          </button>
          <div>
            <p className="text-xs font-semibold tracking-[0.18em] text-amber-300 uppercase">
              Inventory
            </p>
            <h1 className="mt-1 font-mono text-2xl font-black tracking-tight">
              {destruction.destructionNumber}
            </h1>
          </div>
        </div>
        <div className="rounded-full bg-red-500/15 px-4 py-2 text-sm font-semibold text-red-300">
          Destroyed Stock
        </div>
      </div>

      {errorMessage && (
        <p className="rounded-xl bg-red-900/30 px-4 py-3 text-sm text-red-400">{errorMessage}</p>
      )}

      <div className="grid gap-6 lg:grid-cols-[minmax(0,1.1fr)_minmax(320px,0.9fr)]">
        <section className="space-y-6">
          <div className="space-y-4 rounded-[1.75rem] border border-white/10 bg-white/5 p-6">
            <div className="flex items-center justify-between">
              <h2 className="text-sm font-semibold text-stone-300">Destruction Details</h2>
              <p className="text-xs text-stone-500">
                Created {formatDateTime(destruction.createdAt)}
              </p>
            </div>

            <div className="grid gap-4 md:grid-cols-2">
              <Field label="Product">
                <div>
                  <div className="font-medium text-stone-100">{destruction.productName}</div>
                  <div className="text-xs text-stone-500">{destruction.productSku}</div>
                </div>
              </Field>
              <Field label="Destroyed Quantity" value={destruction.quantityDestroyed} />
              <Field
                label="Loss Amount"
                value={<span className="font-semibold text-stone-100">{formatCurrency(destruction.lossAmount)}</span>}
              />
              <Field
                label="Purchase Unit Price Snapshot"
                value={formatCurrency(destruction.purchaseUnitPriceSnapshot)}
              />
              <div className="md:col-span-2">
                <p className="text-xs font-medium text-stone-400">Reason</p>
                <p className="mt-1 text-sm leading-6 text-stone-200">{destruction.reason}</p>
              </div>
            </div>
          </div>

          <div className="space-y-4 rounded-[1.75rem] border border-white/10 bg-white/5 p-6">
            <div className="flex items-center justify-between">
              <h2 className="text-sm font-semibold text-stone-300">Batch Snapshot</h2>
              <span
                className={`rounded-full px-3 py-1 text-xs font-semibold ${EXPIRY_STATUS_CLS[destruction.batchExpiryStatus]}`}
              >
                {EXPIRY_STATUS_LABELS[destruction.batchExpiryStatus]}
              </span>
            </div>

            <div className="grid gap-4 md:grid-cols-2">
              <Field
                label="Batch Number"
                value={<span className="font-mono text-xs text-amber-300">{destruction.batchNumber}</span>}
              />
              <Field label="Batch ID" value={destruction.batchId} />
              <Field label="Expiry Date" value={formatDate(destruction.batchExpiryDate)} />
              <Field label="Remaining Stock After Destruction" value={destruction.batchQuantityRemaining} />
              <Field label="Inventory Transaction ID" value={destruction.inventoryTransactionId} />
              <Field label="Created By User ID" value={destruction.createdBy} />
            </div>
          </div>
        </section>

        <aside className="space-y-6">
          <div className="rounded-[1.75rem] border border-white/10 bg-stone-900/80 p-6">
            <p className="text-xs font-semibold tracking-[0.14em] text-stone-400 uppercase">
              Financial Impact
            </p>
            <div className="mt-5 space-y-3 text-sm">
              <div className="flex items-center justify-between text-stone-300">
                <span>Destroyed Quantity</span>
                <span>{destruction.quantityDestroyed}</span>
              </div>
              <div className="flex items-center justify-between text-stone-300">
                <span>Unit Cost</span>
                <span>{formatCurrency(destruction.purchaseUnitPriceSnapshot)}</span>
              </div>
              <div className="flex items-center justify-between border-t border-white/10 pt-3 text-base font-semibold text-white">
                <span>Total Loss</span>
                <span>{formatCurrency(destruction.lossAmount)}</span>
              </div>
            </div>
          </div>

          <div className="rounded-[1.75rem] border border-white/10 bg-white/5 p-6">
            <p className="text-xs font-semibold tracking-[0.14em] text-stone-400 uppercase">
              Audit Trail
            </p>
            <div className="mt-5 space-y-4 text-sm">
              <div>
                <p className="text-xs font-medium text-stone-400">Created At</p>
                <p className="mt-1 text-stone-200">{formatDateTime(destruction.createdAt)}</p>
              </div>
              <div>
                <p className="text-xs font-medium text-stone-400">Created By</p>
                <p className="mt-1 text-stone-200">{destruction.createdBy}</p>
              </div>
              <div>
                <p className="text-xs font-medium text-stone-400">Inventory Transaction</p>
                <p className="mt-1 text-stone-200">{destruction.inventoryTransactionId}</p>
              </div>
            </div>
          </div>
        </aside>
      </div>
    </div>
  );
}
