package com.viscriptquests.quest.task;

import com.lowdragmc.lowdraglib2.configurator.IConfigurable;
import com.lowdragmc.lowdraglib2.configurator.ui.Configurator;
import com.lowdragmc.lowdraglib2.configurator.ui.ConfiguratorGroup;
import com.lowdragmc.lowdraglib2.configurator.ui.SearchComponentConfigurator;
import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import com.lowdragmc.lowdraglib2.registry.ILDLRegister;
import com.lowdragmc.lowdraglib2.syncdata.IPersistedSerializable;
import com.lowdragmc.lowdraglib2.utils.LDLibExtraCodecs;
import com.lowdragmc.lowdraglib2.utils.PersistedParser;
import com.mojang.serialization.Codec;
import com.viscriptquests.ViScriptQuestsRegistries;
import com.viscriptquests.util.UIElementUtil;
import io.netty.buffer.ByteBuf;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import org.appliedenergistics.yoga.YogaDisplay;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public abstract class IQuestTask implements ILDLRegister<IQuestTask, Supplier<IQuestTask>>, IPersistedSerializable, IConfigurable {
    Codec<IQuestTask> CODEC = ViScriptQuestsRegistries.QUEST_TASK.optionalCodec().dispatch(ILDLRegister::getRegistryHolderOptional,
            optional -> optional.map(holder -> PersistedParser.createCodec(holder.value()).fieldOf("data"))
                    .orElseGet(LDLibExtraCodecs::errorDecoder));
    StreamCodec<ByteBuf, IQuestTask> STREAM_CODEC = ByteBufCodecs.fromCodec(CODEC);

    //runtime
    @Getter
    @Setter
    private String uiType = "";

    public Configurator createConfigurator() {
        ConfiguratorGroup group = new ConfiguratorGroup();
        group.setCanCollapse(false);
        group.setCollapse(false);
        group.lineContainer.setDisplay(YogaDisplay.NONE);

        Map<String, IGuiTexture> arr = new HashMap<>();
        ViScriptQuestsRegistries.QUEST_TASK.forEach(questTask -> {
            IQuestTask iQuestTask = questTask.value().get();
            arr.put(iQuestTask.getType(), iQuestTask.getIcon());
        });
        ConfiguratorGroup configuratorGroup = new ConfiguratorGroup();
        configuratorGroup.setCollapse(false);
        configuratorGroup.setCanCollapse(false);
        configuratorGroup.setCollapse(false);
        configuratorGroup.lineContainer.setDisplay(YogaDisplay.NONE);
        SearchComponentConfigurator<String> typeConfigurator = UIElementUtil.createTaskTypeSearchComponentConfigurator("viscript_quests.task.type", arr, this::getUiType, this::setUiType);
        typeConfigurator.searchComponent.addEventListener(UIEvents.LAYOUT_CHANGED, event -> {
            configuratorGroup.removeAllConfigurators();
            ViScriptQuestsRegistries.QUEST_TASK.forEach(questTask -> {
                IQuestTask iQuestTask = questTask.value().get();
                if (iQuestTask.getType().equals(uiType)) {
                    configuratorGroup.addConfigurators(iQuestTask.createDirectConfigurator());
                }
            });
        });
        group.addConfigurators(typeConfigurator, configuratorGroup);
        return group;
    }

    // 获取任务类型
    abstract public String getType();

    //Editor上显示的图标
    abstract public IGuiTexture getIcon();

    //Editor上显示的名称
    abstract public Component getName();

    //UI上显示的文本
    abstract public Component getProgressText();

    @Nullable
    public CompoundTag serializeWrapper() {
        return (CompoundTag) CODEC.encodeStart(NbtOps.INSTANCE, this).result().orElse(null);
    }

    @Nullable
    public IQuestTask deserializeWrapper(Tag tag) {
        return CODEC.parse(NbtOps.INSTANCE, tag).result().orElse(null);
    }
}
