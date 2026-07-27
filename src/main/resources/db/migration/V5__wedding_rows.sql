-- The wedding table's lines, read the way the spreadsheet meant them.
--
-- V4 carried that table over literally, because the source is offset: the prices sit on the "cupcakes"
-- line, and "4 dz cc or sc" sits on the "12x17 bars" line. Taken at face value it produced five lines,
-- two of which ("Cupcakes", "Sugar cookies") had no quantity in any column and rendered as a row of
-- dashes on the page.
--
-- Reading it as a baker would: a 12x17 pan IS the sheet pan, so "Sheet cakes" and "12x17 bars" are one
-- offering, and "cc or sc" is cupcakes-or-sugar-cookies, which is what the 4/5/6 dozen counts belong to.
-- That gives three lines that each say something:
--
--     Bride & groom cake (8 in)      B&G cake        B&G cake        B&G cake
--     Sheet cakes or 12x17 bars      2 pans          3 pans          4 pans
--     Cupcakes or sugar cookies      4 dz            5 dz            6 dz
--
-- This is an interpretation of somebody else's shorthand, so every statement below is guarded on the
-- text V4 wrote. If the bakery has already edited these lines in the admin, this migration finds nothing
-- to change and leaves their wording alone — which matters, because by the time this runs the tables are
-- live and theirs to edit.
--
-- V4 is deliberately NOT edited: it has already been applied, and Flyway validates checksums.

-- "Sheet cakes" absorbs the pan size, and its cells drop the "or a sheet cake" the label now carries.
update catering_row r
   set label = 'Sheet cakes or 12x17 bars'
  from catering_package p
 where p.id = r.package_id
   and p.name = 'Weddings'
   and r.label = 'Sheet cakes';

update catering_row_value v
   set value = '2 pans'
  from catering_row r join catering_package p on p.id = r.package_id
 where r.id = v.row_id and p.name = 'Weddings'
   and r.label = 'Sheet cakes or 12x17 bars'
   and v.position = 0 and v.value = '2 pans or a sheet cake';

update catering_row_value v
   set value = '3 pans'
  from catering_row r join catering_package p on p.id = r.package_id
 where r.id = v.row_id and p.name = 'Weddings'
   and r.label = 'Sheet cakes or 12x17 bars'
   and v.position = 1 and v.value = '3 pans or a sheet cake';

update catering_row_value v
   set value = '4 pans'
  from catering_row r join catering_package p on p.id = r.package_id
 where r.id = v.row_id and p.name = 'Weddings'
   and r.label = 'Sheet cakes or 12x17 bars'
   and v.position = 2 and v.value = '4 pans or a sheet cake';

-- The dozens were never about the bars; they are the cupcakes-or-sugar-cookies count. The line takes
-- that name, and the two empty lines below it are what it replaces.
update catering_row r
   set label = 'Cupcakes or sugar cookies'
  from catering_package p
 where p.id = r.package_id
   and p.name = 'Weddings'
   and r.label = '12x17 bars';

update catering_row_value v
   set value = '4 dz'
  from catering_row r join catering_package p on p.id = r.package_id
 where r.id = v.row_id and p.name = 'Weddings'
   and r.label = 'Cupcakes or sugar cookies'
   and v.position = 0 and v.value = '4 dz cupcakes or sugar cookies';

update catering_row_value v
   set value = '5 dz'
  from catering_row r join catering_package p on p.id = r.package_id
 where r.id = v.row_id and p.name = 'Weddings'
   and r.label = 'Cupcakes or sugar cookies'
   and v.position = 1 and v.value = '5 dz cupcakes or sugar cookies';

update catering_row_value v
   set value = '6 dz'
  from catering_row r join catering_package p on p.id = r.package_id
 where r.id = v.row_id and p.name = 'Weddings'
   and r.label = 'Cupcakes or sugar cookies'
   and v.position = 2 and v.value = '6 dz cupcakes or sugar cookies';

-- Only the wedding table's copies: "Cupcakes" is a real, quantified line in the parties table, which the
-- join on the package name protects. Their cells go with them (catering_row_value cascades).
delete from catering_row r
 using catering_package p
 where p.id = r.package_id
   and p.name = 'Weddings'
   and r.label in ('Cupcakes', 'Sugar cookies')
   and not exists (
         select 1 from catering_row_value v
          where v.row_id = r.id and v.value <> ''
       );

-- Positions 1, 2, 3 survive the deletion untouched (the removed lines were 4 and 5), so there is nothing
-- to renumber. Stated rather than assumed: the page reads the order from these.
