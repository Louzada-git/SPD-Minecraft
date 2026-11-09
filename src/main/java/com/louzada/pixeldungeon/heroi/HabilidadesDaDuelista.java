package com.louzada.pixeldungeon.heroi;

import java.util.List;

import javax.annotation.Nullable;

import com.louzada.pixeldungeon.Turno;
import com.louzada.pixeldungeon.combate.Combate;
import com.louzada.pixeldungeon.combate.Combatente;
import com.louzada.pixeldungeon.combate.Golpe;
import com.louzada.pixeldungeon.combate.NumerosFlutuantes;
import com.louzada.pixeldungeon.combate.Particulas;
import com.louzada.pixeldungeon.efeito.EfeitosNoTick;
import com.louzada.pixeldungeon.entidade.MobDoShattered;
import com.louzada.pixeldungeon.item.ArmaDoShattered;
import com.louzada.pixeldungeon.item.EfeitosDeVarinha;
import com.louzada.pixeldungeon.item.NivelDoItem;
import com.louzada.pixeldungeon.registro.ModEfeitos;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

// Habilidades das armas da Duelista (tecla V). Igual ao Shattered:
//   - começa com 2 cargas; o máximo é 2 + (nível - 1) / 3, até 8
//   - recarrega 1 carga a cada 60 a 45 turnos (mais rápido quanto mais vazio)
//   - cada habilidade gasta 1 carga
//
// Cada arma tem a sua habilidade e o seu número (fica no ModItens). As armas mais fortes têm números maiores:
//   cortar (espadas): +3 a +7 de dano, acerto garantido; se matar, o próximo corte em 5 turnos é de graça
//   furtivo (adaga, punhal, lâmina assassina): pula até 5/4/3 quadradinhos pra onde está olhando e fica invisível por 2 turnos
//   combinação (luvas, sai): acerto garantido, +3/+4 de dano por golpe dado nos últimos 5 turnos
//   estocada (florete, katana): avança até um inimigo a 1 quadradinho de distância e golpeia com +5/+8, acerto garantido
//   golpe pesado (maça, machadinha, clava, machado de batalha, martelo): acerto garantido; de surpresa dá +3 a +6; atordoa por 5 turnos
//   espetar (lança, gládio): golpeia um inimigo no alcance, mas não do lado, com +9/+12, e empurra ele 1 quadradinho
//   postura defensiva (bastão): esquiva 3x maior por 4 turnos
//   ceifar (foice, foice de guerra): o golpe vira 15/30 de sangramento
//   dança da espada (cimitarra): 60% mais rápida e 50% mais precisa por 4 turnos
//   defender (escudos): anula todo ataque contra a Duelista por 5/3 turnos (depois de bloquear, atacar acaba a defesa)
//   chicotear (chicote): golpeia todos os inimigos no alcance, com acerto garantido
//   girar (mangual): até 3 voltas; o próximo golpe acerta com +8 por volta (só a primeira volta gasta carga)
//   corte rúnico (lâmina rúnica): acerto garantido, com o encantamento 300% mais forte
//   tiro carregado (besta): o próximo golpe acerta e arremessa o inimigo 4 quadradinhos
//   retribuição (machado grande): só com menos de metade da vida; +15 de dano, acerto garantido
public class HabilidadesDaDuelista {
    public static final String CARGAS = "habilidade_cargas";
    public static final String PARCIAL = "habilidade_parcial";
    private static final String COMBO = "combo_golpes";
    private static final String COMBO_ATE = "combo_ate";
    private static final String CORTE_GRATIS = "corte_gratis";
    private static final String GIRO = "giro_voltas";
    private static final String GIRO_ATE = "giro_ate";
    private static final String DEFESA_BLOQUEOU = "defesa_bloqueou";
    // Estocada: alcance (1 quadradinho vazio entre a Duelista e o inimigo)
    private static final double ALCANCE_DA_ESTOCADA = 10;
    private static final int BLOCOS_POR_QUADRADINHO = 4;

    public enum Habilidade {
        CORTAR("cortar"),
        FURTIVO("furtivo"),
        COMBINACAO("combinacao"),
        ESTOCADA("estocada"),
        GOLPE_PESADO("golpe_pesado"),
        ESPETAR("espetar"),
        POSTURA_DEFENSIVA("postura_defensiva"),
        CEIFAR("ceifar"),
        DANCA_DA_ESPADA("danca_da_espada"),
        DEFENDER("defender"),
        CHICOTEAR("chicotear"),
        GIRAR("girar"),
        CORTE_RUNICO("corte_runico"),
        TIRO_CARREGADO("tiro_carregado"),
        RETRIBUICAO("retribuicao");

