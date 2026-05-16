package com.viscriptquests.gui.editor;

import com.lowdragmc.lowdraglib2.LDLib2;
import com.lowdragmc.lowdraglib2.Platform;
import com.lowdragmc.lowdraglib2.editor.project.IProject;
import com.lowdragmc.lowdraglib2.editor.project.ProjectType;
import com.lowdragmc.lowdraglib2.editor.resource.Resources;
import com.lowdragmc.lowdraglib2.editor.ui.Editor;
import com.lowdragmc.lowdraglib2.gui.texture.Icons;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Dialog;
import com.lowdragmc.lowdraglib2.gui.util.TreeBuilder;
import com.lowdragmc.lowdraglib2.networking.rpc.RPCPacketDistributor;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.node.Node;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.node.NodeAttribute;
import com.lowdragmc.lowdraglib2.nodegraphtookit.editor.GraphEditorView;
import com.lowdragmc.lowdraglib2.nodegraphtookit.gui.itemlibrary.ItemLibrary;
import com.lowdragmc.lowdraglib2.nodegraphtookit.gui.itemlibrary.ItemLibraryItem;
import com.lowdragmc.lowdraglib2.nodegraphtookit.gui.itemlibrary.NodeModelLibraryItem;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.graph.CustomGraphModelImpl;
import com.lowdragmc.lowdraglib2.syncdata.ISubscription;
import com.viscriptquests.ViScriptQuests;
import com.viscriptquests.gui.blueprint.QuestBlueprintCompiler;
import com.viscriptquests.gui.blueprint.QuestBlueprintGraph;
import com.viscriptquests.network.c2s.C2SPayload;
import com.viscriptquests.quest.data.QuestFile;
import com.viscriptquests.util.QuestFileHelper;
import lombok.Getter;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public class QuestProject implements IProject {
    public static final ProjectType TYPE = ProjectType.of(
            Icons.NODE,
            "viscript_quests.editor.quest.add",
            QuestFileHelper.PROJECT_SUFFIX,
            QuestProject::new
    );

    // 节点分类显示顺序
    private static final String[] CATEGORY_ORDER = {"flow", "task", "reward", "logic", "math", "debug"};

    @Getter
    private final Resources resources = Resources.EMPTY;
    private CompoundTag graphTag = new CompoundTag();
    @Nullable
    private transient GraphEditorView graphEditorView;
    @Nullable
    private transient Supplier<CompoundTag> graphSnapshotSupplier;
    @Nullable
    private transient ISubscription exportMenuSubscription;

    public static QuestProject createProject(CompoundTag graphTag) {
        QuestProject project = new QuestProject();
        CompoundTag wrapper = new CompoundTag();
        wrapper.put("graph", graphTag);
        project.deserializeProject(Platform.getFrozenRegistry(), wrapper);
        return project;
    }

    @Override
    public ProjectType getProjectType() {
        return TYPE;
    }

    @Override
    public void initNewProject() {
        graphTag = new QuestBlueprintGraph().graphModel.serializeNBT(Platform.getFrozenRegistry());
    }

    @Override
    public CompoundTag serializeProject(@NotNull HolderLookup.Provider provider) {
        refreshGraphSnapshot();
        CompoundTag tag = new CompoundTag();
        tag.put("graph", graphTag.copy());
        return tag;
    }

    @Override
    public void deserializeProject(@NotNull HolderLookup.Provider provider, @NotNull CompoundTag nbt) {
        graphTag = nbt.contains("graph") ? nbt.getCompound("graph").copy() : new CompoundTag();
    }

    @Override
    public void onLoad(Editor editor) {
        QuestBlueprintGraph graph = new QuestBlueprintGraph();
        if (!graphTag.isEmpty()) {
            graph.graphModel.deserializeNBT(Platform.getFrozenRegistry(), graphTag.copy());
        }

        graphEditorView = new GraphEditorView().loadGraph(graph, savedGraph -> graphTag = savedGraph.copy());
        graphSnapshotSupplier = graphEditorView::serializeGraph;
        graphEditorView.setCanRemove(false);
        graphEditorView.setIcon(Icons.NODE);
        graphEditorView.setDynamicName(() -> Component.translatable("viscript_quests.editor.view.quest_blueprint"));

        editor.centerWindow.getLeftTop().addView(graphEditorView);

        rebuildCategorizedNodeLibrary(graphEditorView.graphView.itemLibrary, graph);

        // 注册导出/上传菜单
        if (exportMenuSubscription != null) {
            exportMenuSubscription.unsubscribe();
        }
        exportMenuSubscription = editor.fileMenu.registerMenuCreator((tab, menu) ->
                menu.branch("viscript_quests.editor.quest.export", m -> {
                    // 导出任务文件到本地磁盘
                    m.leaf("viscript_quests.editor.quest.export", () -> {
                        refreshGraphSnapshot();
                        Dialog.showFileDialog(
                                "viscript_quests.editor.saveAs",
                                new File(LDLib2.getAssetsDir(), ViScriptQuests.MOD_ID + "/quest/"),
                                false,
                                Dialog.suffixFilter(QuestFileHelper.QUEST_SUFFIX),
                                file -> {
                                    if (file != null && !file.isDirectory()) {
                                        if (!file.getName().endsWith(QuestFileHelper.QUEST_SUFFIX)) {
                                            file = new File(file.getParentFile(),
                                                    file.getName() + QuestFileHelper.QUEST_SUFFIX);
                                        }
                                        try {
                                            QuestFile questFile = QuestBlueprintCompiler.compile(graphTag);
                                            NbtIo.writeCompressed(questFile.serializeNBT(Platform.getFrozenRegistry()), file.toPath());
                                            QuestFileHelper.clearCache();
                                        } catch (Exception ignored) {
                                        }
                                    }
                                }).show(editor);
                    });
                    // 上传项目文件到服务端（.questproj），保存完整编辑器数据
                    m.leaf("viscript_quests.editor.quest.upload_project", () -> {
                        refreshGraphSnapshot();
                        Dialog.stringEditorDialog(
                                "viscript_quests.editor.upload_project",
                                "",
                                result -> !result.isBlank(),
                                result -> {
                                    CompoundTag data = new CompoundTag();
                                    data.putString("fileName", result);
                                    data.put("graph", graphTag.copy());
                                    RPCPacketDistributor.rpcToServer(C2SPayload.UPLOAD_PROJECT_FILE, data);
                                }
                        ).show(editor);
                    });
                    // 编译并上传运行时文件到服务端（.quest）
                    m.leaf("viscript_quests.editor.quest.upload_quest", () -> {
                        refreshGraphSnapshot();
                        Dialog.stringEditorDialog(
                                "viscript_quests.editor.upload_quest",
                                "",
                                result -> !result.isBlank(),
                                result -> {
                                    try {
                                        QuestFile questFile = QuestBlueprintCompiler.compile(graphTag);
                                        CompoundTag data = new CompoundTag();
                                        data.putString("fileName", result);
                                        data.put("quest", questFile.serializeNBT(Platform.getFrozenRegistry()));
                                        RPCPacketDistributor.rpcToServer(C2SPayload.UPLOAD_QUEST_FILE, data);
                                    } catch (Exception ignored) {
                                    }
                                }
                        ).show(editor);
                    });
                })
        );
    }

    @Override
    public void onClosed(Editor editor) {
        refreshGraphSnapshot();
        if (exportMenuSubscription != null) {
            exportMenuSubscription.unsubscribe();
            exportMenuSubscription = null;
        }
        if (graphEditorView != null) {
            graphEditorView.clear();
            graphEditorView.removeSelf();
        }
        graphEditorView = null;
        graphSnapshotSupplier = null;
    }

    private void refreshGraphSnapshot() {
        if (graphSnapshotSupplier != null) {
            graphTag = graphSnapshotSupplier.get().copy();
        }
    }

    /**
     * 按 @NodeAttribute.group 分类重建节点库，使用翻译键显示分类和节点名称。
     * 替换 LDLib2 默认的平铺英文类名列表。
     */
    private static void rebuildCategorizedNodeLibrary(ItemLibrary itemLibrary, QuestBlueprintGraph graph) {
        List<Class<? extends Node>> nodes = graph.getSupportNodes();

        // 按 group 分组
        Map<String, List<Class<? extends Node>>> grouped = new LinkedHashMap<>();
        for (var nodeClass : nodes) {
            var attr = nodeClass.getAnnotation(NodeAttribute.class);
            String group = (attr != null && !attr.group().isEmpty()) ? attr.group() : "other";
            grouped.computeIfAbsent(group, k -> new ArrayList<>()).add(nodeClass);
        }

        var rootItem = new ItemLibraryItem()
                .setIcon(Icons.NODE)
                .setDisplayName(Component.translatable("graph.library.nodes"));
        var builder = TreeBuilder.<ItemLibraryItem, Void>start(rootItem);

        // 按预定义顺序遍历分类
        for (String group : CATEGORY_ORDER) {
            var nodesInGroup = grouped.remove(group);
            if (nodesInGroup == null || nodesInGroup.isEmpty()) continue;

            var categoryItem = new ItemLibraryItem()
                    .setIcon(Icons.NODE)
                    .setDisplayName(Component.translatable(
                            ViScriptQuests.MOD_ID + ".blueprint.category." + group));

            builder.branch(categoryItem, branchBuilder -> {
                for (var nodeClass : nodesInGroup) {
                    addNodeLeaf(branchBuilder, nodeClass);
                }
            });
        }

        // 处理不在预定义顺序中的其余分类
        for (var entry : grouped.entrySet()) {
            var categoryItem = new ItemLibraryItem()
                    .setIcon(Icons.NODE)
                    .setDisplayName(Component.translatable(
                            ViScriptQuests.MOD_ID + ".blueprint.category." + entry.getKey()));

            builder.branch(categoryItem, branchBuilder -> {
                for (var nodeClass : entry.getValue()) {
                    addNodeLeaf(branchBuilder, nodeClass);
                }
            });
        }

        var root = builder.build();
        itemLibrary.nodeTree.setRoot(root);
        itemLibrary.nodeTree.expandNode(root);
    }

    private static void addNodeLeaf(TreeBuilder<ItemLibraryItem, Void> builder, Class<? extends Node> nodeClass) {
        var attr = nodeClass.getAnnotation(NodeAttribute.class);
        String nodeId = (attr != null && !attr.name().isEmpty()) ? attr.name() : nodeClass.getSimpleName();
        String translationKey = ViScriptQuests.MOD_ID + ".blueprint.node." + nodeId;

        var nodeItem = new NodeModelLibraryItem(
                translationKey,
                data -> CustomGraphModelImpl.createNodeFromData(data, nodeClass)
        );
        // searchableName 包含翻译键、节点 id 和类名，方便中英文搜索
        nodeItem.setSearchableName(translationKey + " " + nodeId + " " + nodeClass.getSimpleName());
        builder.leaf(nodeItem, null);
    }
}
