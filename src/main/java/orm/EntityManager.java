package orm;

import annotations.Column;
import annotations.Entity;
import annotations.PrimaryKey;
import strategies.SchemaInitializationStrategy;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Date;

public class EntityManager implements DBContext {

    private static final String DATE_FORMAT_STRING = "yyyy-M-dd hh:mm:ss";
    private static final String UPDATE_QUERY_STRING = "UPDATE %s.%s SET %s WHERE %s";
    private static final String INSERT_QUERY_STRING = "INSERT INTO %s.%s(%s) VALUES(%s);";
    private static final String DELETE_QUERY_STRING = "DELETE FROM %s.%s WHERE %s";
    private static final String SELECT_QUERY_STRING = "SELECT * FROM %s.%s %s;";

    private Connection connection;
    private String dbName;
    private SchemaInitializationStrategy strategy;

    EntityManager(Connection connection, String dbName, SchemaInitializationStrategy strategy) throws SQLException, ClassNotFoundException {
        this.connection = connection;
        this.dbName = dbName;

        this.strategy = strategy;
        this.strategy.execute();
    }

    public <E> boolean persist(E entity) throws IllegalAccessException, SQLException {
        Field primaryKey = this.getPrimaryKeyField(entity.getClass());
        primaryKey.setAccessible(true);
        Object value = primaryKey.get(entity);

        if (value == null || (Integer) value <= 0) {
            return this.doInsert(entity, primaryKey);
        }

        return this.doUpdate(entity);
    }

    public <E> Iterable<E> find(Class<E> table) throws IllegalAccessException, InstantiationException, InvocationTargetException, SQLException {
        return this.find(table, "");
    }

    public <E> Iterable<E> find(Class<E> table, String where) throws IllegalAccessException, InstantiationException, SQLException, InvocationTargetException {
        String whereString = "";
        if (!where.trim().isEmpty()) {
            whereString = " WHERE " + where;
        }

        String queryString = String.format(SELECT_QUERY_STRING,
                this.dbName,
                this.getTableName(table),
                whereString
        );

        List<E> entities = new ArrayList<>();

        Statement stmt = this.connection.createStatement();
        ResultSet rs = stmt.executeQuery(queryString);

        while (rs.next()) {
            Constructor<E> entityConstructor = this.getEmptyConstructor(table);
            E entity = entityConstructor.newInstance();

            this.fillEntity(entity, rs);

            entities.add(entity);
        }

        return entities;
    }

    public <E> E findFirst(Class<E> table) throws IllegalAccessException, InstantiationException, InvocationTargetException, SQLException {
        return this.findFirst(table, "");
    }

    public <E> E findFirst(Class<E> table, String where) throws InstantiationException, IllegalAccessException, InvocationTargetException, SQLException {
        String whereString = " LIMIT 1";
        if (!where.trim().isEmpty()) {
            whereString = " WHERE " + where +  " LIMIT 1";
        }

        String query = String.format(SELECT_QUERY_STRING,
                this.dbName,
                this.getTableName(table),
                whereString
        );

        Statement stmt = this.connection.createStatement();
        ResultSet rs = stmt.executeQuery(query);

        E entity = null;

        if (rs.first()) {
            Constructor<E> entityConstructor = this.getEmptyConstructor(table);
            entity = entityConstructor.newInstance();
            this.fillEntity(entity, rs);
        }

        return entity;
    }

    public <E> void doDelete(E entity) throws Exception {
        Class<?> entityClass = entity.getClass();

        String tableName = this.getTableName(entityClass);

        Field primaryKey = this.getPrimaryKeyField(entityClass);
        primaryKey.setAccessible(true);

        String pkColumnName = primaryKey.getAnnotation(Column.class).name();
        Object pkValue = primaryKey.get(entity);

        String where = pkColumnName + " = " + pkValue;

        String queryString = String.format(DELETE_QUERY_STRING,
                this.dbName,
                tableName,
                where
        );

        this.connection.createStatement()
                .executeUpdate(queryString);
    }

    private Field getPrimaryKeyField(Class entity) {
        Field[] fields = entity.getDeclaredFields();
        for (Field field : fields) {
            if (field.isAnnotationPresent(PrimaryKey.class)) return field;
        }

        throw new UnsupportedOperationException("Entity does not have primary key!");
    }

    private <E> boolean doUpdate(E entity) throws SQLException, IllegalAccessException {
        PreparedStatement prepStatement = constructStatementQuery(entity);

        this.setStatementValues(entity, prepStatement);

        return prepStatement.execute();
    }