        private String id;

        Habilidade(String id) {
            this.id = id;
        }

        public String chaveDoNome() {
            return "habilidade.pixeldungeon." + id;
        }

        public String chaveDaDescricao() {
            return "habilidade.pixeldungeon." + id + ".descricao";
        }
    }

    // A habilidade de cada arma (o cajado do Mago não tem)
    @Nullable
    public static Habilidade habilidadeDe(Item arma) {
        return arma instanceof ArmaDoShattered armaDoShattered ? armaDoShattered.habilidade() : null;
    }

    // Enquanto um golpe de habilidade está sendo aplicado (a pressa letal precisa saber se a morte veio de uma habilidade)
    private static boolean golpeDeHabilidade = false;

    public static boolean golpeDeHabilidade() {
        return golpeDeHabilidade;
    }

    // Corte rúnico: o poder de encantamento a mais durante o golpe (3 = +300%)
    private static float poderRunico = 0;

    public static float poderExtraDoGolpe() {
        return poderRunico;
    }

    // ---- Cargas ----

    public static int cargasMaximas(DadosDoHeroi dados) {
        return Math.min(8, 2 + (dados.nivel() - 1) / 3);
    }

    // Uma vez por turno
    public static void recarregar(DadosDoHeroi dados) {
        int cargas = dados.inteiro(CARGAS);
        int maximo = cargasMaximas(dados);
        if (cargas >= maximo) {
            dados.definir(PARCIAL, 0.0F);
            return;
        }
        float parcial = dados.decimal(PARCIAL) + 1.0F / (60.0F - 1.5F * (maximo - cargas));
        if (parcial >= 1.0F) {
            cargas++;
            parcial -= 1.0F;
        }
        dados.definir(CARGAS, cargas);
        dados.definir(PARCIAL, parcial);
    }

    // Ganha carga na hora (refeição focada, recarga de arma); o que sobrar de uma carga fica guardado
    public static void somarCarga(DadosDoHeroi dados, float quanto) {
        int cargas = dados.inteiro(CARGAS);
        int maximo = cargasMaximas(dados);
        float parcial = dados.decimal(PARCIAL) + quanto;
        while (parcial >= 1.0F && cargas < maximo) {
            cargas++;
            parcial -= 1.0F;
        }
        if (cargas >= maximo) {
            parcial = 0.0F;
        }
        dados.definir(CARGAS, cargas);
        dados.definir(PARCIAL, parcial);
    }

    // ---- Combinação das luvas ----

    // Cada golpe que acerta conta; 5 turnos sem acertar zera
    public static void contarGolpe(DadosDoHeroi dados, long agora) {
        int golpes = dados.ativo(COMBO_ATE, agora) ? dados.inteiro(COMBO) : 0;
        dados.definir(COMBO, golpes + 1);
        if (!dados.ativo(COMBO_ATE, agora + 5 * Turno.TICKS - 1)) {
            dados.ativarPor(COMBO_ATE, agora, 5 * Turno.TICKS);
        }
    }

    // Depois de matar, a combinação dura 15 turnos
    public static void aoMatar(DadosDoHeroi dados, long agora) {
        if (dados.ativo(COMBO_ATE, agora)) {
            dados.ativarPor(COMBO_ATE, agora, 15 * Turno.TICKS);
        }
    }

    public static int golpesSeguidos(DadosDoHeroi dados, long agora) {
        return dados.ativo(COMBO_ATE, agora) ? dados.inteiro(COMBO) : 0;
    }

    // ---- Usar ----

