package com.viscriptquests.gui.blueprint;

import com.lowdragmc.lowdraglib2.nodegraphtookit.api.graph.Graph;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.graph.GraphNodeRegistry;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.node.Node;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.type.TypeHandle;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.type.TypeHandles;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.graph.CustomGraphModelImpl;
import com.viscriptquests.ViScriptQuests;

import java.util.List;

public class QuestBlueprintGraph extends Graph {
    public static final GraphNodeRegistry NODE_REGISTRY = GraphNodeRegistry.create(ViScriptQuests.id("quest_blueprint"), QuestBlueprintGraph.class);

    private static final List<TypeHandle> SUPPORT_TYPES = List.of(
            TypeHandles.STRING,
            TypeHandles.INT,
            TypeHandles.FLOAT,
            TypeHandles.BOOL,
            TypeHandles.BLOCK,
            TypeHandles.ITEM_STACK
    );

    @Override
    public List<Class<? extends Node>> getSupportNodes() {
        return NODE_REGISTRY.getNodeClasses();
    }

    @Override
    public List<TypeHandle> getSupportTypes() {
        return SUPPORT_TYPES;
    }

    @Override
    protected CustomGraphModelImpl createGraphModel() {
        return new QuestBlueprintGraphModel(this);
    }
}
