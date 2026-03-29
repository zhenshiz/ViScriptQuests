package com.viscriptquests.quest;

import com.lowdragmc.lowdraglib2.configurator.IConfigurable;
import com.lowdragmc.lowdraglib2.configurator.annotation.Configurable;
import com.lowdragmc.lowdraglib2.syncdata.IPersistedSerializable;
import com.viscriptquests.gui.data.IconTexture;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class QuestCategory implements IConfigurable, IPersistedSerializable {
    @Configurable
    private String categoryId = UUID.randomUUID().toString();
    @Configurable
    private String name;
    @Configurable
    private IconTexture icon;
}
