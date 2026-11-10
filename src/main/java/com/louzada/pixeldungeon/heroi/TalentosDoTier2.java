package com.louzada.pixeldungeon.heroi;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import com.louzada.pixeldungeon.Turno;
import com.louzada.pixeldungeon.combate.NumerosFlutuantes;
import com.louzada.pixeldungeon.combate.Particulas;
import com.louzada.pixeldungeon.dungeon.Grama;
import com.louzada.pixeldungeon.efeito.EfeitosNoTick;
import com.louzada.pixeldungeon.entidade.MobDoShattered;
import com.louzada.pixeldungeon.item.ArmaDoShattered;
import com.louzada.pixeldungeon.item.Artefato;
import com.louzada.pixeldungeon.item.ArtefatoAchado;
import com.louzada.pixeldungeon.item.Baga;
import com.louzada.pixeldungeon.item.CajadoDoMago;
import com.louzada.pixeldungeon.item.CapaDasSombras;
import com.louzada.pixeldungeon.item.Cargas;
import com.louzada.pixeldungeon.item.ItemVarinha;
import com.louzada.pixeldungeon.item.RacaoDeSuprimentos;
import com.louzada.pixeldungeon.item.TipoDePergaminho;
import com.louzada.pixeldungeon.item.TipoDePocao;
import com.louzada.pixeldungeon.item.TomoSagrado;
import com.louzada.pixeldungeon.item.Varinha;
import com.louzada.pixeldungeon.rede.PacoteAtaquePronto;
import com.louzada.pixeldungeon.registro.ModEfeitos;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.network.PacketDistributor;

// O que cada talento do tier 2 faz (os números são os do Shattered).
// A comida, as poções, os pergaminhos, o cajado, o combate e o tick do herói chamam estes métodos na hora certa.
public class TalentosDoTier2 {
    // A barrinha de recarga do ataque cheia (o golpe não gastou tempo)
    public static final int ATAQUE_CARREGADO = 10000;

    // Nomes dos efeitos guardados no estado do herói
    public static final String INSCRITO_VEZES = "poder_inscrito_vezes";
    public static final String ULTIMO_PERGAMINHO = "ultimo_pergaminho";
    public static final String ULTIMO_PERGAMINHO_RECENTE = "ultimo_pergaminho_10";
    public static final String ULTIMO_PERGAMINHO_ANTIGO = "ultimo_pergaminho_300";
    public static final String IMPROVISADO_RECARGA = "projeteis_improvisados";
    public static final String REJUVENESCER_RECARGA = "passos_rejuvenescedores";
    public static final String RECARGA_DE_ARMA_TURNOS = "recarga_de_arma_turnos";
    public static final String AGIL_ATAQUE = "agilidade_liquida_ataque";
    public static final String EMPUNHAR_RECARGA = "empunhar_agil";
    public static final String EMPUNHAR_USOS = "empunhar_agil_usos";
    public static final String ULTIMA_ARMA = "ultima_arma";
    public static final String RAIO_SOLAR_CEGOU = "pixeldungeon_raio_solar";

    private static final int QUADRADINHO = 4;

    // ---- Comida ----

    // Os talentos de refeição do tier 2 fazem comer levar só 1 turno
    // (a Caçadora come bagas e o Ladino come rações de suprimentos na hora)
    public static int turnosPraComer(ServerPlayer jogador, ItemStack comida, int normal) {
        DadosDoHeroi dados = RegrasDoHeroi.dados(jogador);
        if (dados.classe() == ClasseDoHeroi.CACADORA && dados.pontos(Talento.REFEICAO_REVIGORANTE) > 0 && comida.getItem() instanceof Baga) {
            return 0;
        }
        if (dados.classe() == ClasseDoHeroi.LADINO && dados.pontos(Talento.REFEICAO_MISTICA) > 0 && comida.getItem() instanceof RacaoDeSuprimentos) {
            return 0;
        }
        for (Talento refeicao : List.of(Talento.ESTOMAGO_DE_FERRO, Talento.REFEICAO_ENERGETICA, Talento.REFEICAO_MISTICA, Talento.REFEICAO_REVIGORANTE,
                Talento.REFEICAO_FOCADA, Talento.REFEICAO_ILUMINADA)) {
            if (dados.pontos(refeicao) > 0) {
                return Math.min(normal, 1);
            }
        }
        return normal;
    }

