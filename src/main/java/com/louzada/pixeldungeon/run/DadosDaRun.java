package com.louzada.pixeldungeon.run;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

// Tudo sobre a run atual que precisa continuar salvo quando o mundo fecha:
// número da run, semente, andares já construídos e em qual run cada jogador está.
public class DadosDaRun extends SavedData {
    private static final String NOME_DO_ARQUIVO = "pixeldungeon_run";

    // Onde o jogador aparece ao chegar num andar: vindo de cima (entrada) ou de baixo (saída)
    public record InfoAndar(BlockPos entrada, @Nullable BlockPos saida) {
    }

    private int numeroDaRun = 0;
    private long semente = 0;
    private Map<Integer, InfoAndar> andares = new HashMap<>();
    private Map<UUID, Integer> runDoJogador = new HashMap<>();

    // Pega os dados salvos no mundo (ou cria do zero se ainda não existem)
    public static DadosDaRun pegar(MinecraftServer servidor) {
        return servidor.overworld().getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(DadosDaRun::new, DadosDaRun::carregar), NOME_DO_ARQUIVO);
    }

    public int numeroDaRun() {
        return numeroDaRun;
    }

    public long semente() {
        return semente;
    }

    public void comecarNovaRun(long novaSemente) {
        numeroDaRun++;
        semente = novaSemente;
        andares.clear();
        runDoJogador.clear();
        setDirty();
    }

    @Nullable
    public InfoAndar andar(int andar) {
        return andares.get(andar);
    }

    public void guardarAndar(int andar, InfoAndar info) {
        andares.put(andar, info);
        setDirty();
    }

    public boolean estaNaRunAtual(UUID jogador) {
        Integer run = runDoJogador.get(jogador);
        return run != null && run == numeroDaRun;
    }

    public void registrarJogador(UUID jogador) {
        runDoJogador.put(jogador, numeroDaRun);
        setDirty();
    }

    // ---- Salvar e carregar ----

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registros) {
        tag.putInt("numero", numeroDaRun);
        tag.putLong("semente", semente);

        ListTag listaDeAndares = new ListTag();
        for (Map.Entry<Integer, InfoAndar> item : andares.entrySet()) {
            CompoundTag andarTag = new CompoundTag();
            andarTag.putInt("andar", item.getKey());
            salvarPosicao(andarTag, "entrada", item.getValue().entrada());
            if (item.getValue().saida() != null) {
                salvarPosicao(andarTag, "saida", item.getValue().saida());
            }
            listaDeAndares.add(andarTag);
        }
        tag.put("andares", listaDeAndares);

        ListTag listaDeJogadores = new ListTag();
        for (Map.Entry<UUID, Integer> item : runDoJogador.entrySet()) {
            CompoundTag jogadorTag = new CompoundTag();
            jogadorTag.putUUID("id", item.getKey());
            jogadorTag.putInt("run", item.getValue());
            listaDeJogadores.add(jogadorTag);
        }
        tag.put("jogadores", listaDeJogadores);
        return tag;
    }

    public static DadosDaRun carregar(CompoundTag tag, HolderLookup.Provider registros) {
        DadosDaRun dados = new DadosDaRun();
        dados.numeroDaRun = tag.getInt("numero");
        dados.semente = tag.getLong("semente");

        ListTag listaDeAndares = tag.getList("andares", Tag.TAG_COMPOUND);
        for (int i = 0; i < listaDeAndares.size(); i++) {
            CompoundTag andarTag = listaDeAndares.getCompound(i);
            BlockPos entrada = carregarPosicao(andarTag, "entrada");
            BlockPos saida = andarTag.contains("saida_x") ? carregarPosicao(andarTag, "saida") : null;
            dados.andares.put(andarTag.getInt("andar"), new InfoAndar(entrada, saida));
        }

        ListTag listaDeJogadores = tag.getList("jogadores", Tag.TAG_COMPOUND);
        for (int i = 0; i < listaDeJogadores.size(); i++) {
            CompoundTag jogadorTag = listaDeJogadores.getCompound(i);
            dados.runDoJogador.put(jogadorTag.getUUID("id"), jogadorTag.getInt("run"));
        }
        return dados;
    }

    private static void salvarPosicao(CompoundTag tag, String nome, BlockPos posicao) {
        tag.putInt(nome + "_x", posicao.getX());
        tag.putInt(nome + "_y", posicao.getY());
        tag.putInt(nome + "_z", posicao.getZ());
    }

    private static BlockPos carregarPosicao(CompoundTag tag, String nome) {
        return new BlockPos(tag.getInt(nome + "_x"), tag.getInt(nome + "_y"), tag.getInt(nome + "_z"));
    }
}
