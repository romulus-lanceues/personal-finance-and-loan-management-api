ALTER TABLE transactions
ADD COLUMN loan_id UUID,
ADD CONSTRAINT fk_transactions_loan
    FOREIGN KEY (loan_id)
    REFERENCES loans(id);


