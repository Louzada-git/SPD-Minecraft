package com.louzada.pixeldungeon.item;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import com.louzada.pixeldungeon.Turno;
import com.louzada.pixeldungeon.combate.EventosDeCombate;
import com.louzada.pixeldungeon.combate.Particulas;
import com.louzada.pixeldungeon.dungeon.Andares;
import com.louzada.pixeldungeon.dungeon.Grama;
import com.louzada.pixeldungeon.dungeon.Teleporte;
import com.louzada.pixeldungeon.efeito.EfeitosNoTick;
import com.louzada.pixeldungeon.entidade.EnxameDeMoscas;
import com.louzada.pixeldungeon.entidade.Guarda;
import com.louzada.pixeldungeon.entidade.GuardiaoDaTerra;
import com.louzada.pixeldungeon.entidade.ImagemEspelhada;
import com.louzada.pixeldungeon.entidade.Mimico;
import com.louzada.pixeldungeon.entidade.MobDoShattered;
import com.louzada.pixeldungeon.entidade.NuvemDeGas;
import com.louzada.pixeldungeon.heroi.ArmaduraDePedra;
import com.louzada.pixeldungeon.heroi.Escudos;
import com.louzada.pixeldungeon.heroi.Mira;
import com.louzada.pixeldungeon.heroi.Talentos;
import com.louzada.pixeldungeon.heroi.TalentosDoTier2;
import com.louzada.pixeldungeon.registro.ModEfeitos;
import com.louzada.pixeldungeon.registro.ModEntidades;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

// O que cada varinha faz quando é lançada (as soltas e a que está no cajado do Mago), com os números do Shattered.
// O míssil mágico voa (MisselMagico); as outras acertam na hora, com um rastro de partículas:
//   raios que param no primeiro inimigo ou na parede (relâmpago, geada, luz prismática, corrosão, onda de choque, transfusão),
//   o da desintegração, que atravessa tudo, e os cones (rajada de fogo, maturação).
// A terra viva, a corrupção e a proteção trazem aliados: o guardião da terra, os inimigos corrompidos e as guardas.
// Varinhas amaldiçoadas fazem uma coisa aleatória (e ruim) no lugar.
public class EfeitosDeVarinha {
    // 1 quadradinho do Shattered = 4 blocos
    private static final double QUADRADINHO = 4.0;
    // Alcance dos raios (o Shattered vai até a parede; aqui, 8 quadradinhos)
    private static final double ALCANCE = 32.0;

    // ---- Lançar ----

    public static void lancar(ServerPlayer jogador, TipoDeVarinha tipo, int nivel, int cargasUsadas) {
        switch (tipo) {
            case MISSEL_MAGICO -> new VarinhaDeMisselMagico().lancar(jogador, nivel);
            case RELAMPAGO -> relampago(jogador, nivel);
            case DESINTEGRACAO -> desintegracao(jogador, nivel);
            case RAJADA_DE_FOGO -> rajadaDeFogo(jogador, nivel, cargasUsadas);
            case CORROSAO -> corrosao(jogador, nivel);
            case ONDA_DE_CHOQUE -> ondaDeChoque(jogador, nivel);
            case GEADA -> geada(jogador, nivel);
            case LUZ_PRISMATICA -> luzPrismatica(jogador, nivel);
            case TRANSFUSAO -> transfusao(jogador, nivel);
            case MATURACAO -> maturacao(jogador, nivel, cargasUsadas);
            case TERRA_VIVA -> terraViva(jogador, nivel);
            case CORRUPCAO -> corrupcao(jogador, nivel);
            case PROTECAO -> protecao(jogador, nivel);
        }
    }

    // Dá pra lançar? (a varinha de proteção não lança se as guardas já gastam toda a energia dela; aí a carga não é gasta)
    public static boolean podeLancar(ServerPlayer jogador, TipoDeVarinha tipo) {
        if (tipo != TipoDeVarinha.PROTECAO) {
            return true;
        }
        int atual = energiaDasGuardas(jogador);
        int maxima = energiaMaxima(jogador);
        boolean pode;
        if (Mira.inimigo(jogador, ALCANCE) instanceof Guarda guarda && jogador.getUUID().equals(guarda.donoDoAliado())) {
            // Melhorar uma guarda gasta energia; uma sentinela sem energia sobrando só se cura
            pode = atual < maxima || guarda.nivel() > 3;
        } else {
            pode = atual + 1 <= maxima;
        }
        if (!pode) {
            jogador.displayClientMessage(Component.translatable("mensagem.pixeldungeon.sem_energia_pra_guardas").withStyle(ChatFormatting.GRAY), true);
        }
        return pode;
    }

    // ---- Raios ----

    // Relâmpago: 5-10 (+1/+5) no alvo e em quem estiver a 1 quadradinho dele (2 na água), pulando de um pro outro.
    // O dano se divide: cada um leva 40% + 60% / (quantos foram atingidos); na água, todos levam tudo.
    private static void relampago(ServerPlayer jogador, int nivel) {
        Vec3 fim = Mira.ponto(jogador, ALCANCE);
        LivingEntity alvo = Mira.inimigo(jogador, ALCANCE);
        if (alvo != null) {
            fim = alvo.getBoundingBox().getCenter();
        }
        rastro(jogador, fim, ParticleTypes.ELECTRIC_SPARK, 3);
        som(jogador, fim, SoundEvents.LIGHTNING_BOLT_IMPACT, 0.6F, 1.6F);
        if (alvo == null) {
            return;
        }
        List<LivingEntity> atingidos = new ArrayList<>(List.of(alvo));
        for (int i = 0; i < atingidos.size(); i++) {
            LivingEntity atual = atingidos.get(i);
            double alcance = (atual.isInWater() ? 2 : 1) * QUADRADINHO;
            for (LivingEntity perto : inimigosEmVolta(jogador, atual.position(), alcance)) {
                if (!atingidos.contains(perto)) {
                    atingidos.add(perto);
                    linha(jogador.serverLevel(), atual.getBoundingBox().getCenter(), perto.getBoundingBox().getCenter(), ParticleTypes.ELECTRIC_SPARK, 3);
                }
            }
        }
        float parte = alvo.isInWater() ? 1.0F : 0.4F + 0.6F / atingidos.size();
        boolean primeiro = true;
        for (LivingEntity atingido : atingidos) {
            int dano = Math.round(TipoDeVarinha.RELAMPAGO.sortearDano(jogador.getRandom(), nivel, 1) * parte);
            ferir(jogador, atingido, dano, primeiro);
            primeiro = false;
        }
    }

