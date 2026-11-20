import {
  ArrowLeft,
  Check,
  ChevronLeft,
  ChevronRight,
  Clock,
  LoaderCircle,
  Pause,
  Star,
  TriangleAlert,
  X,
  type LucideIcon,
} from "lucide-react";
import {
  useEffect,
  useId,
  useRef,
  type ButtonHTMLAttributes,
  type InputHTMLAttributes,
  type ReactNode,
  type SelectHTMLAttributes,
  type TextareaHTMLAttributes,
} from "react";
import { createPortal } from "react-dom";
import { Link, type LinkProps } from "react-router";
import { groupColor, textOn } from "../lib/colors";
import { cx } from "../lib/cx";
import { initials, monthLabel } from "../lib/format";

type Variant = "primary" | "secondary" | "blue" | "dark" | "danger" | "ghost" | "amber";
type Size = "sm" | "md" | "lg";

const variants: Record<Variant, string> = {
  primary: "bg-green text-white border-navy shadow-hard-sm hover:brightness-110",
  secondary: "bg-white text-ink border-navy shadow-hard-sm hover:bg-paper-2",
  blue: "bg-blue text-white border-navy shadow-hard-sm hover:brightness-110",
  dark: "bg-navy text-white border-navy shadow-[4px_4px_0_0_var(--color-amber)] hover:bg-navy-2",
  amber: "bg-amber text-navy border-navy shadow-hard-sm hover:brightness-105",
  danger: "bg-white text-red border-red shadow-[4px_4px_0_0_var(--color-red)] hover:bg-red-soft",
  ghost: "bg-transparent text-ink border-transparent hover:bg-navy/5",
};

const sizes: Record<Size, string> = {
  sm: "h-9 px-3 text-sm gap-1.5 rounded-lg",
  md: "h-11 px-4 text-[15px] gap-2 rounded-[10px]",
  lg: "h-13 px-6 text-base gap-2 rounded-xl",
};

function buttonClass(variant: Variant, size: Size, block?: boolean, extra?: string) {
  return cx(
    "inline-flex shrink-0 items-center justify-center border-2 font-bold whitespace-nowrap transition",
    "active:translate-x-[2px] active:translate-y-[2px] active:shadow-none",
    "disabled:pointer-events-none disabled:opacity-45 disabled:shadow-none",
    "focus-visible:outline-3 focus-visible:outline-offset-2 focus-visible:outline-blue",
    variants[variant],
    sizes[size],
    block && "w-full",
    extra,
  );
}

type ButtonProps = ButtonHTMLAttributes<HTMLButtonElement> & {
  variant?: Variant;
  size?: Size;
  block?: boolean;
  loading?: boolean;
  icon?: LucideIcon;
  iconRight?: LucideIcon;
};

export function Button({
  variant = "secondary",
  size = "md",
  block,
  loading,
  icon: Icon,
  iconRight: IconRight,
  className,
  children,
  disabled,
  type = "button",
  ...rest
}: ButtonProps) {
  return (
    <button
      type={type}
      className={buttonClass(variant, size, block, className)}
      disabled={disabled || loading}
      {...rest}
    >
      {loading ? <LoaderCircle className="size-4 animate-spin" /> : Icon && <Icon className="size-4" strokeWidth={2.5} />}
      {children}
      {IconRight && !loading && <IconRight className="size-4" strokeWidth={2.5} />}
    </button>
  );
}

type LinkButtonProps = LinkProps & {
  variant?: Variant;
  size?: Size;
  block?: boolean;
  icon?: LucideIcon;
  iconRight?: LucideIcon;
  disabled?: boolean;
};

export function LinkButton({
  variant = "secondary",
  size = "md",
  block,
  icon: Icon,
  iconRight: IconRight,
  className,
  children,
  disabled,
  ...rest
}: LinkButtonProps) {
  if (disabled) {
    return (
      <span aria-disabled="true" className={buttonClass(variant, size, block, cx("pointer-events-none opacity-45 shadow-none", className as string))}>
        {Icon && <Icon className="size-4" strokeWidth={2.5} />}
        {children as ReactNode}
      </span>
    );
  }
  return (
    <Link className={buttonClass(variant, size, block, className as string)} {...rest}>
      {Icon && <Icon className="size-4" strokeWidth={2.5} />}
      {children}
      {IconRight && <IconRight className="size-4" strokeWidth={2.5} />}
    </Link>
  );
}

