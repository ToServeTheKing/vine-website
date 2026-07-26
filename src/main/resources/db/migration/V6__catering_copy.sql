-- The catering tables in the bakery's voice instead of the spreadsheet's shorthand.
--
-- V4 kept the source wording deliberately: it was a spreadsheet written for the people who bake from it,
-- and inventing copy for somebody else's prices is not a migration's job. But the page is read by
-- customers, and "B&G cake", "2 pans", "4 dz" and "6+6" are notes-to-self, not an offer. So this rewrites
-- the cells as sentences, fills in the blurb each table always had room for, and merges the parties
-- lines the same way V5 merged the wedding ones — "Cupcakes" quantified as "1 dz sugar cookies or
-- cupcakes" with an empty "Sugar cookies" line beneath it is the same offset, and the same fix.
--
-- Guarded on the exact text V4 and V5 left, as V5 was: anything the bakery has already reworded in the
-- admin is left exactly as they wrote it. Their wording wins over mine.

-- --- names and the line under each heading ----------------------------------------------------------

update catering_package set name = 'Office boxes'
 where name = 'Office';

update catering_package set blurb = 'Mini pastries for a morning meeting, boxed and ready to collect.'
 where name = 'Office boxes' and blurb is null;

update catering_package set blurb = 'A cake, and something to hand round, sized to your guest list.'
 where name = 'Parties' and blurb is null;

update catering_package set blurb = 'A cake for the couple, and dessert for everyone else.'
 where name = 'Weddings' and blurb is null;

-- --- office boxes -----------------------------------------------------------------------------------

update catering_row_value v set value = 'A dozen'
  from catering_row r where r.id = v.row_id and r.label = 'Mini muffins' and v.value = '12 items';
update catering_row_value v set value = 'Eighteen'
  from catering_row r where r.id = v.row_id and r.label = 'Mini muffins' and v.value = '18 items';
update catering_row_value v set value = 'Two dozen'
  from catering_row r where r.id = v.row_id and r.label = 'Mini muffins' and v.value = '24 items';

-- "6+6" is six of one flavour and six of another, which is worth saying out loud.
update catering_row_value v set value = 'A dozen, in two flavors'
  from catering_row r where r.id = v.row_id and r.label = 'Mini scones' and v.value = '6+6';
update catering_row_value v set value = 'Eighteen, in two or three flavors'
  from catering_row r where r.id = v.row_id and r.label = 'Mini scones' and v.value = '6+6+6 or 12+6';
update catering_row_value v set value = 'Two dozen, in up to four flavors'
  from catering_row r where r.id = v.row_id and r.label = 'Mini scones'
   and v.value = '6+6+6+6 or 12+6+6 or 12+12';

update catering_package_note set body = 'Everything is baked in sixes, so each item comes in multiples of six.'
 where body = 'Minimum of 6 items per baked good. Flavors can''t be mixed and matched unless you''re ordering a large quantity.';

insert into catering_package_note (package_id, position, body)
select p.id, 1, 'Mixing flavors within one item needs a larger order — ask us and we will tell you.'
  from catering_package p
 where p.name = 'Office boxes'
   and not exists (select 1 from catering_package_note n where n.package_id = p.id and n.position = 1);

-- --- parties ----------------------------------------------------------------------------------------

update catering_row_value v set value = 'A 6-inch cake'
  from catering_row r where r.id = v.row_id and r.label = 'Cake' and v.value = '6 in cake';
update catering_row_value v set value = 'An 8-inch cake'
  from catering_row r where r.id = v.row_id and r.label = 'Cake' and v.value = '8 in cake';
update catering_row_value v set value = 'A 10-inch cake'
  from catering_row r where r.id = v.row_id and r.label = 'Cake' and v.value = '10 in cake';

-- Same offset as the wedding table: the quantities on the "Cupcakes" line were always
-- cupcakes-or-sugar-cookies, and the "Sugar cookies" line below carried nothing at all.
update catering_row r set label = 'Cupcakes or sugar cookies'
  from catering_package p
 where p.id = r.package_id and p.name = 'Parties' and r.label = 'Cupcakes';

update catering_row_value v set value = 'A dozen, either one'
  from catering_row r where r.id = v.row_id and r.label = 'Cupcakes or sugar cookies'
   and v.value = '1 dz sugar cookies or cupcakes';
update catering_row_value v set value = 'Eighteen, your choice'
  from catering_row r where r.id = v.row_id and r.label = 'Cupcakes or sugar cookies'
   and v.value = '1.5 dz your choice';
update catering_row_value v set value = 'Two dozen, your choice'
  from catering_row r where r.id = v.row_id and r.label = 'Cupcakes or sugar cookies'
   and v.value = '2 dz your choice';

delete from catering_row r
 using catering_package p
 where p.id = r.package_id and p.name = 'Parties' and r.label = 'Sugar cookies'
   and not exists (select 1 from catering_row_value v where v.row_id = r.id and v.value <> '');

update catering_package_note set body = 'An extra dozen is $20.'
 where body = 'Add an extra dozen for $20.';
update catering_package_note
   set body = 'Cake and cupcake flavors can be different once you are ordering a dozen cupcakes or more.'
 where body = 'Cake and cupcake flavors can''t be mixed unless you order at least 1 dz of cupcakes.';

-- --- weddings ---------------------------------------------------------------------------------------

-- The label said it and the cell repeated it. The cell now carries the size instead.
update catering_row r set label = 'Bride & groom cake'
  from catering_package p
 where p.id = r.package_id and p.name = 'Weddings' and r.label = 'Bride & groom cake (8 in)';

update catering_row_value v set value = 'Eight inch'
  from catering_row r where r.id = v.row_id and r.label = 'Bride & groom cake' and v.value = 'B&G cake';

update catering_row r set label = 'Sheet cakes or 12×17 bars'
  from catering_package p
 where p.id = r.package_id and p.name = 'Weddings' and r.label = 'Sheet cakes or 12x17 bars';

update catering_row_value v set value = 'Two pans'
  from catering_row r where r.id = v.row_id and r.label = 'Sheet cakes or 12×17 bars' and v.value = '2 pans';
update catering_row_value v set value = 'Three pans'
  from catering_row r where r.id = v.row_id and r.label = 'Sheet cakes or 12×17 bars' and v.value = '3 pans';
update catering_row_value v set value = 'Four pans'
  from catering_row r where r.id = v.row_id and r.label = 'Sheet cakes or 12×17 bars' and v.value = '4 pans';

update catering_row_value v set value = 'Four dozen'
  from catering_row r where r.id = v.row_id and r.label = 'Cupcakes or sugar cookies' and v.value = '4 dz';
update catering_row_value v set value = 'Five dozen'
  from catering_row r where r.id = v.row_id and r.label = 'Cupcakes or sugar cookies' and v.value = '5 dz';
update catering_row_value v set value = 'Six dozen'
  from catering_row r where r.id = v.row_id and r.label = 'Cupcakes or sugar cookies' and v.value = '6 dz';

-- --- the page's own terms ---------------------------------------------------------------------------

update catering_note
   set body = 'These are a guide rather than a menu. We are happy to make changes, though the price may change with them.'
 where body = 'We''re happy to make changes — the price may change with them.';

update catering_note
   set body = 'If we can''t do something, we will tell you.'
 where body = 'If we can''t do something we''ll tell you. These tables are mostly here to give you an idea of what''s possible.';
