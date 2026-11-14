package com.louzada.pixeldungeon.registro;

import com.louzada.pixeldungeon.PixelDungeon;
import com.louzada.pixeldungeon.entidade.Abelha;
import com.louzada.pixeldungeon.entidade.ArmaArremessada;
import com.louzada.pixeldungeon.entidade.BombaAcesa;
import com.louzada.pixeldungeon.entidade.BombaArremessada;
import com.louzada.pixeldungeon.entidade.CaranguejoDeEsgoto;
import com.louzada.pixeldungeon.entidade.CaranguejoGrande;
import com.louzada.pixeldungeon.entidade.CobraDeEsgoto;
import com.louzada.pixeldungeon.entidade.EnxameDeMoscas;
import com.louzada.pixeldungeon.entidade.Espectro;
import com.louzada.pixeldungeon.entidade.EstatuaAnimada;
import com.louzada.pixeldungeon.entidade.FantasmaAliado;
import com.louzada.pixeldungeon.entidade.FantasmaTriste;
import com.louzada.pixeldungeon.entidade.FlechaEspiritual;
import com.louzada.pixeldungeon.entidade.GnollExplorador;
import com.louzada.pixeldungeon.entidade.GnollTrapaceiro;
import com.louzada.pixeldungeon.entidade.Goo;
import com.louzada.pixeldungeon.entidade.GotaDeOrvalho;
import com.louzada.pixeldungeon.entidade.Guarda;
import com.louzada.pixeldungeon.entidade.GuardiaoDaTerra;
import com.louzada.pixeldungeon.entidade.ImagemEspelhada;
import com.louzada.pixeldungeon.entidade.LuzGuia;
import com.louzada.pixeldungeon.entidade.Mimico;
import com.louzada.pixeldungeon.entidade.MisselMagico;
import com.louzada.pixeldungeon.entidade.MobDoShattered;
import com.louzada.pixeldungeon.entidade.NuvemDeGas;
import com.louzada.pixeldungeon.entidade.OvelhaMagica;
import com.louzada.pixeldungeon.entidade.PedraArremessada;
import com.louzada.pixeldungeon.entidade.Piranha;
import com.louzada.pixeldungeon.entidade.PocaoArremessada;
import com.louzada.pixeldungeon.entidade.PoteArremessado;
import com.louzada.pixeldungeon.entidade.RaioSolar;
import com.louzada.pixeldungeon.entidade.RatoAlbino;
import com.louzada.pixeldungeon.entidade.RatoFetido;
import com.louzada.pixeldungeon.entidade.RatoMarsupial;
import com.louzada.pixeldungeon.entidade.SementeArremessada;
import com.louzada.pixeldungeon.entidade.Sentinela;
import com.louzada.pixeldungeon.entidade.SlimeDeEsgoto;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

// Os inimigos (e outras entidades) do mod.
public class ModEntidades {
    public static final DeferredRegister<EntityType<?>> ENTIDADES = DeferredRegister.create(Registries.ENTITY_TYPE, PixelDungeon.MODID);

    // ---- Esgoto ----

    public static final DeferredHolder<EntityType<?>, EntityType<RatoMarsupial>> RATO_MARSUPIAL = ENTIDADES.register("rato_marsupial",
            () -> EntityType.Builder.of(RatoMarsupial::new, MobCategory.MONSTER).sized(0.6F, 0.6F).build("rato_marsupial"));

    // Rato raro (1 em 50 ratos)
    public static final DeferredHolder<EntityType<?>, EntityType<RatoAlbino>> RATO_ALBINO = ENTIDADES.register("rato_albino",
            () -> EntityType.Builder.of(RatoAlbino::new, MobCategory.MONSTER).sized(0.6F, 0.6F).build("rato_albino"));

    public static final DeferredHolder<EntityType<?>, EntityType<CobraDeEsgoto>> COBRA_DE_ESGOTO = ENTIDADES.register("cobra_de_esgoto",
            () -> EntityType.Builder.of(CobraDeEsgoto::new, MobCategory.MONSTER).sized(0.6F, 0.4F).build("cobra_de_esgoto"));

