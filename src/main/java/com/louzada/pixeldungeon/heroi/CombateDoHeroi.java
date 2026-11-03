package com.louzada.pixeldungeon.heroi;

import javax.annotation.Nullable;

import com.louzada.pixeldungeon.combate.NumerosFlutuantes;
import com.louzada.pixeldungeon.combate.Particulas;
import com.louzada.pixeldungeon.entidade.MobDoShattered;
import com.louzada.pixeldungeon.item.ArmaComDano;
import com.louzada.pixeldungeon.registro.ModEfeitos;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;

// Um golpe do herói com as regras do Shattered. Usado pelos golpes normais, pelas habilidades da Duelista
// e pelas flechas do arco espiritual:
//   1. acerto: surpresa e inimigo iluminado sempre acertam; senão, precisão contra esquiva
//   2. dano da arma (vezes o multiplicador, mais o bônus da habilidade)
//   3. luz abrasadora (inimigo iluminado), armadura do inimigo, talentos e arma celestial
public class CombateDoHeroi {
    // Colado no inimigo, o arco acerta pior (igual às armas de arremesso do Shattered)
    private static final double DISTANCIA_COLADO = 4.5;

    private static boolean golpeJaCalculado = false;

    // Enquanto isso for verdade, o combate não calcula o dano de novo (o golpe já veio pronto)
    public static boolean golpeJaCalculado() {
        return golpeJaCalculado;
    }

    // Devolve o dano final, ou -1 se errou (aí já mostra "desviou")
    //   danoSemArma: dano de quem bate de mão vazia
    //   proporcao: força do golpe do Minecraft (golpe com a barra de ataque cheia = 1)
    public static float calcular(ServerPlayer jogador, LivingEntity alvo, @Nullable ArmaComDano arma, float danoSemArma, float proporcao,
            float multiplicador, int bonus, boolean acertoGarantido, boolean aDistancia) {
        RandomSource sorteio = jogador.getRandom();
        DadosDoHeroi dados = RegrasDoHeroi.dados(jogador);
        long agora = jogador.level().getGameTime();
        boolean surpresa = alvo instanceof MobDoShattered inimigo && inimigo.surpreendidoPor(jogador);
        boolean iluminado = alvo.hasEffect(ModEfeitos.ILUMINADO);

        // 1. Acertou?
        if (!acertoGarantido && !surpresa && !iluminado) {
            float precisao = RegrasDoHeroi.precisao(dados.nivel());
            if (arma != null) {
                precisao *= arma.precisaoCom(dados);
            }
            if (aDistancia && jogador.distanceTo(alvo) < DISTANCIA_COLADO) {
                precisao *= 0.5F;
            }
            if (jogador.hasEffect(ModEfeitos.ATORDOADO)) {
                precisao *= 0.5F;
            }
            if (!RegrasDoHeroi.acertou(sorteio, precisao, EventosDeCombate.esquivaDe(alvo))) {
                EventosDeCombate.mostrarErro(alvo, jogador);
                Furtividade.revelar(jogador);
                return -1;
            }
        }

        // 2. Dano
        float dano = arma != null ? arma.sortearDano(sorteio, dados, surpresa) * proporcao : danoSemArma;
        dano = dano * multiplicador + bonus;

        // 3. A luz guia some com o golpe (e a luz abrasadora queima mais)
        if (iluminado) {
            alvo.removeEffect(ModEfeitos.ILUMINADO);
            int pontos = dados.pontos(Talento.LUZ_ABRASADORA);
            if (pontos > 0) {
                dano += 1 + 2 * pontos;
            }
        }
        dano = Math.max(0, dano - EventosDeCombate.armaduraDe(alvo, jogador, sorteio));
        dano += Talentos.bonusDoGolpe(jogador, dados, alvo, surpresa, aDistancia, agora);
        if (jogador.hasEffect(ModEfeitos.ARMA_CELESTIAL)) {
            dano += 2;
            Particulas.sangue(alvo, 0xFFFF88, 2);
        }

        // A Duelista conta os golpes pra combinação das luvas
        if (dados.classe() == ClasseDoHeroi.DUELISTA) {
            HabilidadesDaDuelista.contarGolpe(dados, agora);
        }
        if (surpresa) {
            NumerosFlutuantes.mostrar(alvo, "✦", NumerosFlutuantes.AMARELO);
            Particulas.faiscas(alvo);
        }
        // Atacar tira a invisibilidade
        Furtividade.revelar(jogador);
        return dano;
    }

    // Aplica um golpe que já foi calculado (habilidades e flechas)
    public static void aplicar(LivingEntity alvo, float dano, DamageSource fonte) {
        golpeJaCalculado = true;
        try {
            alvo.invulnerableTime = 0;
            alvo.hurt(fonte, dano);
        } finally {
            golpeJaCalculado = false;
        }
    }
}