export function Card({
  children,
  className,
  dark,
  as: Tag = "section",
}: {
  children: ReactNode;
  className?: string;
  dark?: boolean;
  as?: "section" | "div" | "article";
}) {
  return (
    <Tag
      className={cx(
        "rounded-2xl border-2 border-navy shadow-hard",
        dark ? "bg-navy text-white" : "bg-white",
        className,
      )}
    >
      {children}
    </Tag>
  );
}

export function Eyebrow({ children, className, tone = "blue" }: { children: ReactNode; className?: string; tone?: "blue" | "amber" | "muted" }) {
  const color = tone === "amber" ? "text-amber" : tone === "muted" ? "text-muted" : "text-blue";
  return <p className={cx("font-mono text-[11px] font-medium tracking-[0.14em] uppercase", color, className)}>{children}</p>;
}

export function PageHeader({
  eyebrow,
  title,
  subtitle,
  actions,
  badge,
  back,
}: {
  eyebrow?: ReactNode;
  title: ReactNode;
  subtitle?: ReactNode;
  actions?: ReactNode;
  badge?: ReactNode;
  back?: BackTarget;
}) {
  return (
    <header className="mb-6 md:mb-8">
      {back && <BackLink {...back} className="mb-6 md:mb-10" />}
      <div className="flex flex-col gap-5 lg:flex-row lg:items-end lg:justify-between">
        <div className="min-w-0">
          {eyebrow && <Eyebrow className="mb-2">{eyebrow}</Eyebrow>}
          <div className="flex flex-wrap items-center gap-3">
            <h1 className="text-[32px] leading-[1.05] font-extrabold tracking-tight break-words sm:text-[44px]">{title}</h1>
            {badge}
          </div>
          {subtitle && <div className="mt-2 max-w-2xl text-[15px] text-muted sm:text-base">{subtitle}</div>}
        </div>
        {actions && <div className="flex flex-wrap items-center gap-3">{actions}</div>}
      </div>
    </header>
  );
}

type BackTarget = { to: string; label: string; state?: unknown; onBack?: () => void };

export function BackLink({ to, label, state, onBack, className }: BackTarget & { className?: string }) {
  return (
    <Link
      to={to}
      state={state}
      onClick={(e) => {
        if (!onBack || e.metaKey || e.ctrlKey || e.shiftKey || e.button !== 0) return;
        e.preventDefault();
        onBack();
      }}
      className={cx("inline-flex items-center gap-2 font-mono text-xs tracking-widest text-muted uppercase hover:text-ink", className)}
    >
      <ArrowLeft className="size-4" />[{label}]
    </Link>
  );
}

export function TotalBar({ label, sub, value, suffix }: { label: ReactNode; sub?: ReactNode; value: ReactNode; suffix?: string }) {
  return (
    <div className="flex flex-col gap-2 rounded-b-[14px] border-t-2 border-navy bg-navy px-5 py-5 text-white sm:flex-row sm:items-center sm:justify-between sm:px-6 sm:py-6">
      <div>
        <p className="font-mono text-[11px] font-bold tracking-[0.14em] text-amber uppercase">[{label}]</p>
        {sub && <p className="mt-1 text-sm text-white/85">{sub}</p>}
      </div>
      <p className="tabular text-3xl font-extrabold tracking-tight text-amber sm:text-[40px]">
        {value}
        {suffix && <span className="ml-1.5 font-mono text-sm font-medium text-white/80">{suffix}</span>}
      </p>
    </div>
  );
}

export function TableHead({ children, className }: { children: ReactNode; className?: string }) {
  return (
    <div className={cx("hidden border-b-2 border-navy bg-paper-2 px-6 py-3 font-mono text-[11px] tracking-[0.12em] text-muted uppercase md:grid rounded-t-[14px]", className)}>
      {children}
    </div>
  );
}

type PillTone = "green" | "amber" | "blue" | "gray" | "red" | "navy";

