package com.viscriptquests.quest.data;

import com.lowdragmc.lowdraglib2.Platform;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.type.TypeHandle;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.type.TypeHandles;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.constant.Constant;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.constant.TypeConstant;
import com.lowdragmc.lowdraglib2.syncdata.IPersistedSerializable;
import com.lowdragmc.lowdraglib2.syncdata.annotation.Persisted;
import com.lowdragmc.lowdraglib2.utils.PersistedParser;
import com.mojang.serialization.Codec;
import com.viscriptquests.util.CodecUtil;
import io.netty.buffer.ByteBuf;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.ItemStack;

import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Objects;

/**
 * 保存从蓝图黑板变量导出的任务运行时变量值。
 *
 * <p>此类型只保存变量的类型标识和 LDLib2 常量序列化结果。它不保存蓝图变量声明的
 * <code>UUID</code>、端口、连线、作用域或修饰符；变量名由外层变量表保存。
 * <p>此类型允许运行时以类型保持方式保存 <code>String</code>、<code>Integer</code>、
 * <code>Float</code>、<code>Boolean</code>、<code>ItemStack</code>，以及当前蓝图系统使用的
 * 字符串数组、枚举和 LDLib2 可持久化对象。数值比较和数学表达式通过数值投影方法读取值，
 * 不改变底层保存的类型。
 */
public class QuestVariableValue implements IPersistedSerializable {
    public static final Codec<QuestVariableValue> CODEC = PersistedParser.createCodec(QuestVariableValue::new);
    public static final StreamCodec<ByteBuf, QuestVariableValue> STREAM_CODEC = PersistedParser.createStreamCodec(QuestVariableValue::new);

    @Persisted
    public String typeId = "";
    @Persisted
    public CompoundTag constantTag = new CompoundTag();

    /**
     * 从 LDLib2 图常量创建任务运行时变量值。
     *
     * <p>此方法用于蓝图导出阶段，把黑板变量声明中的类型和
     * <code>initializationModel</code> 转换为任务文件可保存的运行时值。
     * <code>typeHandle</code> 单独传入，是为了在 <code>constant</code> 缺失或常量未完整
     * 携带类型信息时，仍然保留变量声明的类型。若只有类型没有常量，则会创建一个该类型的
     * 默认 <code>TypeConstant</code>，让导出的变量仍有可反序列化的默认值。
     *
     * @param typeHandle LDLib2 类型句柄，表示蓝图变量声明的类型；可以为 <code>null</code>
     * @param constant   LDLib2 常量，保存变量初始值；可以为 <code>null</code>
     * @return 包含最佳可用类型标识和序列化常量数据的任务运行时变量值
     */
    public static QuestVariableValue fromConstant(TypeHandle typeHandle, Constant constant) {
        QuestVariableValue variableValue = new QuestVariableValue();
        if (typeHandle != null) {
            variableValue.typeId = typeHandle.getIdentification();
        } else if (constant != null && constant.getTypeHandle() != null) {
            variableValue.typeId = constant.getTypeHandle().getIdentification();
        }

        if (constant != null) {
            variableValue.constantTag = TypeConstant.serializeConstant(constant, Platform.getFrozenRegistry());
            if (variableValue.typeId.isEmpty() && constant.getTypeHandle() != null) {
                variableValue.typeId = constant.getTypeHandle().getIdentification();
            }
        } else if (!variableValue.typeId.isEmpty()) {
            TypeConstant fallback = new TypeConstant();
            fallback.init(TypeHandle.create(variableValue.typeId));
            variableValue.constantTag = TypeConstant.serializeConstant(fallback, Platform.getFrozenRegistry());
        }
        return variableValue;
    }

    /**
     * 创建指定 LDLib2 类型的任务运行时变量值。
     *
     * <p>此方法是类型保持写入入口。它会先按传入类型初始化 <code>TypeConstant</code>，
     * 再写入已经规范化到该类型的值。若 <code>value</code> 为 <code>null</code>，则保留
     * LDLib2 为该类型提供的默认值。当前蓝图中使用的基础类型、字符串数组、枚举和
     * <code>DisplayIcon</code> 这类 LDLib2 可持久化对象都通过此路径保存。
     *
     * @param typeHandle LDLib2 类型句柄，表示要保存的目标类型；<code>null</code> 时按 <code>Float</code> 保存
     * @param value      对象值，表示要写入变量的运行时值；可以为 <code>null</code>
     * @return 按指定类型序列化的任务运行时变量值
     */
    public static QuestVariableValue of(TypeHandle typeHandle, Object value) {
        TypeHandle safeType = typeHandle == null ? TypeHandles.FLOAT : typeHandle;
        TypeConstant constant = new TypeConstant();
        constant.init(safeType);
        Object coerced = coerceValueForType(safeType, value);
        if (coerced != null) {
            constant.setValue(coerced);
        }
        return fromConstant(safeType, constant);
    }

