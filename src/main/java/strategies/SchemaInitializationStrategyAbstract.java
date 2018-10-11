package strategies;

import java.sql.Connection;
import java.util.Set;

abstract class SchemaInitializationStrategyAbstract implements SchemaInitializationStrategy {

    protected Connection connection;
    protected String dbName;
    protected TableCreator tableCreator;

    SchemaInitializationStrategyAbstract(Connection connection, String dbName) {
        this.connection = connection;
        this.dbName = dbName;
        this.tableCreator = new TableCreator(connection, dbName);
    }

    Set<Class<?>> scanForEntities() throws ClassNotFoundException {
        return ClassEntityScanner.getEntities(System.getProperty("user.dir"));
    }
}
