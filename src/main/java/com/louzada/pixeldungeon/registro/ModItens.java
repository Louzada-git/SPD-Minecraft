package com.louzada.pixeldungeon.registro;

import java.util.ArrayList;
import java.util.List;

import com.louzada.pixeldungeon.PixelDungeon;
import com.louzada.pixeldungeon.heroi.HabilidadesDaDuelista.Habilidade;
import com.louzada.pixeldungeon.item.ArcoEspiritual;
import com.louzada.pixeldungeon.item.ArmaDeArremesso.Especial;
import com.louzada.pixeldungeon.item.ArmaDeArremesso;
import com.louzada.pixeldungeon.item.ArmaDoShattered.Numeros;
import com.louzada.pixeldungeon.item.ArmaDoShattered;
import com.louzada.pixeldungeon.item.ArmaduraDoShattered;
import com.louzada.pixeldungeon.item.ArtefatoAchado;
import com.louzada.pixeldungeon.item.Baga;
import com.louzada.pixeldungeon.item.CajadoDoMago;
import com.louzada.pixeldungeon.item.CapaDasSombras;
import com.louzada.pixeldungeon.item.ChaveDeCristal;
import com.louzada.pixeldungeon.item.ChaveDeFerro;
import com.louzada.pixeldungeon.item.ComidaDoShattered;
import com.louzada.pixeldungeon.item.CorDePocao;
import com.louzada.pixeldungeon.item.CristalDeEnergia;
import com.louzada.pixeldungeon.item.FrutaSemGraca;
import com.louzada.pixeldungeon.item.GemaDeAnel;
import com.louzada.pixeldungeon.item.ItemAcessorio;
import com.louzada.pixeldungeon.item.ItemAnel;
import com.louzada.pixeldungeon.item.ItemAnkh;
import com.louzada.pixeldungeon.item.ItemBomba;
import com.louzada.pixeldungeon.item.ItemCarneMisteriosa;
import com.louzada.pixeldungeon.item.ItemCatalisador;
import com.louzada.pixeldungeon.item.ItemComDescricao;
import com.louzada.pixeldungeon.item.ItemElixir;
import com.louzada.pixeldungeon.item.ItemFeitico;
import com.louzada.pixeldungeon.item.ItemInfusao;
import com.louzada.pixeldungeon.item.ItemPedraRunica;
import com.louzada.pixeldungeon.item.ItemPergaminho;
import com.louzada.pixeldungeon.item.ItemPergaminhoExotico;
import com.louzada.pixeldungeon.item.ItemPocao;
import com.louzada.pixeldungeon.item.ItemPocaoExotica;
import com.louzada.pixeldungeon.item.ItemSemente;
import com.louzada.pixeldungeon.item.ItemVarinha;
import com.louzada.pixeldungeon.item.MetalLiquido;
import com.louzada.pixeldungeon.item.Odre;
import com.louzada.pixeldungeon.item.Ouro;
import com.louzada.pixeldungeon.item.PaginaDoGuia;
import com.louzada.pixeldungeon.item.PetalaSeca;
import com.louzada.pixeldungeon.item.PoteDeMel;
import com.louzada.pixeldungeon.item.PoteQuebrado;
import com.louzada.pixeldungeon.item.RacaoDeSuprimentos;
import com.louzada.pixeldungeon.item.ResinaArcana;
import com.louzada.pixeldungeon.item.RunaDePergaminho;
import com.louzada.pixeldungeon.item.SeloQuebrado;
import com.louzada.pixeldungeon.item.TipoDeAcessorio;
import com.louzada.pixeldungeon.item.TipoDeBomba;
import com.louzada.pixeldungeon.item.TipoDeElixir;
import com.louzada.pixeldungeon.item.TipoDeFeitico;
import com.louzada.pixeldungeon.item.TipoDeInfusao;
import com.louzada.pixeldungeon.item.TipoDePedra;
import com.louzada.pixeldungeon.item.TipoDePlanta;
import com.louzada.pixeldungeon.item.TipoDeVarinha;
import com.louzada.pixeldungeon.item.TomoSagrado;
import com.louzada.pixeldungeon.item.artefato.AmpulhetaDoTempo;
import com.louzada.pixeldungeon.item.artefato.BracadeiraDoLadrao;
import com.louzada.pixeldungeon.item.artefato.CaliceDeSangue;
import com.louzada.pixeldungeon.item.artefato.ChaveDeCranio;
import com.louzada.pixeldungeon.item.artefato.ChifreDaFartura;
import com.louzada.pixeldungeon.item.artefato.CorrentesEtereas;
import com.louzada.pixeldungeon.item.artefato.KitDoAlquimista;
import com.louzada.pixeldungeon.item.artefato.LivroInstavel;
import com.louzada.pixeldungeon.item.artefato.RosaSeca;
import com.louzada.pixeldungeon.item.artefato.SandaliasDaNatureza;
import com.louzada.pixeldungeon.item.artefato.TalismaDaVidencia;

import net.minecraft.core.Holder;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.DoubleHighBlockItem;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.common.DeferredSpawnEggItem;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

// Todos os itens do mod ficam registrados aqui.
public class ModItens {
    public static final DeferredRegister.Items ITENS = DeferredRegister.createItems(PixelDungeon.MODID);

    // ---- Armas (números do Shattered) ----
    // Cada arma começa com os números padrão do tier (dano de tier até 5 x (tier + 1)) e muda o que tem de diferente.
    // A habilidade é a que a Duelista usa com ela, e o número é o dano a mais (no furtivo, o alcance em quadradinhos).

