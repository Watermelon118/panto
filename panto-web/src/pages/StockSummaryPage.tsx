import { useState } from 'react';
import { useStockSummary } from '../api/inventory';
import { Pagination } from '../components/Pagination';

const inputCls =
  'rounded-xl border border-white/10 bg-white/5 px-4 py-2.5 text-sm text-stone-100 outline-none transition focus:border-amber-300/50 focus:ring-2 focus:ring-amber-300/20 placeholder:text-stone-500';

export function StockSummaryPage() {
  const [page, setPage] = useState(0);
  const [keyword, setKeyword] = useState('');
  const [category, setCategory] = useState('');

  const { data, isLoading } = useStockSummary({
    keyword: keyword || undefined,
    category: category || undefined,
    page,
    size: 20,
  });

  const handleFilterChange = () => setPage(0);

  return (
    <div className="mx-auto max-w-6xl space-y-6">
      <div>
        <p className="text-xs font-semibold tracking-[0.18em] text-amber-300 uppercase">
          Inventory
        </p>
        <h1 className="mt-1 text-3xl font-black tracking-tight">Stock Summary</h1>
      </div>

      <div className="flex flex-wrap gap-3">
        <input
          type="text"
          value={keyword}
          onChange={(e) => { setKeyword(e.target.value); handleFilterChange(); }}
          placeholder="Search SKU or name…"
          className={inputCls}
        />
        <input
          type="text"
          value={category}
          onChange={(e) => { setCategory(e.target.value); handleFilterChange(); }}
          placeholder="Category…"
          className={inputCls}
        />
        {(keyword || category) && (
          <button
            type="button"
            onClick={() => { setKeyword(''); setCategory(''); setPage(0); }}
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
          <div className="p-12 text-center text-sm text-stone-500">No products found.</div>
        ) : (
          <table className="w-full text-sm">
            <thead>
              <tr className="border-b border-white/10 text-left text-xs font-semibold tracking-wider text-stone-400 uppercase">
                <th className="px-6 py-4">SKU</th>
                <th className="px-6 py-4">Name</th>
                <th className="px-6 py-4">Category</th>
                <th className="px-6 py-4">Unit</th>
                <th className="px-6 py-4 text-right">Safety Stock</th>
                <th className="px-6 py-4 text-right">Current Stock</th>
                <th className="px-6 py-4" />
              </tr>
            </thead>
            <tbody>
              {data?.items.map((item) => (
                <tr key={item.productId} className="border-b border-white/5">
                  <td className="px-6 py-4 font-mono text-xs text-amber-300">{item.sku}</td>
                  <td className="px-6 py-4 text-stone-200">{item.name}</td>
                  <td className="px-6 py-4 text-stone-400">{item.category}</td>
                  <td className="px-6 py-4 text-stone-400">{item.unit}</td>
                  <td className="px-6 py-4 text-right text-stone-400">{item.safetyStock}</td>
                  <td className="px-6 py-4 text-right">
                    <span className={item.belowSafetyStock ? 'font-semibold text-red-400' : 'text-stone-200'}>
                      {item.currentStock}
                    </span>
                  </td>
                  <td className="px-6 py-4 text-right">
                    {item.belowSafetyStock && (
                      <span className="rounded-full bg-red-900/40 px-2.5 py-0.5 text-xs font-semibold text-red-400">
                        Low
                      </span>
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
