CREATE TABLE users
(
    id            UUID PRIMARY KEY NOT NULL,
    login         VARCHAR(64) UNIQUE,
    password_hash VARCHAR(64) NOT NULL,
    password_salt VARCHAR(64) NOT NULL,
    email         VARCHAR(64),
    phone         VARCHAR(32),
    status        VARCHAR(32) NOT NULL,
    registered_at TIMESTAMP WITH TIME ZONE NOT NULL
)
