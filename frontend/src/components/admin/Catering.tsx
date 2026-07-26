import { useCallback, useEffect, useState } from 'react';
import {
  addCateringTable,
  adminCatering,
  deleteCateringTable,
  reorderCateringTables,
  saveCateringNotes,
  saveCateringTable,
  type CateringTable,
} from '@/lib/api';
import {
  ARROW_DOWN,
  ARROW_LEFT,
  ARROW_RIGHT,
  ARROW_UP,
  CHECK,
  Icon,
  PLUS,
  TRASH,
  X,
  danger,
  field,
  iconButton,
  primary,
  secondary,
  shift,
} from '@/components/admin/ui';

/**
 * The goodie box and catering price tables, editable by the person who quotes them.
 *
 * A table is edited as a table and saved in one go, unlike the catalogue next door where every change
 * saves as you make it. That's not a different taste in interfaces: a column heading, its price and
 * the entries beneath it only mean anything together, so they have to be moved, added and removed
 * together. Adding a column here adds an empty entry to every line, and removing one takes its
 * entries with it — the server refuses any table whose lines and columns disagree, because the
 * alternative is the Large box quietly advertising the Medium box's contents at the Large price.
 */

// --- what's on screen -------------------------------------------------------

type TierDraft = { id: number | null; label: string; price: string };
type RowDraft = { id: number | null; label: string; values: string[] };
type Draft = { name: string; blurb: string; tiers: TierDraft[]; rows: RowDraft[]; notes: string[] };

const draftOf = (table: CateringTable): Draft => ({
  name: table.name,
  blurb: table.blurb ?? '',
  // The price arrives written out ("$24"); it goes back as whatever the editor leaves in the box, and
  // the server decides what that's worth.
  tiers: table.tiers.map((tier) => ({ id: tier.id, label: tier.label, price: tier.price ?? '' })),
  rows: table.rows.map((row) => ({ id: row.id, label: row.label, values: [...row.values] })),
  notes: [...table.notes],
});

/** Notes are edited as a list; deleting one is an omission, exactly as the server expects. */
const Notes = ({
  notes,
  hint,
  disabled,
  onChange,
}: {
  notes: string[];
  hint: string;
  disabled?: boolean;
  onChange: (notes: string[]) => void;
}) => (
  <div>
    <p className="text-sm text-bakery-600">{hint}</p>
    <ul className="mt-2 space-y-2">
      {notes.map((note, i) => (
        <li key={i} className="flex items-start gap-2">
          <textarea
            className={`${field} min-h-[3.25rem]`}
            rows={2}
            value={note}
            disabled={disabled}
            onChange={(e) => onChange(notes.map((n, at) => (at === i ? e.target.value : n)))}
          />
          <div className="flex gap-1 pt-1">
            <button
              type="button"
              className={iconButton}
              disabled={disabled || i === 0}
              onClick={() => onChange(shift(notes, i, -1))}
              aria-label="Move note up"
            >
              <Icon d={ARROW_UP} />
            </button>
            <button
              type="button"
              className={iconButton}
              disabled={disabled}
              onClick={() => onChange(notes.filter((_, at) => at !== i))}
              aria-label="Remove note"
            >
              <Icon d={TRASH} />
            </button>
          </div>
        </li>
      ))}
    </ul>
    <button
      type="button"
      className={`${secondary} mt-2`}
      disabled={disabled}
      onClick={() => onChange([...notes, ''])}
    >
      <Icon d={PLUS} />
      Add a note
    </button>
  </div>
);

// --- one table --------------------------------------------------------------

