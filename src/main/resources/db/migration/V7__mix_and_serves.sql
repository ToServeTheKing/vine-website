-- Two things the spreadsheet never said out loud, and the page was getting wrong.
--
-- 1. AN OFFICE BOX IS A TOTAL, NOT AN ASSORTMENT. A Small box is twelve items, mixed in sixes from the
--    minis — not twelve muffins AND twelve scones AND some cinnamon rolls, which is what three ticked
--    lines on a card claimed. The bakery confirmed it: mix them, in sixes, to the size's item count.
--
--    The fix keeps the shape everything else uses rather than inventing a mode for this one table: the
--    alternative goes INTO the line, the way "Cupcakes or sugar cookies" already does in the parties and
--    wedding tables. One line, "Any mix of…", and the size's cell carries the count. So the rule for the
--    whole page is now sayable in one sentence: lines are things you get (and), and a choice within a
--    line is written into its name (or).
--
-- 2. "SMALL" DOESN'T TELL ANYONE HOW MANY PEOPLE IT FEEDS. The party and wedding tables size themselves
--    by head count; the office table sizes itself by box. `serves` gives every column somewhere to say
--    it, and the admin gets a box for it. The office numbers below are estimates from the item counts and
--    are meant to be corrected by the people who pack the boxes.

alter table catering_tier add column serves varchar(80);

-- --- 1. the office box's contents ---------------------------------------------------------------------

update catering_row r
   set label = 'Any mix of mini muffins, mini scones and mini cinnamon rolls'
  from catering_package p
 where p.id = r.package_id
   and p.name = 'Office boxes'
   and r.label = 'Mini muffins';

update catering_row_value v set value = '12 items'
  from catering_row r
 where r.id = v.row_id and r.label like 'Any mix of%' and v.position = 0 and v.value = 'A dozen';
update catering_row_value v set value = '18 items'
  from catering_row r
 where r.id = v.row_id and r.label like 'Any mix of%' and v.position = 1 and v.value = 'Eighteen';
update catering_row_value v set value = '24 items'
  from catering_row r
 where r.id = v.row_id and r.label like 'Any mix of%' and v.position = 2 and v.value = 'Two dozen';

-- The other two lines are now named inside the first one. Guarded on the wording V6 left, so a line the
-- bakery has since edited stays and can be tidied by hand.
delete from catering_row r
 using catering_package p
 where p.id = r.package_id
   and p.name = 'Office boxes'
   and r.label in ('Mini scones', 'Mini cinnamon rolls');

-- "6+6" was the spreadsheet showing how a dozen scones splits by flavour. With one mixed line, that
-- belongs in the note, where the bakery's own minimum already lives.
update catering_package_note
   set body = 'Baked in sixes, so a dozen can be six muffins and six scones.'
 where body = 'Everything is baked in sixes, so each item comes in multiples of six.';

-- --- 2. how many people each column feeds -------------------------------------------------------------

update catering_tier t set serves = 'About 6–8 people'
  from catering_package p
 where p.id = t.package_id and p.name = 'Office boxes' and t.label = 'Small' and t.serves is null;
update catering_tier t set serves = 'About 10–12 people'
  from catering_package p
 where p.id = t.package_id and p.name = 'Office boxes' and t.label = 'Medium' and t.serves is null;
update catering_tier t set serves = 'About 15–20 people'
  from catering_package p
 where p.id = t.package_id and p.name = 'Office boxes' and t.label = 'Large' and t.serves is null;

-- The party and wedding columns are already named by head count ("15–20 people", "125 people"), so a
-- `serves` line under the price would say it twice. Left null on purpose.
