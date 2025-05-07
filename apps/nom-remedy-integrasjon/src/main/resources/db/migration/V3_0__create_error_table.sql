CREATE TABLE error_state
(
    error_id         uuid primary key,
    error_state_fid  varchar(50)              not null,
    error_state_type varchar(50)              not null,
    error_message    varchar(1000),
    created_at       timestamp with time zone not null,
    navident         varchar(7)               not null
)