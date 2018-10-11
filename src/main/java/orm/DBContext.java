package orm;

import java.lang.reflect.InvocationTargetException;
import java.sql.SQLException;

public interface DBContext {

    <E> boolean  persist(E entity) throws IllegalAccessException, SQLException, InstantiationException, ClassNotFoundException;

    <E> void doDelete(E table) throws Exception;

    <E> Iterable<E> find(Class<E> table) throws IllegalAccessException, SQLException, InstantiationException, ClassNotFoundException, NoSuchMethodException, InvocationTargetException;

    <E> Iterable<E> find(Class<E> table, String where) throws SQLException, IllegalAccessException, InstantiationException, ClassNotFoundException, NoSuchMethodException, InvocationTargetException;

    <E> E findFirst(Class<E> table) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException, SQLException, ClassNotFoundException;

    <E> E findFirst(Class<E> table, String where) throws SQLException, InstantiationException, IllegalAccessException, ClassNotFoundException, NoSuchMethodException, InvocationTargetException;
}
