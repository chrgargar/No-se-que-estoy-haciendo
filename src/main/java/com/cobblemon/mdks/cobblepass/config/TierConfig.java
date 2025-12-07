package com.cobblemon.mdks.cobblepass.config;

import com.cobblemon.mdks.cobblepass.CobblePass;
import com.cobblemon.mdks.cobblepass.battlepass.BattlePassTier;
import com.cobblemon.mdks.cobblepass.data.Reward;
import com.cobblemon.mdks.cobblepass.data.RewardType;
import com.cobblemon.mdks.cobblepass.util.Constants;
import com.cobblemon.mdks.cobblepass.util.Utils;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.world.item.Items;

import java.util.HashMap;
import java.util.Map;

public class TierConfig {
    private static final String TIERS_FILE = Constants.TIERS_FILE;
    private static final String TIERS_PATH = Constants.CONFIG_DIR;
    
    private final Map<Integer, BattlePassTier> tiers = new HashMap<>();

    public TierConfig() {
        load();
    }

    public void load() {
        String content = Utils.readFileSync(TIERS_PATH, TIERS_FILE);
        if (content == null || content.isEmpty()) {
            generateDefaultTiers();
            save();
            return;
        }

        try {
            JsonObject json = JsonParser.parseString(content).getAsJsonObject();
            loadFromJson(json);
        } catch (Exception e) {
            CobblePass.LOGGER.error("Failed to load tier config", e);
            generateDefaultTiers();
            save();
        }
    }

    private void loadFromJson(JsonObject json) {
        tiers.clear();
        
        // Load templates if they exist
        Map<String, JsonObject> templates = new HashMap<>();
        if (json.has("templates")) {
            JsonObject templatesObj = json.getAsJsonObject("templates");
            for (Map.Entry<String, JsonElement> entry : templatesObj.entrySet()) {
                templates.put(entry.getKey(), entry.getValue().getAsJsonObject());
            }
        }

        JsonArray tiersArray = json.getAsJsonArray("tiers");
        
        for (JsonElement element : tiersArray) {
            JsonObject tierObj = element.getAsJsonObject();
            int level = tierObj.get("level").getAsInt();
            
            // Load rewards
            Reward freeReward = loadReward(tierObj, "freeReward", templates);
            Reward premiumReward = loadReward(tierObj, "premiumReward", templates);
            
            tiers.put(level, new BattlePassTier(level, freeReward, premiumReward));
        }
    }

    private Reward loadReward(JsonObject tierObj, String rewardKey, Map<String, JsonObject> templates) {
        if (!tierObj.has(rewardKey)) {
            return null;
        }

        JsonElement rewardElement = tierObj.get(rewardKey);
        
        // Handle template references
        if (rewardElement.isJsonPrimitive() && rewardElement.getAsString().startsWith("@")) {
            String templateName = rewardElement.getAsString().substring(1);
            if (templates.containsKey(templateName)) {
                return Reward.fromJson(templates.get(templateName));
            }
            return null;
        }

        // Handle direct reward definitions
        if (rewardElement.isJsonObject()) {
            return Reward.fromJson(rewardElement.getAsJsonObject());
        }

        return null;
    }

