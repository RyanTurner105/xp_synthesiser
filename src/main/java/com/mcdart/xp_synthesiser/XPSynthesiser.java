package com.mcdart.xp_synthesiser;

import com.mcdart.xp_synthesiser.blocks.SynthesiserMenu;
import com.mcdart.xp_synthesiser.blocks.SynthesiserScreen;
import com.mcdart.xp_synthesiser.blocks.XPSynthesiserBlock;
import com.mcdart.xp_synthesiser.blocks.XPSynthesiserBlockEntity;
import com.mcdart.xp_synthesiser.items.KillRecorderData;
import com.mcdart.xp_synthesiser.items.KillRecorderItem;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.registries.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;
import java.util.function.Supplier;

// The value here should match an entry in the META-INF/neoforge.mods.toml file
@Mod(XPSynthesiser.MODID)
public class XPSynthesiser
{
    // Define mod id in a common place for everything to reference
    public static final String MODID = "xp_synthesiser";
    // Create a Deferred Register to hold Blocks which will all be registered under the "examplemod" namespace
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(MODID);
    // Create a Deferred Register to hold Items which will all be registered under the "examplemod" namespace
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MODID);
    // Create a Deferred Register to hold CreativeModeTabs which will all be registered under the "examplemod" namespace
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);

    // The kill recorder
    public static final DeferredHolder<Item, KillRecorderItem> KILL_RECORDER = ITEMS.register("kill_recorder", () -> new KillRecorderItem(false));

    private static final DeferredRegister<DataComponentType<?>> DATA_COMPONENT_TYPE_REGISTER =
            DeferredRegister.create(BuiltInRegistries.DATA_COMPONENT_TYPE, MODID);

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<KillRecorderData>> KILL_RECORDER_DATA_COMPONENT =
            DATA_COMPONENT_TYPE_REGISTER.register("kill_recorder_data",
                    () -> DataComponentType.<KillRecorderData>builder().persistent(KillRecorderData.CODEC)
                            .networkSynchronized(KillRecorderData.STREAM_CODEC).build());

    // The XP Synthesiser
    // Block
    public static final DeferredHolder<Block, XPSynthesiserBlock> XP_SYNTHESISER_BLOCK =
            BLOCKS.register("xp_synthesiser", () -> new XPSynthesiserBlock(
                    BlockBehaviour.Properties.of()
                            .noOcclusion()
                            .destroyTime(1.5F)
            ));

    // Tile entity
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_REGISTER = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, MODID);
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<XPSynthesiserBlockEntity>> XP_SYNTHESISER_BLOCK_ENTITY = BLOCK_ENTITY_REGISTER.register(
            "xp_synthesiser_entity",
            () -> BlockEntityType.Builder.of(
                    XPSynthesiserBlockEntity::new,
                    XP_SYNTHESISER_BLOCK.get()
            ).build(null)
    );
    public static final DeferredItem<BlockItem> XP_SYNTHESISER_ITEM = ITEMS.registerSimpleBlockItem("xp_synthesiser", XP_SYNTHESISER_BLOCK);

    private static void onRegisterCapabilities(RegisterCapabilitiesEvent event) {
        // Register energy storing capability
        event.registerBlock(Capabilities.EnergyStorage.BLOCK, (level, pos, state, be, side) -> {
            assert be != null;
            return ((XPSynthesiserBlockEntity) be).energyStorage;
        }, XP_SYNTHESISER_BLOCK.get());
    };

    // Creates a creative tab with the id "examplemod:example_tab" for the example item, that is placed after the combat tab
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> XP_SYNTHESISER_TAB = CREATIVE_MODE_TABS.register("xp_synthesiser", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.xp_synthesiser")) //The language key for the title of your CreativeModeTab
            .withTabsBefore(CreativeModeTabs.COMBAT)
            .icon(() -> XP_SYNTHESISER_ITEM.get().getDefaultInstance())
            .displayItems((parameters, output) -> {
                output.accept(KILL_RECORDER.get());
                output.accept(XP_SYNTHESISER_ITEM.get());
            }).build());

    private static final DeferredRegister<RecipeSerializer<?>> RECIPE_REGISTER = DeferredRegister.create(BuiltInRegistries.RECIPE_SERIALIZER, MODID);

    // Menu register
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, MODID);
    public static final Supplier<MenuType<SynthesiserMenu>> SYNTHESISER_MENU
            = MENUS.register("xp_synthesiser", () -> IMenuTypeExtension.create(SynthesiserMenu::new));

    // The constructor for the mod class is the first code that is run when your mod is loaded.
    // FML will recognize some parameter types like IEventBus or ModContainer and pass them in automatically.
    public XPSynthesiser(IEventBus modEventBus, ModContainer modContainer)
    {

        // Register the Deferred Register to the mod event bus so blocks get registered
        BLOCKS.register(modEventBus);
        // Register the Deferred Register to the mod event bus so items get registered
        ITEMS.register(modEventBus);
        // Register the Deferred Register to the mod event bus so tabs get registered
        CREATIVE_MODE_TABS.register(modEventBus);
        // Register for recipes
        RECIPE_REGISTER.register(modEventBus);
        // Register for KillRecorder data
        DATA_COMPONENT_TYPE_REGISTER.register(modEventBus);
        // Register for XPSynthesiser data
        BLOCK_ENTITY_REGISTER.register(modEventBus);
        // Menu registers
        MENUS.register(modEventBus);

        // Register capabilities (for XPSynthesiser)
        modEventBus.addListener(XPSynthesiser::onRegisterCapabilities);
        modContainer.registerConfig(ModConfig.Type.SERVER, Config.SERVER_CONFIG);

        if (FMLEnvironment.dist.isClient()) {
            // Register config
            modContainer.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
        }

        // Register ourselves for server and other game events we are interested in.
        // Note that this is necessary if and only if we want *this* class (XPSynthesiser) to respond directly to events.
        // Do not add this line if there are no @SubscribeEvent-annotated functions in this class, like onServerStarting() below.
        NeoForge.EVENT_BUS.register(this);

        // Register our mod's ModConfigSpec so that FML can create and load the config file for us
        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }

    // Listens to a mob death
    // Used for the Kill Recorder
    @SubscribeEvent
    public void killed(LivingDeathEvent event) {
        // Only on server
        if (!event.getEntity().level().isClientSide()) {
            if (event.getSource().getEntity() instanceof ServerPlayer) {
                var xpGained = event.getEntity().getExperienceReward((ServerLevel) event.getEntity().level(), event.getSource().getEntity());

                // Find any active Kill Recorders in the player's inventory, if there is one
                ((ServerPlayer) event.getSource().getEntity()).getInventory().items.forEach((slot) ->
                {
                    // Found a kill recorder
                    if (slot.getItem() instanceof KillRecorderItem) {

                        KillRecorderData oldData = slot.get(XPSynthesiser.KILL_RECORDER_DATA_COMPONENT.get());

                        // If it has data and it's recording
                        if (oldData != null && oldData.recording()) {
                            // Add the new XP from the kill
                            slot.update(
                                    XPSynthesiser.KILL_RECORDER_DATA_COMPONENT.get(),
                                    oldData,
                                    data -> data.addKill(xpGained)
                            );
                        }

                    }
                });

            }
        }
    }

    // You can use EventBusSubscriber to automatically register all static methods in the class annotated with @SubscribeEvent
    @EventBusSubscriber(modid = MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientModEvents
    {
        @SubscribeEvent
        // Event is listened to on the mod event bus
        private static void registerScreens(RegisterMenuScreensEvent event) {
            event.register(SYNTHESISER_MENU.get(), SynthesiserScreen::new);
        }
    }
}
