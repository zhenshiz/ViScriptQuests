package com.viscriptquests.quest.event;

import com.viscriptquests.ViScriptQuests;
import com.viscriptquests.quest.runtime.QuestSubmissionService;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@EventBusSubscriber(modid = ViScriptQuests.MOD_ID)
public class EntityInteractEvents {
    private static final Map<UUID, InteractionStamp> LAST_INTERACTIONS = new HashMap<>();

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onEntityInteractSpecific(PlayerInteractEvent.EntityInteractSpecific event) {
        if (event.isCanceled() || !(event.getEntity() instanceof ServerPlayer player) || player.level().isClientSide()) {
            return;
        }
        recordInteraction(player, event.getTarget(), event.getHand());
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (event.isCanceled() || !(event.getEntity() instanceof ServerPlayer player) || player.level().isClientSide()) {
            return;
        }
        recordInteraction(player, event.getTarget(), event.getHand());
    }

    private static void recordInteraction(ServerPlayer player, Entity target, InteractionHand hand) {
        if (target == null) {
            return;
        }
        // 同一次右键可能先触发精确交互，再触发普通交互；按 tick、实体和手去重，避免进度加两次。
        InteractionStamp stamp = new InteractionStamp(target.getId(), hand, player.level().getGameTime());
        UUID playerId = player.getUUID();
        if (stamp.equals(LAST_INTERACTIONS.get(playerId))) {
            return;
        }
        LAST_INTERACTIONS.put(playerId, stamp);
        QuestSubmissionService.recordEntityInteraction(player, target);
    }

    private record InteractionStamp(int targetId, InteractionHand hand, long gameTime) {
    }
}
