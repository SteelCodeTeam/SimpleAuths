package team.steelcode.simple_auths.data.enums;

public enum LoginStatus implements IStatus {
    ALREADY_LOGGED(StatusType.KO_WARN, "Player already logged."),
    NOT_LOGGED(StatusType.KO_WARN, "You need logged again"),
    SUCCESSFULLY_LOGGED(StatusType.OK, "Successfully logged."),
    WRONG_PASSWORD(StatusType.KO_WARN, "Wrong password, try it again."),
    NOT_USER_FOUND(StatusType.OK_WARN, "Player not found. Please, register with /register <password> <password>"),
    ERROR(StatusType.KO_ERROR, "Unexpected error, please, contact with administrator.");

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
