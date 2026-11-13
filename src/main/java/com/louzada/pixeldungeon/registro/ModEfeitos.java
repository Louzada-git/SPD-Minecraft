package com.louzada.pixeldungeon.registro;

import com.louzada.pixeldungeon.PixelDungeon;
import com.louzada.pixeldungeon.efeito.EfeitoSimples;
import com.louzada.pixeldungeon.efeito.MucoCaustico;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

// Efeitos do Shattered (os "buffs" e "debuffs" de lá viram efeitos do Minecraft).
public class ModEfeitos {
    public static final DeferredRegister<MobEffect> EFEITOS = DeferredRegister.create(Registries.MOB_EFFECT, PixelDungeon.MODID);

    // Muco cáustico do Goo: dano com o tempo, sai na água
    public static final DeferredHolder<MobEffect, MucoCaustico> MUCO_CAUSTICO = EFEITOS.register("muco_caustico", MucoCaustico::new);

    // Atordoado (golpe pesado da maça): precisão e esquiva pela metade
    public static final DeferredHolder<MobEffect, EfeitoSimples> ATORDOADO = EFEITOS.register("atordoado",
            () -> new EfeitoSimples(MobEffectCategory.HARMFUL, 0xCCCC66));

    // Iluminado (luz guia do Clérigo): o próximo ataque físico contra ele acerta com certeza
    public static final DeferredHolder<MobEffect, EfeitoSimples> ILUMINADO = EFEITOS.register("iluminado",
            () -> new EfeitoSimples(MobEffectCategory.HARMFUL, 0xFFFF88));

    // Arma celestial (Clérigo): +2 de dano mágico em cada golpe
    public static final DeferredHolder<MobEffect, EfeitoSimples> ARMA_CELESTIAL = EFEITOS.register("arma_celestial",
            () -> new EfeitoSimples(MobEffectCategory.BENEFICIAL, 0xFFEE77));

    // Armadura sagrada (Clérigo): bloqueia 1 de dano a mais em cada golpe
    public static final DeferredHolder<MobEffect, EfeitoSimples> ARMADURA_SAGRADA = EFEITOS.register("armadura_sagrada",
            () -> new EfeitoSimples(MobEffectCategory.BENEFICIAL, 0xFFEE77));

    // Escudo de luz (Clérigo): armadura extra contra um inimigo escolhido
    public static final DeferredHolder<MobEffect, EfeitoSimples> ESCUDO_DE_LUZ = EFEITOS.register("escudo_de_luz",
            () -> new EfeitoSimples(MobEffectCategory.BENEFICIAL, 0xFFFFCC));

    // ---- Efeitos das poções, pergaminhos, plantas e maldições (o que cada um faz fica no EfeitosNoTick e no combate) ----

    // Comendo: igual ao Shattered, comer gasta turnos (ração e carne misteriosa: 3; baga e ração de suprimentos: 1).
    // Come na hora, mas nesse tempo o herói não anda, não ataca e não usa itens.
    public static final DeferredHolder<MobEffect, EfeitoSimples> COMENDO = EFEITOS.register("comendo",
            () -> EfeitoSimples.quePrende(MobEffectCategory.NEUTRAL, 0xC89650, "comendo"));

