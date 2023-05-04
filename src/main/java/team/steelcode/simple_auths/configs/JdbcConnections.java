package team.steelcode.simple_auths.configs;


import org.hibernate.SessionFactory;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import team.steelcode.simple_auths.data.db.entity.PlayerEntityDB;

import java.util.HashMap;
import java.util.Properties;

public class JdbcConnections {

    private static SessionFactory sessionFactory;

    public static SessionFactory getSessionFactory() {
        return sessionFactory;
    }

    public static void initializeSessionFactory() {
        if (sessionFactory == null) {
            try {

                Configuration config = new Configuration();

                config.addAnnotatedClass(PlayerEntityDB.class);

                StandardServiceRegistry serviceRegistry = new StandardServiceRegistryBuilder().build();


                sessionFactory = config.buildSessionFactory(serviceRegistry);
            } catch (Exception e) {
                e.printStackTrace();
            }

        }
    }
}