const pillTones: Record<PillTone, string> = {
  green: "bg-green-soft text-green",
  amber: "bg-amber-soft text-[#8a5300]",
  blue: "bg-blue-soft text-blue",
  gray: "bg-[#e6eaf1] text-muted",
  red: "bg-red-soft text-red",
  navy: "bg-navy text-white",
};

export function Pill({ tone, icon: Icon, children, className }: { tone: PillTone; icon?: LucideIcon; children: ReactNode; className?: string }) {
  return (
    <span className={cx("inline-flex items-center gap-1 rounded-full px-2.5 py-1 text-xs font-bold whitespace-nowrap", pillTones[tone], className)}>
      {Icon && <Icon className="size-3.5" strokeWidth={2.5} />}
      {children}
    </span>
  );
}

const statusMap: Record<string, { tone: PillTone; icon: LucideIcon; label: string }> = {
  "charge:paid": { tone: "green", icon: Check, label: "Pago" },
  "charge:pending": { tone: "gray", icon: Clock, label: "A vencer" },
  "charge:late": { tone: "amber", icon: TriangleAlert, label: "Em atraso" },
  "charge:cancelled": { tone: "gray", icon: X, label: "Cancelada" },
  "invoice:paid": { tone: "green", icon: Check, label: "Paga" },
  "invoice:pending": { tone: "gray", icon: Clock, label: "A vencer" },
  "invoice:late": { tone: "amber", icon: TriangleAlert, label: "Em atraso" },
  "invoice:cancelled": { tone: "gray", icon: X, label: "Cancelada" },
  "client:active": { tone: "green", icon: Check, label: "Ativo" },
  "client:paused": { tone: "gray", icon: Pause, label: "Pausado" },
  "client:archived": { tone: "gray", icon: X, label: "Arquivado" },
  "client:late": { tone: "amber", icon: TriangleAlert, label: "Em atraso" },
  "account:active": { tone: "green", icon: Check, label: "Ativa" },
  "account:trial": { tone: "blue", icon: Star, label: "Em teste" },
  "account:late": { tone: "amber", icon: TriangleAlert, label: "Em atraso" },
  "account:suspended": { tone: "red", icon: TriangleAlert, label: "Suspensa" },
  "account:cancelled": { tone: "gray", icon: X, label: "Cancelada" },
};

export function StatusPill({ kind, status, className }: { kind: "charge" | "invoice" | "client" | "account"; status: string; className?: string }) {
  const s = statusMap[`${kind}:${status}`] ?? { tone: "gray" as PillTone, icon: Clock, label: status };
  return (
    <Pill tone={s.tone} icon={s.icon} className={className}>
      {s.label}
    </Pill>
  );
}

export function GroupAvatar({ name, color, size = "md" }: { name: string; color: string | null; size?: "sm" | "md" | "lg" }) {
  const bg = groupColor(color);
  const dims = size === "lg" ? "size-16 text-2xl rounded-2xl shadow-hard-sm" : size === "sm" ? "size-9 text-xs rounded-lg" : "size-[52px] text-base rounded-xl";
  return (
    <span
      className={cx("inline-flex shrink-0 items-center justify-center border-2 border-navy font-mono font-bold", dims)}
      style={{ background: bg, color: textOn(bg) }}
      aria-hidden="true"
    >
      {initials(name)}
    </span>
  );
}

export function GroupChip({ name, color }: { name: string; color: string | null }) {
  return (
    <span className="inline-flex max-w-full items-center gap-1.5 rounded-full border-2 border-navy bg-white px-2.5 py-0.5 text-xs font-semibold whitespace-nowrap">
      <span className="size-3 shrink-0 rounded-[3px] border border-navy" style={{ background: groupColor(color) }} />
      <span className="truncate">{name}</span>
    </span>
  );
}

