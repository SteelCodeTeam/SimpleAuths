package team.steelcode.simpleauths.entities;

public enum IPValidationResult {
    APPROVED,
    REQUIRES_VERIFICATION,
    IP_ALREADY_IN_USE,
    TOO_MANY_IPS,
    TOO_FREQUENT_CHANGES,
    USER_NOT_FOUND,
    BLOCKED
}