    // Paralisado (gás paralisante): não anda nem ataca. Igual ao Shattered, dura 10 turnos.
    public static final DeferredHolder<MobEffect, EfeitoSimples> PARALISADO = EFEITOS.register("paralisado",
            () -> EfeitoSimples.quePrende(MobEffectCategory.HARMFUL, 0xC8B4FF, "paralisado"));
    // Congelado (poção de gelo, gelo-do-norte): igual ao paralisado
    public static final DeferredHolder<MobEffect, EfeitoSimples> CONGELADO = EFEITOS.register("congelado",
            () -> EfeitoSimples.quePrende(MobEffectCategory.HARMFUL, 0x9BD6FF, "congelado"));
    // Sono mágico (canção de ninar no herói): não se mexe e cura 1 por turno até encher; apanhar acorda
    public static final DeferredHolder<MobEffect, EfeitoSimples> SONO_MAGICO = EFEITOS.register("sono_magico",
            () -> EfeitoSimples.quePrende(MobEffectCategory.HARMFUL, 0x8888FF, "sono_magico"));
    // Sonolento (canção de ninar): quando acaba, dorme
    public static final DeferredHolder<MobEffect, EfeitoSimples> SONOLENTO = EFEITOS.register("sonolento",
            () -> new EfeitoSimples(MobEffectCategory.HARMFUL, 0xAAAAFF));
    // Aterrorizado (pergaminho de terror): foge de quem leu e não ataca
    public static final DeferredHolder<MobEffect, EfeitoSimples> ATERRORIZADO = EFEITOS.register("aterrorizado",
            () -> new EfeitoSimples(MobEffectCategory.HARMFUL, 0xFF4444));
    // Visão mágica (poção da visão mágica): vê todo mundo até 12 quadradinhos, através das paredes
    public static final DeferredHolder<MobEffect, EfeitoSimples> VISAO_MAGICA = EFEITOS.register("visao_magica",
            () -> new EfeitoSimples(MobEffectCategory.BENEFICIAL, 0x66CCFF));
    // Vigor (poção do vigor): anda 1,5x mais rápido
    public static final DeferredHolder<MobEffect, EfeitoSimples> VIGOR = EFEITOS.register("vigor",
            () -> EfeitoSimples.queMudaAVelocidade(MobEffectCategory.BENEFICIAL, 0xFFDD55, "vigor", 0.5));
    // Armadura de barro (poção da armadura de barro): de 0 até (nível + 1) de armadura; perde 1 nível a cada 50 turnos
    public static final DeferredHolder<MobEffect, EfeitoSimples> ARMADURA_DE_BARRO = EFEITOS.register("armadura_de_barro",
            () -> new EfeitoSimples(MobEffectCategory.BENEFICIAL, 0x8A6A3A));
    // Purificado (poção da limpeza): nenhum efeito ruim pega
    public static final DeferredHolder<MobEffect, EfeitoSimples> PURIFICADO = EFEITOS.register("purificado",
            () -> new EfeitoSimples(MobEffectCategory.BENEFICIAL, 0xFF4CD2));
    // Imune à magia (pergaminho antimagia): magia não machuca nem pega, e os glifos e encantamentos do herói param
    public static final DeferredHolder<MobEffect, EfeitoSimples> ANTIMAGIA = EFEITOS.register("antimagia",
            () -> new EfeitoSimples(MobEffectCategory.BENEFICIAL, 0x00FF00));
    // Vidência (pergaminho da vidência): acha sozinho os segredos até 8 quadradinhos
    public static final DeferredHolder<MobEffect, EfeitoSimples> VIDENCIA = EFEITOS.register("videncia",
            () -> new EfeitoSimples(MobEffectCategory.BENEFICIAL, 0x99DDFF));
    // Os elixires: sangue de dragão (fogo nos golpes, imune a fogo), toque gélido (frio nos golpes, imune a frio),
    // essência tóxica (solta gás tóxico, imune a ele e a veneno), queda suave (cair não machuca),
    // armadura arcana (diminui o dano mágico; perde 1 nível a cada 80 turnos) e rejuvenescimento aquático (cura na água)
    public static final DeferredHolder<MobEffect, EfeitoSimples> FOGO_INTERIOR = EFEITOS.register("fogo_interior",
            () -> new EfeitoSimples(MobEffectCategory.BENEFICIAL, 0xFF8800));
    public static final DeferredHolder<MobEffect, EfeitoSimples> FRIO_INTERIOR = EFEITOS.register("frio_interior",
            () -> new EfeitoSimples(MobEffectCategory.BENEFICIAL, 0x66CCFF));
    public static final DeferredHolder<MobEffect, EfeitoSimples> ESSENCIA_TOXICA = EFEITOS.register("essencia_toxica",
            () -> new EfeitoSimples(MobEffectCategory.BENEFICIAL, 0x99CC00));
    public static final DeferredHolder<MobEffect, EfeitoSimples> QUEDA_SUAVE = EFEITOS.register("queda_suave",
            () -> new EfeitoSimples(MobEffectCategory.BENEFICIAL, 0xDDFFEE));
    public static final DeferredHolder<MobEffect, EfeitoSimples> ARMADURA_ARCANA = EFEITOS.register("armadura_arcana",
            () -> new EfeitoSimples(MobEffectCategory.BENEFICIAL, 0x9966FF));
    public static final DeferredHolder<MobEffect, EfeitoSimples> REJUVENESCIMENTO = EFEITOS.register("rejuvenescimento",
            () -> new EfeitoSimples(MobEffectCategory.BENEFICIAL, 0x22BBBB));
    // Arena de desafio (pergaminho do desafio): dentro dela, o herói toma 67% do dano e não fica com fome
    public static final DeferredHolder<MobEffect, EfeitoSimples> DESAFIO = EFEITOS.register("desafio",
            () -> new EfeitoSimples(MobEffectCategory.BENEFICIAL, 0xFF0000));
    // Pavor (pergaminho do pavor): foge e, longe e fora da vista do herói, some da dungeon
    public static final DeferredHolder<MobEffect, EfeitoSimples> PAVOR = EFEITOS.register("pavor",
            () -> new EfeitoSimples(MobEffectCategory.HARMFUL, 0xAA0000));
    // Dominado (pergaminho do canto da sereia): aliado pra sempre (sem perder vida, diferente do corrompido)
    public static final DeferredHolder<MobEffect, EfeitoSimples> DOMINADO = EFEITOS.register("dominado",
            () -> new EfeitoSimples(MobEffectCategory.NEUTRAL, 0xFF66CC));
    // Bem alimentado (torta de carne): sem fome, e cura 1 de vida a cada 18 turnos
    public static final DeferredHolder<MobEffect, EfeitoSimples> BEM_ALIMENTADO = EFEITOS.register("bem_alimentado",
            () -> new EfeitoSimples(MobEffectCategory.BENEFICIAL, 0xC89650));
    // Alvo (pedra da agressão): todos os inimigos que virem preferem atacar quem tem isso
    public static final DeferredHolder<MobEffect, EfeitoSimples> AGRESSAO = EFEITOS.register("agressao",
            () -> new EfeitoSimples(MobEffectCategory.HARMFUL, 0xFF8822));
    // Furioso (pergaminho de fúria): ataca quem estiver mais perto, até os outros inimigos
    public static final DeferredHolder<MobEffect, EfeitoSimples> FURIOSO = EFEITOS.register("furioso",
            () -> new EfeitoSimples(MobEffectCategory.HARMFUL, 0xFF6600));
    // Veneno do Shattered (musgo-do-pesar): o dano por turno vai caindo com o tempo, e pode matar
    public static final DeferredHolder<MobEffect, EfeitoSimples> VENENO = EFEITOS.register("veneno",
            () -> new EfeitoSimples(MobEffectCategory.HARMFUL, 0x8844AA));
    // Sangrando (maldição sacrificial): dano que vai diminuindo
    public static final DeferredHolder<MobEffect, EfeitoSimples> SANGRANDO = EFEITOS.register("sangrando",
            () -> new EfeitoSimples(MobEffectCategory.HARMFUL, 0xAA0000));
    // Aleijado (erva-cega): anda pela metade
    public static final DeferredHolder<MobEffect, EfeitoSimples> ALEIJADO = EFEITOS.register("aleijado",
            () -> EfeitoSimples.queMudaAVelocidade(MobEffectCategory.HARMFUL, 0x996633, "aleijado", -0.5));
    // Resfriado (varinha da geada): anda mais devagar
    public static final DeferredHolder<MobEffect, EfeitoSimples> RESFRIADO = EFEITOS.register("resfriado",
            () -> EfeitoSimples.queMudaAVelocidade(MobEffectCategory.HARMFUL, 0x88CCFF, "resfriado", -0.33));
    // Encantado (varinha da transfusão): não ataca
    public static final DeferredHolder<MobEffect, EfeitoSimples> ENCANTADO = EFEITOS.register("encantado",
            () -> new EfeitoSimples(MobEffectCategory.HARMFUL, 0xFF66CC));
    // Cego (erva-cega, retribuição, maldição ofuscante): inimigos cegos perdem o alvo
    public static final DeferredHolder<MobEffect, EfeitoSimples> CEGO = EFEITOS.register("cego",
            () -> new EfeitoSimples(MobEffectCategory.HARMFUL, 0x222222));
    // Vertigem (videira-da-tempestade, poção de levitação arremessada): inimigos andam pra qualquer lado
    public static final DeferredHolder<MobEffect, EfeitoSimples> VERTIGEM = EFEITOS.register("vertigem",
            () -> new EfeitoSimples(MobEffectCategory.HARMFUL, 0x99CC99));
    // Desviante (maldição): golpes muito menos precisos
    public static final DeferredHolder<MobEffect, EfeitoSimples> DESVIANTE = EFEITOS.register("desviante",
            () -> new EfeitoSimples(MobEffectCategory.HARMFUL, 0x555555));

