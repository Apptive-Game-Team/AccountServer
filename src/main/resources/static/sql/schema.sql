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

ALTER TABLE member ADD COLUMN name VARCHAR(31);

ALTER TABLE member ALTER COLUMN email TYPE VARCHAR(100);

CREATE TABLE key_value (
  id BIGSERIAL PRIMARY KEY,
  principal_id BIGINT REFERENCES principal(id),
  key VARCHAR(31),
  value VARCHAR(31)
);

ALTER TABLE key_value ADD CONSTRAINT uq_key_value_principal_id_key
UNIQUE (principal_id, key);

ALTER TABLE key_value DROP CONSTRAINT uq_key_value_principal_id_key;
ALTER TABLE key_value DROP COLUMN principal_id;
ALTER TABLE key_value ADD COLUMN member_id BIGINT REFERENCES member(id);
ALTER TABLE key_value ADD COLUMN system_id BIGINT REFERENCES system(id);
ALTER TABLE key_value ADD CONSTRAINT uq_key_value_member_id_system_id_key UNIQUE (member_id, system_id, key);

ALTER TABLE system ADD COLUMN created_by BIGINT REFERENCES member(id);

ALTER TABLE member_authority ADD CONSTRAINT uq_member_authority_member_id_authority_id UNIQUE (member_id, authority_id);

ALTER TABLE authority ADD COLUMN systemId BIGINT REFERENCES system(id);
ALTER TABLE authority DROP COLUMN systemId;
ALTER TABLE authority ADD COLUMN system_id BIGINT REFERENCES system(id);