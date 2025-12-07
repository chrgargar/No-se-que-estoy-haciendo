package com.cobblemon.mdks.cobblepass.listeners;

import com.cobblemon.mdks.cobblepass.CobblePass;
import com.cobblemon.mdks.cobblepass.battlepass.PlayerBattlePass;
import com.cobblemon.mdks.cobblepass.config.Config;
import com.cobblemon.mdks.cobblepass.util.Logger;
import com.cobblemon.mod.common.api.Priority;
import com.cobblemon.mod.common.api.events.CobblemonEvents;
import com.cobblemon.mod.common.api.events.pokemon.PokemonCapturedEvent;
import kotlin.Unit;
import net.minecraft.server.level.ServerPlayer;

public class CatchPokemonListener {
    private static final Logger LOGGER = new Logger("CobblePass");

    public static void register() {
        CobblemonEvents.POKEMON_CAPTURED.subscribe(Priority.NORMAL, CatchPokemonListener::handle);
    }

    private static Unit handle(PokemonCapturedEvent event) {
        if (event.getPlayer() instanceof ServerPlayer player) {
            // Get the caught Pokémon's species name
            String speciesName = event.getPokemon().getSpecies().getName();

            // Get player's battle pass
            var playerPass = CobblePass.battlePass.getPlayerPass(player);

            // Base catch XP
            int xp = CobblePass.config.getCatchXP();

            // Check if this is the first time catching this species
            boolean isFirstCatch = !playerPass.hasCaughtSpecies(speciesName);

            if (isFirstCatch) {
                // Add bonus XP for first catch
                int bonusXP = CobblePass.config.getFirstCatchXP();
                xp += bonusXP;

                // Mark species as caught
                playerPass.addCaughtSpecies(speciesName);

                // Save player data
                CobblePass.battlePass.savePlayerPass(player.getUUID().toString());

                LOGGER.debug("First catch of " + speciesName + "! Awarded " + xp + " XP (base: " +
                    CobblePass.config.getCatchXP() + " + bonus: " + bonusXP + ") to " +
                    player.getName().getString());

                // Notify player about first catch bonus
                player.sendSystemMessage(
                    net.minecraft.network.chat.Component.literal(
                        String.format("§6✦ ¡Primera captura de %s! §a+%d XP bonus", speciesName, bonusXP)
                    )
                );
            } else {
                LOGGER.debug("Awarded " + xp + " XP to " + player.getName().getString() + " for catching a Pokémon");
            }

            CobblePass.battlePass.addXP(player, xp);
        }
        return Unit.INSTANCE;
    }
}
