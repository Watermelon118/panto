import { useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import { useLowStockProducts, useStockSummary } from '../api/inventory';
import { Pagination } from '../components/Pagination';

const inputCls =
  'rounded-xl border border-white/10 bg-white/5 px-4 py-2.5 text-sm text-stone-100 outline-none transition focus:border-amber-300/50 focus:ring-2 focus:ring-amber-300/20 placeholder:text-stone-500';

export function StockSummaryPage() {
  const [searchParams, setSearchParams] = useSearchParams();
  const [page, setPage] = useState(0);
  const [keyword, setKeyword] = useState('');
  const [category, setCategory] = useState('');
  const isLowStockView = searchParams.get('lowStock') === 'true';

  const stockSummaryQuery = useStockSummary(
    {
      keyword: keyword || undefined,
      category: category || undefined,
      page,
      size: 20,
    },
    !isLowStockView,
  );
  const lowStockQuery = useLowStockProducts(isLowStockView);

  const handleFilterChange = () => setPage(0);
  const normalizedKeyword = keyword.trim().toLowerCase();
  const normalizedCategory = category.trim().toLowerCase();
  const lowStockItems = (lowStockQuery.data ?? []).filter((item) => {
    const matchesKeyword =
      !normalizedKeyword ||
      item.sku.toLowerCase().includes(normalizedKeyword) ||
      item.name.toLowerCase().includes(normalizedKeyword);
    const matchesCategory =
      !normalizedCategory || item.category.toLowerCase().includes(normalizedCategory);

    return matchesKeyword && matchesCategory;
  });
  const items = isLowStockView ? lowStockItems : (stockSummaryQuery.data?.items ?? []);
  const isLoading = isLowStockView ? lowStockQuery.isLoading : stockSummaryQuery.isLoading;

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
        {(keyword || category || isLowStockView) && (
          <button
            type="button"
            onClick={() => { setKeyword(''); setCategory(''); setSearchParams({}); setPage(0); }}
            className="rounded-xl border border-white/10 px-4 py-2.5 text-sm text-stone-400 transition hover:bg-white/5 hover:text-stone-100"
          >
            Clear filters
          </button>
        )}
        {isLowStockView && (
          <span className="rounded-full border border-red-400/30 bg-red-950/20 px-3 py-2 text-xs font-semibold text-red-300">
            Low stock only
          </span>
        )}
      </div>

      <div className="overflow-hidden rounded-[1.75rem] border border-white/10 bg-white/5">
        {isLoading ? (
          <div className="p-12 text-center text-sm text-stone-500">Loading…</div>
        ) : items.length === 0 ? (
          <div className="p-12 text-center text-sm text-stone-500">
            {isLowStockView ? 'No low-stock products found.' : 'No products found.'}
          </div>
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
              {items.map((item) => (
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

      {!isLowStockView && stockSummaryQuery.data && stockSummaryQuery.data.totalPages > 1 && (
        <Pagination
          page={stockSummaryQuery.data.page}
          totalPages={stockSummaryQuery.data.totalPages}
          totalElements={stockSummaryQuery.data.totalElements}
          size={stockSummaryQuery.data.size}
          onPageChange={setPage}
        />
      )}
    </div>
  );
}
