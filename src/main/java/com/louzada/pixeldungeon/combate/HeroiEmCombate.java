package com.louzada.pixeldungeon.combate;

import java.util.UUID;

import javax.annotation.Nullable;

import com.louzada.pixeldungeon.heroi.Aneis;
import com.louzada.pixeldungeon.heroi.ArmaduraDePedra;
import com.louzada.pixeldungeon.heroi.ClasseDoHeroi;
import com.louzada.pixeldungeon.heroi.DadosDoHeroi;
import com.louzada.pixeldungeon.heroi.Furtividade;
import com.louzada.pixeldungeon.heroi.GlifosDoHeroi;
import com.louzada.pixeldungeon.heroi.HabilidadesDaDuelista;
import com.louzada.pixeldungeon.heroi.MagiasDoClerigo;
import com.louzada.pixeldungeon.heroi.RegrasDoHeroi;
import com.louzada.pixeldungeon.heroi.Talento;
import com.louzada.pixeldungeon.heroi.Talentos;
import com.louzada.pixeldungeon.heroi.TalentosDoTier2;
import com.louzada.pixeldungeon.item.Alteracao;
import com.louzada.pixeldungeon.item.Arma;
import com.louzada.pixeldungeon.item.ArmaDoShattered;
import com.louzada.pixeldungeon.item.ArmaduraDoShattered;
import com.louzada.pixeldungeon.item.Encantamento;
import com.louzada.pixeldungeon.item.Glifo;
import com.louzada.pixeldungeon.item.Maldicao;
import com.louzada.pixeldungeon.item.NivelDoItem;
import com.louzada.pixeldungeon.item.TipoDeAnel;
import com.louzada.pixeldungeon.registro.ModEfeitos;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

// O herói lutando com as regras do Shattered. Os números vêm do nível, da arma do golpe e da armadura no peito:
//   precisão: 10 + nível - 1, vezes a precisão da arma (que cai sem força) e a maldição inconstante
//   esquiva: 5 + nível - 1, que cai se faltar força pra armadura
//   dano: o da arma (vezes a força do golpe do Minecraft); de mão vazia, 1 até força - 8
//   armadura: a do peito, a defesa da arma (florete, katana), o escudo de luz e a armadura sagrada do Clérigo
// E os talentos, as maldições, os glifos e a identificação pelo uso entram nos golpes.
public class HeroiEmCombate implements Combatente {
    private ServerPlayer jogador;
    private DadosDoHeroi dados;

    public HeroiEmCombate(ServerPlayer jogador) {
        this.jogador = jogador;
        this.dados = RegrasDoHeroi.dados(jogador);
    }

    @Override
    public LivingEntity entidade() {
        return jogador;
    }

    public ServerPlayer jogador() {
        return jogador;
    }

    @Nullable
    private static Arma armaDo(Golpe golpe) {
        return golpe.arma().getItem() instanceof Arma arma ? arma : null;
    }

    private ItemStack armadura() {
        return jogador.getItemBySlot(EquipmentSlot.CHEST);
    }

    // ---- Números ----

    @Override
    public float precisao(Golpe golpe) {
        float precisao = RegrasDoHeroi.precisao(dados.nivel());
        Arma arma = armaDo(golpe);
        if (arma != null) {
            precisao *= arma.precisao(golpe.arma(), dados);
        }
        precisao *= Maldicao.multiplicadorDePrecisao(jogador, golpe.arma());
        // Anel da precisão: 1,3x por bônus
        precisao *= (float) Math.pow(1.3, dados.bonusDoAnel(TipoDeAnel.PRECISAO));
        // Dança da espada (cimitarra da Duelista): +50%
        if (jogador.hasEffect(ModEfeitos.DANCA_DA_ESPADA)) {
            precisao *= 1.5F;
        }
        // Agilidade líquida: o próximo golpe de perto depois de usar uma poção
        return golpe.deLonge() ? precisao : TalentosDoTier2.precisaoDePerto(jogador, dados, precisao);
    }

