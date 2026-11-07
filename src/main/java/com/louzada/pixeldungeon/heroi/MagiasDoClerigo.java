package com.louzada.pixeldungeon.heroi;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.ToIntFunction;

import javax.annotation.Nullable;

import com.louzada.pixeldungeon.Turno;
import com.louzada.pixeldungeon.combate.NumerosFlutuantes;
import com.louzada.pixeldungeon.combate.Particulas;
import com.louzada.pixeldungeon.entidade.LuzGuia;
import com.louzada.pixeldungeon.entidade.RaioSolar;
import com.louzada.pixeldungeon.item.Artefato;
import com.louzada.pixeldungeon.item.Cargas;
import com.louzada.pixeldungeon.item.EfeitosDePergaminho;
import com.louzada.pixeldungeon.item.EscolhaDeItem;
import com.louzada.pixeldungeon.item.NivelDoItem;
import com.louzada.pixeldungeon.item.TipoDePergaminho;
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

// As magias do tomo sagrado (Shattered):
//   luz guia (1 carga): raio de 2-8 de dano que ilumina o alvo (o próximo golpe físico acerta com certeza)
//   arma celestial (2 cargas): +2 de dano mágico em cada golpe por 50 turnos
//   armadura sagrada (1 carga): bloqueia mais 1 de dano em cada golpe por 50 turnos
// Dos talentos do tier 1:
//   intuição divina (3/2 cargas): diz se o item na outra mão é amaldiçoado
//   escudo de luz (1 carga): 2-4 / 3-6 de armadura contra o inimigo na mira por 5 turnos
// Dos talentos do tier 2:
//   recuperar inscrição (3 cargas, 6 na transmutação): repete o último pergaminho lido em 10/300 turnos (menos o de melhoria)
//   raio solar (1 carga): 4-8 / 6-12 de dano e cega por 4/6 turnos (quem já foi cegado pelo raio fica paralisado)
//   sentido divino (2 cargas): vê os inimigos a 8/12 quadradinhos através das paredes por 50 turnos
//   bênção (1 carga): no Clérigo, 6/10 turnos de bênção e 10/15 de blindagem; num aliado na mira, 10/15 de bênção e 10/15 de cura
public class MagiasDoClerigo {
    public static final String ESCUDO_ALVO = "escudo_de_luz_alvo";

    public enum Magia {
        LUZ_GUIA("luz_guia"),
        ARMA_CELESTIAL("arma_celestial"),
        ARMADURA_SAGRADA("armadura_sagrada"),
        INTUICAO_DIVINA("intuicao_divina"),
        ESCUDO_DE_LUZ("escudo_de_luz"),
        RECUPERAR_INSCRICAO("recuperar_inscricao"),
        RAIO_SOLAR("raio_solar"),
        SENTIDO_DIVINO("sentido_divino"),
        BENCAO("bencao");

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
                case ARMA_CELESTIAL, SENTIDO_DIVINO -> 2;
                case INTUICAO_DIVINA -> 4 - pontosNoTalento;
                case RECUPERAR_INSCRICAO -> 3;
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

    // Os talentos que ensinam cada magia (as 3 primeiras o Clérigo já sabe)
    private static final Map<Magia, Talento> TALENTO_DA_MAGIA = Map.of(Magia.INTUICAO_DIVINA, Talento.INTUICAO_DIVINA, Magia.ESCUDO_DE_LUZ,
            Talento.ESCUDO_DE_LUZ, Magia.RECUPERAR_INSCRICAO, Talento.RECUPERAR_INSCRICAO, Magia.RAIO_SOLAR, Talento.RAIO_SOLAR,
            Magia.SENTIDO_DIVINO, Talento.SENTIDO_DIVINO, Magia.BENCAO, Talento.BENCAO);

    // As magias que o Clérigo conhece (as dos talentos só com pelo menos 1 ponto)
    public static List<Magia> conhecidas(ToIntFunction<Talento> pontos) {
        List<Magia> lista = new ArrayList<>();
        for (Magia magia : Magia.values()) {
            Talento talento = TALENTO_DA_MAGIA.get(magia);
            if (talento == null || pontos.applyAsInt(talento) > 0) {
                lista.add(magia);
            }
        }
        return lista;
    }

