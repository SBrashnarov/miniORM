package strategies;

import java.sql.*;
import java.util.*;

class TableCreator {

    private static final String CREATE_TABLE_QUERY = "CREATE TABLE IF NOT EXISTS %s.%s(%s)";
    private static final String ALTER_TABLE_QUERY = "ALTER TABLE %s.%s %s;";

    private Connection connection;
    private String dbName;

    TableCreator(Connection connection, String dbName) {
        this.connection = connection;
        this.dbName = dbName;
    }

    void doCreate(String tableName, Map<String, String> tableColumns) throws SQLException {
        List<String> columnsToAdd = new ArrayList<>();

        for (Map.Entry<String, String> column : tableColumns.entrySet()) {
            String columnName = column.getKey();
            String columnType = this.getDatabaseType(column.getValue());

            columnsToAdd.add(columnName + " " + columnType);
        }

        String query = String.format(CREATE_TABLE_QUERY,
                this.dbName,
                tableName,
                String.join(", ", columnsToAdd));

        Statement stmt = this.connection.createStatement();
        stmt.execute(query);
    }

    void doUpdate(String tableName, Map<String, String> tableColumns) throws SQLException {
        List<String> dbColumnsByName = this.getDbTableColumns(tableName);

        for (Map.Entry<String, String> column : tableColumns.entrySet()) {
            String columnName = column.getKey();

            if (!this.checkIfFieldExists(columnName, tableName)) {
                this.addColumn(tableName, column);
            } else {
                dbColumnsByName.remove(columnName);
            }
        }

        for (String column : dbColumnsByName) {
            this.removeColumn(tableName, column);
        }
    }

    String getPrimaryKey(String tableName) throws SQLException {
        try (ResultSet pkColumns = connection.getMetaData().getPrimaryKeys(this.dbName, null, tableName)) {
            if (pkColumns.first()) {
                return pkColumns.getString("COLUMN_NAME");
            }
            return null;
        }
    }

    void dropPrimaryKey(String tableName, String pkColumnName) throws SQLException {
        String dropPkString = " MODIFY COLUMN " + pkColumnName + " INT, DROP PRIMARY KEY";
        String dropPkQueryString = String.format(ALTER_TABLE_QUERY,
                this.dbName,
                tableName,
                dropPkString
        );

        Statement stmt = this.connection.createStatement();
        stmt.executeUpdate(dropPkQueryString);
    }

    void setPrimaryKey(String tableName, String pkColumnName) throws SQLException {
        String setPkString = " MODIFY COLUMN " + pkColumnName + " INT NOT NULL AUTO_INCREMENT, " +
                " ADD CONSTRAINT PK_" + pkColumnName + " PRIMARY KEY(" + pkColumnName + ")";

        String querySrting = String.format(ALTER_TABLE_QUERY,
                this.dbName,
                tableName,
                setPkString
        );

        Statement stmt = this.connection.createStatement();
        stmt.executeUpdate(querySrting);
    }


    boolean checkIfTableExists(String tableName) throws SQLException {
        String query = "SELECT table_name " +
                "FROM information_schema.tables " +
                "WHERE table_schema = '" + this.dbName + "' AND table_name = '" + tableName + "';";

        Statement statement = this.connection.createStatement();
        ResultSet rs = statement.executeQuery(query);

        return rs.first();
    }


    private void removeColumn(String tableName, String columnName) throws SQLException {
        String alterString = " DROP `" + columnName + "`";

        String query = String.format(ALTER_TABLE_QUERY, this.dbName, tableName, alterString);
        PreparedStatement prepStatement = this.connection.prepareStatement(query);

        prepStatement.executeUpdate();
    }

    private void addColumn(String tableName, Map.Entry<String, String> columnNameAndType) throws SQLException {
        String columnName = columnNameAndType.getKey();
        String columnType = this.getDatabaseType(columnNameAndType.getValue());
        String queryString = " ADD " + columnName + " " + columnType;

        String query = String.format(ALTER_TABLE_QUERY, this.dbName, tableName, queryString);
        PreparedStatement prepStatement = this.connection.prepareStatement(query);

        prepStatement.execute();
    }

    private String getDatabaseType(String columnType) {
        switch (columnType) {
            case "int":
            case "Integer":
                return "INT";
            case "String":
                return "VARCHAR(50)";
            case "Date":
                return "DATETIME";
            default:
                throw new UnsupportedOperationException("Not supported Java type!");
        }
    }

    private boolean checkIfFieldExists(String columnName, String tableName) throws SQLException {
        String query = "SELECT column_name " +
                "FROM information_schema.columns " +
                "WHERE table_name = '" + tableName + "' AND column_name = '" + columnName + "';";

        Statement statement = this.connection.createStatement();
        ResultSet rs = statement.executeQuery(query);

        return rs.first();
    }

    private List<String> getDbTableColumns(String tableName) throws SQLException {
        String select = "column_name";

        String query = "SELECT column_name " +
                "FROM information_schema.columns " +
                "WHERE table_schema = '" + this.dbName + "' AND table_name = '" + tableName + "';";

        Statement statement = this.connection.createStatement();
        ResultSet rs = statement.executeQuery(query);

        List<String> tableColumns = new ArrayList<>();

        while (rs.next()) {
            tableColumns.add(rs.getString(select));
        }

        return tableColumns;
    }
}