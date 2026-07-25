package com.viscriptquests.event;

import com.viscriptquests.event.kubejs.QuestEventJS;
import com.viscriptquests.event.neoforge.QuestEvent;
import dev.latvian.mods.kubejs.event.KubeEvent;
import dev.latvian.mods.kubejs.event.TargetedEventHandler;
import dev.latvian.mods.kubejs.script.ScriptType;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;

public final class CommonEventsPostJS {
    private CommonEventsPostJS() {
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onQuestStarted(QuestEvent.QuestStarted event) {
        post(ViScriptQuestsEventJS.QUEST_STARTED, event, new QuestEventJS.QuestStarted(event));
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onQuestCompleted(QuestEvent.QuestCompleted event) {
        post(ViScriptQuestsEventJS.QUEST_COMPLETED, event, new QuestEventJS.QuestCompleted(event));
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onQuestFailed(QuestEvent.QuestFailed event) {
        post(ViScriptQuestsEventJS.QUEST_FAILED, event, new QuestEventJS.QuestFailed(event));
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onQuestRevoked(QuestEvent.QuestRevoked event) {
        post(ViScriptQuestsEventJS.QUEST_REVOKED, event, new QuestEventJS.QuestRevoked(event));
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onTaskStarted(QuestEvent.TaskStarted event) {
        post(ViScriptQuestsEventJS.TASK_STARTED, event, new QuestEventJS.TaskStarted(event));
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onTaskCompleted(QuestEvent.TaskCompleted event) {
        post(ViScriptQuestsEventJS.TASK_COMPLETED, event, new QuestEventJS.TaskCompleted(event));
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onTaskFailed(QuestEvent.TaskFailed event) {
        post(ViScriptQuestsEventJS.TASK_FAILED, event, new QuestEventJS.TaskFailed(event));
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onTaskSkipped(QuestEvent.TaskSkipped event) {
        post(ViScriptQuestsEventJS.TASK_SKIPPED, event, new QuestEventJS.TaskSkipped(event));
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onObjectiveProgress(QuestEvent.ObjectiveProgress event) {
        post(ViScriptQuestsEventJS.OBJECTIVE_PROGRESS, event, new QuestEventJS.ObjectiveProgress(event));
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onObjectiveCompleted(QuestEvent.ObjectiveCompleted event) {
        post(ViScriptQuestsEventJS.OBJECTIVE_COMPLETED, event, new QuestEventJS.ObjectiveCompleted(event));
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onRewardGranted(QuestEvent.RewardGranted event) {
        post(ViScriptQuestsEventJS.REWARD_GRANTED, event, new QuestEventJS.RewardGranted(event));
    }

    private static void post(TargetedEventHandler<String> handler, QuestEvent event, KubeEvent kubeEvent) {
        String target = event.getTarget();
        String targetOrNull = target == null || target.isBlank() ? null : target;
        if (handler.hasListeners(targetOrNull)) {
            handler.post(ScriptType.SERVER, targetOrNull, kubeEvent);
        }
    }
}
