package team.steelcode.simple_auths.data.enums;

import team.steelcode.simple_auths.data.language_providers.ModLanguageProviderES;

public enum RegisterStatus implements IStatus {
    ALREADY_EXISTS(StatusType.KO_WARN, ModLanguageProviderES.PREFIX + "already_exists"),
    PLAYER_DOESNT_EXISTS(StatusType.KO_WARN, ModLanguageProviderES.PREFIX + "doesnt_exists"),
    SUCCESSFULLY_REGISTER(StatusType.OK, ModLanguageProviderES.PREFIX + "register_successful"),
    SUCCESSFULLY_UNREGISTER(StatusType.OK, ModLanguageProviderES.PREFIX + "unregister_successful"),
    SAME_PASSWORD(StatusType.OK_WARN, ModLanguageProviderES.PREFIX + "same_password"),
    PASSWORD_CHANGED(StatusType.OK, ModLanguageProviderES.PREFIX + "password_changed"),
    ERROR(StatusType.KO_ERROR, ModLanguageProviderES.PREFIX + "unexpected_error");


    private StatusType status;
    private String description;

    RegisterStatus(StatusType status, String description) {
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
