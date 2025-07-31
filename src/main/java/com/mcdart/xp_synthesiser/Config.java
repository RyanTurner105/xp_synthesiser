package com.mcdart.xp_synthesiser;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;

// An example config class. This is not required, but it's a good idea to have one to keep your config organized.
// Demonstrates how to use Neo's config APIs
@EventBusSubscriber(modid = XPSynthesiser.MODID, bus = EventBusSubscriber.Bus.MOD)
public class Config
{
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    static final ModConfigSpec SPEC = BUILDER.build();

    @SubscribeEvent
    static void onLoad(final ModConfigEvent event)
    {
        // Necessary??
    }

    private static final ModConfigSpec.Builder SERVER_BUILDER = new ModConfigSpec.Builder();
    public static final CategoryGeneral GENERAL = new CategoryGeneral();

    public static ModConfigSpec SERVER_CONFIG;

    public static final class CategoryGeneral {
        public final ModConfigSpec.BooleanValue requiresPower;
        public final ModConfigSpec.IntValue xpPointCost;
        public final ModConfigSpec.IntValue xpScalingCost;
        public final ModConfigSpec.IntValue xpScalingFactor;

        private CategoryGeneral() {
            SERVER_BUILDER.comment("General settings").push("general");

            requiresPower = SERVER_BUILDER.comment("Whether or not the XP Synthesiser needs power").define("requiresPower", true);
            xpPointCost = SERVER_BUILDER.comment("Affects base cost, which applies equally to small and big recordings")
                    .defineInRange("xpPointCost", 10000, 0, Integer.MAX_VALUE);
            xpScalingCost = SERVER_BUILDER.comment("Affects scaling costs, which applies mostly to big recordings. Doubling this doubles scaling cost")
                    .defineInRange("xpScalingCost", 100000, 0, Integer.MAX_VALUE);
            xpScalingFactor = SERVER_BUILDER.comment("Scaling factor as an exponent. GREATLY influences scaling cost. 2 means no scaling cost")
                    .defineInRange("xpScalingFactor", 3, 2, 10);

            SERVER_BUILDER.pop();
            SERVER_CONFIG = SERVER_BUILDER.build();
        }
    }
}
