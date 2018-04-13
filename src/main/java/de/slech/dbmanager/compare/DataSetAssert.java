package de.slech.dbmanager.compare;

import de.slech.dbmanager.data.DataSet;
import de.slech.dbmanager.data.Row;

import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Mit dieser Klasse können zwei DataSets verglichen werden. Stimmen Sie nicht überein, wird ein
 * AssertionError geworfen.
 */
public class DataSetAssert {

    private final DataSet expected;
    private final DataSet actual;
    private final CompareMode compareMode;

    private DataSetAssert(DataSet expected, DataSet actual, CompareMode compareMode) {
        this.expected = expected;
        this.actual = actual;
        this.compareMode = compareMode;
    }

    /**
     * Vergleicht die DataSets anhand des CompareMode DEFAULT, siehe {@link CompareMode}
     * @param expected erwartetes Dataset
     * @param actual tatsächliches Dataset
     */
    public static void assertContainsExpectedData(DataSet expected, DataSet actual) {
        assertContainsExpectedData(expected, actual, CompareMode.DEFAULT);
    }

    /**
     * Vergleicht die DataSets anhand des angegebenen {@link CompareMode}
     * @param expected erwartetes Dataset
     * @param actual tatsächliches Dataset
     * @param compareMode nach diesem Modus wird verglichen
     */
    public static void assertContainsExpectedData(DataSet expected, DataSet actual, CompareMode compareMode) {
        new DataSetAssert(expected, actual, compareMode).assertContainsExpectedData();
    }

    private void assertContainsExpectedData() {
        if (compareMode.isExactRowSet() && actual.getRowCount() != expected.getRowCount() ) {
            final String errorMsg = String.format("Number of expected rows: %d, actual : %d\n%s",
                    expected.getRowCount(), actual.getRowCount(),
                    expectedVsActual(expected, actual));
            throw new AssertionError(errorMsg);
        }
        final List<Row> actualRows = actual.stream().collect(Collectors.toList());
        expected.stream().forEachOrdered(expectedRow -> assertMatchingRowExists(expectedRow, actualRows));
    }

    private void assertMatchingRowExists(Row expectedRow, List<Row> actualRows) {
        if (!matchingRowExists(expectedRow, actualRows)) {
            final String errorMsg = String.format("Expected row not found: %s\n%s",
                    expectedRow.toString(),
                    expectedVsActual(expected, actual));
            throw new AssertionError(errorMsg);
        }
    }

    private static String expectedVsActual(DataSet expected, DataSet actual) {
        return String.format("Expected:\n%s\nActual:\n%s\n", expected.toString(), actual.toString());
    }

    private boolean matchingRowExists(Row testRow, List<Row> actualRows) {
        for (Iterator<Row> iterator = actualRows.iterator(); iterator.hasNext(); ) {
            final Row row = iterator.next();
            if (row.containsExpectedData(testRow, compareMode.isExactColumnSet())) {
                iterator.remove();
                return true;
            }
            if (compareMode.isExactRowSequence()) {
                iterator.remove();
            }
        }
        return false;
    }

}