    // Curando (poção de cura): cura um quarto do que falta a cada turno
    public static final DeferredHolder<MobEffect, EfeitoSimples> CURANDO = EFEITOS.register("curando",
            () -> new EfeitoSimples(MobEffectCategory.BENEFICIAL, 0xFF5555));
    // Saúde da grama-do-sol: cura aos pouquinhos enquanto não sai do lugar
    public static final DeferredHolder<MobEffect, EfeitoSimples> SAUDE_DA_GRAMA = EFEITOS.register("saude_da_grama",
            () -> new EfeitoSimples(MobEffectCategory.BENEFICIAL, 0x88FF88));
    // Raiz-da-terra: segura parte de cada golpe enquanto não sai do lugar
    public static final DeferredHolder<MobEffect, EfeitoSimples> RAIZ_DA_TERRA = EFEITOS.register("raiz_da_terra",
            () -> new EfeitoSimples(MobEffectCategory.BENEFICIAL, 0x996633));
    // Pele de árvore (auxílio da natureza): armadura extra (0 até o nível)
    public static final DeferredHolder<MobEffect, EfeitoSimples> PELE_DE_ARVORE = EFEITOS.register("pele_de_arvore",
            () -> new EfeitoSimples(MobEffectCategory.BENEFICIAL, 0x8B5A2B));
    // Abençoado (flor-estrela): +25% de precisão e de esquiva
    public static final DeferredHolder<MobEffect, EfeitoSimples> ABENCOADO = EFEITOS.register("abencoado",
            () -> new EfeitoSimples(MobEffectCategory.BENEFICIAL, 0xFFFF88));
    // Imune a gases (poção de pureza)
    public static final DeferredHolder<MobEffect, EfeitoSimples> IMUNE_A_GAS = EFEITOS.register("imune_a_gas",
            () -> new EfeitoSimples(MobEffectCategory.BENEFICIAL, 0xCCFFFF));
    // Levitando (poção de levitação): dá pra voar
    public static final DeferredHolder<MobEffect, EfeitoSimples> LEVITANDO = EFEITOS.register("levitando",
            () -> new EfeitoSimples(MobEffectCategory.BENEFICIAL, 0xCCCCFF));
    // Pressa (poção de pressa): anda 3x mais rápido
    public static final DeferredHolder<MobEffect, EfeitoSimples> PRESSA = EFEITOS.register("pressa",
            () -> EfeitoSimples.queMudaAVelocidade(MobEffectCategory.BENEFICIAL, 0xFFCC44, "pressa", 2.0));
    // Enraizado (natureza líquida da Caçadora): não sai do lugar, mas ainda ataca
    public static final DeferredHolder<MobEffect, EfeitoSimples> ENRAIZADO = EFEITOS.register("enraizado",
            () -> EfeitoSimples.quePrende(MobEffectCategory.HARMFUL, 0x5A8A2A, "enraizado"));
    // Recarregando (pergaminho de recarga, refeição energética do Mago): o cajado ganha 1/4 de carga por turno
    public static final DeferredHolder<MobEffect, EfeitoSimples> RECARREGANDO = EFEITOS.register("recarregando",
            () -> new EfeitoSimples(MobEffectCategory.BENEFICIAL, 0xFFFFFF));
    // Recarga de artefato (refeição mística do Ladino): a capa e o tomo ganham 1/4 de carga por turno
    public static final DeferredHolder<MobEffect, EfeitoSimples> RECARGA_DE_ARTEFATO = EFEITOS.register("recarga_de_artefato",
            () -> new EfeitoSimples(MobEffectCategory.BENEFICIAL, 0x88FF88));
    // Agilidade líquida (Duelista): esquiva 3x maior (infinita no +2) e o próximo golpe de perto 3x mais preciso (certeiro no +2)
    public static final DeferredHolder<MobEffect, EfeitoSimples> AGILIDADE_LIQUIDA = EFEITOS.register("agilidade_liquida",
            () -> new EfeitoSimples(MobEffectCategory.BENEFICIAL, 0xB070FF));
    // Sentido divino (magia do Clérigo): os inimigos perto brilham através das paredes
    public static final DeferredHolder<MobEffect, EfeitoSimples> SENTIDO_DIVINO = EFEITOS.register("sentido_divino",
            () -> new EfeitoSimples(MobEffectCategory.BENEFICIAL, 0xFFE65A));

