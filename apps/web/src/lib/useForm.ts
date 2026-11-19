import { useState } from "react";
import { ApiError } from "./api";

export function useForm<T extends Record<string, unknown>>(initial: T) {
  const [values, setValues] = useState<T>(initial);
  const [errors, setErrors] = useState<Record<string, string>>({});
  const [formError, setFormError] = useState<string | null>(null);

  const set = <K extends keyof T>(key: K, value: T[K]) => {
    setValues((v) => ({ ...v, [key]: value }));
    setErrors((e) => {
      if (!e[key as string]) return e;
      const next = { ...e };
      delete next[key as string];
      return next;
    });
  };

  const fail = (e: unknown, fieldMap: Record<string, string> = {}) => {
    if (e instanceof ApiError) {
      const fe = e.fieldErrors();
      const mapped: Record<string, string> = {};
      for (const [k, msg] of Object.entries(fe)) mapped[fieldMap[k] ?? k] = msg;
      setErrors(mapped);
      setFormError(Object.keys(mapped).length > 0 ? "Confira os campos destacados." : e.message);
    } else {
      setFormError("Erro inesperado. Tente novamente.");
    }
  };

  const reset = () => {
    setErrors({});
    setFormError(null);
  };

  return { values, set, setValues, errors, setErrors, formError, setFormError, fail, reset };
}
