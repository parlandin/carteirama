import { useQuery, useQueryClient } from "@tanstack/react-query";
import { useEffect } from "react";
import { ApiError, api, onUnauthorized } from "./api";
import type { Me } from "./types";

export const ME_KEY = ["me"] as const;

async function fetchMe(): Promise<Me | null> {
  try {
    return await api.get<Me>("/me");
  } catch (e) {
    if (e instanceof ApiError && (e.status === 401 || e.status === 403)) return null;
    throw e;
  }
}

export function useMe() {
  return useQuery({ queryKey: ME_KEY, queryFn: fetchMe, staleTime: 60_000, retry: 1 });
}

/** Se qualquer chamada voltar 401, a sessão acabou: limpa o cache e o usuário. */
export function useSessionWatcher() {
  const qc = useQueryClient();
  useEffect(
    () =>
      onUnauthorized(() => {
        qc.setQueryData(ME_KEY, null);
      }),
    [qc],
  );
}

export function useLogout() {
  const qc = useQueryClient();
  return async () => {
    try {
      await api.post("/auth/logout");
    } finally {
      qc.clear();
      qc.setQueryData(ME_KEY, null);
    }
  };
}

/** Conta suspensa ou cancelada: a API recusa qualquer escrita. */
export function isReadOnly(me: Me | null | undefined): boolean {
  const s = me?.account?.status;
  return s === "suspended" || s === "cancelled";
}

export function displayAccountName(me: Me): string {
  const a = me.account;
  if (!a) return "Administrador";
  return a.company_name || a.legal_name;
}