    public static final DeferredHolder<EntityType<?>, EntityType<GnollExplorador>> GNOLL_EXPLORADOR = ENTIDADES.register("gnoll_explorador",
            () -> EntityType.Builder.of(GnollExplorador::new, MobCategory.MONSTER).sized(0.6F, 1.95F).build("gnoll_explorador"));

    public static final DeferredHolder<EntityType<?>, EntityType<CaranguejoDeEsgoto>> CARANGUEJO_DE_ESGOTO = ENTIDADES.register("caranguejo_de_esgoto",
            () -> EntityType.Builder.of(CaranguejoDeEsgoto::new, MobCategory.MONSTER).sized(0.9F, 0.6F).build("caranguejo_de_esgoto"));

    public static final DeferredHolder<EntityType<?>, EntityType<SlimeDeEsgoto>> SLIME = ENTIDADES.register("slime",
            () -> EntityType.Builder.of(SlimeDeEsgoto::new, MobCategory.MONSTER).sized(1.0F, 1.0F).build("slime"));

    public static final DeferredHolder<EntityType<?>, EntityType<EnxameDeMoscas>> ENXAME_DE_MOSCAS = ENTIDADES.register("enxame_de_moscas",
            () -> EntityType.Builder.of(EnxameDeMoscas::new, MobCategory.MONSTER).sized(0.6F, 0.6F).build("enxame_de_moscas"));

    // Mímico: do tamanho de um baú (escondido, ele é um baú de verdade)
    public static final DeferredHolder<EntityType<?>, EntityType<Mimico>> MIMICO = ENTIDADES.register("mimico",
            () -> EntityType.Builder.of(Mimico::new, MobCategory.MONSTER).sized(0.875F, 0.875F).build("mimico"));

    // ---- As salas especiais ----

    // Piranha (a piscina): pequena, cabe na água rasa
    public static final DeferredHolder<EntityType<?>, EntityType<Piranha>> PIRANHA = ENTIDADES.register("piranha",
            () -> EntityType.Builder.of(Piranha::new, MobCategory.MONSTER).sized(0.6F, 0.5F).fireImmune().build("piranha"));
    // Abelha (sai do pote de mel)
    public static final DeferredHolder<EntityType<?>, EntityType<Abelha>> ABELHA = ENTIDADES.register("abelha",
            () -> EntityType.Builder.of(Abelha::new, MobCategory.MONSTER).sized(0.6F, 0.6F).build("abelha"));
    // Estátua animada (a sala da estátua)
    public static final DeferredHolder<EntityType<?>, EntityType<EstatuaAnimada>> ESTATUA_ANIMADA = ENTIDADES.register("estatua_animada",
            () -> EntityType.Builder.of(EstatuaAnimada::new, MobCategory.MONSTER).sized(0.7F, 1.9F).build("estatua_animada"));
    // Sentinela (a sala da sentinela): não é um inimigo de verdade (não apanha, não anda)
    public static final DeferredHolder<EntityType<?>, EntityType<Sentinela>> SENTINELA = ENTIDADES.register("sentinela",
            () -> EntityType.Builder.of(Sentinela::new, MobCategory.MISC).sized(0.8F, 1.6F).fireImmune().clientTrackingRange(10).build("sentinela"));

    // Espectro (sai dos túmulos do cemitério gramado e da ossada assombrada): do tamanho de uma pessoa, voando
    public static final DeferredHolder<EntityType<?>, EntityType<Espectro>> ESPECTRO = ENTIDADES.register("espectro",
            () -> EntityType.Builder.of(Espectro::new, MobCategory.MONSTER).sized(0.6F, 1.6F).build("espectro"));

    // Chefe do andar 5: uma gosma larga e baixa
    public static final DeferredHolder<EntityType<?>, EntityType<Goo>> GOO = ENTIDADES.register("goo",
            () -> EntityType.Builder.of(Goo::new, MobCategory.MONSTER).sized(1.8F, 1.4F).build("goo"));

    // ---- A missão do fantasma ----

