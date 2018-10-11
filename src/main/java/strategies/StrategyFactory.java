package strategies;

import java.sql.Connection;

public class StrategyFactory {

    public static SchemaInitializationStrategy getStrategy(Enum<Strategies> strategy, Connection connection, String dbName) {

        switch (strategy.toString()) {
            case "update":
                return new UpdateStrategy(connection, dbName);
            case "dropcreate":
                return new DropCreateStrategy(connection, dbName);
            default:
                return null;
        }
    }
}
