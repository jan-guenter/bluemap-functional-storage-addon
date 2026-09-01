/*
 * SPDX-License-Identifier: MIT
 *
 * Reflection-only registered-default-state resolution adapts the verified
 * boundary from the owner's MIT BlueMap Glassential add-on.
 */

package io.github.janguenter.bluemap.functionalstorage.adapter.bluemap523;

import de.bluecolored.bluemap.core.util.Key;
import de.bluecolored.bluemap.core.world.BlockState;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/** Resolves a registered Item to its BlockItem block's exact default state. */
final class BlockItemDefaultStateResolver {

    private static final String BUILT_IN_REGISTRIES =
            "net.minecraft.core.registries.BuiltInRegistries";
    private static final String REGISTRY = "net.minecraft.core.Registry";
    private static final String RESOURCE_LOCATION =
            "net.minecraft.resources.ResourceLocation";
    private static final String BLOCK_ITEM = "net.minecraft.world.item.BlockItem";
    private static final String BLOCK = "net.minecraft.world.level.block.Block";
    private static final String STATE_HOLDER =
            "net.minecraft.world.level.block.state.StateHolder";
    private static final String PROPERTY =
            "net.minecraft.world.level.block.state.properties.Property";

    private final Object itemRegistry;
    private final Object blockRegistry;
    private final Class<?> blockItemType;
    private final Method parseResourceLocation;
    private final Method itemContainsKey;
    private final Method getItem;
    private final Method getItemKey;
    private final Method getBlock;
    private final Method getBlockKey;
    private final Method blockFromItem;
    private final Method defaultBlockState;
    private final Method getValues;
    private final Method getPropertyName;
    private final Method getPropertyValueName;
    private final Map<String, Optional<BlockState>> cache = new ConcurrentHashMap<>();

    private BlockItemDefaultStateResolver() throws ReflectiveOperationException {
        Class<?> resourceLocationType = Class.forName(RESOURCE_LOCATION);
        Class<?> registryType = Class.forName(REGISTRY);
        blockItemType = Class.forName(BLOCK_ITEM);
        Class<?> blockType = Class.forName(BLOCK);
        Class<?> stateHolderType = Class.forName(STATE_HOLDER);
        Class<?> propertyType = Class.forName(PROPERTY);
        Class<?> registriesType = Class.forName(BUILT_IN_REGISTRIES);

        itemRegistry = registriesType.getField("ITEM").get(null);
        blockRegistry = registriesType.getField("BLOCK").get(null);
        parseResourceLocation = resourceLocationType.getMethod("parse", String.class);
        itemContainsKey = registryType.getMethod("containsKey", resourceLocationType);
        getItem = registryType.getMethod("get", resourceLocationType);
        getItemKey = registryType.getMethod("getKey", Object.class);
        getBlock = registryType.getMethod("get", resourceLocationType);
        getBlockKey = registryType.getMethod("getKey", Object.class);
        blockFromItem = blockItemType.getMethod("getBlock");
        defaultBlockState = blockType.getMethod("defaultBlockState");
        getValues = stateHolderType.getMethod("getValues");
        getPropertyName = propertyType.getMethod("getName");
        getPropertyValueName = propertyType.getMethod("getName", Comparable.class);
    }

    static BlockItemDefaultStateResolver createVerified() {
        try {
            BlockItemDefaultStateResolver resolver = new BlockItemDefaultStateResolver();
            BlockState oak = resolver.resolve("minecraft:oak_planks");
            BlockState log = resolver.resolve("minecraft:oak_log");
            BlockState grass = resolver.resolve("minecraft:grass_block");
            return exact(oak, "minecraft:oak_planks", Map.of())
                    && exact(log, "minecraft:oak_log", Map.of("axis", "y"))
                    && exact(grass, "minecraft:grass_block", Map.of("snowy", "false"))
                    ? resolver : null;
        } catch (ReflectiveOperationException | LinkageError | RuntimeException exception) {
            return null;
        }
    }

    /** Returns null for an unknown/non-BlockItem; reflective contract failures throw. */
    BlockState resolve(String itemId) {
        return cache.computeIfAbsent(itemId, this::resolveUncached).orElse(null);
    }

    private Optional<BlockState> resolveUncached(String itemId) {
        try {
            Object location = invoke(parseResourceLocation, null, itemId);
            if (!Boolean.TRUE.equals(invoke(itemContainsKey, itemRegistry, location))) {
                return Optional.empty();
            }
            Object item = invoke(getItem, itemRegistry, location);
            Object registeredItemKey = invoke(getItemKey, itemRegistry, item);
            if (item == null || registeredItemKey == null
                    || !itemId.equals(registeredItemKey.toString())
                    || !blockItemType.isInstance(item)) {
                return Optional.empty();
            }
            Object block = invoke(blockFromItem, item);
            Object blockKey = invoke(getBlockKey, blockRegistry, block);
            if (block == null || blockKey == null) {
                return Optional.empty();
            }
            String blockId = blockKey.toString();
            Object blockLocation = invoke(parseResourceLocation, null, blockId);
            Object registeredBlock = invoke(getBlock, blockRegistry, blockLocation);
            if (registeredBlock != block) {
                throw new IllegalStateException("BlockItem block changed registry identity");
            }
            Object defaultState = invoke(defaultBlockState, block);
            Object rawValues = invoke(getValues, defaultState);
            if (!(rawValues instanceof Map<?, ?> values)) {
                throw new IllegalStateException("default block state values changed type");
            }
            Map<String, String> properties = new LinkedHashMap<>();
            for (Map.Entry<?, ?> entry : values.entrySet()) {
                Object value = entry.getValue();
                if (!(value instanceof Comparable<?> comparable)) {
                    throw new IllegalStateException("default property is not comparable");
                }
                Object name = invoke(getPropertyName, entry.getKey());
                Object serialized = invoke(getPropertyValueName, entry.getKey(), comparable);
                if (!(name instanceof String propertyName)
                        || !(serialized instanceof String propertyValue)
                        || properties.put(propertyName, propertyValue) != null) {
                    throw new IllegalStateException("default property encoding changed");
                }
            }
            return Optional.of(new BlockState(Key.parse(blockId), Map.copyOf(properties)));
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Minecraft BlockItem reflection failed", exception);
        }
    }

    private static boolean exact(
            BlockState state,
            String id,
            Map<String, String> properties
    ) {
        return state != null && id.equals(state.getId().getFormatted())
                && properties.equals(state.getProperties());
    }

    private static Object invoke(Method method, Object target, Object... arguments)
            throws ReflectiveOperationException {
        try {
            return method.invoke(target, arguments);
        } catch (InvocationTargetException exception) {
            Throwable cause = exception.getCause();
            if (cause instanceof Error error) {
                throw error;
            }
            if (cause instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            throw new ReflectiveOperationException("reflected Minecraft call failed", cause);
        }
    }
}
