package team.steelcode.simple_auths.data.enums;

public enum Configs {
    DATABASE_URL(null),
    DATABASE_USER(null),
    DATABASE_PASSWORD(null);


    private String value;

    Configs(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
