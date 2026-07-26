package com.viscriptquests.gui.blueprint.compiler;

import com.lowdragmc.lowdraglib2.nodegraphtookit.api.type.TypeHandles;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.constant.Constant;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.graph.GraphModel;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.*;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.variable.VariableDeclarationModelBase;
import com.viscriptquests.ViScriptQuestsRegistries;
import com.viscriptquests.gui.blueprint.data.QuestRegistryId;
import com.viscriptquests.quest.data.DisplayIcon;
import com.viscriptquests.quest.data.ItemMatchRule;
import com.viscriptquests.quest.data.QuestJoinMode;
import com.viscriptquests.quest.data.QuestSubmitMode;
import com.viscriptquests.quest.data.TaskObjectiveType;
import com.viscriptquests.quest.data.QuestValueToken;
import com.viscriptquests.quest.data.QuestVariableValue;
import com.viscriptquests.quest.data.reward.LootTableReward;
import lombok.Getter;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

import java.util.*;

// 蓝图编译期间共享的读图工具，handler 通过它读取选项、端口、变量和表达式。
public class QuestCompileContext {
    @Getter
    private final GraphModel graphModel;
    private final QuestVariableBindingData variableBindings;
    private final Map<String, QuestVariableValue> variableInitialValues = new LinkedHashMap<>();

    public QuestCompileContext(GraphModel graphModel, QuestVariableBindingData variableBindings) {
        this.graphModel = graphModel;
        this.variableBindings = variableBindings == null ? QuestVariableBindingData.empty() : variableBindings;
        indexVariableInitialValues();
    }

    public IQuestTaskNodeCompiler findTaskCompiler(CustomNodeModelImpl node) {
        for (var holder : ViScriptQuestsRegistries.BLUEPRINT_TASK_NODE_COMPILERS) {
            IQuestTaskNodeCompiler compiler = holder.value().get();
            if (compiler.supports(node)) {
                return compiler;
            }
        }
        return null;
    }

    public IQuestRewardNodeCompiler findRewardCompiler(CustomNodeModelImpl node) {
        for (var holder : ViScriptQuestsRegistries.BLUEPRINT_REWARD_NODE_COMPILERS) {
            IQuestRewardNodeCompiler compiler = holder.value().get();
            if (compiler.supports(node)) {
                return compiler;
            }
        }
        return null;
    }

    public IQuestExpressionNodeCompiler findExpressionCompiler(CustomNodeModelImpl node) {
        for (var holder : ViScriptQuestsRegistries.BLUEPRINT_EXPRESSION_NODE_COMPILERS) {
            IQuestExpressionNodeCompiler compiler = holder.value().get();
            if (compiler.supports(node)) {
                return compiler;
            }
        }
        return null;
    }

    public IQuestPassthroughNodeCompiler findPassthroughCompiler(CustomNodeModelImpl node) {
        for (var holder : ViScriptQuestsRegistries.BLUEPRINT_PASSTHROUGH_NODE_COMPILERS) {
            IQuestPassthroughNodeCompiler compiler = holder.value().get();
            if (compiler.supports(node)) {
                return compiler;
            }
        }
        return null;
    }

    public String getString(NodeModel nodeModel, String optionId) {
        Constant constant = getOptionConstant(nodeModel, optionId);
        if (constant != null && constant.getValue() instanceof String value) {
            return value;
        }
        if (constant != null && constant.getValue() instanceof QuestRegistryId value) {
            return value.value();
        }
        return "";
    }

    public String[] getStringArray(NodeModel nodeModel, String optionId) {
        Constant constant = nodeModel.getInputConstantsById().get(NodeOption.PORT_ID_PREFIX + optionId);
        if (constant != null && constant.getValue() instanceof String[] lines) {
            return lines.clone();
        }
        return new String[0];
    }

    public String getPortString(NodeModel nodeModel, String portId) {
        Constant constant = nodeModel.getInputConstantsById().get(portId);
        if (constant != null && constant.getValue() instanceof String value) {
            return value;
        }
        return "";
    }

    public boolean getBool(NodeModel nodeModel, String optionId) {
        return getBool(nodeModel, optionId, false);
    }

