package com.viscriptquests.util;

import com.lowdragmc.lowdraglib2.integration.kjs.KJSBindings;
import net.minecraft.client.Minecraft;

@KJSBindings(value = "client", modId = "viscript_quests", clientOnly = true)
public class ViScriptQuestsClientUtil {
    private static final Minecraft minecraft = Minecraft.getInstance();
}
