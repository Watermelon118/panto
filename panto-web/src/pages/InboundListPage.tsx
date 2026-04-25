import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useInbounds } from '../api/inbound';
import { useProducts } from '../api/products';
import { Pagination } from '../components/Pagination';

const inputCls =
  'rounded-xl border border-white/10 bg-white/5 px-4 py-2.5 text-sm text-stone-100 outline-none transition focus:border-amber-300/50 focus:ring-2 focus:ring-amber-300/20 placeholder:text-stone-500';

const selectCls =
  'rounded-xl border border-white/10 bg-stone-900 px-4 py-2.5 text-sm text-stone-100 outline-none focus:border-amber-300/50';

export function InboundListPage() {
  const navigate = useNavigate();

  const [page, setPage] = useState(0);
  const [dateFrom, setDateFrom] = useState('');
  const [dateTo, setDateTo] = useState('');
  const [productId, setProductId] = useState<number | undefined>(undefined);

  const { data, isLoading } = useInbounds({
    dateFrom: dateFrom || undefined,
    dateTo: dateTo || undefined,
    productId,
    page,
    size: 20,
  });

  const { data: productsData } = useProducts({ page: 0, size: 100, active: true });

  const handleFilterChange = () => setPage(0);

  return (
    <div className="mx-auto max-w-6xl space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <p className="text-xs font-semibold tracking-[0.18em] text-amber-300 uppercase">
            Inventory
          </p>
          <h1 className="mt-1 text-3xl font-black tracking-tight">Inbound Records</h1>
        </div>
        <button
          type="button"
          onClick={() => navigate('/inbound/new')}
          className="rounded-xl bg-amber-300 px-5 py-2.5 text-sm font-semibold text-stone-900 transition hover:bg-amber-200"
        >
          + New Inbound
        </button>
      </div>

      <div className="flex flex-wrap gap-3">
        <input
          type="date"
          value={dateFrom}
          onChange={(e) => { setDateFrom(e.target.value); handleFilterChange(); }}
          className={inputCls}
          title="Date from"
        />
        <input
          type="date"
          value={dateTo}
          onChange={(e) => { setDateTo(e.target.value); handleFilterChange(); }}
          className={inputCls}
          title="Date to"
        />
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
        {(dateFrom || dateTo || productId) && (
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

      <div className="overflow-hidden rounded-[1.75rem] border border-white/10 bg-white/5">
        {isLoading ? (
          <div className="p-12 text-center text-sm text-stone-500">Loading…</div>
        ) : data?.items.length === 0 ? (
          <div className="p-12 text-center text-sm text-stone-500">No inbound records found.</div>
        ) : (
          <table className="w-full text-sm">
            <thead>
              <tr className="border-b border-white/10 text-left text-xs font-semibold tracking-wider text-stone-400 uppercase">
                <th className="px-6 py-4">Inbound #</th>
                <th className="px-6 py-4">Date</th>
                <th className="px-6 py-4 text-right">Items</th>
                <th className="px-6 py-4">Remarks</th>
                <th className="px-6 py-4">Created At</th>
                <th className="px-6 py-4" />
              </tr>
            </thead>
            <tbody>
              {data?.items.map((record) => (
                <tr
                  key={record.id}
                  onClick={() => navigate(`/inbound/${record.id}`)}
                  className="cursor-pointer border-b border-white/5 transition hover:bg-white/[0.03]"
                >
                  <td className="px-6 py-4 font-mono text-xs text-amber-300">
                    {record.inboundNumber}
                  </td>
                  <td className="px-6 py-4 text-stone-300">{record.inboundDate}</td>
                  <td className="px-6 py-4 text-right text-stone-300">{record.itemCount}</td>
                  <td className="max-w-xs px-6 py-4 truncate text-stone-400">
                    {record.remarks ?? '—'}
                  </td>
                  <td className="px-6 py-4 text-stone-500">
                    {new Date(record.createdAt).toLocaleDateString()}
                  </td>
                  <td className="px-6 py-4 text-right text-stone-500">→</td>
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