    public boolean getBool(NodeModel nodeModel, String optionId, boolean fallback) {
        Constant constant = getOptionConstant(nodeModel, optionId);
        if (constant != null && constant.getValue() instanceof Boolean value) {
            return value;
        }
        return fallback;
    }

    public int getInt(NodeModel nodeModel, String optionId) {
        return getInt(nodeModel, optionId, 0);
    }

    public int getInt(NodeModel nodeModel, String optionId, int fallback) {
        Constant constant = getOptionConstant(nodeModel, optionId);
        if (constant != null && constant.getValue() instanceof Integer value) {
            return value;
        }
        if (constant != null && constant.getValue() instanceof Number number) {
            return number.intValue();
        }
        return fallback;
    }

    public float getFloat(NodeModel nodeModel, String optionId) {
        Constant constant = getOptionConstant(nodeModel, optionId);
        if (constant != null && constant.getValue() instanceof Float value) {
            return value;
        }
        if (constant != null && constant.getValue() instanceof Number number) {
            return number.floatValue();
        }
        return 0.0f;
    }

    public double getDouble(NodeModel nodeModel, String optionId, double fallback) {
        Constant constant = getOptionConstant(nodeModel, optionId);
        if (constant != null && constant.getValue() instanceof Number number) {
            return number.doubleValue();
        }
        return fallback;
    }

    public Object getOptionValue(NodeModel nodeModel, String optionId) {
        Constant constant = getOptionConstant(nodeModel, optionId);
        return constant == null ? null : constant.getValue();
    }

    private Constant getOptionConstant(NodeModel nodeModel, String optionId) {
        return nodeModel.getInputConstantsById().get(NodeOption.PORT_ID_PREFIX + optionId);
    }

    public QuestSubmitMode getSubmitMode(NodeModel nodeModel, String optionId) {
        Constant constant = nodeModel.getInputConstantsById().get(NodeOption.PORT_ID_PREFIX + optionId);
        return constant != null && constant.getValue() instanceof QuestSubmitMode mode
                ? mode
                : QuestSubmitMode.AUTO;
    }

    public TaskObjectiveType getObjectiveType(NodeModel nodeModel, String optionId) {
        Constant constant = nodeModel.getInputConstantsById().get(NodeOption.PORT_ID_PREFIX + optionId);
        return constant != null && constant.getValue() instanceof TaskObjectiveType type
                ? type
                : TaskObjectiveType.REQUIRED;
    }

    public String resolveStepId(NodeModel nodeModel) {
        return nodeModel == null || nodeModel.getUid() == null ? "" : nodeModel.getUid().toString();
    }

    public ItemStack getItemStack(NodeModel nodeModel, String optionId) {
        Constant constant = nodeModel.getInputConstantsById().get(NodeOption.PORT_ID_PREFIX + optionId);
        if (constant != null && constant.getValue() instanceof ItemStack stack) {
            return stack.copy();
        }
        return ItemStack.EMPTY;
    }

    public ItemMatchRule getItemMatchRule(NodeModel nodeModel, String optionId) {
        Constant constant = nodeModel.getInputConstantsById().get(NodeOption.PORT_ID_PREFIX + optionId);
        if (constant != null && constant.getValue() instanceof ItemMatchRule rule) {
            return rule.copy();
        }
        return null;
    }

    public Block getBlock(NodeModel nodeModel, String optionId) {
        Constant constant = nodeModel.getInputConstantsById().get(NodeOption.PORT_ID_PREFIX + optionId);
        if (constant == null) {
            return Blocks.STONE;
        }
        Object value = constant.getValue();
        if (value instanceof Block block) {
            return block;
        }
        if (value instanceof QuestRegistryId registryId) {
            return resolveBlock(registryId.value());
        }
        if (value instanceof String id) {
            return resolveBlock(id);
        }
        return Blocks.STONE;
    }

    public DisplayIcon getDisplayIcon(NodeModel nodeModel, String optionId) {
        Constant constant = nodeModel.getInputConstantsById().get(NodeOption.PORT_ID_PREFIX + optionId);
        if (constant != null && constant.getValue() instanceof DisplayIcon icon) {
            return icon.copy();
        }
        return new DisplayIcon();
    }

