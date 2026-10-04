-- Keep users.follower_count in step with the follows table inside the database.
--
-- Doing the +1/-1 in the route (as post likes do) is a read-then-write race: two
-- concurrent requests can both see "not following" and double count. A row trigger
-- fires once per row actually inserted or deleted, so an ignored duplicate insert or
-- a delete that matches nothing never touches the counter. That is what makes the
-- PUT/DELETE follow endpoints idempotent.

CREATE OR REPLACE FUNCTION public.sync_follower_count() RETURNS trigger AS $$
BEGIN
    IF TG_OP = 'INSERT' THEN
        UPDATE public.users
        SET follower_count = follower_count + 1
        WHERE id = NEW.following_id;
    ELSIF TG_OP = 'DELETE' THEN
        UPDATE public.users
        SET follower_count = GREATEST(follower_count - 1, 0)
        WHERE id = OLD.following_id;
    END IF;
    RETURN NULL;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_follows_sync_follower_count
AFTER INSERT OR DELETE ON public.follows
FOR EACH ROW EXECUTE FUNCTION public.sync_follower_count();

-- Counters were never maintained before this migration; recompute from the source of truth.
UPDATE public.users u
SET follower_count = (SELECT COUNT(*) FROM public.follows f WHERE f.following_id = u.id);
