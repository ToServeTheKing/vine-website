-- The product catalogue. Lives in the database rather than a TypeScript array so the
-- catalogue, its ordering and its category filter are server-side concerns like any other
-- Spring app — the SPA just renders what /api/products returns.
create table product (
    id          bigserial primary key,
    name        varchar(200) not null,
    category    varchar(60)  not null,
    position    integer      not null,
    created_at  timestamptz  not null,
    updated_at  timestamptz
);

create index product_category_idx on product (category, position);

-- Ordered photos for a product; the first is the one the card shows.
create table product_image (
    product_id  bigint      not null references product (id) on delete cascade,
    position    integer     not null,
    image_key   varchar(300) not null,
    primary key (product_id, position)
);

-- Seeded from the catalogue the site shipped with; `position` preserves the original order.
insert into product (id, name, category, position, created_at) values (1, '76th Birthday Cake', 'Cakes', 1, now());
insert into product (id, name, category, position, created_at) values (2, '1964 Graduates Cake', 'Cakes', 2, now());
insert into product (id, name, category, position, created_at) values (3, 'Baby Shower Cake', 'Cakes', 3, now());
insert into product (id, name, category, position, created_at) values (4, 'Blueberry Cream Pie', 'Pie', 4, now());
insert into product (id, name, category, position, created_at) values (5, 'Bridesmaids Sugar Cookies', 'Cookies', 5, now());
insert into product (id, name, category, position, created_at) values (6, 'Bundt Cake', 'Cakes', 6, now());
insert into product (id, name, category, position, created_at) values (7, 'Caramel Rolls', 'Rolls', 7, now());
insert into product (id, name, category, position, created_at) values (8, 'Cat Birthday Cake', 'Cakes', 8, now());
insert into product (id, name, category, position, created_at) values (9, 'Chocolate Chip Scones', 'Pastries', 9, now());
insert into product (id, name, category, position, created_at) values (10, 'Christmas Sugar Cookies', 'Cookies', 10, now());
insert into product (id, name, category, position, created_at) values (11, 'Circus Birthday Cake', 'Cakes', 11, now());
insert into product (id, name, category, position, created_at) values (12, 'Cookie Bars', 'Cookies', 12, now());
insert into product (id, name, category, position, created_at) values (13, 'Cinnamon Rolls', 'Rolls', 13, now());
insert into product (id, name, category, position, created_at) values (14, 'Cow Birthday Cake', 'Cakes', 14, now());
insert into product (id, name, category, position, created_at) values (15, 'Cow Cupcakes', 'Cakes', 15, now());
insert into product (id, name, category, position, created_at) values (16, 'Doggy Sugar Cookies', 'Cookies', 16, now());
insert into product (id, name, category, position, created_at) values (17, 'Fall Sugar Cookies', 'Cookies', 17, now());
insert into product (id, name, category, position, created_at) values (18, 'Flower Cupcakes', 'Cakes', 18, now());
insert into product (id, name, category, position, created_at) values (19, 'Heart Cakes', 'Cakes', 19, now());
insert into product (id, name, category, position, created_at) values (20, 'Lemon Berry Cake', 'Cakes', 20, now());
insert into product (id, name, category, position, created_at) values (21, 'Macarons', 'Pastries', 21, now());
insert into product (id, name, category, position, created_at) values (22, 'Moana Birthday Cake', 'Cakes', 22, now());
insert into product (id, name, category, position, created_at) values (23, 'Natalie Purple Birthday Cake', 'Cakes', 23, now());
insert into product (id, name, category, position, created_at) values (24, 'Oreo Brownies', 'Brownies', 24, now());
insert into product (id, name, category, position, created_at) values (25, 'Peanut Butter Cookie Cake', 'Cakes', 25, now());
insert into product (id, name, category, position, created_at) values (26, 'Pink Rose Birthday Cake', 'Cakes', 26, now());
insert into product (id, name, category, position, created_at) values (27, 'Princeville Sugar Cookies', 'Cookies', 27, now());
insert into product (id, name, category, position, created_at) values (28, 'Princeville XC Sugar Cookies', 'Cookies', 28, now());
insert into product (id, name, category, position, created_at) values (29, 'Pumpkin Birthday Cake', 'Cakes', 29, now());
insert into product (id, name, category, position, created_at) values (30, 'Purple Birthday Cake', 'Cakes', 30, now());
insert into product (id, name, category, position, created_at) values (31, 'Rainbow Sugar Cookies', 'Cookies', 31, now());
insert into product (id, name, category, position, created_at) values (32, 'Retirement Cake', 'Cakes', 32, now());
insert into product (id, name, category, position, created_at) values (33, 'Scones', 'Pastries', 33, now());
insert into product (id, name, category, position, created_at) values (34, 'Soccer Sugar Cookies', 'Cookies', 34, now());
insert into product (id, name, category, position, created_at) values (35, 'Speciality Cookies', 'Cookies', 35, now());
-- 'Pies' in the original data, which no filter button matched — so this one was unreachable unless you
-- were browsing "All". Filed under 'Pie' with the other one.
insert into product (id, name, category, position, created_at) values (36, 'Strawberry Pie', 'Pie', 36, now());
insert into product (id, name, category, position, created_at) values (37, 'Timecapsul Sugar Cookies', 'Cookies', 37, now());
insert into product (id, name, category, position, created_at) values (38, 'Tractor Birthday Cake', 'Cakes', 38, now());
insert into product (id, name, category, position, created_at) values (39, 'Valentines Cookie Cakes', 'Cakes', 39, now());
insert into product (id, name, category, position, created_at) values (40, 'Yellow Wedding Cake', 'Cakes', 40, now());

