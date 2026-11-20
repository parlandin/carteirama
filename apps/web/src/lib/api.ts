import axios, { AxiosError } from "axios";
import type { Page } from "./types";

export interface FieldError {
  field: string;
  code: string;
  message: string;
}

export class ApiError extends Error {
  readonly status: number;
  readonly code: string;
  readonly details: FieldError[];

  constructor(status: number, code: string, message: string, details: FieldError[] = []) {
    super(message);
    this.status = status;
    this.code = code;
    this.details = details;
  }

  fieldErrors(): Record<string, string> {
    const out: Record<string, string> = {};
    for (const d of this.details) {
      if (!out[d.field]) out[d.field] = d.message;
    }
    return out;
  }
}

type Listener = () => void;
const unauthorizedListeners = new Set<Listener>();

export function onUnauthorized(fn: Listener) {
  unauthorizedListeners.add(fn);
  return () => {
    unauthorizedListeners.delete(fn);
  };
}

type Scalar = string | number | boolean | null | undefined;
type Query = Record<string, Scalar | Scalar[]>;

function serialize(params: Query): string {
  const out = new URLSearchParams();
  for (const [k, v] of Object.entries(params)) {
    for (const item of Array.isArray(v) ? v : [v]) {
      if (item !== undefined && item !== null && item !== "") out.append(k, String(item));
    }
  }
  return out.toString();
}

const http = axios.create({
  baseURL: import.meta.env.VITE_API_URL || "/api/v1",
  withCredentials: true,
  withXSRFToken: true,
  headers: { Accept: "application/json" },
  paramsSerializer: { serialize },
});

async function toApiError(e: unknown): Promise<ApiError> {
  if (!(e instanceof AxiosError)) return new ApiError(0, "internal_error", "Erro inesperado. Tente novamente.");
  const res = e.response;
  if (!res) return new ApiError(0, "network_error", "Sem conexão com o servidor. Verifique sua internet.");

  let body: unknown = res.data;
  if (body instanceof Blob) {
    try {
      body = JSON.parse(await body.text());
    } catch {
      body = null;
    }
  }
  const err = (body as { error?: { code?: string; message?: string; details?: FieldError[] } } | null)?.error;
  if (err?.code) return new ApiError(res.status, err.code, err.message ?? "Erro inesperado.", err.details ?? []);
  if (res.status === 502 || res.status === 503 || res.status === 504) {
    return new ApiError(res.status, "unavailable", "O servidor não está respondendo. Tente de novo em instantes.");
  }
  return new ApiError(res.status, "internal_error", "Erro inesperado. Tente novamente.");
}

const UNSAFE_METHODS = new Set(["post", "put", "patch", "delete"]);

function hasCsrfCookie(): boolean {
  return document.cookie.split(";").some((c) => c.trim().startsWith("XSRF-TOKEN="));
}

http.interceptors.request.use(async (config) => {
  if (UNSAFE_METHODS.has((config.method ?? "get").toLowerCase()) && !hasCsrfCookie()) {
    await http.get("/auth/csrf");
  }
  return config;
});

http.interceptors.response.use(undefined, async (e) => {
  const err = await toApiError(e);
  if (err.status === 401 && err.code === "not_authenticated") {
    unauthorizedListeners.forEach((fn) => fn());
  }
  return Promise.reject(err);
});

export const api = {
  get: <T>(path: string, params?: Query) => http.get<T>(path, { params }).then((r) => r.data),
  post: <T>(path: string, body?: unknown) => http.post<T>(path, body).then((r) => r.data),
  patch: <T>(path: string, body: unknown) => http.patch<T>(path, body).then((r) => r.data),
  delete: <T = void>(path: string) => http.delete<T>(path).then((r) => r.data),
};

export async function getAll<T>(path: string, query: Query = {}): Promise<T[]> {
  const limit = 100;
  const out: T[] = [];
  for (let page = 1; page <= 50; page++) {
    const res = await api.get<Page<T>>(path, { ...query, page, limit });
    out.push(...res.data);
    if (out.length >= res.meta.total || res.data.length < limit) break;
  }
  return out;
}

export async function download(path: string, params: Query, fallbackName: string) {
  const res = await http.get<Blob>(path, { params, responseType: "blob" });
  const disposition = String(res.headers["content-disposition"] ?? "");
  const match = /filename\*?=(?:UTF-8'')?"?([^";]+)"?/i.exec(disposition);
  const name = match ? decodeURIComponent(match[1]) : fallbackName;
  const url = URL.createObjectURL(res.data);
  const a = document.createElement("a");
  a.href = url;
  a.download = name;
  document.body.appendChild(a);
  a.click();
  a.remove();
  setTimeout(() => URL.revokeObjectURL(url), 1000);
}

export function errorMessage(e: unknown): string {
  if (e instanceof ApiError) return e.message;
  return "Erro inesperado. Tente novamente.";
}
