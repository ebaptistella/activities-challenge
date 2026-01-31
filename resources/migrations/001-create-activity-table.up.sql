CREATE TABLE IF NOT EXISTS activity (
  id bigserial PRIMARY KEY,
  date date NOT NULL,
  activity text NOT NULL,
  activity_type text NOT NULL,
  unit text NOT NULL,
  amount_planned numeric,
  amount_executed numeric,
  created_at timestamp with time zone DEFAULT now(),
  updated_at timestamp with time zone DEFAULT now()
);