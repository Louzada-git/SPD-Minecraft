package com.louzada.pixeldungeon.run;

import java.util.List;
import java.util.Random;

import com.louzada.pixeldungeon.PixelDungeon;
import com.louzada.pixeldungeon.dungeon.Andares;
import com.louzada.pixeldungeon.dungeon.Armadilhas;
import com.louzada.pixeldungeon.dungeon.GeradorDeAndar;
import com.louzada.pixeldungeon.dungeon.PlantaDoAndar;
import com.louzada.pixeldungeon.dungeon.SaqueDoAndar;
import com.louzada.pixeldungeon.dungeon.TipoDeArmadilha;
import com.louzada.pixeldungeon.entidade.MobDoShattered;
import com.louzada.pixeldungeon.entidade.OvelhaMagica;
import com.louzada.pixeldungeon.heroi.Aneis;
import com.louzada.pixeldungeon.heroi.ClasseDoHeroi;
import com.louzada.pixeldungeon.heroi.DadosDoHeroi;
import com.louzada.pixeldungeon.heroi.RegrasDoHeroi;
import com.louzada.pixeldungeon.heroi.Talento;
import com.louzada.pixeldungeon.heroi.Talentos;
import com.louzada.pixeldungeon.item.Identificacao;
import com.louzada.pixeldungeon.item.NivelDoItem;
import com.louzada.pixeldungeon.item.Ouro;
import com.louzada.pixeldungeon.item.TomoSagrado;
import com.louzada.pixeldungeon.rede.RedeDoHeroi;
import com.louzada.pixeldungeon.registro.ModEfeitos;
import com.louzada.pixeldungeon.registro.ModEntidades;
import com.louzada.pixeldungeon.registro.ModItens;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

// Só no teste do GitHub que tira fotos da tela (propriedade pixeldungeon.fotografar):
// quem entra no mundo vira Guerreiro na hora, com alguns itens, um efeito, um pouco de fome e de dano,
// pra foto mostrar todas as partes da tela do jogo e do inventário.
public class HeroiDasFotos {

    public static boolean ligado() {
        return Boolean.getBoolean("pixeldungeon.fotografar");
    }

    public static void preparar(ServerPlayer jogador) {
        GerenciadorDaRun.escolherClasse(jogador, ClasseDoHeroi.GUERREIRO);
        long semente = Identificacao.sementeDaRun(jogador);
        Random sorteio = new Random(7);
        for (int i = 0; i < 3; i++) {
            jogador.getInventory().add(SaqueDoAndar.pocaoAleatoria(sorteio, semente));
            jogador.getInventory().add(SaqueDoAndar.pergaminhoAleatorio(sorteio, semente));
        }
        jogador.getInventory().add(SaqueDoAndar.sementeAleatoria(sorteio));
        jogador.getInventory().add(NivelDoItem.sortearDoChao(new ItemStack(ModItens.ESPADA.get()), sorteio, true));
        jogador.getInventory().add(new ItemStack(ModItens.KATANA.get()));
        jogador.getInventory().add(NivelDoItem.sortearDoChao(new ItemStack(ModItens.ARMADURA_DE_ESCAMAS.get()), sorteio, true));
        jogador.getInventory().add(SaqueDoAndar.varinhaAleatoria(sorteio));
        // Um anel no dedo (pra foto mostrar as casinhas de anel)
        jogador.setItemSlot(Aneis.PRIMEIRO, SaqueDoAndar.anelAleatorio(sorteio));
        Ouro.pegar(jogador, 87);
        RegrasDoHeroi.ganharXp(jogador, 13);
        jogador.addEffect(new MobEffectInstance(ModEfeitos.ABENCOADO, 20 * 60));
        jogador.addEffect(new MobEffectInstance(ModEfeitos.PRESSA, 20 * 30));
        jogador.getFoodData().setFoodLevel(6);
        jogador.setHealth(jogador.getMaxHealth() * 0.6F);
        // Os ratos do andar 1 vêm atacar enquanto as fotos são tiradas: o herói não pode morrer
        jogador.getAbilities().invulnerable = true;
        jogador.onUpdateAbilities();
        PixelDungeon.LOGGER.info("Herói das fotos pronto: {}", jogador.getName().getString());
    }