    // Tier 1: as armas iniciais das classes
    public static final DeferredItem<ArmaDoShattered> ESPADA_CURTA_DESGASTADA = arma("espada_curta_desgastada",
            Numeros.doTier(1).habilidade(Habilidade.CORTAR, 3));
    // Também lança mísseis mágicos com o clique direito (vem com a varinha de míssil mágico dentro)
    public static final DeferredItem<CajadoDoMago> CAJADO_DO_MAGO = ITENS.registerItem("cajado_do_mago", CajadoDoMago::new);
    // Ataque surpresa quase sempre no dano máximo
    public static final DeferredItem<ArmaDoShattered> ADAGA = arma("adaga", Numeros.doTier(1).dano(1, 8).surpresa(0.75f).habilidade(Habilidade.FURTIVO, 5));
    // Ataca 2x mais rápido, mas bate fraco
    public static final DeferredItem<ArmaDoShattered> LUVAS_CRAVEJADAS = arma("luvas_cravejadas",
            Numeros.doTier(1).dano(1, 5).rapida().habilidade(Habilidade.COMBINACAO, 3));
    // Dá 1 de defesa extra
    public static final DeferredItem<ArmaDoShattered> FLORETE = arma("florete", Numeros.doTier(1).dano(1, 8).defesa(1).habilidade(Habilidade.ESTOCADA, 5));
    // 40% a mais de precisão
    public static final DeferredItem<ArmaDoShattered> MACA = arma("maca", Numeros.doTier(1).dano(1, 8).precisao(1.4f).habilidade(Habilidade.GOLPE_PESADO, 3));

    // Tier 2 (precisam de 12 de força)
    public static final DeferredItem<ArmaDoShattered> ESPADA_CURTA = arma("espada_curta", Numeros.doTier(2).habilidade(Habilidade.CORTAR, 4));
    public static final DeferredItem<ArmaDoShattered> MACHADINHA = arma("machadinha",
            Numeros.doTier(2).dano(2, 12).precisao(1.32f).habilidade(Habilidade.GOLPE_PESADO, 4));
    public static final DeferredItem<ArmaDoShattered> PUNHAL = arma("punhal", Numeros.doTier(2).dano(2, 12).surpresa(0.67f).habilidade(Habilidade.FURTIVO, 4));
    // Lenta, mas alcança 1 quadradinho a mais
    public static final DeferredItem<ArmaDoShattered> LANCA = arma("lanca",
            Numeros.doTier(2).dano(2, 20, 4).atraso(1.5f).alcance(1).habilidade(Habilidade.ESPETAR, 9, 2));
    public static final DeferredItem<ArmaDoShattered> BASTAO = arma("bastao",
            Numeros.doTier(2).dano(2, 12).defesa(2).habilidade(Habilidade.POSTURA_DEFENSIVA, 4, 1));
    public static final DeferredItem<ArmaDoShattered> FOICE = arma("foice", Numeros.doTier(2).dano(2, 20).precisao(0.68f).habilidade(Habilidade.CEIFAR, 15, 2.5f));

    // Tier 3 (14 de força)
    public static final DeferredItem<ArmaDoShattered> ESPADA = arma("espada", Numeros.doTier(3).habilidade(Habilidade.CORTAR, 5));
    public static final DeferredItem<ArmaDoShattered> CLAVA = arma("clava", Numeros.doTier(3).dano(3, 16).precisao(1.28f).habilidade(Habilidade.GOLPE_PESADO, 5));
    public static final DeferredItem<ArmaDoShattered> SAI = arma("sai", Numeros.doTier(3).dano(3, 10).rapida().habilidade(Habilidade.COMBINACAO, 4));
    public static final DeferredItem<ArmaDoShattered> CIMITARRA = arma("cimitarra",
            Numeros.doTier(3).dano(3, 16).atraso(0.8f).habilidade(Habilidade.DANCA_DA_ESPADA, 4, 1));
    // Bate fraco, mas defende 0-4 (+1 por nível)
    public static final DeferredItem<ArmaDoShattered> ESCUDO_REDONDO = arma("escudo_redondo",
            Numeros.doTier(3).dano(3, 12, 2).defesa(4, 1).habilidade(Habilidade.DEFENDER, 5, 1));
    // Alcança 2 quadradinhos a mais
    public static final DeferredItem<ArmaDoShattered> CHICOTE = arma("chicote", Numeros.doTier(3).dano(3, 15, 3).alcance(2).habilidade(Habilidade.CHICOTEAR, 0, 0));

    // Tier 4 (16 de força)
    public static final DeferredItem<ArmaDoShattered> KATANA = arma("katana", Numeros.doTier(4).dano(4, 20).defesa(3).habilidade(Habilidade.ESTOCADA, 8));
    public static final DeferredItem<ArmaDoShattered> LAMINA_ASSASSINA = arma("lamina_assassina",
            Numeros.doTier(4).dano(4, 20).surpresa(0.5f).habilidade(Habilidade.FURTIVO, 3));
    public static final DeferredItem<ArmaDoShattered> ESPADA_LONGA = arma("espada_longa", Numeros.doTier(4).habilidade(Habilidade.CORTAR, 6));
    public static final DeferredItem<ArmaDoShattered> MACHADO_DE_BATALHA = arma("machado_de_batalha",
            Numeros.doTier(4).dano(4, 20).precisao(1.24f).habilidade(Habilidade.GOLPE_PESADO, 5));
    // Pouco preciso e não ataca de surpresa, mas bate muito forte
    public static final DeferredItem<ArmaDoShattered> MANGUAL = arma("mangual",
            Numeros.doTier(4).dano(4, 35, 8).precisao(0.8f).semSurpresa().habilidade(Habilidade.GIRAR, 8, 2));
    // Dano de tier 3, mas cresce como um tier 5
    public static final DeferredItem<ArmaDoShattered> LAMINA_RUNICA = arma("lamina_runica",
            Numeros.doTier(4).dano(4, 20, 6).habilidade(Habilidade.CORTE_RUNICO, 300, 50));
    public static final DeferredItem<ArmaDoShattered> BESTA = arma("besta", Numeros.doTier(4).dano(4, 20, 4).habilidade(Habilidade.TIRO_CARREGADO, 0, 0));