    public LootTableReward getLootTableReward(NodeModel nodeModel, String optionId) {
        Constant constant = nodeModel.getInputConstantsById().get(NodeOption.PORT_ID_PREFIX + optionId);
        if (constant != null && constant.getValue() instanceof LootTableReward reward) {
            return reward.copyLootOptions();
        }
        return null;
    }

    public QuestJoinMode getJoinMode(NodeModel nodeModel) {
        Constant constant = nodeModel.getInputConstantsById().get(NodeOption.PORT_ID_PREFIX + "join_mode");
        if (constant != null && constant.getValue() instanceof QuestJoinMode mode) {
            return mode;
        }
        return QuestJoinMode.ANY;
    }

    private static Block resolveBlock(String id) {
        ResourceLocation location = ResourceLocation.tryParse(id == null ? "" : id.trim());
        if (location == null) {
            return Blocks.STONE;
        }
        return BuiltInRegistries.BLOCK.getOptional(location).orElse(Blocks.STONE);
    }

    public int getPortInt(NodeModel nodeModel, String portId) {
        return getPortInt(nodeModel, portId, 0);
    }

    public int getPortInt(NodeModel nodeModel, String portId, int fallback) {
        Constant constant = nodeModel.getInputConstantsById().get(portId);
        if (constant != null && constant.getValue() instanceof Integer value) {
            return value;
        }
        if (constant != null && constant.getValue() instanceof Number number) {
            return number.intValue();
        }
        return fallback;
    }

    public float getPortFloat(NodeModel nodeModel, String portId) {
        Constant constant = nodeModel.getInputConstantsById().get(portId);
        if (constant != null && constant.getValue() instanceof Float value) {
            return value;
        }
        if (constant != null && constant.getValue() instanceof Number number) {
            return number.floatValue();
        }
        return 0f;
    }

    public List<QuestValueToken> compileRuntimeValueExpression(CustomNodeModelImpl node, String portId, int depth) {
        if (depth <= 0) {
            return null;
        }
        PortModel port = node.getInputsById().get(portId);
        if (port == null) {
            return null;
        }

        String varName = variableBindings.variableNameForPort(port);
        if (varName != null && !varName.isEmpty()) {
            return new ArrayList<>(List.of(QuestValueToken.variable(varName)));
        }

        for (PortModel connectedPort : port.getConnectedPorts()) {
            List<QuestValueToken> expression = compileRuntimeValueExpressionFromPort(connectedPort, depth - 1);
            if (expression != null && !expression.isEmpty()) {
                return expression;
            }
        }

        if (!port.getConnectedPorts().isEmpty()) {
            return null;
        }
        Constant constant = node.getInputConstantsById().get(portId);
        if (constant != null && constant.getValue() instanceof Number) {
            return new ArrayList<>(List.of(QuestValueToken.constant(getPortFloat(node, portId))));
        }
        return null;
    }

    public List<QuestValueToken> compileRuntimeIntExpression(CustomNodeModelImpl node, String portId, int fallback) {
        List<QuestValueToken> expression = compileRuntimeValueExpression(node, portId, 12);
        if (expression != null && !expression.isEmpty()) {
            return new ArrayList<>(expression);
        }
        return new ArrayList<>(List.of(QuestValueToken.constant(getPortInt(node, portId, fallback))));
    }

    public List<QuestValueToken> compileRuntimeValueExpressionFromPort(PortModel sourcePort, int depth) {
        if (depth <= 0) {
            return null;
        }
        var sourceNode = sourcePort.getNodeModel();
        if (sourceNode instanceof VariableNodeModelImpl varNode) {
            var declaration = varNode.getVariableDeclarationModel();
            if (declaration != null) {
                return new ArrayList<>(List.of(QuestValueToken.variable(variableNameForDeclaration(declaration))));
            }
        }
        if (sourceNode instanceof ConstantNodeModel constantNode) {
            Constant constant = constantNode.getConstant();
            if (constant != null && constant.getValue() instanceof Number number) {
                return new ArrayList<>(List.of(QuestValueToken.constant(number.floatValue())));
            }
        }
        if (sourceNode instanceof CustomNodeModelImpl customNode) {
            IQuestExpressionNodeCompiler compiler = findExpressionCompiler(customNode);
            if (compiler != null) {
                return compiler.compileExpression(this, customNode, depth);
            }
        }
        return null;
    }

