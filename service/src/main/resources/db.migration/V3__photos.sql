create table photos
(
    id   uuid primary key,
    url  text  default null,
    blob bytea default null
);

create table offer_photos
(
    photo_id uuid primary key references photos (id),
    offer_id uuid not null references offers (id)
);
