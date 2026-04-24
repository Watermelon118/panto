export function DashboardPage() {
  return (
    <main className="min-h-screen bg-stone-950 px-6 py-10 text-stone-100">
      <div className="mx-auto max-w-6xl space-y-8">
        <header className="flex flex-col gap-4 rounded-[2rem] border border-white/10 bg-white/5 p-8 shadow-[0_30px_80px_rgba(0,0,0,0.28)] backdrop-blur sm:flex-row sm:items-end sm:justify-between">
          <div className="space-y-3">
            <p className="text-sm font-semibold tracking-[0.18em] text-amber-300 uppercase">
              Panto Dashboard
            </p>
            <h1 className="text-4xl font-black tracking-tight text-balance sm:text-5xl">
              Warehouse operations start here.
            </h1>
            <p className="max-w-2xl text-sm leading-7 text-stone-300 sm:text-base">
              This is a temporary landing page for the frontend auth milestone.
              Inventory, inbound, orders, and reporting modules will connect
              here in the next phase.
            </p>
          </div>

          <div className="rounded-3xl border border-amber-300/20 bg-amber-300/10 px-5 py-4 text-sm leading-6 text-amber-100">
            <p className="font-semibold">Milestone status</p>
            <p className="mt-1">Frontend shell is being assembled around auth.</p>
          </div>
        </header>

        <section className="grid gap-5 lg:grid-cols-3">
          <article className="rounded-[1.75rem] border border-white/10 bg-white/5 p-6">
            <p className="text-sm font-semibold tracking-[0.14em] text-stone-400 uppercase">
              Inventory
            </p>
            <h2 className="mt-3 text-2xl font-bold text-white">Batch visibility</h2>
            <p className="mt-3 text-sm leading-7 text-stone-300">
              Upcoming work will surface stock by SKU, batch, expiry status, and
              low-stock warnings from this space.
            </p>
          </article>

          <article className="rounded-[1.75rem] border border-white/10 bg-white/5 p-6">
            <p className="text-sm font-semibold tracking-[0.14em] text-stone-400 uppercase">
              Orders
            </p>
            <h2 className="mt-3 text-2xl font-bold text-white">Sales flow</h2>
            <p className="mt-3 text-sm leading-7 text-stone-300">
              Order entry, FIFO deduction, invoice generation, and rollback
              actions will plug into this dashboard.
            </p>
          </article>

          <article className="rounded-[1.75rem] border border-white/10 bg-white/5 p-6">
            <p className="text-sm font-semibold tracking-[0.14em] text-stone-400 uppercase">
              Audit
            </p>
            <h2 className="mt-3 text-2xl font-bold text-white">Traceable changes</h2>
            <p className="mt-3 text-sm leading-7 text-stone-300">
              User actions, inventory movements, and operational exceptions will
              eventually be visible from a shared control surface.
            </p>
          </article>
        </section>
      </div>
    </main>
  );
}
