CREATE INDEX IF NOT EXISTS idx_activity_date ON activity (date);

CREATE INDEX IF NOT EXISTS idx_activity_activity_type ON activity (activity_type);