import { useLocation, useNavigate } from "react-router";

export type Origin = { to: string; label: string; state?: unknown };
export type NavState = { from: Origin };

const MAX_DEPTH = 6;

function readOrigin(state: unknown): Origin | null {
  const from = (state as Partial<NavState> | null)?.from;
  return from && typeof from.to === "string" && typeof from.label === "string" ? from : null;
}

function trim(state: unknown, depth: number): NavState | undefined {
  const from = readOrigin(state);
  if (!from || depth <= 0) return undefined;
  return { from: { ...from, state: trim(from.state, depth - 1) } };
}

export function useHere(label: string): NavState {
  const location = useLocation();
  return { from: { to: location.pathname + location.search, label, state: trim(location.state, MAX_DEPTH) } };
}

export function useOrigin(fallback: Origin) {
  const location = useLocation();
  const navigate = useNavigate();
  const origin = readOrigin(location.state);
  const back = origin ?? fallback;
  const goBack = () => {
    if (origin) navigate(-1);
    else navigate(back.to, { replace: true, state: back.state });
  };
  return {
    back: { ...back, onBack: origin ? goBack : undefined },
    carry: origin ? ({ from: origin } satisfies NavState) : undefined,
    fromOrigin: !!origin,
    goBack,
  };
}
