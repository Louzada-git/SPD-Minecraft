package com.louzada.pixeldungeon.heroi;

import java.util.UUID;

import javax.annotation.Nullable;

import com.louzada.pixeldungeon.combate.NumerosFlutuantes;
import com.louzada.pixeldungeon.combate.Particulas;
import com.louzada.pixeldungeon.combate.Sorteio;
import com.louzada.pixeldungeon.entidade.EnxameDeMoscas;
import com.louzada.pixeldungeon.entidade.MobDoShattered;
import com.louzada.pixeldungeon.item.ArmaComDano;
import com.louzada.pixeldungeon.item.ArmaDoShattered;
import com.louzada.pixeldungeon.item.ArmaduraDoShattered;
import com.louzada.pixeldungeon.registro.ModEfeitos;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

// Combate corpo a corpo com as regras do Shattered:
//   1. Ataque surpresa (inimigo dormindo ou distraído) sempre acerta
//   2. Senão, sorteia o acerto: precisão do atacante contra esquiva do defensor ("desviou")
//   3. Dano sorteado pela arma (herói) ou pelo inimigo
//   4. A armadura do defensor tira um pouco do dano
//   5. Efeitos especiais (o enxame se divide, o rato albino faz sangrar, talentos...)
//   6. O dano aparece flutuando em cima de quem apanhou, espirra sangue da cor dele, e o inimigo ganha uma barra de vida
// Os golpes do herói são calculados no CombateDoHeroi (o mesmo cálculo serve pras habilidades e flechas).
// Magia (o míssil mágico do cajado, a luz guia) não sorteia acerto nem passa pela armadura, igual ao Shattered: só acorda o inimigo.
public class EventosDeCombate {
    // Mobs que não são do Shattered (do Minecraft ou de outros mods) usam estes valores
    private static final float PRECISAO_PADRAO = 10;
    private static final float ESQUIVA_PADRAO = 0;

    public static void aoReceberDano(LivingIncomingDamageEvent evento) {
        LivingEntity alvo = evento.getEntity();
        DamageSource fonte = evento.getSource();
        Entity atacante = fonte.getEntity();
        if (alvo.level().isClientSide()) {
            return;
        }

        // Golpe que já veio calculado (habilidades da Duelista, flechas): só o que acontece com quem apanha
        if (CombateDoHeroi.golpeJaCalculado()) {
            finalizarGolpe(evento, alvo, atacante, evento.getAmount());
            return;
        }

        // Magia e ataques de longe
        boolean corpoACorpo = atacante != null && fonte.getDirectEntity() == atacante;
        if (!corpoACorpo) {
            if (atacante != null && alvo instanceof MobDoShattered inimigo) {
                evento.setAmount(inimigo.reduzirDanoRecebido(evento.getAmount()));
                acordar(alvo, atacante);
            }
            return;
        }

        // O herói batendo
        if (atacante instanceof ServerPlayer jogador) {
            ArmaComDano arma = jogador.getMainHandItem().getItem() instanceof ArmaDoShattered armaDeVerdade ? armaDeVerdade : null;
            // O Minecraft já calculou o golpe com base 1 (vezes a recarga e o crítico).
            // Mantemos essa proporção e trocamos o 1 pelo dano sorteado da arma.
            double ataqueBase = jogador.getAttributeValue(Attributes.ATTACK_DAMAGE);
            float proporcao = ataqueBase > 0 ? (float) (evento.getAmount() / ataqueBase) : 1.0F;
            float dano = CombateDoHeroi.calcular(jogador, alvo, arma, evento.getAmount(), proporcao, 1.0F, 0, false, false);
            if (dano < 0) {
                evento.setCanceled(true);
                return;
            }
            finalizarGolpe(evento, alvo, jogador, dano);
            return;
        }

        // Um inimigo batendo
        RandomSource sorteio = alvo.getRandom();
        boolean acertoGarantido = atacante instanceof MobDoShattered quemBateu && quemBateu.acertoGarantido();
        if (!acertoGarantido && !RegrasDoHeroi.acertou(sorteio, precisaoDe(atacante), esquivaDe(alvo))) {
            evento.setCanceled(true);
            mostrarErro(alvo, atacante);
            return;
        }
        float dano = evento.getAmount();
        if (atacante instanceof MobDoShattered inimigo) {
            dano = inimigo.sortearDano();
        }
        dano = Math.max(0, dano - armaduraDe(alvo, atacante, sorteio));
        // Armadura sagrada do Clérigo: bloqueia mais 1
        if (alvo.hasEffect(ModEfeitos.ARMADURA_SAGRADA)) {
            dano = Math.max(0, dano - 1);
        }
        if (atacante instanceof MobDoShattered inimigo) {
            inimigo.aoAcertar(alvo, dano);
        }
        finalizarGolpe(evento, alvo, atacante, dano);
    }

