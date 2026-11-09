package com.louzada.pixeldungeon.item.artefato;

import com.louzada.pixeldungeon.Turno;
import com.louzada.pixeldungeon.combate.Particulas;
import com.louzada.pixeldungeon.entidade.MobDoShattered;
import com.louzada.pixeldungeon.heroi.DadosDoHeroi;
import com.louzada.pixeldungeon.heroi.Furtividade;
import com.louzada.pixeldungeon.heroi.RegrasDoHeroi;
import com.louzada.pixeldungeon.item.ArtefatoAchado;
import com.louzada.pixeldungeon.registro.ModEfeitos;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.ItemStack;

// Ampulheta do tempo (o "TimekeepersHourglass" do Shattered): vem cheia e recarrega com o tempo
// (1 carga a cada 90 turnos cheia, 60 vazia). Cargas: 5 + nível (o nível sobe com os saquinhos de areia da loja).
//   estase (clique direito): gasta até 2 cargas e tira o herói do tempo por 5 turnos cada: não se mexe, fica invisível
//     e nada o machuca nem o atrapalha
//   parar o tempo (agachado): gasta 1 carga na hora e mais 1 a cada 2 turnos; os inimigos em volta ficam parados.
//     Acaba quando as cargas acabam, quando o herói ataca ou usa uma varinha (igual a invisibilidade), ou usando de novo
public class AmpulhetaDoTempo extends ArtefatoAchado {
    private static final String CUSTO = "ampulheta_custo";
    // Até onde o tempo para (os andares do Shattered cabem nisso)
    public static final double ALCANCE = 64;

    public AmpulhetaDoTempo(Properties propriedades) {
        super(propriedades);
    }

    @Override
    public String id() {
        return "ampulheta_do_tempo";
    }

    @Override
    public int nivelMaximo() {
        return 5;
    }

    @Override
    public int cargasMaximas(ItemStack pilha) {
        return 5 + nivel(pilha);
    }

    @Override
    protected int cargasIniciais(ItemStack pilha) {
        return cargasMaximas(pilha);
    }

    @Override
    protected int corDaBarra() {
        return 0xFFE08A;
    }

    // Recarga com o tempo (o anel da energia acelera). Com o tempo parado, não recarrega.
    @Override
    public void aoTurno(ServerPlayer jogador, ItemStack pilha) {
        float multiplicador = energia(jogador);
        if (!jogador.hasEffect(ModEfeitos.TEMPO_PARADO)) {
            int faltam = cargasMaximas(pilha) - cargas(pilha);
            carregar(pilha, multiplicador / (90F - faltam * 3F));
        }
        if (jogador.hasEffect(ModEfeitos.TEMPO_PARADO)) {
            passarTempoParado(jogador, pilha);
        }
    }

    // Estase: até 2 cargas
    @Override
    public boolean usar(ServerPlayer jogador, ItemStack pilha) {
        if (jogador.hasEffect(ModEfeitos.TEMPO_PARADO)) {
            soltarOTempo(jogador);
            return true;
        }
        if (jogador.hasEffect(ModEfeitos.ESTASE)) {
            return false;
        }
        if (cargas(pilha) <= 0) {
            avisar(jogador, "mensagem.pixeldungeon.ampulheta_do_tempo.sem_carga");
            return false;
        }
        int gastas = Math.min(2, cargas(pilha));
        definirCargas(pilha, cargas(pilha) - gastas);
        int ticks = Turno.ticks(5 * gastas);
        Furtividade.revelar(jogador);
        jogador.addEffect(new MobEffectInstance(ModEfeitos.ESTASE, ticks, 0, false, false, true));
        Furtividade.ficarInvisivel(jogador, ticks);
        Particulas.subindoEmVolta(jogador, ParticleTypes.ENCHANT, 20, 0.5);
        jogador.level().playSound(null, jogador.blockPosition(), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 0.6F, 1.8F);
        return true;
    }

    // Parar o tempo: 1 carga na hora, depois 1 a cada 2 turnos
    @Override
    public boolean usarAgachado(ServerPlayer jogador, ItemStack pilha) {
        if (jogador.hasEffect(ModEfeitos.TEMPO_PARADO)) {
            soltarOTempo(jogador);
            return true;
        }
        if (jogador.hasEffect(ModEfeitos.ESTASE)) {
            return false;
        }
        if (cargas(pilha) <= 0) {
            avisar(jogador, "mensagem.pixeldungeon.ampulheta_do_tempo.sem_carga");
            return false;
        }
        definirCargas(pilha, cargas(pilha) - 1);
        RegrasDoHeroi.dados(jogador).definir(CUSTO, 2F);
        jogador.addEffect(new MobEffectInstance(ModEfeitos.TEMPO_PARADO, Turno.ticks(2) + 5, 0, false, false, true));
        pararInimigos(jogador);
        Particulas.subindoEmVolta(jogador, ParticleTypes.REVERSE_PORTAL, 30, 0.5);
        jogador.level().playSound(null, jogador.blockPosition(), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 0.8F, 0.6F);
        return true;
    }

    // Um turno de tempo parado: conta o custo e segura os inimigos mais um pouco
    private void passarTempoParado(ServerPlayer jogador, ItemStack pilha) {
        DadosDoHeroi dados = RegrasDoHeroi.dados(jogador);
        float custo = dados.decimal(CUSTO) - 1;
        int cargas = cargas(pilha);
        while (custo < -0.001F) {
            custo += 2;
            cargas--;
        }
        dados.definir(CUSTO, custo);
        if (cargas < 0 || (cargas == 0 && custo <= 0)) {
            definirCargas(pilha, 0);
            soltarOTempo(jogador);
            return;
        }
        definirCargas(pilha, cargas);
        jogador.addEffect(new MobEffectInstance(ModEfeitos.TEMPO_PARADO, Turno.ticks(2) + 5, 0, false, false, true));
        pararInimigos(jogador);
    }

    // Os inimigos em volta (os aliados não) ficam parados mais um pouco
    public static void pararInimigos(ServerPlayer jogador) {
        for (MobDoShattered inimigo : jogador.serverLevel().getEntitiesOfClass(MobDoShattered.class, jogador.getBoundingBox().inflate(ALCANCE))) {
            if (!inimigo.aliado()) {
                inimigo.addEffect(new MobEffectInstance(ModEfeitos.PARADO_NO_TEMPO, Turno.ticks(2) + 5, 0, false, false, false));
            }
        }
    }

    // O tempo volta a andar (os inimigos se soltam na hora)
    public static void soltarOTempo(ServerPlayer jogador) {
        if (!jogador.hasEffect(ModEfeitos.TEMPO_PARADO)) {
            return;
        }
        jogador.removeEffect(ModEfeitos.TEMPO_PARADO);
        for (MobDoShattered inimigo : jogador.serverLevel().getEntitiesOfClass(MobDoShattered.class, jogador.getBoundingBox().inflate(ALCANCE + 8))) {
            inimigo.removeEffect(ModEfeitos.PARADO_NO_TEMPO);
        }
        jogador.level().playSound(null, jogador.blockPosition(), SoundEvents.BEACON_DEACTIVATE, SoundSource.PLAYERS, 0.8F, 0.6F);
    }
}
