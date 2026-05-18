package com.viscriptquests.gui.blueprint.data;

import com.lowdragmc.lowdraglib2.syncdata.IPersistedSerializable;
import com.lowdragmc.lowdraglib2.syncdata.annotation.Persisted;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;

/**
 * 蓝图表单里带补全语义的资源 ID。
 * <p>
 * 运行时仍然只读取 {@link #id} 字符串，这个包装类只用于让编辑器知道应该展示哪种搜索框。
 */
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode
@Accessors(chain = true)
public class QuestRegistryId implements IPersistedSerializable {
    @Persisted
    private String id = "";

    public QuestRegistryId(String id) {
        this.id = normalize(id);
    }

    public String value() {
        return normalize(id);
    }

    @Override
    public String toString() {
        return value();
    }

    public static String normalize(String id) {
        return id == null ? "" : id.trim();
    }
}
