package de.slech.dbmanager.compare;

import de.slech.dbmanager.data.DataSet;

/**
 * Die Klasse beschreibt, wie zwei {@link DataSet}s miteinander verglichen werden sollen. DEFAULT spielt die Reihenfolge
 * der Zeilen keine Rolle, aber es müssen genau die gleichen Zeilen vorhanden sein. Bei den Spalten genügt es,
 * dass alle Spalten des ersten Dataset im zweiten Datenset enthalten sein müssen, das zweite aber noch zusätzliche
 * Spalten enthalten darf. Die Reichenfolge der Spalten spielt keine Rolle.
 */
public class CompareMode {

    static final CompareMode DEFAULT = new CompareMode(false);
    private final boolean exactColumnSet;
    private final boolean exactRowSet;
    private final boolean exactRowSequence;

    public CompareMode(boolean exactRowSequence) {
        this.exactColumnSet = false;
        this.exactRowSet = true;
        this.exactRowSequence = exactRowSequence;
    }

    boolean isExactColumnSet() {
        return exactColumnSet;
    }

    boolean isExactRowSet() {
        return exactRowSet;
    }

    boolean isExactRowSequence() {
        return exactRowSequence;
    }
}
