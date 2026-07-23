-- Contact-form submissions. Written before delivery is attempted, so an SMTP outage costs a
-- notification rather than the enquiry; `delivered` marks the ones that still need chasing.
create table enquiry (
    id          bigserial primary key,
    name        varchar(200) not null,
    email       varchar(320) not null,
    message     text         not null,
    delivered   boolean      not null default false,
    created_at  timestamptz  not null,
    updated_at  timestamptz
);

-- The only query anyone actually runs: what came in, newest first.
create index enquiry_created_at_idx on enquiry (created_at desc);

-- Finding the ones the relay never took.
create index enquiry_undelivered_idx on enquiry (created_at desc) where not delivered;
