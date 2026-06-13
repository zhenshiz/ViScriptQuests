package com.viscriptquests.gui.blueprint.model;

import com.lowdragmc.lowdraglib2.nodegraphtookit.gui.GraphElement;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.CustomNodeModelImpl;
import org.jetbrains.annotations.Nullable;

public class QuestBlueprintNodeModel extends CustomNodeModelImpl {
    @Override
    public @Nullable GraphElement<?> createElementUI() {
        return new QuestBlueprintNodeElement(this);
    }
}
