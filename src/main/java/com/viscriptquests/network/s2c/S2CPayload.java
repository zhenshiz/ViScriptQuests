package com.viscriptquests.network.s2c;

import com.lowdragmc.lowdraglib2.networking.rpc.RPCPacket;
import com.lowdragmc.lowdraglib2.syncdata.rpc.RPCSender;
import com.viscriptquests.util.ViScriptQuestsClientUtil;

public class S2CPayload {
    public static final String OPEN_QUEST_BOOK = "open_quest_book";
    public static final String OPEN_QUEST_EDITOR = "open_quest_editor";

    @RPCPacket(OPEN_QUEST_BOOK)
    public static void openQuestBook(RPCSender sender) {
        ViScriptQuestsClientUtil.openQuestBook();
    }

    @RPCPacket(OPEN_QUEST_EDITOR)
    public static void openQuestEditor(RPCSender sender) {
        ViScriptQuestsClientUtil.openQuestEditor();
    }
}