    /**
     * 创建以浮点数保存的任务运行时变量值。
     *
     * <p>此方法只用于没有既有变量类型可参考的数值写入。若要覆盖已有变量，应优先使用
     * <code>withNumericValue(float)</code> 或 <code>of(TypeHandle, Object)</code>，
     * 以保留蓝图变量原本的 <code>Int</code>、<code>Bool</code>、<code>String</code>、
     * <code>ItemStack</code> 等类型。
     *
     * @param value 浮点数值，表示要保存的变量值
     * @return 以 LDLib2 浮点常量格式序列化的任务运行时变量值
     */
    public static QuestVariableValue ofFloat(float value) {
        return of(TypeHandles.FLOAT, value);
    }

    /**
     * 创建以整数保存的任务运行时变量值。
     *
     * @param value 整数值，表示要保存的变量值
     * @return 以 LDLib2 整数常量格式序列化的任务运行时变量值
     */
    public static QuestVariableValue ofInt(int value) {
        return of(TypeHandles.INT, value);
    }

    /**
     * 创建以布尔值保存的任务运行时变量值。
     *
     * @param value 布尔值，表示要保存的变量值
     * @return 以 LDLib2 布尔常量格式序列化的任务运行时变量值
     */
    public static QuestVariableValue ofBoolean(boolean value) {
        return of(TypeHandles.BOOL, value);
    }

    /**
     * 创建以字符串保存的任务运行时变量值。
     *
     * @param value 字符串值，表示要保存的变量值；<code>null</code> 会保存为空字符串
     * @return 以 LDLib2 字符串常量格式序列化的任务运行时变量值
     */
    public static QuestVariableValue ofString(String value) {
        return of(TypeHandles.STRING, value);
    }

    /**
     * 创建以物品栈保存的任务运行时变量值。
     *
     * @param value 物品栈值，表示要保存的变量值；<code>null</code> 会保存为 <code>ItemStack.EMPTY</code>
     * @return 以 LDLib2 物品栈常量格式序列化的任务运行时变量值
     */
    public static QuestVariableValue ofItemStack(ItemStack value) {
        return of(TypeHandles.ITEM_STACK, value);
    }

    /**
     * 创建此任务运行时变量值的深拷贝。
     *
     * <p>拷贝通过 <code>CODEC</code> 往返完成，而不是只复制当前字段。这样新增字段或
     * 嵌套持久化数据时，拷贝行为会和存档数据、网络同步等 LDLib2 持久化路径保持一致。
     *
     * @return 此任务运行时变量值的独立副本
     */
    public QuestVariableValue copy() {
        Tag tag = CodecUtil.serializeNBT(CODEC, this, Platform.getFrozenRegistry());
        return CodecUtil.deserializeNBT(CODEC, tag, Platform.getFrozenRegistry());
    }

    /**
     * 反序列化并返回此变量保存的原始类型值。
     *
     * <p>返回值可能是 <code>Float</code>、<code>Boolean</code>、<code>String</code>、
     * <code>ItemStack</code> 或其他 LDLib2 常量系统能够还原的类型。此方法只负责还原原始值，
     * 不负责数值转换、显示格式化或缺失值补全。
     *
     * @return 序列化 LDLib2 常量中保存的原始值；没有保存常量时返回 <code>null</code>
     */
    public Object value() {
        if (constantTag == null || constantTag.isEmpty()) {
            return null;
        }
        Constant constant = TypeConstant.deserializeConstant(constantTag, Platform.getFrozenRegistry());
        return constant == null ? null : constant.getValue();
    }

    /**
     * 返回此变量值记录的 LDLib2 类型句柄。
     *
     * <p>当旧数据没有保存 <code>typeId</code> 时，会从 <code>constantTag.type</code> 回退读取；
     * 若仍然无法确定类型，则返回 <code>Float</code>，以保持旧数值变量数据可用。
     *
     * @return 此变量值记录的 LDLib2 类型句柄
     */
    public TypeHandle typeHandle() {
        if (typeId != null && !typeId.isEmpty()) {
            return TypeHandle.create(typeId);
        }
        if (constantTag != null && constantTag.contains("type")) {
            return TypeHandle.create(constantTag.getString("type"));
        }
        return TypeHandles.FLOAT;
    }

