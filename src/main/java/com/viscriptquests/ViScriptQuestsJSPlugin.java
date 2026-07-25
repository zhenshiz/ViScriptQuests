package com.viscriptquests;

import com.viscriptquests.event.CommonEventsPostJS;
import com.viscriptquests.event.ViScriptQuestsEventJS;
import com.viscriptquests.util.ViScriptQuestsClientUtil;
import com.viscriptquests.util.ViScriptQuestsServerUtil;
import dev.latvian.mods.kubejs.event.EventGroupRegistry;
import dev.latvian.mods.kubejs.plugin.KubeJSPlugin;
import dev.latvian.mods.kubejs.script.BindingRegistry;
import dev.latvian.mods.kubejs.script.ScriptType;
import net.neoforged.neoforge.common.NeoForge;

public final class ViScriptQuestsJSPlugin implements KubeJSPlugin {
    @Override
    public void init() {
        NeoForge.EVENT_BUS.register(CommonEventsPostJS.class);
        ViScriptQuests.LOGGER.info("Enabled KubeJS quest event bridge");
    }

    @Override
    public void registerEvents(EventGroupRegistry registry) {
        registry.register(ViScriptQuestsEventJS.QUEST_EVENTS);
    }

    @Override
    public void registerBindings(BindingRegistry bindings) {
        if (bindings.type() == ScriptType.CLIENT) {
            bindings.add("ViScriptQuestsUtil", ViScriptQuestsClientUtil.class);
        } else if (bindings.type() == ScriptType.SERVER) {
            bindings.add("ViScriptQuestsUtil", ViScriptQuestsServerUtil.class);
        }
    }
}
