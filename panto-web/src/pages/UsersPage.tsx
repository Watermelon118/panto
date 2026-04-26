import { useState } from 'react';
import {
  useCreateUser,
  useResetUserPassword,
  useUpdateUser,
  useUpdateUserStatus,
  useUsers,
} from '../api/users';
import { Modal } from '../components/Modal';
import { Pagination } from '../components/Pagination';
import type { UserRole } from '../types/auth';
import type { CreateUserRequest, UpdateUserRequest, UserSummary } from '../types/user';
import { extractErrorMessage } from '../utils/error';

const inputCls =
  'w-full rounded-xl border border-white/10 bg-white/5 px-4 py-2.5 text-sm text-stone-100 outline-none transition focus:border-amber-300/50 focus:ring-2 focus:ring-amber-300/20 placeholder:text-stone-500';

const selectCls =
  'w-full rounded-xl border border-white/10 bg-stone-800 px-4 py-2.5 text-sm text-stone-100 outline-none transition focus:border-amber-300/50';

function Field({ label, required, children }: { label: string; required?: boolean; children: React.ReactNode }) {
  return (
    <label className="block space-y-1.5">
      <span className="text-xs font-medium text-stone-400">
        {label}
        {required && <span className="ml-0.5 text-amber-300">*</span>}
      </span>
      {children}
    </label>
  );
}

const ROLES: UserRole[] = ['ADMIN', 'WAREHOUSE', 'MARKETING', 'ACCOUNTANT'];

const EMPTY_CREATE: CreateUserRequest = {
  username: '',
  password: '',
  fullName: '',
  email: '',
  role: 'WAREHOUSE',
};

const EMPTY_EDIT: UpdateUserRequest = {
  fullName: '',
  email: '',
  role: 'WAREHOUSE',
};

