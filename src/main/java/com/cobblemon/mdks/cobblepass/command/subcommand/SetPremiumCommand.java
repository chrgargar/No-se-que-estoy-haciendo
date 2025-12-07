package com.cobblemon.mdks.cobblepass.command.subcommand;

import com.cobblemon.mdks.cobblepass.CobblePass;
import com.cobblemon.mdks.cobblepass.battlepass.PlayerBattlePass;
import com.cobblemon.mdks.cobblepass.util.Subcommand;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.tree.LiteralCommandNode;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public class SetPremiumCommand extends Subcommand {

    public SetPremiumCommand() {
        super("§9Uso:\n§3- /battlepass setpremium <jugador> <true/false>");
    }

    @Override
    public LiteralCommandNode<CommandSourceStack> build() {
        return Commands.literal("setpremium")
                .requires(source -> source.hasPermission(4)) // Requires operator permission level
                .then(Commands.argument("player", EntityArgument.player())
                    .then(Commands.argument("premium", BoolArgumentType.bool())
                        .executes(this::run)))
                .build();
    }

    @Override
    public int run(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer target = EntityArgument.getPlayer(context, "player");
            boolean premium = BoolArgumentType.getBool(context, "premium");

            PlayerBattlePass battlePass = CobblePass.battlePass.getPlayerPass(target);
            if (battlePass == null) {
                context.getSource().sendSystemMessage(Component.literal("§cError: No se encontró el pase de batalla del jugador"));
                return 0;
            }

            // Set premium status
            battlePass.setPremium(premium);

            // Save the player's data
            CobblePass.battlePass.savePlayerPass(target.getUUID().toString());

            // Notify command sender
            String status = premium ? "activado" : "desactivado";
            context.getSource().sendSystemMessage(Component.literal(
                String.format("§aPase premium %s para %s", status, target.getName().getString())
            ));

            // Notify target player
            if (premium) {
                target.sendSystemMessage(Component.literal(
                    "§a¡Recibiste acceso premium al pase de batalla! ¡Gracias por apoyar al servidor!"
                ));
            }

            return 1;
        } catch (Exception e) {
            context.getSource().sendSystemMessage(Component.literal("§cError: " + e.getMessage()));
            return 0;
        }
    }
}
