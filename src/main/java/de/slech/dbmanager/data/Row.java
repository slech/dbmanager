package de.slech.dbmanager.data;

import de.slech.dbmanager.util.StringUtils;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Die Klasse repräsentiert eine Zeile einer Tabelle {@link DataSet}.
 */
public class Row {
    private Map<String, Object> columns = new HashMap<>();

    public Row addColum(String colName, Object value) {
        columns.put(colName, value);
        return this;
    }

    public Object getColumnValue(String colName) {
        return columns.get(colName);
    }
    /**
     *
     * @return eine nicht modifizierbare Map, die Spaltennamen auf die zugehörigen Werte abbildet;
     */
    public Map<String, Object> getColumns() {
        return Collections.unmodifiableMap(columns);
    }

    public boolean containsExpectedData(Row expected, boolean exactColumnSet) {
        if (exactColumnSet && expected.getColumns().size() != columns.size()) {
            return false;
        }
        return expected.getColumns().entrySet().stream().allMatch(this::checkColumn);
    }

    private boolean checkColumn(Map.Entry<String, Object> column) {
        return columns.containsKey(column.getKey()) && Objects.equals(column.getValue(), columns.get(column.getKey()));
    }

    @Override
    public String toString() {
        final String cols = StringUtils.concatStrings(
                columns.entrySet().stream().map(this::asString).collect(Collectors.toList()), ", ");
        return "Row: " + cols +";";
    }

    private String asString(Map.Entry<String, Object> entry) {
        return "(" + asStringHandleNull(entry.getKey()) + ": " + asStringHandleNull(entry.getValue()) + ")";
    }

    private String asStringHandleNull(Object obj) {
        return obj == null ? "null" : "\"" + obj.toString() + "\"";
    }

    public boolean isEmpty() {
        return columns.isEmpty();
    }
}
