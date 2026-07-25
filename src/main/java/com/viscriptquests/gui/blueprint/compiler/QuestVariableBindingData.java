package com.viscriptquests.gui.blueprint.compiler;

import com.lowdragmc.lowdraglib2.nodegraphtookit.model.GraphElementModel;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.graph.GraphModel;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.PortModel;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.variable.VariableDeclarationModelBase;
import com.viscriptquests.quest.data.QuestVariableValue;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

// 本地子图复制后，内部变量/端口 UUID 可能相同；绑定表需要同时记录所属图，避免小任务互相串变量。
record QuestVariableBindingData(
        Map<QuestGraphElementKey, String> portToVarName,
        Map<QuestGraphElementKey, String> variableAliases,
        Map<QuestGraphElementKey, QuestVariableValue> variableDefaultOverrides,
        Set<QuestGraphElementKey> inheritedVariables
) {
    static QuestVariableBindingData empty() {
        return new QuestVariableBindingData(Map.of(), Map.of(), Map.of(), Set.of());
    }

    static QuestVariableBindingData fromLegacyPortMap(Map<UUID, String> portUuidToVarName) {
        if (portUuidToVarName == null || portUuidToVarName.isEmpty()) {
            return empty();
        }
        Map<QuestGraphElementKey, String> portMap = new LinkedHashMap<>();
        for (var entry : portUuidToVarName.entrySet()) {
            portMap.put(QuestGraphElementKey.legacy(entry.getKey()), entry.getValue());
        }
        return new QuestVariableBindingData(portMap, Map.of(), Map.of(), Set.of());
    }

    String variableNameForPort(PortModel port) {
        if (port == null) {
            return null;
        }
        String scoped = portToVarName.get(QuestGraphElementKey.of(port));
        return scoped == null ? portToVarName.get(QuestGraphElementKey.legacy(port.getUid())) : scoped;
    }

    String aliasFor(VariableDeclarationModelBase declaration) {
        if (declaration == null) {
            return null;
        }
        return variableAliases.get(QuestGraphElementKey.of(declaration));
    }

    QuestVariableValue defaultOverrideFor(VariableDeclarationModelBase declaration) {
        if (declaration == null) {
            return null;
        }
        return variableDefaultOverrides.get(QuestGraphElementKey.of(declaration));
    }

    boolean isInherited(VariableDeclarationModelBase declaration) {
        return declaration != null && inheritedVariables.contains(QuestGraphElementKey.of(declaration));
    }
}

record QuestGraphElementKey(UUID graphUid, UUID elementUid) {
    static QuestGraphElementKey of(GraphElementModel model) {
        if (model == null) {
            return new QuestGraphElementKey(null, null);
        }
        GraphModel graphModel = model.getGraphModel();
        return new QuestGraphElementKey(graphModel == null ? null : graphModel.getUid(), model.getUid());
    }

    static QuestGraphElementKey legacy(UUID elementUid) {
        return new QuestGraphElementKey(null, elementUid);
    }
}
