import { useEffect, useState } from 'react';
import { RouterProvider } from 'react-router-dom';
import { router } from './router';
import { useAuthStore } from './store/auth-store';

function App() {
  const [isBootstrapping, setIsBootstrapping] = useState(true);

  useEffect(() => {
    let active = true;

    const bootstrap = async () => {
      try {
        await useAuthStore.getState().refresh();
      } catch {
        useAuthStore.getState().logout();
      } finally {
        if (active) setIsBootstrapping(false);
      }
    };

    void bootstrap();
    return () => { active = false; };
  }, []);

  if (isBootstrapping) {
    return (
      <main className="flex min-h-screen items-center justify-center bg-stone-950">
        <div className="rounded-3xl border border-white/10 bg-white/5 px-6 py-5 text-sm font-medium tracking-[0.16em] text-stone-300 uppercase backdrop-blur">
          Restoring session...
        </div>
      </main>
    );
  }

  return <RouterProvider router={router} />;
}

export default App;
