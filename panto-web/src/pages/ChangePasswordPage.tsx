import { type FormEvent, useState } from 'react';
import { Navigate } from 'react-router-dom';
import { useAuthStore } from '../store/auth-store';
import { extractErrorMessage } from '../utils/error';

const inputCls =
  'w-full rounded-2xl border border-white/10 bg-white/5 px-4 py-3 text-sm text-stone-100 outline-none transition focus:border-amber-300/50 focus:ring-4 focus:ring-amber-300/20 placeholder:text-stone-500';

export function ChangePasswordPage() {
  const user = useAuthStore((state) => state.user);
  const changePassword = useAuthStore((state) => state.changePassword);
  const logout = useAuthStore((state) => state.logout);

  const [currentPassword, setCurrentPassword] = useState('');
  const [newPassword, setNewPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);

  if (!user) {
    return <Navigate to="/login" replace />;
  }

  if (!user.mustChangePassword) {
    return <Navigate to="/dashboard" replace />;
  }

  const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setErrorMessage(null);

    if (newPassword !== confirmPassword) {
      setErrorMessage('The new password confirmation does not match.');
      return;
    }

    setIsSubmitting(true);
    try {
      await changePassword(currentPassword, newPassword);
      setCurrentPassword('');
      setNewPassword('');
      setConfirmPassword('');
    } catch (error) {
      setErrorMessage(extractErrorMessage(error));
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <main className="flex min-h-screen items-center justify-center bg-stone-950 px-6 py-10 text-stone-100">
      <section className="w-full max-w-lg rounded-[2rem] border border-white/10 bg-white/5 p-8 shadow-[0_30px_80px_rgba(0,0,0,0.35)] backdrop-blur sm:p-10">
        <div className="space-y-2">
          <p className="text-sm font-semibold tracking-[0.16em] text-amber-300 uppercase">
            Security Check
          </p>
          <h1 className="text-3xl font-black tracking-tight text-white">
            Change your password
          </h1>
          <p className="text-sm leading-6 text-stone-400">
            Your account must update its password before you can continue into warehouse operations.
          </p>
        </div>

        <form className="mt-8 space-y-5" onSubmit={handleSubmit}>
          <label className="block space-y-2">
            <span className="text-sm font-medium text-stone-300">Current Password</span>
            <input
              className={inputCls}
              type="password"
              value={currentPassword}
              onChange={(event) => setCurrentPassword(event.target.value)}
              autoComplete="current-password"
              disabled={isSubmitting}
            />
          </label>

          <label className="block space-y-2">
            <span className="text-sm font-medium text-stone-300">New Password</span>
            <input
              className={inputCls}
              type="password"
              value={newPassword}
              onChange={(event) => setNewPassword(event.target.value)}
              autoComplete="new-password"
              placeholder="At least 8 characters with letters and numbers"
              disabled={isSubmitting}
            />
          </label>

          <label className="block space-y-2">
            <span className="text-sm font-medium text-stone-300">Confirm New Password</span>
            <input
              className={inputCls}
              type="password"
              value={confirmPassword}
              onChange={(event) => setConfirmPassword(event.target.value)}
              autoComplete="new-password"
              disabled={isSubmitting}
            />
          </label>

          {errorMessage ? (
            <div className="rounded-2xl border border-red-500/20 bg-red-950/20 px-4 py-3 text-sm text-red-300">
              {errorMessage}
            </div>
          ) : null}

          <div className="space-y-3">
            <button
              className="w-full rounded-2xl bg-amber-300 px-4 py-3 text-base font-semibold text-stone-900 transition hover:bg-amber-200 disabled:cursor-not-allowed disabled:bg-amber-500/60"
              type="submit"
              disabled={isSubmitting}
            >
              {isSubmitting ? 'Updating Password...' : 'Update Password'}
            </button>

            <button
              type="button"
              onClick={() => void logout()}
              className="w-full rounded-2xl border border-white/10 px-4 py-3 text-sm font-medium text-stone-400 transition hover:bg-white/10 hover:text-stone-100"
              disabled={isSubmitting}
            >
              Sign Out
            </button>
          </div>
        </form>
      </section>
    </main>
  );
}
