package com.viscriptquests.event;

import com.viscriptquests.event.kubejs.QuestEventJS;
import dev.latvian.mods.kubejs.event.EventGroup;
import dev.latvian.mods.kubejs.event.EventHandler;

public interface ViScriptQuestsEventJS {
    EventGroup QUEST_EVENTS = EventGroup.of("ViScriptQuestsEvents");

    EventHandler QUEST_STARTED = QUEST_EVENTS
            .server("questStarted", () -> QuestEventJS.QuestStarted.class);
    EventHandler QUEST_COMPLETED = QUEST_EVENTS
            .server("questCompleted", () -> QuestEventJS.QuestCompleted.class);
    EventHandler QUEST_FAILED = QUEST_EVENTS
            .server("questFailed", () -> QuestEventJS.QuestFailed.class);
    EventHandler QUEST_REVOKED = QUEST_EVENTS
            .server("questRevoked", () -> QuestEventJS.QuestRevoked.class);
    EventHandler TASK_STARTED = QUEST_EVENTS
            .server("taskStarted", () -> QuestEventJS.TaskStarted.class);
    EventHandler TASK_COMPLETED = QUEST_EVENTS
            .server("taskCompleted", () -> QuestEventJS.TaskCompleted.class);
    EventHandler TASK_FAILED = QUEST_EVENTS
            .server("taskFailed", () -> QuestEventJS.TaskFailed.class);
    EventHandler TASK_SKIPPED = QUEST_EVENTS
            .server("taskSkipped", () -> QuestEventJS.TaskSkipped.class);
    EventHandler OBJECTIVE_PROGRESS = QUEST_EVENTS
            .server("objectiveProgress", () -> QuestEventJS.ObjectiveProgress.class);
    EventHandler OBJECTIVE_COMPLETED = QUEST_EVENTS
            .server("objectiveCompleted", () -> QuestEventJS.ObjectiveCompleted.class);
    EventHandler REWARD_GRANTED = QUEST_EVENTS
            .server("rewardGranted", () -> QuestEventJS.RewardGranted.class);
}
