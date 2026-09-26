package com.viscriptquests;

import com.lowdragmc.lowdraglib2.gui.factory.PlayerUIMenuType;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.mojang.logging.LogUtils;
import com.viscriptquests.config.ClientConfig;
import com.viscriptquests.gui.editor.QuestEditor;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.loading.FMLEnvironment;
import org.slf4j.Logger;

@Mod(ViScriptQuests.MOD_ID)
public class ViScriptQuests {
    public static final String MOD_ID = "viscript_quests";
    public static final Logger LOGGER = LogUtils.getLogger();

    public ViScriptQuests() {
        if (FMLEnvironment.dist.isClient()) {
            ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, ClientConfig.SPEC, String.format("%s_client_config.toml", MOD_ID));
        }
        PlayerUIMenuType.register(QuestEditor.EDITOR_ID, ignored -> player -> {
            if (player.level().isClientSide) {
                return QuestEditor.createUI();
            }
            return new ModularUI(UI.empty());
        });
    }

    public static ResourceLocation id(String path) {
        return new ResourceLocation(MOD_ID, path);
    }

    public static String formattedMod(String path) {
        return ("%s:" + path).formatted(MOD_ID);
    }

    public static boolean isPresentResource(ResourceLocation resourceLocation) {
        return Minecraft.getInstance().getResourceManager().getResource(resourceLocation).isPresent();
    }
}
