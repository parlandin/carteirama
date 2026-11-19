import { useSearchParams } from "react-router";
import { addMonths, currentMonth, isValidMonth } from "./format";

export function useMonthParam() {
  const [params, setParams] = useSearchParams();
  const raw = params.get("mes");
  const month = isValidMonth(raw) ? raw : currentMonth();
  const shift = (delta: number) => {
    const next = addMonths(month, delta);
    setParams(
      (p) => {
        const n = new URLSearchParams(p);
        if (next === currentMonth()) n.delete("mes");
        else n.set("mes", next);
        return n;
      },
      { replace: true },
    );
  };
  return [month, shift] as const;
}