    // Tier 5 (18 de força)
    public static final DeferredItem<ArmaDoShattered> ESPADA_LARGA = arma("espada_larga", Numeros.doTier(5).habilidade(Habilidade.CORTAR, 7));
    public static final DeferredItem<ArmaDoShattered> MARTELO_DE_GUERRA = arma("martelo_de_guerra",
            Numeros.doTier(5).dano(5, 24).precisao(1.2f).habilidade(Habilidade.GOLPE_PESADO, 6));
    // Lento, mas alcança 1 quadradinho a mais
    public static final DeferredItem<ArmaDoShattered> GLADIO = arma("gladio",
            Numeros.doTier(5).dano(5, 40, 8).atraso(1.5f).alcance(1).habilidade(Habilidade.ESPETAR, 12, 2.5f));
    // Muito pesado: pede a força do tier 6 (20)
    public static final DeferredItem<ArmaDoShattered> MACHADO_GRANDE = arma("machado_grande",
            Numeros.doTier(5).dano(5, 45).forcaDoTier(6).habilidade(Habilidade.RETRIBUICAO, 15, 2));
    // Bate fraco, mas defende 0-6 (+2 por nível)
    public static final DeferredItem<ArmaDoShattered> ESCUDO_GRANDE = arma("escudo_grande",
            Numeros.doTier(5).dano(5, 18, 4).defesa(6, 2).habilidade(Habilidade.DEFENDER, 3, 1));
    public static final DeferredItem<ArmaDoShattered> MANOPLA_DE_PEDRA = arma("manopla_de_pedra",
            Numeros.doTier(5).dano(5, 15).rapida().habilidade(Habilidade.COMBINACAO, 5));
    public static final DeferredItem<ArmaDoShattered> FOICE_DE_GUERRA = arma("foice_de_guerra",
            Numeros.doTier(5).dano(5, 40).precisao(0.8f).habilidade(Habilidade.CEIFAR, 30, 4.5f));

    // ---- Armas de arremesso (números do Shattered: dano de 2 x tier a 5 x tier, 1 de força a menos) ----
    // Usos: quantos acertos o conjunto aguenta antes de uma quebrar. As que não grudam caem no chão depois do acerto.

    // Tier 1: as das classes (Guerreiro, Ladino e Duelista começam com elas)
    public static final DeferredItem<ArmaDeArremesso> PEDRA_DE_ARREMESSO = arremesso("pedra_de_arremesso",
            ArmaDeArremesso.Numeros.doTier(1).usos(5).naoGruda());
    public static final DeferredItem<ArmaDeArremesso> FACA_DE_ARREMESSO = arremesso("faca_de_arremesso",
            ArmaDeArremesso.Numeros.doTier(1).dano(2, 6).crescimento(1, 2).usos(5).surpresa(0.75f));
    public static final DeferredItem<ArmaDeArremesso> AGULHA_DE_ARREMESSO = arremesso("agulha_de_arremesso", ArmaDeArremesso.Numeros.doTier(1).usos(12));

    // Tier 2
    public static final DeferredItem<ArmaDeArremesso> LANCA_DE_PESCA = arremesso("lanca_de_pesca", ArmaDeArremesso.Numeros.doTier(2));
    public static final DeferredItem<ArmaDeArremesso> BASTAO_DE_ARREMESSO = arremesso("bastao_de_arremesso",
            ArmaDeArremesso.Numeros.doTier(2).dano(4, 8).usos(12).naoGruda());
    public static final DeferredItem<ArmaDeArremesso> SHURIKEN = arremesso("shuriken",
            ArmaDeArremesso.Numeros.doTier(2).dano(4, 8).usos(5).especial(Especial.INSTANTANEA));

    // Tier 3
    public static final DeferredItem<ArmaDeArremesso> LANCA_DE_ARREMESSO = arremesso("lanca_de_arremesso", ArmaDeArremesso.Numeros.doTier(3));
    public static final DeferredItem<ArmaDeArremesso> KUNAI = arremesso("kunai", ArmaDeArremesso.Numeros.doTier(3).dano(6, 12).surpresa(0.6f));
    public static final DeferredItem<ArmaDeArremesso> BOLEADEIRAS = arremesso("boleadeiras",
            ArmaDeArremesso.Numeros.doTier(3).dano(4, 9).crescimento(0, 2).usos(5).especial(Especial.ALEIJA));

    // Tier 4
    public static final DeferredItem<ArmaDeArremesso> JAVELIN = arremesso("javelin", ArmaDeArremesso.Numeros.doTier(4));
    public static final DeferredItem<ArmaDeArremesso> TOMAHAWK = arremesso("tomahawk",
            ArmaDeArremesso.Numeros.doTier(4).dano(6, 16).crescimento(1, 3).usos(5).especial(Especial.SANGRA));
    public static final DeferredItem<ArmaDeArremesso> BUMERANGUE_PESADO = arremesso("bumerangue_pesado",
            ArmaDeArremesso.Numeros.doTier(4).dano(8, 16).crescimento(1, 3).usos(5).naoGruda().especial(Especial.VOLTA));

