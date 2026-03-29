package com.viscriptquests.util;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CodecUtil {

    /**
     * 为任意类型的 Map 创建 Codec
     * 将 Map 序列化为 CompoundTag，内部使用 List 存储 key-value 对
     *
     * @param keyCodec   键的 Codec
     * @param valueCodec 值的 Codec
     * @param provider   HolderLookup.Provider
     * @param <K>        键类型
     * @param <V>        值类型
     * @return Map 的 Codec
     */
    public static <K, V> Codec<Map<K, V>> createMapCodec(Codec<K> keyCodec, Codec<V> valueCodec, HolderLookup.Provider provider) {
        return new Codec<>() {
            @Override
            public <T> DataResult<Pair<Map<K, V>, T>> decode(DynamicOps<T> ops, T input) {
                if (!(ops instanceof NbtOps)) {
                    return DataResult.error(() -> "Only NBT operations are supported");
                }
                Tag tag = (Tag) input;
                if (!(tag instanceof CompoundTag compoundTag)) {
                    return DataResult.error(() -> "Expected CompoundTag");
                }

                try {
                    Map<K, V> map = deserializeMap(compoundTag, keyCodec, valueCodec, provider);
                    return DataResult.success(Pair.of(map, input));
                } catch (Exception e) {
                    return DataResult.error(() -> "Failed to deserialize map: " + e.getMessage());
                }
            }

            @Override
            public <T> DataResult<T> encode(Map<K, V> map, DynamicOps<T> ops, T prefix) {
                if (!(ops instanceof NbtOps)) {
                    return DataResult.error(() -> "Only NBT operations are supported");
                }

                try {
                    CompoundTag compoundTag = serializeMap(map, keyCodec, valueCodec, provider);
                    return DataResult.success((T) compoundTag);
                } catch (Exception e) {
                    return DataResult.error(() -> "Failed to serialize map: " + e.getMessage());
                }
            }
        };
    }

    public static <T> T deserializeNBT(Codec<T> codec, Tag tag, HolderLookup.Provider provider) {
        return codec.decode(provider.createSerializationContext(NbtOps.INSTANCE), tag).getOrThrow().getFirst();
    }

    public static <T> Tag serializeNBT(Codec<T> codec, T object, HolderLookup.Provider provider) {
        return codec.encodeStart(provider.createSerializationContext(NbtOps.INSTANCE), object).result().orElse(new CompoundTag());
    }

    // ==================== List 序列化工具方法 ====================

    /**
     * 序列化 List 为 CompoundTag（用于 @ReadOnlyManaged）
     *
     * @param list     要序列化的列表
     * @param codec    元素的 Codec
     * @param provider RegistryAccess
     * @param <T>      元素类型
     * @return 序列化后的 CompoundTag，包含 "list" 字段
     */
    public static <T> CompoundTag serializeList(List<T> list, Codec<T> codec, HolderLookup.Provider provider) {
        CompoundTag tag = new CompoundTag();
        ListTag listTag = new ListTag();

        for (T item : list) {
            Tag itemTag = serializeNBT(codec, item, provider);
            listTag.add(itemTag);
        }

        tag.put("list", listTag);
        return tag;
    }

    /**
     * 反序列化 CompoundTag 为 List（用于 @ReadOnlyManaged）
     *
     * @param tag      包含 "list" 字段的 CompoundTag
     * @param codec    元素的 Codec
     * @param provider RegistryAccess
     * @param <T>      元素类型
     * @return 反序列化后的列表
     */
    public static <T> List<T> deserializeList(CompoundTag tag, Codec<T> codec, HolderLookup.Provider provider) {
        ListTag listTag = tag.getList("list", Tag.TAG_COMPOUND);
        List<T> list = new ArrayList<>();

        for (Tag itemTag : listTag) {
            T item = deserializeNBT(codec, itemTag, provider);
            list.add(item);
        }

        return list;
    }

    // ==================== Map 序列化工具方法 ====================

    /**
     * 序列化 Map 为 CompoundTag（用于 @ReadOnlyManaged）
     *
     * @param map        要序列化的 Map
     * @param keyCodec   键的 Codec
     * @param valueCodec 值的 Codec
     * @param provider   RegistryAccess
     * @param <K>        键类型
     * @param <V>        值类型
     * @return 序列化后的 CompoundTag，包含 "map" 字段
     */
    public static <K, V> CompoundTag serializeMap(Map<K, V> map, Codec<K> keyCodec, Codec<V> valueCodec, HolderLookup.Provider provider) {
        CompoundTag tag = new CompoundTag();
        ListTag listTag = new ListTag();

        for (Map.Entry<K, V> entry : map.entrySet()) {
            CompoundTag entryTag = new CompoundTag();
            entryTag.put("key", serializeNBT(keyCodec, entry.getKey(), provider));
            entryTag.put("value", serializeNBT(valueCodec, entry.getValue(), provider));
            listTag.add(entryTag);
        }

        tag.put("map", listTag);
        return tag;
    }

    /**
     * 反序列化 CompoundTag 为 Map（用于 @ReadOnlyManaged）
     *
     * @param tag        包含 "map" 字段的 CompoundTag
     * @param keyCodec   键的 Codec
     * @param valueCodec 值的 Codec
     * @param provider   RegistryAccess
     * @param <K>        键类型
     * @param <V>        值类型
     * @return 反序列化后的 Map（使用 HashMap）
     */
    public static <K, V> Map<K, V> deserializeMap(CompoundTag tag, Codec<K> keyCodec, Codec<V> valueCodec, HolderLookup.Provider provider) {
        ListTag listTag = tag.getList("map", Tag.TAG_COMPOUND);
        Map<K, V> map = new HashMap<>();

        for (Tag entryTag : listTag) {
            CompoundTag compound = (CompoundTag) entryTag;
            K key = deserializeNBT(keyCodec, compound.get("key"), provider);
            V value = deserializeNBT(valueCodec, compound.get("value"), provider);
            map.put(key, value);
        }

        return map;
    }
}
