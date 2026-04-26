import { useEffect, useState } from 'react';
import { useSettings, useUpdateSettings } from '../api/settings';
import { useAuthStore } from '../store/auth-store';
import { extractErrorMessage } from '../utils/error';

const inputCls =
  'w-full rounded-xl border border-white/10 bg-white/5 px-4 py-2.5 text-sm text-stone-100 outline-none transition focus:border-amber-300/50 focus:ring-2 focus:ring-amber-300/20 placeholder:text-stone-500';

export function SettingsPage() {
  const user = useAuthStore((state) => state.user);
  const settingsQuery = useSettings();
  const updateSettings = useUpdateSettings();

  const [expiryWarningDays, setExpiryWarningDays] = useState('');
  const [invoiceSellerCompanyName, setInvoiceSellerCompanyName] = useState('');
  const [invoiceSellerGstNumber, setInvoiceSellerGstNumber] = useState('');
  const [invoiceSellerAddress, setInvoiceSellerAddress] = useState('');
  const [invoiceSellerPhone, setInvoiceSellerPhone] = useState('');
  const [invoiceSellerEmail, setInvoiceSellerEmail] = useState('');
  const [invoicePaymentInstructions, setInvoicePaymentInstructions] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [successMessage, setSuccessMessage] = useState<string | null>(null);

  useEffect(() => {
    if (settingsQuery.data) {
      setExpiryWarningDays(String(settingsQuery.data.expiryWarningDays));
      setInvoiceSellerCompanyName(settingsQuery.data.invoiceSellerCompanyName);
      setInvoiceSellerGstNumber(settingsQuery.data.invoiceSellerGstNumber);
      setInvoiceSellerAddress(settingsQuery.data.invoiceSellerAddress);
      setInvoiceSellerPhone(settingsQuery.data.invoiceSellerPhone);
      setInvoiceSellerEmail(settingsQuery.data.invoiceSellerEmail);
      setInvoicePaymentInstructions(settingsQuery.data.invoicePaymentInstructions);
    }
  }, [settingsQuery.data]);

  const isAllowed = user?.role === 'ADMIN';

  const handleSubmit = async () => {
    const parsedValue = Number(expiryWarningDays);

    if (!Number.isInteger(parsedValue) || parsedValue < 0 || parsedValue > 3650) {
      setError('Expiry warning days must be an integer between 0 and 3650.');
      setSuccessMessage(null);
      return;
    }

    setError(null);
    setSuccessMessage(null);

    try {
      const updated = await updateSettings.mutateAsync({
        expiryWarningDays: parsedValue,
        invoiceSellerCompanyName,
        invoiceSellerGstNumber,
        invoiceSellerAddress,
        invoiceSellerPhone,
        invoiceSellerEmail,
        invoicePaymentInstructions,
      });
      setExpiryWarningDays(String(updated.expiryWarningDays));
      setInvoiceSellerCompanyName(updated.invoiceSellerCompanyName);
      setInvoiceSellerGstNumber(updated.invoiceSellerGstNumber);
      setInvoiceSellerAddress(updated.invoiceSellerAddress);
      setInvoiceSellerPhone(updated.invoiceSellerPhone);
      setInvoiceSellerEmail(updated.invoiceSellerEmail);
      setInvoicePaymentInstructions(updated.invoicePaymentInstructions);
      setSuccessMessage('Settings saved successfully.');
    } catch (updateError) {
      setError(extractErrorMessage(updateError));
    }
  };

  if (!isAllowed) {
    return (
      <div className="mx-auto max-w-4xl rounded-[1.75rem] border border-amber-300/20 bg-amber-300/10 p-8 text-sm leading-7 text-amber-100">
        System settings are available to ADMIN users only.
      </div>
    );
  }

  return (
    <div className="mx-auto max-w-5xl space-y-8">
      <div className="flex flex-wrap items-end justify-between gap-4">
        <div>
          <p className="text-xs font-semibold tracking-[0.18em] text-amber-300 uppercase">
            System
          </p>
          <h1 className="mt-1 text-3xl font-black tracking-tight">Settings</h1>
          <p className="mt-3 max-w-2xl text-sm leading-7 text-stone-300">
            Configure warehouse warning thresholds and invoice seller details used in preview and PDF downloads.
          </p>
        </div>

        <div className="rounded-3xl border border-white/10 bg-white/5 px-5 py-4 text-sm leading-6 text-stone-300">
          <p className="font-semibold text-stone-100">Scope</p>
          <p className="mt-1">Changes take effect immediately for new dashboard and inventory requests.</p>
        </div>
      </div>

      <section className="grid gap-6 lg:grid-cols-[minmax(0,1.1fr)_minmax(320px,0.9fr)]">
        <div className="space-y-6">
          <div className="rounded-[1.75rem] border border-white/10 bg-white/5 p-6">
            {settingsQuery.isLoading ? (
              <div className="text-sm text-stone-500">Loading settings...</div>
            ) : (
              <>
                <label className="block space-y-1.5">
                  <span className="text-xs font-medium text-stone-400">
                    Expiry warning days
                  </span>
                  <input
                    type="number"
                    min="0"
                    max="3650"
                    value={expiryWarningDays}
                    onChange={(event) => {
                      setExpiryWarningDays(event.target.value);
                      setError(null);
                      setSuccessMessage(null);
                    }}
                    className={inputCls}
                    placeholder="Enter warning threshold"
                  />
                </label>

                <p className="mt-3 text-sm leading-6 text-stone-400">
                  Batches inside this threshold are treated as <span className="text-amber-300">expiring soon</span>.
                </p>

                <div className="mt-8 border-t border-white/10 pt-6">
                  <p className="text-sm font-semibold text-stone-100">Invoice Seller Profile</p>
                  <p className="mt-2 text-sm leading-6 text-stone-400">
                    These values appear in invoice preview cards and generated PDF copies.
                  </p>

                  <div className="mt-5 grid gap-4 md:grid-cols-2">
                    <label className="block space-y-1.5">
                      <span className="text-xs font-medium text-stone-400">Seller company name</span>
                      <input
                        value={invoiceSellerCompanyName}
                        onChange={(event) => {
                          setInvoiceSellerCompanyName(event.target.value);
                          setError(null);
                          setSuccessMessage(null);
                        }}
                        className={inputCls}
                        placeholder="Panto Trading Ltd"
                      />
                    </label>

                    <label className="block space-y-1.5">
                      <span className="text-xs font-medium text-stone-400">Seller GST number</span>
                      <input
                        value={invoiceSellerGstNumber}
                        onChange={(event) => {
                          setInvoiceSellerGstNumber(event.target.value);
                          setError(null);
                          setSuccessMessage(null);
                        }}
                        className={inputCls}
                        placeholder="GST-9988"
                      />
                    </label>

                    <label className="block space-y-1.5">
                      <span className="text-xs font-medium text-stone-400">Seller phone</span>
                      <input
                        value={invoiceSellerPhone}
                        onChange={(event) => {
                          setInvoiceSellerPhone(event.target.value);
                          setError(null);
                          setSuccessMessage(null);
                        }}
                        className={inputCls}
                        placeholder="09 123 4567"
                      />
                    </label>

                    <label className="block space-y-1.5">
                      <span className="text-xs font-medium text-stone-400">Seller email</span>
                      <input
                        type="email"
                        value={invoiceSellerEmail}
                        onChange={(event) => {
                          setInvoiceSellerEmail(event.target.value);
                          setError(null);
                          setSuccessMessage(null);
                        }}
                        className={inputCls}
                        placeholder="accounts@panto.co.nz"
                      />
                    </label>

                    <label className="block space-y-1.5 md:col-span-2">
                      <span className="text-xs font-medium text-stone-400">Seller address</span>
                      <input
                        value={invoiceSellerAddress}
                        onChange={(event) => {
                          setInvoiceSellerAddress(event.target.value);
                          setError(null);
                          setSuccessMessage(null);
                        }}
                        className={inputCls}
                        placeholder="1 Queen Street, Auckland"
                      />
                    </label>

                    <label className="block space-y-1.5 md:col-span-2">
                      <span className="text-xs font-medium text-stone-400">Payment instructions</span>
                      <input
                        value={invoicePaymentInstructions}
                        onChange={(event) => {
                          setInvoicePaymentInstructions(event.target.value);
                          setError(null);
                          setSuccessMessage(null);
                        }}
                        className={inputCls}
                        placeholder="Bank transfer"
                      />
                    </label>
                  </div>
                </div>

                {settingsQuery.error && (
                  <p className="mt-5 rounded-xl bg-red-900/30 px-4 py-3 text-sm text-red-400">
                    {extractErrorMessage(settingsQuery.error)}
                  </p>
                )}

                {error && (
                  <p className="mt-5 rounded-xl bg-red-900/30 px-4 py-3 text-sm text-red-400">
                    {error}
                  </p>
                )}

                {successMessage && (
                  <p className="mt-5 rounded-xl bg-emerald-950/30 px-4 py-3 text-sm text-emerald-300">
                    {successMessage}
                  </p>
                )}

                <div className="mt-6">
                  <button
                    type="button"
                    onClick={() => void handleSubmit()}
                    disabled={settingsQuery.isLoading || updateSettings.isPending}
                    className="rounded-xl bg-amber-300 px-5 py-2.5 text-sm font-semibold text-stone-900 transition hover:bg-amber-200 disabled:opacity-50"
                  >
                    {updateSettings.isPending ? 'Saving...' : 'Save settings'}
                  </button>
                </div>
              </>
            )}
          </div>
        </div>

        <aside className="space-y-6">
          <div className="rounded-[1.75rem] border border-white/10 bg-stone-900/80 p-6">
            <p className="text-xs font-semibold tracking-[0.14em] text-stone-400 uppercase">
              Current Value
            </p>
            <p className="mt-4 text-3xl font-black text-white">
              {settingsQuery.data?.expiryWarningDays ?? '--'}
            </p>
            <p className="mt-2 text-sm leading-6 text-stone-400">
              Days before expiry when a batch should start appearing in warning views.
            </p>
          </div>

          <div className="rounded-[1.75rem] border border-white/10 bg-white/5 p-6">
            <p className="text-xs font-semibold tracking-[0.14em] text-stone-400 uppercase">
              Guidance
            </p>
            <div className="mt-5 space-y-3 text-sm leading-6 text-stone-300">
              <p>Set to 0 if you only want already-expired batches to trigger warnings.</p>
              <p>Use a larger number for product lines with longer outbound planning cycles.</p>
              <p>The backend validates values in the inclusive range 0 to 3650.</p>
              <p>Invoice seller settings can be left blank temporarily, but invoices will show “Not configured”.</p>
            </div>
          </div>
        </aside>
      </section>
    </div>
  );
}