    // Tier 5
    public static final DeferredItem<ArmaDeArremesso> TRIDENTE = arremesso("tridente", ArmaDeArremesso.Numeros.doTier(5));
    public static final DeferredItem<ArmaDeArremesso> MARTELO_DE_ARREMESSO = arremesso("martelo_de_arremesso",
            ArmaDeArremesso.Numeros.doTier(5).dano(10, 20).usos(12).naoGruda());
    public static final DeferredItem<ArmaDeArremesso> CUBO_DE_FORCA = arremesso("cubo_de_forca",
            ArmaDeArremesso.Numeros.doTier(5).dano(10, 30).usos(5).naoGruda().especial(Especial.EM_AREA));

    // ---- Varinhas (uma pra cada tipo, na ordem do enum) ----

    public static final List<DeferredItem<ItemVarinha>> VARINHAS = registrarVarinhas();

    // ---- Anéis (um pra cada gema; o tipo que cada gema esconde muda a cada run) ----

    public static final List<DeferredItem<ItemAnel>> ANEIS = registrarAneis();

    // ---- Itens únicos das classes ----

    // Ladino: fica invisível
    public static final DeferredItem<CapaDasSombras> CAPA_DAS_SOMBRAS = ITENS.registerItem("capa_das_sombras", CapaDasSombras::new);
    // Caçadora: flechas infinitas
    public static final DeferredItem<ArcoEspiritual> ARCO_ESPIRITUAL = ITENS.registerItem("arco_espiritual", ArcoEspiritual::new);
    // Clérigo: magias
    public static final DeferredItem<TomoSagrado> TOMO_SAGRADO = ITENS.registerItem("tomo_sagrado", TomoSagrado::new);
    // Guerreiro: o selo quebrado, quando é solto da armadura
    public static final DeferredItem<SeloQuebrado> SELO_QUEBRADO = ITENS.registerItem("selo_quebrado", SeloQuebrado::new);
    // A flecha que o arco solta (não dá pra pegar; só serve pra desenhar a flecha voando)
    public static final DeferredItem<Item> FLECHA_ESPIRITUAL = ITENS.registerSimpleItem("flecha_espiritual");

    // ---- Os artefatos que se acha pela dungeon (cada um aparece uma vez por run) ----

    public static final DeferredItem<CaliceDeSangue> CALICE_DE_SANGUE = ITENS.registerItem("calice_de_sangue", CaliceDeSangue::new);
    public static final DeferredItem<ChifreDaFartura> CHIFRE_DA_FARTURA = ITENS.registerItem("chifre_da_fartura", ChifreDaFartura::new);
    public static final DeferredItem<AmpulhetaDoTempo> AMPULHETA_DO_TEMPO = ITENS.registerItem("ampulheta_do_tempo", AmpulhetaDoTempo::new);
    public static final DeferredItem<LivroInstavel> LIVRO_INSTAVEL = ITENS.registerItem("livro_instavel", LivroInstavel::new);
    public static final DeferredItem<CorrentesEtereas> CORRENTES_ETEREAS = ITENS.registerItem("correntes_etereas", CorrentesEtereas::new);
    public static final DeferredItem<TalismaDaVidencia> TALISMA_DA_VIDENCIA = ITENS.registerItem("talisma_da_videncia", TalismaDaVidencia::new);
    public static final DeferredItem<BracadeiraDoLadrao> BRACADEIRA_DO_LADRAO = ITENS.registerItem("bracadeira_do_ladrao", BracadeiraDoLadrao::new);
    public static final DeferredItem<SandaliasDaNatureza> SANDALIAS_DA_NATUREZA = ITENS.registerItem("sandalias_da_natureza", SandaliasDaNatureza::new);
    public static final DeferredItem<ChaveDeCranio> CHAVE_DE_CRANIO = ITENS.registerItem("chave_de_cranio", ChaveDeCranio::new);
    // A rosa seca só funciona depois da missão do fantasma; as pétalas sobem o nível dela
    public static final DeferredItem<RosaSeca> ROSA_SECA = ITENS.registerItem("rosa_seca", RosaSeca::new);
    public static final DeferredItem<PetalaSeca> PETALA_SECA = ITENS.registerItem("petala_seca", PetalaSeca::new);
    // O kit do alquimista: a alquimia sem pote
    public static final DeferredItem<KitDoAlquimista> KIT_DO_ALQUIMISTA = ITENS.registerItem("kit_do_alquimista", KitDoAlquimista::new);

    public static List<DeferredItem<? extends ArtefatoAchado>> artefatos() {
        return List.of(CALICE_DE_SANGUE, CHIFRE_DA_FARTURA, AMPULHETA_DO_TEMPO, LIVRO_INSTAVEL, CORRENTES_ETEREAS, TALISMA_DA_VIDENCIA,
                BRACADEIRA_DO_LADRAO, SANDALIAS_DA_NATUREZA, CHAVE_DE_CRANIO, ROSA_SECA, KIT_DO_ALQUIMISTA);
    }

    // Odre: todo herói começa com um; guarda as gotas de orvalho
    public static final DeferredItem<Odre> ODRE = ITENS.registerItem("odre", Odre::new);
    // A gota de orvalho voando (não dá pra pegar; ela vai sozinha pro odre)
    public static final DeferredItem<Item> GOTA_DE_ORVALHO = ITENS.registerSimpleItem("gota_de_orvalho");

