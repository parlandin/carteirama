/**
 * Dados usados nos termos de uso e na política de privacidade.
 * Preencha o e-mail de contato antes de publicar; sem ele, os textos apontam para o suporte.
 */
export const LEGAL = {
  company: "Parlandim TI",
  contactEmail: "contato@parlandim.dev.br" as string,
  updatedAt: "19/11/2026",
  cloudPrice: "R$ 24,99",
  trialDays: 7,
  suspendAfterDays: 5,
  retentionDays: 90,
};

export function contactText(): string {
  return LEGAL.contactEmail
    ? `pelo e-mail ${LEGAL.contactEmail}`
    : "pelos canais de suporte da Parlandim TI (WhatsApp e e-mail)";
}