    private <E> boolean doInsert(E entity, Field primaryKey) throws IllegalAccessException, SQLException {
        String tableName = this.getTableName(entity.getClass());

        List<String> columns = new ArrayList<>();
        List<String> values = new ArrayList<>();

        Field[] fields = entity.getClass().getDeclaredFields();
        for (Field field : fields) {
            field.setAccessible(true);

            if (field.getName().equals(primaryKey.getName())) {
                continue;
            }

            columns.add(this.getColumnName(field));
            Object value = field.get(entity);

            if (value instanceof Date) {
                String dateValue = "'" + new SimpleDateFormat(DATE_FORMAT_STRING).format(value) + "'";
                values.add(dateValue);
            } else {
                values.add("'" + value + "'");
            }
        }

        String queryString = String.format(INSERT_QUERY_STRING,
                this.dbName,
                tableName,
                String.join(", ", columns),
                String.join(", ", values)
        );

        return this.connection.prepareStatement(queryString).execute();
    }

    private <E> void fillEntity(E entity, ResultSet rs) throws SQLException, IllegalAccessException {
        Field[] fields = entity.getClass().getDeclaredFields();
        for (Field field : fields) {
            field.setAccessible(true);
            field.set(entity, this.getFieldValue(field, rs));
        }
    }

    private Object getFieldValue(Field field, ResultSet rs) throws SQLException {
        String columnName = field.getAnnotation(Column.class).name();

        switch (field.getType().getSimpleName()) {
            case "Integer":
            case "int":
                return rs.getInt(columnName);
            case "String":
                return rs.getString(columnName);
            case "Date":
                return rs.getDate(columnName);
            default:
                return null;
        }
    }

    private <E> void setStatementValues(E entity, PreparedStatement stmt) throws IllegalAccessException, SQLException {
        int paramIndex = 0;
        Field[] fields = entity.getClass().getDeclaredFields();

        for (Field field : fields) {
            paramIndex++;
            field.setAccessible(true);

            Object value = field.get(entity);

            if (field.isAnnotationPresent(PrimaryKey.class)) {
                this.setStatementValue(stmt, fields.length, value);
                paramIndex--;
            } else {
                this.setStatementValue(stmt, paramIndex, value);
            }
        }
    }

    private void setStatementValue(PreparedStatement stmt, int index, Object value) throws SQLException {
        String fieldJavaType = value.getClass().getSimpleName();

        switch (fieldJavaType) {
            case "int":
            case "Integer":
                stmt.setInt(index, (Integer) value);
                break;
            case "String":
                stmt.setString(index, (String) value);
                break;
            case "Date":
                stmt.setString(index, new SimpleDateFormat(DATE_FORMAT_STRING).format(value));
                break;
            default:
                throw new UnsupportedOperationException("Not supported Java type!");
        }
    }

    private <E> PreparedStatement constructStatementQuery(E entity) throws SQLException {
        String tableName = this.getTableName(entity.getClass());
        String where = "";

        Field[] fields = entity.getClass().getDeclaredFields();

        List<String> columns = new ArrayList<>();
        for (Field field : fields) {
            field.setAccessible(true);

            if (field.isAnnotationPresent(PrimaryKey.class)) {
                where = field.getAnnotation(Column.class).name() + " = ?;";
                continue;
            }

            columns.add(field.getAnnotation(Column.class).name() + " = ?");
        }

        String stmString = String.format(UPDATE_QUERY_STRING,
                this.dbName,
                tableName,
                String.join(", ", columns),
                where
        );

        return this.connection.prepareStatement(stmString);
    }

    private String getColumnName(Field entityField) {
        if (entityField.isAnnotationPresent(Column.class)) {
            return entityField.getAnnotation(Column.class).name();
        } else {
            return null;
        }

    }

    private String getTableName(Class<?> entityClass) {
        if (entityClass.isAnnotationPresent(Entity.class)) {
            return entityClass.getAnnotation(Entity.class).name();
        } else {
            throw new IllegalArgumentException("Invalid Entity class");
        }
    }

    private <E> Constructor<E> getEmptyConstructor(Class<E> table) {
        Constructor[] allConstructors = table.getDeclaredConstructors();

        for (Constructor ctor : allConstructors) {
            Class<?>[] parameterTypes = ctor.getParameterTypes();
            if (parameterTypes.length == 0) {
                return (Constructor<E>) ctor;
            }
        }
        throw new IllegalArgumentException("Entity must have public default constructor");
    }
}