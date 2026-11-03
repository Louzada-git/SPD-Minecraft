package com.louzada.pixeldungeon.item;

import java.util.ArrayList;
import java.util.List;

import com.louzada.pixeldungeon.Turno;
import com.louzada.pixeldungeon.combate.Particulas;
import com.louzada.pixeldungeon.dungeon.Andares;
import com.louzada.pixeldungeon.dungeon.MapeamentoMagico;
import com.louzada.pixeldungeon.dungeon.PopulacaoDoAndar;
import com.louzada.pixeldungeon.dungeon.Teleporte;
import com.louzada.pixeldungeon.efeito.EfeitosNoTick;
import com.louzada.pixeldungeon.entidade.ImagemEspelhada;
import com.louzada.pixeldungeon.entidade.MobDoShattered;
import com.louzada.pixeldungeon.registro.ModEfeitos;
import com.louzada.pixeldungeon.registro.ModItens;
import com.louzada.pixeldungeon.run.DadosDaRun;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;

// O que cada pergaminho do Shattered faz, com os números de lá (1 turno = 2 segundos).
// "Visíveis" são os inimigos que o leitor consegue ver (até 32 blocos, sem parede no meio).
public class EfeitosDePergaminho {
    private static final double ALCANCE_DA_VISAO = 32;
    private static final int IMAGENS = 2;

    // ---- Os que não pedem item ----

    public static void ler(ServerPlayer jogador, TipoDePergaminho tipo) {
        ServerLevel mundo = jogador.serverLevel();
        switch (tipo) {
            case IMAGEM_ESPELHADA -> {
                int criadas = 0;
                for (int i = 0; i < IMAGENS; i++) {
                    if (ImagemEspelhada.criar(mundo, jogador) != null) {
                        criadas++;
                    }
                }
                avisar(jogador, criadas > 0 ? "efeito.pixeldungeon.imagem_espelhada" : "efeito.pixeldungeon.imagem_espelhada.nenhuma");
            }
            case RECARGA -> {
                // Enche o cajado na hora (no Shattered, as varinhas recarregam rápido por 30 turnos)
                for (ItemStack pilha : todosOsItens(jogador)) {
                    if (pilha.getItem() instanceof CajadoDoMago) {
                        CajadoDoMago.encher(pilha);
                    }
                }
                Particulas.subindoEmVolta(jogador, ParticleTypes.ELECTRIC_SPARK, 20, 0.15);
                mundo.playSound(null, jogador.blockPosition(), SoundEvents.BEACON_POWER_SELECT, SoundSource.PLAYERS, 1.0F, 1.4F);
                avisar(jogador, "efeito.pixeldungeon.recarga");
            }
            case TELETRANSPORTE -> {
                if (!Teleporte.paraLugarAleatorio(mundo, jogador)) {
                    avisar(jogador, "efeito.pixeldungeon.teletransporte.falhou");
                }
            }
            case CANCAO_DE_NINAR -> {
                // Os inimigos visíveis (e o próprio leitor) ficam sonolentos e dormem depois de 5 turnos
                for (MobDoShattered inimigo : visiveis(jogador)) {
                    EfeitosNoTick.deixarSonolento(inimigo);
                    Particulas.subindoEmVolta(inimigo, ParticleTypes.NOTE, 4, 0.0);
                }
                EfeitosNoTick.deixarSonolento(jogador);
                Particulas.subindoEmVolta(jogador, ParticleTypes.NOTE, 6, 0.0);
                mundo.playSound(null, jogador.blockPosition(), SoundEvents.NOTE_BLOCK_HARP.value(), SoundSource.PLAYERS, 1.0F, 0.8F);
                avisar(jogador, "efeito.pixeldungeon.cancao_de_ninar");
            }
            case MAPEAMENTO_MAGICO -> MapeamentoMagico.revelar(jogador);
            case FURIA -> {
                // Todos os inimigos do andar vêm atrás de quem leu; os visíveis ficam furiosos (atacam qualquer um) por 5 turnos
                for (MobDoShattered inimigo : inimigosDoAndar(jogador)) {
                    inimigo.notar(jogador);
                    inimigo.getNavigation().moveTo(jogador, 1.0);
                }
                for (MobDoShattered inimigo : visiveis(jogador)) {
                    inimigo.addEffect(new MobEffectInstance(ModEfeitos.FURIOSO, 5 * Turno.TICKS));
                }
                mundo.playSound(null, jogador.blockPosition(), SoundEvents.RAVAGER_ROAR, SoundSource.PLAYERS, 1.0F, 1.2F);
                avisar(jogador, "efeito.pixeldungeon.furia");
            }
            case RETRIBUICAO -> {
                // Quanto menos vida o leitor tem, mais forte: dano = vida máxima / 10 + vida × força × 0,225
                float faltando = (jogador.getMaxHealth() - jogador.getHealth()) / jogador.getMaxHealth();
                float forca = Math.min(4F, 4.45F * faltando);
                for (MobDoShattered inimigo : visiveis(jogador)) {
                    float dano = Math.round(inimigo.getMaxHealth() / 10F + inimigo.getHealth() * forca * 0.225F);
                    inimigo.invulnerableTime = 0;
                    inimigo.hurt(jogador.damageSources().indirectMagic(jogador, jogador), dano);
                    if (inimigo.isAlive()) {
                        EfeitosNoTick.cegar(inimigo, 10 * Turno.TICKS);
                    }
                }
                EfeitosNoTick.cegar(jogador, 10 * Turno.TICKS);
                Particulas.explosao(mundo, ParticleTypes.FLASH, jogador.getX(), jogador.getY(1.0), jogador.getZ(), 1, 0, 0);
                mundo.playSound(null, jogador.blockPosition(), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 1.0F, 1.4F);
                avisar(jogador, "efeito.pixeldungeon.retribuicao");
            }
            case TERROR -> {
                List<MobDoShattered> afetados = visiveis(jogador);
                for (MobDoShattered inimigo : afetados) {
                    inimigo.acordar();
                    inimigo.addEffect(new MobEffectInstance(ModEfeitos.ATERRORIZADO, 20 * Turno.TICKS));
                    inimigo.setTarget(null);
                }
                Particulas.subindoEmVolta(jogador, Particulas.poeira(0xFF0000, 1.5F), 20, 0.1);
                mundo.playSound(null, jogador.blockPosition(), SoundEvents.WARDEN_ROAR, SoundSource.PLAYERS, 0.6F, 1.6F);
                avisar(jogador, afetados.isEmpty() ? "efeito.pixeldungeon.terror.nenhum" : "efeito.pixeldungeon.terror");
            }
            default -> {
            }
        }
    }