export function MonthPicker({ month, onChange, className }: { month: string; onChange: (delta: number) => void; className?: string }) {
  return (
    <div className={cx("inline-flex h-11 items-center justify-between gap-2 rounded-[10px] border-2 border-navy bg-white px-1.5", className)}>
      <button type="button" onClick={() => onChange(-1)} className="grid size-8 place-items-center rounded-md hover:bg-paper" aria-label="Mês anterior">
        <ChevronLeft className="size-4" strokeWidth={2.5} />
      </button>
      <span className="min-w-[140px] text-center text-[15px] font-bold" aria-live="polite">
        {monthLabel(month)}
      </span>
      <button type="button" onClick={() => onChange(1)} className="grid size-8 place-items-center rounded-md hover:bg-paper" aria-label="Próximo mês">
        <ChevronRight className="size-4" strokeWidth={2.5} />
      </button>
    </div>
  );
}

export function FilterChips<T extends string>({
  options,
  value,
  onChange,
}: {
  options: { value: T; label: string; count?: number; color?: string | null }[];
  value: T;
  onChange: (v: T) => void;
}) {
  return (
    <div className="no-scrollbar -mx-4 flex gap-2.5 overflow-x-auto px-4 pb-1 sm:mx-0 sm:flex-wrap sm:px-0" role="tablist">
      {options.map((o) => {
        const active = o.value === value;
        return (
          <button
            key={o.value}
            type="button"
            role="tab"
            aria-selected={active}
            onClick={() => onChange(o.value)}
            className={cx(
              "inline-flex h-10 shrink-0 items-center gap-2 rounded-full border-2 border-navy px-4 text-sm font-bold whitespace-nowrap transition",
              active ? "bg-navy text-white" : "bg-white hover:bg-paper-2",
            )}
          >
            {o.label}
            {o.count !== undefined && <span className={cx("font-mono text-xs font-medium", active ? "text-white/80" : "text-muted")}>{o.count}</span>}
          </button>
        );
      })}
    </div>
  );
}

const inputBase =
  "w-full rounded-[10px] border-2 border-navy bg-white px-4 text-[15px] text-ink placeholder:text-[#8792a8] transition focus:outline-none focus:ring-3 focus:ring-blue/30 disabled:bg-paper disabled:text-muted";

export function Field({
  label,
  required,
  error,
  hint,
  children,
  htmlFor,
  aside,
  className,
}: {
  label: ReactNode;
  required?: boolean;
  error?: string;
  hint?: ReactNode;
  children: ReactNode;
  htmlFor?: string;
  aside?: ReactNode;
  className?: string;
}) {
  return (
    <div className={className}>
      <div className="mb-1.5 flex items-center justify-between gap-2">
        <label htmlFor={htmlFor} className="text-sm font-semibold">
          {label}
          {required && <span className="ml-0.5 text-blue">*</span>}
        </label>
        {aside}
      </div>
      {children}
      {error ? (
        <p className="mt-1.5 text-[13px] font-medium text-red" role="alert">
          {error}
        </p>
      ) : (
        hint && <p className="mt-1.5 text-[13px] text-muted">{hint}</p>
      )}
    </div>
  );
}

export function Input({ className, invalid, mono, ...rest }: InputHTMLAttributes<HTMLInputElement> & { invalid?: boolean; mono?: boolean }) {
  return (
    <input
      className={cx(inputBase, "h-12", invalid && "border-red ring-2 ring-red/20", mono && "font-mono text-sm", className)}
      aria-invalid={invalid || undefined}
      {...rest}
    />
  );
}

export function Textarea({ className, invalid, ...rest }: TextareaHTMLAttributes<HTMLTextAreaElement> & { invalid?: boolean }) {
  return <textarea className={cx(inputBase, "min-h-28 py-3", invalid && "border-red", className)} aria-invalid={invalid || undefined} {...rest} />;
}

export function Select({ className, invalid, children, ...rest }: SelectHTMLAttributes<HTMLSelectElement> & { invalid?: boolean }) {
  return (
    <select className={cx(inputBase, "h-12 appearance-none bg-[url('data:image/svg+xml;utf8,<svg xmlns=%22http://www.w3.org/2000/svg%22 width=%2216%22 height=%2216%22 fill=%22none%22 stroke=%22%23111b35%22 stroke-width=%222.5%22 viewBox=%220 0 24 24%22><path d=%22m6 9 6 6 6-6%22/></svg>')] bg-[right_14px_center] bg-no-repeat pr-10", invalid && "border-red", className)} aria-invalid={invalid || undefined} {...rest}>
      {children}
    </select>
  );
}

