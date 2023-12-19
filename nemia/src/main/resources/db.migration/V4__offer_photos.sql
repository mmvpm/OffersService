CREATE TABLE offer_photos
(
    offer_id  UUID NOT NULL,
    photo_url TEXT NOT NULL,
    CONSTRAINT offer_id_key FOREIGN KEY (offer_id) REFERENCES offers (id)
)
