create index offers_name_description_idx on offers
    using gin (to_tsvector('russian', name || ' ' || description));
