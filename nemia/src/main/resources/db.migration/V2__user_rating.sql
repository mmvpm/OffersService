CREATE TABLE user_rating
(
    from_user_id UUID NOT NULL,
    to_user_id   UUID NOT NULL,
    mark         INT2 NOT NULL,
    CONSTRAINT from_user_id_key FOREIGN KEY (from_user_id) REFERENCES users(id),
    CONSTRAINT to_user_id_key FOREIGN KEY (to_user_id) REFERENCES users(id)
)
