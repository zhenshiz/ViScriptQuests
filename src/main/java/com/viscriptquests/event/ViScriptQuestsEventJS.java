package com.viscriptquests.event;

import com.viscriptquests.event.kubejs.QuestEventJS;
import dev.latvian.mods.kubejs.event.EventGroup;
import dev.latvian.mods.kubejs.event.EventTargetType;
import dev.latvian.mods.kubejs.event.TargetedEventHandler;

public interface ViScriptQuestsEventJS {
    EventGroup QUEST_EVENTS = EventGroup.of("ViScriptQuestsEvents");
    EventTargetType<String> QUEST_TARGET = EventTargetType.STRING;

    TargetedEventHandler<String> QUEST_STARTED = QUEST_EVENTS
            .server("questStarted", () -> QuestEventJS.QuestStarted.class)
            .supportsTarget(QUEST_TARGET);
    TargetedEventHandler<String> QUEST_COMPLETED = QUEST_EVENTS
            .server("questCompleted", () -> QuestEventJS.QuestCompleted.class)
            .supportsTarget(QUEST_TARGET);
    TargetedEventHandler<String> QUEST_FAILED = QUEST_EVENTS
            .server("questFailed", () -> QuestEventJS.QuestFailed.class)
            .supportsTarget(QUEST_TARGET);
    TargetedEventHandler<String> QUEST_REVOKED = QUEST_EVENTS
            .server("questRevoked", () -> QuestEventJS.QuestRevoked.class)
            .supportsTarget(QUEST_TARGET);
    TargetedEventHandler<String> TASK_STARTED = QUEST_EVENTS
            .server("taskStarted", () -> QuestEventJS.TaskStarted.class)
            .supportsTarget(QUEST_TARGET);
    TargetedEventHandler<String> TASK_COMPLETED = QUEST_EVENTS
            .server("taskCompleted", () -> QuestEventJS.TaskCompleted.class)
            .supportsTarget(QUEST_TARGET);
    TargetedEventHandler<String> TASK_FAILED = QUEST_EVENTS
            .server("taskFailed", () -> QuestEventJS.TaskFailed.class)
            .supportsTarget(QUEST_TARGET);
    TargetedEventHandler<String> TASK_SKIPPED = QUEST_EVENTS
            .server("taskSkipped", () -> QuestEventJS.TaskSkipped.class)
            .supportsTarget(QUEST_TARGET);
    TargetedEventHandler<String> OBJECTIVE_PROGRESS = QUEST_EVENTS
            .server("objectiveProgress", () -> QuestEventJS.ObjectiveProgress.class)
            .supportsTarget(QUEST_TARGET);
    TargetedEventHandler<String> OBJECTIVE_COMPLETED = QUEST_EVENTS
            .server("objectiveCompleted", () -> QuestEventJS.ObjectiveCompleted.class)
            .supportsTarget(QUEST_TARGET);
    TargetedEventHandler<String> REWARD_GRANTED = QUEST_EVENTS
            .server("rewardGranted", () -> QuestEventJS.RewardGranted.class)
            .supportsTarget(QUEST_TARGET);
}
