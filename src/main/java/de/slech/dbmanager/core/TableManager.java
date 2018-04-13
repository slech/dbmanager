package de.slech.dbmanager.core;

import de.slech.dbmanager.data.DataSet;

/**
 * Dieses Basisinterface muss durch ein Interface erweitert werden, das eine Datenbanktabelle beschreibt.
 * @see DatabaseManager
 */
public interface TableManager<T extends TableManager> extends BaseTableManager<T> {
    /**
     * eine weitere Zeile soll dem Dataset oder Insert-Statement hinzugefügt werden. Es muss vorher
     * {@link BaseTableManager#newInsertStatementWithRow()} oder {@link BaseTableManager#newDataSetWithRow()}
     * aufgerufen worden sein.
     * @return das Interface, das die Tablle beschreibt
     */
    T andRow();

    /**
     * Das Insert Statement, das definiert wurde, soll erstellt werden. Es muss vorher
     * {@link BaseTableManager#newInsertStatementWithRow()} aufgerufen worden sein.
     */
    void executeStatement();

    /**
     * Die Query, die definiert wurde, soll ausgeführt werden. Es muss vorher {@link BaseTableManager#newQueryWhere()}
     * aufgerufen worden sein.
     * @return das Ergebnis der Query als Dataset
     */
    DataSet executeQuery();
    /**
     * Das Dataset, das definiert wurde, soll erstellt werden. Es muss vorher
     * {@link BaseTableManager#newDataSetWithRow()} aufgerufen worden sein.
     * @return das erstellte Dataset
     */
    DataSet buildDataset();
}
