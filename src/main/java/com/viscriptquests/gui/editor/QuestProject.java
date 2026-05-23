package com.viscriptquests.gui.editor;

import com.lowdragmc.lowdraglib2.LDLib2;
import com.lowdragmc.lowdraglib2.Platform;
import com.lowdragmc.lowdraglib2.editor.project.IProject;
import com.lowdragmc.lowdraglib2.editor.project.ProjectType;
import com.lowdragmc.lowdraglib2.editor.resource.Resources;
import com.lowdragmc.lowdraglib2.editor.ui.Editor;
import com.lowdragmc.lowdraglib2.gui.texture.Icons;
import com.lowdragmc.lowdraglib2.gui.ui.data.TextWrap;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Dialog;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.networking.rpc.RPCPacketDistributor;
import com.lowdragmc.lowdraglib2.nodegraphtookit.editor.GraphEditorView;
import com.lowdragmc.lowdraglib2.syncdata.ISubscription;
import com.viscriptquests.ViScriptQuests;
import com.viscriptquests.gui.blueprint.QuestBlueprintCompiler;
import com.viscriptquests.gui.blueprint.QuestBlueprintGraph;
import com.viscriptquests.gui.blueprint.QuestBlueprintNodeLibrary;
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
import java.util.function.Supplier;

public class QuestProject implements IProject {
    public static final ProjectType TYPE = new ProjectType(
            Icons.NODE,
            "viscript_quests.editor.quest.add",
            QuestFileHelper.PROJECT_SUFFIX,
            QuestProject::new
    ) {
        @Override
        public IProject loadProjectFromFile(File file) throws Exception {
            QuestProject project = new QuestProject();
            project.deserializeProject(Platform.getFrozenRegistry(),
                    QuestFileHelper.createProjectFileTag(
                            QuestFileHelper.readProjectGraph(file.toPath()).orElseThrow()));
            return project;
        }

        @Override
        public void saveProjectToFile(IProject project, File file) throws Exception {
            if (project instanceof QuestProject questProject) {
                NbtIo.write(QuestFileHelper.createProjectFileTag(questProject.currentGraphTag()), file.toPath());
                return;
            }
            super.saveProjectToFile(project, file);
        }
    };

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
        graphTag = QuestFileHelper.extractProjectGraph(nbt);
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

        editor.placeView(graphEditorView, () -> editor.rootWindow.getLeftTop());

        QuestBlueprintNodeLibrary.rebuild(graphEditorView.graphView.itemLibrary, graph.graphModel);

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
                                        } catch (Exception exception) {
                                            showExportFailure(editor, exception);
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
                                this::uploadProjectToServer
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
                                        uploadQuestToServer(result, questFile);
                                    } catch (Exception exception) {
                                        showExportFailure(editor, exception);
                                    }
                                }
                        ).show(editor);
                    });
                    // 先验证运行时任务文件，再同时上传项目文件和运行时任务文件。
                    m.leaf("viscript_quests.editor.quest.upload_project_and_quest", () -> {
                        refreshGraphSnapshot();
                        Dialog.stringEditorDialog(
                                "viscript_quests.editor.upload_project_and_quest",
                                "",
                                result -> !result.isBlank(),
                                result -> {
                                    try {
                                        QuestFile questFile = QuestBlueprintCompiler.compile(graphTag);
                                        uploadProjectToServer(result);
                                        uploadQuestToServer(result, questFile);
                                    } catch (Exception exception) {
                                        showExportFailure(editor, exception);
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

    private CompoundTag currentGraphTag() {
        refreshGraphSnapshot();
        return graphTag.copy();
    }

    private void uploadProjectToServer(String fileName) {
        CompoundTag data = new CompoundTag();
        data.putString("fileName", fileName);
        data.put("graph", graphTag.copy());
        RPCPacketDistributor.rpcToServer(C2SPayload.UPLOAD_PROJECT_FILE, data);
    }

    private void uploadQuestToServer(String fileName, QuestFile questFile) {
        CompoundTag data = new CompoundTag();
        data.putString("fileName", fileName);
        data.put("quest", questFile.serializeNBT(Platform.getFrozenRegistry()));
        RPCPacketDistributor.rpcToServer(C2SPayload.UPLOAD_QUEST_FILE, data);
    }

    private static void showExportFailure(Editor editor, Exception exception) {
        String message = exception.getMessage();
        if (message == null || message.isBlank()) {
            message = exception.getClass().getSimpleName();
        }
        Dialog dialog = new Dialog();
        dialog.setTitle("viscript_quests.editor.quest.export.validation_failed");
        dialog.addContent(new Label()
                .textStyle(style -> style.textWrap(TextWrap.WRAP).adaptiveHeight(true))
                .setText(Component.translatable("viscript_quests.editor.quest.export.validation_failed.detail", message))
                .layout(layout -> layout.widthPercent(100)));
        dialog.addButton(new Button()
                .setOnClick(event -> dialog.close())
                .setText("ldlib.gui.tips.confirm"));
        dialog.show(editor);
    }

}
