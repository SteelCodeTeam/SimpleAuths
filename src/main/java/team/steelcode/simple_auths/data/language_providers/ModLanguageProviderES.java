package team.steelcode.simple_auths.data.language_providers;

import net.minecraft.data.DataGenerator;
import net.minecraftforge.common.data.LanguageProvider;
import team.steelcode.simple_auths.SimpleAuths;

import java.util.HashMap;
import java.util.Map;

public class ModLanguageProviderES extends LanguageProvider {

    public final static String PREFIX = "simple_auths.translations.";


    private final Map<String, String> translations = new HashMap<>() {{
        put(PREFIX + "password_mismatch", "Las contraseñas no coinciden, por favor intentalo de nuevo.");
        put(PREFIX + "password_incorrect", "La contraseña es incorrecta, por favor intantalo de nuevo.");
        put(PREFIX + "login_successful", "¡Te has logueado correctamente!");
        put(PREFIX + "already_logged", "¡Ya estas logueado!");
        put(PREFIX + "relogin_needed", "Ha ocurrido un error, Necesitas volver a loguearte");
        put(PREFIX + "register_successful", "¡Te has registrado correctamente!");
        put(PREFIX + "unregister_successful", "La cuenta del jugador se ha borrado correctamente.");
        put(PREFIX + "already_exists", "Ya estas registrado");
        put(PREFIX + "doesnt_exists", "Aun no estas registrado, registrate con /register");
        put(PREFIX + "same_password", "Las contraseñas son iguales, no se ha cambiado la contraseña.");
        put(PREFIX + "password_changed", "La contraseña se ha cambiado correctamente.");
        put(PREFIX + "unexpected_error", "Se ha producido un error inesperado, contacta con un administrador.");
    }};


    public ModLanguageProviderES(DataGenerator gen, String locale) {
        super(gen, SimpleAuths.MOD_ID, locale);
    }

    @Override
    protected void addTranslations() {
        for (String key: translations.keySet()) {
            add(key, translations.get(key));
        }
    }
}
