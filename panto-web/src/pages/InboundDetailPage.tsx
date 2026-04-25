import { useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { useInbound, useUpdateInbound } from '../api/inbound';
import { useProducts } from '../api/products';
import type { InboundItemRequest } from '../types/inbound';
import { extractErrorMessage } from '../utils/error';

const inputCls =
  'w-full rounded-xl border border-white/10 bg-white/5 px-4 py-2.5 text-sm text-stone-100 outline-none transition focus:border-amber-300/50 focus:ring-2 focus:ring-amber-300/20 placeholder:text-stone-500';

const selectCls =
  'w-full rounded-xl border border-white/10 bg-stone-900 px-4 py-2.5 text-sm text-stone-100 outline-none focus:border-amber-300/50';

function Field({ label, children }: { label: string; children: React.ReactNode }) {
  return (
    <div className="space-y-1.5">
      <p className="text-xs font-medium text-stone-400">{label}</p>
      {children}
    </div>
  );
}

export function InboundDetailPage() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const recordId = Number(id);

  const { data: record, isLoading } = useInbound(recordId);
  const updateInbound = useUpdateInbound();
  const { data: productsData } = useProducts({ page: 0, size: 100, active: true });
  const activeProducts = productsData?.items ?? [];

  const [editing, setEditing] = useState(false);
  const [inboundDate, setInboundDate] = useState('');
  const [remarks, setRemarks] = useState('');
  const [items, setItems] = useState<InboundItemRequest[]>([]);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (record) {
      setInboundDate(record.inboundDate);
      setRemarks(record.remarks ?? '');
      setItems(
        record.items.map((it) => ({
          productId: it.productId,
          expiryDate: it.expiryDate,
          quantity: it.quantity,
          purchaseUnitPrice: it.purchaseUnitPrice,
          remarks: it.remarks ?? '',
        })),
      );
    }
  }, [record]);

  const updateItem = <K extends keyof InboundItemRequest>(
    index: number,
    key: K,
    value: InboundItemRequest[K],
  ) => {
    setItems((prev) => prev.map((item, i) => (i === index ? { ...item, [key]: value } : item)));
  };

  const addItem = () =>
    setItems((prev) => [
      ...prev,
      { productId: 0, expiryDate: '', quantity: 1, purchaseUnitPrice: 0, remarks: '' },
    ]);

  const removeItem = (index: number) =>
    setItems((prev) => prev.filter((_, i) => i !== index));

  const handleSave = async () => {
    setError(null);
    const invalidItem = items.find((it) => it.productId === 0 || !it.expiryDate || it.quantity < 1);
    if (invalidItem) {
      setError('Please fill in all required fields for each item row.');
      return;
    }
    try {
      await updateInbound.mutateAsync({
        id: recordId,
        request: { inboundDate, remarks: remarks || undefined, items },
      });
      setEditing(false);
    } catch (err) {
      setError(extractErrorMessage(err));
    }
  };

  const handleCancelEdit = () => {
    if (record) {
      setInboundDate(record.inboundDate);
      setRemarks(record.remarks ?? '');
      setItems(
        record.items.map((it) => ({
          productId: it.productId,
          expiryDate: it.expiryDate,
          quantity: it.quantity,
          purchaseUnitPrice: it.purchaseUnitPrice,
          remarks: it.remarks ?? '',
        })),
      );
    }
    setError(null);
    setEditing(false);
  };

  if (isLoading) {
    return <div className="p-12 text-center text-sm text-stone-500">Loading…</div>;
  }

  if (!record) {
    return (
      <div className="p-12 text-center text-sm text-stone-500">
        Inbound record not found.{' '}
        <button
          type="button"
          onClick={() => navigate('/inbound')}
          className="text-amber-300 underline"
        >
          Go back
        </button>
      </div>
    );
  }

  return (
    <div className="mx-auto max-w-5xl space-y-8">
      {/* Header */}
      <div className="flex items-start justify-between">
        <div className="flex items-center gap-4">
          <button
            type="button"
            onClick={() => navigate('/inbound')}
            className="rounded-xl border border-white/10 px-4 py-2.5 text-sm text-stone-400 transition hover:bg-white/5 hover:text-stone-100"
          >
            ← Back
          </button>
          <div>
            <p className="text-xs font-semibold tracking-[0.18em] text-amber-300 uppercase">
              Inventory
            </p>
            <h1 className="mt-1 font-mono text-2xl font-black tracking-tight">
              {record.inboundNumber}
            </h1>
          </div>
        </div>
        {!editing && (
          <button
            type="button"
            onClick={() => setEditing(true)}
            className="rounded-xl border border-white/10 px-5 py-2.5 text-sm font-medium text-stone-300 transition hover:bg-white/5 hover:text-stone-100"
          >
            Edit
          </button>
        )}
      </div>

      {/* Record details */}
      <div className="rounded-[1.75rem] border border-white/10 bg-white/5 p-6 space-y-4">
        <h2 className="text-sm font-semibold text-stone-300">Record Details</h2>
        {editing ? (
          <div className="grid grid-cols-2 gap-4">
            <label className="block space-y-1.5">
              <span className="text-xs font-medium text-stone-400">
                Inbound Date <span className="text-amber-300">*</span>
              </span>
              <input
                type="date"
                value={inboundDate}
                onChange={(e) => setInboundDate(e.target.value)}
                className={inputCls}
              />
            </label>
            <label className="block space-y-1.5">
              <span className="text-xs font-medium text-stone-400">Remarks</span>
              <input
                type="text"
                value={remarks}
                onChange={(e) => setRemarks(e.target.value)}
                placeholder="Optional remarks…"
                className={inputCls}
              />
            </label>
          </div>
        ) : (
          <div className="grid grid-cols-3 gap-6">
            <Field label="Inbound Date">
              <p className="text-sm text-stone-100">{record.inboundDate}</p>
            </Field>
            <Field label="Remarks">
              <p className="text-sm text-stone-300">{record.remarks ?? '—'}</p>
            </Field>
            <Field label="Created At">
              <p className="text-sm text-stone-300">
                {new Date(record.createdAt).toLocaleString()}
              </p>
            </Field>
          </div>
        )}
      </div>

      {/* Line items */}
      <div className="rounded-[1.75rem] border border-white/10 bg-white/5 p-6 space-y-4">
        <div className="flex items-center justify-between">
          <h2 className="text-sm font-semibold text-stone-300">
            Line Items{' '}
            <span className="ml-1 text-stone-500">
              ({editing ? items.length : record.items.length})
            </span>
          </h2>
          {editing && (
            <button
              type="button"
              onClick={addItem}
              className="rounded-xl border border-amber-300/40 px-4 py-2 text-xs font-semibold text-amber-300 transition hover:bg-amber-300/10"
            >
              + Add Row
            </button>
          )}
        </div>

        <div className="overflow-x-auto">
          {editing ? (
            <table className="w-full text-sm">
              <thead>
                <tr className="border-b border-white/10 text-left text-xs font-semibold tracking-wider text-stone-400 uppercase">
                  <th className="pb-3 pr-4">Product<span className="text-amber-300">*</span></th>
                  <th className="pb-3 pr-4">Expiry Date<span className="text-amber-300">*</span></th>
                  <th className="pb-3 pr-4">Qty<span className="text-amber-300">*</span></th>
                  <th className="pb-3 pr-4">Unit Price<span className="text-amber-300">*</span></th>
                  <th className="pb-3 pr-4">Remarks</th>
                  <th className="pb-3" />
                </tr>
              </thead>
              <tbody>
                {items.map((item, index) => (
                  <tr key={index} className="align-top">
                    <td className="py-2 pr-4 min-w-[220px]">
                      <select
                        value={item.productId || ''}
                        onChange={(e) => updateItem(index, 'productId', Number(e.target.value))}
                        className={selectCls}
                      >
                        <option value="">Select product…</option>
                        {activeProducts.map((p) => (
                          <option key={p.id} value={p.id}>
                            {p.sku} — {p.name}
                          </option>
                        ))}
                      </select>
                    </td>
                    <td className="py-2 pr-4 min-w-[160px]">
                      <input
                        type="date"
                        value={item.expiryDate}
                        onChange={(e) => updateItem(index, 'expiryDate', e.target.value)}
                        className={inputCls}
                      />
                    </td>
                    <td className="py-2 pr-4 min-w-[100px]">
                      <input
                        type="number"
                        min="1"
                        value={item.quantity}
                        onChange={(e) => updateItem(index, 'quantity', Number(e.target.value))}
                        className={inputCls}
                      />
                    </td>
                    <td className="py-2 pr-4 min-w-[120px]">
                      <input
                        type="number"
                        min="0"
                        step="0.01"
                        value={item.purchaseUnitPrice}
                        onChange={(e) =>
                          updateItem(index, 'purchaseUnitPrice', Number(e.target.value))
                        }
                        className={inputCls}
                      />
                    </td>
                    <td className="py-2 pr-4 min-w-[160px]">
                      <input
                        type="text"
                        value={item.remarks ?? ''}
                        onChange={(e) => updateItem(index, 'remarks', e.target.value)}
                        placeholder="Optional…"
                        className={inputCls}
                      />
                    </td>
                    <td className="py-2">
                      <button
                        type="button"
                        onClick={() => removeItem(index)}
                        disabled={items.length === 1}
                        className="rounded-lg px-3 py-2 text-xs text-stone-500 transition hover:bg-red-900/30 hover:text-red-400 disabled:opacity-30"
                      >
                        ✕
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          ) : (
            <table className="w-full text-sm">
              <thead>
                <tr className="border-b border-white/10 text-left text-xs font-semibold tracking-wider text-stone-400 uppercase">
                  <th className="pb-3 pr-6">Batch #</th>
                  <th className="pb-3 pr-6">Product</th>
                  <th className="pb-3 pr-6">Expiry Date</th>
                  <th className="pb-3 pr-6 text-right">Qty</th>
                  <th className="pb-3 pr-6 text-right">Unit Price</th>
                  <th className="pb-3">Remarks</th>
                </tr>
              </thead>
              <tbody>
                {record.items.map((item) => (
                  <tr key={item.id} className="border-b border-white/5">
                    <td className="py-3 pr-6 font-mono text-xs text-amber-300">
                      {item.batchNumber}
                    </td>
                    <td className="py-3 pr-6 text-stone-200">
                      <span className="text-stone-400">{item.productSku}</span>
                      {' — '}
                      {item.productName}
                    </td>
                    <td className="py-3 pr-6 text-stone-300">{item.expiryDate}</td>
                    <td className="py-3 pr-6 text-right text-stone-300">{item.quantity}</td>
                    <td className="py-3 pr-6 text-right text-stone-300">
                      ${item.purchaseUnitPrice.toFixed(2)}
                    </td>
                    <td className="py-3 text-stone-400">{item.remarks ?? '—'}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </div>
      </div>

      {error && (
        <p className="rounded-xl bg-red-900/30 px-4 py-3 text-sm text-red-400">{error}</p>
      )}

      {editing && (
        <div className="flex gap-3">
          <button
            type="button"
            onClick={() => void handleSave()}
            disabled={updateInbound.isPending}
            className="rounded-xl bg-amber-300 px-6 py-2.5 text-sm font-semibold text-stone-900 transition hover:bg-amber-200 disabled:opacity-50"
          >
            {updateInbound.isPending ? 'Saving…' : 'Save Changes'}
          </button>
          <button
            type="button"
            onClick={handleCancelEdit}
            className="rounded-xl border border-white/10 px-6 py-2.5 text-sm font-medium text-stone-400 transition hover:bg-white/5 hover:text-stone-100"
          >
            Cancel
          </button>
        </div>
      )}
    </div>
  );
}