    // O que acontece com quem apanha depois do dano calculado: o slime amortece, o enxame se divide, e acorda
    private static void finalizarGolpe(LivingIncomingDamageEvent evento, LivingEntity alvo, @Nullable Entity atacante, float dano) {
        if (alvo instanceof MobDoShattered inimigo) {
            dano = inimigo.reduzirDanoRecebido(dano);
        }
        if (alvo instanceof EnxameDeMoscas enxame && dano > 0) {
            enxame.dividir(dano);
        }
        evento.setAmount(dano);
        acordar(alvo, atacante);
    }

    // "desviou", som de golpe no ar, e o alvo acorda
    public static void mostrarErro(LivingEntity alvo, @Nullable Entity atacante) {
        NumerosFlutuantes.mostrarTraduzido(alvo, "combate.pixeldungeon.desviou", NumerosFlutuantes.BRANCO);
        alvo.level().playSound(null, alvo.blockPosition(), SoundEvents.PLAYER_ATTACK_NODAMAGE, SoundSource.PLAYERS, 1.0F, 1.0F);
        acordar(alvo, atacante);
    }

    // Depois de tudo (armadura, efeitos...), mostra quanto de vida o alvo perdeu de verdade
    public static void aoTomarDano(LivingDamageEvent.Post evento) {
        LivingEntity alvo = evento.getEntity();
        if (alvo.level().isClientSide() || evento.getNewDamage() <= 0) {
            return;
        }
        String cor = alvo instanceof Player ? NumerosFlutuantes.VERMELHO : NumerosFlutuantes.LARANJA;
        NumerosFlutuantes.mostrar(alvo, String.valueOf(Math.max(1, Math.round(evento.getNewDamage()))), cor);

        // Sangue (o slime espirra verde, o Goo espirra gosma preta...)
        int corDoSangue = alvo instanceof MobDoShattered inimigo ? inimigo.corDoSangue() : Particulas.SANGUE_PADRAO;
        Particulas.sangue(alvo, corDoSangue, evento.getNewDamage());

        // Barra de vida em cima do inimigo
        if (alvo instanceof MobDoShattered inimigo) {
            inimigo.atualizarBarraDeVida();
        }

        // Quem bateu vê quanto de vida o alvo ainda tem
        if (evento.getSource().getEntity() instanceof ServerPlayer jogador && alvo != jogador) {
            jogador.displayClientMessage(Component.translatable("combate.pixeldungeon.vida_do_alvo", alvo.getType().getDescription(),
                    Math.max(0, Math.round(alvo.getHealth())), Math.round(alvo.getMaxHealth())), true);
        }
    }

