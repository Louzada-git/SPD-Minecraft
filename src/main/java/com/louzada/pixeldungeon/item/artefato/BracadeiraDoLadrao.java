package com.louzada.pixeldungeon.item.artefato;

import com.louzada.pixeldungeon.Turno;
import com.louzada.pixeldungeon.combate.Combate;
import com.louzada.pixeldungeon.combate.NumerosFlutuantes;
import com.louzada.pixeldungeon.combate.Particulas;
import com.louzada.pixeldungeon.efeito.EfeitosNoTick;
import com.louzada.pixeldungeon.entidade.MobDoShattered;
import com.louzada.pixeldungeon.heroi.Furtividade;
import com.louzada.pixeldungeon.heroi.RegrasDoHeroi;
import com.louzada.pixeldungeon.item.ArtefatoAchado;
import com.louzada.pixeldungeon.registro.ModEfeitos;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

// Braçadeira do ladrão (o "MasterThievesArmband" do Shattered): carrega quando o herói ganha experiência
// (3 + 0,15 x nível cargas por nível de herói). Cargas: 5 + nível/2.
//   roubar (clique direito, mirando um inimigo do lado): 1 carga. Tenta pegar o item que o inimigo deixaria ao morrer,
//     com 1 + 0,1 x nível vezes a chance normal (mais 0,5 se ele for pego de surpresa). Dando certo ou não, o inimigo
//     fica cego e aleijado por 3 + nível/2 turnos (2 a mais de surpresa) e não dá pra roubar dele de novo;
//     se o roubo deu certo, ele não deixa nada ao morrer.
//   sobe de nível (até o 10) roubando: 3 por roubo (2 a mais de surpresa); precisa de 10 + 3,33 x nível
// (No Shattered, a braçadeira também rouba das lojas; aqui ainda não tem lojas.)
public class BracadeiraDoLadrao extends ArtefatoAchado {
    // Marcas no inimigo: tentou roubar (0) ou roubou (1)
    public static final String ROUBADO = "pixeldungeon_roubado";
    private static final double ALCANCE = 6;

    public BracadeiraDoLadrao(Properties propriedades) {
        super(propriedades);
    }

    @Override
    public String id() {
        return "bracadeira_do_ladrao";
    }

    @Override
    public int nivelMaximo() {
        return 10;
    }

    @Override
    public int cargasMaximas(ItemStack pilha) {
        return 5 + nivel(pilha) / 2;
    }

    @Override
    protected int corDaBarra() {
        return 0xFFD700;
    }

    @Override
    protected float aproveitamentoDeFora() {
        return 0.1F;
    }

    @Override
    public void aoGanharXp(ServerPlayer jogador, ItemStack pilha, float porcao) {
        carregar(pilha, (3F + 0.15F * nivel(pilha)) * porcao * energia(jogador));
    }

    // Esse inimigo já teve o item roubado? (então não deixa nada ao morrer)
    public static boolean foiRoubado(MobDoShattered inimigo) {
        return inimigo.getPersistentData().getInt(ROUBADO) > 0;
    }

    @Override
    public boolean usar(ServerPlayer jogador, ItemStack pilha) {
        if (cargas(pilha) < 1) {
            avisar(jogador, "mensagem.pixeldungeon.bracadeira_do_ladrao.sem_carga");
            return false;
        }
        Vec3 olho = jogador.getEyePosition();
        Vec3 fim = olho.add(jogador.getLookAngle().scale(ALCANCE));
        EntityHitResult mira = ProjectileUtil.getEntityHitResult(jogador.level(), jogador, olho, fim, new AABB(olho, fim).inflate(1.0),
                entidade -> entidade instanceof MobDoShattered inimigo && inimigo.isAlive() && !inimigo.aliado(), 0.3F);
        if (mira == null || !jogador.hasLineOfSight(mira.getEntity())) {
            avisar(jogador, "mensagem.pixeldungeon.bracadeira_do_ladrao.sem_alvo");
            return false;
        }
        roubar(jogador, pilha, (MobDoShattered) mira.getEntity());
        return true;
    }

    public void roubar(ServerPlayer jogador, ItemStack pilha, MobDoShattered inimigo) {
        boolean surpresa = inimigo.surpreendidoPor(jogador);
        float vezes = 1F + 0.1F * nivel(pilha);
        int turnos = 3 + nivel(pilha) / 2;
        int ganhou = 3;
        Furtividade.revelar(jogador);
        if (surpresa) {
            vezes += 0.5F;
            turnos += 2;
            ganhou += 2;
            NumerosFlutuantes.mostrar(inimigo, "✦", NumerosFlutuantes.AMARELO);
            Particulas.faiscas(inimigo);
        }
        boolean podeRoubar = !inimigo.getPersistentData().contains(ROUBADO)
                && RegrasDoHeroi.dados(jogador).nivel() <= inimigo.atributos().nivelMaximo() + 2;
        ItemStack roubado = ItemStack.EMPTY;
        if (podeRoubar) {
            // A chance do inimigo, vezes o multiplicador: uma tentativa por vez inteira, e mais uma com a parte quebrada
            int tentativas = (int) vezes + (jogador.getRandom().nextFloat() < vezes % 1 ? 1 : 0);
            for (int i = 0; i < tentativas && roubado.isEmpty(); i++) {
                roubado = inimigo.sortearSaque();
            }
            inimigo.getPersistentData().putInt(ROUBADO, roubado.isEmpty() ? 0 : 1);
        }
        if (!podeRoubar) {
            avisar(jogador, "mensagem.pixeldungeon.bracadeira_do_ladrao.nada");
        } else if (roubado.isEmpty()) {
            avisar(jogador, "mensagem.pixeldungeon.bracadeira_do_ladrao.falhou");
        } else {
            jogador.displayClientMessage(Component.translatable("mensagem.pixeldungeon.bracadeira_do_ladrao.roubou", roubado.getHoverName())
                    .withStyle(ChatFormatting.GOLD), false);
            if (!jogador.getInventory().add(roubado)) {
                jogador.drop(roubado, false);
            }
        }
        Combate.acordar(inimigo, jogador);
        EfeitosNoTick.cegar(inimigo, Turno.ticks(turnos));
        inimigo.addEffect(new MobEffectInstance(ModEfeitos.ALEIJADO, Turno.ticks(turnos)));
        jogador.swing(InteractionHand.MAIN_HAND, true);
        jogador.level().playSound(null, inimigo.blockPosition(), surpresa ? SoundEvents.PLAYER_ATTACK_STRONG : SoundEvents.PLAYER_ATTACK_WEAK,
                SoundSource.PLAYERS, 1.0F, 1.2F);
        definirCargas(pilha, cargas(pilha) - 1);
        definirExperiencia(pilha, experiencia(pilha) + ganhou);
        while (nivel(pilha) < nivelMaximo() && experiencia(pilha) >= 10 + Math.round(3.33F * nivel(pilha))) {
            definirExperiencia(pilha, experiencia(pilha) - (10 + Math.round(3.33F * nivel(pilha))));
            subirNivel(jogador, pilha);
        }
    }
}