    // O fantasma triste (dá a missão) e os inimigos dela: o rato fétido, o gnoll trapaceiro e o caranguejo grande
    public static final DeferredHolder<EntityType<?>, EntityType<FantasmaTriste>> FANTASMA_TRISTE = ENTIDADES.register("fantasma_triste",
            () -> EntityType.Builder.of(FantasmaTriste::new, MobCategory.MISC).sized(0.6F, 1.8F).clientTrackingRange(8).build("fantasma_triste"));
    public static final DeferredHolder<EntityType<?>, EntityType<RatoFetido>> RATO_FETIDO = ENTIDADES.register("rato_fetido",
            () -> EntityType.Builder.of(RatoFetido::new, MobCategory.MONSTER).sized(0.6F, 0.6F).build("rato_fetido"));
    public static final DeferredHolder<EntityType<?>, EntityType<GnollTrapaceiro>> GNOLL_TRAPACEIRO = ENTIDADES.register("gnoll_trapaceiro",
            () -> EntityType.Builder.of(GnollTrapaceiro::new, MobCategory.MONSTER).sized(0.6F, 1.95F).build("gnoll_trapaceiro"));
    public static final DeferredHolder<EntityType<?>, EntityType<CaranguejoGrande>> CARANGUEJO_GRANDE = ENTIDADES.register("caranguejo_grande",
            () -> EntityType.Builder.of(CaranguejoGrande::new, MobCategory.MONSTER).sized(0.9F, 0.6F).build("caranguejo_grande"));
    // O fantasma da rosa seca: o aliado que luta do lado do herói
    public static final DeferredHolder<EntityType<?>, EntityType<FantasmaAliado>> FANTASMA_ALIADO = ENTIDADES.register("fantasma_aliado",
            () -> EntityType.Builder.of(FantasmaAliado::new, MobCategory.MISC).sized(0.6F, 1.8F).fireImmune().clientTrackingRange(8)
                    .build("fantasma_aliado"));

    // ---- Armadilhas ----

    // As ovelhas mágicas da armadilha de rebanho
    public static final DeferredHolder<EntityType<?>, EntityType<OvelhaMagica>> OVELHA_MAGICA = ENTIDADES.register("ovelha_magica",
            () -> EntityType.Builder.of(OvelhaMagica::new, MobCategory.MISC).sized(1.2F, 1.1F).clientTrackingRange(8).build("ovelha_magica"));

    // ---- Magias ----

    // Míssil mágico do cajado do Mago
    public static final DeferredHolder<EntityType<?>, EntityType<MisselMagico>> MISSEL_MAGICO = ENTIDADES.register("missel_magico",
            () -> EntityType.Builder.<MisselMagico>of(MisselMagico::new, MobCategory.MISC).sized(0.25F, 0.25F)
                    .clientTrackingRange(4).updateInterval(10).build("missel_magico"));

    // Luz guia do Clérigo
    public static final DeferredHolder<EntityType<?>, EntityType<LuzGuia>> LUZ_GUIA = ENTIDADES.register("luz_guia",
            () -> EntityType.Builder.<LuzGuia>of(LuzGuia::new, MobCategory.MISC).sized(0.25F, 0.25F)
                    .clientTrackingRange(4).updateInterval(10).build("luz_guia"));

    // Raio solar do Clérigo
    public static final DeferredHolder<EntityType<?>, EntityType<RaioSolar>> RAIO_SOLAR = ENTIDADES.register("raio_solar",
            () -> EntityType.Builder.<RaioSolar>of(RaioSolar::new, MobCategory.MISC).sized(0.25F, 0.25F)
                    .clientTrackingRange(4).updateInterval(10).build("raio_solar"));

    // Flecha do arco espiritual da Caçadora
    public static final DeferredHolder<EntityType<?>, EntityType<FlechaEspiritual>> FLECHA_ESPIRITUAL = ENTIDADES.register("flecha_espiritual",
            () -> EntityType.Builder.<FlechaEspiritual>of(FlechaEspiritual::new, MobCategory.MISC).sized(0.25F, 0.25F)
                    .clientTrackingRange(4).updateInterval(10).build("flecha_espiritual"));

    // ---- Coisas do chão ----

    // Gota de orvalho: cai da grama alta e voa pro odre
    public static final DeferredHolder<EntityType<?>, EntityType<GotaDeOrvalho>> GOTA_DE_ORVALHO = ENTIDADES.register("gota_de_orvalho",
            () -> EntityType.Builder.<GotaDeOrvalho>of(GotaDeOrvalho::new, MobCategory.MISC).sized(0.3F, 0.3F)
                    .clientTrackingRange(8).updateInterval(2).build("gota_de_orvalho"));

