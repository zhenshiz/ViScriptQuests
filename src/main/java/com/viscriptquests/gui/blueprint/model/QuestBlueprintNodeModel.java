package com.viscriptquests.gui.blueprint.model;

import com.lowdragmc.lowdraglib2.nodegraphtookit.gui.GraphElement;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.CustomNodeModelImpl;
import org.jetbrains.annotations.Nullable;

public class QuestBlueprintNodeModel extends CustomNodeModelImpl {
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