    // Visão mental (poção): vê todos os inimigos do andar através das paredes
    public static final DeferredHolder<MobEffect, EfeitoSimples> VISAO_MENTAL = EFEITOS.register("visao_mental",
            () -> new EfeitoSimples(MobEffectCategory.BENEFICIAL, 0x44CCFF));

    // Corrompido (encantamento e varinha de corrupção): o inimigo virou aliado dos heróis
    public static final DeferredHolder<MobEffect, EfeitoSimples> CORROMPIDO = EFEITOS.register("corrompido",
            () -> new EfeitoSimples(MobEffectCategory.NEUTRAL, 0x4B0F4B));

    // Dano conservado (encantamento inercial): o dano que sobrou ao matar vai pro próximo golpe
    public static final DeferredHolder<MobEffect, EfeitoSimples> DANO_CONSERVADO = EFEITOS.register("dano_conservado",
            () -> new EfeitoSimples(MobEffectCategory.BENEFICIAL, 0xFFAA33));

    // Dano adiado (glifo da viscosidade): o dano guardado vem 10% por turno
    public static final DeferredHolder<MobEffect, EfeitoSimples> DANO_ADIADO = EFEITOS.register("dano_adiado",
            () -> new EfeitoSimples(MobEffectCategory.HARMFUL, 0xA0A0A0));

