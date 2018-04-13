package de.slech.dbmanager.data;


import de.slech.dbmanager.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Die Klasse repräsentiert eine Tabelle, eine Liste von Zeilen {@link Row}.
 */
public class DataSet {
    private final List<Row> rows = new ArrayList<>();

    public DataSet addRow(Row row) {
        if (row.isEmpty()) {
            throw new IllegalArgumentException("Row has no columns.");
        }
        rows.add(row);
        return this;
    }

    public Stream<Row> stream() {
        return  rows.stream();
    }


    public int getRowCount() {
        return rows.size();
    }

    @Override
    public String toString() {
        return StringUtils.concatStrings(stream().map(Row::toString).collect(Collectors.toList()), "\n");
    }

    public boolean isEmpty() {
        return rows.isEmpty();
    }
}
