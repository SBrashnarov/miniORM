package orm;

import strategies.SchemaInitializationStrategy;
import strategies.Strategies;
import strategies.StrategyFactory;

import java.sql.Connection;
import java.sql.SQLException;

public class EntityManagerBuilder {

    private Connection connection;
    private String dbName;
    private SchemaInitializationStrategy strategy;

    public Connector configureConnectionString() {
        return new Connector(this);
    }

    public EntityManagerBuilder setStrategy(Enum<Strategies> strategyClassName) {
        this.strategy = StrategyFactory.getStrategy(strategyClassName, this.connection, this.dbName);
        return this;
    }

    public DBContext build() throws SQLException, ClassNotFoundException {
        return new EntityManager(this.connection, this.dbName, this.strategy);
    }

    public EntityManagerBuilder setDataSource(String dbName) {
        this.dbName = dbName;
        return this;
    }

    void setConnection(Connection connection) {
        this.connection = connection;
    }
}
