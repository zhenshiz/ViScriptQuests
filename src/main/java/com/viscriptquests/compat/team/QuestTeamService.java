package com.viscriptquests.compat.team;

import com.lowdragmc.lowdraglib2.Platform;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.fml.ModList;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * ViScriptTeam 的可选联动入口。
 *
 * <p>VST 是联动模组，不是前置。所有直接调用 VST API 的地方都必须先经过 isLoaded 判断。
 */
public class QuestTeamService {
    public static final String VST_MOD_ID = "viscript_team";

    public static boolean isLoaded() {
        return Platform.isModLoaded(VST_MOD_ID);
    }

    public static QuestTeamScope scopeOf(ServerPlayer player) {
        if (player == null) {
            return QuestTeamScope.solo(new UUID(0L, 0L));
        }
        if (!isLoaded()) {
            return QuestTeamScope.solo(player.getUUID());
        }
        return QuestTeamBridge.scopeOf(player);
    }

    public static List<ServerPlayer> onlineMembers(MinecraftServer server, QuestTeamScope scope) {
        List<ServerPlayer> players = new ArrayList<>();
        if (server == null || scope == null) {
            return players;
        }
        for (UUID memberId : scope.memberIds()) {
            ServerPlayer player = server.getPlayerList().getPlayer(memberId);
            if (player != null) {
                players.add(player);
            }
        }
        return players;
    }
}
