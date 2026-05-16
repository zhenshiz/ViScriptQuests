package com.viscriptquests.gui.blueprint;

import com.lowdragmc.lowdraglib2.nodegraphtookit.api.type.TypeHandle;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.type.TypeHandles;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.graph.CustomGraphModelImpl;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.PortModel;

// 自定义图模型，支持数值类型之间的隐式连线转换（INT ↔ FLOAT）
public class QuestBlueprintGraphModel extends CustomGraphModelImpl {

    public QuestBlueprintGraphModel(QuestBlueprintGraph graph) {
        super(graph);
    }

    @Override
    public boolean canAssignTo(PortModel fromPort, PortModel toPort) {
        TypeHandle fromType = fromPort.getDataTypeHandle();
        TypeHandle toType = toPort.getDataTypeHandle();
        // 允许数值类型之间的隐式连接（INT ↔ FLOAT）
        if (isNumericType(fromType) && isNumericType(toType)) {
            return true;
        }
        // 允许任意类型连入 Object 端口，使 DebugPrintVariableNode 等通用节点能接收所有变量类型
        if (toType.equals(QuestBlueprintTypes.OBJECT)) {
            return true;
        }
        return super.canAssignTo(fromPort, toPort);
    }

    private static boolean isNumericType(TypeHandle type) {
        return type.equals(TypeHandles.INT) || type.equals(TypeHandles.FLOAT);
    }
}
