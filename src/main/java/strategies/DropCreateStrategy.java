package strategies;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map;
import java.util.Set;

public class DropCreateStrategy extends SchemaInitializationStrategyAbstract {

    private static final String DROP_QUERY = "DROP DATABASE IF EXISTS `%s`;";
    private static final String CREATE_QUERY = "CREATE DATABASE `%s`;";

    DropCreateStrategy(Connection connection, String dbName) {
        super(connection, dbName);
    }

    @Override
    public void execute() throws SQLException, ClassNotFoundException {
        //drop database if exists
        Statement stmt = super.connection.createStatement();
        stmt.executeUpdate(String.format(DROP_QUERY, super.dbName));

        //create database
        stmt.executeUpdate(String.format(CREATE_QUERY, super.dbName));

        //for each entity, TableCreator class get table structure by ClassEntityScanner and create table
        this.createTables(super.scanForEntities());
    }

    private void createTables(Set<Class<?>> entities) throws SQLException {
        for (Class entity : entities) {
            //get database table name from entity using reflection
            String tableName = ClassEntityScanner.getTableName(entity);

            /*
            for each entity field, which is annotated @Column,
            get its column name and SQL data type
            */
            Map<String, String> columns = ClassEntityScanner.getColumns(entity);

            //get Primary key column name
            String pkColumnName = ClassEntityScanner.getPrimaryKeyField(entity);

            //create table given the table name and each column's name, SQL data type and PK
            super.tableCreator.doCreate(tableName, columns);
            super.tableCreator.setPrimaryKey(tableName, pkColumnName);
        }
    }
}
