-- Plano Cloud: novo preço padrão (R$ 24,99) e teste de 7 dias.
-- Vale para as contas criadas a partir de agora; assinaturas existentes mantêm o preço que já têm.
UPDATE plans SET price_cents = 2499, trial_days = 7 WHERE code = 'cloud';

ALTER TABLE plans ALTER COLUMN trial_days SET DEFAULT 7;
