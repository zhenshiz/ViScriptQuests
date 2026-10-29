package com.viscriptquests.gui.editor;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.lowdragmc.lowdraglib2.Platform;
import com.lowdragmc.lowdraglib2.configurator.EditAction;
import com.lowdragmc.lowdraglib2.editor.project.IProject;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.node.NodeAttribute;
import com.lowdragmc.lowdraglib2.nodegraphtookit.gui.itemlibrary.GraphNodeCreationData;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.graph.CustomGraphModelImpl;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.NodeModel;
import com.viscript_lib.gui.ai.*;
import com.viscript_lib.gui.editor.ViScriptEditor;
import com.viscriptquests.gui.blueprint.QuestBlueprintGraph;
import com.viscriptquests.gui.blueprint.QuestBlueprintGraphModel;
import com.viscriptquests.gui.blueprint.model.QuestSubQuestNodeModel;
import org.joml.Vector2f;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** 提供任务主图、小任务子图、节点常量及连线的 AI 编辑。 */
public final class QuestAiAdapter implements AiEditorAdapter {
    @Override
    public String guide() {
        return """
                This is ViScriptQuests' quest blueprint editor. read_editor section=graph (default) returns
                actual nodes, UUIDs, constants, ports, wires and variables. section=catalog lists registered
                nodes with their default constants and ports; query filters names or IDs. graph defaults to
                root; to read/edit a subquest's content, set graph to that subquest node's UUID. The parent
                subquest's title/subtitle/description remain on the root graph; objectives and rewards live
                in its subgraph, beginning at sub_quest_start's objectives/rewards outputs. Use the exact
                port and value keys returned by read_editor, including the option prefix. Values use the
                returned JSON encoding (item stacks, registry IDs, enum values, arrays, custom structures).
                Item task/reward item_stack selects item identity; use the item_count input constant for amount.
                The catalog is scoped: objectives/rewards are available in subquest graphs, not in root.
                add_node requires a registered type and a new UUID; set_node merges supplied constants,
                remove_node deletes its connected wires. connect uses from/output/to/input; disconnect uses
                the wire UUID. add_constant/add_variable use catalog value_types and value:{data:<JSON>}.
                variable_node creates a read node for a variable declaration. Positions are canvas pixels.
                Group related changes in one operations batch: the resulting quest must pass its native
                export compiler. Read relevant catalog templates before creating nodes. Existing unrelated
                nodes, variables, subgraphs and layout survive. This edits the local project only.
                """;
    }

    @Override
    public JsonObject readSchema() {
        var schema = AiStructuredData.readSchema("graph (default) or catalog (available node types, constants and ports).");
        AiJson.property(schema, "graph", "string", "root (default), or the owning subquest node UUID for its objectives/rewards subgraph.");
        return schema;
    }

    @Override
    public JsonObject describe(IProject project, int offset, int limit) { return describe(project, new JsonObject(), offset, limit); }

    @Override
    public JsonObject describe(IProject project, JsonObject arguments, int offset, int limit) {
        AiJson.keys(arguments, "offset", "limit", "section", "query", "graph");
        var root = graph((QuestProject) project);
        var model = scope(root, arguments);
        String section = arguments.has("section") ? AiJson.string(arguments, "section") : "graph";
        JsonArray rows;
        if (section.equals("graph")) {
            rows = AiGraphTools.rows(model);
            for (var row : rows) if (row.getAsJsonObject().has("id")) {
                var value = model.getModel(UUID.fromString(row.getAsJsonObject().get("id").getAsString()));
                if (value instanceof QuestSubQuestNodeModel subquest) row.getAsJsonObject().addProperty("subgraph", subquest.getUid().toString());
            }
        } else if (section.equals("catalog")) {
            rows = new JsonArray();
            for (var type : model.getSupportNodes()) {
                var annotation = type.getAnnotation(NodeAttribute.class);
                if (annotation == null) continue;
                var probe = (CustomGraphModelImpl) new QuestBlueprintGraph().graphModel;
                var node = (NodeModel) QuestBlueprintGraphModel.createNodeFromData(new GraphNodeCreationData(probe, new Vector2f(), null, null), type);
                var row = AiGraphTools.node(node);
                row.remove("id"); row.remove("x"); row.remove("y");
                row.addProperty("kind", "node_type");
                rows.add(row);
            }
        } else throw new IllegalArgumentException("Unknown quest section: " + section);
        var result = AiStructuredData.page(rows, arguments, offset, limit);
        result.addProperty("graph", arguments.has("graph") ? AiJson.string(arguments, "graph") : "root");
        var types = new JsonArray();
        model.getLibrarySupportTypes().forEach(t -> types.add(t.getIdentification()));
        result.add("value_types", types);
        return result;
    }