    public String findVariableName(CustomNodeModelImpl node, String portId) {
        PortModel port = node.getInputsById().get(portId);
        if (port == null) {
            return "";
        }

        String fromMap = variableBindings.variableNameForPort(port);
        if (fromMap != null) {
            return fromMap;
        }

        var connectedPorts = port.getConnectedPorts();
        for (PortModel connectedPort : connectedPorts) {
            String result = extractVarNameFromConnectedPort(connectedPort);
            if (result != null) {
                return result;
            }
        }

        for (PortModel connectedPort : connectedPorts) {
            if (connectedPort.getNodeModel() instanceof CustomNodeModelImpl customSource) {
                String result = traceVariableThroughCustomNode(customSource, 5);
                if (result != null) {
                    return result;
                }
            }
        }

        String wireResult = scanWiresDirectly(port);
        return wireResult == null ? "" : wireResult;
    }

    public Float tryEvaluateCompileTimeValue(CustomNodeModelImpl node, String portId) {
        List<QuestValueToken> expression = compileRuntimeValueExpression(node, portId, 12);
        return evaluateCompileTimeExpression(expression);
    }

    public float tracePortFloatValue(CustomNodeModelImpl node, String inputPortId) {
        Float value = tryEvaluateCompileTimeValue(node, inputPortId);
        return value == null ? getPortFloat(node, inputPortId) : value;
    }

    public int tracePortIntValue(CustomNodeModelImpl node, String inputPortId, int fallback, int minValue) {
        Float value = tryEvaluateCompileTimeValue(node, inputPortId);
        int resolved = value == null ? getPortInt(node, inputPortId, fallback) : Math.round(value);
        return Math.max(minValue, resolved);
    }

    public static String formatFloatValue(float value) {
        if (value == (long) value) {
            return String.valueOf((long) value);
        }
        return String.valueOf(value);
    }

    private void indexVariableInitialValues() {
        Set<UUID> visitedGraphs = new HashSet<>();
        indexVariableInitialValues(graphModel, visitedGraphs);
    }

    private void indexVariableInitialValues(GraphModel graphModel, Set<UUID> visitedGraphs) {
        if (!visitedGraphs.add(graphModel.getUid())) {
            return;
        }
        for (var nodeModel : graphModel.getNodeModels()) {
            if (nodeModel instanceof VariableNodeModelImpl varNode) {
                var declaration = varNode.getVariableDeclarationModel();
                if (declaration == null || isInheritedDeclaration(declaration)) {
                    continue;
                }
                String runtimeName = variableNameForDeclaration(declaration);
                if (variableInitialValues.containsKey(runtimeName)) {
                    continue;
                }
                QuestVariableValue override = defaultOverrideForDeclaration(declaration);
                variableInitialValues.put(runtimeName, override == null
                        ? QuestVariableValue.fromConstant(declaration.getDataTypeHandle(), declaration.getInitializationModel())
                        : override.copy());
            }
            if (nodeModel instanceof SubgraphNodeModel subgraphNode) {
                GraphModel nested = subgraphNode.getSubgraphModel();
                if (nested != null) {
                    indexVariableInitialValues(nested, visitedGraphs);
                }
            }
        }
        List<GraphModel> localSubGraphs = graphModel.getLocalSubGraphs();
        if (localSubGraphs != null) {
            for (GraphModel subgraph : localSubGraphs) {
                if (subgraph != null) {
                    indexVariableInitialValues(subgraph, visitedGraphs);
                }
            }
        }
    }

    private String extractVarNameFromConnectedPort(PortModel connectedPort) {
        var nodeModel = connectedPort.getNodeModel();
        if (nodeModel instanceof VariableNodeModelImpl varNode) {
            var declaration = varNode.getVariableDeclarationModel();
            if (declaration != null) {
                return variableNameForDeclaration(declaration);
            }
        }
        return null;
    }

