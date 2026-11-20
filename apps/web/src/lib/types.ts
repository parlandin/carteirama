export type PersonType = "pf" | "pj";
export type AccountStatus = "trial" | "active" | "suspended" | "cancelled";
export type ClientStatus = "active" | "paused" | "archived";
export type ChargeStatus = "pending" | "paid" | "cancelled";
export type ChargeDisplayStatus = ChargeStatus | "late";
export type InvoiceStatus = "pending" | "paid" | "cancelled";
export type PaymentMethod = "pix" | "boleto" | "card";
export type SubscriptionStatus = "trialing" | "active" | "cancelled";

export interface Page<T> {
  data: T[];
  meta: { page: number; limit: number; total: number };
}

export interface Me {
  user: { id: string; name: string; email: string; is_admin: boolean };
  account: MeAccount | null;
}

export interface MeAccount {
  id: string;
  person_type: PersonType;
  document: string;
  legal_name: string;
  company_name: string | null;
  phone: string;
  zip: string | null;
  address: string | null;
  city: string | null;
  state: string | null;
  status: AccountStatus;
  trial_ends_at: string | null;
  suspended_at: string | null;
  cancelled_at: string | null;
}

export interface GroupListItem {
  id: string;
  name: string;
  color: string | null;
  notes: string | null;
  client_count: number;
  month_total_cents: number;
}

export interface GroupResponse {
  id: string;
  name: string;
  color: string | null;
  notes: string | null;
  archived_at: string | null;
  created_at: string;
}

export interface GroupDetail extends GroupResponse {
  clients: {
    id: string;
    name: string;
    email: string | null;
    amount_cents: number;
    due_day: number;
    status: ClientStatus;
  }[];
}

export interface Client {
  id: string;
  group_id: string;
  group_name: string;
  group_color: string | null;
  name: string;
  document: string | null;
  email: string | null;
  phone: string | null;
  amount_cents: number;
  due_day: number;
  status: ClientStatus;
  notes: string | null;
  created_at: string;
}

export interface Charge {
  id: string;
  client_id: string | null;
  client_name: string;
  group_id: string | null;
  group_name: string | null;
  group_color: string | null;
  reference_month: string;
  amount_cents: number;
  due_date: string;
  status: ChargeStatus;
  display_status: ChargeDisplayStatus;
  paid_at: string | null;
  note: string | null;
  kind: ChargeKind;
  description: string | null;
  bonus_cents: number;
  cost_cents: number;
  total_cents: number;
  net_cents: number;
}

export type ExtraKind = "bonus" | "cost";

export interface ClientExtra {
  id: string;
  kind: ExtraKind;
  description: string;
  amount_cents: number;
  recurring: boolean;
  start_month: string;
  end_month: string | null;
}

export type ChargeKind = "recurring" | "one_off";

export interface ClientDetail {
  client: Client;
  charges: Charge[];
  extras: ClientExtra[];
}

export interface ChargeSummary {
  month: string;
  total_cents: number;
  paid_cents: number;
  pending_cents: number;
  late_cents: number;
  total_count: number;
  paid_count: number;
  pending_count: number;
  late_count: number;
  cost_cents: number;
  net_cents: number;
}

export interface Invoice {
  id: string;
  number: string;
  amount_cents: number;
  due_date: string;
  status: InvoiceStatus;
  display_status: InvoiceStatus | "late";
  paid_at: string | null;
  method: PaymentMethod | null;
}

export interface Subscription {
  plan: { code: string; name: string };
  status: SubscriptionStatus;
  price_cents: number;
  next_due_date: string | null;
  cancel_at_period_end: boolean;
  account_status: AccountStatus;
  trial_ends_at: string | null;
  usage: { clients: number; users: number };
}

export interface PaymentLink {
  pix_code: string | null;
  boleto_url: string | null;
  card_url: string | null;
}

export type AdminAccountFilter = AccountStatus | "late";

export interface AdminAccountItem {
  id: string;
  person_type: PersonType;
  document: string;
  legal_name: string;
  company_name: string | null;
  email: string;
  user_name: string;
  status: AccountStatus;
  display_status: AccountStatus | "late";
  trial_ends_at: string | null;
  suspended_at: string | null;
  cancelled_at: string | null;
  created_at: string;
}

export interface AdminAccountDetail {
  account: AdminAccountItem;
  phone: string | null;
  zip: string | null;
  address: string | null;
  city: string | null;
  state: string | null;
  subscription: {
    plan_code: string;
    status: SubscriptionStatus;
    price_cents: number;
    next_due_date: string | null;
    cancel_at_period_end: boolean;
  } | null;
  invoices: Invoice[];
  audit_log: {
    id: string;
    action: string;
    actor_user_id: string | null;
    actor_name: string | null;
    meta: Record<string, unknown> | null;
    created_at: string;
  }[];
}

export interface AdminInvoiceItem {
  invoice: Invoice;
  account_id: string;
  account_name: string;
}

export interface AdminPlan {
  code: string;
  name: string;
  price_cents: number;
  trial_days: number;
  active: boolean;
}

export interface AdminSummary {
  mrr_cents: number;
  accounts_by_status: Record<string, number>;
  invoices_month: { month: string; paid_cents: number; pending_cents: number; late_cents: number };
  mrr_by_month: { month: string; cents: number }[];
}