    @Override
    public JsonObject editSchema() {
        var root = AiJson.object();
        var choices = AiGraphTools.schemas();
        for (var choice : choices) AiJson.property(choice.getAsJsonObject(), "graph", "string", "root or owning subquest node UUID; omit for root.");
        var item = new JsonObject(); item.add("oneOf", choices);
        var operations = AiJson.property(root, "operations", "array", "Ordered atomic graph edits. Final quest is checked by its native compiler.");
        operations.add("items", item); operations.addProperty("minItems", 1);
        AiJson.required(root, "operations");
        return root;
    }

    @Override
    public List<String> edit(IProject draft, JsonObject arguments) {
        AiJson.keys(arguments, "operations");
        if (!arguments.has("operations") || !arguments.get("operations").isJsonArray() || arguments.getAsJsonArray("operations").isEmpty())
            throw new IllegalArgumentException("Expected nonempty operations array");
        var project = (QuestProject) draft;
        var root = graph(project);
        var changes = new ArrayList<String>();
        for (var raw : arguments.getAsJsonArray("operations")) {
            var op = raw.getAsJsonObject();
            var model = scope(root, op);
            changes.add(AiGraphTools.edit(model, op, (type, id, position) -> create(model, type, id, position)));
        }
        var data = new net.minecraft.nbt.CompoundTag();
        data.put("graph", root.graphModel.serializeNBT(Platform.getFrozenRegistry()));
        project.deserializeProject(Platform.getFrozenRegistry(), data);
        project.compileQuestFile();
        return changes;
    }

    private static NodeModel create(CustomGraphModelImpl graph, String type, UUID id, Vector2f position) {
        var nodeClass = graph.getSupportNodes().stream().filter(c -> {
            var attribute = c.getAnnotation(NodeAttribute.class);
            return attribute != null && attribute.name().equals(type);
        }).findFirst().orElseThrow(() -> new IllegalArgumentException("Unavailable quest node: " + type));
        return (NodeModel) QuestBlueprintGraphModel.createNodeFromData(new GraphNodeCreationData(graph, position, null, id), nodeClass);
    }

    private static QuestBlueprintGraph graph(QuestProject project) {
        var graph = new QuestBlueprintGraph();
        graph.graphModel.deserializeNBT(Platform.getFrozenRegistry(), project.currentGraphTag());
        return graph;
    }

    private static CustomGraphModelImpl scope(QuestBlueprintGraph root, JsonObject args) {
        String id = args.has("graph") ? AiJson.string(args, "graph") : "root";
        if (id.equals("root")) return (CustomGraphModelImpl) root.graphModel;
        if (root.graphModel.getModel(UUID.fromString(id)) instanceof QuestSubQuestNodeModel node
                && node.ensureLocalSubgraph() instanceof CustomGraphModelImpl graph) return graph;
        throw new IllegalArgumentException("Unknown subquest graph: " + id);
    }

    @Override
    public EditAction createAction(ViScriptEditor editor, IProject draft) {
        var project = (QuestProject) editor.getCurrentProject();
        var before = project.captureAiState();
        var after = ((QuestProject) draft).captureAiState();
        return EditAction.of(() -> project.installAiState(after, editor), () -> project.installAiState(before, editor));
    }

    @Override
    public void setPreviewMode(ViScriptEditor editor, boolean previewing) { ((QuestProject) editor.getCurrentProject()).setAiPreviewing(previewing); }

    @Override
    public void refresh(ViScriptEditor editor) { editor.inspectorView.clear(); }
}
