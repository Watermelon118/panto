import { useExpiringBatches, useLowStockProducts } from '../api/inventory';

function LoadingState({ label }: { label: string }) {
  return <p className="text-sm text-stone-500">{label}</p>;
}

function EmptyState({ label }: { label: string }) {
  return (
    <div className="rounded-2xl border border-dashed border-white/10 bg-stone-950/40 px-4 py-6 text-sm text-stone-500">
      {label}
    </div>
  );
}

export function DashboardPage() {
  const {
    data: lowStockProducts,
    isLoading: isLowStockLoading,
    isFetching: isLowStockFetching,
  } = useLowStockProducts();
  const {
    data: expiringBatches,
    isLoading: isExpiringLoading,
    isFetching: isExpiringFetching,
  } = useExpiringBatches(30);

  const lowStockPreview = lowStockProducts?.slice(0, 5) ?? [];
  const expiringPreview = expiringBatches?.slice(0, 5) ?? [];

  return (
    <div className="mx-auto max-w-7xl space-y-8">
      <header className="flex flex-col gap-4 rounded-[2rem] border border-white/10 bg-white/5 p-8 shadow-[0_30px_80px_rgba(0,0,0,0.28)] backdrop-blur sm:flex-row sm:items-end sm:justify-between">
        <div className="space-y-3">
          <p className="text-sm font-semibold tracking-[0.18em] text-amber-300 uppercase">
            Panto Dashboard
          </p>
          <h1 className="text-4xl font-black tracking-tight text-balance sm:text-5xl">
            Inventory attention points, surfaced early.
          </h1>
          <p className="max-w-2xl text-sm leading-7 text-stone-300 sm:text-base">
            Milestone 3 now highlights products below safety stock and batches expiring in the
            next 30 days, so the warehouse team can react before issues become stock losses.
          </p>
        </div>

        <div className="rounded-3xl border border-amber-300/20 bg-amber-300/10 px-5 py-4 text-sm leading-6 text-amber-100">
          <p className="font-semibold">Milestone 3</p>
          <p className="mt-1">Inventory queries and alerts are now live.</p>
        </div>
      </header>

      <section className="grid gap-5 xl:grid-cols-[1.2fr_1.2fr_0.8fr]">
        <article className="rounded-[1.75rem] border border-red-500/20 bg-red-950/10 p-6">
          <div className="flex items-start justify-between gap-4">
            <div>
              <p className="text-sm font-semibold tracking-[0.14em] text-red-300 uppercase">
                Low Stock
              </p>
              <h2 className="mt-3 text-2xl font-bold text-white">Below safety threshold</h2>
            </div>
            <div className="rounded-2xl bg-red-500/10 px-4 py-3 text-right">
              <p className="text-xs font-semibold tracking-[0.14em] text-red-200 uppercase">
                Products
              </p>
              <p className="mt-1 text-2xl font-black text-red-300">
                {lowStockProducts?.length ?? 0}
              </p>
            </div>
          </div>

          <div className="mt-5 space-y-3">
            {isLowStockLoading ? <LoadingState label="Loading low-stock products..." /> : null}
            {!isLowStockLoading && lowStockPreview.length === 0 ? (
              <EmptyState label="No products are currently below safety stock." />
            ) : null}
            {lowStockPreview.map((product) => (
              <div
                key={product.productId}
                className="rounded-2xl border border-white/10 bg-stone-950/40 px-4 py-4"
              >
                <div className="flex items-start justify-between gap-4">
                  <div>
                    <p className="font-semibold text-stone-100">{product.name}</p>
                    <p className="mt-1 text-xs text-stone-500">
                      {product.sku} · {product.category}
                    </p>
                  </div>
                  <span className="rounded-full bg-red-500/10 px-3 py-1 text-xs font-semibold text-red-300">
                    Low
                  </span>
                </div>
                <div className="mt-4 grid grid-cols-2 gap-3 text-sm">
                  <div className="rounded-xl bg-white/5 px-3 py-2">
                    <p className="text-xs uppercase tracking-[0.14em] text-stone-500">
                      Current
                    </p>
                    <p className="mt-1 font-semibold text-white">
                      {product.currentStock} {product.unit}
                    </p>
                  </div>
                  <div className="rounded-xl bg-white/5 px-3 py-2">
                    <p className="text-xs uppercase tracking-[0.14em] text-stone-500">
                      Safety
                    </p>
                    <p className="mt-1 font-semibold text-white">
                      {product.safetyStock} {product.unit}
                    </p>
                  </div>
                </div>
              </div>
            ))}
          </div>

          {isLowStockFetching && !isLowStockLoading ? (
            <p className="mt-4 text-xs text-stone-500">Refreshing low-stock signals...</p>
          ) : null}
        </article>

        <article className="rounded-[1.75rem] border border-amber-400/20 bg-amber-950/10 p-6">
          <div className="flex items-start justify-between gap-4">
            <div>
              <p className="text-sm font-semibold tracking-[0.14em] text-amber-300 uppercase">
                Expiring Soon
              </p>
              <h2 className="mt-3 text-2xl font-bold text-white">Batches within 30 days</h2>
            </div>
            <div className="rounded-2xl bg-amber-400/10 px-4 py-3 text-right">
              <p className="text-xs font-semibold tracking-[0.14em] text-amber-200 uppercase">
                Batches
              </p>
              <p className="mt-1 text-2xl font-black text-amber-300">
                {expiringBatches?.length ?? 0}
              </p>
            </div>
          </div>

          <div className="mt-5 space-y-3">
            {isExpiringLoading ? <LoadingState label="Loading expiring batches..." /> : null}
            {!isExpiringLoading && expiringPreview.length === 0 ? (
              <EmptyState label="No active batches are due to expire in the next 30 days." />
            ) : null}
            {expiringPreview.map((batch) => (
              <div
                key={batch.id}
                className="rounded-2xl border border-white/10 bg-stone-950/40 px-4 py-4"
              >
                <div className="flex items-start justify-between gap-4">
                  <div>
                    <p className="font-semibold text-stone-100">{batch.productName}</p>
                    <p className="mt-1 text-xs text-stone-500">
                      {batch.productSku} · Batch {batch.batchNumber}
                    </p>
                  </div>
                  <span className="rounded-full bg-amber-400/10 px-3 py-1 text-xs font-semibold text-amber-300">
                    {batch.expiryStatus === 'EXPIRED' ? 'Expired' : 'Warning'}
                  </span>
                </div>
                <div className="mt-4 grid grid-cols-2 gap-3 text-sm">
                  <div className="rounded-xl bg-white/5 px-3 py-2">
                    <p className="text-xs uppercase tracking-[0.14em] text-stone-500">
                      Expiry
                    </p>
                    <p className="mt-1 font-semibold text-white">{batch.expiryDate}</p>
                  </div>
                  <div className="rounded-xl bg-white/5 px-3 py-2">
                    <p className="text-xs uppercase tracking-[0.14em] text-stone-500">
                      Remaining
                    </p>
                    <p className="mt-1 font-semibold text-white">{batch.quantityRemaining}</p>
                  </div>
                </div>
              </div>
            ))}
          </div>

          {isExpiringFetching && !isExpiringLoading ? (
            <p className="mt-4 text-xs text-stone-500">Refreshing expiry alerts...</p>
          ) : null}
        </article>

        <article className="rounded-[1.75rem] border border-white/10 bg-white/5 p-6">
          <p className="text-sm font-semibold tracking-[0.14em] text-stone-400 uppercase">
            Operations
          </p>
          <h2 className="mt-3 text-2xl font-bold text-white">Next control surface</h2>
          <div className="mt-5 space-y-3 text-sm leading-7 text-stone-300">
            <p>
              Inventory alerts are now visible on the dashboard. The next step is wiring the stock,
              batch, and transaction pages into the app shell navigation.
            </p>
            <div className="rounded-2xl border border-white/10 bg-stone-950/40 p-4">
              <p className="text-xs font-semibold tracking-[0.14em] text-stone-500 uppercase">
                Ready for commit 5
              </p>
              <ul className="mt-3 space-y-2 text-stone-300">
                <li>Register inventory routes</li>
                <li>Add inventory entries to the sidebar</li>
                <li>Make dashboard cards discoverable from app navigation</li>
              </ul>
            </div>
          </div>
        </article>
      </section>
    </div>
  );
}
