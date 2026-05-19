package com.viscriptquests.quest.data.task;

import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegister;
import com.lowdragmc.lowdraglib2.syncdata.annotation.Persisted;
import com.viscriptquests.quest.data.DisplayIcon;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Items;

// 自定义触发目标不会从玩家状态自动完成，只能由指令或服务端 API 触发。
@LDLRegister(name = "custom_trigger_task", registry = ITask.ID)
public class CustomTriggerTask extends ITask {
    @Persisted
    public String triggerId = "viscript_quests:custom_trigger";

    public boolean matches(String id) {
        String requiredId = normalize(triggerId);
        return !requiredId.isEmpty() && requiredId.equals(normalize(id));
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
        return Component.translatable("viscript_quests.task_hint.custom_trigger_task", displayTriggerId());
    }

    @Override
    public DisplayIcon getDisplayIcon() {
        return DisplayIcon.item(Items.COMMAND_BLOCK.getDefaultInstance());
    }

    private String displayTriggerId() {
        String id = normalize(triggerId);
        return id.isEmpty() ? "-" : id;
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}
