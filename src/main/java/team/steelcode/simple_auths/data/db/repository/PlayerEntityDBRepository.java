package team.steelcode.simple_auths.data.db.repository;


import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.query.Query;
import team.steelcode.simple_auths.configs.JdbcConnections;
import team.steelcode.simple_auths.data.LoggedPlayerCache;
import team.steelcode.simple_auths.data.db.entity.PlayerEntityDB;
import team.steelcode.simple_auths.data.enums.IStatus;
import team.steelcode.simple_auths.data.enums.LoginStatus;
import team.steelcode.simple_auths.data.enums.RegisterStatus;

import java.util.Optional;
import java.util.UUID;

public class PlayerEntityDBRepository {


    public static IStatus registerUser(String username, String password, UUID uuid) {
        Transaction transaction = null;

        try (Session session = JdbcConnections.getSessionFactory().openSession()) {
            transaction = session.beginTransaction();

            String hql = "SELECT PET " +
                            "FROM player_entity_table AS PET " +
                            "WHERE PET.username = :username";

            Query<PlayerEntityDB> query = session.createQuery(hql, PlayerEntityDB.class);
            query.setParameter("username", username);

            Optional<PlayerEntityDB> playerQuery = query.uniqueResultOptional();

            if (playerQuery.isPresent()) {
                if (transaction != null) {
                    transaction.rollback();
                }
                return RegisterStatus.ALREADY_EXISTS;
            } else {
                PlayerEntityDB playerRegistered = new PlayerEntityDB(username, password, uuid);
                session.persist(playerRegistered);
                transaction.commit();

                LoggedPlayerCache.addPlayer(playerRegistered);

                return RegisterStatus.SUCCESSFULLY_REGISTER;
            }
        } catch (Exception e) {

            if (transaction != null) {
                transaction.rollback();
            }

            e.printStackTrace();

            return RegisterStatus.ERROR;

        }
    }

    public static IStatus loginUser(String username, String password) {
        Transaction transaction = null;
        if (LoggedPlayerCache.playerIsLoggedByUsername(username)) {
            return LoginStatus.ALREADY_LOGGED;
        }

        try (Session session = JdbcConnections.getSessionFactory().openSession()) {
            transaction = session.beginTransaction();

            String hql = "SELECT PET " +
                    "FROM player_entity_table AS PET " +
                    "WHERE PET.username = :username";

            Query<PlayerEntityDB> query = session.createQuery(hql, PlayerEntityDB.class);
            query.setParameter("username", username);

            Optional<PlayerEntityDB> playerQuery = query.uniqueResultOptional();

            if (playerQuery.isPresent()) {
                PlayerEntityDB player = playerQuery.get();


                if (player.getHashedPassword().equals(password)) {
                    LoggedPlayerCache.addPlayer(player);
                    return LoginStatus.SUCCESSFULLY_LOGGED;
                } else {
                    return LoginStatus.WRONG_PASSWORD;
                }


            } else {
                return LoginStatus.NOT_USER_FOUND;
            }

        } catch (Exception e) {

            if (transaction != null) {
                transaction.rollback();
            }

            e.printStackTrace();

            return LoginStatus.ERROR;

        }


    }

    public static IStatus changePassword(String username, String hashedPassword) {
        Transaction transaction = null;

        try (Session session = JdbcConnections.getSessionFactory().openSession()) {
            transaction = session.beginTransaction();

            String hql = "SELECT PET " +
                    "FROM player_entity_table AS PET " +
                    "WHERE PET.username = :username";

            Query<PlayerEntityDB> query = session.createQuery(hql, PlayerEntityDB.class);
            query.setParameter("username", username);

            Optional<PlayerEntityDB> playerQuery = query.uniqueResultOptional();

            if (playerQuery.isPresent()) {
                PlayerEntityDB player = playerQuery.get();


                if (LoggedPlayerCache.playerIsLoggedByUsername(username)) {
                    if (player.getHashedPassword().equals(hashedPassword)) {
                        return RegisterStatus.SAME_PASSWORD;
                    } else {
                        player.setHashedPassword(hashedPassword);
                        session.merge(player);

                        transaction.commit();

                        return RegisterStatus.PASSWORD_CHANGED;
                    }

                } else {
                    return LoginStatus.NOT_LOGGED;
                }

            } else {
                return LoginStatus.NOT_USER_FOUND;
            }

        } catch (Exception e) {

            if (transaction != null) {
                transaction.rollback();
            }

            e.printStackTrace();

            return LoginStatus.ERROR;

        }
    }

    public static IStatus unregisterPlayer(String username) {
            Transaction transaction = null;

            try (Session session = JdbcConnections.getSessionFactory().openSession()) {
                transaction = session.beginTransaction();

                String hql = "SELECT PET " +
                        "FROM player_entity_table AS PET " +
                        "WHERE PET.username = :username";

                Query<PlayerEntityDB> query = session.createQuery(hql, PlayerEntityDB.class);
                query.setParameter("username", username);

                Optional<PlayerEntityDB> playerQuery = query.uniqueResultOptional();

                if (playerQuery.isPresent()) {
                    session.remove(playerQuery.get());
                    transaction.commit();

                    LoggedPlayerCache.removePlayerByUsername(username);

                    return RegisterStatus.SUCCESSFULLY_UNREGISTER;
                } else {

                    return RegisterStatus.PLAYER_DOESNT_EXISTS;
                }
            } catch (Exception e) {

                if (transaction != null) {
                    transaction.rollback();
                }

                e.printStackTrace();

                return RegisterStatus.ERROR;

            }

    }
}