    // Desintegração: atravessa inimigos e paredes até 6 + 2 x nível quadradinhos. Cada inimigo a mais no caminho
    // e cada 3 quadradinhos de parede atravessados contam como +1 nível pro dano de todos.
    private static void desintegracao(ServerPlayer jogador, int nivel) {
        double alcance = (6 + 2 * nivel) * QUADRADINHO;
        Vec3 olho = jogador.getEyePosition();
        Vec3 olhar = jogador.getViewVector(1.0F);
        Vec3 fim = olho.add(olhar.scale(alcance));
        // Quantos quadradinhos de parede o raio atravessa
        int paredes = 0;
        for (double d = 0; d < alcance; d += QUADRADINHO) {
            BlockPos lugar = BlockPos.containing(olho.add(olhar.scale(d)));
            if (!jogador.level().getBlockState(lugar).getCollisionShape(jogador.level(), lugar).isEmpty()) {
                paredes++;
            }
        }
        List<LivingEntity> atingidos = new ArrayList<>();
        for (LivingEntity criatura : jogador.level().getEntitiesOfClass(LivingEntity.class, new AABB(olho, fim).inflate(1.0), criatura -> podeAtingir(jogador, criatura))) {
            if (criatura.getBoundingBox().inflate(0.4).clip(olho, fim).isPresent()) {
                atingidos.add(criatura);
            }
        }
        rastro(jogador, fim, Particulas.poeira(TipoDeVarinha.DESINTEGRACAO.cor(), 1.4F), 4);
        som(jogador, jogador.position(), SoundEvents.BEACON_DEACTIVATE, 0.8F, 1.8F);
        int nivelDoDano = nivel + Math.max(0, atingidos.size() - 1) + paredes / 3;
        boolean primeiro = true;
        for (LivingEntity atingido : atingidos) {
            ferir(jogador, atingido, TipoDeVarinha.DESINTEGRACAO.sortearDano(jogador.getRandom(), nivelDoDano, 1), primeiro);
            primeiro = false;
        }
    }

    // Geada: 2-8 (+1/+5) e resfriado por 2 + nível turnos (4 + nível na água). Quem já está resfriado leva menos;
    // quem está congelado não sente nada.
    private static void geada(ServerPlayer jogador, int nivel) {
        LivingEntity alvo = raioAteOAlvo(jogador, TipoDeVarinha.GEADA, ParticleTypes.SNOWFLAKE);
        som(jogador, jogador.position(), SoundEvents.GLASS_BREAK, 0.5F, 1.8F);
        if (alvo == null || alvo.hasEffect(ModEfeitos.CONGELADO)) {
            return;
        }
        int dano = TipoDeVarinha.GEADA.sortearDano(jogador.getRandom(), nivel, 1);
        MobEffectInstance frio = alvo.getEffect(ModEfeitos.RESFRIADO);
        if (frio != null) {
            float turnos = Math.min(10, frio.getDuration() / (float) Turno.TICKS);
            dano = (int) Math.round(dano * Math.pow(0.9333, turnos));
        }
        ferir(jogador, alvo, dano, true);
        if (alvo.isAlive()) {
            int turnos = (alvo.isInWater() ? 4 : 2) + nivel;
            alvo.addEffect(new MobEffectInstance(ModEfeitos.RESFRIADO, Turno.ticks(turnos)));
            alvo.setTicksFrozen(Math.max(alvo.getTicksFrozen(), 100));
        }
    }

    // Luz prismática: 1-5 (+1/+3) e às vezes cega (sorteio de 0 a 4 + nível dar 3 ou mais) por 2 + nível/3 turnos
    private static void luzPrismatica(ServerPlayer jogador, int nivel) {
        LivingEntity alvo = raioAteOAlvo(jogador, TipoDeVarinha.LUZ_PRISMATICA, ParticleTypes.END_ROD);
        som(jogador, jogador.position(), SoundEvents.AMETHYST_BLOCK_CHIME, 1.0F, 1.6F);
        if (alvo == null) {
            return;
        }
        ferir(jogador, alvo, TipoDeVarinha.LUZ_PRISMATICA.sortearDano(jogador.getRandom(), nivel, 1), true);
        if (alvo.isAlive() && jogador.getRandom().nextInt(5 + nivel) >= 3) {
            EfeitosNoTick.cegar(alvo, Turno.ticks(2 + nivel / 3.0F));
        }
    }

    // Corrosão: uma nuvem de gás corrosivo onde o raio para (o dano começa em 2 + nível e cresce a cada turno dentro dela)
    private static void corrosao(ServerPlayer jogador, int nivel) {
        Vec3 lugar = ondeORaioPara(jogador);
        rastro(jogador, lugar, Particulas.poeira(TipoDeVarinha.CORROSAO.cor(), 1.2F), 3);
        som(jogador, lugar, SoundEvents.FIRE_EXTINGUISH, 0.8F, 0.6F);
        NuvemDeGas nuvem = NuvemDeGas.soltar(jogador.serverLevel(), lugar.subtract(0, 1.0, 0), NuvemDeGas.Tipo.CORROSIVO, 6.0F, 5 + nivel);
        nuvem.definirForca(2 + nivel);
    }

