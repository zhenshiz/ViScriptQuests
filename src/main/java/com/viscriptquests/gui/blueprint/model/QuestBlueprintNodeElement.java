package com.viscriptquests.gui.blueprint.model;

import com.lowdragmc.lowdraglib2.editor.ui.Editor;
import com.lowdragmc.lowdraglib2.gui.ui.Style;
import com.lowdragmc.lowdraglib2.nodegraphtookit.gui.GraphInspector;
import com.lowdragmc.lowdraglib2.nodegraphtookit.gui.GraphView;
import com.lowdragmc.lowdraglib2.nodegraphtookit.gui.dependency.ModelUpdateVisitor;
import com.lowdragmc.lowdraglib2.nodegraphtookit.gui.node.CollapsibleInOutNodeElement;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.AbstractNodeModel;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.NodeModel;
import dev.vfyjxf.taffy.style.TaffyDisplay;

public class QuestBlueprintNodeElement extends CollapsibleInOutNodeElement {
    public QuestBlueprintNodeElement(AbstractNodeModel nodeModel) {
        super(nodeModel);
        addClass("__quest-blueprint-node__");
    }

    @Override
    protected void applyCollapsedState(boolean collapsed) {
        super.applyCollapsedState(collapsed);
        hideInlineEditors();
    }

    @Override
    public void updateUIFromModel(ModelUpdateVisitor visitor) {
        super.updateUIFromModel(visitor);
        hideInlineEditors();
    }

    @Override
    protected void onSelectionInspect(GraphInspector inspector) {
        if (graphView != null) {
            inspector.setHistoryStack(graphView.getHistoryStack());
        }
        inspector.inspect(QuestElementPropertyConfigurableHelper.build(getModel(), graphView));
        inspectQuestParametersInEditorRightWindow();
    }

    @Override
    protected void onSelectionChanged() {
        super.onSelectionChanged();
        GraphView view = getGraphView();
        if (view == null) {
            return;
        }
        if (!isSelected() || view.getSelected().size() != 1) {
            clearEditorRightWindow();
        }
    }

    private void hideInlineEditors() {
        if (nodeOptionContainer != null) {
            Style.importantPipeline(nodeOptionContainer.getLayout(),
                    layout -> layout.display(TaffyDisplay.NONE));
        }
        select(".__port-constant-editor__").forEach(element ->
                Style.importantPipeline(element.getLayout(), layout -> layout.display(TaffyDisplay.NONE)));
    }

    private void inspectQuestParametersInEditorRightWindow() {
        Editor editor = getFirstAncestorOfType(Editor.class);
        GraphView view = getGraphView();
        if (editor == null || view == null || !(getModel() instanceof NodeModel nodeModel)) {
            return;
        }
        QuestNodeParametersPanel.inspect(editor, nodeModel, view);
    }

    private void clearEditorRightWindow() {
        Editor editor = getFirstAncestorOfType(Editor.class);
        if (editor != null) {
            editor.inspectorView.clear();
        }
    }
}
