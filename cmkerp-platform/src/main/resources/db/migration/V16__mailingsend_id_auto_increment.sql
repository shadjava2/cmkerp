-- Table mailingsend parfois créée manuellement sans AUTO_INCREMENT (V14 ignorée par IF NOT EXISTS)
ALTER TABLE mailingsend MODIFY COLUMN id BIGINT NOT NULL AUTO_INCREMENT;
