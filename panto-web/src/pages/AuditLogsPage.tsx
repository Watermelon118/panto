import { useMemo, useState } from 'react';
import { useAuditLogs } from '../api/audit';
import { useUsers } from '../api/users';
import { Pagination } from '../components/Pagination';
import type { AuditAction, AuditLogEntry } from '../types/audit';
import { extractErrorMessage } from '../utils/error';

const inputCls =
  'w-full rounded-xl border border-white/10 bg-white/5 px-4 py-2.5 text-sm text-stone-100 outline-none transition focus:border-amber-300/50 focus:ring-2 focus:ring-amber-300/20 placeholder:text-stone-500';

const selectCls =
  'rounded-xl border border-white/10 bg-stone-900 px-4 py-2.5 text-sm text-stone-100 outline-none focus:border-amber-300/50';

const AUDIT_ACTIONS: AuditAction[] = ['CREATE', 'UPDATE', 'DELETE', 'ROLLBACK', 'LOGIN', 'LOGIN_FAIL'];

function formatDateTime(value: string) {
  return new Intl.DateTimeFormat('en-NZ', {
    dateStyle: 'medium',
    timeStyle: 'short',
  }).format(new Date(value));
}

function formatJson(value: unknown) {
  return JSON.stringify(value ?? null, null, 2);
}

function SnapshotCard({
  label,
  value,
}: {
  label: string;
  value: unknown;
}) {
  return (
    <div className="rounded-2xl border border-white/10 bg-stone-950/50 p-4">
      <p className="text-xs font-semibold tracking-[0.14em] text-stone-400 uppercase">
        {label}
      </p>
      <pre className="mt-3 overflow-x-auto whitespace-pre-wrap break-all text-xs leading-6 text-stone-300">
        {formatJson(value)}
      </pre>
    </div>
  );
}

function AuditLogRow({ log }: { log: AuditLogEntry }) {
  const [expanded, setExpanded] = useState(false);

  return (
    <>
      <tr className="border-b border-white/5 transition hover:bg-white/[0.03]">
        <td className="px-6 py-4 text-xs text-stone-400">{formatDateTime(log.createdAt)}</td>
        <td className="px-6 py-4 text-sm text-stone-200">
          {log.operatorUsername ?? 'System'}
          {log.operatorRole ? (
            <span className="ml-2 text-xs text-stone-500">{log.operatorRole}</span>
          ) : null}
        </td>
        <td className="px-6 py-4">
          <span className="rounded-full bg-amber-300/15 px-2.5 py-1 text-xs font-medium text-amber-300">
            {log.action}
          </span>
        </td>
        <td className="px-6 py-4 text-sm text-stone-300">{log.entityType}</td>
        <td className="px-6 py-4 text-sm text-stone-400">{log.entityId ?? '—'}</td>
        <td className="px-6 py-4 text-sm text-stone-300">{log.description ?? '—'}</td>
        <td className="px-6 py-4 text-right">
          <button
            type="button"
            onClick={() => setExpanded((current) => !current)}
            className="rounded-lg px-3 py-1.5 text-xs font-medium text-stone-400 transition hover:bg-white/10 hover:text-stone-100"
          >
            {expanded ? 'Hide' : 'Inspect'}
          </button>
        </td>
      </tr>
      {expanded ? (
        <tr className="border-b border-white/5 bg-stone-950/30">
          <td colSpan={7} className="px-6 py-5">
            <div className="grid gap-4 lg:grid-cols-2">
              <SnapshotCard label="Before" value={log.beforeValue} />
              <SnapshotCard label="After" value={log.afterValue} />
            </div>
            <div className="mt-4 flex flex-wrap gap-4 text-xs text-stone-500">
              <span>Log ID: {log.id}</span>
              <span>Operator ID: {log.operatorId ?? '—'}</span>
              <span>IP: {log.ipAddress ?? '—'}</span>
            </div>
          </td>
        </tr>
      ) : null}
    </>
  );
}

