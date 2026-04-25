import { useMemo, useState } from 'react';
import { useTransactions } from '../api/inventory';
import { useProducts } from '../api/products';
import { Pagination } from '../components/Pagination';
import type { TransactionType } from '../types/inventory';

const selectCls =
  'rounded-xl border border-white/10 bg-stone-900 px-4 py-2.5 text-sm text-stone-100 outline-none transition focus:border-amber-300/50';

const badgeCls =
  'inline-flex items-center rounded-full border border-white/10 bg-white/5 px-3 py-1 text-xs font-semibold tracking-[0.14em] text-stone-300 uppercase';

const TRANSACTION_TYPE_LABELS: Record<TransactionType, string> = {
  IN: 'Inbound',
  OUT: 'Outbound',
  ROLLBACK: 'Rollback',
  DESTROY: 'Destroy',
  ADJUST: 'Adjust',
};

const TRANSACTION_TYPE_CLS: Record<TransactionType, string> = {
  IN: 'bg-emerald-900/40 text-emerald-400',
  OUT: 'bg-blue-900/40 text-blue-400',
  ROLLBACK: 'bg-amber-900/40 text-amber-300',
  DESTROY: 'bg-red-900/40 text-red-400',
  ADJUST: 'bg-stone-700/60 text-stone-300',
};

function formatDateTime(value: string) {
  return new Intl.DateTimeFormat('en-NZ', {
    dateStyle: 'medium',
    timeStyle: 'short',
  }).format(new Date(value));
}

function formatDocumentLabel(type: string | null, id: number | null) {
  if (!type || id === null) {
    return 'Manual adjustment';
  }

  return `${type} #${id}`;
}

