package com.viscriptquests;

import com.viscriptquests.event.CommonEventsPostJS;
import com.viscriptquests.event.ViScriptQuestsEventJS;
import com.viscriptquests.util.ViScriptQuestsClientUtil;
import com.viscriptquests.util.ViScriptQuestsServerUtil;
import dev.latvian.mods.kubejs.KubeJSPlugin;
import dev.latvian.mods.kubejs.script.BindingsEvent;
import net.minecraftforge.common.MinecraftForge;

public final class ViScriptQuestsJSPlugin extends KubeJSPlugin {
    @Override
    public void init() {
        MinecraftForge.EVENT_BUS.register(CommonEventsPostJS.class);
        ViScriptQuests.LOGGER.info("Enabled KubeJS quest event bridge");
    }

    @Override
    public void registerEvents() {
        ViScriptQuestsEventJS.QUEST_EVENTS.register();
    }

    @Override
    public void registerBindings(BindingsEvent event) {
        if (event.getType().isClient()) {
            event.add("ViScriptQuestsUtil", ViScriptQuestsClientUtil.class);
        } else if (event.getType().isServer()) {
            event.add("ViScriptQuestsUtil", ViScriptQuestsServerUtil.class);
        }
    }
}
