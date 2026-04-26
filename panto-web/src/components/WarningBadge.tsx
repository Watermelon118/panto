interface WarningBadgeProps {
  count: number;
  onClick: () => void;
}

export function WarningBadge({ count, onClick }: WarningBadgeProps) {
  if (count <= 0) {
    return null;
  }

  return (
    <button
      type="button"
      onClick={onClick}
      className="inline-flex items-center gap-2 rounded-full border border-amber-300/30 bg-amber-300/10 px-3 py-1.5 text-xs font-semibold text-amber-200 transition hover:bg-amber-300/15"
      title="Open expiring batch warnings"
    >
      <span className="h-2 w-2 rounded-full bg-amber-300" />
      <span>Expiry Alerts</span>
      <span className="rounded-full bg-amber-300 px-1.5 py-0.5 text-[10px] font-black text-stone-900">
        {count}
      </span>
    </button>
  );
}