    public static void aoComer(ServerPlayer jogador, DadosDoHeroi dados) {
        int pontos;
        // Refeição energética (Mago): 5/8 turnos de recarga de varinha
        pontos = dados.pontos(Talento.REFEICAO_ENERGETICA);
        if (pontos > 0) {
            prolongar(jogador, ModEfeitos.RECARREGANDO, 2 + 3 * pontos, 0);
        }
        // Refeição mística (Ladino): 3/5 turnos de recarga de artefato
        pontos = dados.pontos(Talento.REFEICAO_MISTICA);
        if (pontos > 0) {
            prolongar(jogador, ModEfeitos.RECARGA_DE_ARTEFATO, 1 + 2 * pontos, 0);
        }
        // Refeição revigorante (Caçadora): 1/2 turnos de pressa
        pontos = dados.pontos(Talento.REFEICAO_REVIGORANTE);
        if (pontos > 0) {
            prolongar(jogador, ModEfeitos.PRESSA, pontos, 0);
        }
        // Refeição focada (Duelista): 0,67/1 carga de arma
        pontos = dados.pontos(Talento.REFEICAO_FOCADA);
        if (pontos > 0) {
            HabilidadesDaDuelista.somarCarga(dados, (1 + pontos) / 3.0F);
        }
        // Refeição iluminada (Clérigo): 0,67/1 carga do tomo
        pontos = dados.pontos(Talento.REFEICAO_ILUMINADA);
        if (pontos > 0) {
            ItemStack tomo = TomoSagrado.acharTomo(jogador);
            if (tomo != null) {
                Cargas.adiantar(tomo, jogador.level().getGameTime(), Artefato.cargasMaximas(tomo), Artefato::ticksParaCarregar, (1 + pontos) / 3.0F);
            }
        }
    }

    // ---- Dano recebido ----

    // Estômago de ferro (Guerreiro): comendo, resiste a 75%/100% do dano
    public static void aoReceberDano(LivingIncomingDamageEvent evento) {
        if (!(evento.getEntity() instanceof ServerPlayer jogador) || !jogador.hasEffect(ModEfeitos.COMENDO) || !GerenciadorDoHeroi.emJogo(jogador)) {
            return;
        }
        int pontos = RegrasDoHeroi.dados(jogador).pontos(Talento.ESTOMAGO_DE_FERRO);
        if (pontos > 0) {
            evento.setAmount(evento.getAmount() * (1.0F - (0.5F + 0.25F * pontos)));
        }
    }

    // ---- Poções (beber ou arremessar) ----

    // As poções de força e de experiência valem em dobro, igual ao Shattered
    public static void aoUsarPocao(ServerPlayer jogador, TipoDePocao tipo) {
        aoUsarPocao(jogador, tipo == TipoDePocao.FORCA || tipo == TipoDePocao.EXPERIENCIA ? 2 : 1);
    }

    // "dobro": 2 nas poções que valem em dobro (força, experiência e as exóticas delas), 1 nas outras
    public static void aoUsarPocao(ServerPlayer jogador, int dobro) {
        if (!GerenciadorDoHeroi.emJogo(jogador)) {
            return;
        }
        DadosDoHeroi dados = RegrasDoHeroi.dados(jogador);
        int pontos;
        // Tenacidade líquida (Guerreiro): 6,5%/10% da vida máxima em blindagem
        pontos = dados.pontos(Talento.TENACIDADE_LIQUIDA);
        if (pontos > 0) {
            Escudos.darBarreira(jogador, Math.max(1, Math.round(jogador.getMaxHealth() * (0.03F + 0.035F * pontos) * dobro)));
        }
        // Natureza líquida (Caçadora): até 4/6 quadradinhos de grama alta em volta e os inimigos colados enraizados por 1/2 turnos
        pontos = dados.pontos(Talento.NATUREZA_LIQUIDA);
        if (pontos > 0) {
            naturezaLiquida(jogador, (2 + 2 * pontos) * dobro, pontos * dobro);
        }
        // Agilidade líquida (Duelista): esquiva 3x (infinita) por 5 turnos e o próximo golpe de perto 3x mais preciso (certeiro)
        pontos = dados.pontos(Talento.AGILIDADE_LIQUIDA);
        if (pontos > 0) {
            prolongar(jogador, ModEfeitos.AGILIDADE_LIQUIDA, 4 + dobro, pontos - 1);
            dados.definir(AGIL_ATAQUE, pontos);
            dados.ativarPor(AGIL_ATAQUE + "_ate", jogador.level().getGameTime(), Turno.ticks(4 + dobro));
            Particulas.subindoEmVolta(jogador, Particulas.poeira(0xB070FF, 1.0F), 12, 0.1);
        }
    }