    // Glifo da pedra: não dá pra desviar (o dano cai no lugar, ver aoSerAtingido)
    @Override
    public float esquiva(Golpe golpe) {
        return GlifosDoHeroi.nivel(jogador, Glifo.PEDRA) >= 0 ? 0 : esquivaSemPedra();
    }

    private float esquivaSemPedra() {
        // Anel da evasão: 1,125x por bônus
        float esquiva = RegrasDoHeroi.esquiva(dados.nivel()) * (float) Math.pow(1.125, dados.bonusDoAnel(TipoDeAnel.EVASAO));
        if (armadura().getItem() instanceof ArmaduraDoShattered armadura) {
            esquiva *= armadura.fatorDeEsquiva(armadura(), dados.forca());
            // Alteração da armadura (pedra de alteração): esquiva a mais ou a menos, igual ao Shattered
            esquiva = Math.max(0, esquiva + Alteracao.esquiva(armadura()));
        }
        // Postura defensiva (bastão da Duelista): 3x
        if (jogador.hasEffect(ModEfeitos.POSTURA_DEFENSIVA)) {
            esquiva *= 3;
        }
        return TalentosDoTier2.esquiva(jogador, esquiva);
    }

    @Override
    public float sortearDano(Golpe golpe) {
        RandomSource sorteio = jogador.getRandom();
        Arma arma = armaDo(golpe);
        int dano = arma != null ? arma.sortearDano(golpe.arma(), sorteio, dados, golpe.surpresa()) : danoDeMaoVazia(sorteio);
        // Anel da força: com uma arma de perto, +1 de dano por bônus
        if (golpe.arma().getItem() instanceof ArmaDoShattered) {
            dano += dados.bonusDoAnel(TipoDeAnel.FORCA);
        }
        return dano * golpe.forca();
    }

    // Igual ao Shattered: de mão vazia, 1 até (força - 8); com o anel da força, o soco vira uma arma do tier da força
    private int danoDeMaoVazia(RandomSource sorteio) {
        if (dados.usaAnel(TipoDeAnel.FORCA)) {
            return Aneis.danoDoSoco(sorteio, dados.forca(), dados.bonusDoAnel(TipoDeAnel.FORCA));
        }
        return Sorteio.normal(sorteio, 1, Math.max(1, dados.forca() - 8));
    }

    @Override
    public int sortearArmadura(Golpe golpe) {
        RandomSource sorteio = jogador.getRandom();
        int defesa = 0;
        if (armadura().getItem() instanceof ArmaduraDoShattered armadura) {
            defesa += armadura.sortearDefesa(armadura(), sorteio, dados.forca());
        }
        // A arma na mão também defende um pouco
        ItemStack naMao = jogador.getMainHandItem();
        if (naMao.getItem() instanceof Arma arma) {
            defesa += arma.sortearDefesa(naMao, sorteio, dados);
        }
        // Escudo de luz do Clérigo: 2-4 / 3-6 de armadura contra o inimigo escolhido
        UUID protegidoDe = dados.alvo(MagiasDoClerigo.ESCUDO_ALVO);
        if (jogador.hasEffect(ModEfeitos.ESCUDO_DE_LUZ) && golpe.atacante().entidade().getUUID().equals(protegidoDe)) {
            int minimo = 1 + dados.pontos(Talento.ESCUDO_DE_LUZ);
            defesa += Sorteio.normal(sorteio, minimo, 2 * minimo);
        }
        // Armadura sagrada do Clérigo: bloqueia mais 1
        if (jogador.hasEffect(ModEfeitos.ARMADURA_SAGRADA)) {
            defesa += 1;
        }
        return defesa;
    }

    // O mangual não ataca de surpresa
    @Override
    public boolean fazAtaqueSurpresa() {
        return !(jogador.getMainHandItem().getItem() instanceof ArmaDoShattered arma) || arma.atacaDeSurpresa();
    }

    // ---- Reações ----

