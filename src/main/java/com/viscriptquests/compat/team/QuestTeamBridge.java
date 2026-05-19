package com.viscriptquests.compat.team;

import com.viscript_team.data.party.Party;
import com.viscript_team.util.ViScriptTeamServerUtil;
import net.minecraft.server.level.ServerPlayer;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

/**
 * 真正调用 ViScriptTeam API 的桥接类。
 *
 * <p>只允许在 QuestTeamService 确认 VST 已加载后进入这里，避免 VSQ 把 VST 变成硬前置。
 */
class QuestTeamBridge {
    static QuestTeamScope scopeOf(ServerPlayer player) {
        String partyId = ViScriptTeamServerUtil.getPlayerPartyId(player);
        if (partyId == null || partyId.isBlank()) {
            return QuestTeamScope.solo(player.getUUID());
        }
        Party party = ViScriptTeamServerUtil.getParty(player.serverLevel(), partyId);
        if (party == null) {
            return QuestTeamScope.solo(player.getUUID());
        }
        Set<UUID> memberIds = new LinkedHashSet<>(party.getMembers());
        memberIds.add(player.getUUID());
        if (Party.isValidPlayer(party.getLeaderId())) {
            memberIds.add(party.getLeaderId());
        }
        return new QuestTeamScope(party.getId(), party.getLeaderId(), memberIds);
    }
}
