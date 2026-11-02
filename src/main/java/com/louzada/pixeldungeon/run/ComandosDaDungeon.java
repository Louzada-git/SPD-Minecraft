package com.louzada.pixeldungeon.run;

import com.louzada.pixeldungeon.dungeon.Andares;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

// Comandos de teste (precisa de cheats ligados):
//   /dungeon novarun      -> começa uma run nova e manda todo mundo pro andar 1
//   /dungeon andar <n>    -> vai direto para o andar n
public class ComandosDaDungeon {

    public static void aoRegistrarComandos(RegisterCommandsEvent evento) {
        registrar(evento.getDispatcher());
    }

    public static void registrar(CommandDispatcher<CommandSourceStack> despachante) {
        despachante.register(Commands.literal("dungeon")
                .requires(fonte -> fonte.hasPermission(2))
                .then(Commands.literal("novarun")
                        .executes(contexto -> {
                            GerenciadorDaRun.novaRun(contexto.getSource().getServer());
                            contexto.getSource().sendSuccess(() -> Component.translatable("comando.pixeldungeon.novarun"), true);
                            return 1;
                        }))
                .then(Commands.literal("andar")
                        .then(Commands.argument("numero", IntegerArgumentType.integer(1, Andares.ULTIMO_ANDAR_LIBERADO))
                                .executes(contexto -> {
                                    ServerPlayer jogador = contexto.getSource().getPlayerOrException();
                                    int andar = IntegerArgumentType.getInteger(contexto, "numero");
                                    GerenciadorDaRun.levarPara(jogador, andar, true);
                                    return 1;
                                }))));
    }
}