    // Onda de choque: explode onde o raio para, com 1-3 (+1/+3) em todo mundo a 1 quadradinho.
    // O alvo voa 3 + nível quadradinhos pra frente; os de volta, 1,5 + nível/2 pra longe do centro.
    // Quem bate numa parede no caminho leva mais dano e fica paralisado um pouco.
    private static void ondaDeChoque(ServerPlayer jogador, int nivel) {
        LivingEntity alvo = Mira.inimigo(jogador, ALCANCE);
        Vec3 centro = alvo != null ? alvo.position() : ondeORaioPara(jogador);
        rastro(jogador, centro.add(0, 0.5, 0), Particulas.poeira(TipoDeVarinha.ONDA_DE_CHOQUE.cor(), 1.2F), 3);
        ServerLevel mundo = jogador.serverLevel();
        Particulas.explosao(mundo, ParticleTypes.EXPLOSION, centro.x, centro.y + 0.5, centro.z, 3, 1.0, 0.0);
        som(jogador, centro, SoundEvents.GENERIC_EXPLODE.value(), 0.7F, 1.4F);
        Vec3 frente = jogador.getViewVector(1.0F).multiply(1, 0, 1).normalize();
        for (LivingEntity atingido : inimigosEmVolta(jogador, centro, 1.5 * QUADRADINHO)) {
            ferir(jogador, atingido, TipoDeVarinha.ONDA_DE_CHOQUE.sortearDano(jogador.getRandom(), nivel, 1), atingido == alvo);
            if (!atingido.isAlive()) {
                continue;
            }
            if (atingido == alvo) {
                empurrar(jogador, atingido, frente, nivel + 3);
            } else {
                Vec3 longe = atingido.position().subtract(centro).multiply(1, 0, 1);
                empurrar(jogador, atingido, longe.lengthSqr() < 0.01 ? frente : longe.normalize(), Math.round(1.5F + nivel / 2.0F));
            }
        }
    }

    // Empurra uns quadradinhos pra uma direção, parando antes de uma parede (e aí leva dano e fica paralisado)
    public static void empurrar(ServerPlayer jogador, LivingEntity criatura, Vec3 direcao, int quadradinhos) {
        empurrar(jogador, criatura, direcao, quadradinhos, true);
    }

    // Igual, mas dá pra escolher se bater na parede machuca (a lança só empurra)
    public static void empurrar(ServerPlayer jogador, LivingEntity criatura, Vec3 direcao, int quadradinhos, boolean machucaNaParede) {
        if (criatura.hasEffect(ModEfeitos.ENRAIZADO) || quadradinhos <= 0) {
            return;
        }
        Vec3 inicio = criatura.position().add(0, criatura.getBbHeight() / 2, 0);
        Vec3 fim = inicio.add(direcao.scale(quadradinhos * QUADRADINHO));
        HitResult parede = criatura.level().clip(new ClipContext(inicio, fim, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, criatura));
        boolean bateu = parede.getType() != HitResult.Type.MISS;
        Vec3 destino = bateu ? parede.getLocation().subtract(direcao.scale(criatura.getBbWidth() / 2 + 0.3)) : fim;
        double andou = destino.distanceTo(inicio);
        criatura.teleportTo(destino.x, criatura.getY(), destino.z);
        if (bateu && machucaNaParede) {
            int distancia = (int) Math.round(andou / QUADRADINHO);
            if (distancia > 0) {
                criatura.invulnerableTime = 0;
                criatura.hurt(fonte(jogador), distancia + jogador.getRandom().nextInt(distancia + 1));
                criatura.addEffect(new MobEffectInstance(ModEfeitos.PARALISADO, Turno.ticks(1 + distancia / 2.0F)));
            }
        }
    }

    // Transfusão: num aliado na mira, tira 5% da vida de quem lança e cura o aliado nisso + 3 x nível (a sobra vira blindagem).
    // Num inimigo, ele fica encantado (não ataca) por 5 turnos e quem lança ganha 5 + nível de blindagem.
    private static void transfusao(ServerPlayer jogador, int nivel) {
        LivingEntity aliado = Mira.aliado(jogador, ALCANCE);
        LivingEntity inimigo = aliado == null ? Mira.inimigo(jogador, ALCANCE) : null;
        LivingEntity alvo = aliado != null ? aliado : inimigo;
        Vec3 fim = alvo != null ? alvo.getBoundingBox().getCenter() : Mira.ponto(jogador, ALCANCE);
        rastro(jogador, fim, Particulas.poeira(TipoDeVarinha.TRANSFUSAO.cor(), 1.2F), 3);
        som(jogador, fim, SoundEvents.BEACON_POWER_SELECT, 0.6F, 1.8F);
        if (aliado != null) {
            int custo = Math.round(jogador.getMaxHealth() * 0.05F);
            int cura = custo + 3 * nivel;
            float sobra = cura - (aliado.getMaxHealth() - aliado.getHealth());
            aliado.heal(cura);
            if (sobra > 0 && aliado instanceof ServerPlayer outro) {
                Escudos.darBarreira(outro, Math.round(sobra));
            }
            Particulas.subindoEmVolta(aliado, ParticleTypes.HEART, 4, 0.0);
            jogador.hurt(jogador.damageSources().magic(), custo);
        } else if (inimigo != null) {
            Escudos.darBarreira(jogador, 5 + nivel);
            inimigo.addEffect(new MobEffectInstance(ModEfeitos.ENCANTADO, Turno.ticks(5)));
            Particulas.subindoEmVolta(inimigo, ParticleTypes.HEART, 4, 0.0);
            TalentosDoTier2.aoAcertarComVarinha(jogador, inimigo);
        }
    }

    // ---- Aliados ----

