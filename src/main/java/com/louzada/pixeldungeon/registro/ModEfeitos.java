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

    // Dano adiado (glifo da viscosidade): o dano guardado vem 10% por turno
    public static final DeferredHolder<MobEffect, EfeitoSimples> DANO_ADIADO = EFEITOS.register("dano_adiado",
            () -> new EfeitoSimples(MobEffectCategory.HARMFUL, 0xA0A0A0));
}