    private void generateDefaultTiers() {
        tiers.clear();

        // Generate 100 tiers with varied rewards
        for (int i = 1; i <= 100; i++) {
            Reward freeReward = null;
            Reward premiumReward = null;

            // Every tier gets a free reward
            if (i % 10 == 0) {
                // Every 10 levels - Pokemon reward
                JsonObject pokemonData = new JsonObject();
                pokemonData.addProperty("species", "eevee");
                pokemonData.addProperty("level", i);
                pokemonData.addProperty("shiny", false);
                freeReward = Reward.pokemon(pokemonData,
                    "§a🦊 Pokémon Nivel " + i,
                    java.util.Arrays.asList(
                        "§aRecompensa Gratis",
                        "§7Eevee Nivel " + i,
                        "§8¡Recompensa especial!"
                    )
                );
            } else if (i % 5 == 0) {
                // Every 5 levels - More Poké Balls
                JsonObject itemData = new JsonObject();
                itemData.addProperty("id", "cobblemon:great_ball");
                itemData.addProperty("Count", 10);
                freeReward = Reward.item(itemData,
                    "§a⚪ Super Ball x10",
                    java.util.Arrays.asList(
                        "§aRecompensa Gratis",
                        "§710x Super Ball",
                        "§8Nivel " + i
                    )
                );
            } else {
                // Regular levels - Basic Poké Balls
                JsonObject itemData = new JsonObject();
                itemData.addProperty("id", "cobblemon:poke_ball");
                itemData.addProperty("Count", 5);
                freeReward = Reward.item(itemData,
                    "§a⚪ Poké Ball x5",
                    java.util.Arrays.asList(
                        "§aRecompensa Gratis",
                        "§75x Poké Ball",
                        "§8Nivel " + i
                    )
                );
            }

            // Premium rewards for certain levels
            if (i % 20 == 0) {
                // Every 20 levels - Shiny Pokemon
                JsonObject pokemonData = new JsonObject();
                pokemonData.addProperty("species", "pikachu");
                pokemonData.addProperty("level", i);
                pokemonData.addProperty("shiny", true);
                premiumReward = Reward.pokemon(pokemonData,
                    "§6✦ Pokémon Shiny",
                    java.util.Arrays.asList(
                        "§6Recompensa Premium",
                        "§7Pikachu Shiny",
                        "§7Nivel " + i,
                        "§8¡Recompensa exclusiva!"
                    )
                );
            } else if (i % 10 == 0) {
                // Every 10 levels - Master Ball
                JsonObject itemData = new JsonObject();
                itemData.addProperty("id", "cobblemon:master_ball");
                itemData.addProperty("Count", 1);
                premiumReward = Reward.item(itemData,
                    "§6⭐ Master Ball",
                    java.util.Arrays.asList(
                        "§6Recompensa Premium",
                        "§71x Master Ball",
                        "§8Nivel " + i
                    )
                );
            } else if (i % 5 == 0) {
                // Every 5 levels - Ultra Ball
                JsonObject itemData = new JsonObject();
                itemData.addProperty("id", "cobblemon:ultra_ball");
                itemData.addProperty("Count", 5);
                premiumReward = Reward.item(itemData,
                    "§6⭐ Ultra Ball x5",
                    java.util.Arrays.asList(
                        "§6Recompensa Premium",
                        "§75x Ultra Ball",
                        "§8Nivel " + i
                    )
                );
            }

            tiers.put(i, new BattlePassTier(i, freeReward, premiumReward));
        }
    }

    public void save() {
        JsonObject json = new JsonObject();
        JsonArray tiersArray = new JsonArray();
        
        for (BattlePassTier tier : tiers.values()) {
            JsonObject tierObj = new JsonObject();
            tierObj.addProperty("level", tier.getLevel());
            
            if (tier.getFreeReward() != null) {
                tierObj.add("freeReward", tier.getFreeReward().toJson());
            }
            
            if (tier.getPremiumReward() != null) {
                tierObj.add("premiumReward", tier.getPremiumReward().toJson());
            }
            
            tiersArray.add(tierObj);
        }
        
        json.add("tiers", tiersArray);
        Utils.writeFileSync(TIERS_PATH, TIERS_FILE, Utils.newGson().toJson(json));
    }

    private String extractItemId(String nbtString) {
        // Extract item ID from NBT string format {id:"minecraft:item",Count:1}
        try {
            int startIndex = nbtString.indexOf("\"") + 1;
            int endIndex = nbtString.indexOf("\"", startIndex);
            return nbtString.substring(startIndex, endIndex);
        } catch (Exception e) {
            return "minecraft:stone"; // Fallback item
        }
    }

    public BattlePassTier getTier(int level) {
        return tiers.get(level);
    }

    public Map<Integer, BattlePassTier> getAllTiers() {
        return new HashMap<>(tiers);
    }
}
