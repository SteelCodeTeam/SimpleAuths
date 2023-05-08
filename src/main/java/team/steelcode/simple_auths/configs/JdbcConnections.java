package team.steelcode.simple_auths.configs;


import net.minecraft.CrashReport;
import net.minecraft.client.Minecraft;
import org.hibernate.SessionFactory;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import team.steelcode.simple_auths.SimpleAuths;
import team.steelcode.simple_auths.data.db.entity.PlayerEntityDB;
import team.steelcode.simple_auths.setup.ConfigSpecRegister;

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

                StandardServiceRegistry serviceRegistry = new StandardServiceRegistryBuilder()
                        .applySetting(Environment.DRIVER, "com.mysql.cj.jdbc.Driver")
                        .applySetting(Environment.CURRENT_SESSION_CONTEXT_CLASS, "thread")
                        .applySetting(Environment.HBM2DDL_AUTO, "update")
                        .applySetting(Environment.URL, ConfigSpecRegister.DB_URL.get())
                        .applySetting(Environment.USER, ConfigSpecRegister.DB_USER.get())
                        .applySetting(Environment.PASS, ConfigSpecRegister.DB_PASSWORD.get())
                        .applySetting(Environment.SHOW_SQL, ConfigSpecRegister.DB_SHOW_SQL.get())
                        .build();

                sessionFactory = config.buildSessionFactory(serviceRegistry);

            } catch (Exception e) {
                e.printStackTrace();

                Minecraft.crash(new CrashReport("CANNOT COMMUNICATE WITH DATABASE. PLEASE CHECK CONFIG", e));
                Minecraft.getInstance().close();

            }
        }
    }
}