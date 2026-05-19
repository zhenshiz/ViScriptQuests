package com.viscriptquests.quest.data.task;

import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegister;
import com.lowdragmc.lowdraglib2.syncdata.annotation.Persisted;
import com.viscriptquests.quest.data.DisplayIcon;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

// 访问维度目标，玩家当前所在维度与目标维度一致时即可自动完成。
@LDLRegister(name = "visit_dimension_task", registry = ITask.ID)
public class VisitDimensionTask extends ITask {
    @Persisted
    public String dimension = Level.OVERWORLD.location().toString();

    @Override
    public boolean checkCompletion(ServerPlayer player) {
        return player != null && player.level().dimension().location().equals(targetDimension());
    }

    @Override
    public boolean onComplete(ServerPlayer player) {
        return true;
    }

    @Override
    protected Component getDefaultTaskHint() {
        return Component.translatable("viscript_quests.task_hint.visit_dimension_task", dimensionDisplayName());
    }

    @Override
    public DisplayIcon getDisplayIcon() {
        return DisplayIcon.item(Items.COMPASS.getDefaultInstance());
    }

    private ResourceLocation targetDimension() {
        ResourceLocation id = ResourceLocation.tryParse(dimension == null ? "" : dimension.trim());
        return id == null ? Level.OVERWORLD.location() : id;
    }

    private Component dimensionDisplayName() {
        ResourceLocation id = targetDimension();
        String key = "dimension." + id.getNamespace() + "." + id.getPath();
        return Component.translatableWithFallback(key, id.toString());
    }
}
