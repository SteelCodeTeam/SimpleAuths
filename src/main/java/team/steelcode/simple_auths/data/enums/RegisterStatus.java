package team.steelcode.simple_auths.data.enums;

public enum RegisterStatus implements IStatus {
    ALREADY_EXISTS(StatusType.KO_WARN, "Player already registered."),
    PLAYER_DOESNT_EXISTS(StatusType.KO_WARN, "Player doesn't exists"),
    SUCCESSFULLY_REGISTER(StatusType.OK, "Successfully registered."),
    SUCCESSFULLY_UNREGISTER(StatusType.OK, "Successfully unregistered."),

    SAME_PASSWORD(StatusType.OK_WARN, "Password not changed. The password its the same than registered one."),
    PASSWORD_CHANGED(StatusType.OK, "Successfully changed password."),
    ERROR(StatusType.KO_ERROR, "Unexpected error, please, contact with administrator.");


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
