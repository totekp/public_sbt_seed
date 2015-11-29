# users schema

# --- !Ups

CREATE TABLE users (
    id BIGSERIAL NOT NULL,
    username varchar(255) NOT NULL,
    created varchar(255) NOT NULL,
    hash varchar(255) NOT NULL,
    is_super_user boolean NOT NULL,
    PRIMARY KEY (id)
);

# --- !Downs

DROP TABLE users;