create table offers
(
    id          uuid primary key,
    name        text    not null,
    price       integer not null,
    description text    not null,
    status      text    not null
);

create table users
(
    id            uuid primary key,
    name          text not null,
    login         text not null unique,
    status        text not null,
    password_hash text not null,
    password_salt text not null
);

create table user_offers
(
    offer_id uuid primary key,
    user_id  uuid not null references users(id)
);
