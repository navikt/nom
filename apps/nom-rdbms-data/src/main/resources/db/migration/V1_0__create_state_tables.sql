CREATE TABLE ressurs (
    fid                 text primary key,
    personident         text,
    navident            text,
    sektor              text,
    fornavn             text,
    mellomnavn          text,
    etternavn           text,
    visningsnavn        text,
    primary_telefonnummer text,
    startdato           date,
    sluttdato           date,
    primary_fysisk_brukersted text,
    fysiske_brukersteder text[],
    ident_type          text
);

CREATE TABLE orgenhet (
    fid                 text primary key,
    id                  text,
    navn                text,
    gyldig_fom          date,
    gyldig_tom          date,
    orgenhet_type       text,
    nom_nivaa           text,
    unique_agresso_id   text,
    agresso_id          text,
    agresso_org_nivaa   text,
    agresso_orgenhet_type text
);

CREATE TABLE organisering (
    fid                 text primary key,
    id                  text,
    navn                text,
    id_over             text,
    navn_over           text,
    gyldig_fom          date,
    gyldig_tom          date
);

CREATE TABLE orgtilknytning (
    fid                 text primary key,
    navident            text,
    orgenhet_id         text,
    sektor              text,
    er_daglig_oppfolging boolean,
    gyldig_fom          date,
    gyldig_tom          date,
    orgtilknytning_type text
);

CREATE TABLE mastered_ressurs (
    navident            text primary key,
    since               date
);

CREATE TABLE leder_tilgang (
    fid                 text primary key,
    til                 text,
    av                  text,
    orgenhet_id         text,
    gyldig_fom          date,
    gyldig_tom          date
);
