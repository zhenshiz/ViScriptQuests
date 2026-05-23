package com.viscriptquests.quest.data.task;

import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegister;
import com.lowdragmc.lowdraglib2.syncdata.annotation.Persisted;
import com.viscriptquests.quest.data.DisplayIcon;
import com.viscriptquests.quest.data.runtime.TaskObjectiveProgress;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Items;

// 实体死亡目标，监听任何原因导致的生物死亡，常用于保护目标这类失败条件。
@LDLRegister(name = "entity_death_task", registry = ITask.ID)
public class EntityDeathTask extends ITask {
    @Persisted
    public String entityType = "minecraft:villager";
    @Persisted
    public int deathCount = 1;
    @Persisted
    public String tag = "";

    public boolean matches(LivingEntity entity) {
        return entity != null && matchesEntityType(entity) && matchesEntityTag(entity);
    }

    @Override
    public boolean checkCompletion(ServerPlayer player) {
        return false;
    }

    @Override
    public boolean onComplete(ServerPlayer player) {
        return true;
    }

    @Override
    public int getRequiredAmount() {
        return Math.max(1, deathCount);
    }

    @Override
    public void refreshObjectiveProgress(ServerPlayer player, TaskObjectiveProgress progress) {
        int required = getRequiredAmount();
        progress.requiredAmount = required;
        progress.currentAmount = progress.completed ? required : Math.min(required, Math.max(0, progress.currentAmount));
        progress.completed = progress.completed || progress.currentAmount >= required;
    }

    @Override
    public boolean refreshesProgressFromPlayerState() {
        return false;
    }

    @Override
    public boolean autoCompleteObjective(ServerPlayer player, TaskObjectiveProgress progress) {
        if (progress.completed) {
            return false;
        }
        refreshObjectiveProgress(player, progress);
        return progress.completed;
    }

    @Override
    protected Component getDefaultTaskHint() {
        Component entityName = entityDisplayName();
        String requiredTag = normalize(tag);
        if (!requiredTag.isEmpty()) {
            return Component.translatable("viscript_quests.task_hint.entity_death_task.with_tag",
                    getRequiredAmount(), entityName, requiredTag);
        }
        return Component.translatable("viscript_quests.task_hint.entity_death_task",
                getRequiredAmount(), entityName);
    }

    @Override
    public DisplayIcon getDisplayIcon() {
        return DisplayIcon.item(Items.TOTEM_OF_UNDYING.getDefaultInstance());
    }

    private boolean matchesEntityType(LivingEntity entity) {
        String requiredType = normalize(entityType);
        if (requiredType.isEmpty()) {
            return true;
        }
        ResourceLocation requiredId = ResourceLocation.tryParse(requiredType);
        return requiredId != null && requiredId.equals(BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()));
    }

    private boolean matchesEntityTag(LivingEntity entity) {
        String requiredTag = normalize(tag);
        return requiredTag.isEmpty() || entity.getTags().contains(requiredTag);
    }

    private Component entityDisplayName() {
        String requiredType = normalize(entityType);
        if (requiredType.isEmpty()) {
            return Component.translatable("viscript_quests.task.entity.any");
        }
        ResourceLocation id = ResourceLocation.tryParse(requiredType);
        if (id != null) {
            EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.getOptional(id).orElse(null);
            if (type != null) {
                return type.getDescription();
            }
        }
        return Component.literal(requiredType);
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}
