CREATE TABLE aggregate_state
(
    aggregate_state_id   uuid primary key,
    state_fid            varchar(50)              not null,
    state_type           varchar(50),
    masseflyttet_fra_fid varchar(50),
    created_at           timestamp with time zone not null,
    delayed_until        date,
    navident             varchar(7)               not null,
    masseflytting        boolean
)