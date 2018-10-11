package orm;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

public class Connector {

    private static final String CONNECTION_STRING = "%s:%s://%s:%s";

    private EntityManagerBuilder builder;
    private String adapter;
    private String driver;
    private String host;
    private String port;
    private String user;
    private String password;

    Connector(EntityManagerBuilder builder) {
        this.builder = builder;
    }

    public Connector setAdapter(String adapter) {
        this.adapter = adapter;
        return this;
    }

    public Connector setDriver(String driver) {
        this.driver = driver;
        return this;
    }

    public Connector setHost(String host) {
        this.host = host;
        return this;
    }

    public Connector setPort(String port) {
        this.port = port;
        return this;
    }

    public Connector setUser(String user) {
        this.user = user;
        return this;
    }

    public Connector setPassword(String password) {
        this.password = password;
        return this;
    }

    public EntityManagerBuilder createConnection() throws SQLException {
        Properties props = new Properties();
        props.setProperty("user", this.user);
        props.setProperty("password", this.password);

        Connection conn = DriverManager.getConnection(
                String.format(CONNECTION_STRING,
                        this.adapter,
                        this.driver,
                        this.host,
                        this.port), props);

        this.builder.setConnection(conn);
        return this.builder;
    }
}
