CREATE TABLE delayed_event (
    delayed_event_id    uuid    primary key,
    delayed_until       date    not null,
    event               jsonb   not null
);