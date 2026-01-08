package com.viscriptquests;

import com.lowdragmc.lowdraglib2.registry.AutoRegistry;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegister;
import com.mojang.logging.LogUtils;
import com.viscriptquests.command.ICommand;
import com.viscriptquests.quest.QuestSavedData;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.saveddata.SavedData;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import org.slf4j.Logger;

import java.util.function.Supplier;

@Mod(ViScriptQuests.MOD_ID)
public class ViScriptQuests {
    public static final String MOD_ID = "viscript_quests";
    public static final Logger LOGGER = LogUtils.getLogger();
    @Setter
    @Getter
    private static QuestSavedData questSavedData;

    public ViScriptQuests(IEventBus modEventBus, ModContainer modContainer, Dist dist) {
        NeoForge.EVENT_BUS.addListener(this::onRegisterCommands);
        if (dist == Dist.CLIENT) {
            modContainer.registerConfig(ModConfig.Type.COMMON, Config.CONFIG_SPEC, "%s_config.toml".formatted(MOD_ID));
            modContainer.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
        }
    }

    //注册指令
    private void onRegisterCommands(RegisterCommandsEvent event) {
        for (AutoRegistry.Holder<LDLRegister, ICommand, Supplier<ICommand>> command : ViScriptQuestsRegistries.COMMANDS) {
            command.value().get().register(event.getDispatcher(), event.getBuildContext(), event.getCommandSelection());
        }
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }

    public static String formattedMod(String path) {
        return ("%s:" + path).formatted(MOD_ID);
    }

    public static boolean isPresentResource(ResourceLocation resourceLocation) {
        return Minecraft.getInstance().getResourceManager().getResource(resourceLocation).isPresent();
    }

    private static boolean isModLoaded(String modId) {
        return ModList.get().isLoaded(modId);
    }
}