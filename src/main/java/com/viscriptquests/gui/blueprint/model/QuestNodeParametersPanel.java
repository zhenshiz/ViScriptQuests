package com.viscriptquests.gui.blueprint.model;

import com.lowdragmc.lowdraglib2.configurator.IConfigurable;
import com.lowdragmc.lowdraglib2.editor.ui.Editor;
import com.lowdragmc.lowdraglib2.gui.ui.Style;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.IFieldValueConfigurable;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.port.PortDirection;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.port.PortModelOptions;
import com.lowdragmc.lowdraglib2.nodegraphtookit.gui.FieldValueInspector;
import com.lowdragmc.lowdraglib2.nodegraphtookit.gui.GraphView;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.NodeModel;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.PortModel;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.WirePortalModel;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class QuestNodeParametersPanel extends UIElement {
    private final NodeModel nodeModel;
    @Nullable
    private final GraphView graphView;
    private List<FieldInfo> fieldInfos = List.of();

    public static void inspect(Editor editor, NodeModel nodeModel, GraphView graphView) {
        var container = editor.inspectorView.getViewContainer();
        if (container != null) {
            container.selectView(editor.inspectorView);
        }
        editor.inspectorView.inspect(IConfigurable.create(group ->
                group.configuratorContainer.addChild(new QuestNodeParametersPanel(nodeModel, graphView))));
    }

    public QuestNodeParametersPanel(NodeModel nodeModel, @Nullable GraphView graphView) {
        this.nodeModel = nodeModel;
        this.graphView = graphView;
        addClass("__quest-node-properties-panel__");
        Style.defaultPipeline(getLayout(), layout -> layout
                .widthPercent(100)
                .paddingAll(2)
                .gapAll(2));
        rebuildFields();
    }

    @Override
    public void screenTick() {
        super.screenTick();
        if (!Objects.equals(fieldInfos, collectFieldInfos())) {
            rebuildFields();
        }
    }

    private void rebuildFields() {
        clearAllChildren();
        List<FieldInfo> nextFields = new ArrayList<>();
        for (var nodeOption : nodeModel.getNodeOptions()) {
            appendField(nodeOption.getPortModel(), nextFields);
        }
        for (var port : nodeModel.getVisibleInputsByDisplayOrder()) {
            if (shouldShowInputPort(port)) {
                appendField(port, nextFields);
            }
        }
        fieldInfos = nextFields;
    }

    private List<FieldInfo> collectFieldInfos() {
        List<FieldInfo> nextFields = new ArrayList<>();
        for (var nodeOption : nodeModel.getNodeOptions()) {
            collectField(nodeOption.getPortModel(), nextFields);
        }
        for (var port : nodeModel.getVisibleInputsByDisplayOrder()) {
            if (shouldShowInputPort(port)) {
                collectField(port, nextFields);
            }
        }
        return nextFields;
    }

    private void appendField(PortModel port, List<FieldInfo> fields) {
        if (!collectField(port, fields)) {
            return;
        }
        FieldValueInspector inspector = new FieldValueInspector();
        inspector.setFieldName(port.getDisplayName());
        if (graphView != null) {
            inspector.setHistoryStack(graphView.getHistoryStack());
        }
        inspector.loadValueField((IFieldValueConfigurable) port);
        addChild(inspector);
    }

    private boolean collectField(PortModel port, List<FieldInfo> fields) {
        if (!(port instanceof IFieldValueConfigurable) || port.getEmbeddedValue() == null
                || !port.isConfiguratorEnabled()) {
            return false;
        }
        var typeHandle = port.getDataTypeHandle();
        fields.add(new FieldInfo(
                port.getUniqueName(),
                typeHandle == null ? "" : typeHandle.getIdentification(),
                port.isConnected()));
        return true;
    }

    private static boolean shouldShowInputPort(PortModel port) {
        if (port.getDirection() != PortDirection.INPUT) {
            return false;
        }
        if (port.getNodeModel() instanceof WirePortalModel) {
            return false;
        }
        if (port.getOptions().hasFlag(PortModelOptions.NODE_OPTION)
                || port.getOptions().hasFlag(PortModelOptions.HIDDEN)
                || port.isConnected()
                || hasConnectedAncestor(port)) {
            return false;
        }
        return port.getEmbeddedValue() != null;
    }

    private static boolean hasConnectedAncestor(PortModel port) {
        PortModel parent = port.getParentPort();
        while (parent != null) {
            if (parent.isConnected()) {
                return true;
            }
            parent = parent.getParentPort();
        }
        return false;
    }

    private record FieldInfo(String id, String typeId, boolean connected) {
    }
}
