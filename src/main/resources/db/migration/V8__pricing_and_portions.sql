-- The prices and portions the bakery settled on after costing every package properly.
--
-- V4 seeded the spreadsheet. This is the first migration that changes what things COST rather than how
-- they read, and it exists because the tables were priced before anyone put a cost against them. Working
-- back from the bakery's own goods-plus-labour figures gave a model that reproduces every one of them to
-- the cent -- $15.00 an hour, mini pastry $0.4167, cupcake $0.6667, a 12x17 pan $18.00, cakes $7.00 /
-- $10.50 / $12.50 -- and that model is what the numbers below come out of.
--
-- Two facts from the bakery drive nearly all of it:
--
--   1. A PAN CUTS 48. Portions were being judged on the cupcakes alone, which are barely a third of the
--      food. Counting the pans is what showed the weddings were short.
--   2. THE BRIDE & GROOM CAKE IS CEREMONIAL. It is cut for the couple, not served to the room, so it
--      feeds nobody -- it stays in every wedding package and contributes zero portions.
--
-- Against a planning rate of 1.25 servings a guest, five of the nine live columns promised more than they
-- held. All three weddings were short (the 200 by 37 servings), and the two smaller parties were over-
-- labelled. The fix is deliberately asymmetric: the WEDDINGS get more food so the familiar 125 / 200 / 250
-- can stand, because that is the number a customer says out loud; the PARTIES get a corrected label,
-- because giving the middle one more food would have taken its cupcakes to two dozen -- the same as the
-- tier above it -- leaving the two $2 apart in cost with only cake size between them.
--
-- Guarded on the exact text and prices V4-V7 left, as every migration here has been: anything the bakery
-- has already changed in the admin is theirs and is left alone. Their wording and their prices win.

-- --- office boxes ------------------------------------------------------------------------------------
--
-- No price moves. At 1.25 a head these boxes feed 9.6 / 14.4 / 19.2, so the "About 6-8 people" figures
-- are conservative rather than optimistic and are left as they are. What was missing is that the ladder
-- has a rule -- $24 for a dozen, then a flat $8 for each extra six -- which the three cards never said.

insert into catering_package_note (package_id, position, body)
select p.id, 2, 'A dozen is $24, and every extra six is $8.'
  from catering_package p
 where p.name = 'Office boxes'
   and not exists (select 1 from catering_package_note n where n.package_id = p.id and n.position = 2);

-- --- parties -----------------------------------------------------------------------------------------
--
-- The 15-20 was labelled for twenty and held eighteen servings. Half a dozen more cupcakes fixes that for
-- $4.00 of goods, and the price goes to $57 -- which is not an attempt to make it pay. It is a deliberate
-- loss leader at $4.00 an hour of labour, the same floor as the Small office box, and 40% would have been
-- $81.67. The 20-30 keeps its food and its $76 and is simply relabelled to the 27 it actually feeds;
-- at $88 it would have been a column nobody could rationally buy, since the $98 beside it feeds fourteen
-- more people. The 30-40 already fed 42 and is untouched.

update catering_tier t set price_cents = 5700, updated_at = now()
  from catering_package p
 where p.id = t.package_id and p.name = 'Parties'
   and t.label = '15–20 people' and t.price_cents = 5400;

update catering_row_value v set value = 'Eighteen, your choice'
  from catering_row r
  join catering_package p on p.id = r.package_id
 where r.id = v.row_id and p.name = 'Parties'
   and r.label = 'Cupcakes or sugar cookies'
   and v.position = 0 and v.value = 'A dozen, either one';

update catering_tier t set label = '20–27 people', updated_at = now()
  from catering_package p
 where p.id = t.package_id and p.name = 'Parties'
   and t.label = '20–30 people';

-- --- weddings ----------------------------------------------------------------------------------------
--
-- Every column gains food and every column gains price, and the headcounts do not move.
--
-- PORTIONS. With the bride & groom cake feeding nobody, the columns held 144 / 204 / 264 servings against
-- the 156 / 250 / 313 that 1.25 a guest asks for. A dozen onto the first and a pan onto the other two
-- lands them at 156 / 252 / 312 -- 1.248 / 1.260 / 1.248 a head, which is as even as this table has ever
-- been.
--
-- PRICE. The old figures assumed four hours of labour; six is the honest number, and it reconciles with
-- the three hours a Gatherings order takes (the 125 wedding is the same two pans, two and a half times
-- the cupcakes, and a decorated cake, for twice the hours). At six hours the old prices returned $11.92
-- an hour on the 125 -- less than a Large office box. A flat 20% across all three restores 40.8 / 42.9 /
-- 48.5% and still asks only $1.48-$1.91 a serving, which is about half the low end of what wedding
-- dessert usually costs. The ladder's slope was already right and is left alone: markup is a steady
-- 1.69-1.94x on cost per serving, because six fixed hours and one $10.50 cake spread over more servings.

update catering_row_value v set value = 'Five dozen'
  from catering_row r
  join catering_package p on p.id = r.package_id
 where r.id = v.row_id and p.name = 'Weddings'
   and r.label = 'Cupcakes or sugar cookies'
   and v.position = 0 and v.value = 'Four dozen';

-- Position is in the guard as well as the value: after the first of these runs, two columns read
-- "Four pans", and only the column index tells them apart.
update catering_row_value v set value = 'Five pans'
  from catering_row r
  join catering_package p on p.id = r.package_id
 where r.id = v.row_id and p.name = 'Weddings'
   and r.label = 'Sheet cakes or 12×17 bars'
   and v.position = 2 and v.value = 'Four pans';

update catering_row_value v set value = 'Four pans'
  from catering_row r
  join catering_package p on p.id = r.package_id
 where r.id = v.row_id and p.name = 'Weddings'
   and r.label = 'Sheet cakes or 12×17 bars'
   and v.position = 1 and v.value = 'Three pans';

