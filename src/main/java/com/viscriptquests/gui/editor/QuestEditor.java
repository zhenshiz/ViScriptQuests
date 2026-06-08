package com.viscriptquests.gui.editor;

import com.lowdragmc.lowdraglib2.Platform;
import com.lowdragmc.lowdraglib2.editor.ui.Editor;
import com.lowdragmc.lowdraglib2.editor.ui.EditorLayout;
import com.lowdragmc.lowdraglib2.editor.ui.EditorWindow;
import com.lowdragmc.lowdraglib2.editor.ui.ViewContainer;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.networking.rpc.RPCPacketDistributor;
import com.viscript_lib.gui.editor.EditorFileNames;
import com.viscript_lib.gui.editor.EditorUploadAction;
import com.viscript_lib.gui.editor.ProjectFileEditor;
import com.viscriptquests.ViScriptQuests;
import com.viscriptquests.network.c2s.C2SPayload;
import com.viscriptquests.quest.data.QuestFile;
import com.viscriptquests.util.QuestFileHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nonnull;
import java.io.File;

public class QuestEditor extends ProjectFileEditor {
    public static final ResourceLocation EDITOR_ID = ViScriptQuests.id("quest_editor");

    public QuestEditor() {
        registerProjectFileType(QuestProject.TYPE);
        rootWindow.setViewContainer(new ViewContainer());
    }

    public static ModularUI createUI() {
        return new ModularUI(UI.of(EditorWindow.open(EDITOR_ID, QuestEditor::new)))
                .shouldCloseOnKeyInventory(false);
    }

    @Override
    protected @Nonnull Editor createNewEditorInstance() {
        return new QuestEditor();
    }

    @Override
    protected EditorUploadAction createUploadProjectAction() {
        if (getCurrentProject() instanceof QuestProject project) {
            return new QuestUploadAction(
                    "viscript_lib.editor.menu.upload_project_file",
                    "viscript_lib.editor.dialog.upload_project_file",
                    defaultBaseName(),
                    QuestFileHelper.PROJECT_SUFFIX,
                    QuestFileHelper::normalizeProjectId,
                    fileName -> uploadProjectToServer(project, fileName)
            );
        }
        return null;
    }

    @Override
    protected EditorUploadAction createUploadRuntimeAction() {
        if (getCurrentProject() instanceof QuestProject project) {
            return new QuestUploadAction(
                    "viscript_lib.editor.menu.upload_runtime_file",
                    "viscript_lib.editor.dialog.upload_runtime_file",
                    defaultBaseName(),
                    QuestFileHelper.QUEST_SUFFIX,
                    QuestFileHelper::normalizeQuestId,
                    fileName -> uploadQuestToServer(project, fileName)
            );
        }
        return null;
    }

    @Override
    protected EditorUploadAction createUploadProjectAndRuntimeAction() {
        if (getCurrentProject() instanceof QuestProject project) {
            return new QuestUploadAction(
                    "viscript_lib.editor.menu.upload_project_and_runtime_file",
                    "viscript_lib.editor.dialog.upload_project_and_runtime_file",
                    defaultBaseName(),
                    "",
                    fileName -> EditorFileNames.normalizeBaseName(
                            fileName,
                            QuestFileHelper.PROJECT_SUFFIX,
                            QuestFileHelper.QUEST_SUFFIX),
                    fileName -> {
                        QuestFile questFile = project.compileQuestFile();
                        uploadProjectToServer(project, fileName);
                        uploadQuestToServer(fileName, questFile);
                    }
            );
        }
        return null;
    }

    @Override
    public void applyLayout(EditorLayout layout) {
        // 任务编辑器固定为单窗口蓝图编辑器，不恢复 LDLib2 默认的资源/历史/检查器多面板布局。
    }

    private String defaultBaseName() {
        File currentFile = getCurrentProjectFile();
        if (currentFile == null) {
            return "test";
        }
        return EditorFileNames.normalizeBaseName(
                currentFile.getName(),
                QuestFileHelper.PROJECT_SUFFIX,
                QuestFileHelper.QUEST_SUFFIX);
    }

    private void uploadProjectToServer(QuestProject project, String fileName) {
        CompoundTag data = new CompoundTag();
        data.putString("fileName", fileName);
        data.put("graph", project.currentGraphTag());
        RPCPacketDistributor.rpcToServer(C2SPayload.UPLOAD_PROJECT_FILE, data);
    }

    private void uploadQuestToServer(QuestProject project, String fileName) {
        uploadQuestToServer(fileName, project.compileQuestFile());
    }

    private void uploadQuestToServer(String fileName, QuestFile questFile) {
        CompoundTag data = new CompoundTag();
        data.putString("fileName", fileName);
        data.put("quest", questFile.serializeNBT(Platform.getFrozenRegistry()));
        RPCPacketDistributor.rpcToServer(C2SPayload.UPLOAD_QUEST_FILE, data);
    }

    @FunctionalInterface
    private interface FileNameNormalizer {
        String normalize(String fileName);
    }

    @FunctionalInterface
    private interface UploadHandler {
        void upload(String fileName) throws Exception;
    }

    private record QuestUploadAction(String displayKey, String dialogTitleKey, String defaultFileName,
                                     String suffix, FileNameNormalizer normalizer,
                                     UploadHandler uploadHandler) implements EditorUploadAction {
        @Override
        public Component getDisplayName() {
            return Component.translatable(displayKey);
        }

        @Override
        public String getDialogTitleKey() {
            return dialogTitleKey;
        }

        @Override
        public String getDefaultFileName() {
            return defaultFileName;
        }

        @Override
        public String getSuffix() {
            return suffix;
        }

        @Override
        public String normalizeFileName(String fileName) {
            return normalizer.normalize(fileName);
        }

        @Override
        public void uploadToServer(String fileName) throws Exception {
            uploadHandler.upload(fileName);
        }
    }
}
