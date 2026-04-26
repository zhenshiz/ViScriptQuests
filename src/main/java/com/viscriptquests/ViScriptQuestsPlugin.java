package com.viscriptquests;

import com.lowdragmc.lowdraglib2.plugin.ILDLibPlugin;
import com.lowdragmc.lowdraglib2.plugin.LDLibPlugin;
import com.lowdragmc.lowdraglib2.syncdata.AccessorRegistries;

@LDLibPlugin
public class ViScriptQuestsPlugin implements ILDLibPlugin {
    @Override
    public void onLoad() {
        AccessorRegistries.setPriority(0);
    }
}
