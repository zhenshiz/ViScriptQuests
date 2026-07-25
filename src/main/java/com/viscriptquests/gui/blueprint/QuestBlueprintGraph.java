package com.viscriptquests.gui.blueprint;

import com.lowdragmc.lowdraglib2.nodegraphtookit.api.graph.Graph;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.graph.GraphLogger;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.graph.GraphNodeRegistry;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.node.Node;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.type.TypeHandle;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.type.TypeHandles;
import com.lowdragmc.lowdraglib2.nodegraphtookit.gui.command.CreateForeignLocalSubgraphCommand;
import com.lowdragmc.lowdraglib2.nodegraphtookit.gui.command.CreateLocalSubgraphCommand;
import com.lowdragmc.lowdraglib2.nodegraphtookit.gui.command.CreateSubgraphFromSelectionCommand;
import com.lowdragmc.lowdraglib2.nodegraphtookit.gui.command.IGraphCommand;
import com.lowdragmc.lowdraglib2.nodegraphtookit.gui.command.ImportExternalSubgraphCommand;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.graph.CustomGraphModelImpl;
import com.viscriptquests.ViScriptQuests;

import java.util.List;

public class QuestBlueprintGraph extends Graph {
    public static final GraphNodeRegistry NODE_REGISTRY = GraphNodeRegistry.create(ViScriptQuests.id("quest_blueprint"), QuestBlueprintGraph.class);

    private static final List<TypeHandle> CONSTANT_TYPES = List.of(
            TypeHandles.STRING,
            TypeHandles.INT,
            TypeHandles.FLOAT,
            TypeHandles.BOOL,
            TypeHandles.BLOCK,
            TypeHandles.ITEM_STACK
    );
    private static final List<TypeHandle> VARIABLE_TYPES = List.of(
            TypeHandles.STRING,
            TypeHandles.INT,
            TypeHandles.FLOAT,
            TypeHandles.BOOL,
            TypeHandles.ITEM_STACK
    );

    @Override
    public List<Class<? extends Node>> getSupportNodes() {
        return NODE_REGISTRY.getNodeClasses();
    }

    @Override
    public List<TypeHandle> getSupportTypes() {
        return CONSTANT_TYPES;
    }

    @Override
    public List<TypeHandle> getLibrarySupportTypes() {
        return CONSTANT_TYPES;
    }

    @Override
    public List<TypeHandle> getVariableSupportTypes() {
        return VARIABLE_TYPES;
    }

    @Override
    public boolean canExecuteCommand(IGraphCommand command) {
        return !(command instanceof CreateLocalSubgraphCommand
                || command instanceof CreateForeignLocalSubgraphCommand
                || command instanceof CreateSubgraphFromSelectionCommand
                || command instanceof ImportExternalSubgraphCommand);
    }

    @Override
    public void onGraphChanged(GraphLogger logger) {
        if (graphModel instanceof QuestBlueprintGraphModel model) {
            QuestBlueprintGraphDiagnostics.log(model, logger);
        }
    }

    @Override
    protected CustomGraphModelImpl createGraphModel() {
        return new QuestBlueprintGraphModel(this);
    }
}
