package com.viscriptquests.quest;

import com.lowdragmc.lowdraglib2.configurator.annotation.Configurable;
import com.lowdragmc.lowdraglib2.syncdata.IPersistedSerializable;
import com.lowdragmc.lowdraglib2.syncdata.annotation.Persisted;
import com.viscriptquests.quest.condition.QuestCondition;
import com.viscriptquests.quest.reward.IQuestReward;
import com.viscriptquests.quest.task.IQuestTask;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class QuestStep implements IPersistedSerializable {
    @Configurable(name = "viscript_quests.questStep.stepId")
    private String stepId = UUID.randomUUID().toString();
    @Configurable(name = "viscript_quests.questStep.title")
    private String title;
    @Configurable(name = "viscript_quests.questStep.title")
    private String subTitle;
    @Configurable(name = "viscript_quests.questStep.title")
    private String description;
    @Persisted
    private Map<String, IQuestTask> questTasks = new HashMap<>();
    @Persisted
    private QuestCondition completionCondition;
    @Persisted
    private List<IQuestReward> questRewards;
    @Persisted
    private QuestTransition transition;
}
