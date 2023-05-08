package team.steelcode.simple_auths.setup;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;

public class ConfigSpecRegister {

    public static ForgeConfigSpec.ConfigValue<String> DB_URL;
    public static ForgeConfigSpec.ConfigValue<String> DB_USER;
    public static ForgeConfigSpec.ConfigValue<String> DB_PASSWORD;
    public static ForgeConfigSpec.ConfigValue<String> DB_SHOW_SQL;

    public static ForgeConfigSpec.Builder SERVER_CONFIG;

    static {
        SERVER_CONFIG = new ForgeConfigSpec.Builder();
        setupConfig(SERVER_CONFIG);
    }


    public static void register() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, SERVER_CONFIG.build(), "simple-auths.toml");
    }


    private static void setupConfig(ForgeConfigSpec.Builder builder) {
        builder.comment("""
                
                ##########################
                #  SIMPLE AUTHS CONFIG  ##
                ##########################
                """);
        builder.comment(" If you don't know how to configure these parameters or where to get them, you can ask in our discord: https://discord.gg/YHqMTRYAMT");
        builder.comment(" We only accept MySQL databases!");
        DB_URL = builder.define("database_url", "jdbc:mysql://0.0.0.0:3306/db_name");
        DB_USER = builder.define("database_username", "username");
        DB_PASSWORD = builder.define("database_password", "password");
        DB_SHOW_SQL = builder.define("show_sql", "false");

    }


}