insert into product_image (product_id, position, image_key) values (1, 0, 'products/76th_birthday_cake.webp');
insert into product_image (product_id, position, image_key) values (1, 1, 'products/76th_birthday_cake2.webp');
insert into product_image (product_id, position, image_key) values (1, 2, 'products/76th_birthday_cake3.webp');
insert into product_image (product_id, position, image_key) values (2, 0, 'products/1964_graduates_cake.webp');
insert into product_image (product_id, position, image_key) values (2, 1, 'products/1964_graduates_cake2.webp');
insert into product_image (product_id, position, image_key) values (2, 2, 'products/1964_graduates_cake3.webp');
insert into product_image (product_id, position, image_key) values (3, 0, 'products/babyshower_cake.webp');
insert into product_image (product_id, position, image_key) values (4, 0, 'products/blueberry_cream_pie.webp');
insert into product_image (product_id, position, image_key) values (5, 0, 'products/bridesmaids_sugar_cookies.webp');
insert into product_image (product_id, position, image_key) values (5, 1, 'products/bridesmaids_sugar_cookies2.webp');
insert into product_image (product_id, position, image_key) values (6, 0, 'products/bundt_cake.webp');
insert into product_image (product_id, position, image_key) values (7, 0, 'products/carmel_rolls.webp');
insert into product_image (product_id, position, image_key) values (8, 0, 'products/cat_birthday_cake.webp');
insert into product_image (product_id, position, image_key) values (9, 0, 'products/ChocalateChip_Scones.webp');
insert into product_image (product_id, position, image_key) values (10, 0, 'products/christmas_sugar_cookies.webp');
insert into product_image (product_id, position, image_key) values (10, 1, 'products/christmas_sugar_cookies2.webp');
insert into product_image (product_id, position, image_key) values (11, 0, 'products/circus_birthday_cake.webp');
insert into product_image (product_id, position, image_key) values (11, 1, 'products/circus_birthday_cake2.webp');
insert into product_image (product_id, position, image_key) values (12, 0, 'products/cookie_bars.webp');
insert into product_image (product_id, position, image_key) values (12, 1, 'products/cookie_bars2.webp');
insert into product_image (product_id, position, image_key) values (13, 0, 'products/cinnamonrolls.webp');
insert into product_image (product_id, position, image_key) values (14, 0, 'products/cow_birthday_cake.webp');
insert into product_image (product_id, position, image_key) values (14, 1, 'products/cow_birthday_cake2.webp');
insert into product_image (product_id, position, image_key) values (15, 0, 'products/cow_cupcakes.webp');
insert into product_image (product_id, position, image_key) values (16, 0, 'products/doggy_sugar_cookies.webp');
insert into product_image (product_id, position, image_key) values (16, 1, 'products/doggy_sugar_cookies2.webp');
insert into product_image (product_id, position, image_key) values (16, 2, 'products/doggy_sugar_cookies3.webp');
insert into product_image (product_id, position, image_key) values (17, 0, 'products/fall_sugar_cookies.webp');
insert into product_image (product_id, position, image_key) values (17, 1, 'products/fall_sugar_cookies2.webp');
insert into product_image (product_id, position, image_key) values (18, 0, 'products/flower_cupcakes.webp');
insert into product_image (product_id, position, image_key) values (19, 0, 'products/heart_cakes.webp');
insert into product_image (product_id, position, image_key) values (19, 1, 'products/heart_cakes2.webp');
insert into product_image (product_id, position, image_key) values (19, 2, 'products/heart_cakes3.webp');
insert into product_image (product_id, position, image_key) values (19, 3, 'products/heart_cakes4.webp');
insert into product_image (product_id, position, image_key) values (19, 4, 'products/heart_cakes5.webp');
insert into product_image (product_id, position, image_key) values (19, 5, 'products/heart_cakes6.webp');
insert into product_image (product_id, position, image_key) values (20, 0, 'products/lemon_berry_cake.webp');
insert into product_image (product_id, position, image_key) values (20, 1, 'products/lemon_berry_cake2.webp');
insert into product_image (product_id, position, image_key) values (20, 2, 'products/lemon_berry_cake3.webp');
insert into product_image (product_id, position, image_key) values (20, 3, 'products/lemon_berry_cake4.webp');
insert into product_image (product_id, position, image_key) values (20, 4, 'products/lemon_berry_cake5.webp');
insert into product_image (product_id, position, image_key) values (21, 0, 'products/macarons.webp');
insert into product_image (product_id, position, image_key) values (22, 0, 'products/moana_birthday_cake.webp');
insert into product_image (product_id, position, image_key) values (22, 1, 'products/moana_birthday_cake2.webp');
insert into product_image (product_id, position, image_key) values (23, 0, 'products/natalie_purple_birthday_cake.webp');
insert into product_image (product_id, position, image_key) values (23, 1, 'products/natalie_purple_birthday_cake2.webp');
insert into product_image (product_id, position, image_key) values (23, 2, 'products/natalie_purple_birthday_cake3.webp');
insert into product_image (product_id, position, image_key) values (24, 0, 'products/oreo_brownies.webp');
insert into product_image (product_id, position, image_key) values (25, 0, 'products/peanutbutter_cookie_cake.webp');
insert into product_image (product_id, position, image_key) values (25, 1, 'products/peanutbutter_cookie_cake2.webp');
insert into product_image (product_id, position, image_key) values (26, 0, 'products/pink_rose_birthday_cake.webp');
insert into product_image (product_id, position, image_key) values (27, 0, 'products/princeville_sugar_cookies.webp');
insert into product_image (product_id, position, image_key) values (27, 1, 'products/princeville_sugar_cookies2.webp');
insert into product_image (product_id, position, image_key) values (27, 2, 'products/princeville_sugar_cookies3.webp');
insert into product_image (product_id, position, image_key) values (27, 3, 'products/princeville_sugar_cookies4.webp');
insert into product_image (product_id, position, image_key) values (27, 4, 'products/princeville_sugar_cookies5.webp');
insert into product_image (product_id, position, image_key) values (28, 0, 'products/princeville_xc_sugar_cookies.webp');
insert into product_image (product_id, position, image_key) values (28, 1, 'products/princeville_xc_sugar_cookies2.webp');
insert into product_image (product_id, position, image_key) values (29, 0, 'products/pumpkin_birthday_cake.webp');
insert into product_image (product_id, position, image_key) values (29, 1, 'products/pumpkin_birthday_cake2.webp');
insert into product_image (product_id, position, image_key) values (29, 2, 'products/pumpkin_birthday_cake3.webp');
insert into product_image (product_id, position, image_key) values (30, 0, 'products/purple_birthday_cake.webp');
insert into product_image (product_id, position, image_key) values (31, 0, 'products/rainbow_sugar_cookies.webp');
insert into product_image (product_id, position, image_key) values (31, 1, 'products/rainbow_sugar_cookies2.webp');
insert into product_image (product_id, position, image_key) values (32, 0, 'products/retirement_cake.webp');
insert into product_image (product_id, position, image_key) values (33, 0, 'products/scones.webp');
insert into product_image (product_id, position, image_key) values (34, 0, 'products/soccer_sugar_cookies.webp');
insert into product_image (product_id, position, image_key) values (34, 1, 'products/soccer_sugar_cookies2.webp');
insert into product_image (product_id, position, image_key) values (35, 0, 'products/speciality_cookies.webp');
insert into product_image (product_id, position, image_key) values (36, 0, 'products/strawberry_pie.webp');
insert into product_image (product_id, position, image_key) values (36, 1, 'products/strawberry_pie2.webp');
insert into product_image (product_id, position, image_key) values (36, 2, 'products/strawberry_pie3.webp');
insert into product_image (product_id, position, image_key) values (37, 0, 'products/timecapsul_sugar_cookies.webp');
insert into product_image (product_id, position, image_key) values (38, 0, 'products/tractor_birthday_cake.webp');
insert into product_image (product_id, position, image_key) values (39, 0, 'products/valentines_cookie_cakes.webp');
insert into product_image (product_id, position, image_key) values (39, 1, 'products/valentines_cookie_cakes2.webp');
insert into product_image (product_id, position, image_key) values (39, 2, 'products/valentines_cookie_cakes3.webp');
insert into product_image (product_id, position, image_key) values (40, 0, 'products/yellow_wedding_cake.webp');
insert into product_image (product_id, position, image_key) values (40, 1, 'products/yellow_wedding_cake2.webp');
insert into product_image (product_id, position, image_key) values (40, 2, 'products/yellow_wedding_cake3.webp');

-- bigserial keeps its own counter; move it past the seeded ids so future inserts don't collide.
select setval('product_id_seq', 40);

