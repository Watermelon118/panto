import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useCreateInbound } from '../api/inbound';
import { useProducts } from '../api/products';
import type { InboundItemRequest } from '../types/inbound';
import { extractErrorMessage } from '../utils/error';

const inputCls =
  'w-full rounded-xl border border-white/10 bg-white/5 px-4 py-2.5 text-sm text-stone-100 outline-none transition focus:border-amber-300/50 focus:ring-2 focus:ring-amber-300/20 placeholder:text-stone-500';

const selectCls =
  'w-full rounded-xl border border-white/10 bg-stone-900 px-4 py-2.5 text-sm text-stone-100 outline-none focus:border-amber-300/50';

function Field({
  label,
  required,
  children,
}: {
  label: string;
  required?: boolean;
  children: React.ReactNode;
}) {
  return (
    <label className="block space-y-1.5">
      <span className="text-xs font-medium text-stone-400">
        {label}
        {required && <span className="ml-0.5 text-amber-300">*</span>}
      </span>
      {children}
    </label>
  );
}

const EMPTY_ITEM: InboundItemRequest = {
  productId: 0,
  expiryDate: '',
  quantity: 1,
  purchaseUnitPrice: 0,
  remarks: '',
};

export function InboundCreatePage() {
  const navigate = useNavigate();
  const createInbound = useCreateInbound();

  const [inboundDate, setInboundDate] = useState(new Date().toISOString().slice(0, 10));
  const [remarks, setRemarks] = useState('');
  const [items, setItems] = useState<InboundItemRequest[]>([{ ...EMPTY_ITEM }]);
  const [error, setError] = useState<string | null>(null);

  const { data: productsData } = useProducts({ page: 0, size: 100, active: true });
  const activeProducts = productsData?.items ?? [];

  const updateItem = <K extends keyof InboundItemRequest>(
    index: number,
    key: K,
    value: InboundItemRequest[K],
  ) => {
    setItems((prev) => prev.map((item, i) => (i === index ? { ...item, [key]: value } : item)));
  };

  const addItem = () => setItems((prev) => [...prev, { ...EMPTY_ITEM }]);

  const removeItem = (index: number) =>
    setItems((prev) => prev.filter((_, i) => i !== index));

  const handleSubmit = async () => {
    setError(null);

    const invalidItem = items.find((it) => it.productId === 0 || !it.expiryDate || it.quantity < 1);
    if (invalidItem) {
      setError('Please fill in all required fields for each item row.');
      return;
    }

    try {
      const result = await createInbound.mutateAsync({
        inboundDate,
        remarks: remarks || undefined,
        items,
      });
      navigate(`/inbound/${result.id}`);
    } catch (err) {
      setError(extractErrorMessage(err));
    }
  };

  return (
    <div className="mx-auto max-w-5xl space-y-8">
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
          <h1 className="mt-1 text-3xl font-black tracking-tight">New Inbound Record</h1>
        </div>
      </div>

      {/* Header fields */}
      <div className="rounded-[1.75rem] border border-white/10 bg-white/5 p-6 space-y-4">
        <h2 className="text-sm font-semibold text-stone-300">Record Details</h2>
        <div className="grid grid-cols-2 gap-4">
          <Field label="Inbound Date" required>
            <input
              type="date"
              value={inboundDate}
              onChange={(e) => setInboundDate(e.target.value)}
              className={inputCls}
            />
          </Field>
          <Field label="Remarks">
            <input
              type="text"
              value={remarks}
              onChange={(e) => setRemarks(e.target.value)}
              placeholder="Optional remarks…"
              className={inputCls}
            />
          </Field>
        </div>
      </div>

      {/* Item rows */}
      <div className="rounded-[1.75rem] border border-white/10 bg-white/5 p-6 space-y-4">
        <div className="flex items-center justify-between">
          <h2 className="text-sm font-semibold text-stone-300">
            Line Items <span className="ml-1 text-stone-500">({items.length})</span>
          </h2>
          <button
            type="button"
            onClick={addItem}
            className="rounded-xl border border-amber-300/40 px-4 py-2 text-xs font-semibold text-amber-300 transition hover:bg-amber-300/10"
          >
            + Add Row
          </button>
        </div>

        <div className="overflow-x-auto">
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
            <tbody className="space-y-2">
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
        </div>
      </div>

      {error && (
        <p className="rounded-xl bg-red-900/30 px-4 py-3 text-sm text-red-400">{error}</p>
      )}

      <div className="flex gap-3">
        <button
          type="button"
          onClick={() => void handleSubmit()}
          disabled={createInbound.isPending}
          className="rounded-xl bg-amber-300 px-6 py-2.5 text-sm font-semibold text-stone-900 transition hover:bg-amber-200 disabled:opacity-50"
        >
          {createInbound.isPending ? 'Saving…' : 'Create Inbound Record'}
        </button>
        <button
          type="button"
          onClick={() => navigate('/inbound')}
          className="rounded-xl border border-white/10 px-6 py-2.5 text-sm font-medium text-stone-400 transition hover:bg-white/5 hover:text-stone-100"
        >
          Cancel
        </button>
      </div>
    </div>
  );
}
