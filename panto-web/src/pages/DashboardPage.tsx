import { Link } from 'react-router-dom';
import { useDashboardSummary } from '../api/dashboard';
import type { DashboardSummary, DashboardWarnings } from '../types/dashboard';
import { extractErrorMessage } from '../utils/error';

const ROLE_COPY: Record<
  DashboardSummary['role'],
  { heading: string; summary: string; accent: string }
> = {
  ADMIN: {
    heading: 'Operations overview for managers',
    summary:
      'Track sales momentum, warning pressure, and the products moving fastest this month.',
    accent: 'ADMIN VIEW',
  },
  MARKETING: {
    heading: 'Sales and demand signals in one place',
    summary:
      'Monitor commercial performance alongside inventory pressure so campaigns stay grounded in stock reality.',
    accent: 'MARKETING VIEW',
  },
  WAREHOUSE: {
    heading: 'Warehouse pulse for the current shift',
    summary:
      'Watch today’s inbound and outbound flow while keeping an eye on batches that may need destruction handling.',
    accent: 'WAREHOUSE VIEW',
  },
  ACCOUNTANT: {
    heading: 'Financial summary with loss visibility',
    summary:
      'Compare this month’s sales against recorded destruction loss to keep accounting decisions current.',
    accent: 'ACCOUNTANT VIEW',
  },
};

function formatCurrency(value: number) {
  return new Intl.NumberFormat('en-NZ', {
    style: 'currency',
    currency: 'NZD',
    minimumFractionDigits: 2,
  }).format(value);
}

function StatCard({
  label,
  value,
  tone,
  helper,
  to,
}: {
  label: string;
  value: string | number;
  tone: 'red' | 'amber' | 'stone' | 'emerald';
  helper?: string;
  to?: string;
}) {
  const toneCls = {
    red: 'border-red-500/20 bg-red-950/10 text-red-300',
    amber: 'border-amber-400/20 bg-amber-950/10 text-amber-300',
    stone: 'border-white/10 bg-white/5 text-stone-200',
    emerald: 'border-emerald-400/20 bg-emerald-950/10 text-emerald-300',
  }[tone];
  const cardCls = `rounded-[1.5rem] border p-5 ${toneCls}`;
  const content = (
    <>
      <p className="text-xs font-semibold tracking-[0.14em] uppercase opacity-80">{label}</p>
      <p className="mt-3 text-3xl font-black tracking-tight text-white">{value}</p>
      {helper ? <p className="mt-3 text-sm leading-6 text-stone-400">{helper}</p> : null}
    </>
  );

  if (to) {
    return (
      <Link
        to={to}
        aria-label={`Open ${label}`}
        className={`${cardCls} block transition hover:border-white/25 hover:bg-white/[0.03] focus:ring-2 focus:ring-amber-300/40 focus:outline-none`}
      >
        {content}
      </Link>
    );
  }

  return <article className={cardCls}>{content}</article>;
}

function WarningSection({ warnings }: { warnings: DashboardWarnings }) {
  return (
    <section className="space-y-4">
      <div className="flex items-center justify-between">
        <div>
          <p className="text-xs font-semibold tracking-[0.14em] text-stone-400 uppercase">
            Warning Summary
          </p>
          <h2 className="mt-2 text-2xl font-bold text-white">Inventory pressure points</h2>
        </div>
      </div>

      <div className="grid gap-4 md:grid-cols-3">
        <StatCard
          label="Low Stock"
          value={warnings.lowStockCount}
          tone="red"
          helper="Active products below their configured safety stock threshold."
          to="/inventory/stock?lowStock=true"
        />
        <StatCard
          label="Expiring Soon"
          value={warnings.expiringSoonCount}
          tone="amber"
          helper="Batches inside the current expiry warning window."
          to="/inventory/batches?status=EXPIRING_SOON"
        />
        <StatCard
          label="Expired"
          value={warnings.expiredCount}
          tone="stone"
          helper="Expired batches that still have remaining stock on hand."
        />
      </div>
    </section>
  );
}