    public static void usar(ServerPlayer jogador) {
        DadosDoHeroi dados = RegrasDoHeroi.dados(jogador);
        if (!GerenciadorDoHeroi.emJogo(jogador)) {
            return;
        }
        if (dados.classe() != ClasseDoHeroi.DUELISTA) {
            avisar(jogador, "mensagem.pixeldungeon.habilidade_so_duelista");
            return;
        }
        Item arma = jogador.getMainHandItem().getItem();
        Habilidade habilidade = habilidadeDe(arma);
        if (habilidade == null || !(arma instanceof ArmaDoShattered armaDoShattered)) {
            avisar(jogador, "mensagem.pixeldungeon.habilidade_sem_arma");
            return;
        }
        if (dados.forca() < armaDoShattered.forcaNecessaria(jogador.getMainHandItem())) {
            avisar(jogador, "mensagem.pixeldungeon.habilidade_sem_forca");
            return;
        }
        long agora = jogador.level().getGameTime();
        // Cortar logo depois de matar e girar o mangual que já está girando são de graça
        int custo = (habilidade == Habilidade.CORTAR && dados.ativo(CORTE_GRATIS, agora)) || (habilidade == Habilidade.GIRAR && girando(dados, agora))
                ? 0 : 1;
        if (dados.inteiro(CARGAS) < custo) {
            avisar(jogador, "mensagem.pixeldungeon.habilidade_sem_carga");
            return;
        }

        boolean usou = switch (habilidade) {
            case CORTAR -> cortar(jogador, dados, armaDoShattered, agora);
            case FURTIVO -> furtivo(jogador, armaDoShattered);
            case COMBINACAO -> combinacao(jogador, dados, armaDoShattered, agora);
            case ESTOCADA -> estocada(jogador, armaDoShattered);
            case GOLPE_PESADO -> golpePesado(jogador, armaDoShattered);
            case ESPETAR -> espetar(jogador, armaDoShattered);
            case POSTURA_DEFENSIVA -> postura(jogador, armaDoShattered, ModEfeitos.POSTURA_DEFENSIVA);
            case CEIFAR -> ceifar(jogador, armaDoShattered);
            case DANCA_DA_ESPADA -> postura(jogador, armaDoShattered, ModEfeitos.DANCA_DA_ESPADA);
            case DEFENDER -> defender(jogador, dados, armaDoShattered);
            case CHICOTEAR -> chicotear(jogador, armaDoShattered);
            case GIRAR -> girar(jogador, dados, agora);
            case CORTE_RUNICO -> corteRunico(jogador, armaDoShattered);
            case TIRO_CARREGADO -> carregar(jogador);
            case RETRIBUICAO -> retribuicao(jogador, armaDoShattered);
        };
        if (!usou) {
            return;
        }
        dados.definir(CARGAS, dados.inteiro(CARGAS) - custo);
        NumerosFlutuantes.mostrarTraduzido(jogador, habilidade.chaveDoNome(), NumerosFlutuantes.AMARELO);
        // Barreira agressiva: com metade da vida ou menos, ganha 3/5 de blindagem
        int pontos = dados.pontos(Talento.BARREIRA_AGRESSIVA);
        if (pontos > 0 && jogador.getHealth() / jogador.getMaxHealth() <= 0.5F) {
            Escudos.darBarreira(jogador, 1 + 2 * pontos);
        }
    }

    // Golpe especial: acerto garantido, com dano a mais
    private static boolean golpear(ServerPlayer jogador, LivingEntity alvo, ArmaDoShattered arma, int bonus) {
        return golpear(jogador, alvo, Golpe.de(Combatente.de(jogador), Combatente.de(alvo)).arma(jogador.getMainHandItem()).bonus(bonus).acertoGarantido());
    }

    private static boolean golpear(ServerPlayer jogador, LivingEntity alvo, Golpe golpe) {
        float dano = Combate.resolver(golpe);
        jogador.swing(InteractionHand.MAIN_HAND, true);
        golpeDeHabilidade = true;
        try {
            Combate.aplicar(alvo, dano, jogador.damageSources().playerAttack(jogador));
        } finally {
            golpeDeHabilidade = false;
        }
        jogador.level().playSound(null, alvo.blockPosition(), SoundEvents.PLAYER_ATTACK_STRONG, SoundSource.PLAYERS, 1.0F, 0.9F);
        if (jogador.level() instanceof ServerLevel mundo) {
            mundo.sendParticles(ParticleTypes.SWEEP_ATTACK, alvo.getX(), alvo.getY(0.5), alvo.getZ(), 1, 0.0, 0.0, 0.0, 0.0);
        }
        return true;
    }

    @Nullable
    private static LivingEntity alvoDePerto(ServerPlayer jogador) {
        LivingEntity alvo = Mira.inimigo(jogador, Mira.alcanceCorpoACorpo(jogador));
        if (alvo == null) {
            avisar(jogador, "mensagem.pixeldungeon.habilidade_sem_alvo");
        }
        return alvo;
    }

