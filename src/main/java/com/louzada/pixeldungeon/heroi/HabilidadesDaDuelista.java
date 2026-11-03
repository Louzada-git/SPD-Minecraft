package com.louzada.pixeldungeon.heroi;

import javax.annotation.Nullable;

import com.louzada.pixeldungeon.Turno;
import com.louzada.pixeldungeon.combate.NumerosFlutuantes;
import com.louzada.pixeldungeon.combate.Particulas;
import com.louzada.pixeldungeon.entidade.MobDoShattered;
import com.louzada.pixeldungeon.item.ArmaDoShattered;
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
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
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
//   golpe pesado (maça, machadinha, clava, martelo): acerto garantido; de surpresa dá +3 a +6; atordoa por 5 turnos
public class HabilidadesDaDuelista {
    public static final String CARGAS = "habilidade_cargas";
    public static final String PARCIAL = "habilidade_parcial";
    private static final String COMBO = "combo_golpes";
    private static final String COMBO_ATE = "combo_ate";
    private static final String CORTE_GRATIS = "corte_gratis";
    // Estocada: alcance (1 quadradinho vazio entre a Duelista e o inimigo)
    private static final double ALCANCE_DA_ESTOCADA = 10;
    private static final int BLOCOS_POR_QUADRADINHO = 4;

    public enum Habilidade {
        CORTAR("cortar"),
        FURTIVO("furtivo"),
        COMBINACAO("combinacao"),
        ESTOCADA("estocada"),
        GOLPE_PESADO("golpe_pesado");

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
        int custo = habilidade == Habilidade.CORTAR && dados.ativo(CORTE_GRATIS, agora) ? 0 : 1;
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
        float dano = CombateDoHeroi.calcular(jogador, alvo, jogador.getMainHandItem(), 1, 1, 1, bonus, true, false);
        jogador.swing(InteractionHand.MAIN_HAND, true);
        CombateDoHeroi.aplicar(alvo, dano, jogador.damageSources().playerAttack(jogador));
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
}
