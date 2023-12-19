CREATE TABLE offers
(
    id         UUID PRIMARY KEY,
    user_id    UUID NOT NULL,
    name       TEXT NOT NULL,
    price      INT8 NOT NULL,
    text       TEXT NOT NULL,
    status     VARCHAR(32) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT user_id_key FOREIGN KEY (user_id) REFERENCES users(id)
)
