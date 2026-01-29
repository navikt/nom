CREATE TABLE orgenhet_leder (
    fid                 text primary key,
    navident            text,
    orgenhet_id         text,
    start_dato          date,
    slutt_dato          date,
    sist_endret_av      text
);

CREATE INDEX orgenhet_leder_navident_index ON orgenhet_leder (navident);
CREATE INDEX orgenhet_leder_orgenhet_id_index ON orgenhet_leder (orgenhet_id);
