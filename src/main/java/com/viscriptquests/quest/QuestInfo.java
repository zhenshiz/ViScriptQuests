package com.viscriptquests.quest;

import com.lowdragmc.lowdraglib2.configurator.IConfigurable;
import com.lowdragmc.lowdraglib2.configurator.annotation.Configurable;
import com.lowdragmc.lowdraglib2.configurator.ui.ArrayConfiguratorGroup;
import com.lowdragmc.lowdraglib2.configurator.ui.Configurator;
import com.lowdragmc.lowdraglib2.configurator.ui.ConfiguratorGroup;
import com.lowdragmc.lowdraglib2.syncdata.IPersistedSerializable;
import com.lowdragmc.lowdraglib2.syncdata.annotation.Persisted;
import com.lowdragmc.lowdraglib2.syncdata.annotation.ReadOnlyManaged;
import com.viscriptquests.gui.data.IconTexture;
import com.viscriptquests.quest.reward.IQuestReward;
import com.viscriptquests.quest.reward.ItemReward;
import com.viscriptquests.quest.task.IQuestTask;
import com.viscriptquests.quest.task.ItemTask;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.Tag;

import java.util.ArrayList;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class QuestInfo implements IConfigurable, IPersistedSerializable {
    @Configurable(name = "viscript_quests.questInfo.title")
    private String title;
    @Configurable(name = "viscript_quests.questInfo.subTitle")
    private String subTitle;
    @Configurable(name = "viscript_quests.questInfo.description")
    private String description;
    @Configurable(name = "viscript_quests.questInfo.icon", subConfigurable = true)
    private IconTexture icon = new IconTexture();
    @Persisted
    @ReadOnlyManaged(serializeMethod = "writeIQuestTasks", deserializeMethod = "readIQuestTasks")
    private List<IQuestTask> questTasks = new ArrayList<>();
    @Persisted
    @ReadOnlyManaged(serializeMethod = "writeIQuestReward", deserializeMethod = "readIQuestReward")
    private List<IQuestReward> questRewards = new ArrayList<>();

    @Override
    public void buildConfigurator(ConfiguratorGroup father) {
        IConfigurable.super.buildConfigurator(father);
        ArrayConfiguratorGroup<IQuestTask> iquestTaskArrayConfiguratorGroup = new ArrayConfiguratorGroup<>("viscript_quests.questInfo.questTasks", true,
                () -> new ArrayList<>(this.getQuestTasks()),
                (getter, setter) -> {
                    IQuestTask instance = getter.get();
                    return instance != null ? instance.createConfigurator() : new Configurator();
                }, true);
        iquestTaskArrayConfiguratorGroup.setAddDefault(ItemTask::new);
        iquestTaskArrayConfiguratorGroup.setOnUpdate(list -> {
            List<IQuestTask> origin = this.getQuestTasks();
            origin.clear();
            origin.addAll(list);
        });
        ArrayConfiguratorGroup<IQuestReward> iQuestRewardArrayConfiguratorGroup = new ArrayConfiguratorGroup<>("viscript_quests.questInfo.questRewards", true,
                () -> new ArrayList<>(this.getQuestRewards()),
                (getter, setter) -> {
                    IQuestReward instance = getter.get();
                    return instance != null ? instance.createConfigurator() : new Configurator();
                }, true);
        iQuestRewardArrayConfiguratorGroup.setAddDefault(ItemReward::new);
        iQuestRewardArrayConfiguratorGroup.setOnUpdate(list -> {
            List<IQuestReward> origin = this.getQuestRewards();
            origin.clear();
            origin.addAll(list);
        });
        father.addConfigurators(iquestTaskArrayConfiguratorGroup, iQuestRewardArrayConfiguratorGroup);
    }

    private Tag writeIQuestTasks(List<IQuestTask> value) {
        return IntTag.valueOf(value.size());
    }

    private List<IQuestTask> readIQuestTasks(IntTag tag) {
        List<IQuestTask> list = new ArrayList<>();
        for (int i = 0; i < tag.getAsInt(); i++) {
            list.add(new ItemTask());
        }
        return list;
    }

    private Tag writeIQuestReward(List<IQuestReward> value) {
        return IntTag.valueOf(value.size());
    }

    private List<IQuestReward> readIQuestReward(IntTag tag) {
        List<IQuestReward> list = new ArrayList<>();
        for (int i = 0; i < tag.getAsInt(); i++) {
            list.add(new ItemReward());
        }
        return list;
    }
}
