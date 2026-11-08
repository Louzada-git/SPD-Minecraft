package com.louzada.pixeldungeon.heroi;

import java.util.Locale;
import java.util.Random;

import com.louzada.pixeldungeon.PixelDungeon;
import com.louzada.pixeldungeon.Turno;
import com.louzada.pixeldungeon.combate.Sorteio;
import com.louzada.pixeldungeon.dungeon.Andares;
import com.louzada.pixeldungeon.dungeon.SaqueDoAndar;
import com.louzada.pixeldungeon.item.Artefato;
import com.louzada.pixeldungeon.item.CajadoDoMago;
import com.louzada.pixeldungeon.item.Cargas;
import com.louzada.pixeldungeon.item.CapaDasSombras;
import com.louzada.pixeldungeon.item.Identificacao;
import com.louzada.pixeldungeon.item.ItemAnel;
import com.louzada.pixeldungeon.item.ItemVarinha;
import com.louzada.pixeldungeon.item.NivelDoItem;
import com.louzada.pixeldungeon.item.Ouro;
import com.louzada.pixeldungeon.item.TipoDeAnel;
import com.louzada.pixeldungeon.item.TomoSagrado;
import com.louzada.pixeldungeon.item.Varinha;
import com.louzada.pixeldungeon.registro.ModComponentes;

import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

// O que os anéis que o herói está usando fazem, com os números do Shattered (ver TipoDeAnel).
// Os anéis ficam nas casinhas de anel do inventário (a calça e a bota do Minecraft, que o Shattered não tem).
// A cada meio segundo, o bônus de cada tipo é somado e guardado nos dados do herói (o combate e as armas leem de lá).
public class Aneis {
    public static final EquipmentSlot PRIMEIRO = EquipmentSlot.LEGS;
    public static final EquipmentSlot SEGUNDO = EquipmentSlot.FEET;
    private static final EquipmentSlot[] CASINHAS = { PRIMEIRO, SEGUNDO };

    private static final ResourceLocation FURIA = ResourceLocation.fromNamespaceAndPath(PixelDungeon.MODID, "anel_da_furia");
    private static final ResourceLocation PRESSA = ResourceLocation.fromNamespaceAndPath(PixelDungeon.MODID, "anel_da_pressa");
    private static final ResourceLocation PODER = ResourceLocation.fromNamespaceAndPath(PixelDungeon.MODID, "anel_do_poder");
    // Anel da fortuna: quantos inimigos faltam pro próximo prêmio, e quantos prêmios faltam pro próximo equipamento
    private static final String FORTUNA_INIMIGOS = "fortuna_inimigos";
    private static final String FORTUNA_PREMIOS = "fortuna_premios";

    public static boolean casinhaDeAnel(EquipmentSlot lugar) {
        return lugar == PRIMEIRO || lugar == SEGUNDO;
    }

    // O bônus de um anel: nível + 1; amaldiçoado, de 0 a -2
    public static int bonus(ItemStack anel) {
        int nivel = NivelDoItem.nivel(anel);
        return NivelDoItem.amaldicoado(anel) ? Math.min(0, nivel - 2) : nivel + 1;
    }

    // ---- Todo meio segundo ----