    // Ouro: as moedas vão direto pra bolsa do herói quando ele encosta na pilha
    public static final DeferredItem<Ouro> OURO = ITENS.registerItem("ouro", Ouro::new);

    // ---- Armaduras (números do Shattered: defesa 0 até 2 x tier, força 10 a 18) ----

    // Pano (0-2): todas as classes começam com ela
    public static final DeferredItem<ArmaduraDoShattered> ARMADURA_DE_PANO = armadura("armadura_de_pano", ModMateriais.PANO, 1);
    // Couro (0-4), malha (0-6), escamas (0-8) e placas (0-10)
    public static final DeferredItem<ArmaduraDoShattered> ARMADURA_DE_COURO = armadura("armadura_de_couro", ModMateriais.COURO, 2);
    public static final DeferredItem<ArmaduraDoShattered> ARMADURA_DE_MALHA = armadura("armadura_de_malha", ModMateriais.MALHA, 3);
    public static final DeferredItem<ArmaduraDoShattered> ARMADURA_DE_ESCAMAS = armadura("armadura_de_escamas", ModMateriais.ESCAMAS, 4);
    public static final DeferredItem<ArmaduraDoShattered> ARMADURA_DE_PLACAS = armadura("armadura_de_placas", ModMateriais.PLACAS, 5);

    // ---- Comida ----

    // Ração do Shattered: mata bastante a fome
    public static final DeferredItem<Item> RACAO = ITENS.registerSimpleItem("racao",
            new Item.Properties().food(new FoodProperties.Builder().nutrition(10).saturationModifier(1.0f).build()));

    // Carne misteriosa: cai de caranguejos e ratos albinos (comer é arriscado)
    public static final DeferredItem<ItemCarneMisteriosa> CARNE_MISTERIOSA = ITENS.registerItem("carne_misteriosa", ItemCarneMisteriosa::new);

    // Baga: cai da grama alta pra Caçadora com o talento recompensa da natureza
    public static final DeferredItem<Baga> BAGA = ITENS.registerItem("baga", Baga::new);

    // Ração de suprimentos: aparece em baús pro Ladino com o talento rações guardadas
    public static final DeferredItem<RacaoDeSuprimentos> RACAO_DE_SUPRIMENTOS = ITENS.registerItem("racao_de_suprimentos", RacaoDeSuprimentos::new);

    // As comidas da alquimia (ver ComidaDoShattered e ReceitasDeComida)
    public static final DeferredItem<ComidaDoShattered> CARNE_GRELHADA = ITENS.registerItem("carne_grelhada",
            propriedades -> new ComidaDoShattered(propriedades, "carne_grelhada", 5, false));
    public static final DeferredItem<ComidaDoShattered> ENSOPADO = ITENS.registerItem("ensopado",
            propriedades -> new ComidaDoShattered(propriedades, "ensopado", 5, false));
    public static final DeferredItem<ComidaDoShattered> PASTEL = ITENS.registerItem("pastel",
            propriedades -> new ComidaDoShattered(propriedades, "pastel", 15, false));
    public static final DeferredItem<ComidaDoShattered> TORTA_DE_CARNE = ITENS.registerItem("torta_de_carne",
            propriedades -> new ComidaDoShattered(propriedades, "torta_de_carne", 30, true));
    public static final DeferredItem<ComidaDoShattered> PEDACOS_DE_FRUTA = ITENS.registerItem("pedacos_de_fruta",
            propriedades -> new ComidaDoShattered(propriedades, "pedacos_de_fruta", 15, false));
    public static final DeferredItem<FrutaSemGraca> FRUTA_SEM_GRACA = ITENS.registerItem("fruta_sem_graca", FrutaSemGraca::new);

    // ---- Poções, pergaminhos e sementes do Shattered ----

    // Uma poção pra cada cor, um pergaminho pra cada runa, uma semente pra cada planta (na ordem dos enums).
    // O que cada cor e cada runa faz muda a cada run (ver IdentidadesDaRun).
    public static final List<DeferredItem<ItemPocao>> POCOES = registrarPocoes();
    public static final List<DeferredItem<ItemPergaminho>> PERGAMINHOS = registrarPergaminhos();
    public static final List<DeferredItem<ItemSemente>> SEMENTES = registrarSementes();

    // ---- Alquimia ----