    // Terra viva: 4-6 (+0/+2) no alvo. Atirando num inimigo, o dano vira armadura de pedra no herói (que segura metade dos golpes).
    // Com pedra bastante (8 + 4 x nível), o disparo cria um guardião da terra do lado do alvo (ou onde o raio parar) com a pedra toda.
    // Com o guardião em campo, o dano cura ele; atirando no próprio guardião, também.
    private static void terraViva(ServerPlayer jogador, int nivel) {
        LivingEntity alvo = Mira.inimigo(jogador, ALCANCE);
        Vec3 fim = alvo != null ? alvo.getBoundingBox().getCenter() : Mira.ponto(jogador, ALCANCE);
        rastro(jogador, fim, Particulas.poeira(TipoDeVarinha.TERRA_VIVA.cor(), 1.3F), 3);
        som(jogador, fim, SoundEvents.STONE_HIT, 1.0F, 0.8F);
        int dano = TipoDeVarinha.TERRA_VIVA.sortearDano(jogador.getRandom(), nivel, 1);
        GuardiaoDaTerra guardiao = guardiaoDe(jogador);
        // Só vira pedra o disparo num inimigo (ou no guardião)
        boolean noGuardiao = guardiao != null && alvo == guardiao;
        int pedra = noGuardiao || ehInimigo(alvo) ? dano : 0;
        if (pedra > 0 && (guardiao == null || ArmaduraDePedra.pedra(jogador) > 0)) {
            ArmaduraDePedra.somar(jogador, nivel, pedra);
        }
        if (noGuardiao) {
            guardiao.receber(jogador, nivel, pedra);
            return;
        }
        if (alvo != null) {
            ferir(jogador, alvo, dano, true);
        }
        if (guardiao == null && ArmaduraDePedra.pedra(jogador) >= ArmaduraDePedra.paraOGuardiao(ArmaduraDePedra.nivel(jogador))) {
            // Nasce o guardião, do lado do alvo (virado pro herói), ou onde o raio parou
            ServerLevel mundo = jogador.serverLevel();
            GuardiaoDaTerra novo = ModEntidades.GUARDIAO_DA_TERRA.get().create(mundo);
            Vec3 lugar = novo == null ? null : lugarLivre(mundo, novo, alvo != null ? alvo.position() : fim, jogador.position());
            if (lugar == null) {
                return;
            }
            novo.moveTo(lugar.x, lugar.y, lugar.z, jogador.getYRot() + 180, 0.0F);
            novo.receber(jogador, nivel, ArmaduraDePedra.pedra(jogador));
            ArmaduraDePedra.tirar(jogador);
            if (alvo != null && ehInimigo(alvo)) {
                novo.setTarget(alvo);
            }
            mundo.addFreshEntity(novo);
            som(jogador, lugar, SoundEvents.STONE_PLACE, 1.0F, 0.6F);
        } else if (guardiao != null && alvo != null) {
            guardiao.receber(jogador, nivel, pedra);
            if (ehInimigo(alvo)) {
                guardiao.setTarget(alvo);
            }
        }
    }

    // O guardião da terra desse herói, se ele estiver no andar
    @Nullable
    public static GuardiaoDaTerra guardiaoDe(ServerPlayer jogador) {
        for (GuardiaoDaTerra guardiao : jogador.level().getEntitiesOfClass(GuardiaoDaTerra.class, jogador.getBoundingBox().inflate(256, 5, 256),
                guardiao -> guardiao.isAlive() && jogador.getUUID().equals(guardiao.donoDoAliado()))) {
            return guardiao;
        }
        return null;
    }

    private static boolean ehInimigo(@Nullable LivingEntity criatura) {
        return criatura instanceof MobDoShattered inimigo && !inimigo.aliado();
    }

    // Corrupção: sem dano. Se o poder da varinha (3 + nível/3) passar da resistência do inimigo, ele vira aliado
    // (e o herói ganha o XP e os itens dele, como se tivesse matado). Senão, ele fica com um efeito ruim por 6 + 3 x nível turnos,
    // que às vezes é dos fortes (a chance é poder / resistência).
    // A resistência é 1 + o XP dele, vezes 1 + 4 x (vida / vida máxima)², e cai com cada efeito ruim que ele já tem.
    private static final Map<Holder<MobEffect>, Float> EFEITOS_FRACOS = new LinkedHashMap<>();
    private static final Map<Holder<MobEffect>, Float> EFEITOS_FORTES = new LinkedHashMap<>();

    static {
        // Os de peso 0 não são postos pela varinha, mas contam como efeitos fracos/fortes pra resistência
        EFEITOS_FRACOS.put(ModEfeitos.FRACO, 2F);
        EFEITOS_FRACOS.put(ModEfeitos.VULNERAVEL, 2F);
        EFEITOS_FRACOS.put(ModEfeitos.ALEIJADO, 1F);
        EFEITOS_FRACOS.put(ModEfeitos.CEGO, 1F);
        EFEITOS_FRACOS.put(ModEfeitos.ATERRORIZADO, 1F);
        EFEITOS_FRACOS.put(ModEfeitos.RESFRIADO, 0F);
        EFEITOS_FRACOS.put(ModEfeitos.MUCO_CAUSTICO, 0F);
        EFEITOS_FRACOS.put(ModEfeitos.ENRAIZADO, 0F);
        EFEITOS_FRACOS.put(ModEfeitos.VERTIGEM, 0F);
        EFEITOS_FRACOS.put(ModEfeitos.SONOLENTO, 0F);
        EFEITOS_FRACOS.put(ModEfeitos.SANGRANDO, 0F);
        EFEITOS_FRACOS.put(ModEfeitos.VENENO, 0F);
        EFEITOS_FORTES.put(ModEfeitos.FURIOSO, 3F);
        EFEITOS_FORTES.put(ModEfeitos.LENTO, 2F);
        EFEITOS_FORTES.put(ModEfeitos.FEITICO, 2F);
        EFEITOS_FORTES.put(ModEfeitos.PARALISADO, 1F);
        EFEITOS_FORTES.put(ModEfeitos.ATORDOADO, 0F);
        EFEITOS_FORTES.put(ModEfeitos.ENCANTADO, 0F);
        EFEITOS_FORTES.put(ModEfeitos.SONO_MAGICO, 0F);
        EFEITOS_FORTES.put(ModEfeitos.CONGELADO, 0F);
        EFEITOS_FORTES.put(ModEfeitos.CONDENADO, 0F);
    }

