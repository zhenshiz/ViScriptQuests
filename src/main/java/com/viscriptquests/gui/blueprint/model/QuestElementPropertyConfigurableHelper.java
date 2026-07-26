package com.viscriptquests.gui.blueprint.model;

import com.lowdragmc.lowdraglib2.configurator.IConfigurable;
import com.lowdragmc.lowdraglib2.configurator.annotation.ConfigNumber;
import com.lowdragmc.lowdraglib2.configurator.ui.ColorConfigurator;
import com.lowdragmc.lowdraglib2.configurator.ui.Configurator;
import com.lowdragmc.lowdraglib2.configurator.ui.NumberConfigurator;
import com.lowdragmc.lowdraglib2.configurator.ui.StringConfigurator;
import com.lowdragmc.lowdraglib2.gui.texture.Icons;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.data.TextWrap;
import com.lowdragmc.lowdraglib2.gui.ui.data.Vertical;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.node.IResizeWidth;
import com.lowdragmc.lowdraglib2.nodegraphtookit.gui.GraphView;
import com.lowdragmc.lowdraglib2.nodegraphtookit.gui.command.ElementRenameColorCommands;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.GraphElementModel;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.IHasElementColor;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.IHasName;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.AbstractNodeModel;
import dev.vfyjxf.taffy.style.FlexDirection;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

final class QuestElementPropertyConfigurableHelper {
    private QuestElementPropertyConfigurableHelper() {
    }

    public static IConfigurable build(GraphElementModel model, @Nullable GraphView view) {
        return IConfigurable.create(group -> {
            if (model instanceof AbstractNodeModel nodeModel) {
                group.addConfigurator(createNodeUuidConfigurator(nodeModel));
            }

            if (model.isRenamable() && model instanceof IHasName named) {
                group.addConfigurator(new StringConfigurator(
                        "graph.name",
                        named::getName,
                        newName -> {
                            if (view != null) {
                                view.dispatchCommand(new ElementRenameColorCommands.RenameElementCommand(model, newName));
                            } else {
                                named.setName(newName);
                            }
                        },
                        named.getName(),
                        true));
            }

            if (model.isColorable() && model instanceof IHasElementColor colored) {
                var colorConfigurator = new ColorConfigurator(
                        "graph.color",
                        colored::getElementColor,
                        newColor -> {
                            if (view != null) {
                                view.dispatchCommand(new ElementRenameColorCommands.SetElementColorCommand(model, newColor));
                            } else {
                                colored.setColor(newColor);
                            }
                        },
                        colored.getDefaultColor(),
                        true);

                var resetBtn = createResetColorButton(model, colored, view);
                colorConfigurator.inlineContainer.getLayout().flexDirection(FlexDirection.ROW);
                colorConfigurator.colorPreview.getLayout().flex(1);
                colorConfigurator.inlineContainer.addChild(resetBtn);
                group.addConfigurator(colorConfigurator);
            }

            if (model.isResizable() && model instanceof IResizeWidth resizable) {
                group.addConfigurator(new NumberConfigurator(
                        "graph.min_width",
                        resizable::getMinWidth,
                        value -> resizable.setMinWidth(value.floatValue()),
                        0f,
                        true)
                        .setRange(0, 2000)
                        .setWheel(1)
                        .setType(ConfigNumber.Type.FLOAT));
            }
        });
    }

    private static Configurator createNodeUuidConfigurator(AbstractNodeModel nodeModel) {
        String uuid = nodeModel.getUid() == null ? "" : nodeModel.getUid().toString();
        var uuidLabel = new Label();
        uuidLabel.setText(Component.literal(uuid));
        uuidLabel.textStyle(style -> {
            style.textWrap(TextWrap.HOVER_ROLL);
            style.textAlignVertical(Vertical.CENTER);
        });
        uuidLabel.layout(layout -> {
            layout.height(14);
            layout.flex(1);
        });
        uuidLabel.setOverflowVisible(false);
        uuidLabel.style(style -> style.tooltips(Component.literal(uuid)));

        var configurator = new Configurator("viscript_quests.editor.node_uuid");
        configurator.setCopiableDirect(uuid);
        configurator.addInlineChild(uuidLabel);
        return configurator;
    }

    private static UIElement createResetColorButton(GraphElementModel model,
                                                    IHasElementColor colored,
                                                    @Nullable GraphView view) {
        return new Button().noText()
                .setOnClick(event -> {
                    if (!colored.hasUserColor()) {
                        return;
                    }
                    if (view != null) {
                        view.dispatchCommand(new ElementRenameColorCommands.ResetElementColorCommand(model));
                    } else {
                        colored.resetColor();
                    }
                })
                .layout(layout -> layout.width(14).height(14))
                .style(style -> style.tooltips("graph.color.reset"))
                .addChild(new UIElement()
                        .layout(layout -> {
                            layout.heightPercent(100);
                            layout.setAspectRatio(1);
                        })
                        .addClass("__white_icon__")
                        .style(style -> style.backgroundTexture(Icons.REPLAY)));
    }
}
