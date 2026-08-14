-- Baseline of the account database schema as it already exists.
--
-- This file describes the live schema; it is not a change. The migrate workflow
-- runs Flyway with -baselineOnMigrate=true -baselineVersion=1, so an existing
-- database records version 1 as already applied and never executes this file.
-- Running it against a populated database would fail on the first CREATE TABLE.
--
-- Source: schema-only pg_dump of the account database (PostgreSQL 14.24).
-- Ownership, ACLs, psql meta commands and the session SET preamble are stripped;
-- every table, sequence and constraint is kept.
--
-- The pre-Flyway hand-maintained DDL journal this replaces is kept, unexecuted,
-- at archive/schema.sql.

--
-- Tables
--

CREATE TABLE public.principal (
    id bigint NOT NULL
);

CREATE TABLE public.member (
    id bigint NOT NULL,
    principal_id bigint,
    email character varying(100) NOT NULL,
    password_hash character(60) NOT NULL,
    name character varying(31)
);

CREATE TABLE public.system (
    id bigint NOT NULL,
    principal_id bigint,
    name character varying(31) NOT NULL,
    created_by bigint
);

CREATE TABLE public.authority (
    id bigint NOT NULL,
    value character varying(31) NOT NULL,
    system_id bigint
);

CREATE TABLE public.member_authority (
    id bigint NOT NULL,
    member_id bigint,
    authority_id bigint
);

CREATE TABLE public.key_value (
    id bigint NOT NULL,
    key character varying(31),
    value character varying(31),
    member_id bigint,
    system_id bigint
);

--
-- Sequences
--

CREATE SEQUENCE public.principal_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE public.principal_id_seq OWNED BY public.principal.id;

CREATE SEQUENCE public.member_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE public.member_id_seq OWNED BY public.member.id;

CREATE SEQUENCE public.system_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE public.system_id_seq OWNED BY public.system.id;

CREATE SEQUENCE public.authority_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE public.authority_id_seq OWNED BY public.authority.id;

CREATE SEQUENCE public.member_authority_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE public.member_authority_id_seq OWNED BY public.member_authority.id;

CREATE SEQUENCE public.key_value_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE public.key_value_id_seq OWNED BY public.key_value.id;

--
-- Column defaults
--

ALTER TABLE ONLY public.principal ALTER COLUMN id SET DEFAULT nextval('public.principal_id_seq'::regclass);

ALTER TABLE ONLY public.member ALTER COLUMN id SET DEFAULT nextval('public.member_id_seq'::regclass);

ALTER TABLE ONLY public.system ALTER COLUMN id SET DEFAULT nextval('public.system_id_seq'::regclass);

ALTER TABLE ONLY public.authority ALTER COLUMN id SET DEFAULT nextval('public.authority_id_seq'::regclass);

ALTER TABLE ONLY public.member_authority ALTER COLUMN id SET DEFAULT nextval('public.member_authority_id_seq'::regclass);

ALTER TABLE ONLY public.key_value ALTER COLUMN id SET DEFAULT nextval('public.key_value_id_seq'::regclass);

--
-- Primary keys and unique constraints
--

ALTER TABLE ONLY public.principal
    ADD CONSTRAINT principal_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.member
    ADD CONSTRAINT member_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.member
    ADD CONSTRAINT member_email_key UNIQUE (email);

ALTER TABLE ONLY public.system
    ADD CONSTRAINT system_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.system
    ADD CONSTRAINT system_name_key UNIQUE (name);

ALTER TABLE ONLY public.authority
    ADD CONSTRAINT authority_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.authority
    ADD CONSTRAINT uq_authority_value_system_id UNIQUE (value, system_id);

ALTER TABLE ONLY public.member_authority
    ADD CONSTRAINT member_authority_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.member_authority
    ADD CONSTRAINT uq_member_authority_member_id_authority_id UNIQUE (member_id, authority_id);

ALTER TABLE ONLY public.key_value
    ADD CONSTRAINT key_value_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.key_value
    ADD CONSTRAINT uq_key_value_member_id_system_id_key UNIQUE (member_id, system_id, key);

--
-- Foreign keys
--

ALTER TABLE ONLY public.member
    ADD CONSTRAINT member_principal_id_fkey FOREIGN KEY (principal_id) REFERENCES public.principal(id);

ALTER TABLE ONLY public.system
    ADD CONSTRAINT system_principal_id_fkey FOREIGN KEY (principal_id) REFERENCES public.principal(id);

ALTER TABLE ONLY public.system
    ADD CONSTRAINT system_created_by_fkey FOREIGN KEY (created_by) REFERENCES public.member(id);

ALTER TABLE ONLY public.authority
    ADD CONSTRAINT authority_system_id_fkey FOREIGN KEY (system_id) REFERENCES public.system(id);

ALTER TABLE ONLY public.member_authority
    ADD CONSTRAINT member_authority_member_id_fkey FOREIGN KEY (member_id) REFERENCES public.member(id);

ALTER TABLE ONLY public.member_authority
    ADD CONSTRAINT member_authority_authority_id_fkey FOREIGN KEY (authority_id) REFERENCES public.authority(id);

ALTER TABLE ONLY public.key_value
    ADD CONSTRAINT key_value_member_id_fkey FOREIGN KEY (member_id) REFERENCES public.member(id);

ALTER TABLE ONLY public.key_value
    ADD CONSTRAINT key_value_system_id_fkey FOREIGN KEY (system_id) REFERENCES public.system(id);
