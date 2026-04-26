import { useEffect, useState } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { useBatches } from '../api/inventory';
import { useProducts } from '../api/products';
import { Pagination } from '../components/Pagination';
import type { ExpiryStatus } from '../types/inventory';

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

function parseExpiryStatus(value: string | null): ExpiryStatus | undefined {
  if (value === 'NORMAL' || value === 'EXPIRING_SOON' || value === 'EXPIRED') {
    return value;
  }
  return undefined;
}

export function BatchListPage() {
  const navigate = useNavigate();
  const [searchParams, setSearchParams] = useSearchParams();
  const [page, setPage] = useState(0);
  const [productId, setProductId] = useState<number | undefined>(undefined);
  const [expiryStatus, setExpiryStatus] = useState<ExpiryStatus | undefined>(() =>
    parseExpiryStatus(searchParams.get('status')),
  );

  useEffect(() => {
    setExpiryStatus(parseExpiryStatus(searchParams.get('status')));
    setPage(0);
  }, [searchParams]);

  const { data, isLoading } = useBatches({
    productId,
    expiryStatus,
    page,
    size: 20,
  });

  const { data: productsData } = useProducts({ page: 0, size: 100, active: true });

  const handleFilterChange = () => setPage(0);

  const hasFilter = productId !== undefined || expiryStatus !== undefined;

  return (
    <div className="mx-auto max-w-6xl space-y-6">
      <div>
        <p className="text-xs font-semibold tracking-[0.18em] text-amber-300 uppercase">
          Inventory
        </p>
        <h1 className="mt-1 text-3xl font-black tracking-tight">Batches</h1>
      </div>

      <div className="flex flex-wrap gap-3">
        <select
          value={productId ?? ''}
          onChange={(e) => {
            setProductId(e.target.value ? Number(e.target.value) : undefined);
            handleFilterChange();
          }}
          className={selectCls}
        >
          <option value="">All products</option>
          {productsData?.items.map((p) => (
            <option key={p.id} value={p.id}>
              {p.sku} — {p.name}
            </option>
          ))}
        </select>

        <select
          value={expiryStatus ?? ''}
          onChange={(e) => {
            const nextStatus = parseExpiryStatus(e.target.value);
            setExpiryStatus(nextStatus);
            setSearchParams(nextStatus ? { status: nextStatus } : {});
            handleFilterChange();
          }}
          className={selectCls}
        >
          <option value="">All statuses</option>
          {(Object.keys(EXPIRY_STATUS_LABELS) as ExpiryStatus[]).map((s) => (
            <option key={s} value={s}>
              {EXPIRY_STATUS_LABELS[s]}
            </option>
          ))}
        </select>

        {hasFilter && (
          <button
            type="button"
            onClick={() => {
              setProductId(undefined);
              setExpiryStatus(undefined);
              setSearchParams({});
              setPage(0);
            }}
            className="rounded-xl border border-white/10 px-4 py-2.5 text-sm text-stone-400 transition hover:bg-white/5 hover:text-stone-100"
          >
            Clear filters
          </button>
        )}
      </div>

      <div className="overflow-hidden rounded-[1.75rem] border border-white/10 bg-white/5">
        {isLoading ? (
          <div className="p-12 text-center text-sm text-stone-500">Loading…</div>
        ) : data?.items.length === 0 ? (
          <div className="p-12 text-center text-sm text-stone-500">No batches found.</div>
        ) : (
          <table className="w-full text-sm">
            <thead>
              <tr className="border-b border-white/10 text-left text-xs font-semibold tracking-wider text-stone-400 uppercase">
                <th className="px-6 py-4">Batch #</th>
                <th className="px-6 py-4">Product</th>
                <th className="px-6 py-4">Arrival</th>
                <th className="px-6 py-4">Expiry</th>
                <th className="px-6 py-4 text-right">Received</th>
                <th className="px-6 py-4 text-right">Remaining</th>
                <th className="px-6 py-4 text-right">Unit Price</th>
                <th className="px-6 py-4">Status</th>
                <th className="px-6 py-4 text-right">Action</th>
              </tr>
            </thead>
            <tbody>
              {data?.items.map((batch) => (
                <tr key={batch.id} className="border-b border-white/5">
                  <td className="px-6 py-4 font-mono text-xs text-amber-300">{batch.batchNumber}</td>
                  <td className="px-6 py-4 text-stone-200">
                    <span className="text-stone-400">{batch.productSku}</span>
                    {' — '}
                    {batch.productName}
                  </td>
                  <td className="px-6 py-4 text-stone-300">{batch.arrivalDate}</td>
                  <td className="px-6 py-4 text-stone-300">{batch.expiryDate}</td>
                  <td className="px-6 py-4 text-right text-stone-400">{batch.quantityReceived}</td>
                  <td className="px-6 py-4 text-right text-stone-200">{batch.quantityRemaining}</td>
                  <td className="px-6 py-4 text-right text-stone-300">
                    ${batch.purchaseUnitPrice.toFixed(2)}
                  </td>
                  <td className="px-6 py-4">
                    <span className={`rounded-full px-2.5 py-0.5 text-xs font-semibold ${EXPIRY_STATUS_CLS[batch.expiryStatus]}`}>
                      {EXPIRY_STATUS_LABELS[batch.expiryStatus]}
                    </span>
                  </td>
                  <td className="px-6 py-4 text-right">
                    {batch.quantityRemaining > 0 && batch.expiryStatus !== 'NORMAL' ? (
                      <button
                        type="button"
                        onClick={() => navigate(`/destructions/new?productId=${batch.productId}&batchId=${batch.id}`)}
                        className="rounded-lg border border-red-400/30 px-3 py-1.5 text-xs font-medium text-red-300 transition hover:bg-red-500/10 hover:text-red-200"
                      >
                        Destroy
                      </button>
                    ) : (
                      <span className="text-xs text-stone-500">—</span>
                    )}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>

      {data && data.totalPages > 1 && (
        <Pagination
          page={data.page}
          totalPages={data.totalPages}
          totalElements={data.totalElements}
          size={data.size}
          onPageChange={setPage}
        />
      )}
    </div>
  );
}