    // ---- Poções, sementes e pergaminhos ----

    // Poção arremessada (quebra onde bater)
    // Arma de arremesso voando (pedra, faca, shuriken...)
    public static final DeferredHolder<EntityType<?>, EntityType<ArmaArremessada>> ARMA_ARREMESSADA = ENTIDADES.register("arma_arremessada",
            () -> EntityType.Builder.<ArmaArremessada>of(ArmaArremessada::new, MobCategory.MISC).sized(0.25F, 0.25F)
                    .clientTrackingRange(4).updateInterval(5).build("arma_arremessada"));

    public static final DeferredHolder<EntityType<?>, EntityType<PocaoArremessada>> POCAO_ARREMESSADA = ENTIDADES.register("pocao_arremessada",
            () -> EntityType.Builder.<PocaoArremessada>of(PocaoArremessada::new, MobCategory.MISC).sized(0.25F, 0.25F)
                    .clientTrackingRange(4).updateInterval(10).build("pocao_arremessada"));
    // Semente arremessada (a planta nasce onde cair)
    public static final DeferredHolder<EntityType<?>, EntityType<SementeArremessada>> SEMENTE_ARREMESSADA = ENTIDADES.register("semente_arremessada",
            () -> EntityType.Builder.<SementeArremessada>of(SementeArremessada::new, MobCategory.MISC).sized(0.25F, 0.25F)
                    .clientTrackingRange(4).updateInterval(10).build("semente_arremessada"));
    // Pedra rúnica arremessada (o efeito acontece onde ela cair)
    public static final DeferredHolder<EntityType<?>, EntityType<PedraArremessada>> PEDRA_ARREMESSADA = ENTIDADES.register("pedra_arremessada",
            () -> EntityType.Builder.<PedraArremessada>of(PedraArremessada::new, MobCategory.MISC).sized(0.25F, 0.25F)
                    .clientTrackingRange(4).updateInterval(10).build("pedra_arremessada"));
    // Bomba arremessada (acesa, vira uma bomba acesa onde cair)
    public static final DeferredHolder<EntityType<?>, EntityType<BombaArremessada>> BOMBA_ARREMESSADA = ENTIDADES.register("bomba_arremessada",
            () -> EntityType.Builder.<BombaArremessada>of(BombaArremessada::new, MobCategory.MISC).sized(0.25F, 0.25F)
                    .clientTrackingRange(4).updateInterval(10).build("bomba_arremessada"));
    // Bomba acesa no chão (explode quando o pavio acaba)
    public static final DeferredHolder<EntityType<?>, EntityType<BombaAcesa>> BOMBA_ACESA = ENTIDADES.register("bomba_acesa",
            () -> EntityType.Builder.<BombaAcesa>of(BombaAcesa::new, MobCategory.MISC).sized(0.5F, 0.5F)
                    .clientTrackingRange(8).updateInterval(5).build("bomba_acesa"));
    // Pote de mel arremessado (quebra onde cair e solta uma abelha)
    public static final DeferredHolder<EntityType<?>, EntityType<PoteArremessado>> POTE_ARREMESSADO = ENTIDADES.register("pote_arremessado",
            () -> EntityType.Builder.<PoteArremessado>of(PoteArremessado::new, MobCategory.MISC).sized(0.3F, 0.3F)
                    .clientTrackingRange(4).updateInterval(10).build("pote_arremessado"));
    // Nuvem de gás (tóxico, paralisante, confusão)
    public static final DeferredHolder<EntityType<?>, EntityType<NuvemDeGas>> NUVEM_DE_GAS = ENTIDADES.register("nuvem_de_gas",
            () -> EntityType.Builder.<NuvemDeGas>of(NuvemDeGas::new, MobCategory.MISC).sized(0.5F, 0.5F)
                    .clientTrackingRange(8).updateInterval(20).build("nuvem_de_gas"));
    // Imagem espelhada do herói (pergaminho)
    public static final DeferredHolder<EntityType<?>, EntityType<ImagemEspelhada>> IMAGEM_ESPELHADA = ENTIDADES.register("imagem_espelhada",
            () -> EntityType.Builder.of(ImagemEspelhada::new, MobCategory.MISC).sized(0.6F, 1.8F).clientTrackingRange(8)
                    .build("imagem_espelhada"));

