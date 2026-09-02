-- Tighten listings.kind column to NOT NULL with backfill
--
-- This migration demonstrates non-trivial schema changes:
-- 1. Backfill NULL values with a sensible default
-- 2. Add NOT NULL constraint
-- 3. Document assumptions
--
-- Assumption: existing NULL rows are edge cases or test data.
-- Production listings always have a kind (SEALED/OPENED/DECANT/TESTER).

-- Backfill any NULL kind values with a default.
-- Treat NULL as SEALED (the most common case).
UPDATE public.listings
SET kind = 'SEALED'
WHERE kind IS NULL;

-- Now that all rows have values, add the NOT NULL constraint.
ALTER TABLE public.listings
ALTER COLUMN kind SET NOT NULL;

-- For future reference: this pattern applies to other fill columns
-- (nominal_size_ml, remaining_ml, fill_source, fill_confidence) once
-- the business logic solidifies around their semantics.