    // Cristal de energia alquímica: vira energia na hora em que o herói encosta
    public static final DeferredItem<CristalDeEnergia> CRISTAL_DE_ENERGIA = ITENS.registerItem("cristal_de_energia", CristalDeEnergia::new);
    // O desenho do que vai sair do pote quando não dá pra saber antes (não é um item de verdade)
    public static final DeferredItem<ItemComDescricao> POCAO_ALEATORIA = ITENS.registerItem("pocao_aleatoria",
            propriedades -> new ItemComDescricao(propriedades, "pocao_aleatoria"));
    public static final DeferredItem<ItemComDescricao> PEDRA_ALEATORIA = ITENS.registerItem("pedra_aleatoria",
            propriedades -> new ItemComDescricao(propriedades, "pedra_aleatoria"));
    // As 12 pedras rúnicas (na ordem do TipoDePedra)
    public static final List<DeferredItem<ItemPedraRunica>> PEDRAS = registrarPedras();
    // As poções exóticas, uma pra cada cor (na ordem do CorDePocao)
    public static final List<DeferredItem<ItemPocaoExotica>> POCOES_EXOTICAS = registrarPocoesExoticas();
    // Os pergaminhos exóticos, um pra cada runa (na ordem do RunaDePergaminho)
    public static final List<DeferredItem<ItemPergaminhoExotico>> PERGAMINHOS_EXOTICOS = registrarPergaminhosExoticos();
    // As bombas: a comum e as melhoradas no pote (na ordem do TipoDeBomba)
    public static final List<DeferredItem<ItemBomba>> BOMBAS = registrarBombas();
    // As infusões (de arremessar) e os elixires (de beber), que saem do pote (na ordem do TipoDeInfusao e do TipoDeElixir)
    public static final List<DeferredItem<ItemInfusao>> INFUSOES = registrarInfusoes();
    public static final List<DeferredItem<ItemElixir>> ELIXIRES = registrarElixires();
    public static final List<DeferredItem<ItemFeitico>> FEITICOS = registrarFeiticos();
    public static final List<DeferredItem<ItemAcessorio>> ACESSORIOS = registrarAcessorios();
    public static final DeferredItem<ItemCatalisador> CATALISADOR_MAGICO = ITENS.registerItem("catalisador_magico", ItemCatalisador::new);
    // Metal líquido (conserta armas de arremesso) e resina arcana (aprimora varinhas), que saem do pote
    public static final DeferredItem<MetalLiquido> METAL_LIQUIDO = ITENS.registerItem("metal_liquido", MetalLiquido::new);
    public static final DeferredItem<ResinaArcana> RESINA_ARCANA = ITENS.registerItem("resina_arcana", ResinaArcana::new);
    // A página rasgada do guia de alquimia (fica nos laboratórios)
    public static final DeferredItem<PaginaDoGuia> PAGINA_DO_GUIA = ITENS.registerItem("pagina_do_guia", PaginaDoGuia::new);

    // ---- Itens especiais ----

    // Traz de volta um aliado que morreu
    public static final DeferredItem<ItemAnkh> ANKH = ITENS.registerItem("ankh", ItemAnkh::new);

    // Chave de ferro: abre uma porta trancada do andar dela
    public static final DeferredItem<ChaveDeFerro> CHAVE_DE_FERRO = ITENS.registerItem("chave_de_ferro", ChaveDeFerro::new);
    public static final DeferredItem<ChaveDeCristal> CHAVE_DE_CRISTAL = ITENS.registerItem("chave_de_cristal", ChaveDeCristal::new);
    // O pote de mel (dele sai uma abelha) e o pote quebrado (a abelha protege ele)
    public static final DeferredItem<PoteDeMel> POTE_DE_MEL = ITENS.registerItem("pote_de_mel", PoteDeMel::new);
    public static final DeferredItem<PoteQuebrado> POTE_QUEBRADO = ITENS.registerItem("pote_quebrado", PoteQuebrado::new);

    // Bolha de gosma: cai do Goo quando ele morre (sozinha não faz nada; vai servir pra alquimia)
    public static final DeferredItem<ItemComDescricao> BOLHA_DE_GOSMA = ITENS.registerItem("bolha_de_gosma",
            propriedades -> new ItemComDescricao(propriedades, "bolha_de_gosma"));

    // ---- Ovos dos inimigos (pra testar no criativo) ----

    public static final DeferredItem<DeferredSpawnEggItem> OVO_RATO_MARSUPIAL = ITENS.registerItem("ovo_rato_marsupial",
            propriedades -> new DeferredSpawnEggItem(ModEntidades.RATO_MARSUPIAL, 0x8C7B6B, 0xD9B8A0, propriedades));
    public static final DeferredItem<DeferredSpawnEggItem> OVO_RATO_ALBINO = ITENS.registerItem("ovo_rato_albino",
            propriedades -> new DeferredSpawnEggItem(ModEntidades.RATO_ALBINO, 0xF2F2F2, 0xE88A9A, propriedades));
    public static final DeferredItem<DeferredSpawnEggItem> OVO_COBRA_DE_ESGOTO = ITENS.registerItem("ovo_cobra_de_esgoto",
            propriedades -> new DeferredSpawnEggItem(ModEntidades.COBRA_DE_ESGOTO, 0x6B8E23, 0xC8A44D, propriedades));
    public static final DeferredItem<DeferredSpawnEggItem> OVO_GNOLL_EXPLORADOR = ITENS.registerItem("ovo_gnoll_explorador",
            propriedades -> new DeferredSpawnEggItem(ModEntidades.GNOLL_EXPLORADOR, 0xB08A55, 0x5C4325, propriedades));
    public static final DeferredItem<DeferredSpawnEggItem> OVO_CARANGUEJO_DE_ESGOTO = ITENS.registerItem("ovo_caranguejo_de_esgoto",
            propriedades -> new DeferredSpawnEggItem(ModEntidades.CARANGUEJO_DE_ESGOTO, 0xC8602B, 0xF0D7A0, propriedades));
    public static final DeferredItem<DeferredSpawnEggItem> OVO_ENXAME_DE_MOSCAS = ITENS.registerItem("ovo_enxame_de_moscas",
            propriedades -> new DeferredSpawnEggItem(ModEntidades.ENXAME_DE_MOSCAS, 0x2B2B2B, 0x7A9E3B, propriedades));
    public static final DeferredItem<DeferredSpawnEggItem> OVO_SLIME = ITENS.registerItem("ovo_slime",
            propriedades -> new DeferredSpawnEggItem(ModEntidades.SLIME, 0x5DBB3A, 0x2E6B1E, propriedades));
    public static final DeferredItem<DeferredSpawnEggItem> OVO_GOO = ITENS.registerItem("ovo_goo",
            propriedades -> new DeferredSpawnEggItem(ModEntidades.GOO, 0x262626, 0x808080, propriedades));
    public static final DeferredItem<DeferredSpawnEggItem> OVO_MIMICO = ITENS.registerItem("ovo_mimico",
            propriedades -> new DeferredSpawnEggItem(ModEntidades.MIMICO, 0x8B5A2B, 0xE0B040, propriedades));

