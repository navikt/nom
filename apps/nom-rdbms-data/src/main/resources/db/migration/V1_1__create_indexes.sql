CREATE INDEX ressurs_personident_index ON ressurs (personident);
CREATE INDEX ressurs_navident_index ON ressurs (navident);
CREATE INDEX ressurs_primary_fysisk_brukersted_index ON ressurs (primary_fysisk_brukersted);

CREATE INDEX orgenhet_id_index ON orgenhet (id);
CREATE INDEX orgenhet_unique_agresso_id_index ON orgenhet (unique_agresso_id);
CREATE INDEX orgenhet_agresso_id_index ON orgenhet (agresso_id);

CREATE INDEX organisering_id_index ON organisering (id);
CREATE INDEX organisering_id_over_index ON organisering (id_over);

CREATE INDEX orgtilknytning_navident_index ON orgtilknytning (navident);
CREATE INDEX orgtilknytning_orgenhet_id_index ON orgtilknytning (orgenhet_id);

CREATE INDEX leder_tilgang_til_index ON leder_tilgang (til);
CREATE INDEX leder_tilgang_av_index ON leder_tilgang (av);
CREATE INDEX leder_tilgang_orgenhet_id_index ON leder_tilgang (orgenhet_id);