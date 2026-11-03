package com.louzada.pixeldungeon.heroi;

import com.louzada.pixeldungeon.PixelDungeon;
import com.louzada.pixeldungeon.combate.Particulas;
import com.louzada.pixeldungeon.registro.ModAnexos;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetExperiencePacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;

// As contas do herói, iguais às do Shattered:
//   vida:     20 no nível 1, +5 por nível
//   precisão: 10 no nível 1, +1 por nível
//   esquiva:   5 no nível 1, +1 por nível
//   XP pra subir do nível N: 5 + 5 * N
// A barra de XP do Minecraft passa a mostrar o nível do Shattered.
public class RegrasDoHeroi {
    private static final ResourceLocation ID_VIDA_DO_NIVEL = ResourceLocation.fromNamespaceAndPath(PixelDungeon.MODID, "vida_do_nivel");
    private static final ResourceLocation ID_ESPACO_DA_BLINDAGEM = ResourceLocation.fromNamespaceAndPath(PixelDungeon.MODID, "espaco_da_blindagem");

    public static DadosDoHeroi dados(Player jogador) {
        return jogador.getData(ModAnexos.HEROI);
    }

    public static int vidaMaxima(int nivel) {
        return 20 + 5 * (nivel - 1);
    }

    public static int precisao(int nivel) {
        return 10 + (nivel - 1);
    }

    public static int esquiva(int nivel) {
        return 5 + (nivel - 1);
    }

    public static int xpParaSubir(int nivel) {
        return 5 + 5 * nivel;
    }

    // Regra de acerto do Shattered: sorteia de 0 até a precisão do atacante e de 0 até a esquiva do defensor;
    // acerta se o sorteio do atacante for maior ou igual
    public static boolean acertou(RandomSource sorteio, float precisaoDoAtacante, float esquivaDoDefensor) {
        return sorteio.nextFloat() * precisaoDoAtacante >= sorteio.nextFloat() * esquivaDoDefensor;
    }

    // Ajusta a vida máxima do jogador pro nível dele e atualiza a barra de XP
    public static void aplicar(ServerPlayer jogador) {
        DadosDoHeroi dados = dados(jogador);
        AttributeInstance vida = jogador.getAttribute(Attributes.MAX_HEALTH);
        if (vida != null) {
            // O Minecraft já dá 20 de vida; o modificador soma o que o nível dá a mais
            vida.addOrUpdateTransientModifier(new AttributeModifier(ID_VIDA_DO_NIVEL,
                    vidaMaxima(dados.nivel()) - 20, AttributeModifier.Operation.ADD_VALUE));
        }
        if (jogador.getHealth() > jogador.getMaxHealth()) {
            jogador.setHealth(jogador.getMaxHealth());
        }
        // Espaço pros corações amarelos (a blindagem do Shattered). Sem isso o Minecraft não deixa ter nenhum.
        AttributeInstance blindagem = jogador.getAttribute(Attributes.MAX_ABSORPTION);
        if (blindagem != null) {
            blindagem.addOrUpdateTransientModifier(new AttributeModifier(ID_ESPACO_DA_BLINDAGEM, 100, AttributeModifier.Operation.ADD_VALUE));
        }
        mostrarNivel(jogador);
    }

    // A barra de XP do Minecraft mostra o nível e o progresso do Shattered
    public static void mostrarNivel(ServerPlayer jogador) {
        DadosDoHeroi dados = dados(jogador);
        jogador.experienceLevel = dados.nivel();
        jogador.experienceProgress = (float) dados.xp() / xpParaSubir(dados.nivel());
        jogador.totalExperience = dados.xp();
        jogador.connection.send(new ClientboundSetExperiencePacket(jogador.experienceProgress, jogador.totalExperience, jogador.experienceLevel));
    }

    public static void ganharXp(ServerPlayer jogador, int quantidade) {
        DadosDoHeroi dados = dados(jogador);
        dados.definirXp(dados.xp() + quantidade);

        int niveisGanhos = 0;
        while (dados.xp() >= xpParaSubir(dados.nivel())) {
            dados.definirXp(dados.xp() - xpParaSubir(dados.nivel()));
            dados.definirNivel(dados.nivel() + 1);
            niveisGanhos++;
        }

        if (niveisGanhos > 0) {
            aplicar(jogador);
            // Igual ao Shattered: cada nível dá +5 de vida máxima e cura esses 5
            jogador.heal(5 * niveisGanhos);
            jogador.displayClientMessage(Component.translatable("mensagem.pixeldungeon.subiu_de_nivel", dados.nivel()), false);
            // Do nível 2 ao 6, cada nível dá um ponto de talento
            if (dados.pontosLivres() > 0) {
                jogador.displayClientMessage(Component.translatable("mensagem.pixeldungeon.ponto_de_talento",
                        Component.keybind("key.pixeldungeon.talentos")).withStyle(ChatFormatting.YELLOW), false);
            }
            jogador.level().playSound(null, jogador.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 1.0F, 1.0F);
            // Brilho subindo em volta do herói
            Particulas.subindoEmVolta(jogador, ParticleTypes.END_ROD, 24, 0.12);
            Particulas.subindoEmVolta(jogador, Particulas.poeira(0xFFFF44, 1.0F), 16, 0.0);
        } else {
            mostrarNivel(jogador);
        }
    }
}