    // Espadas: cortar
    private static boolean cortar(ServerPlayer jogador, DadosDoHeroi dados, ArmaDoShattered arma, long agora) {
        LivingEntity alvo = alvoDePerto(jogador);
        if (alvo == null) {
            return false;
        }
        boolean eraGratis = dados.ativo(CORTE_GRATIS, agora);
        golpear(jogador, alvo, arma, arma.valorDaHabilidade(jogador.getMainHandItem()));
        dados.desativar(CORTE_GRATIS);
        // Matou: o próximo corte nos próximos 5 turnos não gasta carga (mas não dá pra encadear dois de graça)
        if (!alvo.isAlive() && !eraGratis) {
            dados.ativarPor(CORTE_GRATIS, agora, 5 * Turno.TICKS);
        }
        return true;
    }

    // Adagas: furtivo
    private static boolean furtivo(ServerPlayer jogador, ArmaDoShattered arma) {
        ServerLevel mundo = jogador.serverLevel();
        Vec3 mira = Mira.ponto(jogador, arma.valorDaHabilidade(jogador.getMainHandItem()) * BLOCOS_POR_QUADRADINHO);
        Vec3 destino = lugarPraFicar(jogador, mira);
        if (destino == null) {
            avisar(jogador, "mensagem.pixeldungeon.habilidade_fora_de_alcance");
            return false;
        }
        Particulas.explosao(mundo, ParticleTypes.POOF, jogador.getX(), jogador.getY(0.5), jogador.getZ(), 10, 0.3, 0.02);
        jogador.teleportTo(destino.x, destino.y, destino.z);
        jogador.resetFallDistance();
        Particulas.explosao(mundo, ParticleTypes.POOF, destino.x, destino.y + 1, destino.z, 10, 0.3, 0.02);
        mundo.playSound(null, jogador.blockPosition(), SoundEvents.ILLUSIONER_MIRROR_MOVE, SoundSource.PLAYERS, 1.0F, 1.2F);
        // Igual ao Shattered: 2 turnos invisível, +1 por nível da arma
        Furtividade.ficarInvisivel(jogador, (2 + NivelDoItem.nivel(jogador.getMainHandItem())) * Turno.TICKS);
        return true;
    }

    // Luvas e sai: combinação
    private static boolean combinacao(ServerPlayer jogador, DadosDoHeroi dados, ArmaDoShattered arma, long agora) {
        LivingEntity alvo = alvoDePerto(jogador);
        if (alvo == null) {
            return false;
        }
        int golpes = golpesSeguidos(dados, agora);
        dados.definir(COMBO, 0);
        dados.desativar(COMBO_ATE);
        golpear(jogador, alvo, arma, arma.valorDaHabilidade(jogador.getMainHandItem()) * golpes);
        if (golpes >= 2) {
            Particulas.faiscas(alvo);
        }
        return true;
    }

    // Florete e katana: estocada
    private static boolean estocada(ServerPlayer jogador, ArmaDoShattered arma) {
        LivingEntity alvo = Mira.inimigo(jogador, ALCANCE_DA_ESTOCADA);
        if (alvo == null) {
            avisar(jogador, "mensagem.pixeldungeon.habilidade_sem_alvo");
            return false;
        }
        double distancia = jogador.distanceTo(alvo);
        if (distancia <= Mira.alcanceCorpoACorpo(jogador)) {
            // Igual ao Shattered: precisa ter espaço pra avançar
            avisar(jogador, "mensagem.pixeldungeon.habilidade_muito_perto");
            return false;
        }
        Vec3 direcao = alvo.position().subtract(jogador.position()).multiply(1, 0, 1).normalize();
        Vec3 destino = alvo.position().subtract(direcao.scale(alvo.getBbWidth() / 2 + 0.9));
        AABB caixa = jogador.getBoundingBox().move(destino.subtract(jogador.position()));
        if (!jogador.level().noCollision(jogador, caixa)) {
            avisar(jogador, "mensagem.pixeldungeon.habilidade_ocupado");
            return false;
        }
        if (jogador.level() instanceof ServerLevel mundo) {
            Particulas.explosao(mundo, ParticleTypes.CLOUD, jogador.getX(), jogador.getY() + 0.2, jogador.getZ(), 6, 0.2, 0.02);
        }
        jogador.teleportTo(destino.x, destino.y, destino.z);
        jogador.level().playSound(null, jogador.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.0F, 1.3F);
        return golpear(jogador, alvo, arma, arma.valorDaHabilidade(jogador.getMainHandItem()));
    }

