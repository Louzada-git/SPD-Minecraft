package com.louzada.pixeldungeon.heroi;

import com.louzada.pixeldungeon.combate.NumerosFlutuantes;
import com.louzada.pixeldungeon.combate.Particulas;
import com.louzada.pixeldungeon.entidade.EnxameDeMoscas;
import com.louzada.pixeldungeon.entidade.MobDoShattered;
import com.louzada.pixeldungeon.item.ArmaDoShattered;
import com.louzada.pixeldungeon.item.ArmaduraDoShattered;

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
//   5. Efeitos especiais (o enxame se divide, o rato albino faz sangrar)
//   6. O dano aparece flutuando em cima de quem apanhou, espirra sangue da cor dele, e o inimigo ganha uma barra de vida
// Magia e ataques de longe (o míssil mágico do cajado) não sorteiam acerto nem passam pela armadura, igual às varinhas
// do Shattered: só acordam o inimigo.
public class EventosDeCombate {
    // Mobs que não são do Shattered (do Minecraft ou de outros mods) usam estes valores
    private static final float PRECISAO_PADRAO = 10;
    private static final float ESQUIVA_PADRAO = 0;

    public static void aoReceberDano(LivingIncomingDamageEvent evento) {
        LivingEntity alvo = evento.getEntity();
        DamageSource fonte = evento.getSource();
        Entity atacante = fonte.getEntity();
        boolean corpoACorpo = atacante != null && fonte.getDirectEntity() == atacante;
        if (alvo.level().isClientSide()) {
            return;
        }
        if (!corpoACorpo) {
            if (atacante != null && alvo instanceof MobDoShattered inimigo) {
                evento.setAmount(inimigo.reduzirDanoRecebido(evento.getAmount()));
                acordar(alvo, atacante);
            }
            return;
        }
        RandomSource sorteio = alvo.getRandom();

        // 1. Ataque surpresa
        boolean surpresa = atacante instanceof Player jogadorAtacante
                && alvo instanceof MobDoShattered inimigo
                && inimigo.surpreendidoPor(jogadorAtacante);

        // 2. Acertou ou desviou?
        if (!surpresa && !RegrasDoHeroi.acertou(sorteio, precisaoDe(atacante), esquivaDe(alvo))) {
            evento.setCanceled(true);
            NumerosFlutuantes.mostrarTraduzido(alvo, "combate.pixeldungeon.desviou", NumerosFlutuantes.BRANCO);
            alvo.level().playSound(null, alvo.blockPosition(), SoundEvents.PLAYER_ATTACK_NODAMAGE, SoundSource.PLAYERS, 1.0F, 1.0F);
            acordar(alvo, atacante);
            return;
        }

        // 3. Dano
        float dano = evento.getAmount();
        if (atacante instanceof Player jogador && jogador.getMainHandItem().getItem() instanceof ArmaDoShattered arma) {
            // O Minecraft já calculou o golpe com base 1 (vezes a recarga e o crítico).
            // Mantemos essa proporção e trocamos o 1 pelo dano sorteado da arma.
            double ataqueBase = jogador.getAttributeValue(Attributes.ATTACK_DAMAGE);
            float proporcao = ataqueBase > 0 ? (float) (evento.getAmount() / ataqueBase) : 1.0F;
            dano = arma.sortearDano(sorteio, RegrasDoHeroi.dados(jogador).forca(), surpresa) * proporcao;
        } else if (atacante instanceof MobDoShattered inimigo) {
            dano = inimigo.sortearDano();
        }

        // 4. Armadura
        dano = Math.max(0, dano - armaduraDe(alvo, sorteio));
        if (alvo instanceof MobDoShattered inimigo) {
            dano = inimigo.reduzirDanoRecebido(dano);
        }

        // 5. Efeitos especiais: o enxame se divide antes de levar o golpe; o rato albino faz sangrar
        if (alvo instanceof EnxameDeMoscas enxame && dano > 0) {
            enxame.dividir(dano);
        }
        if (atacante instanceof MobDoShattered inimigo) {
            inimigo.aoAcertar(alvo, dano);
        }
        evento.setAmount(dano);

        if (surpresa) {
            NumerosFlutuantes.mostrar(alvo, "✦", NumerosFlutuantes.AMARELO);
            Particulas.faiscas(alvo);
        }
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
        }
    }

    // ---- Números de cada um ----

    private static float precisaoDe(Entity atacante) {
        if (atacante instanceof Player jogador) {
            DadosDoHeroi dados = RegrasDoHeroi.dados(jogador);
            float precisao = RegrasDoHeroi.precisao(dados.nivel());
            if (jogador.getMainHandItem().getItem() instanceof ArmaDoShattered arma) {
                precisao *= arma.precisaoCom(dados.forca());
            }
            return precisao;
        }
        if (atacante instanceof MobDoShattered inimigo) {
            return inimigo.precisao();
        }
        return PRECISAO_PADRAO;
    }

    private static float esquivaDe(LivingEntity alvo) {
        if (alvo instanceof Player jogador) {
            return RegrasDoHeroi.esquiva(RegrasDoHeroi.dados(jogador).nivel());
        }
        if (alvo instanceof MobDoShattered inimigo) {
            // Dormindo não dá pra desviar
            return inimigo.dormindo() ? 0 : inimigo.esquiva();
        }
        return ESQUIVA_PADRAO;
    }

    // Herói: armadura no peito + defesa da arma (florete). Inimigo: a armadura dele.
    private static int armaduraDe(LivingEntity alvo, RandomSource sorteio) {
        if (alvo instanceof Player jogador) {
            int forca = RegrasDoHeroi.dados(jogador).forca();
            int defesa = 0;
            ItemStack peito = jogador.getItemBySlot(EquipmentSlot.CHEST);
            if (peito.getItem() instanceof ArmaduraDoShattered armadura) {
                defesa += armadura.sortearDefesa(sorteio, forca);
            }
            if (jogador.getMainHandItem().getItem() instanceof ArmaDoShattered arma) {
                defesa += arma.sortearDefesa(sorteio, forca);
            }
            return defesa;
        }
        if (alvo instanceof MobDoShattered inimigo) {
            return inimigo.sortearArmadura();
        }
        return 0;
    }

    // Apanhou (ou quase): acorda e vai atrás de quem atacou
    private static void acordar(LivingEntity alvo, Entity atacante) {
        if (alvo instanceof MobDoShattered inimigo && atacante instanceof LivingEntity quemAtacou) {
            inimigo.acordar();
            if (!(quemAtacou instanceof Player jogador) || !jogador.isCreative()) {
                inimigo.setTarget(quemAtacou);
            }
        }
    }
}
