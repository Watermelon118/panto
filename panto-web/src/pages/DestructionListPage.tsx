import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useDestructions } from '../api/destruction';
import { useProducts } from '../api/products';
import { Pagination } from '../components/Pagination';
import { useAuthStore } from '../store/auth-store';
import type { ExpiryStatus } from '../types/inventory';
import { extractErrorMessage } from '../utils/error';

const inputCls =
  'rounded-xl border border-white/10 bg-white/5 px-4 py-2.5 text-sm text-stone-100 outline-none transition focus:border-amber-300/50 focus:ring-2 focus:ring-amber-300/20 placeholder:text-stone-500';

const selectCls =
  'rounded-xl border border-white/10 bg-stone-900 px-4 py-2.5 text-sm text-stone-100 outline-none focus:border-amber-300/50';

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

function formatDateTime(value: string) {
  return new Intl.DateTimeFormat('en-NZ', {
    dateStyle: 'medium',
    timeStyle: 'short',
  }).format(new Date(value));
}

export function DestructionListPage() {
  const navigate = useNavigate();
  const user = useAuthStore((state) => state.user);

  const [page, setPage] = useState(0);
  const [productId, setProductId] = useState<number | undefined>(undefined);
  const [dateFrom, setDateFrom] = useState('');
  const [dateTo, setDateTo] = useState('');

  const destructionsQuery = useDestructions({
    productId,
    dateFrom: dateFrom || undefined,
    dateTo: dateTo || undefined,
    page,
    size: 20,
  });
  const productsQuery = useProducts({ page: 0, size: 100, active: true });

  const canCreate = user?.role === 'ADMIN' || user?.role === 'WAREHOUSE';
  const hasFilter = Boolean(productId || dateFrom || dateTo);
  const errorMessage = destructionsQuery.error
    ? extractErrorMessage(destructionsQuery.error)
    : null;

  return (
    <div className="mx-auto max-w-6xl space-y-6">
      <div className="flex flex-wrap items-center justify-between gap-4">
        <div>
          <p className="text-xs font-semibold tracking-[0.18em] text-amber-300 uppercase">
            Inventory
          </p>
          <h1 className="mt-1 text-3xl font-black tracking-tight">Destructions</h1>
        </div>
        {canCreate && (
          <button
            type="button"
            onClick={() => navigate('/destructions/new')}
            className="rounded-xl bg-amber-300 px-5 py-2.5 text-sm font-semibold text-stone-900 transition hover:bg-amber-200"
          >
            + New Destruction
          </button>
        )}
      </div>

      <div className="flex flex-wrap gap-3">
        <input
          type="date"
          value={dateFrom}
          onChange={(event) => {
            setDateFrom(event.target.value);
            setPage(0);
          }}
          className={inputCls}
          title="Date from"
        />
        <input
          type="date"
          value={dateTo}
          onChange={(event) => {
            setDateTo(event.target.value);
            setPage(0);
          }}
          className={inputCls}
          title="Date to"
        />
        <select
          value={productId ?? ''}
          onChange={(event) => {
            setProductId(event.target.value ? Number(event.target.value) : undefined);
            setPage(0);
          }}
          className={selectCls}
          disabled={productsQuery.isLoading || Boolean(productsQuery.error)}
        >
          <option value="">All products</option>
          {productsQuery.data?.items.map((product) => (
            <option key={product.id} value={product.id}>
              {product.sku} - {product.name}
            </option>
          ))}
        </select>

        {hasFilter && (
          <button
            type="button"
            onClick={() => {
              setDateFrom('');
              setDateTo('');
              setProductId(undefined);
              setPage(0);
            }}
            className="rounded-xl border border-white/10 px-4 py-2.5 text-sm text-stone-400 transition hover:bg-white/5 hover:text-stone-100"
          >
            Clear filters
          </button>
        )}
      </div>

      {productsQuery.error && (
        <div className="rounded-2xl border border-amber-300/20 bg-amber-300/10 px-5 py-4 text-sm leading-6 text-amber-100">
          Product filter options are temporarily unavailable. You can still review destruction records and refresh later.
        </div>
      )}

      {errorMessage && (
        <p className="rounded-xl bg-red-900/30 px-4 py-3 text-sm text-red-400">{errorMessage}</p>
      )}

      <div className="overflow-hidden rounded-[1.75rem] border border-white/10 bg-white/5">
        {destructionsQuery.isLoading ? (
          <div className="p-12 text-center text-sm text-stone-500">Loading...</div>
        ) : destructionsQuery.data?.items.length === 0 ? (
          <div className="p-12 text-center text-sm text-stone-500">
            No destruction records found.
          </div>
        ) : (
          <table className="w-full text-sm">
            <thead>
              <tr className="border-b border-white/10 text-left text-xs font-semibold tracking-wider text-stone-400 uppercase">
                <th className="px-6 py-4">Destruction #</th>
                <th className="px-6 py-4">Product</th>
                <th className="px-6 py-4">Batch</th>
                <th className="px-6 py-4">Expiry</th>
                <th className="px-6 py-4 text-right">Destroyed</th>
                <th className="px-6 py-4 text-right">Loss</th>
                <th className="px-6 py-4">Reason</th>
                <th className="px-6 py-4">Created</th>
              </tr>
            </thead>
            <tbody>
              {destructionsQuery.data?.items.map((item) => (
                <tr
                  key={item.id}
                  onClick={() => navigate(`/destructions/${item.id}`)}
                  className="cursor-pointer border-b border-white/5 transition hover:bg-white/[0.03]"
                >
                  <td className="px-6 py-4 font-mono text-xs text-amber-300">
                    {item.destructionNumber}
                  </td>
                  <td className="px-6 py-4 text-stone-200">
                    <div className="font-medium text-stone-100">{item.productName}</div>
                    <div className="text-xs text-stone-500">{item.productSku}</div>
                  </td>
                  <td className="px-6 py-4">
                    <div className="font-mono text-xs text-stone-300">{item.batchNumber}</div>
                    <div className="mt-1">
                      <span
                        className={`rounded-full px-2.5 py-0.5 text-xs font-semibold ${EXPIRY_STATUS_CLS[item.batchExpiryStatus]}`}
                      >
                        {EXPIRY_STATUS_LABELS[item.batchExpiryStatus]}
                      </span>
                    </div>
                  </td>
                  <td className="px-6 py-4 text-stone-300">{item.batchExpiryDate}</td>
                  <td className="px-6 py-4 text-right text-stone-200">{item.quantityDestroyed}</td>
                  <td className="px-6 py-4 text-right font-medium text-stone-100">
                    {formatCurrency(item.lossAmount)}
                  </td>
                  <td className="max-w-xs px-6 py-4 truncate text-stone-400">{item.reason}</td>
                  <td className="px-6 py-4 text-stone-400">{formatDateTime(item.createdAt)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>

      {destructionsQuery.data && destructionsQuery.data.totalPages > 1 && (
        <Pagination
          page={destructionsQuery.data.page}
          totalPages={destructionsQuery.data.totalPages}
          totalElements={destructionsQuery.data.totalElements}
          size={destructionsQuery.data.size}
          onPageChange={setPage}
        />
      )}
    </div>
  );
}
