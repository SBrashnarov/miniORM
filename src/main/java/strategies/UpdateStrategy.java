package strategies;

import java.sql.*;
import java.util.Map;
import java.util.Set;

public class UpdateStrategy extends SchemaInitializationStrategyAbstract {

    private static final String CREATE_QUERY = "CREATE DATABASE IF NOT EXISTS `%s`;";

    UpdateStrategy(Connection connection, String dbName) {
        super(connection, dbName);
    }

    @Override
    public void execute() throws SQLException, ClassNotFoundException {
        Statement createDbStatement = super.connection.createStatement();
        String queryString = String.format(CREATE_QUERY, super.dbName);
        createDbStatement.execute(queryString);

        this.doUpdate(super.scanForEntities());
    }

    private void doUpdate(Set<Class<?>> entities) throws SQLException {
        for (Class entity : entities) {
            String tableName = ClassEntityScanner.getTableName(entity);
            String pkColumnName = ClassEntityScanner.getPrimaryKeyField(entity);
            Map<String, String> columns = ClassEntityScanner.getColumns(entity);

            if (!super.tableCreator.checkIfTableExists(tableName)) {
                super.tableCreator.doCreate(tableName, columns);
                super.tableCreator.setPrimaryKey(tableName, pkColumnName);
            } else {
                String pk = super.tableCreator.getPrimaryKey(tableName);

                if (pk != null) {
                    super.tableCreator.dropPrimaryKey(tableName, pk);
                }

                super.tableCreator.doUpdate(tableName, columns);
                super.tableCreator.setPrimaryKey(tableName, pkColumnName);
            }
        }
    }
}
