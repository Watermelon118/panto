import { useMemo, useState } from 'react';
import {
  type ExportReportRequest,
  type ReportFormat,
  useExportLossReport,
  useExportSalesReport,
} from '../api/reports';
import { useAuthStore } from '../store/auth-store';
import { extractErrorMessage } from '../utils/error';

const inputCls =
  'w-full rounded-xl border border-white/10 bg-white/5 px-4 py-2.5 text-sm text-stone-100 outline-none transition focus:border-amber-300/50 focus:ring-2 focus:ring-amber-300/20 placeholder:text-stone-500';

function formatDateInput(date: Date) {
  return date.toISOString().slice(0, 10);
}

function getCurrentMonthRange() {
  const now = new Date();
  const monthStart = new Date(now.getFullYear(), now.getMonth(), 1);
  return {
    from: formatDateInput(monthStart),
    to: formatDateInput(now),
  };
}

export function ReportsPage() {
  const user = useAuthStore((state) => state.user);
  const salesExport = useExportSalesReport();
  const lossExport = useExportLossReport();

  const defaultRange = useMemo(() => getCurrentMonthRange(), []);
  const [from, setFrom] = useState(defaultRange.from);
  const [to, setTo] = useState(defaultRange.to);
  const [format, setFormat] = useState<ReportFormat>('xlsx');
  const [error, setError] = useState<string | null>(null);

  const isAllowed = user?.role === 'ADMIN' || user?.role === 'ACCOUNTANT';
  const isBusy = salesExport.isPending || lossExport.isPending;

  const buildRequest = (): ExportReportRequest | null => {
    if (!from || !to) {
      setError('Please select both a start date and an end date.');
      return null;
    }

    if (from > to) {
      setError('The start date must be on or before the end date.');
      return null;
    }

    setError(null);
    return { from, to, format };
  };

  const handleSalesExport = async () => {
    const request = buildRequest();
    if (!request) {
      return;
    }

    try {
      await salesExport.mutateAsync(request);
    } catch (exportError) {
      setError(extractErrorMessage(exportError));
    }
  };

  const handleLossExport = async () => {
    const request = buildRequest();
    if (!request) {
      return;
    }

    try {
      await lossExport.mutateAsync(request);
    } catch (exportError) {
      setError(extractErrorMessage(exportError));
    }
  };

  if (!isAllowed) {
    return (
      <div className="mx-auto max-w-4xl rounded-[1.75rem] border border-amber-300/20 bg-amber-300/10 p-8 text-sm leading-7 text-amber-100">
        Reports export is available to ADMIN and ACCOUNTANT roles only.
      </div>
    );
  }

  return (
    <div className="mx-auto max-w-6xl space-y-8">
      <div className="flex flex-wrap items-end justify-between gap-4">
        <div>
          <p className="text-xs font-semibold tracking-[0.18em] text-amber-300 uppercase">
            Reporting
          </p>
          <h1 className="mt-1 text-3xl font-black tracking-tight">Exports</h1>
          <p className="mt-3 max-w-2xl text-sm leading-7 text-stone-300">
            Export sales activity and destruction loss records for a selected date range in CSV or
            Excel format.
          </p>
        </div>

        <div className="rounded-3xl border border-white/10 bg-white/5 px-5 py-4 text-sm leading-6 text-stone-300">
          <p className="font-semibold text-stone-100">Role access</p>
          <p className="mt-1">ADMIN and ACCOUNTANT can generate finance-ready export files.</p>
        </div>
      </div>

      <section className="grid gap-6 lg:grid-cols-[minmax(0,1.15fr)_minmax(320px,0.85fr)]">
        <div className="space-y-6">
          <div className="rounded-[1.75rem] border border-white/10 bg-white/5 p-6">
            <div className="grid gap-4 md:grid-cols-2">
              <label className="block space-y-1.5">
                <span className="text-xs font-medium text-stone-400">From</span>
                <input
                  type="date"
                  value={from}
                  onChange={(event) => {
                    setFrom(event.target.value);
                    setError(null);
                  }}
                  className={inputCls}
                />
              </label>

              <label className="block space-y-1.5">
                <span className="text-xs font-medium text-stone-400">To</span>
                <input
                  type="date"
                  value={to}
                  onChange={(event) => {
                    setTo(event.target.value);
                    setError(null);
                  }}
                  className={inputCls}
                />
              </label>
            </div>

            <div className="mt-5">
              <p className="text-xs font-medium text-stone-400">Format</p>
              <div className="mt-3 inline-flex rounded-2xl border border-white/10 bg-stone-900/80 p-1">
                {(['xlsx', 'csv'] as ReportFormat[]).map((option) => (
                  <button
                    key={option}
                    type="button"
                    onClick={() => setFormat(option)}
                    className={`rounded-xl px-4 py-2 text-sm font-medium transition ${
                      format === option
                        ? 'bg-amber-300 text-stone-900'
                        : 'text-stone-400 hover:text-stone-100'
                    }`}
                  >
                    {option.toUpperCase()}
                  </button>
                ))}
              </div>
            </div>

            {error && (
              <p className="mt-5 rounded-xl bg-red-900/30 px-4 py-3 text-sm text-red-400">
                {error}
              </p>
            )}
          </div>

          <div className="grid gap-4 md:grid-cols-2">
            <button
              type="button"
              onClick={() => void handleSalesExport()}
              disabled={isBusy}
              className="rounded-[1.75rem] border border-emerald-400/20 bg-emerald-950/10 p-6 text-left transition hover:border-emerald-300/30 disabled:opacity-50"
            >
              <p className="text-xs font-semibold tracking-[0.14em] text-emerald-300 uppercase">
                Sales Export
              </p>
              <h2 className="mt-3 text-2xl font-bold text-white">Active order lines</h2>
              <p className="mt-3 text-sm leading-6 text-stone-300">
                Includes order number, customer, quantity, unit price, GST, line total, and operator.
              </p>
              <p className="mt-5 text-sm font-semibold text-emerald-300">
                {salesExport.isPending ? 'Generating sales file...' : 'Download sales report'}
              </p>
            </button>

            <button
              type="button"
              onClick={() => void handleLossExport()}
              disabled={isBusy}
              className="rounded-[1.75rem] border border-red-400/20 bg-red-950/10 p-6 text-left transition hover:border-red-300/30 disabled:opacity-50"
            >
              <p className="text-xs font-semibold tracking-[0.14em] text-red-300 uppercase">
                Loss Export
              </p>
              <h2 className="mt-3 text-2xl font-bold text-white">Destruction write-offs</h2>
              <p className="mt-3 text-sm leading-6 text-stone-300">
                Includes destruction number, batch, quantity, unit cost snapshot, loss amount, and reason.
              </p>
              <p className="mt-5 text-sm font-semibold text-red-300">
                {lossExport.isPending ? 'Generating loss file...' : 'Download loss report'}
              </p>
            </button>
          </div>
        </div>

        <aside className="space-y-6">
          <div className="rounded-[1.75rem] border border-white/10 bg-stone-900/80 p-6">
            <p className="text-xs font-semibold tracking-[0.14em] text-stone-400 uppercase">
              Current Selection
            </p>
            <div className="mt-5 space-y-3 text-sm">
              <div className="flex items-center justify-between text-stone-300">
                <span>From</span>
                <span className="text-stone-100">{from || '-'}</span>
              </div>
              <div className="flex items-center justify-between text-stone-300">
                <span>To</span>
                <span className="text-stone-100">{to || '-'}</span>
              </div>
              <div className="flex items-center justify-between border-t border-white/10 pt-3 text-base font-semibold text-white">
                <span>Format</span>
                <span>{format.toUpperCase()}</span>
              </div>
            </div>
          </div>

          <div className="rounded-[1.75rem] border border-white/10 bg-white/5 p-6">
            <p className="text-xs font-semibold tracking-[0.14em] text-stone-400 uppercase">
              Export Notes
            </p>
            <div className="mt-5 space-y-3 text-sm leading-6 text-stone-300">
              <p>Sales export only includes active orders inside the selected date range.</p>
              <p>Loss export uses the persisted destruction cost snapshot for accounting consistency.</p>
              <p>Empty datasets still download a valid file with header rows for downstream processing.</p>
            </div>
          </div>
        </aside>
      </section>
    </div>
  );
}
