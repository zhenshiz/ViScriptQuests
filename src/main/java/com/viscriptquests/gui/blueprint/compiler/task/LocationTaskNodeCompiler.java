package com.viscriptquests.gui.blueprint.compiler.task;

import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.CustomNodeModelImpl;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegister;
import com.viscriptquests.gui.blueprint.compiler.IQuestTaskNodeCompiler;
import com.viscriptquests.gui.blueprint.compiler.QuestCompileContext;
import com.viscriptquests.gui.blueprint.node.task.LocationTaskNode;
import com.viscriptquests.quest.data.task.ITask;
import com.viscriptquests.quest.data.task.LocationTask;

@LDLRegister(name = "location_task", registry = IQuestTaskNodeCompiler.ID)
public class LocationTaskNodeCompiler implements IQuestTaskNodeCompiler {
    @Override
    public boolean supports(CustomNodeModelImpl node) {
        return node.getNode() instanceof LocationTaskNode;
    }

    @Override
    public ITask compileTask(QuestCompileContext context, CustomNodeModelImpl node, String stepId) {
        LocationTask task = new LocationTask();
        task.stepId = stepId;
        task.dimension = context.getString(node, "dimension");
        task.x = context.getFloat(node, "x");
        task.y = context.getFloat(node, "y");
        task.z = context.getFloat(node, "z");
        task.arrivalRadius = Math.max(0.0f, context.getFloat(node, "arrival_radius"));
        task.markerLabel = context.getString(node, "marker_label");
        task.markerIcon = context.getDisplayIcon(node, "marker_icon");
        task.markerColor = context.getInt(node, "marker_color");
        return task;
    }
}
