package com.louzada.pixeldungeon.heroi;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import com.louzada.pixeldungeon.combate.Particulas;
import com.louzada.pixeldungeon.entidade.LuzGuia;
import com.louzada.pixeldungeon.item.Artefato;
import com.louzada.pixeldungeon.item.Cargas;
import com.louzada.pixeldungeon.item.NivelDoItem;
import com.louzada.pixeldungeon.item.TomoSagrado;
import com.louzada.pixeldungeon.registro.ModComponentes;
import com.louzada.pixeldungeon.registro.ModEfeitos;

import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

// As magias do tier 1 do tomo sagrado (Shattered):
//   luz guia (1 carga): raio de 2-8 de dano que ilumina o alvo (o próximo golpe físico acerta com certeza)
//   arma celestial (2 cargas): +2 de dano mágico em cada golpe por 50 turnos
//   armadura sagrada (1 carga): bloqueia mais 1 de dano em cada golpe por 50 turnos
//   intuição divina (talento, 3/2 cargas): diz se o item na outra mão é amaldiçoado
//   escudo de luz (talento, 1 carga): 2-4 / 3-6 de armadura contra o inimigo na mira por 5 turnos
public class MagiasDoClerigo {
    public static final String ESCUDO_ALVO = "escudo_de_luz_alvo";
    private static final int TURNO = 20;

    public enum Magia {
        LUZ_GUIA("luz_guia"),
        ARMA_CELESTIAL("arma_celestial"),
        ARMADURA_SAGRADA("armadura_sagrada"),
        INTUICAO_DIVINA("intuicao_divina"),
        ESCUDO_DE_LUZ("escudo_de_luz");

        private String id;

        Magia(String id) {
            this.id = id;
        }

        public String id() {
            return id;
        }

        public String chaveDoNome() {
            return "magia.pixeldungeon." + id;
        }

        public String chaveDaDescricao() {
            return "magia.pixeldungeon." + id + ".descricao";
        }

        public int custo(int pontosNoTalento) {
            return switch (this) {
                case ARMA_CELESTIAL -> 2;
                case INTUICAO_DIVINA -> 4 - pontosNoTalento;
                default -> 1;
            };
        }

        @Nullable
        public static Magia peloId(String id) {
            for (Magia magia : values()) {
                if (magia.id.equals(id)) {
                    return magia;
                }
            }
            return null;
        }
    }

    // As magias que o Clérigo conhece (duas vêm de talentos)
    public static List<Magia> conhecidas(int pontosIntuicao, int pontosEscudo) {
        List<Magia> lista = new ArrayList<>(List.of(Magia.LUZ_GUIA, Magia.ARMA_CELESTIAL, Magia.ARMADURA_SAGRADA));
        if (pontosIntuicao > 0) {
            lista.add(Magia.INTUICAO_DIVINA);
        }
        if (pontosEscudo > 0) {
            lista.add(Magia.ESCUDO_DE_LUZ);
        }
        return lista;
    }

    public static int pontosPraCusto(DadosDoHeroi dados, Magia magia) {
        return magia == Magia.INTUICAO_DIVINA ? dados.pontos(Talento.INTUICAO_DIVINA) : 0;
    }

    // O jogador escolheu uma magia na tela do tomo
    public static void lancar(ServerPlayer jogador, Magia magia) {
        DadosDoHeroi dados = RegrasDoHeroi.dados(jogador);
        if (dados.classe() != ClasseDoHeroi.CLERIGO || !GerenciadorDoHeroi.emJogo(jogador)
                || !conhecidas(dados.pontos(Talento.INTUICAO_DIVINA), dados.pontos(Talento.ESCUDO_DE_LUZ)).contains(magia)) {
            return;
        }
        ItemStack tomo = TomoSagrado.acharTomo(jogador);
        if (tomo == null || jogador.getCooldowns().isOnCooldown(tomo.getItem())) {
            return;
        }
        long agora = jogador.level().getGameTime();
        int maximo = Artefato.cargasMaximas(tomo);
        Cargas.recarregar(tomo, agora, maximo, Artefato::ticksParaCarregar);
        int custo = magia.custo(pontosPraCusto(dados, magia));
        if (Cargas.cargas(tomo, maximo) < custo) {
            jogador.displayClientMessage(Component.translatable("mensagem.pixeldungeon.tomo_sem_carga").withStyle(ChatFormatting.GRAY), true);
            return;
        }

        boolean lancou = switch (magia) {
            case LUZ_GUIA -> luzGuia(jogador, tomo);
            case ARMA_CELESTIAL -> abencoar(jogador, ModEfeitos.ARMA_CELESTIAL, "magia.pixeldungeon.arma_celestial.lancada");
            case ARMADURA_SAGRADA -> abencoar(jogador, ModEfeitos.ARMADURA_SAGRADA, "magia.pixeldungeon.armadura_sagrada.lancada");
            case INTUICAO_DIVINA -> intuicaoDivina(jogador);
            case ESCUDO_DE_LUZ -> escudoDeLuz(jogador, dados);
        };
        if (!lancou) {
            return;
        }
        // Igual ao Shattered: lançar tira a invisibilidade, ativa as magias saciadas e gasta as cargas
        Furtividade.revelar(jogador);
        Talentos.aoLancarMagia(jogador);
        Cargas.gastar(tomo, agora, maximo, custo);
        Artefato.ganharExperiencia(jogador, tomo, dados.nivel(), custo, "mensagem.pixeldungeon.tomo_subiu");
    }

