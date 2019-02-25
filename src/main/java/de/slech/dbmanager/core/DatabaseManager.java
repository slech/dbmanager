package de.slech.dbmanager.core;


import de.slech.dbmanager.converter.EnumToNameConverter;
import de.slech.dbmanager.data.DataSet;
import de.slech.dbmanager.data.Row;
import de.slech.dbmanager.util.StringUtils;
import de.slech.dbmanager.exeption.SystemException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.AttributeConverter;
import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Table;
import javax.sql.DataSource;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
/**
 * Über diese Klasse kann ein dynamischer Proxy für ein Interface erzeugt werden, das TableManager erweitert.
 * Dieses Interface beschreibt eine Datenbanktabelle.
 * Der Name des Interfaces ist der Name der Tabelle, dies kann aber über die Annotation {@link Table} überschrieben
 * werden. Die Methoden beschreiben die Spalten, der Methodenname ist der Name der Spalte (kann über die {@link Column}
 * Annotation überschrieben werden) und der Typ des Parameters legt den Typ der Spalte fest. Die Methode muss immer
 * {@code this} zurückgeben.
 * <p>Über eine {@link Convert} der Methode
 * Annotation kann ein Konverter für eine Typtransformation gesetzt werden. Der Konverter muss das Interface
 * {@link AttributeConverter} implementieren, allerdings kann die Methode
 * {@link AttributeConverter#convertToEntityAttribute} eine {@link UnsupportedOperationException} werfen, da diese
 * nicht aufgerufen wird. Beispiel: {@link EnumToNameConverter} <p>
 * Liegt für die Tabelle ein JPA entity mit Field Annotationen vor, kann man die Methoden des Interface erzeugen,
 * indem man die Felder des Entity in das Interface kopiert und Suchen und Ersetzen mit folgendem
 * RegEx Ausdruck ausführt: Ersetze {@code private\s*(\w+)\s*(\w+)(\s*|=).*;} durch {@code <interfacename> $2($1 $2);}
 */
public class DatabaseManager {

    private final JdbcHelper jdbcHelper;

    private static String getTableName(Class<? extends TableManager<?>> tableClass) {
        final Table tblAnno = tableClass.getAnnotation(Table.class);
        if (tblAnno != null && !StringUtils.isBlank(tblAnno.name())) {
            return tblAnno.name();
        }
        return tableClass.getSimpleName();
    }

    public DatabaseManager(DataSource dataSource) {
        this.jdbcHelper = new JdbcHelper(dataSource);
    }

    /**
     * Creates a tabl manager of the passed class
     * @param tableClass table manager class
     * @param <T> type of the table manager class
     * @return new Tablemanager
     */
    public <T extends TableManager<T>> BaseTableManager<T> createTableManager(Class<T> tableClass) {
        return tableClass.cast(Proxy.newProxyInstance(tableClass.getClassLoader(), new Class[]{tableClass},
                new TableManagerInvocationHandler(tableClass, jdbcHelper)));
    }

    /**
     * Executes an sql update statement
     * @param sql sql update statement
     */
    public void executeUpdate(String sql) {
        jdbcHelper.executeUpdate(sql);
    }

    /**
     * deletes all content of the table corespondending to the passed table manager classes
     * @param tableClasses this classes represent the tables from which the data is to be deleted
     */
    public void deleteAllFromTables(List<Class<? extends TableManager<?>>> tableClasses) {
        jdbcHelper.deleteAllFromTables(
                tableClasses.stream().map(DatabaseManager::getTableName).collect(Collectors.toList()));
    }

    private enum OperationState {NONE, DEFAULT_VALUES, GENERATED_VALUES, QUERY, ROW_IN_DATASET, ROW_IN_INSERT_STMT}

    /**
     * Invocationhandler zur Realisierung eines Proxys für Interfaces, die zur Erzeugung von Insert Statements dienen
     */
    private static class TableManagerInvocationHandler implements InvocationHandler {

        private static final Logger LOGGER = LoggerFactory.getLogger(TableManagerInvocationHandler.class);
        private final Map<String, IntegralValueGenerator<?>> generatedValues = new HashMap<>();
        private final String tableName;
        private final Map<Method, AttributeConverter> converters = new HashMap<>();
        private final Map<Method, String> columnNames = new HashMap<>();
        private final JdbcHelper jdbcHelper;
        private DataSet dataSet = new DataSet();
        private Row currentRow = new Row();
        private Row defaultValues = new Row();
        private OperationState currentOperation = OperationState.NONE;