    private static void corrupcao(ServerPlayer jogador, int nivel) {
        LivingEntity alvo = Mira.inimigo(jogador, ALCANCE);
        Vec3 fim = alvo != null ? alvo.getBoundingBox().getCenter() : Mira.ponto(jogador, ALCANCE);
        rastro(jogador, fim, Particulas.poeira(TipoDeVarinha.CORRUPCAO.cor(), 1.4F), 4);
        som(jogador, fim, SoundEvents.ILLUSIONER_CAST_SPELL, 0.8F, 0.8F);
        if (!(alvo instanceof MobDoShattered inimigo)) {
            return;
        }
        float poder = 3 + nivel / 3F;
        float resistencia = resistenciaACorrupcao(inimigo);
        // Quem já é aliado (ou está condenado) não pode ser corrompido de novo: ganha um efeito forte
        if (inimigo.aliado() || inimigo.hasEffect(ModEfeitos.CONDENADO)) {
            poder = resistencia - 0.001F;
        }
        if (poder > resistencia) {
            corromper(jogador, inimigo);
        } else if (jogador.getRandom().nextFloat() < poder / resistencia) {
            enfraquecer(jogador, inimigo, EFEITOS_FORTES, nivel);
        } else {
            enfraquecer(jogador, inimigo, EFEITOS_FRACOS, nivel);
        }
        TalentosDoTier2.aoAcertarComVarinha(jogador, inimigo);
    }

    // A resistência do inimigo à corrupção agora
    public static float resistenciaACorrupcao(MobDoShattered inimigo) {
        float resistencia;
        if (inimigo instanceof Mimico) {
            resistencia = 1 + Math.max(1, Andares.andarPelaAltura(inimigo.getBlockY()));
        } else if (inimigo instanceof EnxameDeMoscas && inimigo.xpAoMorrer() == 0) {
            // Os enxames divididos não dão XP, então contam como 3
            resistencia = 1 + 3;
        } else {
            resistencia = 1 + inimigo.xpAoMorrer();
        }
        float vida = inimigo.getHealth() / inimigo.getMaxHealth();
        resistencia *= 1 + 4 * vida * vida;
        for (MobEffectInstance efeito : inimigo.getActiveEffects()) {
            if (naLista(EFEITOS_FORTES, efeito.getEffect())) {
                resistencia *= 0.5F;
            } else if (naLista(EFEITOS_FRACOS, efeito.getEffect()) || efeito.getEffect().value().getCategory() == MobEffectCategory.HARMFUL) {
                resistencia *= 0.75F;
            }
        }
        // Pegando fogo também é um efeito ruim
        if (inimigo.isOnFire()) {
            resistencia *= 0.75F;
        }
        return resistencia;
    }

    private static boolean naLista(Map<Holder<MobEffect>, Float> lista, Holder<MobEffect> efeito) {
        for (Holder<MobEffect> daLista : lista.keySet()) {
            if (daLista.unwrapKey().isPresent() && efeito.is(daLista.unwrapKey().get())) {
                return true;
            }
        }
        return false;
    }

    // Um efeito que ele ainda não tem (se já tem todos, sobe pros fortes; se já tem todos os fortes, é corrompido)
    private static void enfraquecer(ServerPlayer jogador, MobDoShattered inimigo, Map<Holder<MobEffect>, Float> categoria, int nivel) {
        List<Holder<MobEffect>> possiveis = new ArrayList<>();
        List<Float> pesos = new ArrayList<>();
        float total = 0;
        for (Map.Entry<Holder<MobEffect>, Float> item : categoria.entrySet()) {
            if (item.getValue() > 0 && !inimigo.hasEffect(item.getKey()) && inimigo.canBeAffected(new MobEffectInstance(item.getKey(), 1))) {
                possiveis.add(item.getKey());
                pesos.add(item.getValue());
                total += item.getValue();
            }
        }
        if (possiveis.isEmpty()) {
            if (categoria == EFEITOS_FRACOS) {
                enfraquecer(jogador, inimigo, EFEITOS_FORTES, nivel);
            } else {
                corromper(jogador, inimigo);
            }
            return;
        }
        float dado = jogador.getRandom().nextFloat() * total;
        int escolhido = 0;
        while (escolhido < possiveis.size() - 1 && dado >= pesos.get(escolhido)) {
            dado -= pesos.get(escolhido);
            escolhido++;
        }
        inimigo.acordar();
        inimigo.addEffect(new MobEffectInstance(possiveis.get(escolhido), Turno.ticks(6 + 3 * nivel)));
        Particulas.subindoEmVolta(inimigo, Particulas.poeira(TipoDeVarinha.CORRUPCAO.cor(), 1.2F), 8, 0.0);
    }

    // Corrompe: vira aliado, e o herói ganha o XP e os itens dele. Os chefes (e os da missão do fantasma) não podem ser corrompidos: ficam condenados.
    private static void corromper(ServerPlayer jogador, MobDoShattered inimigo) {
        if (inimigo.aliado() || inimigo.hasEffect(ModEfeitos.CONDENADO)) {
            jogador.displayClientMessage(Component.translatable("mensagem.pixeldungeon.ja_corrompido").withStyle(ChatFormatting.GRAY), true);
            return;
        }
        if (inimigo.chefe()) {
            inimigo.addEffect(new MobEffectInstance(ModEfeitos.CONDENADO, MobEffectInstance.INFINITE_DURATION));
            Particulas.subindoEmVolta(inimigo, Particulas.poeira(0x330033, 1.5F), 20, 0.0);
            return;
        }
        inimigo.corromper(jogador);
        EventosDeCombate.darRecompensa(inimigo, jogador);
    }

