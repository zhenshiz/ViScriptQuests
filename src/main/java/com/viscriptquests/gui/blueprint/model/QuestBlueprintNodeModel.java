package com.viscriptquests.gui.blueprint.model;

import com.lowdragmc.lowdraglib2.nodegraphtookit.gui.GraphElement;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.constant.Constant;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.CustomNodeModelImpl;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.NodeOption;
import com.viscriptquests.gui.blueprint.node.QuestBlueprintNode;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

public class QuestBlueprintNodeModel extends CustomNodeModelImpl {
    @Override
    protected void removeObsoleteWiresAndConstants() {
        Map<String, Constant> retainedOptions = new HashMap<>();
        if (getNode() instanceof QuestBlueprintNode questNode) {
            for (var entry : inputConstantsById.entrySet()) {
                String key = entry.getKey();
                if (!key.startsWith(NodeOption.PORT_ID_PREFIX)) {
                    continue;
                }
                String optionId = key.substring(NodeOption.PORT_ID_PREFIX.length());
                if (questNode.retainsOptionValue(optionId)) {
                    retainedOptions.put(key, entry.getValue());
                }
            }
        }

        super.removeObsoleteWiresAndConstants();

        retainedOptions.forEach(inputConstantsById::putIfAbsent);
    }

    @Override
    public int getElementColor() {
        return normalizeUserColor(super.getElementColor());
    }

    @Override
    public void setColor(int color) {
        super.setColor(normalizeUserColor(color));
    }

    @Override
    public @Nullable GraphElement<?> createElementUI() {
        return new QuestBlueprintNodeElement(this);
    }

    private static int normalizeUserColor(int color) {
        return color != 0 && (color & 0xFF000000) == 0 ? color | 0xFF000000 : color;
    }
}