    public static int pontosPraCusto(DadosDoHeroi dados, Magia magia) {
        return magia == Magia.INTUICAO_DIVINA ? dados.pontos(Talento.INTUICAO_DIVINA) : 0;
    }

    // O jogador escolheu uma magia na tela do tomo
    public static void lancar(ServerPlayer jogador, Magia magia) {
        DadosDoHeroi dados = RegrasDoHeroi.dados(jogador);
        if (dados.classe() != ClasseDoHeroi.CLERIGO || !GerenciadorDoHeroi.emJogo(jogador) || !conhecidas(dados::pontos).contains(magia)) {
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
        // Recuperar inscrição custa o dobro com a transmutação
        TipoDePergaminho lembrado = pergaminhoLembrado(dados, agora);
        if (magia == Magia.RECUPERAR_INSCRICAO && lembrado == TipoDePergaminho.TRANSMUTACAO) {
            custo *= 2;
        }
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
            case RECUPERAR_INSCRICAO -> recuperarInscricao(jogador, dados, lembrado);
            case RAIO_SOLAR -> raioSolar(jogador, tomo, dados.pontos(Talento.RAIO_SOLAR));
            case SENTIDO_DIVINO -> sentidoDivino(jogador, dados.pontos(Talento.SENTIDO_DIVINO));
            case BENCAO -> bencao(jogador, dados.pontos(Talento.BENCAO));
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
        jogador.getCooldowns().addCooldown(tomo.getItem(), Turno.TICKS);
        return true;
    }

    // Arma celestial e armadura sagrada: 50 turnos, sem gastar tempo
    private static boolean abencoar(ServerPlayer jogador, Holder<MobEffect> efeito, String chave) {
        jogador.addEffect(new MobEffectInstance(efeito, 50 * Turno.TICKS));
        Particulas.subindoEmVolta(jogador, Particulas.poeira(0xFFEE77, 1.2F), 20, 0.0);
        Particulas.subindoEmVolta(jogador, ParticleTypes.END_ROD, 8, 0.08);
        jogador.level().playSound(null, jogador.blockPosition(), SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 0.8F, 1.2F);
        jogador.displayClientMessage(Component.translatable(chave).withStyle(ChatFormatting.YELLOW), true);
        return true;
    }

    // Intuição divina: diz se a arma ou a armadura na outra mão tem maldição
    private static boolean intuicaoDivina(ServerPlayer jogador) {
        ItemStack item = jogador.getOffhandItem();
        if (item.isEmpty() || item.getItem() instanceof TomoSagrado) {
            jogador.displayClientMessage(Component.translatable("magia.pixeldungeon.intuicao_divina.sem_item").withStyle(ChatFormatting.GRAY), true);
            return false;
        }
        // Revela se a arma ou a armadura tem maldição (os outros itens nunca são amaldiçoados)
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
        jogador.addEffect(new MobEffectInstance(ModEfeitos.ESCUDO_DE_LUZ, 5 * Turno.TICKS));
        Particulas.subindoEmVolta(jogador, Particulas.poeira(0xFFFFCC, 1.3F), 20, 0.0);
        jogador.level().playSound(null, jogador.blockPosition(), SoundEvents.BEACON_POWER_SELECT, SoundSource.PLAYERS, 0.6F, 1.6F);
        return true;
    }

    // ---- Magias dos talentos do tier 2 ----

    // O último pergaminho lido, se ainda dá pra recuperar (10 turnos com +1, 300 com +2); melhoria nunca
    @Nullable
    public static TipoDePergaminho pergaminhoLembrado(DadosDoHeroi dados, long agora) {
        int numero = dados.inteiro(TalentosDoTier2.ULTIMO_PERGAMINHO);
        if (numero <= 0 || numero > TipoDePergaminho.values().length) {
            return null;
        }
        String janela = dados.pontos(Talento.RECUPERAR_INSCRICAO) >= 2 ? TalentosDoTier2.ULTIMO_PERGAMINHO_ANTIGO
                : TalentosDoTier2.ULTIMO_PERGAMINHO_RECENTE;
        TipoDePergaminho tipo = TipoDePergaminho.values()[numero - 1];
        return dados.ativo(janela, agora) && tipo != TipoDePergaminho.MELHORIA ? tipo : null;
    }

    // Recuperar inscrição: o efeito do último pergaminho de novo (os que pedem item abrem a tela de escolher).
    // Cada pergaminho lido só pode ser recuperado uma vez.
    private static boolean recuperarInscricao(ServerPlayer jogador, DadosDoHeroi dados, @Nullable TipoDePergaminho tipo) {
        if (tipo == null) {
            jogador.displayClientMessage(Component.translatable("magia.pixeldungeon.recuperar_inscricao.nada").withStyle(ChatFormatting.GRAY), true);
            return false;
        }
        if (tipo.escolheItem()) {
            if (!EscolhaDeItem.pedirSemPergaminho(jogador, tipo)) {
                jogador.displayClientMessage(Component.translatable("mensagem.pixeldungeon.pergaminho_sem_item").withStyle(ChatFormatting.GRAY), true);
                return false;
            }
        } else {
            EfeitosDePergaminho.ler(jogador, tipo);
        }
        dados.definir(TalentosDoTier2.ULTIMO_PERGAMINHO, 0);
        jogador.level().playSound(null, jogador.blockPosition(), SoundEvents.BOOK_PAGE_TURN, SoundSource.PLAYERS, 1.0F, 0.8F);
        Particulas.subindoEmVolta(jogador, ParticleTypes.ENCHANT, 16, 0.3);
        return true;
    }

    // Raio solar: um raio igual à luz guia, que cega (ou paralisa quem já foi cegado por ele)
    private static boolean raioSolar(ServerPlayer jogador, ItemStack tomo, int pontos) {
        RaioSolar raio = new RaioSolar(jogador.level(), jogador, pontos);
        raio.shootFromRotation(jogador, jogador.getXRot(), jogador.getYRot(), 0.0F, 2.0F, 0.0F);
        jogador.level().addFreshEntity(raio);
        jogador.level().playSound(null, jogador.blockPosition(), SoundEvents.ILLUSIONER_CAST_SPELL, SoundSource.PLAYERS, 0.8F, 1.9F);
        jogador.getCooldowns().addCooldown(tomo.getItem(), Turno.TICKS);
        return true;
    }

    // Sentido divino: 50 turnos vendo os inimigos perto através das paredes (não gasta tempo)
    private static boolean sentidoDivino(ServerPlayer jogador, int pontos) {
        jogador.addEffect(new MobEffectInstance(ModEfeitos.SENTIDO_DIVINO, 50 * Turno.TICKS, pontos - 1));
        Particulas.subindoEmVolta(jogador, Particulas.poeira(0xFFE65A, 1.2F), 16, 0.0);
        jogador.level().playSound(null, jogador.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.0F, 1.4F);
        jogador.displayClientMessage(Component.translatable("magia.pixeldungeon.sentido_divino.lancada").withStyle(ChatFormatting.YELLOW), true);
        return true;
    }

    // Bênção: num aliado na mira (outro jogador ou a imagem espelhada) ou no próprio Clérigo
    private static boolean bencao(ServerPlayer jogador, int pontos) {
        LivingEntity aliado = Mira.aliado(jogador, 32);
        if (aliado == null) {
            jogador.addEffect(new MobEffectInstance(ModEfeitos.ABENCOADO, Turno.ticks(2 + 4 * pontos)));
            Escudos.darBarreira(jogador, 5 + 5 * pontos);
            Particulas.subindoEmVolta(jogador, Particulas.poeira(0xFFFF88, 1.2F), 16, 0.0);
        } else {
            aliado.addEffect(new MobEffectInstance(ModEfeitos.ABENCOADO, Turno.ticks(5 + 5 * pontos)));
            int cura = 5 + 5 * pontos;
            float sobra = cura - (aliado.getMaxHealth() - aliado.getHealth());
            aliado.heal(cura);
            NumerosFlutuantes.mostrar(aliado, "+" + cura, NumerosFlutuantes.VERDE);
            // A cura que sobra vira blindagem
            if (sobra > 0 && aliado instanceof ServerPlayer outro) {
                Escudos.darBarreira(outro, Math.round(sobra));
            }
            Particulas.subindoEmVolta(aliado, Particulas.poeira(0xFFFF88, 1.2F), 16, 0.0);
        }
        jogador.level().playSound(null, jogador.blockPosition(), SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 0.8F, 1.4F);
        return true;
    }
}
