package strategies;

import annotations.Column;
import annotations.Entity;
import annotations.PrimaryKey;

import java.io.File;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

public class ClassEntityScanner {

    private static final String DIR_SEPARATOR = File.separator;
    private static final Pattern PROJECT_DIRECTORY_R = Pattern.compile(".*classes" + DIR_SEPARATOR);
    private static final Pattern DIRECTORY_SEPARATOR_R = Pattern.compile(DIR_SEPARATOR);
    private static final Pattern FILE_EXTENSION_R = Pattern.compile("\\.class");

    public ClassEntityScanner() {
    }

    static Set<Class<?>> getEntities(String path) throws ClassNotFoundException {
        Set<Class<?>> entities = new HashSet<>();
        File directory = new File(path);
        File[] files = directory.listFiles();

        for (File file : files) {
            if (file.isDirectory()) {
                entities.addAll(getEntities(file.getPath()));
            }

            if (file.getPath().endsWith(".class")) {
                String className = file.getPath();

                String classNamePath = getPackageNameFromPath(className);

                Class<?> entity = Class.forName(classNamePath);
                if (entity.isAnnotationPresent(Entity.class)) {
                    entities.add(entity);
                }
            }
        }

        return entities;
    }

    static Map<String, String> getColumns(Class<?> entity) {
        Map<String, String> fieldNameAndType = new HashMap<>();
        Field[] entityFields = entity.getDeclaredFields();

        for (Field entityField : entityFields) {
            entityField.setAccessible(true);

            if (entityField.isAnnotationPresent(Column.class)) {
                String fieldName = entityField.getAnnotation(Column.class).name();
                String fieldType = entityField.getType().getSimpleName();
                fieldNameAndType.put(fieldName, fieldType);
            }
        }
        return fieldNameAndType;
    }

    static String getTableName(Class<?> entityClass) {
        if (entityClass.isAnnotationPresent(Entity.class)) {
            return entityClass.getAnnotation(Entity.class).name();
        } else {
            return null;
        }
    }

    static String getPrimaryKeyField(Class entity) {
        Field[] fields = entity.getDeclaredFields();
        for (Field field : fields) {
            if (field.isAnnotationPresent(PrimaryKey.class)) return field.getAnnotation(Column.class).name();
        }

        throw new UnsupportedOperationException("Entity does not have primary key!");
    }

    private static String getPackageNameFromPath(String className) {
        className = PROJECT_DIRECTORY_R.matcher(className).replaceFirst("");
        className = FILE_EXTENSION_R.matcher(className).replaceFirst("");
        className = DIRECTORY_SEPARATOR_R.matcher(className).replaceAll(".");

        return className;
    }
}