package com.viscriptquests.network.c2s;

import com.lowdragmc.lowdraglib2.Platform;
import com.lowdragmc.lowdraglib2.networking.rpc.RPCPacket;
import com.lowdragmc.lowdraglib2.networking.rpc.RPCPacketDistributor;
import com.lowdragmc.lowdraglib2.syncdata.rpc.RPCSender;
import com.viscriptquests.ViScriptQuests;
import com.viscriptquests.network.s2c.S2CPayload;
import com.viscriptquests.quest.data.runtime.QuestCategoryListData;
import com.viscriptquests.quest.data.QuestFile;
import com.viscriptquests.gui.blueprint.compiler.QuestBlueprintValidator;
import com.viscriptquests.quest.runtime.QuestManager;
import com.viscriptquests.quest.runtime.QuestTrackingService;
import com.viscriptquests.util.QuestFileHelper;
import com.viscriptquests.quest.data.QuestSavedData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.io.IOException;

public class C2SPayload {
    public static final String UPLOAD_PROJECT_FILE = ViScriptQuests.MOD_ID + ":upload_project_file";
    public static final String UPLOAD_QUEST_FILE = ViScriptQuests.MOD_ID + ":upload_quest_file";
    public static final String SAVE_DEFAULT_QUEST_CATEGORIES = ViScriptQuests.MOD_ID + ":save_default_quest_categories";
    public static final String SAVE_TRACKED_QUEST = ViScriptQuests.MOD_ID + ":save_tracked_quest";
    public static final String SUBMIT_QUEST_TASK = ViScriptQuests.MOD_ID + ":submit_quest_task";
    public static final String REQUEST_OPEN_QUEST_BOOK = ViScriptQuests.MOD_ID + ":request_open_quest_book";

    // 客户端上传项目文件到服务端（.questproj），保存完整图数据供后续编辑
    @RPCPacket(value = UPLOAD_PROJECT_FILE, modId = ViScriptQuests.MOD_ID)
    public static void uploadProjectFile(RPCSender sender, CompoundTag data) {
        ServerPlayer player = sender.asPlayer();
        if (player == null) return;
        String fileName = data.getString("fileName");
        if (fileName.isBlank()) return;
        CompoundTag graphTag = data.getCompound("graph");
        try {
            QuestFileHelper.writeProject(fileName, graphTag);
        } catch (IOException e) {
            ViScriptQuests.LOGGER.error(Component.translatable("commands.viscript_quests.quest.upload.project.failed", e.getMessage()).getString());
        }
    }

    // 客户端编译并上传运行时文件到服务端（.quest），QuestManager 可直接加载执行
    @RPCPacket(value = UPLOAD_QUEST_FILE, modId = ViScriptQuests.MOD_ID)
    public static void uploadQuestFile(RPCSender sender, CompoundTag data) {
        ServerPlayer player = sender.asPlayer();
        if (player == null) return;
        String fileName = data.getString("fileName");
        if (fileName.isBlank()) return;
        CompoundTag questTag = data.getCompound("quest");
        try {
            QuestFile questFile = new QuestFile();
            questFile.deserializeNBT(Platform.getFrozenRegistry(), questTag);
            QuestBlueprintValidator.validateExport(questFile);
            QuestFileHelper.writeQuest(fileName, questFile, Platform.getFrozenRegistry());
            QuestFileHelper.clearCache();
        } catch (Exception e) {
            ViScriptQuests.LOGGER.error(Component.translatable("commands.viscript_quests.quest.upload.quest.failed", e.getMessage()).getString());
        }
    }

    @RPCPacket(value = SAVE_DEFAULT_QUEST_CATEGORIES, modId = ViScriptQuests.MOD_ID)
    public static void saveDefaultQuestCategories(RPCSender sender, CompoundTag data) {
        ServerPlayer player = sender.asPlayer();
        if (player == null || !player.hasPermissions(2)) return;
        QuestCategoryListData listData = new QuestCategoryListData();
        listData.deserializeNBT(Platform.getFrozenRegistry(), data);
        QuestSavedData.get(player.getServer()).replaceDefaultCategories(listData.copyCategories());
    }

    @RPCPacket(value = SAVE_TRACKED_QUEST, modId = ViScriptQuests.MOD_ID)
    public static void saveTrackedQuest(RPCSender sender, CompoundTag data) {
        ServerPlayer player = sender.asPlayer();
        if (player == null) return;
        QuestSavedData savedData = QuestSavedData.get(player.getServer());
        var playerData = savedData.getPlayer(player.getUUID());
        String trackedQuestId = data.getString("trackedQuestId");
        String trackedStepId = data.getString("trackedStepId");
        if (trackedQuestId.isBlank()) {
            playerData.trackedQuestId = "";
            playerData.trackedStepId = "";
            savedData.setDirty();
            QuestTrackingService.refresh(player);
            return;
        }
        var questState = playerData.findQuest(trackedQuestId);
        if (questState.isEmpty()) {
            playerData.trackedQuestId = "";
            playerData.trackedStepId = "";
            savedData.setDirty();
            QuestTrackingService.refresh(player);
            return;
        }
        playerData.trackedQuestId = trackedQuestId;
        playerData.trackedStepId = trackedStepId.isBlank() || questState.get().findStepProgress(trackedStepId).isPresent()
                ? trackedStepId
                : "";
        savedData.setDirty();
        QuestTrackingService.refresh(player);
    }

    @RPCPacket(value = REQUEST_OPEN_QUEST_BOOK, modId = ViScriptQuests.MOD_ID)
    public static void openQuestBook(RPCSender sender) {
        ServerPlayer player = sender.asPlayer();
        if (player == null) return;
        QuestManager.openQuestBook(player);
    }

    @RPCPacket(value = SUBMIT_QUEST_TASK, modId = ViScriptQuests.MOD_ID)
    public static void submitQuestTask(RPCSender sender, CompoundTag data) {
        ServerPlayer player = sender.asPlayer();
        if (player == null) return;
        String questId = data.getString("questId");
        String stepId = data.getString("stepId");
        int objectiveIndex = data.getInt("objectiveIndex");
        if (!questId.isBlank() && !stepId.isBlank()) {
            QuestManager.submitObjective(player, questId, stepId, objectiveIndex);
        }
        QuestManager.refreshQuestBookDisplayData(player);
        QuestTrackingService.refresh(player);
        CompoundTag response = QuestSavedData.get(player.getServer())
                .getPlayer(player.getUUID())
                .serializeNBT(Platform.getFrozenRegistry());
        RPCPacketDistributor.rpcToPlayer(player, S2CPayload.SYNC_QUEST_BOOK, response);
    }
}
