package team.steelcode.simple_auths.setup;


import net.minecraft.data.DataGenerator;
import net.minecraftforge.data.event.GatherDataEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import team.steelcode.simple_auths.SimpleAuths;
import team.steelcode.simple_auths.data.language_providers.ModLanguageProviderEN;
import team.steelcode.simple_auths.data.language_providers.ModLanguageProviderES;

@Mod.EventBusSubscriber(modid = SimpleAuths.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class DataGenerators {

    private DataGenerators() {}

    @SubscribeEvent
    public static void generateData(GatherDataEvent event) {
        DataGenerator gen = event.getGenerator();

        gen.addProvider(true, new ModLanguageProviderES(gen, "es_es"));
        gen.addProvider(true, new ModLanguageProviderES(gen, "es_ar"));
        gen.addProvider(true, new ModLanguageProviderES(gen, "es_mx"));
        gen.addProvider(true, new ModLanguageProviderES(gen, "es_uy"));
        gen.addProvider(true, new ModLanguageProviderES(gen, "es_ve"));

        gen.addProvider(true, new ModLanguageProviderEN(gen, "en_us"));
        gen.addProvider(true, new ModLanguageProviderEN(gen, "en_au"));
        gen.addProvider(true, new ModLanguageProviderEN(gen, "en_ca"));
        gen.addProvider(true, new ModLanguageProviderEN(gen, "en_gb"));
    }

}
