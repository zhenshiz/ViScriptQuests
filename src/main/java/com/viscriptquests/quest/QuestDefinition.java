package com.viscriptquests.quest;

import com.lowdragmc.lowdraglib2.configurator.IConfigurable;
import com.lowdragmc.lowdraglib2.configurator.annotation.Configurable;
import com.lowdragmc.lowdraglib2.syncdata.IPersistedSerializable;
import com.lowdragmc.lowdraglib2.syncdata.annotation.Persisted;
import com.viscriptquests.gui.data.IconTexture;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class QuestDefinition implements IConfigurable, IPersistedSerializable {
    @Configurable(name = "viscript_quests.questDefinition.questId")
    private String questId = UUID.randomUUID().toString();
    @Configurable(name = "viscript_quests.questDefinition.categoryId")
    private String categoryId;
    @Configurable(name = "viscript_quests.questDefinition.name")
    private String name;
    @Configurable(name = "viscript_quests.questDefinition.description")
    private String description;
    @Configurable(name = "viscript_quests.questInfo.icon", subConfigurable = true)
    private IconTexture icon = new IconTexture();
    @Persisted
    private String startStepId;
    @Persisted
    private Map<String, QuestVariable> variables = new HashMap<>();
    @Persisted
    private Map<String, QuestStep> steps = new HashMap<>();
}
