package com.louzada.pixeldungeon.registro;

import com.louzada.pixeldungeon.PixelDungeon;
import com.louzada.pixeldungeon.entidade.CaranguejoDeEsgoto;
import com.louzada.pixeldungeon.entidade.CobraDeEsgoto;
import com.louzada.pixeldungeon.entidade.EnxameDeMoscas;
import com.louzada.pixeldungeon.entidade.FlechaEspiritual;
import com.louzada.pixeldungeon.entidade.GnollExplorador;
import com.louzada.pixeldungeon.entidade.Goo;
import com.louzada.pixeldungeon.entidade.LuzGuia;
import com.louzada.pixeldungeon.entidade.MisselMagico;
import com.louzada.pixeldungeon.entidade.MobDoShattered;
import com.louzada.pixeldungeon.entidade.RatoAlbino;
import com.louzada.pixeldungeon.entidade.RatoMarsupial;
import com.louzada.pixeldungeon.entidade.SlimeDeEsgoto;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
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

    // Chefe do andar 5: uma gosma larga e baixa
    public static final DeferredHolder<EntityType<?>, EntityType<Goo>> GOO = ENTIDADES.register("goo",
            () -> EntityType.Builder.of(Goo::new, MobCategory.MONSTER).sized(1.8F, 1.4F).build("goo"));

    // ---- Magias ----

    // Míssil mágico do cajado do Mago
    public static final DeferredHolder<EntityType<?>, EntityType<MisselMagico>> MISSEL_MAGICO = ENTIDADES.register("missel_magico",
            () -> EntityType.Builder.<MisselMagico>of(MisselMagico::new, MobCategory.MISC).sized(0.25F, 0.25F)
                    .clientTrackingRange(4).updateInterval(10).build("missel_magico"));

    // Luz guia do Clérigo
    public static final DeferredHolder<EntityType<?>, EntityType<LuzGuia>> LUZ_GUIA = ENTIDADES.register("luz_guia",
            () -> EntityType.Builder.<LuzGuia>of(LuzGuia::new, MobCategory.MISC).sized(0.25F, 0.25F)
                    .clientTrackingRange(4).updateInterval(10).build("luz_guia"));

    // Flecha do arco espiritual da Caçadora
    public static final DeferredHolder<EntityType<?>, EntityType<FlechaEspiritual>> FLECHA_ESPIRITUAL = ENTIDADES.register("flecha_espiritual",
            () -> EntityType.Builder.<FlechaEspiritual>of(FlechaEspiritual::new, MobCategory.MISC).sized(0.25F, 0.25F)
                    .clientTrackingRange(4).updateInterval(10).build("flecha_espiritual"));

    // Diz ao Minecraft a vida, velocidade etc. de cada inimigo
    public static void aoCriarAtributos(EntityAttributeCreationEvent evento) {
        evento.put(RATO_MARSUPIAL.get(), MobDoShattered.atributosDoMinecraft(RatoMarsupial.ATRIBUTOS).build());
        evento.put(COBRA_DE_ESGOTO.get(), MobDoShattered.atributosDoMinecraft(CobraDeEsgoto.ATRIBUTOS).build());
        evento.put(GNOLL_EXPLORADOR.get(), MobDoShattered.atributosDoMinecraft(GnollExplorador.ATRIBUTOS).build());
        evento.put(CARANGUEJO_DE_ESGOTO.get(), MobDoShattered.atributosDoMinecraft(CaranguejoDeEsgoto.ATRIBUTOS).build());
        evento.put(SLIME.get(), MobDoShattered.atributosDoMinecraft(SlimeDeEsgoto.ATRIBUTOS).build());
        evento.put(RATO_ALBINO.get(), MobDoShattered.atributosDoMinecraft(RatoAlbino.ATRIBUTOS).build());
        evento.put(ENXAME_DE_MOSCAS.get(), EnxameDeMoscas.atributosDoEnxame().build());
        evento.put(GOO.get(), Goo.atributosDoGoo().build());
    }
}
