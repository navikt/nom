CREATE TABLE aggregate_state (
    aggregate_state_id  uuid                        primary key,
    created_at          timestamp with time zone    not null,
    delayed_until       date,
    navident            varchar(7)                  not null,
    masseflytting       boolean
)