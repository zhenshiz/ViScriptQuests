package com.viscriptquests.quest.data.task;

import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegister;
import com.lowdragmc.lowdraglib2.syncdata.annotation.Persisted;
import com.viscriptquests.quest.data.DisplayIcon;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Items;

// 实体交互目标，进度由玩家右键实体事件触发，支持实体类型和命令标签过滤。
@LDLRegister(name = "interact_entity_task", registry = ITask.ID)
public class InteractEntityTask extends ITask {
    @Persisted
    public String entityType = "minecraft:pig";
    @Persisted
    public String tag = "";

    public boolean matches(Entity entity) {
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
    public boolean refreshesProgressFromPlayerState() {
        return false;
    }

    @Override
    protected Component getDefaultTaskHint() {
        Component entityName = entityDisplayName();
        String requiredTag = normalize(tag);
        if (!requiredTag.isEmpty()) {
            return Component.translatable("viscript_quests.task_hint.interact_entity_task.with_tag",
                    entityName, requiredTag);
        }
        return Component.translatable("viscript_quests.task_hint.interact_entity_task", entityName);
    }

    @Override
    public DisplayIcon getDisplayIcon() {
        return DisplayIcon.item(Items.NAME_TAG.getDefaultInstance());
    }

    private boolean matchesEntityType(Entity entity) {
        String requiredType = normalize(entityType);
        if (requiredType.isEmpty()) {
            return true;
        }
        ResourceLocation requiredId = ResourceLocation.tryParse(requiredType);
        return requiredId != null && requiredId.equals(BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()));
    }

    private boolean matchesEntityTag(Entity entity) {
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