    // Os inimigos do esgoto dormindo em fila na frente do herói (pra foto dos desenhos do Shattered)
    public static void mostrarInimigos(ServerPlayer jogador) {
        DadosDaRun run = DadosDaRun.pegar(jogador.getServer());
        BlockPos entrada = run.andar(1).entrada();
        jogador.teleportTo(jogador.serverLevel(), entrada.getX() + 0.5, entrada.getY(), entrada.getZ() + 0.5, 0.0F, 10.0F);
        List<EntityType<? extends MobDoShattered>> tipos = List.of(ModEntidades.RATO_MARSUPIAL.get(), ModEntidades.RATO_ALBINO.get(),
                ModEntidades.COBRA_DE_ESGOTO.get(), ModEntidades.GNOLL_EXPLORADOR.get(), ModEntidades.CARANGUEJO_DE_ESGOTO.get(),
                ModEntidades.ENXAME_DE_MOSCAS.get(), ModEntidades.SLIME.get(), ModEntidades.MIMICO.get(), ModEntidades.GOO.get());
        for (int i = 0; i < tipos.size(); i++) {
            MobDoShattered inimigo = tipos.get(i).create(jogador.serverLevel());
            if (inimigo == null) {
                continue;
            }
            // Em fila de lado, 4 blocos na frente (olhando pro sul); em volta da escada sempre tem chão livre
            inimigo.moveTo(entrada.getX() + 0.5 + (i - 4) * 1.25, entrada.getY(), entrada.getZ() + 4.0, 180.0F, 0.0F);
            jogador.serverLevel().addFreshEntity(inimigo);
            inimigo.dormir();
        }
        PixelDungeon.LOGGER.info("Herói das fotos olhando os inimigos");
    }

    // O herói vira um Clérigo do nível 12 com o tomo e talentos dos dois tiers
    // (pra foto da tela de talentos e da tela do tomo com todas as magias)
    public static void mostrarTalentos(ServerPlayer jogador) {
        DadosDoHeroi dados = RegrasDoHeroi.dados(jogador);
        dados.comecar(ClasseDoHeroi.CLERIGO, DadosDaRun.pegar(jogador.getServer()).numeroDaRun());
        dados.definirNivel(12);
        for (Talento talento : List.of(Talento.REFEICAO_ILUMINADA, Talento.INTUICAO_DIVINA, Talento.ESCUDO_DE_LUZ, Talento.ESCUDO_DE_LUZ,
                Talento.RECUPERAR_INSCRICAO, Talento.RAIO_SOLAR, Talento.RAIO_SOLAR, Talento.SENTIDO_DIVINO, Talento.BENCAO)) {
            Talentos.aprimorar(jogador, talento);
        }
        if (TomoSagrado.acharTomo(jogador) == null) {
            jogador.getInventory().add(new ItemStack(ModItens.TOMO_SAGRADO.get()));
        }
        RedeDoHeroi.enviarEstado(jogador, false);
        PixelDungeon.LOGGER.info("Herói das fotos virou Clérigo do nível {} com {} pontos livres", dados.nivel(), dados.pontosLivres());
    }