    // Proteção: mirando numa guarda sua, ela melhora (ou se cura, se as guardas já gastam toda a energia da varinha);
    // mirando no chão, nasce uma guarda nova ali. Igual ao Shattered, se tiver alguém na mira, a guarda fica um
    // quadradinho antes dele. As guardas não podem gastar mais energia que as varinhas de proteção do herói (2 + nível cada).
    private static void protecao(ServerPlayer jogador, int nivel) {
        ServerLevel mundo = jogador.serverLevel();
        LivingEntity naMira = Mira.inimigo(jogador, ALCANCE);
        if (naMira instanceof Guarda guarda && jogador.getUUID().equals(guarda.donoDoAliado())) {
            rastro(jogador, guarda.getBoundingBox().getCenter(), Particulas.poeira(TipoDeVarinha.PROTECAO.cor(), 1.2F), 3);
            if (energiaDasGuardas(jogador) < energiaMaxima(jogador)) {
                guarda.melhorar(nivel);
            } else {
                guarda.curar(nivel, 1.0F);
            }
            som(jogador, guarda.position(), SoundEvents.AMETHYST_BLOCK_RESONATE, 1.0F, 1.2F);
            return;
        }
        Vec3 ponto = Mira.ponto(jogador, ALCANCE);
        if (naMira != null) {
            Vec3 volta = jogador.position().subtract(naMira.position()).multiply(1, 0, 1);
            if (volta.length() < QUADRADINHO * 1.5) {
                semLugarPraGuarda(jogador);
                return;
            }
            ponto = naMira.position().add(volta.normalize().scale(QUADRADINHO)).add(0, 0.5, 0);
        }
        rastro(jogador, ponto, Particulas.poeira(TipoDeVarinha.PROTECAO.cor(), 1.2F), 3);
        Guarda nova = ModEntidades.GUARDA.get().create(mundo);
        Vec3 chao = chaoAbaixo(mundo, ponto);
        if (nova == null || chao == null) {
            semLugarPraGuarda(jogador);
            return;
        }
        nova.moveTo(chao.x, chao.y, chao.z, 0.0F, 0.0F);
        if (!mundo.noCollision(nova) || !mundo.getEntitiesOfClass(LivingEntity.class, nova.getBoundingBox().inflate(0.2)).isEmpty()) {
            semLugarPraGuarda(jogador);
            return;
        }
        nova.nascer(jogador, nivel);
        mundo.addFreshEntity(nova);
        som(jogador, chao, SoundEvents.AMETHYST_BLOCK_RESONATE, 1.0F, 1.0F);
    }

    private static void semLugarPraGuarda(ServerPlayer jogador) {
        jogador.displayClientMessage(Component.translatable("mensagem.pixeldungeon.sem_lugar_pra_guarda").withStyle(ChatFormatting.GRAY), true);
    }

    // A energia que as guardas desse herói gastam (o nível de cada uma), no andar dele
    public static int energiaDasGuardas(ServerPlayer jogador) {
        int energia = 0;
        for (Guarda guarda : jogador.level().getEntitiesOfClass(Guarda.class, jogador.getBoundingBox().inflate(256, 5, 256),
                guarda -> guarda.isAlive() && jogador.getUUID().equals(guarda.donoDoAliado()))) {
            energia += guarda.nivel();
        }
        return energia;
    }

    // A energia das varinhas de proteção que o herói carrega (2 + nível de cada; o cajado com ela dentro também conta)
    public static int energiaMaxima(ServerPlayer jogador) {
        int energia = 0;
        List<ItemStack> pilhas = new ArrayList<>(jogador.getInventory().items);
        pilhas.addAll(jogador.getInventory().offhand);
        for (ItemStack pilha : pilhas) {
            if (pilha.getItem() instanceof ItemVarinha varinha && varinha.tipo() == TipoDeVarinha.PROTECAO) {
                energia += 2 + NivelDoItem.nivel(pilha);
            } else if (pilha.getItem() instanceof CajadoDoMago && CajadoDoMago.varinha(pilha) == TipoDeVarinha.PROTECAO) {
                energia += 2 + NivelDoItem.nivel(pilha);
            }
        }
        return energia;
    }

    // O chão embaixo de um ponto (até 12 blocos pra baixo), ou null se não tiver
    @Nullable
    private static Vec3 chaoAbaixo(ServerLevel mundo, Vec3 ponto) {
        BlockPos.MutableBlockPos lugar = BlockPos.containing(ponto).mutable();
        for (int i = 0; i < 12; i++) {
            BlockPos embaixo = lugar.below();
            if (!mundo.getBlockState(embaixo).getCollisionShape(mundo, embaixo).isEmpty()) {
                return new Vec3(ponto.x, lugar.getY(), ponto.z);
            }
            lugar.move(Direction.DOWN);
        }
        return null;
    }

    // Um lugar no chão perto de "perto" (do lado virado pra "frente") onde a criatura cabe
    @Nullable
    private static Vec3 lugarLivre(ServerLevel mundo, LivingEntity criatura, Vec3 perto, Vec3 frente) {
        Vec3 direcao = frente.subtract(perto).multiply(1, 0, 1);
        direcao = direcao.lengthSqr() < 0.01 ? new Vec3(1, 0, 0) : direcao.normalize();
        Vec3 lado = new Vec3(-direcao.z, 0, direcao.x);
        Vec3[] tentativas = { direcao.scale(1.5), direcao.scale(2.5), lado.scale(1.5), lado.scale(-1.5), Vec3.ZERO, direcao.scale(-1.5) };
        for (Vec3 desvio : tentativas) {
            Vec3 chao = chaoAbaixo(mundo, perto.add(desvio).add(0, 0.5, 0));
            if (chao == null) {
                continue;
            }
            criatura.moveTo(chao.x, chao.y, chao.z);
            if (mundo.noCollision(criatura)) {
                return chao;
            }
        }
        return null;
    }

    // ---- Cones ----

