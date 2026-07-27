-- Goodie boxes and catering: the Office / Parties / Weddings price tables the bakery hands out.
--
-- These arrived as a spreadsheet, and a spreadsheet is what they are, so that is what this models:
-- a PACKAGE is one table on the page, its TIERS are the columns (what you get, for a price) and its
-- ROWS are the lines (which baked good, and how much of it in each column). A row therefore holds
-- one value per column, in column order — the two are kept in step in Java, because a table whose
-- lines and columns disagree quietly misprices what a customer is actually buying.
--
-- All of it is data rather than markup so the prices can move without a deploy. They will: the last
-- line of the spreadsheet says the tables are "mostly just an idea for people".
create table catering_package (
    id          bigserial    primary key,
    name        varchar(120) not null,
    -- Optional sentence under the heading. Nothing in the spreadsheet fills this in; it exists so
    -- the bakery can explain a table without one of us editing a page.
    blurb       varchar(400),
    position    integer      not null,
    created_at  timestamptz  not null,
    updated_at  timestamptz
);

-- Footnotes belonging to one table: the minimums, the flavor rules, the wedding delivery terms.
-- Kept as rows rather than one blob so each rule can be edited, reordered or dropped on its own.
create table catering_package_note (
    package_id  bigint       not null references catering_package (id) on delete cascade,
    position    integer      not null,
    body        varchar(600) not null,
    primary key (package_id, position)
);

-- A column: the size, and what it costs. price_cents is nullable for a tier that is priced on
-- asking, and is cents rather than a formatted string so the app — not the browser — decides how
-- money is written.
create table catering_tier (
    id          bigserial    primary key,
    package_id  bigint       not null references catering_package (id) on delete cascade,
    label       varchar(120) not null,
    price_cents integer,
    position    integer      not null,
    created_at  timestamptz  not null,
    updated_at  timestamptz
);

create index catering_tier_package_idx on catering_tier (package_id, position);

-- A line of the table: which baked good.
create table catering_row (
    id          bigserial    primary key,
    package_id  bigint       not null references catering_package (id) on delete cascade,
    label       varchar(200) not null,
    position    integer      not null,
    created_at  timestamptz  not null,
    updated_at  timestamptz
);

create index catering_row_package_idx on catering_row (package_id, position);

-- One cell. `position` is the COLUMN — position 0 is the first tier, and so on — so a row always has
-- exactly as many values as its package has tiers. Empty strings are meaningful and expected: the
-- spreadsheet has lines that are named but not yet quantified.
create table catering_row_value (
    row_id      bigint       not null references catering_row (id) on delete cascade,
    position    integer      not null,
    value       varchar(300) not null,
    primary key (row_id, position)
);

-- Footnotes for the page as a whole rather than any one table.
create table catering_note (
    id          bigserial    primary key,
    body        varchar(600) not null,
    position    integer      not null,
    created_at  timestamptz  not null,
    updated_at  timestamptz
);

-- Seeded from the bakery's own spreadsheet. Wording is theirs; the only changes are expanded
-- shorthand ("4 dz cc or sc" -> "4 dz cupcakes or sugar cookies") and fixed typos, because these
-- lines are read by customers. Everything here is editable in the admin.
insert into catering_package (id, name, position, created_at) values
    (1, 'Office',   1, now()),
    (2, 'Parties',  2, now()),
    (3, 'Weddings', 3, now());

insert into catering_tier (id, package_id, label, price_cents, position, created_at) values
    (1, 1, 'Small',          2400, 1, now()),
    (2, 1, 'Medium',         3200, 2, now()),
    (3, 1, 'Large',          4000, 3, now()),
    (4, 2, '15–20 people',   5400, 1, now()),
    (5, 2, '20–30 people',   7600, 2, now()),
    (6, 2, '30–40 people',   9800, 3, now()),
    (7, 3, '125 people',    23600, 1, now()),
    (8, 3, '200 people',    31000, 2, now()),
    (9, 3, '250 people',    38600, 3, now());

insert into catering_row (id, package_id, label, position, created_at) values
    ( 1, 1, 'Mini muffins',                1, now()),
    ( 2, 1, 'Mini scones',                 2, now()),
    ( 3, 1, 'Mini cinnamon rolls',         3, now()),
    ( 4, 2, 'Cake',                        1, now()),
    ( 5, 2, 'Cupcakes',                    2, now()),
    ( 6, 2, 'Sugar cookies',               3, now()),
    ( 7, 3, 'Bride & groom cake (8 in)',   1, now()),
    ( 8, 3, 'Sheet cakes',                 2, now()),
    ( 9, 3, '12x17 bars',                  3, now()),
    (10, 3, 'Cupcakes',                    4, now()),
    (11, 3, 'Sugar cookies',               5, now());

insert into catering_row_value (row_id, position, value) values
    ( 1, 0, '12 items'), ( 1, 1, '18 items'), ( 1, 2, '24 items'),
    ( 2, 0, '6+6'),      ( 2, 1, '6+6+6 or 12+6'), ( 2, 2, '6+6+6+6 or 12+6+6 or 12+12'),
    -- Named in the spreadsheet but never quantified; the minimum below is the only rule it gives.
    ( 3, 0, ''),         ( 3, 1, ''),         ( 3, 2, ''),
    ( 4, 0, '6 in cake'), ( 4, 1, '8 in cake'), ( 4, 2, '10 in cake'),
    ( 5, 0, '1 dz sugar cookies or cupcakes'), ( 5, 1, '1.5 dz your choice'), ( 5, 2, '2 dz your choice'),
    ( 6, 0, ''),         ( 6, 1, ''),         ( 6, 2, ''),
    ( 7, 0, 'B&G cake'), ( 7, 1, 'B&G cake'), ( 7, 2, 'B&G cake'),
    ( 8, 0, '2 pans or a sheet cake'), ( 8, 1, '3 pans or a sheet cake'), ( 8, 2, '4 pans or a sheet cake'),
    ( 9, 0, '4 dz cupcakes or sugar cookies'),
    ( 9, 1, '5 dz cupcakes or sugar cookies'),
    ( 9, 2, '6 dz cupcakes or sugar cookies'),
    (10, 0, ''),         (10, 1, ''),         (10, 2, ''),
    (11, 0, ''),         (11, 1, ''),         (11, 2, '');

insert into catering_package_note (package_id, position, body) values
    (1, 0, 'Minimum of 6 items per baked good. Flavors can''t be mixed and matched unless you''re ordering a large quantity.'),
    (2, 0, 'Add an extra dozen for $20.'),
    (2, 1, 'Cake and cupcake flavors can''t be mixed unless you order at least 1 dz of cupcakes.'),
    -- Wedding-specific, so it sits with the wedding table rather than under the whole page.
    (3, 0, 'The delivery fee depends on where the wedding is, and setup is charged separately.'),
    (3, 1, 'We don''t provide serving materials, and we don''t set up decorations.');

insert into catering_note (id, body, position, created_at) values
    (1, 'We''re happy to make changes — the price may change with them.', 1, now()),
    (2, 'If we can''t do something we''ll tell you. These tables are mostly here to give you an idea of what''s possible.', 2, now());

-- bigserial keeps its own counter; move it past the seeded ids so future inserts don't collide.
select setval('catering_package_id_seq', 3);
select setval('catering_tier_id_seq', 9);
select setval('catering_row_id_seq', 11);
select setval('catering_note_id_seq', 2);