    // Três armadilhas à vista lado a lado (uma já desarmada) e uma ovelha mágica, com o herói olhando de perto
    // (no primeiro andar de 2 a 4 com um pedaço de chão de 3x3 quadradinhos; a grama em cima sai)
    public static void mostrarArmadilhas(ServerPlayer jogador) {
        DadosDaRun run = DadosDaRun.pegar(jogador.getServer());
        ServerLevel mundo = jogador.serverLevel();
        int t = Andares.TAMANHO_DO_QUADRADINHO;
        for (int andar : new int[] { 2, 3, 4, 1 }) {
            PlantaDoAndar planta = GeradorDeAndar.gerar(run.semente(), andar);
            BlockPos origem = Andares.origemDoAndar(run.numeroDaRun(), andar);
            for (int y = 1; y < planta.altura() - 2; y++) {
                for (int x = 1; x < planta.largura() - 1; x++) {
                    boolean cabe = true;
                    for (int dx = -1; dx <= 1; dx++) {
                        for (int dy = 0; dy <= 2; dy++) {
                            cabe &= planta.tipo(x + dx, y + dy) == PlantaDoAndar.CHAO && !planta.temArmadilha(x + dx, y + dy)
                                    && !planta.temPremioEm(x + dx, y + dy) && planta.salaEm(x + dx, y + dy) != null;
                        }
                    }
                    if (!cabe) {
                        continue;
                    }
                    for (int dx = -1; dx <= 1; dx++) {
                        for (int dy = 0; dy <= 2; dy++) {
                            BlockPos canto = Armadilhas.controle(origem, x + dx, y + dy);
                            for (BlockPos bloco : BlockPos.betweenClosed(canto.offset(0, 1, 0), canto.offset(t - 1, 3, t - 1))) {
                                if (!mundo.getBlockState(bloco).isAir() && mundo.getBlockState(bloco).getCollisionShape(mundo, bloco).isEmpty()) {
                                    mundo.removeBlock(bloco, false);
                                }
                            }
                        }
                    }
                    Armadilhas.colocar(mundo, Armadilhas.controle(origem, x - 1, y), TipoDeArmadilha.GAS_TOXICO, false);
                    Armadilhas.colocar(mundo, Armadilhas.controle(origem, x, y), TipoDeArmadilha.PORTAL, false);
                    BlockPos desarmada = Armadilhas.controle(origem, x + 1, y);
                    Armadilhas.colocar(mundo, desarmada, TipoDeArmadilha.AGARRADORA, false);
                    Armadilhas.desarmar(mundo, desarmada);
                    OvelhaMagica.criar(mundo, Armadilhas.centro(Armadilhas.controle(origem, x + 1, y + 1)), 30);
                    Vec3 lugar = Armadilhas.centro(Armadilhas.controle(origem, x, y + 2));
                    jogador.teleportTo(mundo, lugar.x, lugar.y, lugar.z, 180.0F, 25.0F);
                    PixelDungeon.LOGGER.info("Herói das fotos olhando as armadilhas do andar {} em {}, {}", andar, x, y);
                    return;
                }
            }
        }
        PixelDungeon.LOGGER.info("Herói das fotos: nenhum andar tem lugar pras armadilhas");
    }

    // Leva o herói pra olhar uma porta do andar 2 (comum ou trancada) de 2 quadradinhos de distância.
    // (As do andar 1 já foram mexidas pelo teste do servidor.)
    public static void olharPraPorta(ServerPlayer jogador, boolean trancada) {
        DadosDaRun run = DadosDaRun.pegar(jogador.getServer());
        PlantaDoAndar planta = GeradorDeAndar.gerar(run.semente(), 2);
        BlockPos origem = Andares.origemDoAndar(run.numeroDaRun(), 2);
        byte procurado = trancada ? PlantaDoAndar.PORTA_TRANCADA : PlantaDoAndar.PORTA;
        int t = Andares.TAMANHO_DO_QUADRADINHO;
        for (int y = 0; y < planta.altura(); y++) {
            for (int x = 0; x < planta.largura(); x++) {
                if (planta.tipo(x, y) != procurado) {
                    continue;
                }
                // Um lado aberto, 2 quadradinhos longe da porta, sem grama alta no meio
                int[][] lados = { { 1, 0 }, { -1, 0 }, { 0, 1 }, { 0, -1 } };
                for (int[] lado : lados) {
                    int qx = x + lado[0] * 2;
                    int qy = y + lado[1] * 2;
                    if (!planta.passavel(x + lado[0], y + lado[1]) || !planta.passavel(qx, qy)
                            || planta.grama(x + lado[0], y + lado[1]) == PlantaDoAndar.GRAMA_ALTA || planta.grama(qx, qy) == PlantaDoAndar.GRAMA_ALTA) {
                        continue;
                    }
                    double px = origem.getX() + qx * t + t / 2.0;
                    double pz = origem.getZ() + qy * t + t / 2.0;
                    double ax = origem.getX() + x * t + t / 2.0;
                    double az = origem.getZ() + y * t + t / 2.0;
                    float giro = (float) (Math.toDegrees(Math.atan2(az - pz, ax - px)) - 90.0);
                    jogador.teleportTo(jogador.serverLevel(), px, origem.getY() + 1, pz, giro, -8.0F);
                    PixelDungeon.LOGGER.info("Herói das fotos olhando a porta {} em {}, {}", trancada ? "trancada" : "comum", x, y);
                    return;
                }
            }
        }
        PixelDungeon.LOGGER.info("Herói das fotos: o andar 2 não tem porta {}", trancada ? "trancada" : "comum");
    }
}