    // Rajada de fogo: cone de 5/7/9 quadradinhos (1/2/3 cargas) que põe fogo no chão e em quem estiver nele;
    // com 2 cargas também aleija por 4 turnos, com 3 paralisa por 4 turnos
    private static void rajadaDeFogo(ServerPlayer jogador, int nivel, int cargasUsadas) {
        double alcance = (3 + 2 * cargasUsadas) * QUADRADINHO;
        double angulo = 30 + 20 * cargasUsadas;
        ServerLevel mundo = jogador.serverLevel();
        for (Vec3 ponto : pontosDoCone(jogador, alcance, angulo)) {
            Particulas.explosao(mundo, ParticleTypes.FLAME, ponto.x, ponto.y + 0.5, ponto.z, 6, 0.8, 0.02);
            // Fogo no chão (menos no quadradinho colado no herói)
            if (ponto.distanceTo(jogador.position()) > QUADRADINHO * 1.5) {
                BlockPos lugar = BlockPos.containing(ponto);
                if (mundo.getBlockState(lugar).isAir() && mundo.getBlockState(lugar.below()).isFaceSturdy(mundo, lugar.below(), Direction.UP)) {
                    mundo.setBlockAndUpdate(lugar, BaseFireBlock.getState(mundo, lugar));
                }
            }
        }
        som(jogador, jogador.position(), SoundEvents.FIRECHARGE_USE, 1.0F, 0.8F);
        boolean primeiro = true;
        for (LivingEntity atingido : noCone(jogador, alcance, angulo)) {
            ferir(jogador, atingido, TipoDeVarinha.RAJADA_DE_FOGO.sortearDano(jogador.getRandom(), nivel, cargasUsadas), primeiro);
            primeiro = false;
            if (!atingido.isAlive()) {
                continue;
            }
            atingido.igniteForSeconds(8);
            atingido.removeEffect(ModEfeitos.CONGELADO);
            if (cargasUsadas == 2) {
                atingido.addEffect(new MobEffectInstance(ModEfeitos.ALEIJADO, Turno.ticks(4)));
            } else if (cargasUsadas >= 3) {
                atingido.addEffect(new MobEffectInstance(ModEfeitos.PARALISADO, Turno.ticks(4)));
            }
        }
    }

    // Maturação: cone de 4/6/8 quadradinhos (1/2/3 cargas). Cresce grama alta em (3,67 + nível/3) x cargas quadradinhos
    // e enraíza quem estiver no cone por 4 x cargas turnos.
    private static void maturacao(ServerPlayer jogador, int nivel, int cargasUsadas) {
        double alcance = (2 + 2 * cargasUsadas) * QUADRADINHO;
        double angulo = 20 + 10 * cargasUsadas;
        ServerLevel mundo = jogador.serverLevel();
        int grama = Math.round((3.67F + nivel / 3.0F) * cargasUsadas);
        List<Vec3> pontos = pontosDoCone(jogador, alcance, angulo);
        // Mais perto primeiro, igual ao Shattered (a grama cresce ao longo do raio)
        pontos.sort(Comparator.comparingDouble(ponto -> ponto.distanceToSqr(jogador.position())));
        for (Vec3 ponto : pontos) {
            Particulas.explosao(mundo, ParticleTypes.HAPPY_VILLAGER, ponto.x, ponto.y + 0.5, ponto.z, 4, 0.8, 0.0);
            if (grama > 0 && ponto.distanceTo(jogador.position()) > QUADRADINHO) {
                int x = (int) Math.floor(ponto.x / QUADRADINHO) * (int) QUADRADINHO;
                int z = (int) Math.floor(ponto.z / QUADRADINHO) * (int) QUADRADINHO;
                if (Grama.crescer(mundo, x, jogador.getBlockY(), z)) {
                    grama--;
                }
            }
        }
        som(jogador, jogador.position(), SoundEvents.GRASS_PLACE, 1.0F, 0.8F);
        for (LivingEntity atingido : noCone(jogador, alcance, angulo)) {
            TalentosDoTier2.prolongar(atingido, ModEfeitos.ENRAIZADO, 4 * cargasUsadas, 0);
            TalentosDoTier2.aoAcertarComVarinha(jogador, atingido);
        }
    }

    // O centro de cada quadradinho dentro do cone (na altura dos pés do herói)
    private static List<Vec3> pontosDoCone(ServerPlayer jogador, double alcance, double angulo) {
        List<Vec3> pontos = new ArrayList<>();
        Vec3 pe = jogador.position();
        int raio = (int) Math.ceil(alcance / QUADRADINHO);
        int tileX = (int) Math.floor(pe.x / QUADRADINHO);
        int tileZ = (int) Math.floor(pe.z / QUADRADINHO);
        for (int dx = -raio; dx <= raio; dx++) {
            for (int dz = -raio; dz <= raio; dz++) {
                Vec3 ponto = new Vec3((tileX + dx + 0.5) * QUADRADINHO, pe.y, (tileZ + dz + 0.5) * QUADRADINHO);
                if (dentroDoCone(jogador, ponto, alcance, angulo) && enxerga(jogador, ponto.add(0, 1.0, 0))) {
                    pontos.add(ponto);
                }
            }
        }
        return pontos;
    }

    private static List<LivingEntity> noCone(ServerPlayer jogador, double alcance, double angulo) {
        List<LivingEntity> lista = new ArrayList<>();
        for (LivingEntity criatura : jogador.level().getEntitiesOfClass(LivingEntity.class, jogador.getBoundingBox().inflate(alcance),
                criatura -> podeAtingir(jogador, criatura))) {
            Vec3 centro = criatura.getBoundingBox().getCenter();
            if (dentroDoCone(jogador, centro, alcance, angulo) && enxerga(jogador, centro)) {
                lista.add(criatura);
            }
        }
        return lista;
    }

    private static boolean dentroDoCone(ServerPlayer jogador, Vec3 ponto, double alcance, double angulo) {
        Vec3 ate = ponto.subtract(jogador.getEyePosition()).multiply(1, 0, 1);
        double distancia = ate.length();
        if (distancia > alcance) {
            return false;
        }
        if (distancia < QUADRADINHO * 0.75) {
            return true;
        }
        Vec3 olhar = jogador.getViewVector(1.0F).multiply(1, 0, 1).normalize();
        double cosseno = ate.normalize().dot(olhar);
        return cosseno >= Math.cos(Math.toRadians(angulo / 2.0));
    }

    private static boolean enxerga(ServerPlayer jogador, Vec3 ponto) {
        Vec3 olho = jogador.getEyePosition();
        return jogador.level().clip(new ClipContext(olho, ponto, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, jogador)).getType() == HitResult.Type.MISS;
    }

