ALTER TABLE stock_intelligence_recipients
  ADD COLUMN receive_evening_report TINYINT(1) NOT NULL DEFAULT 1 AFTER receive_morning_report;
