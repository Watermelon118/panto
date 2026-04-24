import { useState } from 'react';
import {
  useCategories,
  useCreateProduct,
  useProducts,
  useUpdateProduct,
  useUpdateProductStatus,
} from '../api/products';
import { Modal } from '../components/Modal';
import { Pagination } from '../components/Pagination';
import type { CreateProductRequest, ProductSummary } from '../types/product';
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

const EMPTY_FORM: CreateProductRequest = {
  sku: '',
  name: '',
  category: '',
  specification: '',
  unit: '',
  referencePurchasePrice: 0,
  referenceSalePrice: 0,
  safetyStock: 0,
  gstApplicable: false,
};

export function ProductsPage() {
  const [page, setPage] = useState(0);
  const [keyword, setKeyword] = useState('');
  const [activeFilter, setActiveFilter] = useState<boolean | undefined>(undefined);

  const [modalMode, setModalMode] = useState<'create' | 'edit' | null>(null);
  const [editingId, setEditingId] = useState<number | null>(null);
  const [form, setForm] = useState<CreateProductRequest>(EMPTY_FORM);
  const [formError, setFormError] = useState<string | null>(null);

  const { data, isLoading } = useProducts({
    keyword: keyword || undefined,
    active: activeFilter,
    page,
    size: 20,
  });
  const { data: categories } = useCategories();
  const createProduct = useCreateProduct();
  const updateProduct = useUpdateProduct();
  const toggleStatus = useUpdateProductStatus();

  const setField = <K extends keyof CreateProductRequest>(key: K, value: CreateProductRequest[K]) =>
    setForm((prev) => ({ ...prev, [key]: value }));

  const openCreate = () => {
    setForm(EMPTY_FORM);
    setFormError(null);
    setModalMode('create');
  };

  const openEdit = (product: ProductSummary) => {
    setEditingId(product.id);
    setForm({
      sku: product.sku,
      name: product.name,
      category: product.category,
      specification: '',
      unit: product.unit,
      referencePurchasePrice: product.referencePurchasePrice,
      referenceSalePrice: product.referenceSalePrice,
      safetyStock: product.safetyStock,
      gstApplicable: product.gstApplicable,
    });
    setFormError(null);
    setModalMode('edit');
  };

  const handleSubmit = async () => {
    setFormError(null);
    try {
      if (modalMode === 'create') {
        await createProduct.mutateAsync(form);
      } else if (editingId !== null) {
        await updateProduct.mutateAsync({ id: editingId, request: form });
      }
      setModalMode(null);
    } catch (error) {
      setFormError(extractErrorMessage(error));
    }
  };

  const isSaving = createProduct.isPending || updateProduct.isPending;

  return (
    <div className="mx-auto max-w-6xl space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <p className="text-xs font-semibold tracking-[0.18em] text-amber-300 uppercase">
            Master Data
          </p>
          <h1 className="mt-1 text-3xl font-black tracking-tight">Products</h1>
        </div>
        <button
          type="button"
          onClick={openCreate}
          className="rounded-xl bg-amber-300 px-5 py-2.5 text-sm font-semibold text-stone-900 transition hover:bg-amber-200"
        >
          + New Product
        </button>
      </div>

      <div className="flex gap-3">
        <input
          type="text"
          placeholder="Search SKU or name…"
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
          <div className="p-12 text-center text-sm text-stone-500">No products found.</div>
        ) : (
          <table className="w-full text-sm">
            <thead>
              <tr className="border-b border-white/10 text-left text-xs font-semibold tracking-wider text-stone-400 uppercase">
                <th className="px-6 py-4">SKU</th>
                <th className="px-6 py-4">Name</th>
                <th className="px-6 py-4">Category</th>
                <th className="px-6 py-4">Unit</th>
                <th className="px-6 py-4 text-right">Sale Price</th>
                <th className="px-6 py-4 text-right">Stock</th>
                <th className="px-6 py-4">Status</th>
                <th className="px-6 py-4" />
              </tr>
            </thead>
            <tbody>
              {data?.items.map((product) => (
                <tr key={product.id} className="border-b border-white/5 transition hover:bg-white/[0.03]">
                  <td className="px-6 py-4 font-mono text-xs text-amber-300">{product.sku}</td>
                  <td className="px-6 py-4 font-medium text-stone-100">{product.name}</td>
                  <td className="px-6 py-4 text-stone-400">{product.category}</td>
                  <td className="px-6 py-4 text-stone-400">{product.unit}</td>
                  <td className="px-6 py-4 text-right text-stone-300">
                    ${product.referenceSalePrice.toFixed(2)}
                  </td>
                  <td className="px-6 py-4 text-right text-stone-300">{product.currentStock}</td>
                  <td className="px-6 py-4">
                    <button
                      type="button"
                      onClick={() => toggleStatus.mutate({ id: product.id, active: !product.active })}
                      className={`rounded-full px-3 py-1 text-xs font-semibold transition ${
                        product.active
                          ? 'bg-emerald-400/15 text-emerald-400 hover:bg-emerald-400/25'
                          : 'bg-stone-700 text-stone-400 hover:bg-stone-600'
                      }`}
                    >
                      {product.active ? 'Active' : 'Inactive'}
                    </button>
                  </td>
                  <td className="px-6 py-4 text-right">
                    <button
                      type="button"
                      onClick={() => openEdit(product)}
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
          title={modalMode === 'create' ? 'New Product' : 'Edit Product'}
          onClose={() => setModalMode(null)}
        >
          <div className="space-y-4">
            <Field label="SKU" required>
              <input
                value={form.sku}
                onChange={(e) => setField('sku', e.target.value)}
                disabled={modalMode === 'edit'}
                className={inputCls}
                placeholder="SKU-001"
              />
            </Field>
            <Field label="Name" required>
              <input
                value={form.name}
                onChange={(e) => setField('name', e.target.value)}
                className={inputCls}
                placeholder="Frozen Dumplings"
              />
            </Field>
            <div className="grid grid-cols-2 gap-4">
              <Field label="Category" required>
                <input
                  value={form.category}
                  onChange={(e) => setField('category', e.target.value)}
                  className={inputCls}
                  list="category-options"
                  placeholder="Frozen Food"
                />
                <datalist id="category-options">
                  {categories?.map((c) => <option key={c} value={c} />)}
                </datalist>
              </Field>
              <Field label="Unit" required>
                <input
                  value={form.unit}
                  onChange={(e) => setField('unit', e.target.value)}
                  className={inputCls}
                  placeholder="carton"
                />
              </Field>
            </div>
            <Field label="Specification">
              <input
                value={form.specification ?? ''}
                onChange={(e) => setField('specification', e.target.value)}
                className={inputCls}
                placeholder="1kg × 10"
              />
            </Field>
            <div className="grid grid-cols-3 gap-4">
              <Field label="Purchase Price" required>
                <input
                  type="number"
                  min="0"
                  step="0.01"
                  value={form.referencePurchasePrice}
                  onChange={(e) => setField('referencePurchasePrice', Number(e.target.value))}
                  className={inputCls}
                />
              </Field>
              <Field label="Sale Price" required>
                <input
                  type="number"
                  min="0"
                  step="0.01"
                  value={form.referenceSalePrice}
                  onChange={(e) => setField('referenceSalePrice', Number(e.target.value))}
                  className={inputCls}
                />
              </Field>
              <Field label="Safety Stock" required>
                <input
                  type="number"
                  min="0"
                  value={form.safetyStock}
                  onChange={(e) => setField('safetyStock', Number(e.target.value))}
                  className={inputCls}
                />
              </Field>
            </div>
            <label className="flex cursor-pointer items-center gap-3 text-sm text-stone-300">
              <input
                type="checkbox"
                checked={form.gstApplicable}
                onChange={(e) => setField('gstApplicable', e.target.checked)}
                className="h-4 w-4 rounded accent-amber-300"
              />
              GST Applicable
            </label>
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
