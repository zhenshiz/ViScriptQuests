package com.viscriptquests.gui.editor;

import com.lowdragmc.lowdraglib2.Platform;
import com.lowdragmc.lowdraglib2.editor.resource.Resources;
import com.lowdragmc.lowdraglib2.editor.ui.Editor;
import com.lowdragmc.lowdraglib2.gui.texture.Icons;
import com.lowdragmc.lowdraglib2.nodegraphtookit.editor.GraphEditorView;
import com.viscript_lib.gui.editor.IRuntimeFileProject;
import com.viscript_lib.gui.editor.ProjectFileProjectType;
import com.viscriptquests.gui.blueprint.compiler.QuestBlueprintCompiler;
import com.viscriptquests.gui.blueprint.QuestBlueprintExamples;
import com.viscriptquests.gui.blueprint.QuestBlueprintGraph;
import com.viscriptquests.gui.blueprint.QuestBlueprintNodeLibrary;
import com.viscriptquests.quest.data.QuestFile;
import com.viscriptquests.util.QuestFileHelper;
import lombok.Getter;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

public class QuestProject implements IRuntimeFileProject {
    public static final ProjectFileProjectType TYPE = new QuestProjectType();

    @Getter
    private final Resources resources = Resources.EMPTY;
    private CompoundTag graphTag = new CompoundTag();
    @Nullable
    private transient GraphEditorView graphEditorView;
    @Nullable
    private transient Supplier<CompoundTag> graphSnapshotSupplier;
    @Nullable
    private transient QuestBlueprintGraph deserializedGraph;
    private boolean aiPreviewing;

    /** 保存可恢复的图对象及其视图，保留 AI 操作之前的节点历史引用。 */
    public static final class AiState {
        private final QuestBlueprintGraph graph;
        private GraphEditorView view;

        private AiState(QuestBlueprintGraph graph, @Nullable GraphEditorView view) {
            this.graph = graph;
            this.view = view;
        }
    }

    /**
     * 获取当前图的可逆状态，不复制已在历史中使用的节点对象。
     *
     * @return 可由此项目恢复的图状态
     */
    public AiState captureAiState() {
        refreshGraphSnapshot();
        var graph = graphEditorView != null && graphEditorView.getGraph() instanceof QuestBlueprintGraph live
                ? live : deserializeGraph(Platform.getFrozenRegistry(), graphTag);
        return new AiState(graph, graphEditorView);
    }

    /**
     * 恢复图和视图，不通过重新载入项目清空原生历史。
     *
     * @param state 先前捕获的图状态
     * @param editor 当前项目所属编辑器
     */
    public void installAiState(AiState state, Editor editor) {
        var before = captureAiState();
        try { installState(state, editor); }
        catch (RuntimeException exception) { installState(before, editor); throw exception; }
    }

    private void installState(AiState state, Editor editor) {
        if (graphEditorView != null) graphEditorView.removeSelf();
        editor.inspectorView.clear();
        graphTag = state.graph.graphModel.serializeNBT(Platform.getFrozenRegistry());
        if (state.view == null) state.view = createGraphView(state.graph);
        graphEditorView = state.view;
        graphSnapshotSupplier = graphEditorView::serializeGraph;
        deserializedGraph = null;
        editor.placeView(graphEditorView, () -> editor.centerWindow.getLeftTop());
        setAiPreviewing(aiPreviewing);
    }

    /**
     * 切换图的只读预览，允许选择和进入小任务子图。
     *
     * @param previewing 是否等待玩家确认 AI 修改
     */
    public void setAiPreviewing(boolean previewing) {
        aiPreviewing = previewing;
        if (graphEditorView != null) graphEditorView.setReadOnly(previewing);
    }

    public static QuestProject createProject(CompoundTag graphTag) {
        QuestProject project = new QuestProject();
        project.graphTag = graphTag.copy();
        return project;
    }

    @Override
    public ProjectFileProjectType getProjectType() {
        return TYPE;
    }