    private String scanWiresDirectly(PortModel targetPort) {
        UUID targetUid = targetPort.getUid();
        GraphModel ownerGraph = targetPort.getGraphModel() == null ? graphModel : targetPort.getGraphModel();
        for (var wire : ownerGraph.getWireModels()) {
            var fromPort = wire.getFromPort();
            var toPort = wire.getToPort();
            if (fromPort == null || toPort == null) {
                continue;
            }

            if (toPort == targetPort || toPort.getUid().equals(targetUid)) {
                var sourceNode = fromPort.getNodeModel();
                if (sourceNode instanceof VariableNodeModelImpl varNode) {
                    var declaration = varNode.getVariableDeclarationModel();
                    if (declaration != null) {
                        return variableNameForDeclaration(declaration);
                    }
                }
            }
        }
        return null;
    }

    private String traceVariableThroughCustomNode(CustomNodeModelImpl node, int depth) {
        if (depth <= 0) {
            return null;
        }
        for (var entry : node.getInputsById().entrySet()) {
            PortModel inputPort = entry.getValue();
            if (inputPort.getDataTypeHandle().equals(TypeHandles.EXECUTION_FLOW)) {
                continue;
            }

            String fromMap = variableBindings.variableNameForPort(inputPort);
            if (fromMap != null) {
                return fromMap;
            }

            for (PortModel connectedPort : inputPort.getConnectedPorts()) {
                if (connectedPort.getNodeModel() instanceof VariableNodeModelImpl varNode) {
                    var declaration = varNode.getVariableDeclarationModel();
                    if (declaration != null) {
                        return variableNameForDeclaration(declaration);
                    }
                }
                if (connectedPort.getNodeModel() instanceof CustomNodeModelImpl customSource) {
                    String result = traceVariableThroughCustomNode(customSource, depth - 1);
                    if (result != null) {
                        return result;
                    }
                }
            }
        }
        return null;
    }

    private String variableNameForDeclaration(VariableDeclarationModelBase declaration) {
        String alias = aliasForDeclaration(declaration);
        return alias == null || alias.isEmpty() ? declaration.getName() : alias;
    }

    private String aliasForDeclaration(VariableDeclarationModelBase declaration) {
        return variableBindings.aliasFor(declaration);
    }

    private QuestVariableValue defaultOverrideForDeclaration(VariableDeclarationModelBase declaration) {
        return variableBindings.defaultOverrideFor(declaration);
    }

    private boolean isInheritedDeclaration(VariableDeclarationModelBase declaration) {
        return variableBindings.isInherited(declaration);
    }

    private Float evaluateCompileTimeExpression(List<QuestValueToken> expression) {
        if (expression == null || expression.isEmpty()) {
            return null;
        }
        ArrayDeque<Float> stack = new ArrayDeque<>();
        for (QuestValueToken token : expression) {
            switch (token.kind) {
                case CONSTANT -> stack.push(token.value);
                case VARIABLE -> {
                    QuestVariableValue value = variableInitialValues.get(token.variableName);
                    if (value == null) {
                        return null;
                    }
                    stack.push(value.asFloat());
                }
                case SCOREBOARD -> {
                    // 计分板值依赖当前玩家和服务器运行时，导出阶段不能提前折叠成常量。
                    return null;
                }
                case ADD -> {
                    Float b = pop(stack);
                    Float a = pop(stack);
                    if (a == null || b == null) return null;
                    stack.push(a + b);
                }
                case SUBTRACT -> {
                    Float b = pop(stack);
                    Float a = pop(stack);
                    if (a == null || b == null) return null;
                    stack.push(a - b);
                }
                case MULTIPLY -> {
                    Float b = pop(stack);
                    Float a = pop(stack);
                    if (a == null || b == null) return null;
                    stack.push(a * b);
                }
                case DIVIDE -> {
                    Float b = pop(stack);
                    Float a = pop(stack);
                    if (a == null || b == null) return null;
                    stack.push(b != 0f ? a / b : 0f);
                }
                case CLAMP -> {
                    Float max = pop(stack);
                    Float min = pop(stack);
                    Float value = pop(stack);
                    if (value == null || min == null || max == null) return null;
                    stack.push(Math.max(min, Math.min(max, value)));
                }
                case RANDOM -> {
                    return null;
                }
            }
        }
        return stack.isEmpty() ? null : stack.pop();
    }

    private static Float pop(ArrayDeque<Float> stack) {
        return stack.isEmpty() ? null : stack.pop();
    }
}
