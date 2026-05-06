ALTER TABLE users
    ADD COLUMN phone_number VARCHAR(64),
    ADD COLUMN language VARCHAR(8),
    ADD COLUMN bot_blocked BOOLEAN NOT NULL DEFAULT FALSE;

UPDATE users
SET language = 'UZ'
WHERE language IS NULL;

ALTER TABLE users
    ALTER COLUMN language SET DEFAULT 'UZ';
