package com.viscriptquests.quest.data.task;

import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegister;
import com.lowdragmc.lowdraglib2.syncdata.annotation.Persisted;
import com.viscriptquests.quest.data.DisplayIcon;
import com.viscriptquests.quest.data.runtime.QuestGuideMarker;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

// 到达指定位置的目标，同时为客户端提供任务导航标记。
@LDLRegister(name = "location_task", registry = ITask.ID)
public class LocationTask extends ITask {
    @Persisted
    public String dimension = "minecraft:overworld";
    @Persisted
    public double x = 0.0;
    @Persisted
    public double y = 64.0;
    @Persisted
    public double z = 0.0;
    @Persisted
    public double arrivalRadius = 3.0;
    @Persisted
    public String taskHint = "";
    @Persisted
    public String markerLabel = "";
    @Persisted
    public DisplayIcon markerIcon = DisplayIcon.item(Items.COMPASS.getDefaultInstance());
    @Persisted
    public int markerColor = 0xFFD8C7FF;

    @Override
    public boolean checkCompletion(ServerPlayer player) {
        if (player == null || !sameDimension(player)) {
            return false;
        }
        return player.position().distanceToSqr(targetPosition()) <= arrivalRadius * arrivalRadius;
    }

    @Override
    public boolean onComplete(ServerPlayer player) {
        return true;
    }

    @Override
    public Component getTaskHint() {
        if (taskHint != null && !taskHint.isBlank()) {
            return Component.translatableWithFallback(taskHint, taskHint);
        }
        return Component.translatable("viscript_quests.task_hint.location_task", x, y, z);
    }

    @Override
    public DisplayIcon getDisplayIcon() {
        return markerIcon == null ? DisplayIcon.item(new ItemStack(Items.COMPASS)) : markerIcon.copy();
    }

    @Override
    public QuestGuideMarker getGuideMarker(ServerPlayer player) {
        return QuestGuideMarker.position(
                targetDimension(),
                targetPosition(),
                markerLabel,
                getDisplayIcon(),
                markerColor,
                arrivalRadius
        );
    }

    private boolean sameDimension(ServerPlayer player) {
        return player.level().dimension().location().equals(targetDimension());
    }

    private ResourceLocation targetDimension() {
        ResourceLocation dimensionId = ResourceLocation.tryParse(dimension);
        return dimensionId == null ? Level.OVERWORLD.location() : dimensionId;
    }

    private Vec3 targetPosition() {
        return new Vec3(x + 0.5, y + 0.5, z + 0.5);
    }
}