    /**
     * 返回此变量值是否按指定 LDLib2 类型保存。
     *
     * @param typeHandle LDLib2 类型句柄，表示要与此变量值保存类型比较的类型
     * @return 当前变量值的类型标识与参数相同时返回 <code>true</code>
     */
    public boolean isType(TypeHandle typeHandle) {
        return typeHandle != null && Objects.equals(typeHandle().getIdentification(), typeHandle.getIdentification());
    }

    /**
     * 将此变量值转换为任务运行时计算使用的浮点数。
     *
     * <p>数值类型直接转换，布尔值按 <code>true = 1</code>、<code>false = 0</code> 转换，
     * 字符串会尝试按浮点数解析。无法转换或缺失的值返回 <code>0</code>。此方法是任务条件和
     * 表达式系统的数值投影，不表示变量底层只能保存浮点数。
     *
     * @return 任务条件和算术表达式使用的浮点数表示
     */
    public float asFloat() {
        Object value = value();
        if (value instanceof Number number) {
            return number.floatValue();
        }
        if (value instanceof Boolean bool) {
            return bool ? 1f : 0f;
        }
        if (value instanceof String string) {
            try {
                return Float.parseFloat(string);
            } catch (NumberFormatException ignored) {
            }
        }
        return 0f;
    }