    // Maças, machadinha e martelo: golpe pesado
    private static boolean golpePesado(ServerPlayer jogador, ArmaDoShattered arma) {
        LivingEntity alvo = alvoDePerto(jogador);
        if (alvo == null) {
            return false;
        }
        // Só dá o dano a mais de surpresa (igual ao Shattered)
        boolean surpresa = alvo instanceof MobDoShattered inimigo && inimigo.surpreendidoPor(jogador);
        golpear(jogador, alvo, arma, surpresa ? arma.valorDaHabilidade(jogador.getMainHandItem()) : 0);
        if (alvo.isAlive()) {
            alvo.addEffect(new MobEffectInstance(ModEfeitos.ATORDOADO, 5 * Turno.TICKS));
            NumerosFlutuantes.mostrarTraduzido(alvo, "efeito.pixeldungeon.atordoado.mostrar", NumerosFlutuantes.AMARELO);
            Particulas.subindoEmVolta(alvo, ParticleTypes.WAX_OFF, 8, 0.05);
        }
        return true;
    }

    // Um lugar perto do ponto onde dá pra ficar em pé (chão embaixo, espaço livre)
    @Nullable
    private static Vec3 lugarPraFicar(ServerPlayer jogador, Vec3 ponto) {
        BlockPos base = BlockPos.containing(ponto);
        for (int descida = 0; descida <= 8; descida++) {
            BlockPos lugar = base.below(descida);
            if (jogador.level().getBlockState(lugar.below()).isCollisionShapeFullBlock(jogador.level(), lugar.below())) {
                Vec3 destino = new Vec3(lugar.getX() + 0.5, lugar.getY(), lugar.getZ() + 0.5);
                AABB caixa = jogador.getBoundingBox().move(destino.subtract(jogador.position()));
                if (jogador.level().noCollision(jogador, caixa)) {
                    return destino;
                }
                return null;
            }
        }
        return null;
    }

    private static void avisar(ServerPlayer jogador, String chave) {
        jogador.displayClientMessage(Component.translatable(chave).withStyle(ChatFormatting.GRAY), true);
    }

    // ---- As habilidades das armas novas ----

    // Lança e gládio: espetar um inimigo no alcance, mas não do lado; ele é empurrado 1 quadradinho
    private static boolean espetar(ServerPlayer jogador, ArmaDoShattered arma) {
        LivingEntity alvo = alvoDePerto(jogador);
        if (alvo == null) {
            return false;
        }
        if (jogador.distanceTo(alvo) <= Mira.alcanceCorpoACorpo(jogador) - arma.alcanceExtra()) {
            avisar(jogador, "mensagem.pixeldungeon.habilidade_espetar_perto");
            return false;
        }
        golpear(jogador, alvo, arma, arma.valorDaHabilidade(jogador.getMainHandItem()));
        if (alvo.isAlive()) {
            Vec3 direcao = alvo.position().subtract(jogador.position()).multiply(1, 0, 1).normalize();
            EfeitosDeVarinha.empurrar(jogador, alvo, direcao, 1, false);
        }
        return true;
    }

    // Bastão e cimitarra: uma postura que dura alguns turnos (não leva tempo)
    private static boolean postura(ServerPlayer jogador, ArmaDoShattered arma, Holder<MobEffect> efeito) {
        jogador.addEffect(new MobEffectInstance(efeito, arma.valorDaHabilidade(jogador.getMainHandItem()) * Turno.TICKS, 0, false, false, true));
        jogador.level().playSound(null, jogador.blockPosition(), SoundEvents.ARMOR_EQUIP_CHAIN.value(), SoundSource.PLAYERS, 1.0F, 1.2F);
        Particulas.subindoEmVolta(jogador, ParticleTypes.CRIT, 8, 0.05);
        return true;
    }