    // ---- Os que pedem item (o jogador já escolheu) ----

    public static void aplicarNoItem(ServerPlayer jogador, TipoDePergaminho tipo, int espaco) {
        ItemStack pilha = jogador.getInventory().getItem(espaco);
        ServerLevel mundo = jogador.serverLevel();
        switch (tipo) {
            case MELHORIA -> {
                NivelDoItem.melhorar(jogador, pilha, jogador.getRandom());
                Particulas.subindoEmVolta(jogador, ParticleTypes.ENCHANT, 20, 0.2);
                mundo.playSound(null, jogador.blockPosition(), SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 1.0F, 1.0F);
                jogador.displayClientMessage(Component.translatable("efeito.pixeldungeon.melhoria", pilha.getHoverName()).withStyle(ChatFormatting.AQUA), false);
            }
            case IDENTIFICACAO -> {
                Identificacao.identificarItem(jogador, pilha);
                Particulas.subindoEmVolta(jogador, ParticleTypes.END_ROD, 8, 0.05);
            }
            case REMOVER_MALDICAO -> {
                boolean tinha = NivelDoItem.removerMaldicao(pilha);
                Particulas.subindoEmVolta(jogador, ParticleTypes.END_ROD, 12, 0.1);
                mundo.playSound(null, jogador.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.0F, 1.0F);
                jogador.displayClientMessage(Component.translatable(tinha ? "efeito.pixeldungeon.remover_maldicao" : "efeito.pixeldungeon.sem_maldicao",
                        pilha.getHoverName()).withStyle(ChatFormatting.AQUA), false);
            }
            case TRANSMUTACAO -> {
                ItemStack novo = transmutar(jogador, pilha);
                if (!novo.isEmpty()) {
                    Component antes = pilha.getHoverName();
                    jogador.getInventory().setItem(espaco, novo);
                    Particulas.subindoEmVolta(jogador, ParticleTypes.PORTAL, 16, 0.2);
                    mundo.playSound(null, jogador.blockPosition(), SoundEvents.ILLUSIONER_CAST_SPELL, SoundSource.PLAYERS, 1.0F, 1.0F);
                    jogador.displayClientMessage(Component.translatable("efeito.pixeldungeon.transmutacao", antes, novo.getHoverName())
                            .withStyle(ChatFormatting.AQUA), false);
                }
            }
            default -> {
            }
        }
    }

    // ---- Transmutação ----

