import { useState } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { useCreateDestruction } from '../api/destruction';
import { useBatches } from '../api/inventory';
import { useProducts } from '../api/products';
import { Modal } from '../components/Modal';
import type { BatchItem, ExpiryStatus } from '../types/inventory';
import { extractErrorMessage } from '../utils/error';

const inputCls =
  'w-full rounded-xl border border-white/10 bg-white/5 px-4 py-2.5 text-sm text-stone-100 outline-none transition focus:border-amber-300/50 focus:ring-2 focus:ring-amber-300/20 placeholder:text-stone-500';

const selectCls =
  'w-full rounded-xl border border-white/10 bg-stone-900 px-4 py-2.5 text-sm text-stone-100 outline-none focus:border-amber-300/50';

const textareaCls =
  'w-full rounded-xl border border-white/10 bg-white/5 px-4 py-3 text-sm text-stone-100 outline-none transition focus:border-amber-300/50 focus:ring-2 focus:ring-amber-300/20 placeholder:text-stone-500';

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

function resolveSelectedBatch(batches: BatchItem[], batchId: number | undefined) {
  return batches.find((batch) => batch.id === batchId);
}

export function DestructionCreatePage() {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const createDestruction = useCreateDestruction();

  const initialProductId = Number(searchParams.get('productId'));
  const initialBatchId = Number(searchParams.get('batchId'));

  const [productId, setProductId] = useState<number | undefined>(
    Number.isFinite(initialProductId) && initialProductId > 0 ? initialProductId : undefined,
  );
  const [batchId, setBatchId] = useState<number | undefined>(
    Number.isFinite(initialBatchId) && initialBatchId > 0 ? initialBatchId : undefined,
  );
  const [quantityDestroyed, setQuantityDestroyed] = useState('1');
  const [reason, setReason] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [confirmOpen, setConfirmOpen] = useState(false);

  const productsQuery = useProducts({ page: 0, size: 100, active: true });
  const batchesQuery = useBatches({
    productId,
    page: 0,
    size: 100,
  });

  const availableBatches = (batchesQuery.data?.items ?? []).filter(
    (batch) => batch.quantityRemaining > 0,
  );
  const selectedBatch = resolveSelectedBatch(availableBatches, batchId);
  const quantityValue = Number(quantityDestroyed);
  const estimatedLoss =
    selectedBatch && Number.isFinite(quantityValue) && quantityValue > 0
      ? quantityValue * selectedBatch.purchaseUnitPrice
      : 0;

  const resetBatchSelection = () => {
    setBatchId(undefined);
    setQuantityDestroyed('1');
  };

  const validateForm = () => {
    if (!productId) {
      setError('Please select a product first.');
      return false;
    }

    if (!batchId || !selectedBatch) {
      setError('Please select a batch to destroy.');
      return false;
    }

    if (!Number.isInteger(quantityValue) || quantityValue < 1) {
      setError('Destroyed quantity must be at least 1.');
      return false;
    }

    if (quantityValue > selectedBatch.quantityRemaining) {
      setError('Destroyed quantity cannot exceed the batch remaining stock.');
      return false;
    }

    if (!reason.trim()) {
      setError('Please provide a destruction reason.');
      return false;
    }

    setError(null);
    return true;
  };

  const handleOpenConfirm = () => {
    if (!validateForm()) {
      return;
    }

    setConfirmOpen(true);
  };

  const handleConfirmCreate = async () => {
    if (!validateForm() || !selectedBatch) {
      return;
    }

    try {
      const created = await createDestruction.mutateAsync({
        batchId: selectedBatch.id,
        quantityDestroyed: quantityValue,
        reason: reason.trim(),
      });
      setConfirmOpen(false);
      navigate(`/destructions/${created.id}`);
    } catch (createError) {
      setError(extractErrorMessage(createError));
      setConfirmOpen(false);
    }
  };

  return (
    <div className="mx-auto max-w-5xl space-y-8">
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
            <h1 className="mt-1 text-3xl font-black tracking-tight">New Destruction</h1>
          </div>
        </div>
      </div>

      <div className="grid gap-6 lg:grid-cols-[minmax(0,1.1fr)_minmax(320px,0.9fr)]">
        <section className="space-y-6">
          <div className="space-y-5 rounded-[1.75rem] border border-white/10 bg-white/5 p-6">
            <div>
              <p className="text-xs font-semibold tracking-[0.14em] text-stone-400 uppercase">
                Step 1
              </p>
              <h2 className="mt-2 text-lg font-semibold text-stone-100">Choose product and batch</h2>
            </div>

            <label className="block space-y-1.5">
              <span className="text-xs font-medium text-stone-400">
                Product <span className="text-amber-300">*</span>
              </span>
              <select
                value={productId ?? ''}
                onChange={(event) => {
                  setProductId(event.target.value ? Number(event.target.value) : undefined);
                  resetBatchSelection();
                  setError(null);
                }}
                className={selectCls}
                disabled={productsQuery.isLoading || Boolean(productsQuery.error)}
              >
                <option value="">Select product</option>
                {productsQuery.data?.items.map((product) => (
                  <option key={product.id} value={product.id}>
                    {product.sku} - {product.name}
                  </option>
                ))}
              </select>
            </label>

            <label className="block space-y-1.5">
              <span className="text-xs font-medium text-stone-400">
                Batch <span className="text-amber-300">*</span>
              </span>
              <select
                value={batchId ?? ''}
                onChange={(event) => {
                  setBatchId(event.target.value ? Number(event.target.value) : undefined);
                  setQuantityDestroyed('1');
                  setError(null);
                }}
                className={selectCls}
                disabled={!productId || batchesQuery.isLoading || Boolean(batchesQuery.error)}
              >
                <option value="">
                  {productId ? 'Select batch' : 'Select a product first'}
                </option>
                {availableBatches.map((batch) => (
                  <option key={batch.id} value={batch.id}>
                    {batch.batchNumber} | Remaining {batch.quantityRemaining} | Expires {batch.expiryDate}
                  </option>
                ))}
              </select>
            </label>

            {batchesQuery.error && (
              <p className="rounded-xl bg-red-900/30 px-4 py-3 text-sm text-red-400">
                {extractErrorMessage(batchesQuery.error)}
              </p>
            )}

            {selectedBatch && (
              <div className="grid gap-4 rounded-[1.5rem] border border-white/10 bg-stone-900/70 p-5 md:grid-cols-2">
                <div>
                  <p className="text-xs font-medium text-stone-400">Batch Number</p>
                  <p className="mt-1 font-mono text-sm text-amber-300">{selectedBatch.batchNumber}</p>
                </div>
                <div>
                  <p className="text-xs font-medium text-stone-400">Expiry Status</p>
                  <div className="mt-2">
                    <span
                      className={`rounded-full px-2.5 py-0.5 text-xs font-semibold ${EXPIRY_STATUS_CLS[selectedBatch.expiryStatus]}`}
                    >
                      {EXPIRY_STATUS_LABELS[selectedBatch.expiryStatus]}
                    </span>
                  </div>
                </div>
                <div>
                  <p className="text-xs font-medium text-stone-400">Arrival Date</p>
                  <p className="mt-1 text-sm text-stone-200">{formatDate(selectedBatch.arrivalDate)}</p>
                </div>
                <div>
                  <p className="text-xs font-medium text-stone-400">Expiry Date</p>
                  <p className="mt-1 text-sm text-stone-200">{formatDate(selectedBatch.expiryDate)}</p>
                </div>
                <div>
                  <p className="text-xs font-medium text-stone-400">Remaining Stock</p>
                  <p className="mt-1 text-sm text-stone-100">{selectedBatch.quantityRemaining}</p>
                </div>
                <div>
                  <p className="text-xs font-medium text-stone-400">Purchase Unit Price</p>
                  <p className="mt-1 text-sm text-stone-100">
                    {formatCurrency(selectedBatch.purchaseUnitPrice)}
                  </p>
                </div>
              </div>
            )}
          </div>

          <div className="space-y-5 rounded-[1.75rem] border border-white/10 bg-white/5 p-6">
            <div>
              <p className="text-xs font-semibold tracking-[0.14em] text-stone-400 uppercase">
                Step 2
              </p>
              <h2 className="mt-2 text-lg font-semibold text-stone-100">Confirm quantity and reason</h2>
            </div>

            <div className="grid gap-4 md:grid-cols-2">
              <label className="block space-y-1.5">
                <span className="text-xs font-medium text-stone-400">
                  Quantity to destroy <span className="text-amber-300">*</span>
                </span>
                <input
                  type="number"
                  min="1"
                  value={quantityDestroyed}
                  onChange={(event) => {
                    setQuantityDestroyed(event.target.value);
                    setError(null);
                  }}
                  className={inputCls}
                  placeholder="Enter quantity"
                />
              </label>

              <div className="rounded-[1.5rem] border border-white/10 bg-stone-900/70 p-5">
                <p className="text-xs font-medium text-stone-400">Estimated loss</p>
                <p className="mt-2 text-2xl font-semibold text-stone-100">
                  {formatCurrency(estimatedLoss)}
                </p>
                <p className="mt-2 text-xs leading-5 text-stone-500">
                  Based on the selected batch purchase unit price snapshot.
                </p>
              </div>
            </div>

            <label className="block space-y-1.5">
              <span className="text-xs font-medium text-stone-400">
                Reason <span className="text-amber-300">*</span>
              </span>
              <textarea
                value={reason}
                onChange={(event) => {
                  setReason(event.target.value);
                  setError(null);
                }}
                rows={5}
                className={textareaCls}
                placeholder="Explain why this stock needs to be destroyed"
              />
            </label>
          </div>
        </section>

        <aside className="space-y-6">
          <div className="rounded-[1.75rem] border border-white/10 bg-stone-900/80 p-6">
            <p className="text-xs font-semibold tracking-[0.14em] text-stone-400 uppercase">
              Submission Checklist
            </p>
            <div className="mt-5 space-y-3 text-sm text-stone-300">
              <div className="flex items-start gap-3">
                <span className="mt-1 h-2 w-2 rounded-full bg-amber-300" />
                <p>Pick the exact product and batch that should be written off.</p>
              </div>
              <div className="flex items-start gap-3">
                <span className="mt-1 h-2 w-2 rounded-full bg-amber-300" />
                <p>Make sure the quantity does not exceed remaining stock.</p>
              </div>
              <div className="flex items-start gap-3">
                <span className="mt-1 h-2 w-2 rounded-full bg-amber-300" />
                <p>Provide a clear reason for audit and accounting traceability.</p>
              </div>
            </div>
          </div>

          <div className="rounded-[1.75rem] border border-white/10 bg-white/5 p-6">
            <p className="text-xs font-semibold tracking-[0.14em] text-stone-400 uppercase">
              Summary
            </p>
            <div className="mt-5 space-y-3 text-sm">
              <div className="flex items-center justify-between text-stone-300">
                <span>Product</span>
                <span className="max-w-[180px] truncate text-right text-stone-100">
                  {productsQuery.data?.items.find((product) => product.id === productId)?.name ?? '-'}
                </span>
              </div>
              <div className="flex items-center justify-between text-stone-300">
                <span>Batch</span>
                <span className="max-w-[180px] truncate text-right text-stone-100">
                  {selectedBatch?.batchNumber ?? '-'}
                </span>
              </div>
              <div className="flex items-center justify-between text-stone-300">
                <span>Quantity</span>
                <span className="text-stone-100">{Number.isFinite(quantityValue) ? quantityValue : '-'}</span>
              </div>
              <div className="flex items-center justify-between border-t border-white/10 pt-3 text-base font-semibold text-white">
                <span>Estimated loss</span>
                <span>{formatCurrency(estimatedLoss)}</span>
              </div>
            </div>
          </div>

          {error && (
            <p className="rounded-xl bg-red-900/30 px-4 py-3 text-sm text-red-400">{error}</p>
          )}

          <button
            type="button"
            onClick={handleOpenConfirm}
            disabled={createDestruction.isPending}
            className="w-full rounded-xl bg-amber-300 px-6 py-3 text-sm font-semibold text-stone-900 transition hover:bg-amber-200 disabled:opacity-50"
          >
            {createDestruction.isPending ? 'Submitting...' : 'Review and Submit'}
          </button>
        </aside>
      </div>

      {confirmOpen && selectedBatch && (
        <Modal
          title="Confirm Destruction"
          onClose={() => {
            setConfirmOpen(false);
          }}
        >
          <div className="space-y-5">
            <p className="text-sm leading-6 text-stone-300">
              This action will deduct stock immediately and create a permanent destruction record.
            </p>

            <div className="rounded-[1.5rem] border border-white/10 bg-white/5 p-5">
              <div className="grid gap-3 text-sm text-stone-300">
                <div className="flex items-center justify-between gap-4">
                  <span>Product</span>
                  <span className="text-right font-medium text-stone-100">
                    {selectedBatch.productName}
                  </span>
                </div>
                <div className="flex items-center justify-between gap-4">
                  <span>Batch</span>
                  <span className="text-right font-mono text-xs text-amber-300">
                    {selectedBatch.batchNumber}
                  </span>
                </div>
                <div className="flex items-center justify-between gap-4">
                  <span>Quantity</span>
                  <span className="font-medium text-stone-100">{quantityValue}</span>
                </div>
                <div className="flex items-center justify-between gap-4">
                  <span>Loss Amount</span>
                  <span className="font-medium text-stone-100">{formatCurrency(estimatedLoss)}</span>
                </div>
              </div>
              <div className="mt-4 border-t border-white/10 pt-4">
                <p className="text-xs font-medium text-stone-400">Reason</p>
                <p className="mt-2 text-sm leading-6 text-stone-200">{reason.trim()}</p>
              </div>
            </div>

            <div className="space-y-2">
              <button
                type="button"
                onClick={() => void handleConfirmCreate()}
                disabled={createDestruction.isPending}
                className="w-full rounded-xl bg-red-500/90 px-5 py-2.5 text-sm font-semibold text-white transition hover:bg-red-500 disabled:opacity-50"
              >
                {createDestruction.isPending ? 'Submitting...' : 'Confirm Destruction'}
              </button>
              <button
                type="button"
                onClick={() => setConfirmOpen(false)}
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
