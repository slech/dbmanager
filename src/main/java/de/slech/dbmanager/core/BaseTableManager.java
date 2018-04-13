package de.slech.dbmanager.core;

/**
 * Dieses Interface enthält die Operationen, um eine Interaktion mit der Tabelle zu starten, die durch den Typ Parameter
 * beschrieben wird
 * @param <T> Typ des Interface, das eine Datenbanktabelle beschreibt
 */
public interface BaseTableManager<T extends TableManager> {
    /**
     * ein Insert Statement soll für die Tabelle erstellt werden
     * @return das Interface, das die Tabelle beschreibt
     */
    T newInsertStatementWithRow();

    /**
     * eine Query soll für die Tabelle erstellt werden
     * @return das Interface, das die Tabelle beschreibt
     */
    T newQueryWhere();

    /**
     * ein DataSet soll für die Tabelle erstellt werden
     * @return das Interface, das die Tabelle beschreibt
     */
    T newDataSetWithRow();

    /**
     * ein Insert Statement soll für die Tabelle erstellt werden
     * @return der Name der Tabelle
     */
    String getTableName();

    /**
     * es sollen Default Werte gesetzt werden
     * @return das Interface, das die Tabelle beschreibt
     */
    T setDefaultValues();

    /**
     * die Werte für die folgeneden Spalten sollen generiert werden
     * @return das Interface, das die Tabelle beschreibt
     */
    T generateValuesFor();
}
