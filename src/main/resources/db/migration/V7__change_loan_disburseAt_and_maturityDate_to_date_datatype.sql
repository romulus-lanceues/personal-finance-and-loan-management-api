ALTER TABLE loans
ALTER COLUMN disbursed_at TYPE DATE USING(disbursed_at AT TIME ZONE 'UTC')::date,
ALTER COLUMN maturity_date TYPE DATE USING(maturity_date AT TIME ZONE 'UTC')::date;