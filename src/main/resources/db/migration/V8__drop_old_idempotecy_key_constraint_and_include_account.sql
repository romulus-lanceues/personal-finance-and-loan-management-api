ALTER TABLE transactions
    DROP CONSTRAINT uke58eyb3fj0t2cl16q1ll436b7,
    ADD CONSTRAINT unique_idempotency_key_and_account_id UNIQUE (idempotency_key, account_id),
    ADD COLUMN loan_schedule_id UUID REFERENCES loan_schedules(id),
    ADD COLUMN loan_status_after_payment VARCHAR(255);
