package com.cobblemon.mdks.cobblepass.listeners;

import com.cobblemon.mdks.cobblepass.CobblePass;
import com.cobblemon.mdks.cobblepass.battlepass.PlayerBattlePass;
import com.cobblemon.mdks.cobblepass.util.Logger;
import com.cobblemon.mod.common.api.Priority;
import com.cobblemon.mod.common.api.events.CobblemonEvents;
import com.cobblemon.mod.common.api.events.pokemon.LevelUpEvent;
import kotlin.Unit;
import net.minecraft.server.level.ServerPlayer;

public class LevelUpPokemonListener {
    private static final Logger LOGGER = new Logger("CobblePass");

    public static void register() {
        CobblemonEvents.LEVEL_UP_EVENT.subscribe(Priority.NORMAL, LevelUpPokemonListener::handle);
    }

    private static Unit handle(LevelUpEvent event) {
        if (event.getPokemon().getOwnerPlayer() instanceof ServerPlayer player) {
            int xp = CobblePass.config.getLevelUpXP();
            CobblePass.battlePass.addXP(player, xp);
            LOGGER.debug("Awarded " + xp + " XP to " + player.getName().getString() + " for leveling up a Pokémon");
        }
        return Unit.INSTANCE;
    }
}