    @Override
    public void initNewProject() {
        graphTag = QuestBlueprintExamples.createSimpleQuestGraphTag();
    }

    @Override
    public CompoundTag serializeProject(@NotNull HolderLookup.Provider provider) {
        refreshGraphSnapshot();
        CompoundTag tag = new CompoundTag();
        tag.put("graph", graphTag.copy());
        return tag;
    }

    @Override
    public CompoundTag serializeRuntimeFile(@NotNull HolderLookup.Provider provider) {
        return compileQuestFile().serializeNBT(provider);
    }

    @Override
    public void deserializeProject(@NotNull HolderLookup.Provider provider, @NotNull CompoundTag nbt) {
        graphTag = nbt.getCompound("graph").copy();
        deserializedGraph = deserializeGraph(provider, graphTag);
    }

    @Override
    public void onLoad(Editor editor) {
        QuestBlueprintGraph graph = deserializedGraph == null
                ? deserializeGraph(Platform.getFrozenRegistry(), graphTag)
                : deserializedGraph;
        deserializedGraph = null;

        graphEditorView = createGraphView(graph);
        graphSnapshotSupplier = graphEditorView::serializeGraph;
        editor.placeView(graphEditorView, () -> editor.centerWindow.getLeftTop());
    }

    private GraphEditorView createGraphView(QuestBlueprintGraph graph) {
        var view = new GraphEditorView(QuestGraphView::new).loadGraph(graph, savedGraph -> graphTag = savedGraph.copy());
        view.setCanRemove(false);
        view.setIcon(Icons.NODE);
        view.setDynamicName(() -> Component.translatable("viscript_quests.editor.view.quest_blueprint"));

        QuestBlueprintNodeLibrary.rebuild(view.graphView.itemLibrary, graph.graphModel);
        return view;
    }

    @Override
    public void onClosed(Editor editor) {
        refreshGraphSnapshot();
        editor.inspectorView.clear();
        if (graphEditorView != null) {
            graphEditorView.clear();
            graphEditorView.removeSelf();
        }
        graphEditorView = null;
        graphSnapshotSupplier = null;
        deserializedGraph = null;
    }

    public QuestFile compileQuestFile() {
        refreshGraphSnapshot();
        return QuestBlueprintCompiler.compile(graphTag);
    }

    public CompoundTag currentGraphTag() {
        refreshGraphSnapshot();
        return graphTag.copy();
    }

    private void refreshGraphSnapshot() {
        if (graphSnapshotSupplier != null) {
            graphTag = graphSnapshotSupplier.get().copy();
        }
    }

    private static QuestBlueprintGraph deserializeGraph(HolderLookup.Provider provider, CompoundTag graphTag) {
        QuestBlueprintGraph graph = new QuestBlueprintGraph();
        if (!graphTag.isEmpty()) {
            graph.graphModel.deserializeNBT(provider, graphTag.copy());
        }
        return graph;
    }

    private static final class QuestGraphView extends com.lowdragmc.lowdraglib2.nodegraphtookit.gui.GraphView {
        @Override
        public com.lowdragmc.lowdraglib2.nodegraphtookit.gui.GraphView setReadOnly(boolean readOnly) {
            boolean changed = isReadOnly() != readOnly;
            super.setReadOnly(readOnly);
            if (changed && getGraph() != null) rebuildGraphUI();
            return this;
        }

        @Override
        public com.lowdragmc.lowdraglib2.nodegraphtookit.gui.GraphView loadGraph(
                @Nullable com.lowdragmc.lowdraglib2.nodegraphtookit.api.graph.Graph graph) {
            // AI 预览切换会重新挂接已有视图；同一图保留原画布和手动编辑历史。
            if (graph != null && getGraph() == graph) return this;
            return super.loadGraph(graph);
        }
    }

    private static class QuestProjectType extends ProjectFileProjectType {
        private QuestProjectType() {
            super(Icons.NODE, "viscript_quests.editor.quest.add", QuestFileHelper.FORMAT, QuestProject::new);
        }
    }
}