export function Checkbox({ label, checked, onChange, className, children }: { label?: ReactNode; checked: boolean; onChange: (v: boolean) => void; className?: string; children?: ReactNode }) {
  const id = useId();
  return (
    <label htmlFor={id} className={cx("flex cursor-pointer items-start gap-3 text-[15px]", className)}>
      <input id={id} type="checkbox" checked={checked} onChange={(e) => onChange(e.target.checked)} className="mt-0.5 size-5 shrink-0 cursor-pointer accent-blue" />
      <span>{label ?? children}</span>
    </label>
  );
}

export function OptionCard({
  type,
  checked,
  onChange,
  title,
  description,
  name,
  className,
}: {
  type: "radio" | "checkbox";
  checked: boolean;
  onChange: () => void;
  title: ReactNode;
  description?: ReactNode;
  name?: string;
  className?: string;
}) {
  const id = useId();
  return (
    <label
      htmlFor={id}
      className={cx(
        "flex cursor-pointer items-start gap-3.5 rounded-xl border-2 border-navy px-4 py-3.5 transition sm:px-5 sm:py-4",
        checked ? "bg-blue-soft" : "bg-white hover:bg-paper-2",
        className,
      )}
    >
      <input id={id} type={type} name={name} checked={checked} onChange={onChange} className="mt-1 size-5 shrink-0 cursor-pointer accent-blue" />
      <span>
        <span className="block text-base font-bold">{title}</span>
        {description && <span className="mt-0.5 block text-sm text-muted">{description}</span>}
      </span>
    </label>
  );
}

export function FormSection({
  step,
  stepColor = "#2553E6",
  title,
  description,
  children,
}: {
  step?: number;
  stepColor?: string;
  title: string;
  description?: string;
  children: ReactNode;
}) {
  return (
    <Card className="p-5 sm:p-7">
      <div className="mb-5 flex items-start gap-3.5">
        {step !== undefined && (
          <span
            className="grid size-11 shrink-0 place-items-center rounded-xl border-2 border-navy font-bold"
            style={{ background: stepColor, color: textOn(stepColor) }}
          >
            {step}
          </span>
        )}
        <div>
          <h2 className="text-xl font-bold">{title}</h2>
          {description && <p className="text-sm text-muted">{description}</p>}
        </div>
      </div>
      {children}
    </Card>
  );
}

export function FormError({ message }: { message?: string | null }) {
  if (!message) return null;
  return (
    <div role="alert" className="flex items-start gap-2.5 rounded-xl border-2 border-red bg-red-soft px-4 py-3 text-sm font-semibold text-red">
      <TriangleAlert className="mt-0.5 size-4 shrink-0" />
      {message}
    </div>
  );
}

export function Spinner({ className }: { className?: string }) {
  return <LoaderCircle className={cx("size-6 animate-spin text-blue", className)} aria-label="Carregando" />;
}

export function Loading({ label = "Carregando…" }: { label?: string }) {
  return (
    <div className="flex items-center justify-center gap-3 py-20 text-muted">
      <Spinner />
      <span className="font-mono text-sm">{label}</span>
    </div>
  );
}

export function ErrorState({ message, onRetry }: { message: string; onRetry?: () => void }) {
  return (
    <Card className="flex flex-col items-center gap-4 px-6 py-12 text-center">
      <TriangleAlert className="size-8 text-amber" />
      <p className="max-w-md font-semibold">{message}</p>
      {onRetry && (
        <Button onClick={onRetry} size="sm">
          Tentar de novo
        </Button>
      )}
    </Card>
  );
}

export function EmptyState({ icon: Icon, title, description, action }: { icon: LucideIcon; title: string; description?: ReactNode; action?: ReactNode }) {
  return (
    <div className="flex flex-col items-center gap-3 px-6 py-14 text-center">
      <span className="grid size-14 place-items-center rounded-2xl border-2 border-navy bg-blue-soft text-blue">
        <Icon className="size-6" />
      </span>
      <p className="text-lg font-bold">{title}</p>
      {description && <p className="max-w-md text-sm text-muted">{description}</p>}
      {action && <div className="mt-2">{action}</div>}
    </div>
  );
}

