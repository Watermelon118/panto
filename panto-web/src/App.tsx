import { useState } from 'react';
import './App.css';
import { DashboardPage } from './pages/DashboardPage';
import { LoginPage } from './pages/LoginPage';

type AppView = 'login' | 'dashboard';

function App() {
  const [view, setView] = useState<AppView>('login');

  return (
    <div className="min-h-screen">
      <div className="fixed inset-x-0 top-0 z-20 flex justify-center px-4 py-4">
        <div className="inline-flex rounded-full border border-stone-900/10 bg-white/70 p-1 shadow-lg backdrop-blur">
          <button
            className={`rounded-full px-4 py-2 text-sm font-semibold transition ${
              view === 'login'
                ? 'bg-stone-900 text-white'
                : 'text-stone-700 hover:bg-stone-900/5'
            }`}
            onClick={() => setView('login')}
            type="button"
          >
            Login View
          </button>
          <button
            className={`rounded-full px-4 py-2 text-sm font-semibold transition ${
              view === 'dashboard'
                ? 'bg-stone-900 text-white'
                : 'text-stone-700 hover:bg-stone-900/5'
            }`}
            onClick={() => setView('dashboard')}
            type="button"
          >
            Dashboard View
          </button>
        </div>
      </div>

      {view === 'login' ? <LoginPage /> : <DashboardPage />}
    </div>
  );
}

export default App;
