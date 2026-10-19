-- key_value.value is widened from varchar(31) to varchar(1000).
--
-- The column was sized for short settings such as a nickname or a flag. GyeMong and WordVenture
-- also store free-text player feedback under the key `feedback-{id}`, and any value longer than
-- 31 characters made PostgreSQL reject the INSERT or UPDATE, which the key-value PUT endpoint
-- surfaced as a 500.
--
-- Widening a varchar keeps every existing row as it is and needs no rewrite. The key column stays
-- varchar(31). The 1000 limit is also enforced by ValueRequest, so an oversized value is rejected
-- with 400 before it reaches the database; keep the two numbers in step.

--
-- Columns
--

ALTER TABLE ONLY public.key_value
    ALTER COLUMN value TYPE character varying(1000);