export function UsersPage() {
  const [page, setPage] = useState(0);

  const [modalMode, setModalMode] = useState<'create' | 'edit' | null>(null);
  const [editingId, setEditingId] = useState<number | null>(null);
  const [createForm, setCreateForm] = useState<CreateUserRequest>(EMPTY_CREATE);
  const [editForm, setEditForm] = useState<UpdateUserRequest>(EMPTY_EDIT);
  const [formError, setFormError] = useState<string | null>(null);

  const [resetUserId, setResetUserId] = useState<number | null>(null);
  const [resetUsername, setResetUsername] = useState('');
  const [newPassword, setNewPassword] = useState('');
  const [resetError, setResetError] = useState<string | null>(null);

  const [statusTarget, setStatusTarget] = useState<UserSummary | null>(null);
  const [statusError, setStatusError] = useState<string | null>(null);

  const { data, isLoading } = useUsers({ page, size: 20 });
  const createUser = useCreateUser();
  const updateUser = useUpdateUser();
  const toggleStatus = useUpdateUserStatus();
  const resetPassword = useResetUserPassword();

  const openCreate = () => {
    setCreateForm(EMPTY_CREATE);
    setFormError(null);
    setModalMode('create');
  };

  const openEdit = (user: UserSummary) => {
    setEditingId(user.id);
    setEditForm({ fullName: user.fullName, email: user.email ?? '', role: user.role });
    setFormError(null);
    setModalMode('edit');
  };

  const openResetPassword = (user: UserSummary) => {
    setResetUserId(user.id);
    setResetUsername(user.username);
    setNewPassword('');
    setResetError(null);
  };

  const handleSubmit = async () => {
    setFormError(null);
    try {
      if (modalMode === 'create') {
        await createUser.mutateAsync(createForm);
      } else if (editingId !== null) {
        await updateUser.mutateAsync({ id: editingId, request: editForm });
      }
      setModalMode(null);
    } catch (error) {
      setFormError(extractErrorMessage(error));
    }
  };

  const handleResetPassword = async () => {
    if (!resetUserId) return;
    setResetError(null);
    try {
      await resetPassword.mutateAsync({ id: resetUserId, request: { newPassword } });
      setResetUserId(null);
    } catch (error) {
      setResetError(extractErrorMessage(error));
    }
  };

  const handleConfirmStatusToggle = async () => {
    if (!statusTarget) return;
    setStatusError(null);
    try {
      await toggleStatus.mutateAsync({ id: statusTarget.id, active: !statusTarget.active });
      setStatusTarget(null);
    } catch (error) {
      setStatusError(extractErrorMessage(error));
    }
  };

  const isSaving = createUser.isPending || updateUser.isPending;

  return (
    <div className="mx-auto max-w-6xl space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <p className="text-xs font-semibold tracking-[0.18em] text-amber-300 uppercase">
            Admin
          </p>
          <h1 className="mt-1 text-3xl font-black tracking-tight">Users</h1>
        </div>
        <button
          type="button"
          onClick={openCreate}
          className="rounded-xl bg-amber-300 px-5 py-2.5 text-sm font-semibold text-stone-900 transition hover:bg-amber-200"
        >
          + New User
        </button>
      </div>

      <div className="overflow-hidden rounded-[1.75rem] border border-white/10 bg-white/5">
        {isLoading ? (
          <div className="p-12 text-center text-sm text-stone-500">Loading…</div>
        ) : data?.items.length === 0 ? (
          <div className="p-12 text-center text-sm text-stone-500">No users found.</div>
        ) : (
          <table className="w-full text-sm">
            <thead>
              <tr className="border-b border-white/10 text-left text-xs font-semibold tracking-wider text-stone-400 uppercase">
                <th className="px-6 py-4">Username</th>
                <th className="px-6 py-4">Full Name</th>
                <th className="px-6 py-4">Role</th>
                <th className="px-6 py-4">Flags</th>
                <th className="px-6 py-4">Status</th>
                <th className="px-6 py-4" />
              </tr>
            </thead>
            <tbody>
              {data?.items.map((user) => (
                <tr key={user.id} className="border-b border-white/5 transition hover:bg-white/[0.03]">
                  <td className="px-6 py-4 font-mono text-xs text-amber-300">{user.username}</td>
                  <td className="px-6 py-4 font-medium text-stone-100">{user.fullName}</td>
                  <td className="px-6 py-4">
                    <span className="rounded-full border border-white/10 bg-white/5 px-2.5 py-1 text-xs font-medium text-stone-300">
                      {user.role}
                    </span>
                  </td>
                  <td className="px-6 py-4">
                    {user.mustChangePassword && (
                      <span className="rounded-full bg-amber-300/15 px-2.5 py-1 text-xs font-medium text-amber-300">
                        Must reset
                      </span>
                    )}
                  </td>
                  <td className="px-6 py-4">
                    <button
                      type="button"
                      onClick={() => {
                        setStatusError(null);
                        setStatusTarget(user);
                      }}
                      className={`rounded-full px-3 py-1 text-xs font-semibold transition ${
                        user.active
                          ? 'bg-emerald-400/15 text-emerald-400 hover:bg-emerald-400/25'
                          : 'bg-stone-700 text-stone-400 hover:bg-stone-600'
                      }`}
                    >
                      {user.active ? 'Active' : 'Inactive'}
                    </button>
                  </td>
                  <td className="px-6 py-4">
                    <div className="flex justify-end gap-2">
                      <button
                        type="button"
                        onClick={() => openEdit(user)}
                        className="rounded-lg px-3 py-1.5 text-xs font-medium text-stone-400 transition hover:bg-white/10 hover:text-stone-100"
                      >
                        Edit
                      </button>
                      <button
                        type="button"
                        onClick={() => openResetPassword(user)}
                        className="rounded-lg px-3 py-1.5 text-xs font-medium text-stone-400 transition hover:bg-white/10 hover:text-stone-100"
                      >
                        Reset pw
                      </button>
                    </div>
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

      {/* Create modal */}
      {modalMode === 'create' && (
        <Modal title="New User" onClose={() => setModalMode(null)}>
          <div className="space-y-4">
            <Field label="Username" required>
              <input
                value={createForm.username}
                onChange={(e) => setCreateForm((f) => ({ ...f, username: e.target.value }))}
                className={inputCls}
                placeholder="john.doe"
                autoComplete="off"
              />
            </Field>
            <Field label="Password" required>
              <input
                type="password"
                value={createForm.password}
                onChange={(e) => setCreateForm((f) => ({ ...f, password: e.target.value }))}
                className={inputCls}
                placeholder="Min 8 chars, letters + numbers"
                autoComplete="new-password"
              />
            </Field>
            <Field label="Full Name" required>
              <input
                value={createForm.fullName}
                onChange={(e) => setCreateForm((f) => ({ ...f, fullName: e.target.value }))}
                className={inputCls}
                placeholder="John Doe"
              />
            </Field>
            <Field label="Email">
              <input
                type="email"
                value={createForm.email ?? ''}
                onChange={(e) => setCreateForm((f) => ({ ...f, email: e.target.value }))}
                className={inputCls}
                placeholder="john@panto.co.nz"
              />
            </Field>
            <Field label="Role" required>
              <select
                value={createForm.role}
                onChange={(e) => setCreateForm((f) => ({ ...f, role: e.target.value as UserRole }))}
                className={selectCls}
              >
                {ROLES.map((r) => <option key={r} value={r}>{r}</option>)}
              </select>
            </Field>
            {formError && (
              <p className="rounded-xl bg-red-900/30 px-4 py-3 text-sm text-red-400">{formError}</p>
            )}
            <div className="mt-2 space-y-2">
              <button type="button" onClick={() => void handleSubmit()} disabled={isSaving} className="w-full rounded-xl border border-amber-300 bg-amber-300 px-5 py-2.5 text-sm font-semibold text-stone-900 transition hover:bg-amber-200 disabled:opacity-50">
                {isSaving ? 'Saving…' : 'Create User'}
              </button>
              <button type="button" onClick={() => setModalMode(null)} className="w-full rounded-xl border border-white/10 px-5 py-2.5 text-sm font-medium text-stone-400 transition hover:bg-white/10">
                Cancel
              </button>
            </div>
          </div>
        </Modal>
      )}

      {/* Edit modal */}
      {modalMode === 'edit' && (
        <Modal title="Edit User" onClose={() => setModalMode(null)}>
          <div className="space-y-4">
            <Field label="Full Name" required>
              <input
                value={editForm.fullName}
                onChange={(e) => setEditForm((f) => ({ ...f, fullName: e.target.value }))}
                className={inputCls}
                placeholder="John Doe"
              />
            </Field>
            <Field label="Email">
              <input
                type="email"
                value={editForm.email ?? ''}
                onChange={(e) => setEditForm((f) => ({ ...f, email: e.target.value }))}
                className={inputCls}
                placeholder="john@panto.co.nz"
              />
            </Field>
            <Field label="Role" required>
              <select
                value={editForm.role}
                onChange={(e) => setEditForm((f) => ({ ...f, role: e.target.value as UserRole }))}
                className={selectCls}
              >
                {ROLES.map((r) => <option key={r} value={r}>{r}</option>)}
              </select>
            </Field>
            {formError && (
              <p className="rounded-xl bg-red-900/30 px-4 py-3 text-sm text-red-400">{formError}</p>
            )}
            <div className="grid grid-cols-2 gap-3 pt-2">
              <button type="button" onClick={() => setModalMode(null)} className="rounded-xl border border-white/10 px-5 py-2.5 text-sm font-medium text-stone-400 transition hover:bg-white/10">
                Cancel
              </button>
              <button type="button" onClick={() => void handleSubmit()} disabled={isSaving} className="rounded-xl border border-amber-300 bg-amber-300 px-5 py-2.5 text-sm font-semibold text-stone-900 transition hover:bg-amber-200 disabled:opacity-50">
                {isSaving ? 'Saving…' : 'Save'}
              </button>
            </div>
          </div>
        </Modal>
      )}

      {/* Status toggle confirmation modal */}
      {statusTarget && (
        <Modal
          title={statusTarget.active ? 'Disable User' : 'Enable User'}
          onClose={() => setStatusTarget(null)}
        >
          <div className="space-y-4">
            <p className="text-sm text-stone-300">
              {statusTarget.active ? 'Disable' : 'Enable'} account{' '}
              <span className="font-mono text-amber-300">{statusTarget.username}</span>
              {statusTarget.active
                ? '? They will no longer be able to log in.'
                : '? They will be able to log in again.'}
            </p>
            {statusError && (
              <p className="rounded-xl bg-red-900/30 px-4 py-3 text-sm text-red-400">{statusError}</p>
            )}
            <div className="grid grid-cols-2 gap-3 pt-2">
              <button
                type="button"
                onClick={() => setStatusTarget(null)}
                className="rounded-xl border border-white/10 px-5 py-2.5 text-sm font-medium text-stone-400 transition hover:bg-white/10"
              >
                Cancel
              </button>
              <button
                type="button"
                onClick={() => void handleConfirmStatusToggle()}
                disabled={toggleStatus.isPending}
                className={`rounded-xl px-5 py-2.5 text-sm font-semibold transition disabled:opacity-50 ${
                  statusTarget.active
                    ? 'bg-red-500/90 text-white hover:bg-red-500'
                    : 'bg-amber-300 text-stone-900 hover:bg-amber-200'
                }`}
              >
                {toggleStatus.isPending
                  ? 'Saving…'
                  : statusTarget.active
                    ? 'Disable'
                    : 'Enable'}
              </button>
            </div>
          </div>
        </Modal>
      )}

      {/* Reset password modal */}
      {resetUserId !== null && (
        <Modal title="Reset Password" onClose={() => setResetUserId(null)}>
          <div className="space-y-4">
            <p className="text-sm text-stone-400">
              Reset password for <span className="font-mono text-amber-300">{resetUsername}</span>.
              The user will be required to change it on next login.
            </p>
            <Field label="New Password" required>
              <input
                type="password"
                value={newPassword}
                onChange={(e) => setNewPassword(e.target.value)}
                className={inputCls}
                placeholder="Min 8 chars, letters + numbers"
                autoComplete="new-password"
              />
            </Field>
            {resetError && (
              <p className="rounded-xl bg-red-900/30 px-4 py-3 text-sm text-red-400">{resetError}</p>
            )}
            <div className="mt-2 space-y-2">
              <button type="button" onClick={() => void handleResetPassword()} disabled={resetPassword.isPending} className="w-full rounded-xl border border-amber-300 bg-amber-300 px-5 py-2.5 text-sm font-semibold text-stone-900 transition hover:bg-amber-200 disabled:opacity-50">
                {resetPassword.isPending ? 'Resetting…' : 'Reset Password'}
              </button>
              <button type="button" onClick={() => setResetUserId(null)} className="w-full rounded-xl border border-white/10 px-5 py-2.5 text-sm font-medium text-stone-400 transition hover:bg-white/10">
                Cancel
              </button>
            </div>
          </div>
        </Modal>
      )}
    </div>
  );
}