    // ---- Blocos (pra poder pegar no criativo e testar) ----

    public static final DeferredItem<BlockItem> PAREDE_ESGOTO = ITENS.registerSimpleBlockItem("parede_esgoto", ModBlocos.PAREDE_ESGOTO);
    public static final DeferredItem<BlockItem> CHAO_ESGOTO = ITENS.registerSimpleBlockItem("chao_esgoto", ModBlocos.CHAO_ESGOTO);
    public static final DeferredItem<BlockItem> RODAPE_ESGOTO = ITENS.registerSimpleBlockItem("rodape_esgoto", ModBlocos.RODAPE_ESGOTO);
    public static final DeferredItem<BlockItem> PILAR_ESGOTO = ITENS.registerSimpleBlockItem("pilar_esgoto", ModBlocos.PILAR_ESGOTO);
    public static final DeferredItem<BlockItem> MOLDURA_ESGOTO = ITENS.registerSimpleBlockItem("moldura_esgoto", ModBlocos.MOLDURA_ESGOTO);
    public static final DeferredItem<BlockItem> CORNIJA_ESGOTO = ITENS.registerSimpleBlockItem("cornija_esgoto", ModBlocos.CORNIJA_ESGOTO);
    public static final DeferredItem<BlockItem> LANTERNA_ESGOTO = ITENS.registerSimpleBlockItem("lanterna_esgoto", ModBlocos.LANTERNA_ESGOTO);
    public static final DeferredItem<BlockItem> CORRENTE_ESGOTO = ITENS.registerSimpleBlockItem("corrente_esgoto", ModBlocos.CORRENTE_ESGOTO);
    public static final DeferredItem<BlockItem> MUSGO_ESGOTO = ITENS.registerSimpleBlockItem("musgo_esgoto", ModBlocos.MUSGO_ESGOTO);
    public static final DeferredItem<DoubleHighBlockItem> CAPIM_ALTO = ITENS.registerItem("capim_alto",
            propriedades -> new DoubleHighBlockItem(ModBlocos.CAPIM_ALTO.get(), propriedades));
    public static final DeferredItem<BlockItem> GRAMA_BAIXA = ITENS.registerSimpleBlockItem("grama_baixa", ModBlocos.GRAMA_BAIXA);
    public static final DeferredItem<BlockItem> BARRIL_ESGOTO = ITENS.registerSimpleBlockItem("barril_esgoto", ModBlocos.BARRIL_ESGOTO);
    public static final DeferredItem<BlockItem> ESTANTE_ESGOTO = ITENS.registerSimpleBlockItem("estante_esgoto", ModBlocos.ESTANTE_ESGOTO);
    public static final DeferredItem<BlockItem> ESCADA_SUBIDA = ITENS.registerSimpleBlockItem("escada_subida", ModBlocos.ESCADA_SUBIDA);
    public static final DeferredItem<BlockItem> ESCADA_DESCIDA = ITENS.registerSimpleBlockItem("escada_descida", ModBlocos.ESCADA_DESCIDA);

    private static DeferredItem<ArmaDoShattered> arma(String nome, Numeros numeros) {
        return ITENS.registerItem(nome, propriedades -> new ArmaDoShattered(propriedades, nome, numeros));
    }

    private static DeferredItem<ArmaDeArremesso> arremesso(String nome, ArmaDeArremesso.Numeros numeros) {
        return ITENS.registerItem(nome, propriedades -> new ArmaDeArremesso(propriedades, nome, numeros));
    }

    private static DeferredItem<ArmaduraDoShattered> armadura(String nome, Holder<ArmorMaterial> material, int tier) {
        return ITENS.registerItem(nome, propriedades -> new ArmaduraDoShattered(material, propriedades, nome, tier));
    }

    private static List<DeferredItem<ItemAnel>> registrarAneis() {
        List<DeferredItem<ItemAnel>> lista = new ArrayList<>();
        for (GemaDeAnel gema : GemaDeAnel.values()) {
            lista.add(ITENS.registerItem(gema.nomeDoItem(), propriedades -> new ItemAnel(propriedades, gema)));
        }
        return lista;
    }

    private static List<DeferredItem<ItemVarinha>> registrarVarinhas() {
        List<DeferredItem<ItemVarinha>> lista = new ArrayList<>();
        for (TipoDeVarinha tipo : TipoDeVarinha.values()) {
            lista.add(ITENS.registerItem("varinha_" + tipo.id(), propriedades -> new ItemVarinha(propriedades, tipo)));
        }
        return lista;
    }

    // O item da varinha desse tipo
    public static ItemVarinha varinha(TipoDeVarinha tipo) {
        return VARINHAS.get(tipo.ordinal()).get();
    }

    private static List<DeferredItem<ItemPocao>> registrarPocoes() {
        List<DeferredItem<ItemPocao>> lista = new ArrayList<>();
        for (CorDePocao cor : CorDePocao.values()) {
            lista.add(ITENS.registerItem(cor.nomeDoItem(), propriedades -> new ItemPocao(propriedades, cor)));
        }
        return lista;
    }

