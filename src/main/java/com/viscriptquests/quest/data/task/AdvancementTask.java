package com.viscriptquests.quest.data.task;

import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegister;
import com.lowdragmc.lowdraglib2.syncdata.annotation.Persisted;
import com.viscriptquests.quest.data.DisplayIcon;
import com.viscriptquests.quest.data.QuestVariableValue;
import com.viscriptquests.quest.data.runtime.TaskObjectiveProgress;
import net.minecraft.advancements.Advancement;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.Map;
import java.util.Optional;

// 进度目标，检查玩家是否已经完成指定 Minecraft Advancement。
@LDLRegister(name = "advancement_task", registry = ITask.ID)
public class AdvancementTask extends ITask {
    @Persisted
    public String advancementId = "minecraft:story/root";

    public boolean matches(Advancement advancement) {
        ResourceLocation id = targetAdvancementId();
        return id != null && advancement != null && id.equals(advancement.getId());
    }

    @Override
    public boolean checkCompletion(ServerPlayer player) {
        Advancement advancement = findAdvancement(player);
        return advancement != null && player.getAdvancements().getOrStartProgress(advancement).isDone();
    }

    @Override
    public boolean onComplete(ServerPlayer player) {
        return true;
    }

    @Override
    public void refreshObjectiveProgress(ServerPlayer player, TaskObjectiveProgress progress) {
        refreshObjectiveProgress(player, progress, null);
    }

    @Override
    public void refreshObjectiveProgress(ServerPlayer player, TaskObjectiveProgress progress,
                                         Map<String, QuestVariableValue> questVariables) {
        Advancement advancement = findAdvancement(player);
        if (advancement != null) {
            if (taskHint == null || taskHint.isBlank()) {
                progress.hint = Component.translatable("viscript_quests.task_hint.advancement_task",
                        advancement.getChatComponent());
            }
            ItemStack icon = Optional.ofNullable(advancement.getDisplay().getIcon())
                .orElse(Items.KNOWLEDGE_BOOK.getDefaultInstance());
            progress.displayIcon = DisplayIcon.item(icon);
        }
        super.refreshObjectiveProgress(player, progress, questVariables);
    }

    @Override
    protected Component getDefaultTaskHint() {
        return Component.translatable("viscript_quests.task_hint.advancement_task", advancementDisplayNameKey());
    }

    @Override
    public DisplayIcon getDisplayIcon() {
        return DisplayIcon.item(Items.KNOWLEDGE_BOOK.getDefaultInstance());
    }

    private Advancement findAdvancement(ServerPlayer player) {
        ResourceLocation id = targetAdvancementId();
        if (player == null || player.getServer() == null || id == null) {
            return null;
        }
        return player.getServer().getAdvancements().getAdvancement(id);
    }

    private Component advancementDisplayNameKey() {
        ResourceLocation id = targetAdvancementId();
        if (id == null) {
            return Component.literal(normalize(advancementId));
        }
        // 原版进度标题通常有固定翻译键；数据包/模组进度没有匹配翻译时回退到资源 ID。
        String key = "advancements." + id.getNamespace() + "." + id.getPath().replace('/', '.') + ".title";
        return Component.translatableWithFallback(key, id.toString());
    }

    private ResourceLocation targetAdvancementId() {
        return ResourceLocation.tryParse(normalize(advancementId));
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}
