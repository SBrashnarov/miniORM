import entities.Town;
import entities.User;
import orm.*;
import strategies.Strategies;

import java.util.*;

public class Main {
    public static void main(String[] args) throws Exception {

        EntityManagerBuilder entityBuilder = new EntityManagerBuilder();

        DBContext entityManager =
                entityBuilder.configureConnectionString()
                        .setAdapter("jdbc")
                        .setDriver("mysql")
                        .setHost("localhost")
                        .setPort("3306")
                        .setUser("userName")
                        .setPassword("password")
                        .createConnection()
                        .setDataSource("dbName")
                        .setStrategy(Strategies.UPDATE)
                        .build();

        User user = new User("userName", 19, new Date(), "Sofia");

        //persist() must be called every time when you want to save an instance of an Entity class in the database
        entityManager.persist(user);

        Town town = new Town("Sofia");
        entityManager.persist(town);

        //findFirst() will return the first record in the database, if it exists otherwise null
        User firstUser = entityManager.findFirst(User.class);

        /**
         * To retrieve the first instance of an Entity class from the database,
         * which complies with a given filter,
         * the filter must be valid SQL WHERE clause
         */
        User nullUser = entityManager.findFirst(User.class, " id = 100 ");

        List<Town> towns = (List<Town>) entityManager.find(Town.class, " id BETWEEN 1 AND 10");

        //Remove the passed entity instance from the database
        entityManager.doDelete(firstUser);
    }
}
