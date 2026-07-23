-- The filter buttons were a hard-coded List.of(...) in ProductCatalog. That meant adding a category
-- was a deploy, and anything an editor invented appeared last, alphabetically, with no way to move
-- it. This makes the order data so the admin screens can arrange it.
--
-- product.category deliberately stays a varchar holding the name rather than becoming a foreign key:
-- every existing row, query and derived repository method keeps working untouched, and six rows of
-- reference data don't warrant rewriting the catalogue's shape. Renaming a category updates the
-- products alongside it, in one transaction.
create table category (
    id          bigserial    primary key,
    name        varchar(60)  not null unique,
    position    integer      not null,
    created_at  timestamptz  not null,
    updated_at  timestamptz
);

-- Seeded in the order the page has always shown them, so the site looks identical the moment this
-- lands. Categories found on products but missing here still appear on the filter (appended
-- alphabetically) rather than silently vanishing.
insert into category (name, position, created_at) values
    ('Cookies',  1, now()),
    ('Cakes',    2, now()),
    ('Rolls',    3, now()),
    ('Pie',      4, now()),
    ('Brownies', 5, now()),
    ('Pastries', 6, now());
