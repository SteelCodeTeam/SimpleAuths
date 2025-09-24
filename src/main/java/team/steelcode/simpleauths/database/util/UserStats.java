package team.steelcode.simpleauths.database.util;

public record UserStats(long playTime, long totalIPs, long trustedIPs,
                            long blockedIPs, int maxIpsAllowed,boolean requiresIPVerification) {
}
