package com.cobblemon.mdks.cobblepass.data;

import com.cobblemon.mdks.cobblepass.CobblePass;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class Reward {
    private final RewardType type;
    private final JsonObject data;
    private final String command;
    private final List<String> customLore;
    private final String customTitle;

    public Reward(RewardType type, JsonObject data, String command, List<String> customLore, String customTitle) {
        this.type = type;
        this.data = data;
        this.command = command;
        this.customLore = customLore;
        this.customTitle = customTitle;
    }

    public RewardType getType() {
        return type;
    }

    public JsonObject getData() {
        return data;
    }

    public String getCommand() {
        return command;
    }

    public List<String> getCustomLore() {
        return customLore;
    }

    public boolean hasCustomLore() {
        return customLore != null && !customLore.isEmpty();
    }

    public String getCustomTitle() {
        return customTitle;
    }

    public boolean hasCustomTitle() {
        return customTitle != null && !customTitle.isEmpty();
    }

    public ItemStack getItemStack(RegistryAccess registryAccess) {
        if (type != RewardType.ITEM) {
            return ItemStack.EMPTY;
        }

        try {
            CompoundTag tag = TagParser.parseTag(data.toString());
            ItemStack stack = ItemStack.parse(registryAccess, tag).orElse(ItemStack.EMPTY);
            if (tag.contains("Count")) {
                stack.setCount(tag.getInt("Count"));
            }
            return stack;
        } catch (Exception e) {
            return ItemStack.EMPTY;
        }
    }

    public void grant(ServerPlayer player) {
        switch (type) {
            case ITEM:
                ItemStack item = getItemStack(player.level().registryAccess());
                if (!item.isEmpty()) {
                    player.getInventory().add(item);
                }
                break;
            case POKEMON:
                if (data != null && !data.isEmpty()) {
                    try {
                        // Parse Pokemon data
                        JsonObject pokemonData = data;
                        String species = pokemonData.get("species").getAsString();
                        
                        // Build command with attributes
                        StringBuilder cmd = new StringBuilder();
                        cmd.append("givepokemonother ").append(player.getName().getString()).append(" ").append(species);
                        
                        if (pokemonData.has("shiny") && pokemonData.get("shiny").getAsBoolean()) {
                            cmd.append(" shiny");
                        }
                        if (pokemonData.has("level")) {
                            cmd.append(" level=").append(pokemonData.get("level").getAsInt());
                        }
                        if (pokemonData.has("ability")) {
                            cmd.append(" ability=").append(pokemonData.get("ability").getAsString());
                        }
                        
                        // Execute command as server
                        CommandSourceStack source = player.getServer().createCommandSourceStack();
                        player.getServer().getCommands().performPrefixedCommand(source, cmd.toString());
                    } catch (Exception e) {
                        CobblePass.LOGGER.error("Failed to grant Pokemon reward: " + e.getMessage());
                    }
                }
                break;
            case COMMAND:
                if (command != null && !command.isEmpty()) {
                    // Replace placeholders in command
                    String finalCommand = command
                        .replace("%player%", player.getName().getString())
                        .replace("%uuid%", player.getUUID().toString());
                    
                    // Always execute commands as server to ensure proper permissions
                    CommandSourceStack source = player.getServer().createCommandSourceStack();
                    player.getServer().getCommands().performPrefixedCommand(source, finalCommand);
                }
                break;
        }
    }

    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        json.addProperty("type", type.name());
        json.add("data", data);
        // Only include command for COMMAND type rewards
        if (type == RewardType.COMMAND && command != null) {
            json.addProperty("command", command);
        }
        // Include custom lore if present
        if (customLore != null && !customLore.isEmpty()) {
            JsonArray loreArray = new JsonArray();
            for (String line : customLore) {
                loreArray.add(line);
            }
            json.add("lore", loreArray);
        }
        // Include custom title if present
        if (customTitle != null && !customTitle.isEmpty()) {
            json.addProperty("title", customTitle);
        }
        return json;
    }

    public static Reward fromJson(JsonObject json) {
        RewardType type = RewardType.fromString(json.get("type").getAsString());
        JsonObject data = json.get("data").getAsJsonObject();
        String command = json.has("command") ? json.get("command").getAsString() : null;

        // Parse custom lore if present
        List<String> customLore = null;
        if (json.has("lore")) {
            customLore = new ArrayList<>();
            JsonArray loreArray = json.getAsJsonArray("lore");
            for (JsonElement element : loreArray) {
                customLore.add(element.getAsString());
            }
        }

        // Parse custom title if present
        String customTitle = json.has("title") ? json.get("title").getAsString() : null;

        return new Reward(type, data, command, customLore, customTitle);
    }

    // Factory methods for different reward types
    public static Reward item(JsonObject nbtData) {
        return new Reward(RewardType.ITEM, nbtData, null, null, null);
    }

    public static Reward item(JsonObject nbtData, String title, List<String> lore) {
        return new Reward(RewardType.ITEM, nbtData, null, lore, title);
    }

    public static Reward pokemon(JsonObject pokemonData) {
        return new Reward(RewardType.POKEMON, pokemonData, null, null, null);
    }

    public static Reward pokemon(JsonObject pokemonData, String title, List<String> lore) {
        return new Reward(RewardType.POKEMON, pokemonData, null, lore, title);
    }

    public static Reward command(String commandData, String displayId, String displayName) {
        JsonObject data = new JsonObject();
        data.addProperty("id", displayId); // Item to show in UI
        data.addProperty("display_name", displayName); // Custom name to show in UI
        return new Reward(RewardType.COMMAND, data, commandData, null, null);
    }

    public static Reward command(String commandData, String displayId, String displayName, String title, List<String> lore) {
        JsonObject data = new JsonObject();
        data.addProperty("id", displayId); // Item to show in UI
        data.addProperty("display_name", displayName); // Custom name to show in UI
        return new Reward(RewardType.COMMAND, data, commandData, lore, title);
    }
}