    // Guardião da terra (varinha de terra viva): aliado de pedra
    public static final DeferredHolder<EntityType<?>, EntityType<GuardiaoDaTerra>> GUARDIAO_DA_TERRA = ENTIDADES.register("guardiao_da_terra",
            () -> EntityType.Builder.of(GuardiaoDaTerra::new, MobCategory.MISC).sized(0.7F, 1.8F).clientTrackingRange(8)
                    .build("guardiao_da_terra"));
    // Guardas e sentinelas (varinha de proteção): aliados parados que disparam nos inimigos
    public static final DeferredHolder<EntityType<?>, EntityType<Guarda>> GUARDA = ENTIDADES.register("guarda",
            () -> EntityType.Builder.of(Guarda::new, MobCategory.MISC).sized(0.6F, 0.8F).clientTrackingRange(8).build("guarda"));

    // Diz ao Minecraft a vida, velocidade etc. de cada inimigo
    public static void aoCriarAtributos(EntityAttributeCreationEvent evento) {
        evento.put(RATO_MARSUPIAL.get(), MobDoShattered.atributosDoMinecraft(RatoMarsupial.ATRIBUTOS).build());
        evento.put(FANTASMA_TRISTE.get(), FantasmaTriste.atributos().build());
        evento.put(OVELHA_MAGICA.get(), OvelhaMagica.atributos().build());
        evento.put(RATO_FETIDO.get(), MobDoShattered.atributosDoMinecraft(RatoFetido.ATRIBUTOS).build());
        evento.put(GNOLL_TRAPACEIRO.get(), MobDoShattered.atributosDoMinecraft(GnollTrapaceiro.ATRIBUTOS).build());
        evento.put(CARANGUEJO_GRANDE.get(), MobDoShattered.atributosDoMinecraft(CaranguejoGrande.ATRIBUTOS).build());
        evento.put(FANTASMA_ALIADO.get(), MobDoShattered.atributosDoMinecraft(FantasmaAliado.ATRIBUTOS).build());
        evento.put(COBRA_DE_ESGOTO.get(), MobDoShattered.atributosDoMinecraft(CobraDeEsgoto.ATRIBUTOS).build());
        evento.put(GNOLL_EXPLORADOR.get(), MobDoShattered.atributosDoMinecraft(GnollExplorador.ATRIBUTOS).build());
        evento.put(CARANGUEJO_DE_ESGOTO.get(), MobDoShattered.atributosDoMinecraft(CaranguejoDeEsgoto.ATRIBUTOS).build());
        evento.put(SLIME.get(), MobDoShattered.atributosDoMinecraft(SlimeDeEsgoto.ATRIBUTOS).build());
        evento.put(RATO_ALBINO.get(), MobDoShattered.atributosDoMinecraft(RatoAlbino.ATRIBUTOS).build());
        evento.put(ENXAME_DE_MOSCAS.get(), EnxameDeMoscas.atributosDoEnxame().build());
        evento.put(GOO.get(), Goo.atributosDoGoo().build());
        evento.put(MIMICO.get(), MobDoShattered.atributosDoMinecraft(Mimico.ATRIBUTOS).build());
        evento.put(IMAGEM_ESPELHADA.get(), ImagemEspelhada.atributosDaImagem().build());
        evento.put(GUARDIAO_DA_TERRA.get(), MobDoShattered.atributosDoMinecraft(GuardiaoDaTerra.ATRIBUTOS).build());
        evento.put(GUARDA.get(), MobDoShattered.atributosDoMinecraft(Guarda.ATRIBUTOS).add(Attributes.KNOCKBACK_RESISTANCE, 1.0).build());
        evento.put(PIRANHA.get(), Piranha.atributosDaPiranha().build());
        evento.put(ABELHA.get(), Abelha.atributosDaAbelha().build());
        evento.put(ESTATUA_ANIMADA.get(), MobDoShattered.atributosDoMinecraft(EstatuaAnimada.ATRIBUTOS).add(Attributes.KNOCKBACK_RESISTANCE, 0.5).build());
        evento.put(SENTINELA.get(), Sentinela.atributosDaSentinela().build());
        evento.put(ESPECTRO.get(), Espectro.atributosDoEspectro().build());
    }
}