export function TransactionListPage() {
  const [page, setPage] = useState(0);
  const [productId, setProductId] = useState<number | undefined>(undefined);
  const [transactionType, setTransactionType] = useState<TransactionType | undefined>(undefined);

  const { data, isLoading, isFetching } = useTransactions({
    productId,
    transactionType,
    page,
    size: 20,
  });

  const { data: productsData } = useProducts({ page: 0, size: 100, active: true });

  const activeFilterCount = useMemo(() => {
    let count = 0;

    if (productId !== undefined) {
      count += 1;
    }

    if (transactionType !== undefined) {
      count += 1;
    }

    return count;
  }, [productId, transactionType]);

  return (
    <div className="mx-auto max-w-7xl space-y-6">
      <header className="rounded-[2rem] border border-white/10 bg-white/5 p-8 shadow-[0_30px_80px_rgba(0,0,0,0.28)] backdrop-blur">
        <div className="flex flex-col gap-4 lg:flex-row lg:items-end lg:justify-between">
          <div className="space-y-3">
            <p className="text-xs font-semibold tracking-[0.18em] text-amber-300 uppercase">
              Inventory
            </p>
            <div>
              <h1 className="text-3xl font-black tracking-tight sm:text-4xl">Transaction Trail</h1>
              <p className="mt-3 max-w-2xl text-sm leading-7 text-stone-300">
                Review every inbound, outbound, rollback, destroy, and manual adjustment event
                across warehouse batches.
              </p>
            </div>
          </div>

          <div className="flex flex-wrap gap-2">
            <span className={badgeCls}>{data?.totalElements ?? 0} records</span>
            <span className={badgeCls}>{activeFilterCount} active filters</span>
            {isFetching && !isLoading ? (
              <span className="inline-flex items-center rounded-full bg-amber-300/10 px-3 py-1 text-xs font-semibold text-amber-200">
                Refreshing
              </span>
            ) : null}
          </div>
        </div>
      </header>

      <section className="rounded-[1.75rem] border border-white/10 bg-white/5 p-5">
        <div className="flex flex-col gap-4 lg:flex-row lg:items-center">
          <div className="grid flex-1 gap-3 md:grid-cols-2">
            <select
              value={productId ?? ''}
              onChange={(event) => {
                setProductId(event.target.value ? Number(event.target.value) : undefined);
                setPage(0);
              }}
              className={selectCls}
            >
              <option value="">All products</option>
              {productsData?.items.map((product) => (
                <option key={product.id} value={product.id}>
                  {product.sku} - {product.name}
                </option>
              ))}
            </select>

            <select
              value={transactionType ?? ''}
              onChange={(event) => {
                setTransactionType((event.target.value as TransactionType) || undefined);
                setPage(0);
              }}
              className={selectCls}
            >
              <option value="">All transaction types</option>
              {(Object.keys(TRANSACTION_TYPE_LABELS) as TransactionType[]).map((type) => (
                <option key={type} value={type}>
                  {TRANSACTION_TYPE_LABELS[type]}
                </option>
              ))}
            </select>
          </div>

          <button
            type="button"
            onClick={() => {
              setProductId(undefined);
              setTransactionType(undefined);
              setPage(0);
            }}
            disabled={activeFilterCount === 0}
            className="rounded-xl border border-white/10 px-4 py-2.5 text-sm text-stone-400 transition hover:bg-white/5 hover:text-stone-100 disabled:cursor-not-allowed disabled:opacity-50"
          >
            Clear filters
          </button>
        </div>
      </section>

      <section className="overflow-hidden rounded-[1.75rem] border border-white/10 bg-white/5">
        {isLoading ? (
          <div className="p-12 text-center text-sm text-stone-500">Loading transactions...</div>
        ) : data?.items.length === 0 ? (
          <div className="p-12 text-center text-sm text-stone-500">
            No inventory transactions matched the current filters.
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="min-w-full text-sm">
              <thead>
                <tr className="border-b border-white/10 text-left text-xs font-semibold tracking-[0.14em] text-stone-400 uppercase">
                  <th className="px-6 py-4">Time</th>
                  <th className="px-6 py-4">Type</th>
                  <th className="px-6 py-4">Batch</th>
                  <th className="px-6 py-4">Product</th>
                  <th className="px-6 py-4 text-right">Change</th>
                  <th className="px-6 py-4 text-right">Before</th>
                  <th className="px-6 py-4 text-right">After</th>
                  <th className="px-6 py-4">Document</th>
                  <th className="px-6 py-4">Note</th>
                </tr>
              </thead>
              <tbody>
                {data?.items.map((transaction) => (
                  <tr key={transaction.id} className="border-b border-white/5 align-top last:border-b-0">
                    <td className="px-6 py-4 text-xs text-stone-400">
                      {formatDateTime(transaction.createdAt)}
                    </td>
                    <td className="px-6 py-4">
                      <span
                        className={`rounded-full px-2.5 py-0.5 text-xs font-semibold ${TRANSACTION_TYPE_CLS[transaction.transactionType]}`}
                      >
                        {TRANSACTION_TYPE_LABELS[transaction.transactionType]}
                      </span>
                    </td>
                    <td className="px-6 py-4 font-mono text-xs text-amber-300">
                      {transaction.batchNumber}
                    </td>
                    <td className="px-6 py-4 text-stone-200">
                      <div className="font-medium">{transaction.productName}</div>
                      <div className="mt-1 text-xs text-stone-500">{transaction.productSku}</div>
                    </td>
                    <td className="px-6 py-4 text-right font-semibold">
                      <span
                        className={
                          transaction.quantityDelta >= 0 ? 'text-emerald-400' : 'text-red-400'
                        }
                      >
                        {transaction.quantityDelta >= 0 ? '+' : ''}
                        {transaction.quantityDelta}
                      </span>
                    </td>
                    <td className="px-6 py-4 text-right text-stone-400">
                      {transaction.quantityBefore}
                    </td>
                    <td className="px-6 py-4 text-right text-stone-200">
                      {transaction.quantityAfter}
                    </td>
                    <td className="px-6 py-4 text-xs text-stone-400">
                      {formatDocumentLabel(
                        transaction.relatedDocumentType,
                        transaction.relatedDocumentId,
                      )}
                    </td>
                    <td className="px-6 py-4 text-xs leading-6 text-stone-400">
                      {transaction.note?.trim() || 'No note'}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </section>

      {data && data.totalPages > 1 ? (
        <Pagination
          page={data.page}
          totalPages={data.totalPages}
          totalElements={data.totalElements}
          size={data.size}
          onPageChange={setPage}
        />
      ) : null}
    </div>
  );
}
