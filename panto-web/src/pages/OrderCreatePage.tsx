import { useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useCustomers } from '../api/customers';
import { useBatches } from '../api/inventory';
import { useCreateOrder } from '../api/orders';
import { useProducts } from '../api/products';
import type { BatchItem, ExpiryStatus } from '../types/inventory';
import type { CreateOrderRequest } from '../types/order';
import type { ProductSummary } from '../types/product';
import { extractErrorMessage } from '../utils/error';

const inputCls =
  'w-full rounded-xl border border-white/10 bg-white/5 px-4 py-2.5 text-sm text-stone-100 outline-none transition focus:border-amber-300/50 focus:ring-2 focus:ring-amber-300/20 placeholder:text-stone-500';

const selectCls =
  'w-full rounded-xl border border-white/10 bg-stone-900 px-4 py-2.5 text-sm text-stone-100 outline-none focus:border-amber-300/50';

interface DraftOrderItem {
  productId: number;
  quantity: number;
  unitPrice: string;
}

const EMPTY_ITEM: DraftOrderItem = {
  productId: 0,
  quantity: 1,
  unitPrice: '',
};

const EMPTY_PRODUCT: ProductSummary = {
  id: 0,
  sku: '',
  name: '',
  category: '',
  specification: null,
  unit: '',
  referencePurchasePrice: 0,
  referenceSalePrice: 0,
  safetyStock: 0,
  gstApplicable: false,
  active: false,
  currentStock: 0,
};

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

function formatCurrency(value: number) {
  return `$${value.toFixed(2)}`;
}

function ExpiryChip({
  status,
  expiryDate,
  batchNumber,
}: {
  status: ExpiryStatus;
  expiryDate: string;
  batchNumber: string;
}) {
  if (status === 'NORMAL') {
    return null;
  }
  const isExpired = status === 'EXPIRED';
  const labelClass = isExpired
    ? 'bg-red-500/15 text-red-400 border-red-500/40'
    : 'bg-amber-300/15 text-amber-300 border-amber-300/40';
  const label = isExpired ? 'EXPIRED' : 'EXPIRING SOON';
  return (
    <p
      className={`mt-2 inline-flex items-center gap-2 rounded-full border px-2.5 py-1 text-xs font-semibold ${labelClass}`}
      title={`FIFO will deduct from batch ${batchNumber}`}
    >
      <span>{label}</span>
      <span className="text-stone-300/80">{batchNumber} · {expiryDate}</span>
    </p>
  );
}

function getEffectiveUnitPrice(item: DraftOrderItem, product?: ProductSummary) {
  if (item.unitPrice.trim() !== '') {
    return Number(item.unitPrice);
  }
  return product?.referenceSalePrice ?? 0;
}

