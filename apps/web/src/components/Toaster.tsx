import { Check, Info, TriangleAlert, X } from "lucide-react";
import { useCallback, useMemo, useRef, useState, type ReactNode } from "react";
import { cx } from "../lib/cx";
import { ToastContext, type ToastTone } from "../lib/toast";

interface Item {
  id: number;
  message: string;
  tone: ToastTone;
}

const icons = { success: Check, error: TriangleAlert, info: Info };
const tones = {
  success: "bg-green text-white",
  error: "bg-red text-white",
  info: "bg-navy text-white",
};

export function ToastProvider({ children }: { children: ReactNode }) {
  const [items, setItems] = useState<Item[]>([]);
  const seq = useRef(0);

  const dismiss = useCallback((id: number) => setItems((list) => list.filter((t) => t.id !== id)), []);

  const show = useCallback(
    (message: string, tone: ToastTone = "success") => {
      const id = ++seq.current;
      setItems((list) => [...list.slice(-2), { id, message, tone }]);
      setTimeout(() => dismiss(id), tone === "error" ? 6000 : 3500);
    },
    [dismiss],
  );

  const value = useMemo(() => ({ show }), [show]);

  return (
    <ToastContext.Provider value={value}>
      {children}
      <div className="pointer-events-none fixed inset-x-0 bottom-0 z-[60] flex flex-col items-center gap-2 p-4 sm:items-end sm:p-6" aria-live="polite">
        {items.map((t) => {
          const Icon = icons[t.tone];
          return (
            <div
              key={t.id}
              role={t.tone === "error" ? "alert" : "status"}
              className={cx("pointer-events-auto flex w-full max-w-sm items-start gap-3 rounded-xl border-2 border-navy px-4 py-3 text-sm font-semibold shadow-hard-sm", tones[t.tone])}
            >
              <Icon className="mt-0.5 size-4 shrink-0" strokeWidth={2.5} />
              <span className="flex-1">{t.message}</span>
              <button type="button" onClick={() => dismiss(t.id)} aria-label="Fechar aviso" className="opacity-80 hover:opacity-100">
                <X className="size-4" />
              </button>
            </div>
          );
        })}
      </div>
    </ToastContext.Provider>
  );
}
