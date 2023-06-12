package team.steelcode.simple_auths.data.language_providers;

import net.minecraft.data.DataGenerator;
import net.minecraftforge.common.data.LanguageProvider;
import team.steelcode.simple_auths.SimpleAuths;

import java.util.HashMap;
import java.util.Map;

public class ModLanguageProviderEN extends LanguageProvider {

    public final static String PREFIX = "simple_auths.translations.";


    private final Map<String, String> translations = new HashMap<>() {{
        put(PREFIX + "password_mismatch", "Password missmatch. Please try it again!");
        put(PREFIX + "password_incorrect", "Password incorrect. Please try it again!");
        put(PREFIX + "login_successful", "Successfully login!");
        put(PREFIX + "already_logged", "You were already logged!");
        put(PREFIX + "relogin_needed", "You need to sign in again in the server.");
        put(PREFIX + "register_successful", "Te has registrado correctamente!");
        put(PREFIX + "unregister_successful", "The account of the player are succesfully unregistered.");
        put(PREFIX + "already_exists", "You are already registered!");
        put(PREFIX + "doesnt_exists", "You are not registered, register now with /register");
        put(PREFIX + "same_password", "Passwords are the same, please type another password.");
        put(PREFIX + "password_changed", "Password sucessfully changed.");
        put(PREFIX + "unexpected_error", "Unexpected error, contact with administrator.");
    }};


    public ModLanguageProviderEN(DataGenerator gen, String locale) {
        super(gen, SimpleAuths.MOD_ID, locale);
    }

    @Override
    protected void addTranslations() {
        for (String key: translations.keySet()) {
            add(key, translations.get(key));
        }
    }
}
