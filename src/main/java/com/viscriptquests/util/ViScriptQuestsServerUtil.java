package com.viscriptquests.util;

import com.lowdragmc.lowdraglib2.integration.kjs.KJSBindings;
import com.lowdragmc.lowdraglib2.networking.rpc.RPCPacketDistributor;
import com.viscriptquests.ViScriptQuests;
import com.viscriptquests.network.s2c.S2CPayload;
import dev.latvian.mods.kubejs.typings.Info;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;

@KJSBindings(value = "server", modId = "viscript_quests")
public class ViScriptQuestsServerUtil {

    @Info("打开任务编辑器")
    public static void openQuestEditor(ServerPlayer player) {
        RPCPacketDistributor.rpcToPlayer(player, S2CPayload.OPEN_QUEST_EDITOR);
    }

    @Info("服务端打开任务书")
    public static void openQuestBook(ServerPlayer player) {
        RPCPacketDistributor.rpcToPlayer(player, S2CPayload.OPEN_QUEST_BOOK);
    }

    @Info("获取玩家任务列表")
    public static List<CategoryQuest> getCateQuestsForPlayer(ServerPlayer player) {
        return ViScriptQuests.getQuestSavedData().getCategoryQuests(player);
    }
}
