package team.steelcode.simple_auths.data.enums;

import org.hibernate.type.EntityType;
import team.steelcode.simple_auths.data.language_providers.ModLanguageProviderES;

import java.util.ArrayList;

public enum LoginStatus implements IStatus {
    ALREADY_LOGGED(StatusType.KO_WARN, ModLanguageProviderES.PREFIX + "already_logged"),
    NOT_LOGGED(StatusType.KO_WARN, ModLanguageProviderES.PREFIX + "relogin_needed"),
    SUCCESSFULLY_LOGGED(StatusType.OK, ModLanguageProviderES.PREFIX + "login_successful"),
    WRONG_PASSWORD(StatusType.KO_WARN, ModLanguageProviderES.PREFIX + "password_incorrect"),
    NOT_USER_FOUND(StatusType.OK_WARN, ModLanguageProviderES.PREFIX + "doesnt_exists"),
    ERROR(StatusType.KO_ERROR, ModLanguageProviderES.PREFIX + "unexpected_error");

    private StatusType status;
    private String description;

    LoginStatus(StatusType status, String description) {
        this.status = status;
        this.description = description;
    }

    @Override
    public StatusType getStatus() {
        return status;
    }

    @Override
    public String getDescription() {
        return description;
    }
}
