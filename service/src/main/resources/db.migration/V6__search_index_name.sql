create index offers_name_idx on offers
    using gin (to_tsvector('russian', name));
