CREATE TABLE principal (
    id BIGSERIAL PRIMARY KEY
);

CREATE TABLE member (
    id BIGSERIAL PRIMARY KEY,
    principal_id BIGINT REFERENCES principal(id),
    email VARCHAR(31) UNIQUE NOT NULL,
    password_hash CHAR(60) NOT NULL
);

CREATE TABLE system (
    id BIGSERIAL PRIMARY KEY,
    principal_id BIGINT REFERENCES principal(id),
    name VARCHAR(31) UNIQUE NOT NULL
);

CREATE TABLE authority (
    id BIGSERIAL PRIMARY KEY,
    value VARCHAR(31) UNIQUE NOT NULL
);

CREATE TABLE member_authority (
    id BIGSERIAL PRIMARY KEY,
    member_id BIGINT REFERENCES member(id),
    authority_id BIGINT REFERENCES authority(id)
);