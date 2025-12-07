package com.cobblemon.mdks.cobblepass.command.subcommand;

import com.cobblemon.mdks.cobblepass.CobblePass;
import com.cobblemon.mdks.cobblepass.battlepass.BattlePass;
import com.cobblemon.mdks.cobblepass.util.Subcommand;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.tree.LiteralCommandNode;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

public class ReloadCommand extends Subcommand {

    public ReloadCommand() {
        super("§9Uso:\n§3- /battlepass reload");
    }

    @Override
    public LiteralCommandNode<CommandSourceStack> build() {
        return Commands.literal("reload")
                .requires(source -> source.hasPermission(4)) // Requires operator permission level
                .executes(this::run)
                .build();
    }

    @Override
    public int run(CommandContext<CommandSourceStack> context) {
        try {
            // Only reload configuration files
            CobblePass.reload();
            
            // Reload both configuration and player data
            CobblePass.battlePass.reload();

            context.getSource().sendSystemMessage(Component.literal(
                "§a¡Configuración del Pase de Batalla recargada exitosamente!"
            ));

            // Log reload event
            CobblePass.LOGGER.info("Configuración del Pase de Batalla recargada por " +
                (context.getSource().isPlayer() ?
                    context.getSource().getPlayer().getName().getString() :
                    "Consola"
                ));

            return 1;
        } catch (Exception e) {
            String errorMessage = "§cError al recargar la configuración del Pase de Batalla: " + e.getMessage();
            context.getSource().sendSystemMessage(Component.literal(errorMessage));

            // Log error
            CobblePass.LOGGER.error("Error al recargar la configuración del Pase de Batalla:");
            e.printStackTrace();
            
            return 0;
        }
    }
}
