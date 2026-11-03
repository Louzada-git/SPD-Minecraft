package com.louzada.pixeldungeon.heroi;

import com.louzada.pixeldungeon.combate.NumerosFlutuantes;
import com.louzada.pixeldungeon.combate.Particulas;
import com.louzada.pixeldungeon.entidade.MobDoShattered;
import com.louzada.pixeldungeon.item.ArmaduraDoShattered;
import com.louzada.pixeldungeon.registro.ModComponentes;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

// Blindagem (os "escudos" do Shattered). No Minecraft ela aparece como os corações amarelos (absorção).
// Tem dois tipos, que somados dão os corações amarelos:
//   - escudo do selo do Guerreiro: é gasto primeiro, some se ficar 5 turnos sem inimigos por perto
//   - barreira (talentos, magias...): vai sumindo devagar sozinha, igual à barreira do Shattered
// Quando um deles acaba por causa de dano, o talento Raiva Provocada prepara o próximo golpe.
public class Escudos {
    private static final String SELO = "escudo_selo";
    private static final String SELO_INICIAL = "escudo_selo_inicial";
    private static final String SELO_RECARGA = "selo_recarga";
    private static final String SEM_INIMIGOS = "selo_sem_inimigos";
    private static final String BARREIRA = "barreira";
    private static final String BARREIRA_PERDA = "barreira_perda";
    private static final String ABSORCAO = "absorcao";
    // Igual ao Shattered: o selo leva 150 turnos pra poder proteger de novo
    private static final int RECARGA_DO_SELO = 150;
    private static final double DISTANCIA_DOS_INIMIGOS = 32;
    public static final String COR_DO_ESCUDO = "#99ccff";

    // ---- Barreira ----

    // Igual à barreira do Shattered: fica com a maior entre a que já tinha e a nova
    public static void darBarreira(ServerPlayer jogador, int quantidade) {
        DadosDoHeroi dados = RegrasDoHeroi.dados(jogador);
        if (quantidade > dados.decimal(BARREIRA)) {
            dados.definir(BARREIRA, (float) quantidade);
            dados.definir(BARREIRA_PERDA, 0.0F);
            atualizar(jogador, dados);
        }
        NumerosFlutuantes.mostrar(jogador, "+" + quantidade, COR_DO_ESCUDO);
        Particulas.subindoEmVolta(jogador, Particulas.poeira(0x99CCFF, 1.0F), 10, 0.0);
    }

    // Soma um pouco de barreira (até um máximo), usado pelas sombras protetoras do Ladino
    public static void somarBarreira(ServerPlayer jogador, float quantidade, float maximo) {
        DadosDoHeroi dados = RegrasDoHeroi.dados(jogador);
        float nova = Math.min(maximo, dados.decimal(BARREIRA) + quantidade);
        if (nova > dados.decimal(BARREIRA)) {
            dados.definir(BARREIRA, nova);
            dados.definir(BARREIRA_PERDA, 0.0F);
            atualizar(jogador, dados);
        }
    }

    public static float barreira(ServerPlayer jogador) {
        return RegrasDoHeroi.dados(jogador).decimal(BARREIRA);
    }

    // ---- Selo quebrado do Guerreiro ----

    // A armadura no peito tem o selo?
    public static boolean temSelo(ServerPlayer jogador) {
        ItemStack peito = jogador.getItemBySlot(EquipmentSlot.CHEST);
        return peito.getItem() instanceof ArmaduraDoShattered && peito.getOrDefault(ModComponentes.SELO.get(), false);
    }

    // Igual ao Shattered: 3 + 2 por tier da armadura + determinação de ferro (pano: 5, 6 ou 7)
    public static int escudoDoSelo(ServerPlayer jogador) {
        int tier = jogador.getItemBySlot(EquipmentSlot.CHEST).getItem() instanceof ArmaduraDoShattered armadura ? armadura.tier() : 1;
        return 3 + 2 * tier + RegrasDoHeroi.dados(jogador).pontos(Talento.DETERMINACAO_DE_FERRO);
    }