    // Foices: o golpe não machuca na hora, vira sangramento (o dano do golpe depois da armadura)
    private static boolean ceifar(ServerPlayer jogador, ArmaDoShattered arma) {
        LivingEntity alvo = alvoDePerto(jogador);
        if (alvo == null) {
            return false;
        }
        Golpe golpe = Golpe.de(Combatente.de(jogador), Combatente.de(alvo)).arma(jogador.getMainHandItem()).multiplicador(0)
                .bonus(arma.valorDaHabilidade(jogador.getMainHandItem())).acertoGarantido();
        float sangue = Combate.resolver(golpe);
        jogador.swing(InteractionHand.MAIN_HAND, true);
        if (sangue > 0) {
            EfeitosNoTick.sangrar(alvo, Math.round(sangue));
            Particulas.sangue(alvo, Particulas.SANGUE_PADRAO, Math.round(sangue));
        }
        if (alvo instanceof MobDoShattered inimigo) {
            inimigo.acordar();
        }
        jogador.level().playSound(null, alvo.blockPosition(), SoundEvents.PLAYER_ATTACK_STRONG, SoundSource.PLAYERS, 1.0F, 0.8F);
        return true;
    }

    // Escudos: defender por alguns turnos
    private static boolean defender(ServerPlayer jogador, DadosDoHeroi dados, ArmaDoShattered arma) {
        jogador.addEffect(new MobEffectInstance(ModEfeitos.DEFENDENDO, arma.valorDaHabilidade(jogador.getMainHandItem()) * Turno.TICKS, 0, false, false,
                true));
        dados.definir(DEFESA_BLOQUEOU, 0);
        jogador.level().playSound(null, jogador.blockPosition(), SoundEvents.ARMOR_EQUIP_IRON.value(), SoundSource.PLAYERS, 1.0F, 1.0F);
        return true;
    }

    // Defendendo, um ataque contra a Duelista (de perto, de longe ou mágico) é anulado. Devolve se anulou.
    public static boolean defendeu(ServerPlayer jogador) {
        if (!jogador.hasEffect(ModEfeitos.DEFENDENDO)) {
            return false;
        }
        RegrasDoHeroi.dados(jogador).definir(DEFESA_BLOQUEOU, 1);
        NumerosFlutuantes.mostrarTraduzido(jogador, "efeito.pixeldungeon.defendeu", NumerosFlutuantes.BRANCO);
        jogador.level().playSound(null, jogador.blockPosition(), SoundEvents.SHIELD_BLOCK, SoundSource.PLAYERS, 1.0F, 1.0F);
        return true;
    }

    // Depois de bloquear um ataque, atacar ou usar magia acaba a defesa (igual ao Shattered)
    public static void aoAgir(ServerPlayer jogador) {
        if (jogador.hasEffect(ModEfeitos.DEFENDENDO) && RegrasDoHeroi.dados(jogador).inteiro(DEFESA_BLOQUEOU) > 0) {
            jogador.removeEffect(ModEfeitos.DEFENDENDO);
        }
    }

    // Chicote: golpeia todos os inimigos à vista dentro do alcance, sem dano a mais
    private static boolean chicotear(ServerPlayer jogador, ArmaDoShattered arma) {
        double alcance = Mira.alcanceCorpoACorpo(jogador);
        List<LivingEntity> alvos = jogador.level().getEntitiesOfClass(LivingEntity.class, jogador.getBoundingBox().inflate(alcance),
                criatura -> criatura instanceof MobDoShattered inimigo && !inimigo.aliado() && criatura.isAlive() && jogador.distanceTo(criatura) <= alcance
                        && jogador.hasLineOfSight(criatura));
        if (alvos.isEmpty()) {
            avisar(jogador, "mensagem.pixeldungeon.habilidade_sem_alvo");
            return false;
        }
        jogador.level().playSound(null, jogador.blockPosition(), SoundEvents.FISHING_BOBBER_RETRIEVE, SoundSource.PLAYERS, 1.0F, 1.4F);
        for (LivingEntity alvo : alvos) {
            golpear(jogador, alvo, arma, 0);
        }
        return true;
    }

    // Mangual: mais uma volta (até 3); cada volta dura 3 turnos
    public static boolean girando(DadosDoHeroi dados, long agora) {
        return dados.ativo(GIRO_ATE, agora) && dados.inteiro(GIRO) > 0;
    }

