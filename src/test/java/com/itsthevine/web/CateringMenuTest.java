package com.itsthevine.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import com.itsthevine.web.domain.Money;

import jakarta.persistence.EntityManager;

/**
 * The catering tables, against the real seeded spreadsheet — so the migration is covered too.
 *
 * <p>Every test that writes runs inside the test's own transaction and is rolled back, so the seeded
 * page is the same for each one.
 */
@SpringBootTest
@Testcontainers
@Transactional
class CateringMenuTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>(DockerImageName.parse("postgres:18-alpine"));

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        // The contact starter refuses to start on a blank recipient, and this app has a
        // ContactController, so the context needs one even to test the catering page.
        registry.add("platform.contact.to", () -> "test@example.com");
        registry.add("platform.contact.from", () -> "noreply@example.com");
        registry.add("platform.storage.access-key", () -> "test");
        registry.add("platform.storage.secret-key", () -> "test");
    }

    @Autowired
    CateringMenu catering;

    @Autowired
    EntityManager entityManager;

    @Test
    void carriesTheBakerysSpreadsheetIntoTheDatabase() {
        List<CateringMenu.PackageView> tables = catering.menu().packages();

        assertThat(tables).extracting(CateringMenu.PackageView::name)
                .containsExactly("Office", "Parties", "Weddings");

        CateringMenu.PackageView office = tables.get(0);
        assertThat(office.tiers()).extracting(CateringMenu.TierView::label)
                .containsExactly("Small", "Medium", "Large");
        assertThat(office.tiers()).extracting(CateringMenu.TierView::price)
                .containsExactly("$24", "$32", "$40");
        assertThat(office.rows()).extracting(CateringMenu.RowView::label)
                .containsExactly("Mini muffins", "Mini scones", "Mini cinnamon rolls");
        assertThat(office.rows().get(0).values()).containsExactly("12 items", "18 items", "24 items");
        assertThat(office.rows().get(1).values().get(2)).isEqualTo("6+6+6+6 or 12+6+6 or 12+12");
        assertThat(office.notes()).singleElement().asString().contains("Minimum of 6 items per baked good");

        assertThat(tables.get(1).tiers()).extracting(CateringMenu.TierView::label)
                .containsExactly("15–20 people", "20–30 people", "30–40 people");
        assertThat(tables.get(2).tiers()).extracting(CateringMenu.TierView::price)
                .containsExactly("$236", "$310", "$386");
        // Wedding delivery terms belong to the wedding table, not to the page.
        assertThat(tables.get(2).notes()).anySatisfy(note -> assertThat(note).contains("delivery fee"));
    }

    @Test
    void everyLineCarriesOneEntryPerColumn() {
        // The invariant the whole aggregate exists to hold: if these ever fall out of step, a box is
        // advertised at another box's price.
        assertThat(catering.everything().packages()).allSatisfy(table ->
                assertThat(table.rows()).allSatisfy(row ->
                        assertThat(row.values()).hasSameSizeAs(table.tiers())));
    }

    @Test
    void keepsBlankCellsRatherThanCollapsingThem() {
        // "Mini cinnamon rolls" is named but not quantified in the spreadsheet. Dropping its blanks
        // would shorten the line and shift everything after it.
        CateringMenu.RowView rolls = catering.menu().packages().get(0).rows().get(2);
        assertThat(rolls.label()).isEqualTo("Mini cinnamon rolls");
        assertThat(rolls.values()).containsExactly("", "", "");
    }

    @Test
    void statesThePageWideTermsSeparatelyFromAnyOneTable() {
        assertThat(catering.menu().notes()).hasSize(2);
        assertThat(catering.menu().notes().get(0)).contains("price may change");
    }

    @Test
    void writesMoneyTheWayAPriceListDoes() {
        assertThat(Money.format(2400)).isEqualTo("$24");
        assertThat(Money.format(23600)).isEqualTo("$236");
        assertThat(Money.format(2450)).isEqualTo("$24.50");
        assertThat(Money.format(2405)).isEqualTo("$24.05");
        assertThat(Money.format(150000)).isEqualTo("$1,500");
        // "Ask us" is a legitimate price. It must not render as "$0".
        assertThat(Money.format(null)).isNull();
    }

    @Test
    void takesAPriceHoweverTheBakeryTypesIt() {
        assertThat(Money.cents("24")).isEqualTo(2400);
        assertThat(Money.cents("$24")).isEqualTo(2400);
        assertThat(Money.cents(" $1,250.00 ")).isEqualTo(125000);
        // The float route gives 2410.0000000000005 for this one.
        assertThat(Money.cents("24.10")).isEqualTo(2410);
        assertThat(Money.cents("")).isNull();
        assertThat(Money.cents(null)).isNull();

        assertThatThrownBy(() -> Money.cents("ask us"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("isn't a price");
        assertThatThrownBy(() -> Money.cents("24.005"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("go to the cent");
    }

    @Test
    void droppingAColumnTakesItsValuesWithIt() {
        CateringMenu.PackageView office = catering.everything().packages().get(0);
        Long medium = office.tiers().get(1).id();

        catering.save(office.id(), withoutColumn(office, 1));
        // Straight back to the database: the point of this test is the rows that were deleted, and a
        // session cache would happily show the right answer without them having been.
        entityManager.flush();
        entityManager.clear();

        CateringMenu.PackageView saved = catering.everything().packages().get(0);
        assertThat(saved.tiers()).extracting(CateringMenu.TierView::label).containsExactly("Small", "Large");
        assertThat(saved.tiers()).extracting(CateringMenu.TierView::id).doesNotContain(medium);
        assertThat(saved.rows().get(0).values()).containsExactly("12 items", "24 items");
        assertThat(saved.rows()).allSatisfy(row -> assertThat(row.values()).hasSize(2));
    }

    @Test
    void refusesATableWhoseLinesAndColumnsDisagree() {
        CateringMenu.PackageView office = catering.everything().packages().get(0);
        // A column removed but the values left alone — the mistake that would shift every price.
        CateringMenu.PackageEdit half = new CateringMenu.PackageEdit(
                office.name(), office.blurb(),
                asEdits(office).subList(0, 2),
                asLines(office),
                office.notes());

        assertThatThrownBy(() -> catering.save(office.id(), half))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Mini muffins")
                .hasMessageContaining("3 entries but the table has 2 columns");
    }

    @Test
    void editingATableKeepsTheLinesItAlreadyHad() {
        CateringMenu.PackageView office = catering.everything().packages().get(0);
        Long muffins = office.rows().get(0).id();

        List<CateringMenu.RowEdit> rows = new ArrayList<>(asLines(office));
        rows.set(0, new CateringMenu.RowEdit(muffins, "Mini muffins", List.of("12 items", "20 items", "24 items")));
        catering.save(office.id(), new CateringMenu.PackageEdit(
                "Office boxes", "For meetings and staff mornings.", asEdits(office), rows, office.notes()));

        CateringMenu.PackageView saved = catering.everything().packages().get(0);
        assertThat(saved.name()).isEqualTo("Office boxes");
        assertThat(saved.blurb()).isEqualTo("For meetings and staff mornings.");
        // Same line, edited — not a new line that happens to read the same.
        assertThat(saved.rows().get(0).id()).isEqualTo(muffins);
        assertThat(saved.rows().get(0).values()).containsExactly("12 items", "20 items", "24 items");
    }

    @Test
    void aNewTableStaysOffThePublicPageUntilItSaysSomething() {
        CateringMenu.PackageView fresh = catering.add("Holiday boxes");

        assertThat(catering.everything().packages()).extracting(CateringMenu.PackageView::name)
                .containsExactly("Office", "Parties", "Weddings", "Holiday boxes");
        assertThat(catering.menu().packages()).extracting(CateringMenu.PackageView::name)
                .doesNotContain("Holiday boxes");

        // Once it has a column and a line, it's a price table and it belongs on the page.
        catering.save(fresh.id(), new CateringMenu.PackageEdit("Holiday boxes", null,
                List.of(new CateringMenu.TierEdit(null, "Dozen", "18")),
                List.of(new CateringMenu.RowEdit(null, "Frosted cut-outs", List.of("12 items"))),
                List.of()));
        assertThat(catering.menu().packages()).extracting(CateringMenu.PackageView::name)
                .contains("Holiday boxes");
    }

    @Test
    void deletingATableTakesItsColumnsLinesAndCellsWithIt() {
        CateringMenu.PackageView weddings = catering.everything().packages().get(2);

        catering.remove(weddings.id());
        // Flushed on purpose: a delete that leaves its children behind fails against the real foreign
        // keys, not in memory, and this one is a button on the admin screen.
        entityManager.flush();
        entityManager.clear();

        assertThat(catering.everything().packages()).extracting(CateringMenu.PackageView::name)
                .containsExactly("Office", "Parties");
        // The page's own terms outlive any one table.
        assertThat(catering.menu().notes()).hasSize(2);
    }

    @Test
    void putsTheTablesWhereTheEditorLeftThem() {
        List<CateringMenu.PackageView> tables = catering.everything().packages();
        List<Long> weddingsFirst = List.of(tables.get(2).id(), tables.get(0).id(), tables.get(1).id());

        assertThat(catering.reorder(weddingsFirst)).extracting(CateringMenu.PackageView::name)
                .containsExactly("Weddings", "Office", "Parties");
        assertThat(catering.menu().packages()).extracting(CateringMenu.PackageView::name)
                .containsExactly("Weddings", "Office", "Parties");
    }

    @Test
    void refusesPricesAndHeadingsThatCantBeRight() {
        CateringMenu.PackageView office = catering.everything().packages().get(0);

        assertThatThrownBy(() -> catering.save(office.id(), withColumnPrice(office, "-1")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("less than nothing");
        assertThatThrownBy(() -> catering.save(office.id(), withColumnPrice(office, "$50,000")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("$10,000");
        assertThatThrownBy(() -> catering.save(office.id(), withColumnLabel(office, "  ")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Every column needs a heading");
        assertThatThrownBy(() -> catering.save(office.id(), withCell(office, "x".repeat(301))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("300 characters");
    }

    @Test
    void replacingThePageNotesIsTheWholeList() {
        assertThat(catering.replaceNotes(List.of("One term.", "  ", "Another term.")))
                // A blank line in the editor is not a note.
                .containsExactly("One term.", "Another term.");
        assertThat(catering.menu().notes()).containsExactly("One term.", "Another term.");

        assertThat(catering.replaceNotes(List.of("Only this one now."))).hasSize(1);
        entityManager.flush();
        entityManager.clear();
        assertThat(catering.menu().notes()).containsExactly("Only this one now.");
    }

    // --- turning what was read back into what the editor would send ---------------------------

    /** Note the round trip: what came back as "$24" goes out again as "$24" and must still mean 2400. */
    private static List<CateringMenu.TierEdit> asEdits(CateringMenu.PackageView table) {
        return table.tiers().stream()
                .map(t -> new CateringMenu.TierEdit(t.id(), t.label(), t.price()))
                .toList();
    }

    private static List<CateringMenu.RowEdit> asLines(CateringMenu.PackageView table) {
        return table.rows().stream()
                .map(r -> new CateringMenu.RowEdit(r.id(), r.label(), r.values()))
                .toList();
    }

    /** The table with one column gone, and every line's values narrowed to match — as the screen sends it. */
    private static CateringMenu.PackageEdit withoutColumn(CateringMenu.PackageView table, int column) {
        List<CateringMenu.TierEdit> tiers = new ArrayList<>(asEdits(table));
        tiers.remove(column);
        List<CateringMenu.RowEdit> rows = table.rows().stream().map(row -> {
            List<String> values = new ArrayList<>(row.values());
            values.remove(column);
            return new CateringMenu.RowEdit(row.id(), row.label(), values);
        }).toList();
        return new CateringMenu.PackageEdit(table.name(), table.blurb(), tiers, rows, table.notes());
    }

    private static CateringMenu.PackageEdit withColumnPrice(CateringMenu.PackageView table, String price) {
        List<CateringMenu.TierEdit> tiers = new ArrayList<>(asEdits(table));
        tiers.set(0, new CateringMenu.TierEdit(tiers.get(0).id(), tiers.get(0).label(), price));
        return new CateringMenu.PackageEdit(table.name(), table.blurb(), tiers, asLines(table), table.notes());
    }

    private static CateringMenu.PackageEdit withColumnLabel(CateringMenu.PackageView table, String label) {
        List<CateringMenu.TierEdit> tiers = new ArrayList<>(asEdits(table));
        tiers.set(0, new CateringMenu.TierEdit(tiers.get(0).id(), label, tiers.get(0).price()));
        return new CateringMenu.PackageEdit(table.name(), table.blurb(), tiers, asLines(table), table.notes());
    }

    private static CateringMenu.PackageEdit withCell(CateringMenu.PackageView table, String value) {
        List<CateringMenu.RowEdit> rows = new ArrayList<>(asLines(table));
        List<String> values = new ArrayList<>(rows.get(0).values());
        values.set(0, value);
        rows.set(0, new CateringMenu.RowEdit(rows.get(0).id(), rows.get(0).label(), values));
        return new CateringMenu.PackageEdit(table.name(), table.blurb(), asEdits(table), rows, table.notes());
    }
}
