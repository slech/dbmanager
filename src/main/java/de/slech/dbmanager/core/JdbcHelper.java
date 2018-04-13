package de.slech.dbmanager.core;

import de.slech.dbmanager.util.StringUtils;
import de.slech.dbmanager.exeption.SystemException;
import de.slech.dbmanager.data.DataSet;
import de.slech.dbmanager.data.Row;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.*;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Helper to execute JDBC Statements
 */
class JdbcHelper {

    private static final Logger LOGGER = LoggerFactory.getLogger(JdbcHelper.class);

    private final DataSource dataSource;

    JdbcHelper(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    void deleteAllFromTables(List<String> tablenames) {
        executeUpdate(tablenames.stream().map(tablename -> "delete from " + tablename).collect(Collectors.toList()));
    }


    void executeUpdate(String sqlStatement) {
        executeUpdate(Collections.singletonList(sqlStatement));
    }

    DataSet executeQuery(String tableName, Collection<String> searchedColumns, Map<String, Object> columnConditions) {
        final ArrayList<Map.Entry<String, Object>> columns = new ArrayList<>(columnConditions.entrySet());
        String sqlString = String.format("select %s from %s where %s",
                StringUtils.concatStrings(searchedColumns, ", "),
                tableName,
                createWhereClause(columns));
        LOGGER.debug("Wird ausgeführt: " +sqlString);
        try(Connection connection = dataSource.getConnection();
            PreparedStatement sqlStmt = connection.prepareStatement(sqlString)) {
            setParameters(sqlStmt, columns);
            final ResultSet resultSet = sqlStmt.executeQuery();
            DataSet result = new DataSet();
            while (resultSet.next()) {
                final Row resultRow = new Row();
                for (String colName : searchedColumns) {
                    resultRow.addColum(colName, resultSet.getObject(colName));
                }
                result.addRow(resultRow);
            }

            return result;
        } catch (SQLException e) {
            throw new SystemException(e);
        }

    }

    void executeInsert(String tableName, DataSet dataSet) {
        try (Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);
            final List<Row> rows = dataSet.stream().collect(Collectors.toList());
            for (Row row : rows) {
                final ArrayList<Map.Entry<String, Object>> columns = new ArrayList<>(row.getColumns().entrySet());
                final String sql = String.format("insert into %s(%s) values(%s)",
                        tableName,
                        StringUtils.concatStrings(columns.stream().map(Map.Entry::getKey).collect(Collectors.toList()), ", "),
                        StringUtils.concatStrings(Collections.nCopies(columns.size(), "?"), ", "));
                try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                    setParameters(stmt, columns);
                    stmt.executeUpdate();
                    connection.commit();
                } catch (Exception e) {
                    connection.rollback();
                    throw e;
                }
            }
        } catch (SQLException e) {
            throw new SystemException(e);
        }
    }

    private void setParameters(PreparedStatement sqlStmt, ArrayList<Map.Entry<String, Object>> columns)
            throws SQLException {
        for (int i = 0; i < columns.size(); i++) {
            sqlStmt.setObject(i+1, columns.get(i).getValue());
        }
    }

    private String createWhereClause(ArrayList<Map.Entry<String, Object>> columns) {
        return StringUtils.concatStrings(
                columns.stream().map(column -> column.getKey() + " = ?").collect(Collectors.toList()),
                " and ");
    }

    private void executeUpdate(List<String> sqlStatements) {

        try (Connection connection = dataSource.getConnection()) {

            try ( Statement stmt = connection.createStatement()) {
                connection.setAutoCommit(false);
                for (String sqlStatement : sqlStatements) {
                    stmt.executeUpdate(sqlStatement);
                }
                connection.commit();
            }
            catch (Exception e) {
                connection.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new SystemException(e);
        }
    }
}