    // ---- Os efeitos ruins da varinha de corrupção ----

    // Fraco: causa 33% menos dano
    public static final DeferredHolder<MobEffect, EfeitoSimples> FRACO = EFEITOS.register("fraco",
            () -> new EfeitoSimples(MobEffectCategory.HARMFUL, 0x998866));
    // Vulnerável: leva 33% mais dano dos golpes (depois da armadura)
    public static final DeferredHolder<MobEffect, EfeitoSimples> VULNERAVEL = EFEITOS.register("vulneravel",
            () -> new EfeitoSimples(MobEffectCategory.HARMFUL, 0xBB9955));
    // Lento: anda pela metade e ataca a cada 2 turnos
    public static final DeferredHolder<MobEffect, EfeitoSimples> LENTO = EFEITOS.register("lento",
            () -> EfeitoSimples.queMudaAVelocidade(MobEffectCategory.HARMFUL, 0xFF5533, "lento", -0.5));
    // Feitiço: precisão e esquiva 20% menores
    public static final DeferredHolder<MobEffect, EfeitoSimples> FEITICO = EFEITOS.register("feitico",
            () -> new EfeitoSimples(MobEffectCategory.HARMFUL, 0x9933CC));
    // Condenado (o chefe não pode ser corrompido, então fica condenado): leva 67% mais de qualquer dano
    public static final DeferredHolder<MobEffect, EfeitoSimples> CONDENADO = EFEITOS.register("condenado",
            () -> new EfeitoSimples(MobEffectCategory.HARMFUL, 0x330033));