function ManagerPanel({ summary }: { summary: NonNullable<DashboardSummary['managerSummary']> }) {
  return (
    <section className="grid gap-6 xl:grid-cols-[0.95fr_1.05fr]">
      <div className="space-y-4">
        <div className="grid gap-4 md:grid-cols-2">
          <StatCard
            label="Today Sales"
            value={formatCurrency(summary.todaySalesTotal)}
            tone="emerald"
          />
          <StatCard
            label="Month Sales"
            value={formatCurrency(summary.monthSalesTotal)}
            tone="stone"
          />
        </div>
        <StatCard
          label="Pending Tasks"
          value={summary.pendingTaskCount}
          tone="amber"
          helper="Calculated from current low-stock, expiring-soon, and expired warning counts."
        />
      </div>

      <article className="rounded-[1.75rem] border border-white/10 bg-white/5 p-6">
        <div className="flex items-center justify-between">
          <div>
            <p className="text-xs font-semibold tracking-[0.14em] text-stone-400 uppercase">
              Monthly Ranking
            </p>
            <h2 className="mt-2 text-2xl font-bold text-white">Top products</h2>
          </div>
          <div className="rounded-2xl bg-amber-300/10 px-4 py-3 text-right">
            <p className="text-xs font-semibold tracking-[0.14em] text-amber-200 uppercase">
              Products
            </p>
            <p className="mt-1 text-2xl font-black text-amber-300">{summary.topProducts.length}</p>
          </div>
        </div>

        <div className="mt-5 space-y-3">
          {summary.topProducts.length === 0 ? (
            <div className="rounded-2xl border border-dashed border-white/10 bg-stone-950/40 px-4 py-6 text-sm text-stone-500">
              No active sales have been recorded this month yet.
            </div>
          ) : (
            summary.topProducts.map((product, index) => (
              <div
                key={product.productId}
                className="rounded-2xl border border-white/10 bg-stone-950/40 px-4 py-4"
              >
                <div className="flex items-start justify-between gap-4">
                  <div>
                    <p className="font-semibold text-stone-100">{product.productName}</p>
                    <p className="mt-1 text-xs text-stone-500">
                      #{index + 1} | {product.productSku}
                    </p>
                  </div>
                  <div className="text-right">
                    <p className="text-xs font-semibold tracking-[0.14em] text-stone-500 uppercase">
                      Quantity
                    </p>
                    <p className="mt-1 text-lg font-bold text-white">{product.quantitySold}</p>
                  </div>
                </div>
                <div className="mt-4 rounded-xl bg-white/5 px-3 py-2">
                  <p className="text-xs uppercase tracking-[0.14em] text-stone-500">
                    Sales Amount
                  </p>
                  <p className="mt-1 font-semibold text-white">{formatCurrency(product.salesAmount)}</p>
                </div>
              </div>
            ))
          )}
        </div>
      </article>
    </section>
  );
}

function WarehousePanel({
  summary,
}: {
  summary: NonNullable<DashboardSummary['warehouseSummary']>;
}) {
  return (
    <section className="grid gap-4 md:grid-cols-3">
      <StatCard
        label="Inbound Today"
        value={summary.todayInboundCount}
        tone="emerald"
        helper="Inbound records dated today."
      />
      <StatCard
        label="Outbound Today"
        value={summary.todayOutboundCount}
        tone="stone"
        helper="Active orders created today."
      />
      <StatCard
        label="Pending Destruction"
        value={summary.pendingDestructionCount}
        tone="amber"
        helper="Expired batches that still need warehouse write-off action."
      />
    </section>
  );
}

function AccountantPanel({
  summary,
}: {
  summary: NonNullable<DashboardSummary['accountantSummary']>;
}) {
  const netAfterLoss = summary.monthSalesTotal - summary.monthLossTotal;

  return (
    <section className="grid gap-4 md:grid-cols-3">
      <StatCard
        label="Month Sales"
        value={formatCurrency(summary.monthSalesTotal)}
        tone="emerald"
        helper="Current-month total across active orders."
      />
      <StatCard
        label="Month Loss"
        value={formatCurrency(summary.monthLossTotal)}
        tone="red"
        helper="Current-month destruction loss captured from stock write-offs."
      />
      <StatCard
        label="Net After Loss"
        value={formatCurrency(netAfterLoss)}
        tone="stone"
        helper="Sales total less recorded destruction loss for the month."
      />
    </section>
  );
}

export function DashboardPage() {
  const summaryQuery = useDashboardSummary();
  const summary = summaryQuery.data;

  if (summaryQuery.isLoading) {
    return <div className="p-12 text-center text-sm text-stone-500">Loading dashboard...</div>;
  }

  if (!summary) {
    return (
      <div className="mx-auto max-w-4xl rounded-[1.75rem] border border-red-500/20 bg-red-950/10 p-8 text-sm leading-7 text-red-200">
        {extractErrorMessage(summaryQuery.error)}
      </div>
    );
  }

  const roleCopy = ROLE_COPY[summary.role];

  return (
    <div className="mx-auto max-w-7xl space-y-8">
      <header className="flex flex-col gap-4 rounded-[2rem] border border-white/10 bg-white/5 p-8 shadow-[0_30px_80px_rgba(0,0,0,0.28)] backdrop-blur sm:flex-row sm:items-end sm:justify-between">
        <div className="space-y-3">
          <p className="text-sm font-semibold tracking-[0.18em] text-amber-300 uppercase">
            Panto Dashboard
          </p>
          <h1 className="text-4xl font-black tracking-tight text-balance sm:text-5xl">
            {roleCopy.heading}
          </h1>
          <p className="max-w-2xl text-sm leading-7 text-stone-300 sm:text-base">
            {roleCopy.summary}
          </p>
        </div>

        <div className="rounded-3xl border border-amber-300/20 bg-amber-300/10 px-5 py-4 text-sm leading-6 text-amber-100">
          <p className="font-semibold">{roleCopy.accent}</p>
          <p className="mt-1">Live data is tailored to the signed-in role.</p>
        </div>
      </header>

      <WarningSection warnings={summary.warnings} />

      {summary.managerSummary ? <ManagerPanel summary={summary.managerSummary} /> : null}
      {summary.warehouseSummary ? <WarehousePanel summary={summary.warehouseSummary} /> : null}
      {summary.accountantSummary ? <AccountantPanel summary={summary.accountantSummary} /> : null}
    </div>
  );
}
