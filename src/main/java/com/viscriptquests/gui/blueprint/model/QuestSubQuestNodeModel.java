package com.viscriptquests.gui.blueprint.model;

import com.lowdragmc.lowdraglib2.nodegraphtookit.editor.GraphEditorView;
import com.lowdragmc.lowdraglib2.nodegraphtookit.gui.GraphElement;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.ChangeHint;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.graph.GraphModel;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.CustomNodeModelImpl;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.SubgraphNodeModel;
import com.lowdragmc.lowdraglib2.syncdata.annotation.Persisted;
import com.viscriptquests.gui.blueprint.QuestBlueprintNodeLibrary;
import lombok.Getter;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * 小任务节点自己的编辑子图。
 * <p>
 * LDLib2 内置子图是独立的 SubgraphNodeModel，这里只复用它的导航能力，
 * 让作者可以直接双击小任务节点进入目标/奖励编辑区域。
 */
public class QuestSubQuestNodeModel extends CustomNodeModelImpl {
    private static final String ADDITIONAL_TAG = "_additional";
    public static final String COPY_SUBGRAPH_TAG = "viscript_quests_subgraph_copy";

    @Persisted
    @Getter
    @Nullable
    private UUID localGraphId;

    public static void putCopiedSubgraph(CompoundTag nodeTag, CompoundTag subgraphTag) {
        CompoundTag additional = nodeTag.contains(ADDITIONAL_TAG, Tag.TAG_COMPOUND)
                ? nodeTag.getCompound(ADDITIONAL_TAG)
                : new CompoundTag();
        additional.put(COPY_SUBGRAPH_TAG, subgraphTag);
        nodeTag.put(ADDITIONAL_TAG, additional);
    }

    @Nullable
    public GraphModel getSubgraphModel() {
        if (getGraphModel() == null || localGraphId == null) {
            return null;
        }
        return getGraphModel().findLocalSubgraphByUid(localGraphId);
    }

    @Nullable
    public GraphModel ensureLocalSubgraph() {
        GraphModel graphModel = getGraphModel();
        if (graphModel == null || getSpawnFlags().isOrphan()) {
            return null;
        }

        GraphModel existing = getSubgraphModel();
        if (existing != null) {
            return existing;
        }

        GraphModel subgraph = graphModel.createLocalSubgraphInstance();
        if (subgraph == null) {
            return null;
        }
        graphModel.addLocalSubgraph(subgraph);
        localGraphId = subgraph.getUid();
        graphModel.getCurrentGraphChangeDescription().addChangedModel(this, ChangeHint.DATA);
        return subgraph;
    }

    public void setLocalSubgraph(@Nullable GraphModel subgraph) {
        localGraphId = subgraph == null ? null : subgraph.getUid();
        GraphModel graphModel = getGraphModel();
        if (graphModel != null) {
            graphModel.getCurrentGraphChangeDescription().addChangedModel(this, ChangeHint.DATA);
        }
    }

    @Override
    public void onCreateNode() {
        ensureLocalSubgraph();
        super.onCreateNode();
    }

    @Override
    public void onDeleteNode() {
        GraphModel subgraph = getSubgraphModel();
        if (subgraph != null && getGraphModel() != null) {
            getGraphModel().removeLocalSubgraph(subgraph);
        }
        super.onDeleteNode();
    }

    @Override
    public void deserializeAdditionalNBT(Tag tag, HolderLookup.Provider provider) {
        super.deserializeAdditionalNBT(tag, provider);
        if (!(tag instanceof CompoundTag compound) || !compound.contains(COPY_SUBGRAPH_TAG)) {
            return;
        }
        GraphModel graphModel = getGraphModel();
        if (graphModel == null) {
            return;
        }
        GraphModel copy = graphModel.createLocalSubgraphInstance();
        if (copy == null) {
            return;
        }
        copy.deserializeNBT(provider, compound.getCompound(COPY_SUBGRAPH_TAG));
        copy.setUid(UUID.randomUUID());
        graphModel.addLocalSubgraph(copy);
        setLocalSubgraph(copy);
    }

    @Override
    public @Nullable GraphElement<?> createElementUI() {
        return new QuestSubQuestNodeElement(this);
    }

    private static class QuestSubQuestNodeElement extends QuestBlueprintNodeElement {
        public QuestSubQuestNodeElement(QuestSubQuestNodeModel nodeModel) {
            super(nodeModel);
        }

        @Override
        protected void buildUI() {
            super.buildUI();
            addEventListener(com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents.DOUBLE_CLICK, event -> {
                if (!(getModel() instanceof QuestSubQuestNodeModel subQuestNode)) {
                    return;
                }
                GraphModel subgraph = subQuestNode.ensureLocalSubgraph();
                if (subgraph == null) {
                    return;
                }
                GraphEditorView editorView = getFirstAncestorOfType(GraphEditorView.class);
                if (editorView == null) {
                    return;
                }

                // GraphEditorView 目前只认识 LDLib2 的 SubgraphNodeModel，这里用临时代理进入同一个本地子图。
                SubgraphNodeModel proxy = new SubgraphNodeModel();
                proxy.setGraphModel(subQuestNode.getGraphModel());
                proxy.setLocalSubgraph(subgraph);
                proxy.setTitle(subQuestNode.getTitle());
                editorView.enterSubgraph(proxy);
                QuestBlueprintNodeLibrary.rebuild(editorView.getCurrentView().itemLibrary, subgraph);
                event.stopPropagation();
            });
        }
    }
}
