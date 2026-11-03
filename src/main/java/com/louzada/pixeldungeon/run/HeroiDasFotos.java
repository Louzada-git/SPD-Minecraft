package com.louzada.pixeldungeon.run;

import java.util.Random;

import com.louzada.pixeldungeon.PixelDungeon;
import com.louzada.pixeldungeon.dungeon.SaqueDoAndar;
import com.louzada.pixeldungeon.heroi.ClasseDoHeroi;
import com.louzada.pixeldungeon.heroi.RegrasDoHeroi;
import com.louzada.pixeldungeon.item.Identificacao;
import com.louzada.pixeldungeon.item.NivelDoItem;
import com.louzada.pixeldungeon.item.Ouro;
import com.louzada.pixeldungeon.registro.ModEfeitos;
import com.louzada.pixeldungeon.registro.ModItens;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.ItemStack;

// Só no teste do GitHub que tira fotos da tela (propriedade pixeldungeon.fotografar):
// quem entra no mundo vira Guerreiro na hora, com alguns itens, um efeito, um pouco de fome e de dano,
// pra foto mostrar todas as partes da tela do jogo e do inventário.
public class HeroiDasFotos {

    public static boolean ligado() {
        return Boolean.getBoolean("pixeldungeon.fotografar");
    }

    public static void preparar(ServerPlayer jogador) {
        GerenciadorDaRun.escolherClasse(jogador, ClasseDoHeroi.GUERREIRO);
        long semente = Identificacao.sementeDaRun(jogador);
        Random sorteio = new Random(7);
        for (int i = 0; i < 3; i++) {
            jogador.getInventory().add(SaqueDoAndar.pocaoAleatoria(sorteio, semente));
            jogador.getInventory().add(SaqueDoAndar.pergaminhoAleatorio(sorteio, semente));
        }
        jogador.getInventory().add(SaqueDoAndar.sementeAleatoria(sorteio));
        jogador.getInventory().add(NivelDoItem.sortearDoChao(new ItemStack(ModItens.ESPADA.get()), sorteio, true));
        jogador.getInventory().add(new ItemStack(ModItens.KATANA.get()));
        jogador.getInventory().add(NivelDoItem.sortearDoChao(new ItemStack(ModItens.ARMADURA_DE_ESCAMAS.get()), sorteio, true));
        Ouro.pegar(jogador, 87);
        RegrasDoHeroi.ganharXp(jogador, 13);
        jogador.addEffect(new MobEffectInstance(ModEfeitos.ABENCOADO, 20 * 60));
        jogador.addEffect(new MobEffectInstance(ModEfeitos.PRESSA, 20 * 30));
        jogador.getFoodData().setFoodLevel(6);
        jogador.setHealth(jogador.getMaxHealth() * 0.6F);
        // Os ratos do andar 1 vêm atacar enquanto as fotos são tiradas: o herói não pode morrer
        jogador.getAbilities().invulnerable = true;
        jogador.onUpdateAbilities();
        PixelDungeon.LOGGER.info("Herói das fotos pronto: {}", jogador.getName().getString());
    }
}
