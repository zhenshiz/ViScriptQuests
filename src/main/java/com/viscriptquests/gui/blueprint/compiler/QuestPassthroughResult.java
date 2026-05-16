package com.viscriptquests.gui.blueprint.compiler;

import com.viscriptquests.quest.data.QuestDebugPrint;
import com.viscriptquests.quest.data.VariableMutation;

import java.util.ArrayList;
import java.util.List;

// 透传节点沿流程线累积的运行时效果。
public class QuestPassthroughResult {
    public final List<VariableMutation> mutations = new ArrayList<>();
    public final List<QuestDebugPrint> debugPrints = new ArrayList<>();

    public QuestDebugPrint addDebugPrint(String message, boolean sendToChat) {
        if (message == null || message.isEmpty()) {
            return null;
        }
        QuestDebugPrint debugPrint = new QuestDebugPrint();
        debugPrint.message = message;
        debugPrint.sendToChat = sendToChat;
        debugPrints.add(debugPrint);
        return debugPrint;
    }
}
