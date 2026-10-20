package com.viscriptquests.quest.data;

import lombok.AllArgsConstructor;
import lombok.Getter;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionHand;
import org.jetbrains.annotations.NotNull;

/**
 * 物品使用事件的手部筛选条件。
 */
@Getter
@AllArgsConstructor
public enum ItemUseHand implements StringRepresentable {
    ANY("viscript_quests.item_use_hand.any"),
    MAIN_HAND("viscript_quests.item_use_hand.main_hand"),
    OFF_HAND("viscript_quests.item_use_hand.off_hand");

    private final String name;

    /**
     * 判断事件中的手是否符合此筛选条件。
     *
     * @param hand 事件中使用物品的手
     * @return 不限制手部或与指定手一致时返回 {@code true}
     */
    public boolean matches(InteractionHand hand) {
        return this == ANY || (this == MAIN_HAND && hand == InteractionHand.MAIN_HAND)
                || (this == OFF_HAND && hand == InteractionHand.OFF_HAND);
    }

    @Override
    public @NotNull String getSerializedName() {
        return name;
    }
}
