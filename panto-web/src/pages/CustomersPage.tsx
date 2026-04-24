import { useState } from 'react';
import {
  useCreateCustomer,
  useCustomers,
  useUpdateCustomer,
  useUpdateCustomerStatus,
} from '../api/customers';
import { Modal } from '../components/Modal';
import { Pagination } from '../components/Pagination';
import type { CreateCustomerRequest, CustomerSummary } from '../types/customer';
import { extractErrorMessage } from '../utils/error';

const inputCls =
  'w-full rounded-xl border border-white/10 bg-white/5 px-4 py-2.5 text-sm text-stone-100 outline-none transition focus:border-amber-300/50 focus:ring-2 focus:ring-amber-300/20 placeholder:text-stone-500';

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

const EMPTY_FORM: CreateCustomerRequest = {
  companyName: '',
  contactPerson: '',
  phone: '',
  email: '',
  address: '',
  gstNumber: '',
  remarks: '',
};

export function CustomersPage() {
  const [page, setPage] = useState(0);
  const [keyword, setKeyword] = useState('');
  const [activeFilter, setActiveFilter] = useState<boolean | undefined>(undefined);

  const [modalMode, setModalMode] = useState<'create' | 'edit' | null>(null);
  const [editingId, setEditingId] = useState<number | null>(null);
  const [form, setForm] = useState<CreateCustomerRequest>(EMPTY_FORM);
  const [formError, setFormError] = useState<string | null>(null);

  const { data, isLoading } = useCustomers({
    keyword: keyword || undefined,
    active: activeFilter,
    page,
    size: 20,
  });
  const createCustomer = useCreateCustomer();
  const updateCustomer = useUpdateCustomer();
  const toggleStatus = useUpdateCustomerStatus();

  const setField = <K extends keyof CreateCustomerRequest>(key: K, value: CreateCustomerRequest[K]) =>
    setForm((prev) => ({ ...prev, [key]: value }));

  const openCreate = () => {
    setForm(EMPTY_FORM);
    setFormError(null);
    setModalMode('create');
  };

  const openEdit = (customer: CustomerSummary) => {
    setEditingId(customer.id);
    setForm({
      companyName: customer.companyName,
      contactPerson: customer.contactPerson ?? '',
      phone: customer.phone ?? '',
      email: customer.email ?? '',
      address: '',
      gstNumber: '',
      remarks: '',
    });
    setFormError(null);
    setModalMode('edit');
  };

  const handleSubmit = async () => {
    setFormError(null);
    try {
      if (modalMode === 'create') {
        await createCustomer.mutateAsync(form);
      } else if (editingId !== null) {
        await updateCustomer.mutateAsync({ id: editingId, request: form });
      }
      setModalMode(null);
    } catch (error) {
      setFormError(extractErrorMessage(error));
    }
  };

  const isSaving = createCustomer.isPending || updateCustomer.isPending;

  return (
    <div className="mx-auto max-w-6xl space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <p className="text-xs font-semibold tracking-[0.18em] text-amber-300 uppercase">
            Master Data
          </p>
          <h1 className="mt-1 text-3xl font-black tracking-tight">Customers</h1>
        </div>
        <button
          type="button"
          onClick={openCreate}
          className="rounded-xl bg-amber-300 px-5 py-2.5 text-sm font-semibold text-stone-900 transition hover:bg-amber-200"
        >
          + New Customer
        </button>
      </div>

      <div className="flex gap-3">
        <input
          type="text"
          placeholder="Search company name or phone…"
          value={keyword}
          onChange={(e) => { setKeyword(e.target.value); setPage(0); }}
          className={inputCls + ' max-w-xs'}
        />
        <select
          value={activeFilter === undefined ? '' : String(activeFilter)}
          onChange={(e) => {
            setActiveFilter(e.target.value === '' ? undefined : e.target.value === 'true');
            setPage(0);
          }}
          className="rounded-xl border border-white/10 bg-stone-900 px-4 py-2.5 text-sm text-stone-100 outline-none focus:border-amber-300/50"
        >
          <option value="">All status</option>
          <option value="true">Active</option>
          <option value="false">Inactive</option>
        </select>
      </div>

      <div className="overflow-hidden rounded-[1.75rem] border border-white/10 bg-white/5">
        {isLoading ? (
          <div className="p-12 text-center text-sm text-stone-500">Loading…</div>
        ) : data?.items.length === 0 ? (
          <div className="p-12 text-center text-sm text-stone-500">No customers found.</div>
        ) : (
          <table className="w-full text-sm">
            <thead>
              <tr className="border-b border-white/10 text-left text-xs font-semibold tracking-wider text-stone-400 uppercase">
                <th className="px-6 py-4">Company</th>
                <th className="px-6 py-4">Contact</th>
                <th className="px-6 py-4">Phone</th>
                <th className="px-6 py-4">Email</th>
                <th className="px-6 py-4">Status</th>
                <th className="px-6 py-4" />
              </tr>
            </thead>
            <tbody>
              {data?.items.map((customer) => (
                <tr key={customer.id} className="border-b border-white/5 transition hover:bg-white/[0.03]">
                  <td className="px-6 py-4 font-medium text-stone-100">{customer.companyName}</td>
                  <td className="px-6 py-4 text-stone-400">{customer.contactPerson ?? '—'}</td>
                  <td className="px-6 py-4 text-stone-400">{customer.phone ?? '—'}</td>
                  <td className="px-6 py-4 text-stone-400">{customer.email ?? '—'}</td>
                  <td className="px-6 py-4">
                    <button
                      type="button"
                      onClick={() => toggleStatus.mutate({ id: customer.id, active: !customer.active })}
                      className={`rounded-full px-3 py-1 text-xs font-semibold transition ${
                        customer.active
                          ? 'bg-emerald-400/15 text-emerald-400 hover:bg-emerald-400/25'
                          : 'bg-stone-700 text-stone-400 hover:bg-stone-600'
                      }`}
                    >
                      {customer.active ? 'Active' : 'Inactive'}
                    </button>
                  </td>
                  <td className="px-6 py-4 text-right">
                    <button
                      type="button"
                      onClick={() => openEdit(customer)}
                      className="rounded-lg px-3 py-1.5 text-xs font-medium text-stone-400 transition hover:bg-white/10 hover:text-stone-100"
                    >
                      Edit
                    </button>
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

      {modalMode && (
        <Modal
          title={modalMode === 'create' ? 'New Customer' : 'Edit Customer'}
          onClose={() => setModalMode(null)}
        >
          <div className="space-y-4">
            <Field label="Company Name" required>
              <input
                value={form.companyName}
                onChange={(e) => setField('companyName', e.target.value)}
                className={inputCls}
                placeholder="Panto Trading Ltd"
              />
            </Field>
            <div className="grid grid-cols-2 gap-4">
              <Field label="Contact Person">
                <input
                  value={form.contactPerson ?? ''}
                  onChange={(e) => setField('contactPerson', e.target.value)}
                  className={inputCls}
                  placeholder="Alex Chen"
                />
              </Field>
              <Field label="Phone">
                <input
                  value={form.phone ?? ''}
                  onChange={(e) => setField('phone', e.target.value)}
                  className={inputCls}
                  placeholder="021 888 999"
                />
              </Field>
            </div>
            <Field label="Email">
              <input
                type="email"
                value={form.email ?? ''}
                onChange={(e) => setField('email', e.target.value)}
                className={inputCls}
                placeholder="alex@panto.co.nz"
              />
            </Field>
            <Field label="Address">
              <input
                value={form.address ?? ''}
                onChange={(e) => setField('address', e.target.value)}
                className={inputCls}
                placeholder="99 Queen Street, Auckland"
              />
            </Field>
            <div className="grid grid-cols-2 gap-4">
              <Field label="GST Number">
                <input
                  value={form.gstNumber ?? ''}
                  onChange={(e) => setField('gstNumber', e.target.value)}
                  className={inputCls}
                  placeholder="GST-7788"
                />
              </Field>
              <Field label="Remarks">
                <input
                  value={form.remarks ?? ''}
                  onChange={(e) => setField('remarks', e.target.value)}
                  className={inputCls}
                  placeholder="Morning delivery preferred"
                />
              </Field>
            </div>
            {formError && (
              <p className="rounded-xl bg-red-900/30 px-4 py-3 text-sm text-red-400">
                {formError}
              </p>
            )}
            <div className="mt-2 space-y-2">
              <button
                type="button"
                onClick={() => void handleSubmit()}
                disabled={isSaving}
                className="w-full rounded-xl border border-amber-300 bg-amber-300 px-5 py-2.5 text-sm font-semibold text-stone-900 transition hover:bg-amber-200 disabled:opacity-50"
              >
                {isSaving ? 'Saving…' : 'Save'}
              </button>
              <button
                type="button"
                onClick={() => setModalMode(null)}
                className="w-full rounded-xl border border-white/10 px-5 py-2.5 text-sm font-medium text-stone-400 transition hover:bg-white/10"
              >
                Cancel
              </button>
            </div>
          </div>
        </Modal>
      )}
    </div>
  );
}