    private static boolean luzGuia(ServerPlayer jogador, ItemStack tomo) {
        LuzGuia luz = new LuzGuia(jogador.level(), jogador);
        luz.shootFromRotation(jogador, jogador.getXRot(), jogador.getYRot(), 0.0F, 2.0F, 0.0F);
        jogador.level().addFreshEntity(luz);
        jogador.level().playSound(null, jogador.blockPosition(), SoundEvents.ILLUSIONER_CAST_SPELL, SoundSource.PLAYERS, 0.8F, 1.6F);
        // Lançar a luz gasta um turno
        jogador.getCooldowns().addCooldown(tomo.getItem(), TURNO);
        return true;
    }

    // Arma celestial e armadura sagrada: 50 turnos, sem gastar tempo
    private static boolean abencoar(ServerPlayer jogador, Holder<MobEffect> efeito, String chave) {
        jogador.addEffect(new MobEffectInstance(efeito, 50 * TURNO));
        Particulas.subindoEmVolta(jogador, Particulas.poeira(0xFFEE77, 1.2F), 20, 0.0);
        Particulas.subindoEmVolta(jogador, ParticleTypes.END_ROD, 8, 0.08);
        jogador.level().playSound(null, jogador.blockPosition(), SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 0.8F, 1.2F);
        jogador.displayClientMessage(Component.translatable(chave).withStyle(ChatFormatting.YELLOW), true);
        return true;
    }

    // Intuição divina: no mod ainda não existem itens amaldiçoados, então a resposta é sempre "não"
    private static boolean intuicaoDivina(ServerPlayer jogador) {
        ItemStack item = jogador.getOffhandItem();
        if (item.isEmpty() || item.getItem() instanceof TomoSagrado) {
            jogador.displayClientMessage(Component.translatable("magia.pixeldungeon.intuicao_divina.sem_item").withStyle(ChatFormatting.GRAY), true);
            return false;
        }
        // Revela se a arma tem maldição (os outros itens nunca são amaldiçoados)
        boolean amaldicoado = NivelDoItem.amaldicoado(item);
        if (NivelDoItem.podeIdentificar(item)) {
            item.remove(ModComponentes.MALDICAO_CONHECIDA.get());
        }
        jogador.displayClientMessage(Component.translatable(amaldicoado ? "magia.pixeldungeon.intuicao_divina.amaldicoado"
                : "magia.pixeldungeon.intuicao_divina.limpo", item.getHoverName())
                .withStyle(amaldicoado ? ChatFormatting.DARK_PURPLE : ChatFormatting.YELLOW), false);
        Particulas.subindoEmVolta(jogador, ParticleTypes.END_ROD, 6, 0.05);
        jogador.level().playSound(null, jogador.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.0F, 1.2F);
        return true;
    }

    // Escudo de luz: contra o inimigo na mira
    private static boolean escudoDeLuz(ServerPlayer jogador, DadosDoHeroi dados) {
        LivingEntity alvo = Mira.inimigo(jogador, 32);
        if (alvo == null) {
            jogador.displayClientMessage(Component.translatable("magia.pixeldungeon.escudo_de_luz.sem_alvo").withStyle(ChatFormatting.GRAY), true);
            return false;
        }
        dados.definirAlvo(ESCUDO_ALVO, alvo.getUUID());
        jogador.addEffect(new MobEffectInstance(ModEfeitos.ESCUDO_DE_LUZ, 5 * TURNO));
        Particulas.subindoEmVolta(jogador, Particulas.poeira(0xFFFFCC, 1.3F), 20, 0.0);
        jogador.level().playSound(null, jogador.blockPosition(), SoundEvents.BEACON_POWER_SELECT, SoundSource.PLAYERS, 0.6F, 1.6F);
        return true;
    }
}