    // Quem mata um inimigo do Shattered ganha o XP dele (se não passou do nível máximo dele).
    // Alguns inimigos deixam itens (carne misteriosa...).
    public static void aoMorrerAlguem(LivingDeathEvent evento) {
        if (!(evento.getEntity() instanceof MobDoShattered inimigo) || inimigo.level().isClientSide()) {
            return;
        }
        ItemStack saque = inimigo.sortearSaque();
        if (!saque.isEmpty()) {
            inimigo.spawnAtLocation(saque);
        }
        if (evento.getSource().getEntity() instanceof ServerPlayer jogador) {
            DadosDoHeroi dados = RegrasDoHeroi.dados(jogador);
            int xp = inimigo.xpAoMorrer();
            if (xp > 0 && dados.nivel() <= inimigo.atributos().nivelMaximo()) {
                NumerosFlutuantes.mostrar(jogador, "+" + xp + " XP", NumerosFlutuantes.ROXO);
                RegrasDoHeroi.ganharXp(jogador, xp);
            }
            if (dados.classe() == ClasseDoHeroi.DUELISTA) {
                HabilidadesDaDuelista.aoMatar(dados, jogador.level().getGameTime());
            }
        }
    }

    // ---- Números de cada um ----

    private static float precisaoDe(Entity atacante) {
        float precisao = PRECISAO_PADRAO;
        if (atacante instanceof Player jogador) {
            precisao = RegrasDoHeroi.precisao(RegrasDoHeroi.dados(jogador).nivel());
        } else if (atacante instanceof MobDoShattered inimigo) {
            precisao = inimigo.precisao();
        }
        // Atordoado: precisão pela metade
        if (atacante instanceof LivingEntity criatura && criatura.hasEffect(ModEfeitos.ATORDOADO)) {
            precisao *= 0.5F;
        }
        return precisao;
    }

    public static float esquivaDe(LivingEntity alvo) {
        float esquiva = ESQUIVA_PADRAO;
        if (alvo instanceof Player jogador) {
            esquiva = RegrasDoHeroi.esquiva(RegrasDoHeroi.dados(jogador).nivel());
        } else if (alvo instanceof MobDoShattered inimigo) {
            // Dormindo não dá pra desviar
            esquiva = inimigo.dormindo() ? 0 : inimigo.esquiva();
        }
        if (alvo.hasEffect(ModEfeitos.ATORDOADO)) {
            esquiva *= 0.5F;
        }
        return esquiva;
    }

    // Herói: armadura no peito + defesa da arma (florete) + escudo de luz contra esse inimigo.
    // Inimigo: a armadura dele.
    public static int armaduraDe(LivingEntity alvo, @Nullable Entity atacante, RandomSource sorteio) {
        if (alvo instanceof Player jogador) {
            DadosDoHeroi dados = RegrasDoHeroi.dados(jogador);
            int forca = dados.forca();
            int defesa = 0;
            ItemStack peito = jogador.getItemBySlot(EquipmentSlot.CHEST);
            if (peito.getItem() instanceof ArmaduraDoShattered armadura) {
                defesa += armadura.sortearDefesa(sorteio, forca);
            }
            if (jogador.getMainHandItem().getItem() instanceof ArmaDoShattered arma) {
                defesa += arma.sortearDefesa(sorteio, forca);
            }
            // Escudo de luz do Clérigo: 2-4 / 3-6 de armadura contra o inimigo escolhido
            UUID protegidoDe = dados.alvo(MagiasDoClerigo.ESCUDO_ALVO);
            if (atacante != null && jogador.hasEffect(ModEfeitos.ESCUDO_DE_LUZ) && atacante.getUUID().equals(protegidoDe)) {
                int minimo = 1 + dados.pontos(Talento.ESCUDO_DE_LUZ);
                defesa += Sorteio.normal(sorteio, minimo, 2 * minimo);
            }
            return defesa;
        }
        if (alvo instanceof MobDoShattered inimigo) {
            return inimigo.sortearArmadura();
        }
        return 0;
    }

    // Apanhou (ou quase): acorda e vai atrás de quem atacou
    private static void acordar(LivingEntity alvo, @Nullable Entity atacante) {
        if (alvo instanceof MobDoShattered inimigo && atacante instanceof LivingEntity quemAtacou) {
            inimigo.acordar();
            if (!(quemAtacou instanceof Player jogador) || !jogador.isCreative()) {
                inimigo.setTarget(quemAtacou);
            }
        }
    }
}
