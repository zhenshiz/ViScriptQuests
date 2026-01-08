package com.viscriptquests.gui.project;

import com.lowdragmc.lowdraglib2.LDLib2;
import com.lowdragmc.lowdraglib2.Platform;
import com.lowdragmc.lowdraglib2.editor.project.IProject;
import com.lowdragmc.lowdraglib2.editor.project.ProjectType;
import com.lowdragmc.lowdraglib2.editor.resource.Resources;
import com.lowdragmc.lowdraglib2.editor.resource.TexturesResource;
import com.lowdragmc.lowdraglib2.editor.ui.Editor;
import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Dialog;
import com.lowdragmc.lowdraglib2.syncdata.ISubscription;
import com.viscriptquests.ViScriptQuests;
import com.viscriptquests.gui.data.Quest;
import com.viscriptquests.util.QuestHelper;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.io.File;

public class QuestProject implements IProject {
    public static int VERSION = 1;
    public static final ProjectType PROVIDER = ProjectType.of(IGuiTexture.EMPTY, Component.translatable("viscript_quests.editor.quest.add").getString(), ".questproj", QuestProject::new);

    public Quest quest = new Quest();

    // runtime
    //导出shop数据文本按钮
    @Nullable
    private ISubscription exportMenuSubscription;


    @Override
    public String getVersion() {
        return "%d.0".formatted(VERSION);
    }

    @Override
    public Resources getResources() {
        return Resources.of(
                TexturesResource.INSTANCE
        );
    }

    @Override
    public ProjectType getProjectType() {
        return PROVIDER;
    }

    @Override
    public CompoundTag serializeProject(@NotNull HolderLookup.Provider provider) {
        var data = new CompoundTag();
        data.put("quest", quest.serializeNBT(provider));
        return data;
    }

    @Override
    public void deserializeProject(@NotNull HolderLookup.Provider provider, @NotNull CompoundTag nbt) {
        quest.deserializeNBT(provider, nbt.getCompound("quest"));
    }

    @Override
    public CompoundTag getMetadata() {
        var meta = IProject.super.getMetadata();
        meta.putInt("version_num", VERSION);
        return meta;
    }

    @Override
    public void onLoad(Editor editor) {
        IProject.super.onLoad(editor);
        if (exportMenuSubscription != null) {
            exportMenuSubscription.unsubscribe();
        }
        exportMenuSubscription = editor.fileMenu.registerMenuCreator((tab, menu) ->
                menu.branch("viscript_quests.editor.shop.export", m ->
                        m.leaf("viscript_quests.editor.shop.export", () -> {
                            Dialog.showFileDialog("viscript_quests.editor.saveAs", new File(LDLib2.getAssetsDir(), "%s/quest/".formatted(ViScriptQuests.MOD_ID)), false,
                                    Dialog.suffixFilter(Quest.SUFFIX), file -> {
                                        if (file != null && !file.isDirectory()) {
                                            if (!file.getName().endsWith(Quest.SUFFIX)) {
                                                file = new File(file.getParentFile(), file.getName() + Quest.SUFFIX);
                                            }
                                            try {
                                                var fileData = quest.serializeNBT(Platform.getFrozenRegistry());
                                                NbtIo.writeCompressed(fileData, file.toPath());
                                                QuestHelper.clearCache();
                                            } catch (Exception ignored) {
                                            }
                                        }
                                    }).show(editor);
                        })
                ));
    }

    @Override
    public void onClosed(Editor editor) {
        IProject.super.onClosed(editor);
        if (exportMenuSubscription != null) {
            exportMenuSubscription.unsubscribe();
            exportMenuSubscription = null;
        }
    }
}