    // Soma os bônus dos anéis que o herói está usando e aplica os que mudam atributos (fúria, pressa, poder)
    public static void atualizar(ServerPlayer jogador, DadosDoHeroi dados) {
        int[] bonus = new int[TipoDeAnel.values().length];
        boolean[] usando = new boolean[TipoDeAnel.values().length];
        for (EquipmentSlot lugar : CASINHAS) {
            ItemStack anel = jogador.getItemBySlot(lugar);
            if (anel.getItem() instanceof ItemAnel item) {
                TipoDeAnel tipo = Identificacao.tipoDa(jogador, item.gema());
                bonus[tipo.ordinal()] += bonus(anel);
                usando[tipo.ordinal()] = true;
            }
        }
        dados.definirAneis(bonus, usando);
        multiplicar(jogador, Attributes.ATTACK_SPEED, FURIA, Math.pow(1.09051, bonus[TipoDeAnel.FURIA.ordinal()]));
        multiplicar(jogador, Attributes.MOVEMENT_SPEED, PRESSA, Math.pow(1.15, bonus[TipoDeAnel.PRESSA.ordinal()]));
        multiplicar(jogador, Attributes.MAX_HEALTH, PODER, Math.pow(1.035, bonus[TipoDeAnel.PODER.ordinal()]));
        // Intuição do ladrão +2: o Ladino sabe o tipo dos anéis que pega
        if (dados.pontos(Talento.INTUICAO_DO_LADRAO) >= 2) {
            for (ItemStack pilha : jogador.getInventory().items) {
                if (pilha.getItem() instanceof ItemAnel item && !Identificacao.conhece(jogador, item.gema())) {
                    Identificacao.identificar(jogador, item.gema());
                }
            }
        }
    }