    // ---- As habilidades das armas da Duelista ----

    // Postura defensiva (bastão): esquiva 3x maior
    public static final DeferredHolder<MobEffect, EfeitoSimples> POSTURA_DEFENSIVA = EFEITOS.register("postura_defensiva",
            () -> new EfeitoSimples(MobEffectCategory.BENEFICIAL, 0x66AAFF));
    // Dança da espada (cimitarra): 60% mais rápida e 50% mais precisa
    public static final DeferredHolder<MobEffect, EfeitoSimples> DANCA_DA_ESPADA = EFEITOS.register("danca_da_espada",
            () -> EfeitoSimples.queMudaAVelocidadeDeAtaque(MobEffectCategory.BENEFICIAL, 0xFFCC44, "danca_da_espada", 0.6));
    // Defendendo (escudos): todo ataque contra a Duelista é anulado
    public static final DeferredHolder<MobEffect, EfeitoSimples> DEFENDENDO = EFEITOS.register("defendendo",
            () -> new EfeitoSimples(MobEffectCategory.BENEFICIAL, 0xC0C0C0));
    // Girando (mangual): o próximo golpe acerta com dano a mais por volta
    public static final DeferredHolder<MobEffect, EfeitoSimples> GIRANDO = EFEITOS.register("girando",
            () -> new EfeitoSimples(MobEffectCategory.BENEFICIAL, 0x88FF44));
    // Carregado (besta): o próximo golpe acerta e arremessa o inimigo
    public static final DeferredHolder<MobEffect, EfeitoSimples> CARREGADO = EFEITOS.register("carregado",
            () -> new EfeitoSimples(MobEffectCategory.BENEFICIAL, 0xFFAA33));

    // Armadura de pedra (varinha de terra viva): segura metade de cada golpe até a pedra acabar
    public static final DeferredHolder<MobEffect, EfeitoSimples> ARMADURA_DE_PEDRA = EFEITOS.register("armadura_de_pedra",
            () -> new EfeitoSimples(MobEffectCategory.BENEFICIAL, 0x806040));

    // ---- A ampulheta do tempo ----

    // Estase: o herói sai do tempo (não se mexe, não age, nada o machuca nem pega nele)
    public static final DeferredHolder<MobEffect, EfeitoSimples> ESTASE = EFEITOS.register("estase",
            () -> EfeitoSimples.quePrende(MobEffectCategory.BENEFICIAL, 0xFFD966, "estase"));
    // Tempo parado: o herói anda e os inimigos em volta não (eles ficam "parados no tempo")
    public static final DeferredHolder<MobEffect, EfeitoSimples> TEMPO_PARADO = EFEITOS.register("tempo_parado",
            () -> new EfeitoSimples(MobEffectCategory.BENEFICIAL, 0xCCCCFF));
    public static final DeferredHolder<MobEffect, EfeitoSimples> PARADO_NO_TEMPO = EFEITOS.register("parado_no_tempo",
            () -> EfeitoSimples.quePrende(MobEffectCategory.NEUTRAL, 0x8899FF, "parado_no_tempo"));
}
