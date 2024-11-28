package com.mcdart.xp_synthesiser;

import com.mcdart.xp_synthesiser.blocks.SynthesiserMenu;
import com.mcdart.xp_synthesiser.blocks.SynthesiserScreen;
import com.mcdart.xp_synthesiser.blocks.XPSynthesiserBlock;
import com.mcdart.xp_synthesiser.blocks.XPSynthesiserBlockEntity;
import com.mcdart.xp_synthesiser.items.KillRecorderData;
import com.mcdart.xp_synthesiser.items.KillRecorderItem;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.registries.*;
import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;

import java.time.LocalTime;
import java.util.function.Supplier;

// The value here should match an entry in the META-INF/neoforge.mods.toml file
@Mod(XPSynthesiser.MODID)
public class XPSynthesiser
{
    // Define mod id in a common place for everything to reference
    public static final String MODID = "xp_synthesiser";
    // Directly reference a slf4j logger
    private static final Logger LOGGER = LogUtils.getLogger();
    // Create a Deferred Register to hold Blocks which will all be registered under the "examplemod" namespace
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(MODID);
    // Create a Deferred Register to hold Items which will all be registered under the "examplemod" namespace
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MODID);
    // Create a Deferred Register to hold CreativeModeTabs which will all be registered under the "examplemod" namespace
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);

    // Creates a new Block with the id "examplemod:example_block", combining the namespace and path
    public static final DeferredBlock<Block> EXAMPLE_BLOCK = BLOCKS.registerSimpleBlock("example_block", BlockBehaviour.Properties.of().mapColor(MapColor.STONE));
    // Creates a new BlockItem with the id "examplemod:example_block", combining the namespace and path
    public static final DeferredItem<BlockItem> EXAMPLE_BLOCK_ITEM = ITEMS.registerSimpleBlockItem("example_block", EXAMPLE_BLOCK);

    // my_block
    public static final DeferredBlock<Block> MY_BLOCK = BLOCKS.register("my_block", () -> new Block(BlockBehaviour.Properties.of()
            .mapColor(MapColor.TERRACOTTA_PURPLE)
            .destroyTime(0.2F)
            .sound(SoundType.ANCIENT_DEBRIS)
            .lightLevel(state -> 15)
    ));
    public static final DeferredItem<BlockItem> MY_BLOCK_ITEM = ITEMS.registerSimpleBlockItem("my_block", MY_BLOCK);

    // my_item
    public static final DeferredItem<Item> MY_ITEM = ITEMS.register("my_item", () -> new Item(new Item.Properties()
            .stacksTo(4)
            .fireResistant()
            .rarity(Rarity.EPIC)
    ));

    // data item
    // Record def
    public record ExampleRecord(int value1, boolean value2) {}

    // Codec def
    public static final Codec<ExampleRecord> BASIC_CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    Codec.INT.fieldOf("value1").forGetter(ExampleRecord::value1),
                    Codec.BOOL.fieldOf("value2").forGetter(ExampleRecord::value2)
            ).apply(instance, ExampleRecord::new)
    );
    public static final StreamCodec<ByteBuf, ExampleRecord> BASIC_STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.INT, ExampleRecord::value1,
            ByteBufCodecs.BOOL, ExampleRecord::value2,
            ExampleRecord::new
    );

    // Registrar def
    public static final DeferredRegister.DataComponents REGISTRAR = DeferredRegister.createDataComponents(MODID);

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<ExampleRecord>> BASIC_EXAMPLE = REGISTRAR.registerComponentType(
            "basic",
            builder -> builder
                    // The codec to read/write the data to disk
                    .persistent(BASIC_CODEC)
                    // The codec to read/write the data across the network
                    .networkSynchronized(BASIC_STREAM_CODEC)
    );

    public static final DeferredItem<Item> DATA_ITEM = ITEMS.register("data_item", () -> new Item(new Item.Properties()
            .component(BASIC_EXAMPLE, new ExampleRecord(24, true))
            .stacksTo(1)
            .fireResistant()
            .rarity(Rarity.EPIC)
    ));

    // The kill recorder
    private static DataComponentType<? super ExampleRecord> KillRecorder;
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
            BLOCKS.register("xp_synthesiser", () -> new XPSynthesiserBlock(BlockBehaviour.Properties.of().noOcclusion()));

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

    // Data Attachments
    // Create the DeferredRegister for attachment types
    private static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES = DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, MODID);

    // Serialization via INBTSerializable
    public static final Supplier<AttachmentType<ItemStackHandler>> KILL_RECORDER_SLOT = ATTACHMENT_TYPES.register(
            "kill_recorder_slot", () -> AttachmentType.serializable(() -> new ItemStackHandler(1)).build()
    );
    // Serialization via codec
    public static final Supplier<AttachmentType<Integer>> POWER = ATTACHMENT_TYPES.register(
            "power", () -> AttachmentType.builder(() -> 0).serialize(Codec.INT).build()
    );

    private static void onRegisterCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK, // capability to register for
                XP_SYNTHESISER_BLOCK_ENTITY.get(), // block entity type to register for
                (object, context) -> object.onRegisterCapability(Capabilities.ItemHandler.BLOCK, context)
        );
        event.registerBlockEntity(
                Capabilities.EnergyStorage.BLOCK, // capability to register for
                XP_SYNTHESISER_BLOCK_ENTITY.get(), // block entity type to register for
                (object, context) -> object.onRegisterCapability(Capabilities.EnergyStorage.BLOCK, context)
        );
    };


    // Creates a creative tab with the id "examplemod:example_tab" for the example item, that is placed after the combat tab
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> EXAMPLE_TAB = CREATIVE_MODE_TABS.register("xp_synthesiser", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.xp_synthesiser")) //The language key for the title of your CreativeModeTab
            .withTabsBefore(CreativeModeTabs.COMBAT)
            .icon(() -> XP_SYNTHESISER_ITEM.get().getDefaultInstance())
            .displayItems((parameters, output) -> {
                output.accept(EXAMPLE_BLOCK_ITEM.get());
                output.accept(MY_BLOCK_ITEM.get());
                output.accept(MY_ITEM.get());
                output.accept(DATA_ITEM.get());
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
        // Register the commonSetup method for modloading
        modEventBus.addListener(this::commonSetup);

        // Register the Deferred Register to the mod event bus so blocks get registered
        BLOCKS.register(modEventBus);
        // Register the Deferred Register to the mod event bus so items get registered
        ITEMS.register(modEventBus);
        // Register the Deferred Register to the mod event bus so tabs get registered
        CREATIVE_MODE_TABS.register(modEventBus);
        // Register for the data item?
        REGISTRAR.register(modEventBus);
        // Register for block drops
        // Register for recipes
        RECIPE_REGISTER.register(modEventBus);
        // Register for KillRecorder data
        DATA_COMPONENT_TYPE_REGISTER.register(modEventBus);
        // Register for XPSynthesiser data
        BLOCK_ENTITY_REGISTER.register(modEventBus);
        // Register for Data attachments for XPSynthesiser
        ATTACHMENT_TYPES.register(modEventBus);
        // Menu registers
        MENUS.register(modEventBus);

        // Register capabilities (for XPSynthesiser)
        modEventBus.addListener(XPSynthesiser::onRegisterCapabilities);

        if (FMLEnvironment.dist.isClient()) {
            // Register config
            modContainer.registerConfig(ModConfig.Type.SERVER, Config.SERVER_CONFIG);
            modContainer.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
        }

        // Register ourselves for server and other game events we are interested in.
        // Note that this is necessary if and only if we want *this* class (XPSynthesiser) to respond directly to events.
        // Do not add this line if there are no @SubscribeEvent-annotated functions in this class, like onServerStarting() below.
        NeoForge.EVENT_BUS.register(this);

        // Register the item to a creative tab
        modEventBus.addListener(this::addCreative);

        // Register our mod's ModConfigSpec so that FML can create and load the config file for us
        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }

    private void commonSetup(final FMLCommonSetupEvent event)
    {
        // Some common setup code
        LOGGER.info("HELLO FROM COMMON SETUP");

        if (Config.logDirtBlock)
            LOGGER.info("DIRT BLOCK >> {}", BuiltInRegistries.BLOCK.getKey(Blocks.DIRT));

        LOGGER.info(Config.magicNumberIntroduction + Config.magicNumber);

        Config.items.forEach((item) -> LOGGER.info("ITEM >> {}", item.toString()));
    }

    // Add the example block item to the building blocks tab
    private void addCreative(BuildCreativeModeTabContentsEvent event)
    {
        if (event.getTabKey() == CreativeModeTabs.BUILDING_BLOCKS)
            event.accept(EXAMPLE_BLOCK_ITEM);
    }

    // You can use SubscribeEvent and let the Event Bus discover methods to call
    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event)
    {
        // Do something when the server starts
        LOGGER.info("HELLO from server starting");
    }

    // Listens to a mob death
    // Used for the Kill Recorder
    @SubscribeEvent
    public void killed(LivingDeathEvent event) {
        // Only on server
        if (!event.getEntity().level().isClientSide()) {
            if (event.getSource().getEntity() instanceof ServerPlayer) {
                LOGGER.info("PLAYER KILL");
                LOGGER.info("{} Killed {}, using {} ", event.getSource().getEntity(), event.getEntity(), event.getSource());
                var xpGained = event.getEntity().getExperienceReward((ServerLevel) event.getEntity().level(), event.getSource().getEntity());
                LOGGER.info("XP Gained: {}", xpGained);

                // Find any active Kill Recorders in the player's inventory, if there is one
                ((ServerPlayer) event.getSource().getEntity()).getInventory().items.forEach((slot) ->
                {
                    // Found a kill recorder
                    if (slot.getItem() instanceof KillRecorderItem) {

                        KillRecorderData oldData = slot.get(XPSynthesiser.KILL_RECORDER_DATA_COMPONENT.get());

                        // If it has data and it's recording
                        if (oldData != null && oldData.recording()) {
                            LOGGER.info("Old kill recorder data: {}, {}, {}, {}", oldData.recordingEnd(), oldData.xp(), oldData.recordingStart(), oldData.recordingEnd());
                            // Add the new XP from the kill
//                            oldData.setXP(oldData.getXP() + xpGained);
//                            KillRecorderItem.setRecordingData(slot, oldData);
                            slot.update(
                                    XPSynthesiser.KILL_RECORDER_DATA_COMPONENT.get(),
                                    oldData,
                                    data -> data.addKill(xpGained)
                            );
                            LOGGER.info("New kill recorder data: {}, {}, {}, {}", oldData.recording(), oldData.xp(), oldData.recordingStart(), oldData.recordingEnd());
//                            var newData = ((KillRecorderItem) slot.getItem().getItem()).getRecordingData(slot.getItem());
//                            LOGGER.info("New kill recorder data?: {}, {}", newData.getRecording(), newData.getXP());
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
        public static void onClientSetup(FMLClientSetupEvent event) {
            // Some client setup code
            LOGGER.info("HELLO FROM CLIENT SETUP");
            LOGGER.info("MINECRAFT NAME >> {}", Minecraft.getInstance().getUser().getName());
        }

        @SubscribeEvent
        // Event is listened to on the mod event bus
        private static void registerScreens(RegisterMenuScreensEvent event) {
            event.register(SYNTHESISER_MENU.get(), SynthesiserScreen::new);
        }
    }
}