    // Chamado depois que o combate calculou o dano (prioridade baixa): se o golpe vai deixar o Guerreiro
    // com metade da vida ou menos, o selo dá o escudo na hora (e esse mesmo golpe já bate no escudo)
    public static void aoReceberDano(LivingIncomingDamageEvent evento) {
        if (!(evento.getEntity() instanceof ServerPlayer jogador) || evento.getAmount() <= 0
                || evento.getSource().is(DamageTypes.STARVE) || !GerenciadorDoHeroi.emJogo(jogador) || !temSelo(jogador)) {
            return;
        }
        DadosDoHeroi dados = RegrasDoHeroi.dados(jogador);
        if (dados.inteiro(SELO_RECARGA) > 0) {
            return;
        }
        float vida = jogador.getHealth();
        float metade = jogador.getMaxHealth() / 2;
        if (vida <= metade || vida + jogador.getAbsorptionAmount() - evento.getAmount() <= metade) {
            int escudo = escudoDoSelo(jogador);
            dados.definir(SELO, dados.decimal(SELO) + escudo);
            dados.definir(SELO_INICIAL, escudo);
            dados.definir(SELO_RECARGA, RECARGA_DO_SELO);
            dados.definir(SEM_INIMIGOS, 0);
            atualizar(jogador, dados);
            NumerosFlutuantes.mostrar(jogador, "+" + escudo, COR_DO_ESCUDO);
            Particulas.subindoEmVolta(jogador, ParticleTypes.ENCHANTED_HIT, 16, 0.2);
            jogador.level().playSound(null, jogador.blockPosition(), SoundEvents.ARMOR_EQUIP_IRON.value(), SoundSource.PLAYERS, 1.0F, 0.8F);
        }
    }

    // Turnos que faltam pro selo poder proteger de novo
    public static int recargaDoSelo(ServerPlayer jogador) {
        return RegrasDoHeroi.dados(jogador).inteiro(SELO_RECARGA);
    }

    // ---- Todo tick ----

    public static void aoTick(ServerPlayer jogador, DadosDoHeroi dados, boolean umTurno) {
        float selo = dados.decimal(SELO);
        float barreira = dados.decimal(BARREIRA);

        // O Minecraft gastou corações amarelos com dano: tira primeiro do selo, depois da barreira
        float gasto = dados.decimal(ABSORCAO) - jogador.getAbsorptionAmount();
        if (gasto > 0.001F) {
            float doSelo = Math.min(selo, gasto);
            float daBarreira = Math.min(barreira, gasto - doSelo);
            boolean quebrou = (doSelo > 0 && selo - doSelo <= 0.001F) || (daBarreira > 0 && barreira - daBarreira <= 0.001F);
            selo -= doSelo;
            barreira -= daBarreira;
            if (quebrou && dados.pontos(Talento.RAIVA_PROVOCADA) > 0) {
                dados.ativarPor(Talentos.RAIVA, jogador.level().getGameTime(), 5 * 20);
            }
        }

        if (umTurno) {
            // A barreira vai sumindo (mais rápido quanto maior)
            if (barreira > 0) {
                float perda = dados.decimal(BARREIRA_PERDA) + Math.min(1.0F, barreira / 20.0F);
                if (perda >= 1.0F) {
                    barreira = Math.max(0, barreira - 1);
                    perda = 0;
                }
                dados.definir(BARREIRA_PERDA, perda);
            }
            // O selo recarrega
            int recarga = dados.inteiro(SELO_RECARGA);
            if (recarga > 0) {
                dados.definir(SELO_RECARGA, recarga - 1);
            }
            // Sem inimigos por 5 turnos, o escudo do selo some (e devolve até metade da recarga)
            if (selo > 0) {
                if (temInimigosPerto(jogador)) {
                    dados.definir(SEM_INIMIGOS, 0);
                } else {
                    int turnos = dados.inteiro(SEM_INIMIGOS) + 1;
                    dados.definir(SEM_INIMIGOS, turnos);
                    if (turnos >= 5) {
                        float sobrou = selo / Math.max(1, dados.inteiro(SELO_INICIAL));
                        dados.definir(SELO_RECARGA, Math.max(0, dados.inteiro(SELO_RECARGA) - Math.round(RECARGA_DO_SELO * sobrou / 2)));
                        selo = 0;
                    }
                }
            }
        }

        dados.definir(SELO, Math.max(0, selo));
        dados.definir(BARREIRA, Math.max(0, barreira));
        atualizar(jogador, dados);
    }

    // Os corações amarelos do Minecraft = escudo do selo + barreira
    private static void atualizar(ServerPlayer jogador, DadosDoHeroi dados) {
        float total = dados.decimal(SELO) + dados.decimal(BARREIRA);
        if (Math.abs(jogador.getAbsorptionAmount() - total) > 0.001F) {
            jogador.setAbsorptionAmount(total);
        }
        dados.definir(ABSORCAO, jogador.getAbsorptionAmount());
    }

    private static boolean temInimigosPerto(ServerPlayer jogador) {
        for (MobDoShattered inimigo : jogador.level().getEntitiesOfClass(MobDoShattered.class,
                jogador.getBoundingBox().inflate(DISTANCIA_DOS_INIMIGOS, 6, DISTANCIA_DOS_INIMIGOS))) {
            if (!inimigo.dormindo() && inimigo.hasLineOfSight(jogador)) {
                return true;
            }
        }
        return false;
    }
}