    // Luz abrasadora (Clérigo): o inimigo iluminado pela luz guia leva +3/+5
    @Override
    public float antesDaArmadura(Golpe golpe, float dano) {
        int pontos = dados.pontos(Talento.LUZ_ABRASADORA);
        if (golpe.iluminado() && pontos > 0) {
            dano += 1 + 2 * pontos;
        }
        return dano;
    }

    // Apanhou: a maldição ou o glifo da armadura agem, e a armadura vai se identificando
    @Override
    public float aoSerAtingido(Golpe golpe, float dano) {
        ItemStack armadura = armadura();
        if (armadura.getItem() instanceof ArmaduraDoShattered) {
            dano = Maldicao.aoSerAtingidoCom(jogador, golpe.atacante().entidade(), armadura, dano);
            dano = Glifo.aoSerAtingidoCom(jogador, golpe.atacante().entidade(), armadura, dano, GlifosDoHeroi.poder(jogador));
            if (NivelDoItem.glifo(armadura) == Glifo.PEDRA) {
                dano = GlifosDoHeroi.pedra(golpe, esquivaSemPedra() * Combate.pelosEfeitos(jogador), dano);
            }
            NivelDoItem.aoUsar(jogador, armadura);
        }
        // A armadura de pedra (varinha de terra viva) segura metade do golpe
        return ArmaduraDePedra.segurar(jogador, dano);
    }

    // Glifo da viscosidade: parte do dano que passou pela armadura vem aos poucos
    @Override
    public float depoisDaArmadura(Golpe golpe, float dano) {
        int viscosidade = GlifosDoHeroi.nivel(jogador, Glifo.VISCOSIDADE);
        return viscosidade >= 0 ? GlifosDoHeroi.adiarDano(jogador, dados, viscosidade, dano) : dano;
    }

    // Acertou: talentos, arma celestial, a maldição ou o encantamento da arma e a identificação pelo uso
    @Override
    public float aoAcertar(Golpe golpe, float dano) {
        LivingEntity alvo = golpe.alvo().entidade();
        long agora = jogador.level().getGameTime();
        dano += Talentos.bonusDoGolpe(jogador, dados, alvo, golpe.surpresa(), golpe.deLonge(), agora);
        if (jogador.hasEffect(ModEfeitos.ARMA_CELESTIAL)) {
            dano += 2;
            Particulas.sangue(alvo, 0xFFFF88, 2);
        }
        if (NivelDoItem.podeIdentificar(golpe.arma())) {
            dano = Maldicao.aoGolpearCom(jogador, alvo, golpe.arma(), dano);
            // O corte rúnico (lâmina rúnica da Duelista) deixa o encantamento mais forte.
            // Imune à magia (pergaminho antimagia), o encantamento da arma não funciona.
            if (!jogador.hasEffect(ModEfeitos.ANTIMAGIA)) {
                dano = Encantamento.aoGolpearCom(jogador, alvo, golpe.arma(), dano, GlifosDoHeroi.poder(jogador) + HabilidadesDaDuelista.poderExtraDoGolpe());
            }
            NivelDoItem.aoUsar(jogador, golpe.arma());
        }
        // A Duelista conta os golpes pra combinação das luvas, e a besta carregada arremessa
        if (dados.classe() == ClasseDoHeroi.DUELISTA) {
            HabilidadesDaDuelista.contarGolpe(dados, agora);
        }
        if (!golpe.deLonge()) {
            HabilidadesDaDuelista.aoAcertar(jogador, golpe);
        }
        if (golpe.surpresa()) {
            NumerosFlutuantes.mostrar(alvo, "✦", NumerosFlutuantes.AMARELO);
            Particulas.faiscas(alvo);
        }
        return dano;
    }

    // Atacar tira a invisibilidade, acertando ou não
    @Override
    public void depoisDeAtacar(Golpe golpe, boolean acertou) {
        Furtividade.revelar(jogador);
        if (!golpe.deLonge()) {
            TalentosDoTier2.depoisDoGolpeDePerto(dados);
        }
    }
}
