import axios from 'axios';
import { type FormEvent, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuthStore } from '../store/auth-store';

function resolveErrorMessage(error: unknown): string {
  if (axios.isAxiosError<{ message?: string }>(error)) {
    return error.response?.data?.message ?? 'Unable to sign in right now.';
  }
  return 'Unable to sign in right now.';
}

export function LoginPage() {
  const navigate = useNavigate();
  const login = useAuthStore((s) => s.login);

  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);

  const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setErrorMessage(null);
    setIsSubmitting(true);

    try {
      await login(username.trim(), password);
      navigate('/dashboard', { replace: true });
    } catch (error) {
      setErrorMessage(resolveErrorMessage(error));
      setPassword('');
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <main className="min-h-screen bg-[radial-gradient(circle_at_top,_#f4ede2_0%,_#efe4d4_32%,_#dcc5a3_100%)] px-6 py-10 text-stone-900">
      <div className="mx-auto grid min-h-[calc(100vh-5rem)] max-w-6xl items-center gap-10 lg:grid-cols-[1.1fr_0.9fr]">
        <section className="space-y-6">
          <p className="inline-flex rounded-full border border-stone-900/15 bg-white/55 px-4 py-1 text-sm font-medium tracking-[0.2em] text-stone-700 uppercase backdrop-blur">
            Panto Warehouse System
          </p>

          <div className="space-y-4">
            <h1 className="max-w-3xl text-5xl font-black leading-tight tracking-tight text-balance sm:text-6xl">
              Food stock, batch expiry, and invoice work in one place.
            </h1>
            <p className="max-w-2xl text-lg leading-8 text-stone-700 sm:text-xl">
              Built for small wholesale teams who need clear inventory movement,
              batch traceability, and fast daily operations without spreadsheet
              chaos.
            </p>
          </div>

          <div className="grid gap-4 sm:grid-cols-3">
            <article className="rounded-3xl border border-stone-900/10 bg-white/60 p-5 backdrop-blur">
              <p className="text-sm font-semibold tracking-[0.16em] text-stone-500 uppercase">
                Batch First
              </p>
              <p className="mt-3 text-sm leading-6 text-stone-700">
                Track expiry-sensitive stock by batch instead of flattening
                everything into one number.
              </p>
            </article>

            <article className="rounded-3xl border border-stone-900/10 bg-white/60 p-5 backdrop-blur">
              <p className="text-sm font-semibold tracking-[0.16em] text-stone-500 uppercase">
                Sales Ready
              </p>
              <p className="mt-3 text-sm leading-6 text-stone-700">
                Support warehouse and marketing staff with one shared order and
                invoice workflow.
              </p>
            </article>

            <article className="rounded-3xl border border-stone-900/10 bg-white/60 p-5 backdrop-blur">
              <p className="text-sm font-semibold tracking-[0.16em] text-stone-500 uppercase">
                Audit Clear
              </p>
              <p className="mt-3 text-sm leading-6 text-stone-700">
                Keep a cleaner trail of who changed data and when operations
                happened.
              </p>
            </article>
          </div>
        </section>

        <section className="rounded-[2rem] border border-stone-900/10 bg-white/80 p-8 shadow-[0_30px_80px_rgba(79,55,25,0.18)] backdrop-blur sm:p-10">
          <div className="space-y-2">
            <p className="text-sm font-semibold tracking-[0.16em] text-amber-700 uppercase">
              Sign In
            </p>
            <h2 className="text-3xl font-bold tracking-tight text-stone-900">
              Welcome back
            </h2>
            <p className="text-sm leading-6 text-stone-600">
              Use your staff account to access warehouse operations, sales, and
              reporting tools.
            </p>
          </div>

          <form className="mt-8 space-y-5" onSubmit={handleSubmit}>
            <label className="block space-y-2">
              <span className="text-sm font-medium text-stone-700">Username</span>
              <input
                className="w-full rounded-2xl border border-stone-300 bg-white px-4 py-3 text-base text-stone-900 outline-none transition focus:border-amber-700 focus:ring-4 focus:ring-amber-200"
                placeholder="Enter your username"
                type="text"
                value={username}
                onChange={(e) => setUsername(e.target.value)}
                disabled={isSubmitting}
                autoComplete="username"
              />
            </label>

            <label className="block space-y-2">
              <span className="text-sm font-medium text-stone-700">Password</span>
              <input
                className="w-full rounded-2xl border border-stone-300 bg-white px-4 py-3 text-base text-stone-900 outline-none transition focus:border-amber-700 focus:ring-4 focus:ring-amber-200"
                placeholder="Enter your password"
                type="password"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                disabled={isSubmitting}
                autoComplete="current-password"
              />
            </label>

            {errorMessage ? (
              <div className="rounded-2xl border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700">
                {errorMessage}
              </div>
            ) : null}

            <button
              className="w-full rounded-2xl bg-stone-900 px-4 py-3 text-base font-semibold text-white transition hover:bg-stone-800 disabled:cursor-not-allowed disabled:bg-stone-500"
              type="submit"
              disabled={isSubmitting}
            >
              {isSubmitting ? 'Signing In...' : 'Sign In to Panto'}
            </button>
          </form>
        </section>
      </div>
    </main>
  );
}
