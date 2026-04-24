import axios from 'axios';
import { useEffect, useState } from 'react';
import './App.css';
import { DashboardPage } from './pages/DashboardPage';
import { LoginPage } from './pages/LoginPage';
import { useAuthStore } from './store/auth-store';

interface ErrorResponseBody {
  message?: string;
}

function App() {
  const accessToken = useAuthStore((state) => state.accessToken);
  const isAuthenticated = useAuthStore((state) => state.isAuthenticated);
  const login = useAuthStore((state) => state.login);
  const logout = useAuthStore((state) => state.logout);
  const user = useAuthStore((state) => state.user);

  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [isBootstrapping, setIsBootstrapping] = useState(true);

  useEffect(() => {
    let active = true;

    const bootstrapAuth = async () => {
      try {
        await useAuthStore.getState().refresh();
      } catch {
        useAuthStore.getState().logout();
      } finally {
        if (active) {
          setIsBootstrapping(false);
        }
      }
    };

    void bootstrapAuth();

    return () => {
      active = false;
    };
  }, []);

  const handleSubmit = async () => {
    setErrorMessage(null);
    setIsSubmitting(true);

    try {
      await login(username.trim(), password);
      setPassword('');
    } catch (error) {
      setErrorMessage(resolveErrorMessage(error));
    } finally {
      setIsSubmitting(false);
    }
  };

  if (isBootstrapping) {
    return (
      <main className="flex min-h-screen items-center justify-center bg-stone-950 px-6 text-stone-100">
        <div className="rounded-3xl border border-white/10 bg-white/5 px-6 py-5 text-sm font-medium tracking-[0.16em] text-stone-300 uppercase backdrop-blur">
          Restoring session...
        </div>
      </main>
    );
  }

  return (
    <div className="min-h-screen">
      {isAuthenticated ? (
        <>
          <div className="fixed inset-x-0 top-0 z-20 flex justify-end px-4 py-4">
            <div className="inline-flex items-center gap-3 rounded-full border border-white/10 bg-stone-950/80 px-3 py-2 text-sm text-stone-200 shadow-lg backdrop-blur">
              <span className="hidden sm:inline">
                {user?.username} · {user?.role}
              </span>
              <span className="sm:hidden">{user?.username}</span>
              <button
                className="rounded-full bg-white/10 px-4 py-2 font-semibold text-white transition hover:bg-white/20"
                onClick={logout}
                type="button"
              >
                Sign Out
              </button>
            </div>
          </div>
          <DashboardPage />
        </>
      ) : (
        <LoginPage
          username={username}
          password={password}
          errorMessage={errorMessage}
          isSubmitting={isSubmitting}
          onUsernameChange={setUsername}
          onPasswordChange={setPassword}
          onSubmit={handleSubmit}
        />
      )}
    </div>
  );
}

function resolveErrorMessage(error: unknown): string {
  if (axios.isAxiosError<ErrorResponseBody>(error)) {
    return error.response?.data?.message ?? 'Unable to sign in right now.';
  }

  return 'Unable to sign in right now.';
}

export default App;