    private static void naturezaLiquida(ServerPlayer jogador, int quadradinhos, int turnos) {
        BlockPos pe = jogador.blockPosition();
        int tileX = Math.floorDiv(pe.getX(), QUADRADINHO);
        int tileZ = Math.floorDiv(pe.getZ(), QUADRADINHO);
        List<int[]> vizinhos = new ArrayList<>();
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if (dx != 0 || dz != 0) {
                    vizinhos.add(new int[] { tileX + dx, tileZ + dz });
                }
            }
        }
        Collections.shuffle(vizinhos, new Random(jogador.getRandom().nextLong()));
        int crescidos = 0;
        for (int[] tile : vizinhos) {
            if (crescidos >= quadradinhos) {
                break;
            }
            if (Grama.crescer(jogador.serverLevel(), tile[0] * QUADRADINHO, pe.getY(), tile[1] * QUADRADINHO)) {
                crescidos++;
            }
        }
        for (MobDoShattered inimigo : inimigosPerto(jogador, 1.5 * QUADRADINHO)) {
            prolongar(inimigo, ModEfeitos.ENRAIZADO, turnos, 0);
            Particulas.subindoEmVolta(inimigo, Particulas.poeira(0x5A8A2A, 1.0F), 8, 0.0);
        }
    }

    // ---- Pergaminhos ----

    // Leu um pergaminho (ou repetiu com recuperar inscrição). Melhoria e transmutação valem em dobro.
    // (o pergaminho repetido pela recuperar inscrição não ativa os talentos, igual ao Shattered)
    public static void aoLerPergaminho(ServerPlayer jogador, TipoDePergaminho tipo, boolean repetido) {
        if (repetido || !GerenciadorDoHeroi.emJogo(jogador)) {
            return;
        }
        DadosDoHeroi dados = RegrasDoHeroi.dados(jogador);
        long agora = jogador.level().getGameTime();
        int dobro = tipo == TipoDePergaminho.MELHORIA || tipo == TipoDePergaminho.TRANSMUTACAO ? 2 : 1;
        int pontos;
        // Poder inscrito (Mago): os próximos 2/3 disparos da varinha ganham +2 níveis
        pontos = dados.pontos(Talento.PODER_INSCRITO);
        if (pontos > 0) {
            dados.definir(INSCRITO_VEZES, Math.max(dados.inteiro(INSCRITO_VEZES), (1 + pontos) * dobro));
            Particulas.subindoEmVolta(jogador, ParticleTypes.ENCHANT, 12, 0.3);
        }
        // Furtividade inscrita (Ladino): 3/5 turnos de invisibilidade
        pontos = dados.pontos(Talento.FURTIVIDADE_INSCRITA);
        if (pontos > 0) {
            Furtividade.ficarInvisivel(jogador, Turno.ticks((1 + 2 * pontos) * dobro));
        }
        // O Clérigo lembra do último pergaminho (pra recuperar inscrição)
        dados.definir(ULTIMO_PERGAMINHO, tipo.ordinal() + 1);
        dados.ativarPor(ULTIMO_PERGAMINHO_RECENTE, agora, Turno.ticks(10));
        dados.ativarPor(ULTIMO_PERGAMINHO_ANTIGO, agora, Turno.ticks(300));
    }

    // ---- Inimigos dormindo ----

    // Passos silenciosos (Ladino): quem dorme não acorda com ele a 3 quadradinhos ou mais (+1), ou sem estar colado (+2)
    public static boolean passosSilenciosos(Player jogador, double distanciaEmQuadradinhos) {
        if (!(jogador instanceof ServerPlayer heroi) || !GerenciadorDoHeroi.emJogo(heroi)) {
            return false;
        }
        int pontos = RegrasDoHeroi.dados(heroi).pontos(Talento.PASSOS_SILENCIOSOS);
        return pontos >= 2 ? distanciaEmQuadradinhos >= 1.5 : pontos == 1 && distanciaEmQuadradinhos >= 3;
    }

    // ---- Varinha (cajado do Mago) ----

    // Poder inscrito: +2 níveis neste disparo (gasta uma das vezes)
    public static int niveisInscritos(ServerPlayer jogador) {
        DadosDoHeroi dados = RegrasDoHeroi.dados(jogador);
        int vezes = dados.inteiro(INSCRITO_VEZES);
        if (vezes <= 0) {
            return 0;
        }
        dados.definir(INSCRITO_VEZES, vezes - 1);
        return 2;
    }

    // Visão arcana (Mago): quem leva o disparo brilha através das paredes por 10/15 turnos
    public static void aoAcertarComVarinha(ServerPlayer jogador, LivingEntity alvo) {
        int pontos = RegrasDoHeroi.dados(jogador).pontos(Talento.VISAO_ARCANA);
        if (pontos > 0) {
            alvo.addEffect(new MobEffectInstance(MobEffects.GLOWING, Turno.ticks(5 + 5 * pontos), 0, false, false));
        }
    }

    // Bateria de escudo (Mago): agachado, a varinha (ou o cajado) mira no próprio Mago e vira escudo (4%/6% da vida máxima por carga)
    public static boolean bateriaDeEscudo(ServerPlayer jogador, ItemStack varinha, int cargas, int maximo) {
        int pontos = RegrasDoHeroi.dados(jogador).pontos(Talento.BATERIA_DE_ESCUDO);
        if (pontos <= 0 || !jogador.isShiftKeyDown() || cargas <= 0) {
            return false;
        }
        long agora = jogador.level().getGameTime();
        Escudos.darBarreira(jogador, Math.max(1, Math.round(jogador.getMaxHealth() * (0.02F + 0.02F * pontos) * cargas)));
        Cargas.gastar(varinha, agora, maximo, cargas);
        jogador.level().playSound(null, jogador.blockPosition(), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 0.8F, 1.6F);
        return true;
    }

    // ---- Arremesso ----

    // Projéteis improvisados (Guerreiro): poção ou semente arremessada que acerta um inimigo cega por 2/3 turnos (recarga de 50 turnos)
    public static void aoAcertarArremesso(ServerPlayer jogador, LivingEntity alvo) {
        DadosDoHeroi dados = RegrasDoHeroi.dados(jogador);
        int pontos = dados.pontos(Talento.PROJETEIS_IMPROVISADOS);
        long agora = jogador.level().getGameTime();
        if (pontos <= 0 || !(alvo instanceof MobDoShattered) || dados.ativo(IMPROVISADO_RECARGA, agora)) {
            return;
        }
        EfeitosNoTick.cegar(alvo, Turno.ticks(1 + pontos));
        dados.ativarPor(IMPROVISADO_RECARGA, agora, Turno.ticks(50));
    }

    // ---- Mortes ----

    // Alguém matou um inimigo: ímpeto letal e pressa letal
    public static void aoMatar(ServerPlayer jogador, DamageSource fonte, boolean comHabilidade) {
        DadosDoHeroi dados = RegrasDoHeroi.dados(jogador);
        // Ímpeto letal (Guerreiro): golpe mortal de perto não gasta tempo (67%/100%)
        int pontos = dados.pontos(Talento.IMPETO_LETAL);
        if (pontos > 0 && fonte.getDirectEntity() == jogador && jogador.getRandom().nextInt(3) < 1 + pontos) {
            prepararAtaque(jogador);
        }
        // Pressa letal (Duelista): golpe mortal com habilidade dá 3/5 turnos de movimento muito rápido
        pontos = dados.pontos(Talento.PRESSA_LETAL);
        if (pontos > 0 && comHabilidade) {
            prolongar(jogador, ModEfeitos.PRESSA, 1 + 2 * pontos, 1);
        }
    }

    // O próximo golpe já está carregado (no servidor e no jogo do jogador)
    public static void prepararAtaque(ServerPlayer jogador) {
        jogador.attackStrengthTicker = ATAQUE_CARREGADO;
        PacketDistributor.sendToPlayer(jogador, new PacoteAtaquePronto());
    }

    // ---- Agilidade líquida no combate ----

    public static float esquiva(ServerPlayer jogador, float esquiva) {
        MobEffectInstance agil = jogador.getEffect(ModEfeitos.AGILIDADE_LIQUIDA);
        if (agil == null) {
            return esquiva;
        }
        return agil.getAmplifier() >= 1 ? 1_000_000.0F : esquiva * 3.0F;
    }

    // O próximo golpe de perto (gasta o bônus)
    public static float precisaoDePerto(ServerPlayer jogador, DadosDoHeroi dados, float precisao) {
        int pontos = dados.inteiro(AGIL_ATAQUE);
        if (pontos <= 0 || !dados.ativo(AGIL_ATAQUE + "_ate", jogador.level().getGameTime())) {
            return precisao;
        }
        return pontos >= 2 ? 1_000_000.0F : precisao * 3.0F;
    }

    public static void depoisDoGolpeDePerto(DadosDoHeroi dados) {
        dados.definir(AGIL_ATAQUE, 0);
    }

    // ---- Todo turno ----

    public static void aoTurno(ServerPlayer jogador, DadosDoHeroi dados) {
        long agora = jogador.level().getGameTime();
        boolean recarregando = jogador.hasEffect(ModEfeitos.RECARREGANDO);
        boolean recargaDeArtefato = jogador.hasEffect(ModEfeitos.RECARGA_DE_ARTEFATO);
        // Recarregando: o cajado e as varinhas ganham 1/4 de carga por turno
        if (recarregando) {
            for (ItemStack pilha : jogador.getInventory().items) {
                if (pilha.getItem() instanceof CajadoDoMago) {
                    Cargas.adiantar(pilha, agora, CajadoDoMago.cargasMaximas(pilha), Varinha::ticksParaCarregar, 0.25F);
                } else if (pilha.getItem() instanceof ItemVarinha) {
                    Cargas.adiantar(pilha, agora, ItemVarinha.cargasMaximas(pilha), Varinha::ticksParaCarregar, 0.25F);
                }
            }
        }
        // Recarga de artefato: a capa e o tomo ganham 1/4 de carga por turno; os artefatos achados equipados, cada um do seu jeito
        if (recargaDeArtefato) {
            for (ItemStack pilha : new ItemStack[] { CapaDasSombras.acharCapa(jogador), TomoSagrado.acharTomo(jogador) }) {
                if (pilha != null && !pilha.isEmpty()) {
                    Cargas.adiantar(pilha, agora, Artefato.cargasMaximas(pilha), Artefato::ticksParaCarregar, 0.25F);
                }
            }
            ArtefatoAchado.recargaDeFora(jogador, 1);
        }
        // Recarga de arma (Duelista): com recarga de varinha ou de artefato, 1 carga a cada 15/10 turnos
        int pontos = dados.pontos(Talento.RECARGA_DE_ARMA);
        if (pontos > 0 && (recarregando || recargaDeArtefato)) {
            int turnos = dados.inteiro(RECARGA_DE_ARMA_TURNOS) + 1;
            if (turnos >= 20 - 5 * pontos) {
                HabilidadesDaDuelista.somarCarga(dados, 1.0F);
                turnos = 0;
            }
            dados.definir(RECARGA_DE_ARMA_TURNOS, turnos);
        }
        // Passos rejuvenescedores (Caçadora): onde ela pisa no chão limpo cresce grama alta (recarga de 10/5 turnos)
        pontos = dados.pontos(Talento.PASSOS_REJUVENESCEDORES);
        if (pontos > 0 && !dados.ativo(REJUVENESCER_RECARGA, agora) && jogador.onGround()) {
            BlockPos pe = jogador.blockPosition();
            int inicioX = pe.getX() - Math.floorMod(pe.getX(), QUADRADINHO);
            int inicioZ = pe.getZ() - Math.floorMod(pe.getZ(), QUADRADINHO);
            if (Grama.crescer(jogador.serverLevel(), inicioX, pe.getY(), inicioZ)) {
                dados.ativarPor(REJUVENESCER_RECARGA, agora, Turno.ticks(15 - 5 * pontos));
            }
        }
        // Sentidos apurados (Caçadora): vê os inimigos a 2/3 quadradinhos através das paredes
        pontos = dados.pontos(Talento.SENTIDOS_APURADOS);
        if (pontos > 0) {
            revelarEmVolta(jogador, (1 + pontos) * QUADRADINHO);
        }
        // Sentido divino (magia do Clérigo): vê os inimigos a 8/12 quadradinhos
        MobEffectInstance sentido = jogador.getEffect(ModEfeitos.SENTIDO_DIVINO);
        if (sentido != null) {
            revelarEmVolta(jogador, (8 + 4 * sentido.getAmplifier()) * QUADRADINHO);
        }
    }

    // Os inimigos até essa distância brilham através das paredes (até o próximo turno)
    private static void revelarEmVolta(ServerPlayer jogador, double distancia) {
        for (MobDoShattered inimigo : inimigosPerto(jogador, distancia)) {
            inimigo.addEffect(new MobEffectInstance(MobEffects.GLOWING, Turno.TICKS + 5, 0, false, false));
        }
    }

    // ---- Todo tick ----

    // Empunhar ágil (Duelista): trocar de arma não gasta o golpe carregado, 1/2 vezes a cada 20 turnos
    public static void aoTick(ServerPlayer jogador, DadosDoHeroi dados) {
        int pontos = dados.pontos(Talento.EMPUNHAR_AGIL);
        if (pontos <= 0) {
            return;
        }
        ItemStack naMao = jogador.getMainHandItem();
        int arma = naMao.isEmpty() ? -1 : BuiltInRegistries.ITEM.getId(naMao.getItem());
        int antes = dados.inteiro(ULTIMA_ARMA);
        dados.definir(ULTIMA_ARMA, arma);
        if (antes == arma || antes == 0 || !(naMao.getItem() instanceof ArmaDoShattered)) {
            return;
        }
        long agora = jogador.level().getGameTime();
        if (!dados.ativo(EMPUNHAR_RECARGA, agora)) {
            dados.ativarPor(EMPUNHAR_RECARGA, agora, Turno.ticks(20));
            dados.definir(EMPUNHAR_USOS, pontos - 1);
        } else if (dados.inteiro(EMPUNHAR_USOS) > 0) {
            dados.definir(EMPUNHAR_USOS, dados.inteiro(EMPUNHAR_USOS) - 1);
        } else {
            return;
        }
        prepararAtaque(jogador);
        NumerosFlutuantes.mostrarTraduzido(jogador, "talento.pixeldungeon.empunhar_agil", NumerosFlutuantes.AMARELO);
    }

    // ---- Ajudantes ----

    // Um efeito por tantos turnos (se já tinha um mais longo, fica o mais longo)
    public static void prolongar(LivingEntity criatura, Holder<MobEffect> efeito, float turnos, int nivel) {
        MobEffectInstance atual = criatura.getEffect(efeito);
        int ticks = Turno.ticks(turnos);
        if (atual == null || atual.getDuration() < ticks || atual.getAmplifier() < nivel) {
            criatura.addEffect(new MobEffectInstance(efeito, Math.max(ticks, atual == null ? 0 : atual.getDuration()), Math.max(nivel,
                    atual == null ? 0 : atual.getAmplifier())));
        }
    }

    public static List<MobDoShattered> inimigosPerto(Player jogador, double distancia) {
        return jogador.level().getEntitiesOfClass(MobDoShattered.class, jogador.getBoundingBox().inflate(distancia),
                inimigo -> inimigo.isAlive() && inimigo.distanceTo(jogador) <= distancia);
    }
}
