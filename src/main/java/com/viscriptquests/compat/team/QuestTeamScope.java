package com.viscriptquests.compat.team;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

/**
 * VSQ 内部使用的队伍范围数据。
 *
 * <p>这里保存的是普通 UUID 和队伍 ID，让运行时同步逻辑不需要关心 VST 的具体数据类。
 */
public record QuestTeamScope(String partyId, UUID leaderId, Set<UUID> memberIds) {
    private static final UUID EMPTY_UUID = new UUID(0L, 0L);

    public QuestTeamScope {
        partyId = partyId == null ? "" : partyId;
        LinkedHashSet<UUID> sanitizedMembers = new LinkedHashSet<>();
        if (memberIds != null) {
            for (UUID memberId : memberIds) {
                if (isValidPlayerId(memberId)) {
                    sanitizedMembers.add(memberId);
                }
            }
        }
        if (isValidPlayerId(leaderId)) {
            sanitizedMembers.add(leaderId);
        } else {
            leaderId = sanitizedMembers.stream().findFirst().orElse(EMPTY_UUID);
        }
        memberIds = Collections.unmodifiableSet(sanitizedMembers);
    }

    public static QuestTeamScope solo(UUID playerId) {
        return new QuestTeamScope("", playerId, Set.of(playerId));
    }

    public boolean isParty() {
        return !partyId.isBlank() && memberIds.size() > 1;
    }

    public UUID leaderOr(UUID fallback) {
        return isValidPlayerId(leaderId) ? leaderId : fallback;
    }

    private static boolean isValidPlayerId(UUID playerId) {
        return playerId != null && !EMPTY_UUID.equals(playerId);
    }
}
