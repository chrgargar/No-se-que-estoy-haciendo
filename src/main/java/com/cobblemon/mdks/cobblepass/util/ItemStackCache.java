package com.cobblemon.mdks.cobblepass.util;

import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.HashMap;
import java.util.Map;

/**
 * Caché de ItemStacks pre-parseados para optimizar rendimiento.
 * Evita llamadas repetidas a TagParser.parseTag() para items comunes.
 */
public class ItemStackCache {
    private static final Map<String, CompoundTag> TAG_CACHE = new HashMap<>();
    private static RegistryAccess cachedRegistryAccess;

    // Pre-define tags comunes
    static {
        TAG_CACHE.put("poke_ball", parseTagSafe("{id:\"cobblemon:poke_ball\",Count:1}"));
        TAG_CACHE.put("great_ball", parseTagSafe("{id:\"cobblemon:great_ball\",Count:1}"));
        TAG_CACHE.put("ultra_ball", parseTagSafe("{id:\"cobblemon:ultra_ball\",Count:1}"));
        TAG_CACHE.put("master_ball", parseTagSafe("{id:\"cobblemon:master_ball\",Count:1}"));
        TAG_CACHE.put("luxury_ball", parseTagSafe("{id:\"cobblemon:luxury_ball\",Count:1}"));
        TAG_CACHE.put("citrine_ball", parseTagSafe("{id:\"cobblemon:citrine_ball\",Count:1,components:{\"minecraft:enchantment_glint_override\":true}}"));
    }

    /**
     * Parsea un tag de forma segura, retornando null si falla
     */
    private static CompoundTag parseTagSafe(String tag) {
        try {
            return TagParser.parseTag(tag);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Obtiene un ItemStack cacheado o lo crea si no existe
     * @param key Clave del item en caché (ej: "poke_ball")
     * @param registryAccess Registry access para parsear items
     * @param fallback Item de respaldo si falla
     * @return ItemStack cacheado o fallback
     */
    public static ItemStack getCachedItem(String key, RegistryAccess registryAccess, ItemStack fallback) {
        cachedRegistryAccess = registryAccess;
        CompoundTag tag = TAG_CACHE.get(key);

        if (tag != null) {
            try {
                return ItemStack.parse(registryAccess, tag).orElse(fallback);
            } catch (Exception e) {
                return fallback;
            }
        }

        return fallback;
    }

    /**
     * Obtiene un CompoundTag cacheado
     * @param key Clave del tag en caché
     * @return CompoundTag cacheado o null
     */
    public static CompoundTag getCachedTag(String key) {
        return TAG_CACHE.get(key);
    }

    /**
     * Agrega un nuevo tag al caché
     * @param key Clave única para el tag
     * @param tagString String del tag NBT
     */
    public static void cacheTag(String key, String tagString) {
        CompoundTag tag = parseTagSafe(tagString);
        if (tag != null) {
            TAG_CACHE.put(key, tag);
        }
    }

    /**
     * Limpia el caché (útil para reloads)
     */
    public static void clearCache() {
        TAG_CACHE.clear();
        // Re-cargar tags predefinidos
        TAG_CACHE.put("poke_ball", parseTagSafe("{id:\"cobblemon:poke_ball\",Count:1}"));
        TAG_CACHE.put("luxury_ball", parseTagSafe("{id:\"cobblemon:luxury_ball\",Count:1}"));
        TAG_CACHE.put("citrine_ball", parseTagSafe("{id:\"cobblemon:citrine_ball\",Count:1,components:{\"minecraft:enchantment_glint_override\":true}}"));
    }
}
