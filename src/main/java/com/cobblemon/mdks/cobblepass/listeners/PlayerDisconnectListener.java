package com.cobblemon.mdks.cobblepass.listeners;

import com.cobblemon.mdks.cobblepass.CobblePass;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.level.ServerPlayer;

/**
 * Maneja la desconexión de jugadores para prevenir memory leaks.
 * Guarda los datos del jugador y opcionalmente limpia el caché de memoria.
 */
public class PlayerDisconnectListener {

    /**
     * Registra el listener de desconexión
     */
    public static void register() {
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            ServerPlayer player = handler.getPlayer();
            handlePlayerDisconnect(player);
        });
    }

    /**
     * Maneja la desconexión de un jugador
     * @param player El jugador que se desconectó
     */
    private static void handlePlayerDisconnect(ServerPlayer player) {
        String uuid = player.getUUID().toString();

        // Guardar datos del jugador antes de desconectar
        CobblePass.battlePass.savePlayerPass(uuid);

        CobblePass.LOGGER.debug("Saved battle pass data for disconnecting player: " + player.getName().getString());

        // Nota: No removemos del mapa para mantener datos en caché durante la sesión del servidor
        // Los datos se limpiarán solo cuando se reinicie el servidor o se use /battlepass reload
        // Esto es un trade-off entre memoria y rendimiento (evita I/O en cada reconexión)
    }
}