export function Modal({
  open,
  onClose,
  title,
  children,
  footer,
}: {
  open: boolean;
  onClose: () => void;
  title: string;
  children: ReactNode;
  footer?: ReactNode;
}) {
  const ref = useRef<HTMLDivElement>(null);
  useEffect(() => {
    if (!open) return;
    const prev = document.activeElement as HTMLElement | null;
    const onKey = (e: KeyboardEvent) => e.key === "Escape" && onClose();
    document.addEventListener("keydown", onKey);
    document.body.style.overflow = "hidden";
    ref.current?.querySelector<HTMLElement>("input, select, textarea, button:not([data-close])")?.focus();
    return () => {
      document.removeEventListener("keydown", onKey);
      document.body.style.overflow = "";
      prev?.focus();
    };
  }, [open, onClose]);

  if (!open) return null;
  return createPortal(
    <div className="fixed inset-0 z-50 flex items-end justify-center bg-navy/60 p-0 sm:items-center sm:p-6" onMouseDown={(e) => e.target === e.currentTarget && onClose()}>
      <div
        ref={ref}
        role="dialog"
        aria-modal="true"
        aria-label={title}
        className="max-h-[92dvh] w-full overflow-y-auto rounded-t-2xl border-2 border-navy bg-white shadow-hard sm:max-w-lg sm:rounded-2xl"
      >
        <div className="flex items-center justify-between gap-4 border-b-2 border-navy px-5 py-4 sm:px-6">
          <h2 className="text-lg font-bold">{title}</h2>
          <button type="button" data-close onClick={onClose} className="grid size-9 place-items-center rounded-lg hover:bg-paper" aria-label="Fechar">
            <X className="size-5" />
          </button>
        </div>
        <div className="px-5 py-5 sm:px-6">{children}</div>
        {footer && <div className="flex flex-col-reverse gap-3 border-t-2 border-line px-5 py-4 sm:flex-row sm:justify-end sm:px-6">{footer}</div>}
      </div>
    </div>,
    document.body,
  );
}

export function Logo({ tagline = "Suíte Parlandim TI", dark = true, admin }: { tagline?: string; dark?: boolean; admin?: boolean }) {
  return (
    <span className="inline-flex items-center gap-3">
      <img src="/favicon.svg" alt="" className="size-10 rounded-[11px]" />
      <span className="leading-none">
        <span className={cx("block text-[21px] font-extrabold tracking-tight", dark ? "text-white" : "text-ink")}>Carteirama</span>
        {admin ? (
          <span className="mt-1 inline-block rounded bg-amber px-1.5 py-0.5 font-mono text-[10px] font-bold tracking-widest text-navy">ADMIN</span>
        ) : (
          <span className={cx("mt-1 block font-mono text-[10px] tracking-[0.14em] uppercase", dark ? "text-white/70" : "text-muted")}>{tagline}</span>
        )}
      </span>
    </span>
  );
}

export function GithubIcon({ className }: { className?: string }) {
  return (
    <svg viewBox="0 0 24 24" fill="currentColor" className={className} aria-hidden="true">
      <path d="M12 .5a11.5 11.5 0 0 0-3.64 22.41c.58.1.79-.25.79-.56v-2c-3.2.7-3.88-1.37-3.88-1.37-.52-1.33-1.28-1.69-1.28-1.69-1.05-.71.08-.7.08-.7 1.16.08 1.77 1.19 1.77 1.19 1.03 1.77 2.7 1.26 3.36.96.1-.75.4-1.26.73-1.55-2.55-.29-5.24-1.28-5.24-5.7 0-1.26.45-2.29 1.19-3.1-.12-.29-.52-1.46.11-3.05 0 0 .97-.31 3.17 1.18a11 11 0 0 1 5.78 0c2.2-1.49 3.17-1.18 3.17-1.18.63 1.59.23 2.76.11 3.05.74.81 1.19 1.84 1.19 3.1 0 4.43-2.7 5.4-5.26 5.69.41.36.78 1.06.78 2.14v3.17c0 .31.21.67.8.56A11.5 11.5 0 0 0 12 .5Z" />
    </svg>
  );
}