    private static boolean girar(ServerPlayer jogador, DadosDoHeroi dados, long agora) {
        int voltas = girando(dados, agora) ? dados.inteiro(GIRO) : 0;
        if (voltas >= 3) {
            avisar(jogador, "mensagem.pixeldungeon.habilidade_giro_maximo");
            return false;
        }
        voltas++;
        dados.definir(GIRO, voltas);
        dados.ativarPor(GIRO_ATE, agora, 3 * Turno.TICKS);
        jogador.addEffect(new MobEffectInstance(ModEfeitos.GIRANDO, 3 * Turno.TICKS, voltas - 1, false, false, true));
        jogador.level().playSound(null, jogador.blockPosition(), SoundEvents.CHAIN_PLACE, SoundSource.PLAYERS, 1.0F, 0.9F + 0.1F * voltas);
        return true;
    }

    // Lâmina rúnica: golpe com acerto garantido e o encantamento mais forte
    private static boolean corteRunico(ServerPlayer jogador, ArmaDoShattered arma) {
        LivingEntity alvo = alvoDePerto(jogador);
        if (alvo == null) {
            return false;
        }
        poderRunico = arma.valorDaHabilidade(jogador.getMainHandItem()) / 100F;
        try {
            golpear(jogador, alvo, arma, 0);
        } finally {
            poderRunico = 0;
        }
        Particulas.subindoEmVolta(alvo, ParticleTypes.ENCHANT, 12, 0.1);
        return true;
    }

    // Besta: carrega o próximo golpe
    private static boolean carregar(ServerPlayer jogador) {
        if (jogador.hasEffect(ModEfeitos.CARREGADO)) {
            avisar(jogador, "mensagem.pixeldungeon.habilidade_ja_carregada");
            return false;
        }
        jogador.addEffect(new MobEffectInstance(ModEfeitos.CARREGADO, MobEffectInstance.INFINITE_DURATION, 0, false, false, true));
        jogador.level().playSound(null, jogador.blockPosition(), SoundEvents.TRIPWIRE_ATTACH, SoundSource.PLAYERS, 1.0F, 0.8F);
        return true;
    }

    // Machado grande: só com menos de metade da vida
    private static boolean retribuicao(ServerPlayer jogador, ArmaDoShattered arma) {
        if (jogador.getHealth() / jogador.getMaxHealth() >= 0.5F) {
            avisar(jogador, "mensagem.pixeldungeon.habilidade_vida_alta");
            return false;
        }
        LivingEntity alvo = alvoDePerto(jogador);
        if (alvo == null) {
            return false;
        }
        return golpear(jogador, alvo, arma, arma.valorDaHabilidade(jogador.getMainHandItem()));
    }

    // ---- O golpe normal depois de girar o mangual ou carregar a besta ----

    // Antes do golpe: acerto garantido (e o dano das voltas do mangual)
    public static void antesDoGolpe(ServerPlayer jogador, Golpe golpe) {
        ItemStack arma = golpe.arma();
        Habilidade habilidade = habilidadeDe(arma.getItem());
        DadosDoHeroi dados = RegrasDoHeroi.dados(jogador);
        long agora = jogador.level().getGameTime();
        if (habilidade == Habilidade.GIRAR && girando(dados, agora) && arma.getItem() instanceof ArmaDoShattered mangual) {
            golpe.acertoGarantido().bonus(dados.inteiro(GIRO) * mangual.valorDaHabilidade(arma));
            dados.definir(GIRO, 0);
            dados.desativar(GIRO_ATE);
            jogador.removeEffect(ModEfeitos.GIRANDO);
            jogador.level().playSound(null, jogador.blockPosition(), SoundEvents.PLAYER_ATTACK_STRONG, SoundSource.PLAYERS, 1.0F, 0.8F);
        } else if (habilidade == Habilidade.TIRO_CARREGADO && jogador.hasEffect(ModEfeitos.CARREGADO)) {
            golpe.acertoGarantido();
        }
    }

    // Acertou: a besta carregada arremessa o inimigo 4 quadradinhos
    public static void aoAcertar(ServerPlayer jogador, Golpe golpe) {
        if (habilidadeDe(golpe.arma().getItem()) == Habilidade.TIRO_CARREGADO && jogador.hasEffect(ModEfeitos.CARREGADO)) {
            jogador.removeEffect(ModEfeitos.CARREGADO);
            LivingEntity alvo = golpe.alvo().entidade();
            Vec3 direcao = alvo.position().subtract(jogador.position()).multiply(1, 0, 1).normalize();
            EfeitosDeVarinha.empurrar(jogador, alvo, direcao, 4, true);
        }
    }
}