    /**
     * 将此变量值转换为整数。
     *
     * <p>此方法用于需要整数语义的运行时逻辑。数值会直接取整，布尔值按
     * <code>true = 1</code>、<code>false = 0</code> 转换，字符串会尝试按整数解析。
     * 无法转换时返回 <code>0</code>。
     *
     * @return 此变量值的整数表示
     */
    public int asInt() {
        Object value = value();
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value instanceof Boolean bool) {
            return bool ? 1 : 0;
        }
        if (value instanceof String string) {
            try {
                return Integer.parseInt(string);
            } catch (NumberFormatException ignored) {
            }
        }
        return 0;
    }

    /**
     * 将此变量值转换为布尔值。
     *
     * <p>布尔值直接返回，数值按非零为真转换，字符串支持
     * <code>true</code>、<code>false</code> 和 <code>1</code>。其他类型返回
     * <code>false</code>。
     *
     * @return 此变量值的布尔表示
     */
    public boolean asBoolean() {
        Object value = value();
        if (value instanceof Boolean bool) {
            return bool;
        }
        if (value instanceof Number number) {
            return number.floatValue() != 0f;
        }
        if (value instanceof String string) {
            String trimmed = string.trim();
            return Boolean.parseBoolean(trimmed) || "1".equals(trimmed);
        }
        return false;
    }

    /**
     * 将此变量值转换为字符串。
     *
     * <p>字符串值直接返回，字符串数组会格式化为数组文本，物品栈复用
     * <code>displayValue()</code> 的显示规则。缺失值返回空字符串。
     *
     * @return 此变量值的字符串表示
     */
    public String asString() {
        Object value = value();
        return switch (value) {
            case null -> "";
            case String string -> string;
            case String[] strings -> Arrays.toString(strings);
            case ItemStack itemStack -> displayValue();
            default -> String.valueOf(value);
        };
    }

    /**
     * 将此变量值转换为物品栈。
     *
     * <p>只有实际保存为 <code>ItemStack</code> 时才返回副本；其他类型返回
     * <code>ItemStack.EMPTY</code>，避免调用方修改内部值或误把其他类型解释为物品。
     *
     * @return 此变量值的物品栈副本，或空物品栈
     */
    public ItemStack asItemStack() {
        Object value = value();
        return value instanceof ItemStack stack ? stack.copy() : ItemStack.EMPTY;
    }

    /**
     * 按当前变量类型写入数值投影。
     *
     * <p>数值变量会保持原有数值类型：<code>Int</code> 写回整数，<code>Float</code> 写回浮点数。
     * 布尔变量按非零为 <code>true</code> 写回。字符串和物品栈不接受算术写入，会返回当前值副本，
     * 避免非数值变量被运行时修改错误地转换成浮点数。
     *
     * @param numericValue 浮点数值，表示要通过此变量当前类型写回的数值投影
     * @return 保持当前类型的新变量值；非数值类型返回当前值副本
     */
    public QuestVariableValue withNumericValue(float numericValue) {
        TypeHandle typeHandle = typeHandle();
        if (typeHandle.equals(TypeHandles.INT)) {
            return ofInt(Math.round(numericValue));
        }
        if (typeHandle.equals(TypeHandles.FLOAT)) {
            return ofFloat(numericValue);
        }
        if (typeHandle.equals(TypeHandles.BOOL)) {
            return ofBoolean(numericValue != 0f);
        }
        return copy();
    }

    /**
     * 返回此变量类型是否接受运行时算术修改。
     *
     * @return 当前变量类型为 <code>Int</code>、<code>Float</code> 或 <code>Bool</code> 时返回 <code>true</code>
     */
    public boolean supportsNumericMutation() {
        TypeHandle typeHandle = typeHandle();
        return typeHandle.equals(TypeHandles.INT) || typeHandle.equals(TypeHandles.FLOAT) || typeHandle.equals(TypeHandles.BOOL);
    }

    /**
     * 将此变量值格式化为调试消息和文本插值使用的显示文本。
     *
     * <p><code>ItemStack</code> 会显示为数量加物品名，空物品栈显示为 <code>empty</code>。
     * 其他类型使用 <code>String.valueOf(Object)</code>，因此缺失值会显示为 <code>null</code>。
     * 此方法用于运行时调试文本，不作为本地化 UI 标签。
     *
     * @return 此任务运行时变量值的显示文本
     */
    public String displayValue() {
        Object value = value();
        if (value instanceof ItemStack stack) {
            if (stack.isEmpty()) {
                return "empty";
            }
            return stack.getCount() + "x " + stack.getHoverName().getString();
        }
        if (value instanceof String[] strings) {
            return Arrays.toString(strings);
        }
        return String.valueOf(value);
    }

    /**
     * 将传入值规范化为指定 LDLib2 类型可以写入 <code>TypeConstant</code> 的值。
     *
     * <p>基础类型使用显式转换规则，避免字符串、布尔值和数值之间的运行时写入出现
     * <code>ClassCastException</code>。物品栈会保存副本，其他类型会交给对象类型归一化逻辑，
     * 以支持数组、枚举和 LDLib2 可持久化对象。
     *
     * @param typeHandle LDLib2 类型句柄，表示要写入的目标类型
     * @param value      对象值，表示待规范化的运行时值；可以为 <code>null</code>
     * @return 可写入对应 <code>TypeConstant</code> 的规范化值
     */
    private static Object coerceValueForType(TypeHandle typeHandle, Object value) {
        if (typeHandle.equals(TypeHandles.STRING)) {
            return value == null ? "" : String.valueOf(value);
        }
        if (typeHandle.equals(TypeHandles.INT)) {
            if (value instanceof Number number) {
                return number.intValue();
            }
            if (value instanceof Boolean bool) {
                return bool ? 1 : 0;
            }
            if (value instanceof String string) {
                try {
                    return Integer.parseInt(string);
                } catch (NumberFormatException ignored) {
                    return 0;
                }
            }
            return 0;
        }
        if (typeHandle.equals(TypeHandles.FLOAT)) {
            if (value instanceof Number number) {
                return number.floatValue();
            }
            if (value instanceof Boolean bool) {
                return bool ? 1f : 0f;
            }
            if (value instanceof String string) {
                try {
                    return Float.parseFloat(string);
                } catch (NumberFormatException ignored) {
                    return 0f;
                }
            }
            return 0f;
        }
        if (typeHandle.equals(TypeHandles.BOOL)) {
            if (value instanceof Boolean bool) {
                return bool;
            }
            if (value instanceof Number number) {
                return number.floatValue() != 0f;
            }
            if (value instanceof String string) {
                return Boolean.parseBoolean(string) || "1".equals(string.trim());
            }
            return false;
        }
        if (typeHandle.equals(TypeHandles.ITEM_STACK)) {
            return value instanceof ItemStack stack ? stack.copy() : ItemStack.EMPTY;
        }
        return coerceObjectValue(typeHandle, value);
    }

    /**
     * 创建指定 LDLib2 类型的默认常量值。
     *
     * <p>默认值由 LDLib2 的 <code>TypeConstant</code> 初始化逻辑决定，因此会跟随类型系统中
     * 对该类型定义的空值、默认实例或初始值。
     *
     * @param typeHandle LDLib2 类型句柄，表示要创建默认值的类型
     * @return 指定类型的默认值；类型系统没有默认值时可以为 <code>null</code>
     */
    private static Object defaultValueFor(TypeHandle typeHandle) {
        TypeConstant constant = new TypeConstant();
        constant.init(typeHandle);
        return constant.getValue();
    }

    /**
     * 将传入值规范化为指定数组类型。
     *
     * <p>已是目标数组类型时会返回浅拷贝，避免外部数组引用被后续修改影响保存值。
     * 字符串数组额外支持从单个字符串创建数组；其他数组类型无法安全转换时返回该数组类型的
     * LDLib2 默认值。
     *
     * @param rawType 类对象，表示目标数组类型
     * @param value   对象值，表示待规范化的运行时值；可以为 <code>null</code>
     * @return 与 <code>rawType</code> 匹配的数组值，或该数组类型的默认值
     */
    private static Object coerceArrayValue(Class<?> rawType, Object value) {
        Class<?> componentType = rawType.getComponentType();
        if (rawType.isInstance(value)) {
            if (value instanceof Object[] array) {
                return array.clone();
            }
            return value;
        }
        if (componentType == String.class) {
            if (value instanceof String[] strings) {
                return strings.clone();
            }
            if (value instanceof String string) {
                return string.isEmpty() ? new String[0] : new String[]{string};
            }
            return new String[0];
        }
        return defaultValueFor(TypeHandle.create(rawType.getName()));
    }

    /**
     * 将传入值规范化为指定枚举类型。
     *
     * <p>字符串会按枚举常量名进行大小写不敏感匹配，并支持
     * <code>StringRepresentable</code> 的序列化名称。数值会按枚举常量下标读取。
     * 无法匹配时返回第一个枚举常量，确保保存值仍然属于目标枚举类型。
     *
     * @param rawType 类对象，表示目标枚举类型
     * @param value   对象值，表示待规范化的运行时值；可以为 <code>null</code>
     * @return 与 <code>rawType</code> 匹配的枚举常量；枚举没有常量时返回 <code>null</code>
     */
    private static Object coerceEnumValue(Class<?> rawType, Object value) {
        if (rawType.isInstance(value)) {
            return value;
        }
        Object[] constants = rawType.getEnumConstants();
        if (constants == null || constants.length == 0) {
            return null;
        }
        if (value instanceof String string) {
            String trimmed = string.trim();
            for (Object constant : constants) {
                Enum<?> enumValue = (Enum<?>) constant;
                if (enumValue.name().equalsIgnoreCase(trimmed)) {
                    return enumValue;
                }
                if (constant instanceof StringRepresentable representable
                        && representable.getSerializedName().equals(trimmed)) {
                    return enumValue;
                }
            }
        }
        if (value instanceof Number number) {
            int index = number.intValue();
            if (index >= 0 && index < constants.length) {
                return constants[index];
            }
        }
        return constants[0];
    }

    /**
     * 将传入值规范化为指定 LDLib2 对象类型。
     *
     * <p>此方法通过 <code>TypeHandle.resolve()</code> 获取原始类，并在值已经匹配目标类型时保留
     * 原类型。对已知可变对象会返回副本，避免保存后的变量值继续共享外部可变状态；数组和枚举
     * 使用对应的专用转换规则。无法确认或无法安全转换的对象类型会回退到 LDLib2 默认值。
     *
     * @param typeHandle LDLib2 类型句柄，表示要写入的目标对象类型
     * @param value      对象值，表示待规范化的运行时值；可以为 <code>null</code>
     * @return 可写入对应对象类型常量的值，或该类型的默认值
     */
    private static Object coerceObjectValue(TypeHandle typeHandle, Object value) {
        Type type = typeHandle.resolve();
        if (!(type instanceof Class<?> rawType)) {
            return value;
        }
        if (rawType.isInstance(value)) {
            return switch (value) {
                case ItemStack stack -> stack.copy();
                case DisplayIcon icon -> icon.copy();
                case String[] strings -> strings.clone();
                default -> value;
            };
        }
        if (rawType.isArray()) {
            return coerceArrayValue(rawType, value);
        }
        if (rawType.isEnum()) {
            return coerceEnumValue(rawType, value);
        }
        return defaultValueFor(typeHandle);
    }
}
