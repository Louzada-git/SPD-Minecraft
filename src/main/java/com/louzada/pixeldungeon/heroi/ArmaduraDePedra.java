package com.louzada.pixeldungeon.heroi;

import com.louzada.pixeldungeon.combate.Particulas;
import com.louzada.pixeldungeon.registro.ModEfeitos;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;

// A armadura de pedra da varinha de terra viva (o "RockArmor" do Shattered):
//   - cada disparo num inimigo vira pedra em volta do herói (o dano que causou)
//   - a pedra segura metade de cada golpe que o herói leva, até acabar
//   - com pedra bastante (8 + 4 x nível da varinha), o próximo disparo cria um guardião da terra com ela
//   - cabe no máximo o dobro disso
public class ArmaduraDePedra {
    private static final String PEDRA = "armadura_de_pedra";
    private static final String NIVEL = "nivel_da_armadura_de_pedra";

    public static int pedra(Player jogador) {
        return RegrasDoHeroi.dados(jogador).inteiro(PEDRA);
    }

    public static int nivel(Player jogador) {
        return RegrasDoHeroi.dados(jogador).inteiro(NIVEL);
    }

    // Quanta pedra precisa pra criar o guardião
    public static int paraOGuardiao(int nivel) {
        return 8 + 4 * nivel;
    }

    public static void somar(Player jogador, int nivelDaVarinha, int quanto) {
        DadosDoHeroi dados = RegrasDoHeroi.dados(jogador);
        int nivel = Math.max(dados.inteiro(NIVEL), nivelDaVarinha);
        dados.definir(NIVEL, nivel);
        definir(jogador, Math.min(dados.inteiro(PEDRA) + quanto, 2 * paraOGuardiao(nivel)));
        if (quanto > 0) {
            Particulas.subindoEmVolta(jogador, Particulas.poeira(0x805500, 1.2F), 8 + nivel / 2, 0.0);
        }
    }

    // A pedra virou um guardião
    public static void tirar(Player jogador) {
        definir(jogador, 0);
    }

    // Segura metade do golpe (arredondando pra cima) até a pedra acabar
    public static float segurar(Player jogador, float dano) {
        int pedra = pedra(jogador);
        if (pedra <= 0 || dano <= 0) {
            return dano;
        }
        int golpe = Math.round(dano);
        int segura = golpe - golpe / 2;
        if (pedra <= segura) {
            definir(jogador, 0);
            Particulas.subindoEmVolta(jogador, ParticleTypes.POOF, 6, 0.02);
            return golpe - pedra;
        }
        definir(jogador, pedra - segura);
        return golpe - segura;
    }

    private static void definir(Player jogador, int pedra) {
        DadosDoHeroi dados = RegrasDoHeroi.dados(jogador);
        dados.definir(PEDRA, pedra);
        if (pedra <= 0) {
            dados.definir(NIVEL, 0);
            jogador.removeEffect(ModEfeitos.ARMADURA_DE_PEDRA);
        } else if (!jogador.hasEffect(ModEfeitos.ARMADURA_DE_PEDRA)) {
            jogador.addEffect(new MobEffectInstance(ModEfeitos.ARMADURA_DE_PEDRA, MobEffectInstance.INFINITE_DURATION, 0, false, false, true));
        }
    }
}
