ALTER TABLE orgtilknytning
    ADD COLUMN primary_fysisk_brukersted_nummer         text,
    ADD COLUMN additional_fysiske_brukersteder_nummer   text[];