export function OrderCreatePage() {
  const navigate = useNavigate();
  const createOrder = useCreateOrder();

  const [customerId, setCustomerId] = useState(0);
  const [remarks, setRemarks] = useState('');
  const [items, setItems] = useState<DraftOrderItem[]>([{ ...EMPTY_ITEM }]);
  const [error, setError] = useState<string | null>(null);

  const customersQuery = useCustomers({
    active: true,
    page: 0,
    size: 100,
  });
  const productsQuery = useProducts({
    active: true,
    page: 0,
    size: 100,
  });
  const batchesQuery = useBatches({ page: 0, size: 500 });

  const customerItems = customersQuery.data?.items;
  const productItems = productsQuery.data?.items;
  const customers = customerItems ?? [];
  const products = productItems ?? [];
  const queryError = customersQuery.error || productsQuery.error;
  const productMap = useMemo(
    () => new Map((productItems ?? []).map((product) => [product.id, product])),
    [productItems],
  );
  const nextFifoBatchByProduct = useMemo(() => {
    const map = new Map<number, BatchItem>();
    for (const batch of batchesQuery.data?.items ?? []) {
      if (batch.quantityRemaining <= 0) continue;
      const existing = map.get(batch.productId);
      if (!existing || batch.expiryDate < existing.expiryDate) {
        map.set(batch.productId, batch);
      }
    }
    return map;
  }, [batchesQuery.data]);
  const hasBlockingData = !!queryError || customers.length === 0 || products.length === 0;

  const orderPreview = useMemo(() => {
    let subtotalAmount = 0;
    let gstAmount = 0;

    for (const item of items) {
      const product = productMap.get(item.productId);
      const unitPrice = getEffectiveUnitPrice(item, product);
      const subtotal = unitPrice * item.quantity;
      subtotalAmount += subtotal;
      if (product?.gstApplicable) {
        gstAmount += subtotal * 0.15;
      }
    }

    return {
      subtotalAmount,
      gstAmount,
      totalAmount: subtotalAmount + gstAmount,
    };
  }, [items, productMap]);

  const updateItem = <K extends keyof DraftOrderItem>(
    index: number,
    key: K,
    value: DraftOrderItem[K],
  ) => {
    setError(null);
    setItems((prev) => prev.map((item, i) => (i === index ? { ...item, [key]: value } : item)));
  };

  const addItem = () => {
    setError(null);
    setItems((prev) => [...prev, { ...EMPTY_ITEM }]);
  };

  const removeItem = (index: number) => {
    setError(null);
    setItems((prev) => prev.filter((_, i) => i !== index));
  };

  const buildRequest = (): CreateOrderRequest | null => {
    if (!customers.length) {
      setError('No active customers are available. Create or enable a customer first.');
      return null;
    }

    if (!products.length) {
      setError('No active products are available. Create or enable a product first.');
      return null;
    }

    if (!customerId) {
      setError('Please select a customer before submitting the order.');
      return null;
    }

    const selectedProductIds = new Set<number>();
    for (const item of items) {
      if (!item.productId || item.quantity < 1) {
        setError('Please complete all required line item fields.');
        return null;
      }

      if (selectedProductIds.has(item.productId)) {
        setError('Each product can only appear once in the order.');
        return null;
      }
      selectedProductIds.add(item.productId);

      if (item.unitPrice.trim() !== '' && Number(item.unitPrice) < 0) {
        setError('Unit price cannot be negative.');
        return null;
      }

      const product = productMap.get(item.productId);
      if (product && item.quantity > product.currentStock) {
        setError(`Requested quantity for ${product.name} exceeds current stock.`);
        return null;
      }
    }

    return {
      customerId,
      remarks: remarks || undefined,
      items: items.map((item) => ({
        productId: item.productId,
        quantity: item.quantity,
        unitPrice: item.unitPrice.trim() === '' ? undefined : Number(item.unitPrice),
      })),
    };
  };

  const handleSubmit = async () => {
    setError(null);
    const request = buildRequest();
    if (!request) {
      return;
    }

    try {
      await createOrder.mutateAsync(request);
      navigate('/orders');
    } catch (submitError) {
      setError(extractErrorMessage(submitError));
    }
  };

  return (
    <div className="mx-auto max-w-6xl space-y-8">
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
          <h1 className="mt-1 text-3xl font-black tracking-tight">New Order</h1>
        </div>
      </div>

      {queryError && (
        <p className="rounded-xl bg-red-900/30 px-4 py-3 text-sm text-red-400">
          {extractErrorMessage(queryError)}
        </p>
      )}

      {!queryError && customers.length === 0 && !customersQuery.isLoading && (
        <p className="rounded-xl border border-amber-300/20 bg-amber-300/10 px-4 py-3 text-sm text-amber-100">
          No active customers are available yet. Add a customer before creating an order.
        </p>
      )}

      {!queryError && products.length === 0 && !productsQuery.isLoading && (
        <p className="rounded-xl border border-amber-300/20 bg-amber-300/10 px-4 py-3 text-sm text-amber-100">
          No active products are available yet. Add inventory-ready products before creating an order.
        </p>
      )}

      <div className="grid gap-6 lg:grid-cols-[minmax(0,1.5fr)_minmax(320px,0.9fr)]">
        <div className="space-y-6">
          <div className="rounded-[1.75rem] border border-white/10 bg-white/5 p-6 space-y-4">
            <h2 className="text-sm font-semibold text-stone-300">Order Details</h2>
            <div className="grid gap-4 md:grid-cols-2">
              <Field label="Customer" required>
                <select
                  value={customerId || ''}
                  onChange={(e) => {
                    setError(null);
                    setCustomerId(Number(e.target.value));
                  }}
                  className={selectCls}
                  disabled={customersQuery.isLoading || !!customersQuery.error || customers.length === 0}
                >
                  <option value="">
                    {customersQuery.isLoading ? 'Loading customers...' : 'Select customer...'}
                  </option>
                  {customers.map((customer) => (
                    <option key={customer.id} value={customer.id}>
                      {customer.companyName}
                    </option>
                  ))}
                </select>
              </Field>
              <Field label="Remarks">
                <input
                  type="text"
                  value={remarks}
                  onChange={(e) => {
                    setError(null);
                    setRemarks(e.target.value);
                  }}
                  className={inputCls}
                  placeholder="Optional order note"
                />
              </Field>
            </div>
          </div>

          <div className="rounded-[1.75rem] border border-white/10 bg-white/5 p-6 space-y-4">
            <div className="flex items-center justify-between">
              <h2 className="text-sm font-semibold text-stone-300">
                Line Items <span className="ml-1 text-stone-500">({items.length})</span>
              </h2>
              <button
                type="button"
                onClick={addItem}
                disabled={productsQuery.isLoading || !!productsQuery.error || products.length === 0}
                className="rounded-xl border border-amber-300/40 px-4 py-2 text-xs font-semibold text-amber-300 transition hover:bg-amber-300/10 disabled:cursor-not-allowed disabled:opacity-50"
              >
                + Add Row
              </button>
            </div>

            <div className="overflow-x-auto">
              <table className="w-full text-sm">
                <thead>
                  <tr className="border-b border-white/10 text-left text-xs font-semibold tracking-wider text-stone-400 uppercase">
                    <th className="pb-3 pr-4">Product</th>
                    <th className="pb-3 pr-4">Qty</th>
                    <th className="pb-3 pr-4">Unit Price</th>
                    <th className="pb-3 pr-4">Stock</th>
                    <th className="pb-3 pr-4">GST</th>
                    <th className="pb-3 pr-4 text-right">Line Total</th>
                    <th className="pb-3" />
                  </tr>
                </thead>
                <tbody>
                  {items.map((item, index) => {
                    const selectedProduct = productMap.get(item.productId);
                    const hasProduct = !!selectedProduct;
                    const product = selectedProduct ?? EMPTY_PRODUCT;
                    const effectiveUnitPrice = getEffectiveUnitPrice(item, product);
                    const lineTotal = effectiveUnitPrice * item.quantity;
                    const fifoBatch = hasProduct
                      ? nextFifoBatchByProduct.get(item.productId)
                      : undefined;

                    return (
                      <tr key={index} className="border-b border-white/5 align-top">
                        <td className="py-3 pr-4 min-w-[260px]">
                          <select
                            value={item.productId || ''}
                            onChange={(e) => updateItem(index, 'productId', Number(e.target.value))}
                            className={selectCls}
                            disabled={productsQuery.isLoading || !!productsQuery.error || products.length === 0}
                          >
                            <option value="">
                              {productsQuery.isLoading ? 'Loading products...' : 'Select product...'}
                            </option>
                            {products.map((candidate) => {
                              const alreadySelected = items.some(
                                (existingItem, existingIndex) =>
                                  existingIndex !== index && existingItem.productId === candidate.id,
                              );

                              return (
                                <option
                                  key={candidate.id}
                                  value={candidate.id}
                                  disabled={alreadySelected}
                                >
                                  {candidate.sku} - {candidate.name}
                                </option>
                              );
                            })}
                          </select>
                          {hasProduct && (
                            <p className="mt-2 text-xs text-stone-500">
                              {product.category} | {product.unit}
                              {product.specification ? ` | ${product.specification}` : ''}
                            </p>
                          )}
                          {fifoBatch && (
                            <ExpiryChip
                              status={fifoBatch.expiryStatus}
                              expiryDate={fifoBatch.expiryDate}
                              batchNumber={fifoBatch.batchNumber}
                            />
                          )}
                        </td>
                        <td className="py-3 pr-4 min-w-[110px]">
                          <input
                            type="number"
                            min="1"
                            value={item.quantity}
                            onChange={(e) => updateItem(index, 'quantity', Number(e.target.value))}
                            className={inputCls}
                          />
                        </td>
                        <td className="py-3 pr-4 min-w-[150px]">
                          <input
                            type="number"
                            min="0"
                            step="0.01"
                            value={item.unitPrice}
                            onChange={(e) => updateItem(index, 'unitPrice', e.target.value)}
                            className={inputCls}
                            placeholder={product ? `${product.referenceSalePrice.toFixed(2)} default` : 'Default'}
                          />
                        </td>
                        <td className="py-3 pr-4 min-w-[90px] text-stone-300">
                          {hasProduct ? product.currentStock : '-'}
                        </td>
                        <td className="py-3 pr-4 min-w-[90px]">
                          {hasProduct ? (
                            <span
                              className={`rounded-full px-2.5 py-1 text-xs font-semibold ${
                                product.gstApplicable
                                  ? 'bg-emerald-400/15 text-emerald-400'
                                  : 'bg-stone-700 text-stone-300'
                              }`}
                            >
                              {product.gstApplicable ? 'GST' : 'No GST'}
                            </span>
                          ) : (
                            <span className="text-stone-500">-</span>
                          )}
                        </td>
                        <td className="py-3 pr-4 min-w-[120px] text-right font-medium text-stone-100">
                          {hasProduct ? formatCurrency(lineTotal) : '-'}
                        </td>
                        <td className="py-3 text-right">
                          <button
                            type="button"
                            onClick={() => removeItem(index)}
                            disabled={items.length === 1}
                            className="rounded-lg px-3 py-2 text-xs text-stone-500 transition hover:bg-red-900/30 hover:text-red-400 disabled:opacity-30"
                          >
                            Remove
                          </button>
                        </td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>
            </div>
          </div>
        </div>

        <aside className="space-y-4">
          <div className="rounded-[1.75rem] border border-white/10 bg-stone-900/80 p-6">
            <p className="text-xs font-semibold tracking-[0.14em] text-stone-400 uppercase">
              Order Summary
            </p>
            <div className="mt-5 space-y-3 text-sm">
              <div className="flex items-center justify-between text-stone-300">
                <span>Subtotal</span>
                <span>{formatCurrency(orderPreview.subtotalAmount)}</span>
              </div>
              <div className="flex items-center justify-between text-stone-300">
                <span>GST</span>
                <span>{formatCurrency(orderPreview.gstAmount)}</span>
              </div>
              <div className="flex items-center justify-between border-t border-white/10 pt-3 text-base font-semibold text-white">
                <span>Total</span>
                <span>{formatCurrency(orderPreview.totalAmount)}</span>
              </div>
            </div>
          </div>

          <div className="rounded-[1.75rem] border border-white/10 bg-white/5 p-6 text-sm leading-7 text-stone-300">
            <p className="font-semibold text-stone-100">Pricing notes</p>
            <p className="mt-2">
              Leave unit price blank to use the product reference sale price. Line items are checked against current
              stock before the order is submitted.
            </p>
          </div>

          {error && (
            <p className="rounded-xl bg-red-900/30 px-4 py-3 text-sm text-red-400">{error}</p>
          )}

          <div className="space-y-3">
            <button
              type="button"
              onClick={() => void handleSubmit()}
              disabled={createOrder.isPending || hasBlockingData}
              className="w-full rounded-xl bg-amber-300 px-6 py-2.5 text-sm font-semibold text-stone-900 transition hover:bg-amber-200 disabled:opacity-50"
            >
              {createOrder.isPending ? 'Submitting...' : 'Create Order'}
            </button>
            <button
              type="button"
              onClick={() => navigate('/orders')}
              className="w-full rounded-xl border border-white/10 px-6 py-2.5 text-sm font-medium text-stone-400 transition hover:bg-white/5 hover:text-stone-100"
            >
              Cancel
            </button>
          </div>
        </aside>
      </div>
    </div>
  );
}