    private static List<DeferredItem<ItemPergaminho>> registrarPergaminhos() {
        List<DeferredItem<ItemPergaminho>> lista = new ArrayList<>();
        for (RunaDePergaminho runa : RunaDePergaminho.values()) {
            lista.add(ITENS.registerItem(runa.nomeDoItem(), propriedades -> new ItemPergaminho(propriedades, runa)));
        }
        return lista;
    }

    private static List<DeferredItem<ItemSemente>> registrarSementes() {
        List<DeferredItem<ItemSemente>> lista = new ArrayList<>();
        for (TipoDePlanta planta : TipoDePlanta.values()) {
            lista.add(ITENS.registerItem(planta.nomeDaSemente(), propriedades -> new ItemSemente(propriedades, planta)));
        }
        return lista;
    }

    private static List<DeferredItem<ItemPocaoExotica>> registrarPocoesExoticas() {
        List<DeferredItem<ItemPocaoExotica>> lista = new ArrayList<>();
        for (CorDePocao cor : CorDePocao.values()) {
            lista.add(ITENS.registerItem("pocao_exotica_" + cor.id(), propriedades -> new ItemPocaoExotica(propriedades, cor)));
        }
        return lista;
    }

    private static List<DeferredItem<ItemPergaminhoExotico>> registrarPergaminhosExoticos() {
        List<DeferredItem<ItemPergaminhoExotico>> lista = new ArrayList<>();
        for (RunaDePergaminho runa : RunaDePergaminho.values()) {
            lista.add(ITENS.registerItem("pergaminho_exotico_" + runa.id(), propriedades -> new ItemPergaminhoExotico(propriedades, runa)));
        }
        return lista;
    }

    private static List<DeferredItem<ItemPedraRunica>> registrarPedras() {
        List<DeferredItem<ItemPedraRunica>> lista = new ArrayList<>();
        for (TipoDePedra tipo : TipoDePedra.values()) {
            lista.add(ITENS.registerItem(tipo.nomeDoItem(), propriedades -> new ItemPedraRunica(propriedades, tipo)));
        }
        return lista;
    }

    // O item da pedra desse tipo
    public static ItemPedraRunica pedra(TipoDePedra tipo) {
        return PEDRAS.get(tipo.ordinal()).get();
    }

    private static List<DeferredItem<ItemBomba>> registrarBombas() {
        List<DeferredItem<ItemBomba>> lista = new ArrayList<>();
        for (TipoDeBomba tipo : TipoDeBomba.values()) {
            lista.add(ITENS.registerItem(tipo.id(), propriedades -> new ItemBomba(propriedades, tipo)));
        }
        return lista;
    }

    private static List<DeferredItem<ItemInfusao>> registrarInfusoes() {
        List<DeferredItem<ItemInfusao>> lista = new ArrayList<>();
        for (TipoDeInfusao tipo : TipoDeInfusao.values()) {
            lista.add(ITENS.registerItem(tipo.nomeDoItem(), propriedades -> new ItemInfusao(propriedades, tipo)));
        }
        return lista;
    }

    private static List<DeferredItem<ItemElixir>> registrarElixires() {
        List<DeferredItem<ItemElixir>> lista = new ArrayList<>();
        for (TipoDeElixir tipo : TipoDeElixir.values()) {
            lista.add(ITENS.registerItem(tipo.nomeDoItem(), propriedades -> new ItemElixir(propriedades, tipo)));
        }
        return lista;
    }

    private static List<DeferredItem<ItemFeitico>> registrarFeiticos() {
        List<DeferredItem<ItemFeitico>> lista = new ArrayList<>();
        for (TipoDeFeitico tipo : TipoDeFeitico.values()) {
            lista.add(ITENS.registerItem(tipo.nomeDoItem(), propriedades -> new ItemFeitico(propriedades, tipo)));
        }
        return lista;
    }

    private static List<DeferredItem<ItemAcessorio>> registrarAcessorios() {
        List<DeferredItem<ItemAcessorio>> lista = new ArrayList<>();
        for (TipoDeAcessorio tipo : TipoDeAcessorio.values()) {
            lista.add(ITENS.registerItem(tipo.nomeDoItem(), propriedades -> new ItemAcessorio(propriedades, tipo)));
        }
        return lista;
    }

    public static Item acessorio(TipoDeAcessorio tipo) {
        return ACESSORIOS.get(tipo.ordinal()).get();
    }

    public static Item feitico(TipoDeFeitico tipo) {
        return FEITICOS.get(tipo.ordinal()).get();
    }

    public static Item infusao(TipoDeInfusao tipo) {
        return INFUSOES.get(tipo.ordinal()).get();
    }

    public static Item elixir(TipoDeElixir tipo) {
        return ELIXIRES.get(tipo.ordinal()).get();
    }

    public static Item bomba(TipoDeBomba tipo) {
        return BOMBAS.get(tipo.ordinal()).get();
    }

    // Os desenhos do pote de alquimia (não aparecem no criativo)
    public static boolean marcadorDaAlquimia(Item item) {
        return item == POCAO_ALEATORIA.get() || item == PEDRA_ALEATORIA.get();
    }

    // Usado pela aba do criativo para mostrar todos os itens do mod
    public static void colocarTodos(CreativeModeTab.Output saida) {
        for (var item : ITENS.getEntries()) {
            if (item.get() != FLECHA_ESPIRITUAL.get() && item.get() != GOTA_DE_ORVALHO.get() && !marcadorDaAlquimia(item.get())) {
                saida.accept(item.get());
            }
        }
    }
}
