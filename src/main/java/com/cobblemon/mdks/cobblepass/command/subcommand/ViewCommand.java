package com.cobblemon.mdks.cobblepass.command.subcommand;

import com.google.gson.JsonObject;
import ca.landonjw.gooeylibs2.api.UIManager;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import com.cobblemon.mdks.cobblepass.data.Reward;
import ca.landonjw.gooeylibs2.api.button.Button;
import ca.landonjw.gooeylibs2.api.button.GooeyButton;
import ca.landonjw.gooeylibs2.api.button.PlaceholderButton;
import ca.landonjw.gooeylibs2.api.button.linked.LinkedPageButton;
import ca.landonjw.gooeylibs2.api.button.linked.LinkType;
import ca.landonjw.gooeylibs2.api.helpers.PaginationHelper;
import ca.landonjw.gooeylibs2.api.page.LinkedPage;
import ca.landonjw.gooeylibs2.api.page.Page;
import ca.landonjw.gooeylibs2.api.template.types.ChestTemplate;
import com.cobblemon.mdks.cobblepass.CobblePass;
import com.cobblemon.mdks.cobblepass.battlepass.BattlePassTier;
import com.cobblemon.mdks.cobblepass.battlepass.PlayerBattlePass;
import com.cobblemon.mdks.cobblepass.util.Constants;
import com.cobblemon.mdks.cobblepass.util.Subcommand;
import com.cobblemon.mdks.cobblepass.util.Utils;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.tree.CommandNode;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.core.component.DataComponents;
import net.minecraft.util.Unit;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class ViewCommand extends Subcommand {
    public ViewCommand() {
        super("§9Uso: §3/battlepass view");
    }

    @Override
    public CommandNode<CommandSourceStack> build() {
        return Commands.literal("view")
            .executes(this::run)
            .build();
    }

    @Override
    public int run(CommandContext<CommandSourceStack> context) {
        if (!context.getSource().isPlayer()) {
            context.getSource().sendSystemMessage(
                Component.literal(Constants.ERROR_PREFIX + "¡Este comando debe ser ejecutado por un jugador!")
            );
            return 1;
        }

        ServerPlayer player = context.getSource().getPlayer();
        showBattlePassInfo(player);
        return 1;
    }

    private static String formatTimeRemaining(long milliseconds) {
        long seconds = milliseconds / 1000;
        long days = seconds / (24 * 3600);
        seconds %= (24 * 3600);
        long hours = seconds / 3600;
        seconds %= 3600;
        long minutes = seconds / 60;

        return String.format("%d días, %d horas, %d minutos", days, hours, minutes);
    }

    private static List<Component> getRewardLore(BattlePassTier tier, boolean isPremium) {
        List<Component> lore = new ArrayList<>();
        if (isPremium) {
            lore.add(Component.literal("§6Recompensa Premium"));
        } else {
            lore.add(Component.literal("§aRecompensa Gratis"));
        }

        Reward reward = isPremium ? tier.getPremiumReward() : tier.getFreeReward();
        if (reward != null) {
            JsonObject data = reward.getData();
            switch (reward.getType()) {
                case ITEM:
                    if (data != null) {
                        String itemId = data.get("id").getAsString();
                        int count = data.has("Count") ? data.get("Count").getAsInt() : 1;
                        // Extract item name from ID (e.g., "minecraft:stone" -> "Stone")
                        String[] parts = itemId.split(":");
                        String itemName = parts[parts.length - 1];
                        itemName = itemName.substring(0, 1).toUpperCase() + itemName.substring(1);
                        // Add mod name to lore if not minecraft
                        if (!parts[0].equals("minecraft")) {
                            String modName = parts[0].substring(0, 1).toUpperCase() + parts[0].substring(1);
                            lore.add(Component.literal("§8" + modName + " Item"));
                        }
                        lore.add(Component.literal("§7" + count + "x " + itemName));
                    } else {
                        lore.add(Component.literal("§7Item"));
                    }
                    break;
                case POKEMON:
                    lore.add(Component.literal("§7Pokemon"));
                    if (data != null) {
                        if (data.has("species")) {
                            lore.add(Component.literal(data.get("species").getAsString()));
                        }
                        if (data.has("level")) {
                            lore.add(Component.literal("§7Level: §f" + data.get("level").getAsInt()));
                        }
                        if (data.has("shiny") && data.get("shiny").getAsBoolean()) {
                            lore.add(Component.literal("§6✦ Shiny"));
                        }
                    }
                    break;
                case COMMAND:
                    if (data != null) {
                        if (data.has("display_name")) {
                            // Use custom display name if provided
                            lore.add(Component.literal("§7" + data.get("display_name").getAsString()));
                        } else if (data.has("id")) {
                            // Fall back to item ID if no display name
                            String itemId = data.get("id").getAsString();
                            String[] parts = itemId.split(":");
                            String itemName = parts[parts.length - 1];
                            itemName = itemName.substring(0, 1).toUpperCase() + itemName.substring(1);
                            lore.add(Component.literal("§7" + itemName));
                        }
                    }
                    break;
            }
        }
        return lore;
    }

    private static List<Component> getPremiumRewardLore(BattlePassTier tier) {
        return getRewardLore(tier, true);
    }

    public static void showBattlePassInfo(ServerPlayer player) {
        PlayerBattlePass pass = CobblePass.battlePass.getPlayerPass(player);
        
        // Create info button showing level, XP and time remaining
        int currentXP = pass.getXP();
        int xpForNext = (int)(CobblePass.config.getXpPerLevel() * Math.pow(Constants.XP_MULTIPLIER, pass.getLevel() - 1));
        List<Component> infoLore = new ArrayList<>(Arrays.asList(
            Component.literal(String.format("§3Nivel: §f%d", pass.getLevel())),
            Component.literal(String.format("§3XP: §f%d§7/§f%d", currentXP, xpForNext))
        ));

        if (CobblePass.config.isSeasonActive()) {
            long timeLeft = CobblePass.config.getSeasonEndTime() - System.currentTimeMillis();
            if (timeLeft > 0) {
                infoLore.add(Component.literal("§3Tiempo Restante: §b" + formatTimeRemaining(timeLeft)));
            }
        }

        GooeyButton infoButton = GooeyButton.builder()
            .display(new ItemStack(Items.NETHER_STAR))
            .with(DataComponents.CUSTOM_NAME, Component.literal("§bProgreso del Pase de Batalla"))
            .with(DataComponents.LORE, new ItemLore(infoLore))
            .with(DataComponents.HIDE_ADDITIONAL_TOOLTIP, Unit.INSTANCE)
            .build();

        // Create XP rewards info button
        List<Component> xpInfoLore = new ArrayList<>(Arrays.asList(
            Component.literal("§7Gana XP completando tareas:"),
            Component.literal(""),
            Component.literal("§a✦ Capturar Pokémon: §f+" + CobblePass.config.getCatchXP() + " XP"),
            Component.literal("§6✦ Primera captura: §f+" + CobblePass.config.getFirstCatchXP() + " XP bonus"),
            Component.literal("§c✦ Derrotar Pokémon: §f+" + CobblePass.config.getDefeatXP() + " XP"),
            Component.literal("§e✦ Evolucionar Pokémon: §f+" + CobblePass.config.getEvolveXP() + " XP"),
            Component.literal("§b✦ Subir nivel Pokémon: §f+" + CobblePass.config.getLevelUpXP() + " XP")
        ));

        GooeyButton xpInfoButton = GooeyButton.builder()
            .display(new ItemStack(Items.EXPERIENCE_BOTTLE))
            .with(DataComponents.CUSTOM_NAME, Component.literal("§6⭐ Cómo Ganar XP"))
            .with(DataComponents.LORE, new ItemLore(xpInfoLore))
            .with(DataComponents.HIDE_ADDITIONAL_TOOLTIP, Unit.INSTANCE)
            .build();

        // Create premium status button
        ItemStack premiumDisplay;
        if (pass.isPremium()) {
            // Premium active - show Luxury Ball
            try {
                CompoundTag luxuryTag = TagParser.parseTag("{id:\"cobblemon:luxury_ball\",Count:1}");
                premiumDisplay = ItemStack.parse(player.level().registryAccess(), luxuryTag).orElse(new ItemStack(Items.GOLDEN_APPLE));
            } catch (Exception e) {
                premiumDisplay = new ItemStack(Items.GOLDEN_APPLE);
            }
        } else {
            // Not premium - show regular Poké Ball
            try {
                CompoundTag pokeballTag = TagParser.parseTag("{id:\"cobblemon:poke_ball\",Count:1}");
                premiumDisplay = ItemStack.parse(player.level().registryAccess(), pokeballTag).orElse(new ItemStack(Items.APPLE));
            } catch (Exception e) {
                premiumDisplay = new ItemStack(Items.APPLE);
            }
        }

        List<Component> premiumLore = new ArrayList<>();
        if (pass.isPremium()) {
            if (CobblePass.config.isSeasonActive()) {
                premiumLore.add(Component.literal("§3Temporada " + CobblePass.config.getCurrentSeason()));
            } else {
                premiumLore.add(Component.literal("§cNo hay temporada activa"));
            }
        } else {
            premiumLore.add(Component.literal("§cInactivo"));
            premiumLore.add(Component.literal("§7¡Haz clic para mejorar!"));
        }

        // Create empty background button
        Button background = new PlaceholderButton();

        // Create base template
        ChestTemplate baseTemplate = ChestTemplate.builder(4)
            .fill(background) // Fill entire GUI with background
            .set(0, 4, infoButton)
            .build();

        // Create three rows: free rewards (row 1), status glass (row 2), premium rewards (row 3)
        List<Button> freeRewardButtons = new ArrayList<>();
        List<Button> statusButtons = new ArrayList<>();
        List<Button> premiumRewardButtons = new ArrayList<>();
        Map<Integer, BattlePassTier> tiers = CobblePass.battlePass.getTiers();
        int totalTiers = tiers.size();

        for (int i = 1; i <= totalTiers; i++) {
            final int level = i;
            BattlePassTier tier = tiers.get(level);
            if (tier == null) continue;

            // Create FREE reward button (Poké Ball) - Only if tier has free reward
            Button freeRewardButton;

            if (tier.hasFreeReward()) {
                ItemStack freeDisplayItem;
                try {
                    CompoundTag pokeballTag = TagParser.parseTag("{id:\"cobblemon:poke_ball\",Count:1}");
                    freeDisplayItem = ItemStack.parse(player.level().registryAccess(), pokeballTag).orElse(new ItemStack(Items.STONE));
                } catch (Exception e) {
                    freeDisplayItem = new ItemStack(Items.STONE);
                }

                // Use custom title if available, otherwise use default
                Reward freeReward = tier.getFreeReward();
                String displayTitle = (freeReward != null && freeReward.hasCustomTitle())
                    ? freeReward.getCustomTitle()
                    : "§fNivel " + level;

                // Use custom lore if available, otherwise generate default lore
                List<Component> displayLore = (freeReward != null && freeReward.hasCustomLore())
                    ? freeReward.getCustomLore().stream().map(Component::literal).collect(java.util.stream.Collectors.<Component>toList())
                    : getRewardLore(tier, false);

                freeRewardButton = GooeyButton.builder()
                    .display(freeDisplayItem)
                    .with(DataComponents.CUSTOM_NAME, Component.literal(displayTitle))
                    .with(DataComponents.LORE, new ItemLore(displayLore))
                    .with(DataComponents.HIDE_ADDITIONAL_TOOLTIP, Unit.INSTANCE)
                    .onClick(action -> {
                        if (level > pass.getLevel()) {
                            player.sendSystemMessage(Component.literal(String.format(Constants.MSG_LEVEL_NOT_REACHED, level)));
                            return;
                        }

                        // If premium, claim both rewards
                        if (pass.isPremium()) {
                            boolean claimedFree = CobblePass.battlePass.claimReward(player, level, false);
                            boolean claimedPremium = CobblePass.battlePass.claimReward(player, level, true);
                            if (claimedFree || claimedPremium) {
                                player.sendSystemMessage(Component.literal("§a¡Recompensas reclamadas del nivel " + level + "!"));
                                showBattlePassInfo(player);
                            }
                        } else {
                            // Only claim free reward
                            if (CobblePass.battlePass.claimReward(player, level, false)) {
                                player.sendSystemMessage(Component.literal("§a¡Recompensa gratis reclamada del nivel " + level + "!"));
                                showBattlePassInfo(player);
                            }
                        }
                    })
                    .build();
            } else {
                // If no free reward, use empty placeholder
                freeRewardButton = new PlaceholderButton();
            }

            // Create STATUS indicator glass pane
            ItemStack statusGlass;
            List<Component> statusLore = new ArrayList<>();
            boolean hasPremiumReward = tier.hasPremiumReward();

            if (level > pass.getLevel()) {
                statusGlass = new ItemStack(Items.GRAY_STAINED_GLASS_PANE);
                statusLore.add(Component.literal("§7No Alcanzado"));
            } else if (pass.hasClaimedFreeReward(level) && (!hasPremiumReward || pass.hasClaimedPremiumReward(level))) {
                statusGlass = new ItemStack(Items.ORANGE_STAINED_GLASS_PANE);
                statusLore.add(Component.literal("§6Reclamado"));
            } else {
                statusGlass = new ItemStack(Items.GREEN_STAINED_GLASS_PANE);
                statusLore.add(Component.literal("§aDisponible"));
            }

            Button statusButton = GooeyButton.builder()
                .display(statusGlass)
                .with(DataComponents.CUSTOM_NAME, Component.literal("§3Nivel " + level))
                .with(DataComponents.LORE, new ItemLore(statusLore))
                .with(DataComponents.HIDE_ADDITIONAL_TOOLTIP, Unit.INSTANCE)
                .build();

            // Create PREMIUM reward button (Citrine Ball with enchantment glint)
            ItemStack premiumDisplayItem;
            try {
                CompoundTag citrineTag = TagParser.parseTag("{id:\"cobblemon:citrine_ball\",Count:1,components:{\"minecraft:enchantment_glint_override\":true}}");
                premiumDisplayItem = ItemStack.parse(player.level().registryAccess(), citrineTag).orElse(new ItemStack(Items.DIAMOND));
            } catch (Exception e) {
                premiumDisplayItem = new ItemStack(Items.DIAMOND);
            }

            // Use custom title if available, otherwise use default
            Reward premiumReward = tier.getPremiumReward();
            String premiumDisplayTitle = (premiumReward != null && premiumReward.hasCustomTitle())
                ? premiumReward.getCustomTitle()
                : "§6Nivel " + level;

            // Use custom lore if available, otherwise generate default lore
            List<Component> premiumDisplayLore = (premiumReward != null && premiumReward.hasCustomLore())
                ? premiumReward.getCustomLore().stream().map(Component::literal).collect(java.util.stream.Collectors.<Component>toList())
                : getRewardLore(tier, true);

            Button premiumRewardButton = GooeyButton.builder()
                .display(premiumDisplayItem)
                .with(DataComponents.CUSTOM_NAME, Component.literal(premiumDisplayTitle))
                .with(DataComponents.LORE, new ItemLore(premiumDisplayLore))
                .with(DataComponents.HIDE_ADDITIONAL_TOOLTIP, Unit.INSTANCE)
                .onClick(action -> {
                    if (level > pass.getLevel()) {
                        player.sendSystemMessage(Component.literal(String.format(Constants.MSG_LEVEL_NOT_REACHED, level)));
                        return;
                    }

                    if (!pass.isPremium()) {
                        player.sendSystemMessage(Component.literal(Constants.MSG_NOT_PREMIUM));
                        return;
                    }

                    // If premium, claim both rewards
                    boolean claimedFree = CobblePass.battlePass.claimReward(player, level, false);
                    boolean claimedPremium = CobblePass.battlePass.claimReward(player, level, true);
                    if (claimedFree || claimedPremium) {
                        player.sendSystemMessage(Component.literal("§a¡Recompensas reclamadas del nivel " + level + "!"));
                        showBattlePassInfo(player);
                    }
                })
                .build();

            freeRewardButtons.add(freeRewardButton);
            statusButtons.add(statusButton);
            premiumRewardButtons.add(premiumRewardButton);
        }

        // Create pages with proper button placement
        List<LinkedPage> pages = new ArrayList<>();
        int buttonsPerPage = 7; // Only use columns 1-7 (leaving 0 and 8 empty)
        int totalPages = (int) Math.ceil((double) totalTiers / buttonsPerPage);

        for (int pageNum = 0; pageNum < totalPages; pageNum++) {
            ChestTemplate template = ChestTemplate.builder(5)
                .fill(background)
                .set(0, 0, xpInfoButton)
                .set(0, 4, infoButton)
                .build();

            int startIdx = pageNum * buttonsPerPage;
            int endIdx = Math.min(startIdx + buttonsPerPage, totalTiers);

            // Place FREE reward buttons in row 1 (columns 1-7)
            for (int i = startIdx; i < endIdx; i++) {
                template.set(1, (i - startIdx) + 1, freeRewardButtons.get(i));
            }

            // Place STATUS glass buttons in row 2 (columns 1-7)
            for (int i = startIdx; i < endIdx; i++) {
                template.set(2, (i - startIdx) + 1, statusButtons.get(i));
            }

            // Place PREMIUM reward buttons in row 3 (columns 1-7)
            for (int i = startIdx; i < endIdx; i++) {
                template.set(3, (i - startIdx) + 1, premiumRewardButtons.get(i));
            }

            LinkedPage page = LinkedPage.builder()
                .template(template)
                .title("§6Pase de Batalla")
                .build();

            pages.add(page);
        }

        // Link pages together
        for (int i = 0; i < pages.size(); i++) {
            LinkedPage current = pages.get(i);
            if (i > 0) {
                current.setPrevious(pages.get(i - 1));
            }
            if (i < pages.size() - 1) {
                current.setNext(pages.get(i + 1));
            }

            // Add navigation and close buttons to row 4
            ChestTemplate template = (ChestTemplate) current.getTemplate();

            // Previous button (left corner, position 0)
            if (current.getPrevious() != null) {
                Button prevBtn = LinkedPageButton.builder()
                    .display(new ItemStack(Items.SPECTRAL_ARROW))
                    .with(DataComponents.CUSTOM_NAME, Component.literal("§f← Página Anterior"))
                    .with(DataComponents.HIDE_ADDITIONAL_TOOLTIP, Unit.INSTANCE)
                    .linkType(LinkType.Previous)
                    .build();
                template.set(4, 0, prevBtn);
            }

            // Close button (center, position 4)
            Button closeBtn = GooeyButton.builder()
                .display(new ItemStack(Items.BARRIER))
                .with(DataComponents.CUSTOM_NAME, Component.literal("§cCerrar"))
                .with(DataComponents.HIDE_ADDITIONAL_TOOLTIP, Unit.INSTANCE)
                .onClick(action -> {
                    player.closeContainer();
                })
                .build();
            template.set(4, 4, closeBtn);

            // Next button (right corner, position 8)
            if (current.getNext() != null) {
                Button nextBtn = LinkedPageButton.builder()
                    .display(new ItemStack(Items.SPECTRAL_ARROW))
                    .with(DataComponents.CUSTOM_NAME, Component.literal("§fPágina Siguiente →"))
                    .with(DataComponents.HIDE_ADDITIONAL_TOOLTIP, Unit.INSTANCE)
                    .linkType(LinkType.Next)
                    .build();
                template.set(4, 8, nextBtn);
            }
        }

        // Open the first page
        UIManager.openUIForcefully(player, pages.get(0));
    }
}
