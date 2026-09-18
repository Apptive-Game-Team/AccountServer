-- Refresh tokens with rotation and reuse detection.
--
-- The server never stores the token itself, only the SHA-256 hex of it, so a dump of this
-- table cannot be replayed against the API.
--
-- family_id groups every token descended from one login. Rotation issues a successor in the
-- same family and stamps rotated_at on the spent row; a spent row presented again outside the
-- retry grace means the token leaked, and the whole family is revoked in one update.

--
-- Tables
--

CREATE TABLE public.refresh_token (
    id bigint NOT NULL,
    family_id uuid NOT NULL,
    member_id bigint NOT NULL,
    token_hash character varying(64) NOT NULL,
    platform character varying(31),
    issued_at timestamptz NOT NULL,
    expires_at timestamptz NOT NULL,
    rotated_at timestamptz,
    revoked_at timestamptz,
    last_used_at timestamptz
);

--
-- Sequences
--

CREATE SEQUENCE public.refresh_token_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE public.refresh_token_id_seq OWNED BY public.refresh_token.id;

--
-- Column defaults
--

ALTER TABLE ONLY public.refresh_token ALTER COLUMN id SET DEFAULT nextval('public.refresh_token_id_seq'::regclass);

--
-- Primary keys and unique constraints
--

ALTER TABLE ONLY public.refresh_token
    ADD CONSTRAINT refresh_token_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.refresh_token
    ADD CONSTRAINT refresh_token_token_hash_key UNIQUE (token_hash);

--
-- Foreign keys
--

ALTER TABLE ONLY public.refresh_token
    ADD CONSTRAINT refresh_token_member_id_fkey FOREIGN KEY (member_id) REFERENCES public.member(id);

--
-- Indexes
--

CREATE INDEX idx_refresh_token_family_id ON public.refresh_token USING btree (family_id);

CREATE INDEX idx_refresh_token_member_id ON public.refresh_token USING btree (member_id);
