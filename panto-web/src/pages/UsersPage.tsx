export function UsersPage() {
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
          className="rounded-xl bg-amber-300 px-5 py-2.5 text-sm font-semibold text-stone-900 transition hover:bg-amber-200"
        >
          + New User
        </button>
      </div>

      <div className="rounded-[1.75rem] border border-white/10 bg-white/5 p-8 text-center text-stone-500">
        <p className="text-sm">User list coming in next commit.</p>
      </div>
    </div>
  );
}