    // Armas viram outra arma do mesmo tier (com o mesmo nível e maldição), sementes viram outra semente,
    // poções e pergaminhos viram outro tipo. Força e melhoria não podem ser transmutadas.
    // (No Shattered, poções e pergaminhos viram as versões "exóticas", que o mod ainda não tem.)
    public static boolean podeTransmutar(ServerPlayer jogador, ItemStack pilha) {
        long semente = Identificacao.sementeDaRun(jogador);
        if (pilha.getItem() instanceof ArmaDoShattered && !(pilha.getItem() instanceof CajadoDoMago)) {
            return true;
        }
        if (pilha.getItem() instanceof ItemSemente) {
            return true;
        }
        TipoDePocao pocao = Identificacao.tipoDaPocao(semente, pilha);
        if (pocao != null) {
            return pocao != TipoDePocao.FORCA;
        }
        TipoDePergaminho pergaminho = Identificacao.tipoDoPergaminho(semente, pilha);
        return pergaminho != null && pergaminho != TipoDePergaminho.MELHORIA;
    }

    private static ItemStack transmutar(ServerPlayer jogador, ItemStack pilha) {
        RandomSource sorteio = jogador.getRandom();
        long semente = Identificacao.sementeDaRun(jogador);
        List<Item> opcoes = new ArrayList<>();
        if (pilha.getItem() instanceof ArmaDoShattered arma) {
            for (var registrado : ModItens.ITENS.getEntries()) {
                if (registrado.get() instanceof ArmaDoShattered outra && outra.tier() == arma.tier() && outra != arma && !(outra instanceof CajadoDoMago)) {
                    opcoes.add(outra);
                }
            }
            // Fica com o nível, a maldição e o que já se sabe dela
            return opcoes.isEmpty() ? ItemStack.EMPTY : pilha.transmuteCopy(opcoes.get(sorteio.nextInt(opcoes.size())), pilha.getCount());
        }
        if (pilha.getItem() instanceof ItemSemente atual) {
            for (TipoDePlanta planta : TipoDePlanta.values()) {
                if (planta != atual.planta()) {
                    opcoes.add(ItemSemente.daPlanta(planta));
                }
            }
        }
        TipoDePocao pocao = Identificacao.tipoDaPocao(semente, pilha);
        if (pocao != null) {
            for (TipoDePocao outro : TipoDePocao.values()) {
                if (outro != pocao && outro != TipoDePocao.FORCA) {
                    opcoes.add(Identificacao.pocao(semente, outro).getItem());
                }
            }
        }
        TipoDePergaminho pergaminho = Identificacao.tipoDoPergaminho(semente, pilha);
        if (pergaminho != null) {
            for (TipoDePergaminho outro : TipoDePergaminho.values()) {
                if (outro != pergaminho && outro != TipoDePergaminho.MELHORIA) {
                    opcoes.add(Identificacao.pergaminho(semente, outro).getItem());
                }
            }
        }
        return opcoes.isEmpty() ? ItemStack.EMPTY : new ItemStack(opcoes.get(sorteio.nextInt(opcoes.size())), pilha.getCount());
    }

    // ---- Ajudantes ----

    // Inimigos que o leitor consegue ver
    public static List<MobDoShattered> visiveis(ServerPlayer jogador) {
        List<MobDoShattered> lista = new ArrayList<>();
        for (MobDoShattered inimigo : jogador.serverLevel().getEntitiesOfClass(MobDoShattered.class, jogador.getBoundingBox().inflate(ALCANCE_DA_VISAO))) {
            if (inimigo.isAlive() && jogador.hasLineOfSight(inimigo)) {
                lista.add(inimigo);
            }
        }
        return lista;
    }

    private static List<MobDoShattered> inimigosDoAndar(ServerPlayer jogador) {
        DadosDaRun run = DadosDaRun.pegar(jogador.serverLevel().getServer());
        Integer andar = PopulacaoDoAndar.andarEm(run, jogador.getX(), jogador.getY(), jogador.getZ());
        AABB caixa = andar == null ? jogador.getBoundingBox().inflate(64)
                : PopulacaoDoAndar.caixaDoAndar(Andares.origemDoAndar(run.numeroDaRun(), andar));
        return jogador.serverLevel().getEntitiesOfClass(MobDoShattered.class, caixa);
    }

    private static List<ItemStack> todosOsItens(ServerPlayer jogador) {
        List<ItemStack> lista = new ArrayList<>();
        for (int i = 0; i < jogador.getInventory().getContainerSize(); i++) {
            lista.add(jogador.getInventory().getItem(i));
        }
        return lista;
    }

    private static void avisar(ServerPlayer jogador, String chave) {
        jogador.displayClientMessage(Component.translatable(chave).withStyle(ChatFormatting.AQUA), true);
    }
}
