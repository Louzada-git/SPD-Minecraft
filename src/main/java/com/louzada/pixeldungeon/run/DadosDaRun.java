package com.louzada.pixeldungeon.run;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

// Tudo sobre a run atual que precisa continuar salvo quando o mundo fecha:
// número da run, se ela está acontecendo, semente, andares já construídos, chefes já derrotados
// e quantas vezes caíram os itens que são limitados por run (as armas do slime).
// (Em qual run cada jogador está fica salvo no próprio jogador, no DadosDoHeroi.)
public class DadosDaRun extends SavedData {
    private static final String NOME_DO_ARQUIVO = "pixeldungeon_run";

    // Onde o jogador aparece ao chegar num andar: vindo de cima (entrada) ou de baixo (saída)
    public record InfoAndar(BlockPos entrada, @Nullable BlockPos saida) {
    }

    private int numeroDaRun = 0;
    // A run começa quando alguém escolhe a classe e acaba quando todos morrem
    private boolean ativa = false;
    private long semente = 0;
    private Map<Integer, InfoAndar> andares = new HashMap<>();
    // Andares de chefe em que o chefe já morreu (a saída deles fica aberta)
    private Set<Integer> chefesDerrotados = new HashSet<>();
    // Igual aos "LimitedDrops" do Shattered: quantas vezes cada item limitado já caiu nesta run
    private Map<String, Integer> quedas = new HashMap<>();

    // Pega os dados salvos no mundo (ou cria do zero se ainda não existem)
    public static DadosDaRun pegar(MinecraftServer servidor) {
        return servidor.overworld().getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(DadosDaRun::new, DadosDaRun::carregar), NOME_DO_ARQUIVO);
    }

    public int numeroDaRun() {
        return numeroDaRun;
    }

    public boolean ativa() {
        return ativa;
    }

    public long semente() {
        return semente;
    }

    public void comecarNovaRun(long novaSemente) {
        numeroDaRun++;
        ativa = true;
        semente = novaSemente;
        andares.clear();
        chefesDerrotados.clear();
        quedas.clear();
        setDirty();
    }

    public void terminarRun() {
        ativa = false;
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

    public boolean chefeDerrotado(int andar) {
        return chefesDerrotados.contains(andar);
    }

    public void marcarChefeDerrotado(int andar) {
        chefesDerrotados.add(andar);
        setDirty();
    }

    public int quedas(String item) {
        return quedas.getOrDefault(item, 0);
    }

    public void contarQueda(String item) {
        quedas.put(item, quedas(item) + 1);
        setDirty();
    }

    // ---- Salvar e carregar ----

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registros) {
        tag.putInt("numero", numeroDaRun);
        tag.putBoolean("ativa", ativa);
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
        tag.putIntArray("chefes_derrotados", chefesDerrotados.stream().mapToInt(Integer::intValue).toArray());
        CompoundTag quedasTag = new CompoundTag();
        for (Map.Entry<String, Integer> item : quedas.entrySet()) {
            quedasTag.putInt(item.getKey(), item.getValue());
        }
        tag.put("quedas", quedasTag);
        return tag;
    }

    public static DadosDaRun carregar(CompoundTag tag, HolderLookup.Provider registros) {
        DadosDaRun dados = new DadosDaRun();
        dados.numeroDaRun = tag.getInt("numero");
        dados.ativa = tag.getBoolean("ativa");
        dados.semente = tag.getLong("semente");

        ListTag listaDeAndares = tag.getList("andares", Tag.TAG_COMPOUND);
        for (int i = 0; i < listaDeAndares.size(); i++) {
            CompoundTag andarTag = listaDeAndares.getCompound(i);
            BlockPos entrada = carregarPosicao(andarTag, "entrada");
            BlockPos saida = andarTag.contains("saida_x") ? carregarPosicao(andarTag, "saida") : null;
            dados.andares.put(andarTag.getInt("andar"), new InfoAndar(entrada, saida));
        }
        for (int andar : tag.getIntArray("chefes_derrotados")) {
            dados.chefesDerrotados.add(andar);
        }
        CompoundTag quedasTag = tag.getCompound("quedas");
        for (String item : quedasTag.getAllKeys()) {
            dados.quedas.put(item, quedasTag.getInt(item));
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