update catering_tier t set price_cents = 29800, updated_at = now()
  from catering_package p
 where p.id = t.package_id and p.name = 'Weddings'
   and t.label = '125 people' and t.price_cents = 23600;

update catering_tier t set price_cents = 37200, updated_at = now()
  from catering_package p
 where p.id = t.package_id and p.name = 'Weddings'
   and t.label = '200 people' and t.price_cents = 31000;

update catering_tier t set price_cents = 46300, updated_at = now()
  from catering_package p
 where p.id = t.package_id and p.name = 'Weddings'
   and t.label = '250 people' and t.price_cents = 38600;

-- A wedding is booked months out and is the largest order the shop takes; it was the only one with no
-- deposit at all.
insert into catering_package_note (package_id, position, body)
select p.id, 2, 'A $50 deposit books the date, and the balance is due on delivery.'
  from catering_package p
 where p.name = 'Weddings'
   and not exists (select 1 from catering_package_note n where n.package_id = p.id and n.position = 2);

-- The old wedding note predates there being a delivery policy at all, so it promised a conversation the
-- page can now just answer.
update catering_package_note
   set body = 'Delivery is $10 in Princeville and $30 elsewhere in Peoria County. Setup is charged separately.'
 where body = 'The delivery fee depends on where the wedding is, and setup is charged separately.';

-- --- gatherings --------------------------------------------------------------------------------------
--
-- A new table, because the page went from "30-40 people" straight to "125" and a sixty-person graduation
-- or a hundred-person church supper found nothing on it. The bakery confirmed those enquiries do come in.
--
-- It is the wedding table's shape without the bride & groom cake, and it is sized from the 48-unit pan
-- rather than guessed: 72 and 120 servings, which at 1.25 a head is 57.6 and 96.0 people. The labels
-- round DOWN to 55 and 95 -- with a number nobody has promised before, under is the safe direction.
-- Two hours of labour on the first and three on the second, which is what the larger one really takes.

update catering_package set position = 4, updated_at = now()
 where name = 'Weddings' and position = 3;

insert into catering_package (name, blurb, position, created_at)
select 'Gatherings', 'Dessert for a graduation, a shower or a supper — sized by the head count.', 3, now()
 where not exists (select 1 from catering_package where name = 'Gatherings');

insert into catering_tier (package_id, label, price_cents, position, created_at)
select p.id, 'About 55 people', 11000, 1, now()
  from catering_package p
 where p.name = 'Gatherings'
   and not exists (select 1 from catering_tier t where t.package_id = p.id and t.position = 1);

insert into catering_tier (package_id, label, price_cents, position, created_at)
select p.id, 'About 95 people', 17000, 2, now()
  from catering_package p
 where p.name = 'Gatherings'
   and not exists (select 1 from catering_tier t where t.package_id = p.id and t.position = 2);

insert into catering_row (package_id, label, position, created_at)
select p.id, 'Sheet cakes or 12×17 bars', 1, now()
  from catering_package p
 where p.name = 'Gatherings'
   and not exists (select 1 from catering_row r where r.package_id = p.id and r.position = 1);

insert into catering_row (package_id, label, position, created_at)
select p.id, 'Cupcakes or sugar cookies', 2, now()
  from catering_package p
 where p.name = 'Gatherings'
   and not exists (select 1 from catering_row r where r.package_id = p.id and r.position = 2);

insert into catering_row_value (row_id, position, value)
select r.id, 0, 'One pan'
  from catering_row r
  join catering_package p on p.id = r.package_id
 where p.name = 'Gatherings' and r.label = 'Sheet cakes or 12×17 bars'
   and not exists (select 1 from catering_row_value v where v.row_id = r.id and v.position = 0);

insert into catering_row_value (row_id, position, value)
select r.id, 1, 'Two pans'
  from catering_row r
  join catering_package p on p.id = r.package_id
 where p.name = 'Gatherings' and r.label = 'Sheet cakes or 12×17 bars'
   and not exists (select 1 from catering_row_value v where v.row_id = r.id and v.position = 1);

insert into catering_row_value (row_id, position, value)
select r.id, 0, 'Two dozen, your choice'
  from catering_row r
  join catering_package p on p.id = r.package_id
 where p.name = 'Gatherings' and r.label = 'Cupcakes or sugar cookies'
   and not exists (select 1 from catering_row_value v where v.row_id = r.id and v.position = 0);

insert into catering_row_value (row_id, position, value)
select r.id, 1, 'Two dozen, your choice'
  from catering_row r
  join catering_package p on p.id = r.package_id
 where p.name = 'Gatherings' and r.label = 'Cupcakes or sugar cookies'
   and not exists (select 1 from catering_row_value v where v.row_id = r.id and v.position = 1);

insert into catering_package_note (package_id, position, body)
select p.id, 0, 'Bars travel better than cake, so they are what we suggest for anything outdoors.'
  from catering_package p
 where p.name = 'Gatherings'
   and not exists (select 1 from catering_package_note n where n.package_id = p.id and n.position = 0);

-- --- the page's own terms ----------------------------------------------------------------------------
--
-- Delivery has never been stated anywhere on this page for anything but weddings, and for a box bought
-- FOR an office that is close to the whole product. The $40 floor is there because the hour of labour is
-- charged per order rather than per box: a single Small collected returns $4.00 for that hour, while two
-- on one order return $23.00.

insert into catering_note (body, position, created_at)
select 'Delivery is $10 in Princeville and $30 anywhere else in Peoria County, on orders of $40 or more. We do not deliver outside the county, but you are always welcome to collect.', 3, now()
 where not exists (select 1 from catering_note where position = 3);
