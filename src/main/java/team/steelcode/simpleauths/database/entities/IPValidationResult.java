package team.steelcode.simpleauths.database.entities;

public enum IPValidationResult {
    APPROVED,
    REQUIRES_VERIFICATION,
    IP_ALREADY_IN_USE,
    TOO_MANY_IPS,
    TOO_FREQUENT_CHANGES,
    USER_NOT_FOUND,
    BLOCKED,
    MULTI_ACCOUNT_BLOCKED,
    MULTI_ACCOUNT_WARNING,
    TOO_MANY_IPS_WARNING,
    SYSTEM_ERROR
}