package team.steelcode.simple_auths.data.db.service;


import team.steelcode.simple_auths.data.db.entity.PlayerEntityDB;
import team.steelcode.simple_auths.data.db.repository.PlayerEntityDBRepository;
import team.steelcode.simple_auths.data.enums.IStatus;

import java.util.UUID;

public class PlayerEntityDBService {

    public static IStatus registerUser(String username, String hashedPassword, UUID uuid) {
        return PlayerEntityDBRepository.registerUser(username, hashedPassword, uuid);

    }
    public static IStatus loginUser(String username, String hashedPassword) {
        return PlayerEntityDBRepository.loginUser(username, hashedPassword);

    }

    public static IStatus changePasswordFromUsername(String username, String hashedPassword) {

        return PlayerEntityDBRepository.changePassword(username, hashedPassword);
    }

    public static IStatus unregisterPlayerByUsername(String username) {
        return PlayerEntityDBRepository.unregisterPlayer(username);
    }
}