    private static void multiplicar(ServerPlayer jogador, Holder<Attribute> atributo, ResourceLocation id, double vezes) {
        AttributeInstance instancia = jogador.getAttribute(atributo);
        if (instancia == null) {
            return;
        }
        AttributeModifier atual = instancia.getModifier(id);
        double valor = vezes - 1.0;
        if (atual != null && Math.abs(atual.amount() - valor) < 1e-6) {
            return;
        }
        instancia.removeModifier(id);
        if (Math.abs(valor) > 1e-6) {
            instancia.addTransientModifier(new AttributeModifier(id, valor, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        }
    }

    // ---- Pôr o anel ----

    // Igual ao Shattered: pôr o anel revela se ele é amaldiçoado (e aí ele gruda no dedo).
    // Intuição do ladrão (Ladino): +1 revela o tipo; +2 identifica tudo.
    public static void aoPor(ServerPlayer jogador, ItemStack anel) {
        if (!(anel.getItem() instanceof ItemAnel item)) {
            return;
        }
        int pontos = RegrasDoHeroi.dados(jogador).pontos(Talento.INTUICAO_DO_LADRAO);
        if (pontos >= 1 && !Identificacao.conhece(jogador, item.gema())) {
            Identificacao.identificar(jogador, item.gema());
        }
        if (pontos >= 2) {
            Identificacao.identificarItem(jogador, anel);
        }
        NivelDoItem.aoEquipar(jogador, anel);
        atualizar(jogador, RegrasDoHeroi.dados(jogador));
    }

    // ---- Identificar com o uso ----

    // Igual ao Shattered: o anel se identifica depois de 1 nível de experiência usando ele
    // (a intuição do ladrão acelera 2x / 3x)
    public static void aoGanharXp(ServerPlayer jogador, DadosDoHeroi dados, int xp) {
        float parte = xp / (float) RegrasDoHeroi.xpParaSubir(dados.nivel()) * (1 + dados.pontos(Talento.INTUICAO_DO_LADRAO));
        for (EquipmentSlot lugar : CASINHAS) {
            ItemStack anel = jogador.getItemBySlot(lugar);
            if (!(anel.getItem() instanceof ItemAnel item) || NivelDoItem.identificado(anel)) {
                continue;
            }
            float falta = anel.getOrDefault(ModComponentes.USOS_PARA_IDENTIFICAR.get(), 1.0F) - parte;
            if (falta <= 0) {
                Identificacao.identificar(jogador, item.gema());
                NivelDoItem.identificar(jogador, anel);
            } else {
                anel.set(ModComponentes.USOS_PARA_IDENTIFICAR.get(), falta);
            }
        }
    }

    // ---- Todo turno: anel da energia ----

    // Varinhas, cajado e artefatos carregam 1,175x mais rápido por bônus (amaldiçoado: mais devagar)
    public static void aoTurno(ServerPlayer jogador, DadosDoHeroi dados) {
        int bonus = dados.bonusDoAnel(TipoDeAnel.ENERGIA);
        if (bonus == 0) {
            return;
        }
        float extra = (float) (Math.pow(1.175, bonus) - 1.0);
        long agora = jogador.level().getGameTime();
        for (ItemStack pilha : jogador.getInventory().items) {
            if (pilha.getItem() instanceof CajadoDoMago) {
                adiantar(pilha, agora, CajadoDoMago.cargasMaximas(pilha), Varinha::ticksParaCarregar, extra);
            } else if (pilha.getItem() instanceof ItemVarinha) {
                adiantar(pilha, agora, ItemVarinha.cargasMaximas(pilha), Varinha::ticksParaCarregar, extra);
            }
        }
        for (ItemStack pilha : new ItemStack[] { CapaDasSombras.acharCapa(jogador), TomoSagrado.acharTomo(jogador) }) {
            if (pilha != null && !pilha.isEmpty()) {
                adiantar(pilha, agora, Artefato.cargasMaximas(pilha), Artefato::ticksParaCarregar, extra);
            }
        }
    }

    // Um turno a mais (ou a menos) de recarga, na proporção do tempo que a próxima carga leva
    private static void adiantar(ItemStack pilha, long agora, int maximo, Cargas.TempoDeRecarga tempo, float extra) {
        int cargas = Cargas.cargas(pilha, maximo);
        if (cargas >= maximo) {
            return;
        }
        Cargas.adiantar(pilha, agora, maximo, tempo, extra * Turno.TICKS / (float) tempo.ticks(maximo - cargas));
    }

    // ---- Dano recebido: elementos e tenacidade ----

    public static void aoReceberDano(LivingIncomingDamageEvent evento) {
        if (!(evento.getEntity() instanceof ServerPlayer jogador) || !GerenciadorDoHeroi.emJogo(jogador)) {
            return;
        }
        DadosDoHeroi dados = RegrasDoHeroi.dados(jogador);
        float dano = evento.getAmount();
        // Elementos: fogo, gelo, raio, gás, veneno e magia
        int elementos = dados.bonusDoAnel(TipoDeAnel.ELEMENTOS);
        if (elementos != 0 && elemental(evento.getSource())) {
            dano *= (float) Math.pow(0.825, elementos);
        }
        // Tenacidade: quanto mais ferido, menos dano
        int tenacidade = dados.bonusDoAnel(TipoDeAnel.TENACIDADE);
        if (tenacidade != 0) {
            float ferido = 1.0F - jogador.getHealth() / jogador.getMaxHealth();
            dano *= (float) Math.pow(0.85, tenacidade * ferido);
        }
        evento.setAmount(dano);
    }

    private static boolean elemental(DamageSource fonte) {
        return fonte.is(DamageTypeTags.IS_FIRE) || fonte.is(DamageTypeTags.IS_FREEZING) || fonte.is(DamageTypeTags.IS_LIGHTNING)
                || fonte.is(DamageTypeTags.WITCH_RESISTANT_TO);
    }

    // ---- Matar: anel da fortuna ----

    // Igual ao Shattered: a cada 0-20 inimigos (sorteado), cai um prêmio a mais; a cada 5-10 prêmios, um equipamento
    public static void aoMatar(ServerPlayer jogador, LivingEntity inimigo) {
        DadosDoHeroi dados = RegrasDoHeroi.dados(jogador);
        if (dados.bonusDoAnel(TipoDeAnel.FORTUNA) <= 0) {
            return;
        }
        Random sorteio = new Random(jogador.getRandom().nextLong());
        if (!dados.sim(FORTUNA_INIMIGOS + "_comecou")) {
            dados.definir(FORTUNA_INIMIGOS + "_comecou", true);
            dados.definir(FORTUNA_INIMIGOS, normal(sorteio, 0, 20));
            dados.definir(FORTUNA_PREMIOS, normal(sorteio, 5, 10));
        }
        int faltam = dados.inteiro(FORTUNA_INIMIGOS) - 1;
        while (faltam <= 0) {
            int premios = dados.inteiro(FORTUNA_PREMIOS);
            int andar = Math.max(1, Andares.andarPelaAltura(inimigo.getBlockY()));
            long semente = Identificacao.sementeDaRun(jogador);
            ItemStack premio;
            if (premios <= 0) {
                premio = SaqueDoAndar.premioDoMimico(sorteio, andar);
                dados.definir(FORTUNA_PREMIOS, normal(sorteio, 5, 10));
            } else {
                premio = premioConsumivel(sorteio, andar, semente);
                dados.definir(FORTUNA_PREMIOS, premios - 1);
            }
            ItemEntity noChao = inimigo.spawnAtLocation(premio);
            if (noChao != null) {
                noChao.setUnlimitedLifetime();
            }
            faltam += normal(sorteio, 0, 20);
        }
        dados.definir(FORTUNA_INIMIGOS, faltam);
    }

    // Um prêmio comum: poção, pergaminho, semente ou ouro (o anel da fortuna e o encantamento da sorte usam)
    public static ItemStack premioConsumivel(Random sorteio, int andar, long semente) {
        return switch (sorteio.nextInt(4)) {
            case 0 -> SaqueDoAndar.pocaoAleatoria(sorteio, semente);
            case 1 -> SaqueDoAndar.pergaminhoAleatorio(sorteio, semente);
            case 2 -> SaqueDoAndar.sementeAleatoria(sorteio);
            default -> Ouro.sortear(sorteio, andar);
        };
    }

    private static int normal(Random sorteio, int minimo, int maximo) {
        return Math.round(minimo + (maximo - minimo) * (sorteio.nextFloat() + sorteio.nextFloat()) / 2.0F);
    }

    // ---- Anel da força: o soco ----

    // O soco vira uma arma do tier da força do herói: tier = (força - 8) / 2 (de 1 em diante; cada ponto depois do 18 vale metade)
    public static int danoDoSoco(RandomSource sorteio, int forca, int bonus) {
        float tier = Math.max(1, (forca - 8) / 2.0F);
        if (tier > 5) {
            tier = 5 + (tier - 5) / 2.0F;
        }
        if (bonus <= 0) {
            tier = 1;
        }
        int minimo = Math.max(0, Math.round(tier + bonus));
        int maximo = Math.max(0, Math.round(5 * (tier + 1) + bonus * (tier + 1)));
        return Sorteio.normal(sorteio, minimo, Math.max(minimo, maximo));
    }

    // ---- Texto da dica ----

    // O número que aparece na dica de cada anel (ex.: "30%" de precisão a mais, "+2" de força)
    public static Object textoDoBonus(TipoDeAnel tipo, int bonus) {
        return switch (tipo) {
            case PRECISAO -> porcento(Math.pow(1.3, bonus));
            case EVASAO -> porcento(Math.pow(1.125, bonus));
            case FURIA -> porcento(Math.pow(1.09051, bonus));
            case PRESSA -> porcento(Math.pow(1.15, bonus));
            case ENERGIA, ARCANO -> porcento(Math.pow(1.175, bonus));
            case ELEMENTOS -> porcento(1.0 / Math.pow(0.825, bonus));
            case TENACIDADE -> porcento(1.0 / Math.pow(0.85, bonus));
            case TIRO_CERTEIRO -> porcento(Math.pow(1.2, bonus));
            case FORTUNA -> porcento(Math.pow(1.2, bonus));
            case PODER, FORCA -> (bonus >= 0 ? "+" : "") + bonus;
        };
    }

    private static String porcento(double vezes) {
        double diferenca = (vezes - 1.0) * 100.0;
        return (diferenca >= 0 ? "+" : "") + String.format(Locale.ROOT, "%.0f", diferenca) + "%";
    }
}
