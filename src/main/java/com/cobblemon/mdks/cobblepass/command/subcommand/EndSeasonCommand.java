package com.cobblemon.mdks.cobblepass.command.subcommand;

import com.cobblemon.mdks.cobblepass.CobblePass;
import com.cobblemon.mdks.cobblepass.util.Subcommand;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.tree.LiteralCommandNode;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

public class EndSeasonCommand extends Subcommand {

    public EndSeasonCommand() {
        super("§9Uso:\n§3- /battlepass endseason");
    }

    @Override
    public LiteralCommandNode<CommandSourceStack> build() {
        return Commands.literal("endseason")
                .requires(source -> source.hasPermission(4)) // Requires operator permission level
                .executes(this::run)
                .build();
    }

    @Override
    public int run(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();

        // Check if a season is active
        if (!CobblePass.config.isSeasonActive()) {
            source.sendSystemMessage(Component.literal("§c¡No hay ninguna temporada activa para terminar!"));
            return 0;
        }

        int currentSeason = CobblePass.config.getCurrentSeason();

        // End the season by setting the end time to now
        CobblePass.config.endCurrentSeason();

        // Broadcast to all players
        source.getServer().getPlayerList().getPlayers().forEach(player ->
            player.sendSystemMessage(Component.literal("§6¡La Temporada " + currentSeason +
                " del Pase de Batalla ha terminado!"))
        );

        source.sendSystemMessage(Component.literal("§a¡Temporada " + currentSeason + " terminada exitosamente!"));

        return 1;
    }
}