    // ---- Varinha amaldiçoada ----

    // Igual ao Shattered, a magia de uma varinha amaldiçoada sai aleatória (e quase sempre ruim):
    // nada, fogo ou gelo onde o raio para, gás tóxico, grama em volta, ou quem lançou é teletransportado
    public static void lancarAmaldicoada(ServerPlayer jogador) {
        ServerLevel mundo = jogador.serverLevel();
        Vec3 lugar = ondeORaioPara(jogador);
        rastro(jogador, lugar, Particulas.poeira(0x5A1E5A, 1.3F), 3);
        String mensagem;
        switch (jogador.getRandom().nextInt(6)) {
            case 0 -> {
                EfeitosDePocao.incendiar(mundo, lugar);
                mensagem = "fogo";
            }
            case 1 -> {
                EfeitosDePocao.congelar(mundo, lugar);
                mensagem = "gelo";
            }
            case 2 -> {
                NuvemDeGas.soltar(mundo, lugar.subtract(0, 1.0, 0), NuvemDeGas.Tipo.TOXICO, 6.0F, 10);
                mensagem = "gas";
            }
            case 3 -> {
                BlockPos pe = jogador.blockPosition();
                for (int dx = -1; dx <= 1; dx++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        int x = (Math.floorDiv(pe.getX(), 4) + dx) * 4;
                        int z = (Math.floorDiv(pe.getZ(), 4) + dz) * 4;
                        Grama.crescer(mundo, x, pe.getY(), z);
                    }
                }
                mensagem = "grama";
            }
            case 4 -> {
                Teleporte.paraLugarAleatorio(mundo, jogador);
                mensagem = "teletransporte";
            }
            default -> mensagem = "nada";
        }
        jogador.displayClientMessage(Component.translatable("mensagem.pixeldungeon.varinha_amaldicoada." + mensagem).withStyle(ChatFormatting.DARK_PURPLE),
                false);
        som(jogador, jogador.position(), SoundEvents.ILLUSIONER_MIRROR_MOVE, 0.8F, 0.6F);
    }

    // ---- Ajudantes ----

    // Dano mágico da varinha (a armadura não segura; quem lançou leva o crédito da morte)
    private static void ferir(ServerPlayer jogador, LivingEntity alvo, int dano, boolean alvoPrincipal) {
        // Refeição empoderadora: +2/+3 no alvo principal dos próximos 3 disparos
        if (alvoPrincipal) {
            dano += Talentos.bonusDaVarinha(jogador);
        }
        alvo.invulnerableTime = 0;
        if (dano > 0) {
            alvo.hurt(fonte(jogador), dano);
        }
        // Visão arcana do Mago
        if (alvo.isAlive()) {
            TalentosDoTier2.aoAcertarComVarinha(jogador, alvo);
        }
    }

    // Dano de magia "de longe" (sem ser corpo a corpo: não passa pelo golpe do Shattered)
    public static DamageSource fonte(ServerPlayer jogador) {
        return new DamageSource(jogador.level().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(DamageTypes.INDIRECT_MAGIC),
                null, jogador);
    }

    // Quem a magia pode atingir: tudo que está vivo, menos os heróis e as imagens espelhadas
    private static boolean podeAtingir(ServerPlayer jogador, LivingEntity criatura) {
        return criatura.isAlive() && criatura != jogador && !(criatura instanceof Player) && !(criatura instanceof ImagemEspelhada) && !criatura.isSpectator();
    }

    private static List<LivingEntity> inimigosEmVolta(ServerPlayer jogador, Vec3 centro, double alcance) {
        return jogador.level().getEntitiesOfClass(LivingEntity.class, new AABB(centro, centro).inflate(alcance),
                criatura -> podeAtingir(jogador, criatura) && criatura.position().distanceTo(centro) <= alcance);
    }

    // Um raio que para no primeiro inimigo (ou na parede): o rastro e quem foi atingido
    @Nullable
    private static LivingEntity raioAteOAlvo(ServerPlayer jogador, TipoDeVarinha tipo, ParticleOptions particula) {
        LivingEntity alvo = Mira.inimigo(jogador, ALCANCE);
        Vec3 fim = alvo != null ? alvo.getBoundingBox().getCenter() : Mira.ponto(jogador, ALCANCE);
        rastro(jogador, fim, Particulas.poeira(tipo.cor(), 1.2F), 3);
        rastro(jogador, fim, particula, 1);
        return alvo;
    }

    // Onde o raio para: no primeiro inimigo, ou um pouco antes da parede
    private static Vec3 ondeORaioPara(ServerPlayer jogador) {
        LivingEntity alvo = Mira.inimigo(jogador, ALCANCE);
        return alvo != null ? alvo.getBoundingBox().getCenter() : Mira.ponto(jogador, ALCANCE);
    }

    private static void rastro(ServerPlayer jogador, Vec3 fim, ParticleOptions particula, int porBloco) {
        Vec3 inicio = jogador.getEyePosition().add(jogador.getViewVector(1.0F).scale(0.8)).subtract(0, 0.3, 0);
        linha(jogador.serverLevel(), inicio, fim, particula, porBloco);
    }

    private static void linha(ServerLevel mundo, Vec3 inicio, Vec3 fim, ParticleOptions particula, int porBloco) {
        Vec3 caminho = fim.subtract(inicio);
        int passos = Math.max(1, (int) (caminho.length() * porBloco));
        for (int i = 0; i <= passos; i++) {
            Vec3 ponto = inicio.add(caminho.scale(i / (double) passos));
            mundo.sendParticles(particula, ponto.x, ponto.y, ponto.z, 1, 0.02, 0.02, 0.02, 0.0);
        }
    }

    private static void som(ServerPlayer jogador, Vec3 lugar, SoundEvent som, float volume, float tom) {
        jogador.level().playSound(null, lugar.x, lugar.y, lugar.z, som, SoundSource.PLAYERS, volume, tom);
    }
}