        TableManagerInvocationHandler(Class<? extends TableManager<?>> tableClass, JdbcHelper jdbcHelper) {
            this.tableName = getTableName(tableClass);
            this.jdbcHelper = jdbcHelper;
            Arrays.stream(tableClass.getMethods())
                    .filter(method ->  !BaseTableManager.class.equals(method.getDeclaringClass()) &&
                            !TableManager.class.equals(method.getDeclaringClass()))
                    .forEach(this::cacheConvertersAndColumnNames);
        }

        private void cacheConvertersAndColumnNames(Method method) {
            cacheConverters(method);
            cacheColumnNames(method);
        }


        private void cacheConverters(Method method) {
            final Convert convertAnnotation = method.getAnnotation(Convert.class);
            if (convertAnnotation != null
                    && AttributeConverter.class.isAssignableFrom(convertAnnotation.converter())) {
                try {
                    converters.put(method, (AttributeConverter) convertAnnotation.converter().newInstance());
                } catch (InstantiationException | IllegalAccessException e) {
                    throw new SystemException(e);
                }
            }
        }

        private void cacheColumnNames(Method method) {
            final Column columnAnnotation = method.getAnnotation(Column.class);
            if (columnAnnotation != null && !StringUtils.isBlank(columnAnnotation.name())) {
                columnNames.put(method, columnAnnotation.name());
            } else {
                columnNames.put(method, method.getName());
            }
        }



        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            if ("getTableName".equals(method.getName())) {
                return tableName;
            }
            if ("executeStatement".equals(method.getName())) {
                executeStatement();
                return null;
            }
            if ("buildDataset".equals(method.getName())) {
                return buildDataset();
            }
            if ("executeQuery".equals(method.getName())) {
                return executeQuery();
            }
            if ("getAll".equals(method.getName())) {
                return getAll();
            }
            if ("generateValuesFor".equals(method.getName())) {
                generateValuesFor();
            } else if ("setDefaultValues".equals(method.getName())) {
                setDefaultValues();
            } else if ("newInsertStatementWithRow".equals(method.getName())) {
                newInsertStatementWithRow();
            } else if ("newDataSetWithRow".equals(method.getName())) {
                newDataSetWithRow();
            } else if ("newQueryWhere".equals(method.getName())) {
                newQueryWhere();
            } else if ("andRow".equals(method.getName())) {
                andRow();
            } else {
                addColumn(method, args);
            }
            return proxy;
        }

        private void setDefaultValues() {
            checkState(OperationState.NONE, OperationState.GENERATED_VALUES);
            currentOperation = OperationState.DEFAULT_VALUES;
        }

        private void generateValuesFor() {
            checkState(OperationState.NONE, OperationState.DEFAULT_VALUES);
            currentOperation = OperationState.GENERATED_VALUES;
        }

        private void newQueryWhere() {
            checkState(OperationState.NONE);
            currentOperation = OperationState.QUERY;
        }

        private void newDataSetWithRow() {
            checkState(OperationState.NONE, OperationState.DEFAULT_VALUES, OperationState.GENERATED_VALUES);
            currentOperation = OperationState.ROW_IN_DATASET;
        }

        private void newInsertStatementWithRow() {
            checkState(OperationState.NONE, OperationState.DEFAULT_VALUES, OperationState.GENERATED_VALUES);
            currentOperation = OperationState.ROW_IN_INSERT_STMT;
        }

        private void addColumn(Method method, Object[] args) {
            checkState(OperationState.DEFAULT_VALUES, OperationState.GENERATED_VALUES, OperationState.QUERY,
                    OperationState.ROW_IN_DATASET, OperationState.ROW_IN_INSERT_STMT);
            if (args!= null && args.length > 0)  {
                if (currentOperation == OperationState.GENERATED_VALUES) {
                    putGeneratorToMap(method, args[0]);
                    return;
                }
                Row usedRow = currentOperation == OperationState.DEFAULT_VALUES ? defaultValues : currentRow;
                usedRow.addColum(getColName(method), getValue(method, args[0]));
            } else {
                LOGGER.warn("Die Liste der Argumente für Methode {} ist leer", method);
            }
        }

        private void putGeneratorToMap(Method method, Object arg) {
            Object convertedArg = getValue(method, arg);
            if (convertedArg instanceof Short) {
                generatedValues.put(getColName(method), new ShortValueGenerator((Short) convertedArg));
            } else if (convertedArg instanceof Integer) {
                generatedValues.put(getColName(method), new IntegerValueGenerator((Integer) convertedArg));
            } else if (convertedArg instanceof Long) {
                generatedValues.put(getColName(method), new LongValueGenerator((Long) convertedArg));
            }
        }

        private void andRow() {
            checkState(OperationState.ROW_IN_DATASET, OperationState.ROW_IN_INSERT_STMT);
            addRowToDataset();
            currentRow = new Row();
        }

