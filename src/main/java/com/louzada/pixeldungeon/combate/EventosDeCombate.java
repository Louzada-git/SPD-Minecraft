package com.louzada.pixeldungeon.combate;

import javax.annotation.Nullable;

import com.louzada.pixeldungeon.efeito.EfeitosNoTick;
import com.louzada.pixeldungeon.entidade.ArmaArremessada;
import com.louzada.pixeldungeon.entidade.EnxameDeMoscas;
import com.louzada.pixeldungeon.entidade.ImagemEspelhada;
import com.louzada.pixeldungeon.entidade.MobDoShattered;
import com.louzada.pixeldungeon.heroi.ClasseDoHeroi;
import com.louzada.pixeldungeon.heroi.DadosDoHeroi;
import com.louzada.pixeldungeon.heroi.Furtividade;
import com.louzada.pixeldungeon.heroi.HabilidadesDaDuelista;
import com.louzada.pixeldungeon.heroi.RegrasDoHeroi;
import com.louzada.pixeldungeon.heroi.TalentosDoTier2;
import com.louzada.pixeldungeon.item.ArmaDoShattered;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

// Liga o combate do Minecraft às regras do Shattered:
//   - todo golpe corpo a corpo (do herói, dos inimigos, da imagem espelhada) vira um Golpe e o Combate resolve
//   - golpes já resolvidos (habilidades da Duelista, flechas) só passam pelo que acontece com quem apanha
//   - magia (o míssil mágico, a luz guia) não sorteia acerto nem passa pela armadura, igual ao Shattered: só acorda o inimigo
//   - depois do dano: o número flutuando, o sangue, a barra de vida
//   - quando um inimigo morre: o XP e os itens dele
public class EventosDeCombate {

    public static void aoReceberDano(LivingIncomingDamageEvent evento) {
        LivingEntity alvo = evento.getEntity();
        DamageSource fonte = evento.getSource();
        Entity atacante = fonte.getEntity();
        if (alvo.level().isClientSide()) {
            return;
        }

        // Golpe que já veio resolvido: só o que acontece com quem apanha
        if (Combate.golpeJaCalculado()) {
            finalizarGolpe(evento, alvo, atacante, evento.getAmount());
            return;
        }

        boolean corpoACorpo = atacante instanceof LivingEntity && fonte.getDirectEntity() == atacante;
        if (!corpoACorpo) {
            if (atacante != null && alvo instanceof MobDoShattered inimigo) {
                evento.setAmount(inimigo.reduzirDanoRecebido(evento.getAmount()));
                Combate.acordar(alvo, atacante);
            }
            return;
        }
        LivingEntity quemBate = (LivingEntity) atacante;
        // Paralisado, congelado, dormindo, aterrorizado ou comendo não consegue bater;
        // e os inimigos não acertam um herói invisível
        if (EfeitosNoTick.impedidoDeAtacar(quemBate) || (quemBate instanceof MobDoShattered && Furtividade.invisivel(alvo))) {
            evento.setCanceled(true);
            return;
        }

        Golpe golpe = Golpe.de(Combatente.de(quemBate), Combatente.de(alvo)).danoDoMinecraft(evento.getAmount());
        if (quemBate instanceof ServerPlayer jogador) {
            // Corpo a corpo só conta a arma de perto (com o arco na mão, é um soco)
            ItemStack naMao = jogador.getMainHandItem();
            golpe.arma(naMao.getItem() instanceof ArmaDoShattered ? naMao : ItemStack.EMPTY);
            // O Minecraft já calculou o golpe com base 1 (vezes a recarga e o crítico): fica essa proporção
            double ataqueBase = jogador.getAttributeValue(Attributes.ATTACK_DAMAGE);
            golpe.forca(ataqueBase > 0 ? (float) (evento.getAmount() / ataqueBase) : 1.0F);
        }
        float dano = Combate.resolver(golpe);
        if (dano == Combate.ERROU) {
            evento.setCanceled(true);
            return;
        }
        finalizarGolpe(evento, alvo, quemBate, dano);
    }

    // O que acontece com quem apanha depois do dano calculado: o slime amortece, a raiz-da-terra segura,
    // o enxame se divide, e acorda
    private static void finalizarGolpe(LivingIncomingDamageEvent evento, LivingEntity alvo, @Nullable Entity atacante, float dano) {
        if (alvo instanceof MobDoShattered inimigo) {
            dano = inimigo.reduzirDanoRecebido(dano);
        }
        dano = EfeitosNoTick.segurarComARaiz(alvo, dano);
        if (alvo instanceof EnxameDeMoscas enxame && dano > 0) {
            enxame.dividir(dano);
        }
        evento.setAmount(dano);
        Combate.acordar(alvo, atacante);
    }

    // Depois de tudo (armadura, efeitos...), mostra quanto de vida o alvo perdeu de verdade
    public static void aoTomarDano(LivingDamageEvent.Post evento) {
        LivingEntity alvo = evento.getEntity();
        if (alvo.level().isClientSide() || evento.getNewDamage() <= 0) {
            return;
        }
        String cor = alvo instanceof Player ? NumerosFlutuantes.VERMELHO : NumerosFlutuantes.LARANJA;
        NumerosFlutuantes.mostrar(alvo, String.valueOf(Math.max(1, Math.round(evento.getNewDamage()))), cor);
        // Apanhar acorda do sono mágico
        EfeitosNoTick.aoApanhar(alvo);

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
    // Alguns inimigos deixam itens (carne misteriosa, ouro...). Igual ao Shattered, herói com mais de
    // 2 níveis acima do máximo do inimigo não ganha mais itens dele, e os itens nunca somem do chão.
    public static void aoMorrerAlguem(LivingDeathEvent evento) {
        if (!(evento.getEntity() instanceof MobDoShattered inimigo) || inimigo.level().isClientSide()) {
            return;
        }
        // As armas de arremesso cravadas nele caem
        ArmaArremessada.soltarEspetados(inimigo);
        ServerPlayer matador = evento.getSource().getEntity() instanceof ServerPlayer quem ? quem : null;
        // O que a imagem espelhada mata conta pro herói dela
        if (evento.getSource().getEntity() instanceof ImagemEspelhada imagem) {
            matador = imagem.jogadorDono();
        }
        boolean ganhaItens = matador == null || RegrasDoHeroi.dados(matador).nivel() <= inimigo.atributos().nivelMaximo() + 2;
        ItemStack saque = ganhaItens ? inimigo.sortearSaque() : ItemStack.EMPTY;
        if (!saque.isEmpty()) {
            ItemEntity noChao = inimigo.spawnAtLocation(saque);
            if (noChao != null) {
                noChao.setUnlimitedLifetime();
            }
        }
        if (matador != null) {
            DadosDoHeroi dados = RegrasDoHeroi.dados(matador);
            int xp = inimigo.xpAoMorrer();
            if (xp > 0 && dados.nivel() <= inimigo.atributos().nivelMaximo()) {
                NumerosFlutuantes.mostrar(matador, "+" + xp + " XP", NumerosFlutuantes.ROXO);
                RegrasDoHeroi.ganharXp(matador, xp);
            }
            if (dados.classe() == ClasseDoHeroi.DUELISTA) {
                HabilidadesDaDuelista.aoMatar(dados, matador.level().getGameTime());
            }
            // Ímpeto letal e pressa letal
            if (evento.getSource().getEntity() == matador) {
                TalentosDoTier2.aoMatar(matador, evento.getSource(), HabilidadesDaDuelista.golpeDeHabilidade());
            }
        }
    }
}