export function AuditLogsPage() {
  const [page, setPage] = useState(0);
  const [operatorId, setOperatorId] = useState<number | undefined>(undefined);
  const [entityType, setEntityType] = useState('');
  const [action, setAction] = useState<AuditAction | undefined>(undefined);
  const [dateFrom, setDateFrom] = useState('');
  const [dateTo, setDateTo] = useState('');

  const params = useMemo(
    () => ({
      operatorId,
      entityType: entityType || undefined,
      action,
      dateFrom: dateFrom || undefined,
      dateTo: dateTo || undefined,
      page,
      size: 20,
    }),
    [action, dateFrom, dateTo, entityType, operatorId, page],
  );

  const auditLogsQuery = useAuditLogs(params);
  const usersQuery = useUsers({ page: 0, size: 100 });

  return (
    <div className="mx-auto max-w-7xl space-y-6">
      <div>
        <p className="text-xs font-semibold tracking-[0.18em] text-amber-300 uppercase">
          Admin
        </p>
        <h1 className="mt-1 text-3xl font-black tracking-tight">Audit Logs</h1>
        <p className="mt-3 max-w-3xl text-sm leading-7 text-stone-300">
          Review who changed data, when the change happened, and the before/after snapshot recorded
          for that operation.
        </p>
      </div>

      <div className="grid gap-3 lg:grid-cols-5">
        <select
          value={operatorId ?? ''}
          onChange={(event) => {
            setOperatorId(event.target.value ? Number(event.target.value) : undefined);
            setPage(0);
          }}
          className={selectCls}
        >
          <option value="">All operators</option>
          {usersQuery.data?.items.map((user) => (
            <option key={user.id} value={user.id}>
              {user.username} ({user.role})
            </option>
          ))}
        </select>

        <input
          value={entityType}
          onChange={(event) => {
            setEntityType(event.target.value);
            setPage(0);
          }}
          className={inputCls}
          placeholder="Entity type, e.g. ORDER"
        />

        <select
          value={action ?? ''}
          onChange={(event) => {
            setAction(event.target.value ? (event.target.value as AuditAction) : undefined);
            setPage(0);
          }}
          className={selectCls}
        >
          <option value="">All actions</option>
          {AUDIT_ACTIONS.map((item) => (
            <option key={item} value={item}>
              {item}
            </option>
          ))}
        </select>

        <input
          type="date"
          value={dateFrom}
          onChange={(event) => {
            setDateFrom(event.target.value);
            setPage(0);
          }}
          className={inputCls}
        />

        <input
          type="date"
          value={dateTo}
          onChange={(event) => {
            setDateTo(event.target.value);
            setPage(0);
          }}
          className={inputCls}
        />
      </div>

      <div className="overflow-hidden rounded-[1.75rem] border border-white/10 bg-white/5">
        {auditLogsQuery.isLoading ? (
          <div className="p-12 text-center text-sm text-stone-500">Loading audit logs...</div>
        ) : auditLogsQuery.error ? (
          <div className="p-12 text-center text-sm text-red-300">
            {extractErrorMessage(auditLogsQuery.error)}
          </div>
        ) : auditLogsQuery.data?.items.length === 0 ? (
          <div className="p-12 text-center text-sm text-stone-500">No audit logs found.</div>
        ) : (
          <table className="w-full text-sm">
            <thead>
              <tr className="border-b border-white/10 text-left text-xs font-semibold tracking-wider text-stone-400 uppercase">
                <th className="px-6 py-4">Time</th>
                <th className="px-6 py-4">Operator</th>
                <th className="px-6 py-4">Action</th>
                <th className="px-6 py-4">Entity</th>
                <th className="px-6 py-4">Entity ID</th>
                <th className="px-6 py-4">Description</th>
                <th className="px-6 py-4" />
              </tr>
            </thead>
            <tbody>
              {auditLogsQuery.data?.items.map((log) => <AuditLogRow key={log.id} log={log} />)}
            </tbody>
          </table>
        )}
      </div>

      {auditLogsQuery.data && auditLogsQuery.data.totalPages > 1 ? (
        <Pagination
          page={auditLogsQuery.data.page}
          totalPages={auditLogsQuery.data.totalPages}
          totalElements={auditLogsQuery.data.totalElements}
          size={auditLogsQuery.data.size}
          onPageChange={setPage}
        />
      ) : null}
    </div>
  );
}