        private void executeStatement() {
            checkState(OperationState.ROW_IN_INSERT_STMT);
            addRowToDataset();
            jdbcHelper.executeInsert(tableName, dataSet);
            resetDataSet();
        }

        private void resetDataSet() {
            dataSet = new DataSet();
            currentRow = new Row();
            defaultValues = new Row();
            currentOperation = OperationState.NONE;
            generatedValues.clear();
        }

        private DataSet buildDataset() {
            checkState(OperationState.ROW_IN_DATASET);
            addRowToDataset();
            final DataSet currentDs = dataSet;
            resetDataSet();
            return currentDs;
        }

        private void addRowToDataset() {
            addDefaultValuesToCurrentRow();
            addGeneratedValuesToCurrentRow();
            dataSet.addRow(currentRow);
        }

        private void addDefaultValuesToCurrentRow() {
            defaultValues.getColumns().entrySet().forEach(this::addToCurrentRow);
        }

        private void addGeneratedValuesToCurrentRow() {
            generatedValues.entrySet().forEach(this::setGeneratedValue);
            defaultValues.getColumns().entrySet().forEach(this::addToCurrentRow);
        }

        private void setGeneratedValue(Map.Entry<String, IntegralValueGenerator<?>> entry) {
            final String columnName = entry.getKey();
            final IntegralValueGenerator<?> valueGenerator = entry.getValue();
            final Object columnValue = currentRow.getColumnValue(columnName);
            if (columnValue == null) {
                currentRow.addColum(columnName, valueGenerator.nextValue());
            } else {
                valueGenerator.setValueGreaterThan((Number)columnValue);
            }
        }

        private void addToCurrentRow(Map.Entry<String, Object> stringObjectEntry) {
            if (!currentRow.getColumns().containsKey(stringObjectEntry.getKey())) {
                currentRow.addColum(stringObjectEntry.getKey(), stringObjectEntry.getValue());
            }
        }

        private DataSet executeQuery() {
            checkState(OperationState.QUERY);
            final DataSet result = jdbcHelper.executeQuery(tableName, columnNames.values(),
                    currentRow.getColumns());
            resetDataSet();
            return result;
        }

        private DataSet getAll() {
            checkState(OperationState.NONE);
            final DataSet result = jdbcHelper.getAll(tableName, columnNames.values());
            resetDataSet();
            return result;
        }

        private void checkState(OperationState ... expected) {
            if (Arrays.stream(expected).noneMatch(operationState ->  operationState == currentOperation)) {
                final String errorMsg = errorMsgWrongState(currentOperation, expected);
                resetDataSet();
                throw new IllegalStateException(errorMsg);
            }

        }

        private String errorMsgWrongState (OperationState currentOperation, OperationState ... expected) {
            return String.format("Wrong operation state, expected: %s, actual: %s.",
                    StringUtils.concatStrings(Arrays.stream(expected).map(Enum::name).collect(Collectors.toList()),","),
                    currentOperation.name());
        }


        @SuppressWarnings("unchecked")
        private Object getValue(Method method, Object arg) {
            AttributeConverter converter = converters.get(method);
            if ( converter != null) {
                return converter.convertToDatabaseColumn(arg);
            }
            return arg;
        }

        private String getColName(Method method) {
            return columnNames.get(method);
        }

    }
    private interface IntegralValueGenerator<T extends Number> {
        T nextValue();
        void setValueGreaterThan(Number newValue);

    }

    private static class IntegerValueGenerator implements IntegralValueGenerator<Integer> {
        private int value;

        IntegerValueGenerator(Integer startValue) {
            value = startValue == null ? 0 : startValue;
        }

        @Override
        public Integer nextValue() {
            return value++;
        }

        @Override
        public void setValueGreaterThan(Number newValue) {
            if (newValue != null && newValue.intValue() >= value) {
                value = newValue.intValue() + 1;
            }
        }
    }

    private static class LongValueGenerator implements IntegralValueGenerator<Long> {
        private long value;

        LongValueGenerator(Long startValue) {
            value = startValue == null ? 0 : startValue;
        }

        @Override
        public Long nextValue() {
            return value++;
        }

        @Override
        public void setValueGreaterThan(Number newValue) {
            if (newValue != null && newValue.longValue() >= value) {
                value = newValue.longValue() + 1L;
            }
        }
    }

    private static class ShortValueGenerator implements IntegralValueGenerator<Short> {
        private short value;

        ShortValueGenerator(Short startValue) {
            value = startValue == null ? 0 : startValue;
        }

        @Override
        public Short nextValue() {
            return value++;
        }

        @Override
        public void setValueGreaterThan(Number newValue) {
            if (newValue != null && newValue.shortValue() >= value) {
                value = (short)(newValue.shortValue() + 1);
            }
        }
    }
}
