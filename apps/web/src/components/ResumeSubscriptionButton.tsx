import { useQueryClient } from "@tanstack/react-query";
import { RotateCcw } from "lucide-react";
import { useState } from "react";
import { api, errorMessage } from "../lib/api";
import { keys } from "../lib/queries";
import { useToast } from "../lib/toast";
import type { Subscription } from "../lib/types";
import { Button } from "./ui";

export function ResumeSubscriptionButton({ variant = "primary", className }: { variant?: "primary" | "amber"; className?: string }) {
  const qc = useQueryClient();
  const toast = useToast();
  const [busy, setBusy] = useState(false);

  async function resume() {
    setBusy(true);
    try {
      const updated = await api.post<Subscription>("/subscription/resume");
      qc.setQueryData(keys.subscription, updated);
      await qc.invalidateQueries({ queryKey: keys.invoices });
      toast.show("Cancelamento desfeito. Sua assinatura continua.");
    } catch (e) {
      toast.show(errorMessage(e), "error");
    } finally {
      setBusy(false);
    }
  }

  return (
    <Button variant={variant} icon={RotateCcw} loading={busy} onClick={resume} className={className}>
      Manter assinatura
    </Button>
  );
}
