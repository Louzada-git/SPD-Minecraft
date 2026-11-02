package com.louzada.pixeldungeon.run;

import com.louzada.pixeldungeon.dungeon.Andares;
import com.louzada.pixeldungeon.heroi.RegrasDoHeroi;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

// Comandos:
//   /heroi                -> abre o menu de escolha de herói (pra quem ainda não está na run)
// De teste (precisa de cheats ligados):
//   /dungeon novarun      -> acaba a run atual e manda todo mundo pro menu
//   /dungeon andar <n>    -> vai direto para o andar n
//   /dungeon xp <n>       -> ganha n de XP do Shattered
public class ComandosDaDungeon {

    public static void aoRegistrarComandos(RegisterCommandsEvent evento) {
        registrar(evento.getDispatcher());
    }

    public static void registrar(CommandDispatcher<CommandSourceStack> despachante) {
        despachante.register(Commands.literal("heroi")
                .executes(contexto -> {
                    ServerPlayer jogador = contexto.getSource().getPlayerOrException();
                    if (GerenciadorDaRun.estaNaRun(jogador)) {
                        contexto.getSource().sendFailure(Component.translatable("comando.pixeldungeon.ja_na_run"));
                        return 0;
                    }
                    GerenciadorDaRun.esperarNoMenu(jogador);
                    return 1;
                }));

        despachante.register(Commands.literal("dungeon")
                .requires(fonte -> fonte.hasPermission(2))
                .then(Commands.literal("novarun")
                        .executes(contexto -> {
                            GerenciadorDaRun.fimDaRun(contexto.getSource().getServer());
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
                                })))
                .then(Commands.literal("xp")
                        .then(Commands.argument("quantidade", IntegerArgumentType.integer(1, 1000))
                                .executes(contexto -> {
                                    ServerPlayer jogador = contexto.getSource().getPlayerOrException();
                                    RegrasDoHeroi.ganharXp(jogador, IntegerArgumentType.getInteger(contexto, "quantidade"));
                                    return 1;
                                }))));
    }
}