const TableCard = ({
  table,
  first,
  last,
  onSaved,
  onMove,
  onDelete,
  onError,
}: {
  table: CateringTable;
  first: boolean;
  last: boolean;
  onSaved: (saved: CateringTable) => void;
  onMove: (delta: number) => void;
  onDelete: () => void;
  onError: (message: string) => void;
}) => {
  const [draft, setDraft] = useState<Draft>(() => draftOf(table));
  const [busy, setBusy] = useState(false);

  const stored = draftOf(table);
  const dirty = JSON.stringify(draft) !== JSON.stringify(stored);

  // A reorder re-renders this card with a fresh copy from the server; the boxes should follow along
  // unless they're being edited.
  const [synced, setSynced] = useState(table);
  if (synced !== table) {
    setSynced(table);
    if (!dirty) setDraft(draftOf(table));
  }

  const edit = (change: Partial<Draft>) => setDraft({ ...draft, ...change });

  // Columns. Every one of these keeps the lines in step — that is the whole job of this screen.
  const addColumn = () =>
    edit({
      tiers: [...draft.tiers, { id: null, label: '', price: '' }],
      rows: draft.rows.map((row) => ({ ...row, values: [...row.values, ''] })),
    });

  const removeColumn = (column: number) =>
    edit({
      tiers: draft.tiers.filter((_, at) => at !== column),
      rows: draft.rows.map((row) => ({ ...row, values: row.values.filter((_, at) => at !== column) })),
    });

  const moveColumn = (column: number, delta: number) =>
    edit({
      tiers: shift(draft.tiers, column, delta),
      rows: draft.rows.map((row) => ({ ...row, values: shift(row.values, column, delta) })),
    });

  const setColumn = (column: number, change: Partial<TierDraft>) =>
    edit({ tiers: draft.tiers.map((tier, at) => (at === column ? { ...tier, ...change } : tier)) });

  // Lines.
  const addLine = () =>
    edit({ rows: [...draft.rows, { id: null, label: '', values: draft.tiers.map(() => '') }] });

  const setLine = (line: number, change: Partial<RowDraft>) =>
    edit({ rows: draft.rows.map((row, at) => (at === line ? { ...row, ...change } : row)) });

  const setCell = (line: number, column: number, value: string) =>
    setLine(line, {
      values: draft.rows[line].values.map((entry, at) => (at === column ? value : entry)),
    });

  const save = async () => {
    setBusy(true);
    try {
      onSaved(
        await saveCateringTable(table.id, {
          name: draft.name.trim(),
          blurb: draft.blurb.trim() || null,
          tiers: draft.tiers.map((tier) => ({ id: tier.id, label: tier.label.trim(), price: tier.price.trim() })),
          rows: draft.rows.map((row) => ({
            id: row.id,
            label: row.label.trim(),
            values: row.values.map((entry) => entry.trim()),
          })),
          notes: draft.notes,
        }),
      );
    } catch (e) {
      onError(e instanceof Error ? e.message : 'That did not save.');
    } finally {
      setBusy(false);
    }
  };

  const columns = draft.tiers.length;

  return (
    <li className="rounded-lg border border-bakery-200 bg-white p-4 shadow-sm">
      <div className="flex flex-wrap items-start gap-2">
        <div className="flex gap-1 pt-1">
          <button
            type="button"
            className={iconButton}
            disabled={first}
            onClick={() => onMove(-1)}
            aria-label={`Move the ${table.name} table up`}
          >
            <Icon d={ARROW_UP} />
          </button>
          <button
            type="button"
            className={iconButton}
            disabled={last}
            onClick={() => onMove(1)}
            aria-label={`Move the ${table.name} table down`}
          >
            <Icon d={ARROW_DOWN} />
          </button>
        </div>
        <div className="grid flex-1 gap-2 sm:grid-cols-[14rem_1fr]">
          <label className="block">
            <span className="sr-only">Table name</span>
            <input
              className={field}
              value={draft.name}
              onChange={(e) => edit({ name: e.target.value })}
              placeholder="Weddings"
            />
          </label>
          <label className="block">
            <span className="sr-only">A line under the heading</span>
            <input
              className={field}
              value={draft.blurb}
              onChange={(e) => edit({ blurb: e.target.value })}
              placeholder="Optional — a line under the heading"
            />
          </label>
        </div>
      </div>

      {/* Wide tables scroll here rather than making the page scroll sideways. */}
      <div className="mt-4 -mx-4 overflow-x-auto px-4">
        <table className="w-full border-separate border-spacing-1">
          <thead>
            <tr>
              <th scope="col" className="w-48 text-left text-sm font-medium text-bakery-600">
                What they get
              </th>
              {draft.tiers.map((tier, column) => (
                <th key={tier.id ?? `new-${column}`} scope="col" className="min-w-44 align-top">
                  <input
                    className={field}
                    value={tier.label}
                    onChange={(e) => setColumn(column, { label: e.target.value })}
                    placeholder="Small"
                    aria-label={`Heading for column ${column + 1}`}
                  />
                  <input
                    className={`${field} mt-1`}
                    value={tier.price}
                    onChange={(e) => setColumn(column, { price: e.target.value })}
                    placeholder="$24 — leave empty to ask"
                    aria-label={`Price for column ${column + 1}`}
                  />
                  <div className="mt-1 flex justify-center gap-1">
                    <button
                      type="button"
                      className={iconButton}
                      disabled={column === 0}
                      onClick={() => moveColumn(column, -1)}
                      aria-label="Move this column left"
                    >
                      <Icon d={ARROW_LEFT} />
                    </button>
                    <button
                      type="button"
                      className={iconButton}
                      disabled={column === columns - 1}
                      onClick={() => moveColumn(column, 1)}
                      aria-label="Move this column right"
                    >
                      <Icon d={ARROW_RIGHT} />
                    </button>
                    <button
                      type="button"
                      className={iconButton}
                      onClick={() => removeColumn(column)}
                      aria-label="Remove this column"
                      title="Removes this column and its entries on every line"
                    >
                      <Icon d={TRASH} />
                    </button>
                  </div>
                </th>
              ))}
              <th scope="col" className="w-32 align-top">
                <button type="button" className={secondary} onClick={addColumn}>
                  <Icon d={PLUS} />
                  Column
                </button>
              </th>
            </tr>
          </thead>
          <tbody>
            {draft.rows.map((row, line) => (
              <tr key={row.id ?? `new-${line}`}>
                <th scope="row" className="text-left align-top">
                  <input
                    className={field}
                    value={row.label}
                    onChange={(e) => setLine(line, { label: e.target.value })}
                    placeholder="Mini muffins"
                    aria-label={`Name of line ${line + 1}`}
                  />
                </th>
                {row.values.map((entry, column) => (
                  <td key={column} className="align-top">
                    <input
                      className={field}
                      value={entry}
                      onChange={(e) => setCell(line, column, e.target.value)}
                      placeholder="—"
                      aria-label={`${row.label || `Line ${line + 1}`}, ${
                        draft.tiers[column]?.label || `column ${column + 1}`
                      }`}
                    />
                  </td>
                ))}
                <td className="align-top">
                  <div className="flex gap-1">
                    <button
                      type="button"
                      className={iconButton}
                      disabled={line === 0}
                      onClick={() => edit({ rows: shift(draft.rows, line, -1) })}
                      aria-label="Move this line up"
                    >
                      <Icon d={ARROW_UP} />
                    </button>
                    <button
                      type="button"
                      className={iconButton}
                      disabled={line === draft.rows.length - 1}
                      onClick={() => edit({ rows: shift(draft.rows, line, 1) })}
                      aria-label="Move this line down"
                    >
                      <Icon d={ARROW_DOWN} />
                    </button>
                    <button
                      type="button"
                      className={iconButton}
                      onClick={() => edit({ rows: draft.rows.filter((_, at) => at !== line) })}
                      aria-label="Remove this line"
                    >
                      <Icon d={TRASH} />
                    </button>
                  </div>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      <button type="button" className={`${secondary} mt-1`} onClick={addLine}>
        <Icon d={PLUS} />
        Line
      </button>

      <div className="mt-4">
        <Notes
          notes={draft.notes}
          hint="Small print under this table — minimums, what can't be mixed, how delivery is charged."
          disabled={busy}
          onChange={(notes) => edit({ notes })}
        />
      </div>

      <div className="mt-4 flex flex-wrap items-center gap-2 border-t border-bakery-100 pt-3">
        <button type="button" className={primary} disabled={busy || !dirty} onClick={() => void save()}>
          <Icon d={CHECK} />
          {busy ? 'Saving…' : 'Save this table'}
        </button>
        <button
          type="button"
          className={secondary}
          disabled={busy || !dirty}
          onClick={() => setDraft(draftOf(table))}
        >
          <Icon d={X} />
          Undo my changes
        </button>
        {dirty && <span className="text-sm text-bakery-600">Not saved yet.</span>}
        {(columns === 0 || draft.rows.length === 0) && !dirty && (
          <span className="text-sm text-bakery-600">
            Needs a column and a line before it shows on the page.
          </span>
        )}
        <button type="button" className={`${danger} ml-auto`} disabled={busy} onClick={onDelete}>
          <Icon d={TRASH} />
          Delete table
        </button>
      </div>
    </li>
  );
};

// --- the section ------------------------------------------------------------

const Catering = ({ onError }: { onError: (message: string) => void }) => {
  const [tables, setTables] = useState<CateringTable[] | null>(null);
  const [pageNotes, setPageNotes] = useState<string[]>([]);
  const [storedNotes, setStoredNotes] = useState<string[]>([]);
  const [fresh, setFresh] = useState('');
  const [busy, setBusy] = useState(false);

  const load = useCallback(async () => {
    try {
      const menu = await adminCatering();
      setTables(menu.packages);
      setPageNotes(menu.notes);
      setStoredNotes(menu.notes);
    } catch (e) {
      onError(e instanceof Error ? e.message : 'Could not load the catering tables.');
      setTables([]);
    }
  }, [onError]);

  useEffect(() => {
    void load();
  }, [load]);

  /** Moving a table applies on screen first; this page shouldn't freeze between clicks. */
  const settle = async (optimistic: CateringTable[], work: () => Promise<unknown>) => {
    const before = tables ?? [];
    setTables(optimistic);
    try {
      await work();
    } catch (e) {
      setTables(before);
      onError(e instanceof Error ? e.message : 'That did not save.');
    }
  };

  const guard = async (work: () => Promise<unknown>) => {
    setBusy(true);
    try {
      await work();
    } catch (e) {
      onError(e instanceof Error ? e.message : 'That did not save.');
    } finally {
      setBusy(false);
    }
  };

  const notesDirty = JSON.stringify(pageNotes) !== JSON.stringify(storedNotes);

  return (
    <section>
      <h2 className="font-adbhashitha text-xl text-bakery-800">Goodie boxes &amp; catering</h2>
      <p className="mt-1 text-sm text-bakery-600">
        The price tables, in the order they appear on the page. Each one saves on its own.
      </p>

      {tables === null ? (
        <p className="mt-3 text-bakery-600">Loading…</p>
      ) : (
        <>
          <ul className="mt-3 space-y-4">
            {tables.map((table, i) => (
              <TableCard
                key={table.id}
                table={table}
                first={i === 0}
                last={i === tables.length - 1}
                onError={onError}
                onSaved={(saved) => setTables(tables.map((t) => (t.id === saved.id ? saved : t)))}
                onMove={(delta) => {
                  const moved = shift(tables, i, delta);
                  void settle(moved, () => reorderCateringTables(moved.map((t) => t.id)));
                }}
                onDelete={() => {
                  if (!confirm(`Delete the ${table.name} table and everything in it?`)) return;
                  void settle(
                    tables.filter((t) => t.id !== table.id),
                    () => deleteCateringTable(table.id),
                  );
                }}
              />
            ))}
          </ul>

          <div className="mt-4 flex gap-2">
            <input
              className={field}
              value={fresh}
              onChange={(e) => setFresh(e.target.value)}
              placeholder="New table, e.g. Graduation parties"
            />
            <button
              type="button"
              className={primary}
              disabled={busy || !fresh.trim()}
              onClick={() =>
                void guard(async () => {
                  const added = await addCateringTable(fresh.trim());
                  setTables([...(tables ?? []), added]);
                  setFresh('');
                })
              }
            >
              <Icon d={PLUS} />
              Add
            </button>
          </div>

          <div className="mt-6 rounded-lg border border-bakery-200 bg-white p-4 shadow-sm">
            <h3 className="font-adbhashitha text-lg text-bakery-800">Under the whole page</h3>
            <div className="mt-2">
              <Notes
                notes={pageNotes}
                hint="Terms that apply whichever table someone is reading."
                disabled={busy}
                onChange={setPageNotes}
              />
            </div>
            <div className="mt-3 flex items-center gap-2">
              <button
                type="button"
                className={primary}
                disabled={busy || !notesDirty}
                onClick={() =>
                  void guard(async () => {
                    const saved = await saveCateringNotes(pageNotes);
                    setPageNotes(saved);
                    setStoredNotes(saved);
                  })
                }
              >
                <Icon d={CHECK} />
                Save these notes
              </button>
              {notesDirty && (
                <button type="button" className={secondary} onClick={() => setPageNotes(storedNotes)}>
                  <Icon d={X} />
                  Undo
                </button>
              )}
            </div>
          </div>
        </>
      )}
    </section>
  );
};

export default Catering;
