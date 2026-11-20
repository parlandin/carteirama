import { useQuery, useQueryClient } from "@tanstack/react-query";
import { useEffect } from "react";
import { api, getAll } from "./api";
import { currentMonth } from "./format";
import type {
  AdminAccountDetail,
  AdminAccountFilter,
  AdminAccountItem,
  AdminInvoiceItem,
  AdminPlan,
  AdminSummary,
  Charge,
  ChargeSummary,
  Client,
  ClientDetail,
  ClientStatus,
  GroupDetail,
  GroupListItem,
  Invoice,
  Page,
  Subscription,
} from "./types";

export const keys = {
  groups: (month: string) => ["groups", month] as const,
  group: (id: string) => ["group", id] as const,
  clients: (params: object) => ["clients", params] as const,
  client: (id: string) => ["client", id] as const,
  charges: (params: object) => ["charges", params] as const,
  charge: (id: string) => ["charge", id] as const,
  summary: (month: string) => ["summary", month] as const,
  subscription: ["subscription"] as const,
  invoices: ["invoices"] as const,
};

export function useInvalidateData() {
  const qc = useQueryClient();
  return () =>
    Promise.all(
      ["groups", "group", "clients", "client", "charges", "charge", "summary", "subscription"].map((k) =>
        qc.invalidateQueries({ queryKey: [k] }),
      ),
    );
}

export function useGroups(month: string) {
  return useQuery({
    queryKey: keys.groups(month),
    queryFn: () => api.get<{ month: string; data: GroupListItem[] }>("/groups", { month }).then((r) => r.data),
  });
}

export function useGroup(id: string) {
  return useQuery({ queryKey: keys.group(id), queryFn: () => api.get<GroupDetail>(`/groups/${id}`) });
}

export function useClients(params: { group_id?: string; status?: ClientStatus; q?: string } = {}) {
  return useQuery({
    queryKey: keys.clients(params),
    queryFn: () => getAll<Client>("/clients", params),
    placeholderData: (prev) => prev,
  });
}

export function useClient(id: string) {
  return useQuery({ queryKey: keys.client(id), queryFn: () => api.get<ClientDetail>(`/clients/${id}`) });
}

export function useCharges(params: { month: string; status?: string; group_id?: string; client_id?: string }, enabled = true) {
  return useQuery({
    queryKey: keys.charges(params),
    queryFn: () => getAll<Charge>("/charges", params),
    enabled,
  });
}

export function useCharge(id: string) {
  return useQuery({ queryKey: keys.charge(id), queryFn: () => api.get<Charge>(`/charges/${id}`) });
}

export function useSummary(month: string) {
  return useQuery({ queryKey: keys.summary(month), queryFn: () => api.get<ChargeSummary>("/summary", { month }) });
}

export function useSubscription(enabled = true) {
  return useQuery({ queryKey: keys.subscription, queryFn: () => api.get<Subscription>("/subscription"), enabled });
}

export function useInvoices(enabled = true) {
  return useQuery({
    queryKey: keys.invoices,
    queryFn: () => getAll<Invoice>("/subscription/invoices"),
    enabled,
  });
}

const generated = new Set<string>();

export function useAutoGenerateCharges(enabled: boolean) {
  const qc = useQueryClient();
  useEffect(() => {
    const month = currentMonth();
    if (!enabled || generated.has(month)) return;
    generated.add(month);
    api
      .post<{ month: string; created: number }>("/charges/generate", { month })
      .then((r) => {
        if (r.created > 0) {
          for (const k of ["groups", "group", "charges", "summary", "client"]) qc.invalidateQueries({ queryKey: [k] });
        }
      })
      .catch(() => generated.delete(month));
  }, [enabled, qc]);
}

export function resetAutoGenerate() {
  generated.clear();
}

export function useAdminSummary() {
  return useQuery({ queryKey: ["admin", "summary"], queryFn: () => api.get<AdminSummary>("/admin/summary") });
}

export function useAdminAccounts(params: { status?: AdminAccountFilter; q?: string; limit?: number }) {
  return useQuery({
    queryKey: ["admin", "accounts", params],
    queryFn: () =>
      params.limit
        ? api.get<Page<AdminAccountItem>>("/admin/accounts", { ...params, page: 1 }).then((r) => r.data)
        : getAll<AdminAccountItem>("/admin/accounts", params),
    placeholderData: (prev) => prev,
  });
}

export function useAdminAccount(id: string) {
  return useQuery({ queryKey: ["admin", "account", id], queryFn: () => api.get<AdminAccountDetail>(`/admin/accounts/${id}`) });
}

export function useAdminInvoices(params: { month: string; status?: string }) {
  return useQuery({
    queryKey: ["admin", "invoices", params],
    queryFn: () => getAll<AdminInvoiceItem>("/admin/invoices", params),
    placeholderData: (prev) => prev,
  });
}

export function useAdminPlans() {
  return useQuery({ queryKey: ["admin", "plans"], queryFn: () => api.get<{ data: AdminPlan[] }>("/admin/plans").then((r) => r.data) });
}
