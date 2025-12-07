package com.cobblemon.mdks.cobblepass.listeners;

import com.cobblemon.mdks.cobblepass.CobblePass;
import com.cobblemon.mdks.cobblepass.battlepass.PlayerBattlePass;
import com.cobblemon.mdks.cobblepass.util.Logger;
import com.cobblemon.mod.common.api.Priority;
import com.cobblemon.mod.common.api.events.CobblemonEvents;
import com.cobblemon.mod.common.api.events.pokemon.evolution.EvolutionCompleteEvent;
import kotlin.Unit;
import net.minecraft.server.level.ServerPlayer;

public class EvolvePokemonListener {
    private static final Logger LOGGER = new Logger("CobblePass");

    public static void register() {
        CobblemonEvents.EVOLUTION_COMPLETE.subscribe(Priority.NORMAL, EvolvePokemonListener::handle);
    }

    private static Unit handle(EvolutionCompleteEvent event) {
        if (event.getPokemon().getOwnerPlayer() instanceof ServerPlayer player) {
            int xp = CobblePass.config.getEvolveXP();
            CobblePass.battlePass.addXP(player, xp);
            LOGGER.debug("Awarded " + xp + " XP to " + player.getName().getString() + " for evolving a Pokémon");
        }
        return Unit.INSTANCE;
    }
}
