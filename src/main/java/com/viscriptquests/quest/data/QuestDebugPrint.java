package com.viscriptquests.quest.data;

import com.lowdragmc.lowdraglib2.syncdata.IPersistedSerializable;
import com.lowdragmc.lowdraglib2.syncdata.annotation.Persisted;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

// DebugPrint 节点编译出的运行时输出；默认写日志，只有显式勾选时才发到玩家聊天栏。
public class QuestDebugPrint implements IPersistedSerializable {
    @Persisted
    public String message = "";
    @Persisted
    public boolean sendToChat = false;
    @Persisted
    public final List<DebugValuePrint> valuePrints = new ArrayList<>();

    public QuestDebugPrint copy() {
        QuestDebugPrint copy = new QuestDebugPrint();
        copy.message = message;
        copy.sendToChat = sendToChat;
        copy.valuePrints.addAll(valuePrints);
        return copy;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) return true;
        if (!(object instanceof QuestDebugPrint that)) return false;
        return sendToChat == that.sendToChat
                && Objects.equals(message, that.message)
                && Objects.equals(valuePrints, that.valuePrints);
    }

    @Override
    public int hashCode() {
        return Objects.hash(message, sendToChat, valuePrints);
    }
}
