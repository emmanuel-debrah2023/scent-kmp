-- Daily count of model calls made by the ask route, so it can stop calling the model once a
-- user or the whole app reaches its limit.
--
-- scope is 'user:<id>' or 'global'; day is the UTC date. A row is created on the first call
-- of the day and only ever counted up. Old rows are not pruned yet (they are a few bytes each).

CREATE TABLE public.ask_quota_usage (
    scope varchar(32) NOT NULL,
    day date NOT NULL,
    calls integer DEFAULT 0 NOT NULL,
    CONSTRAINT pk_ask_quota_usage PRIMARY KEY (scope, day)
);
