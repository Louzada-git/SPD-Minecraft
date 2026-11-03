package com.louzada.pixeldungeon.heroi;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import javax.annotation.Nullable;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.neoforged.neoforge.attachment.IAttachmentHolder;
import net.neoforged.neoforge.attachment.IAttachmentSerializer;

// Os dados do herói de cada jogador, salvos junto com o jogador.
// Valem só para a run de número "run"; numa run nova tudo começa do zero.
//
// Além da classe, nível etc., guarda:
//   - os pontos em cada talento
//   - o "estado": efeitos que duram um tempo (os buffs do Shattered), como a raiva provocada,
//     o escudo do selo, as cargas das habilidades da Duelista... Cada um tem um nome (a chave).
public class DadosDoHeroi {
    public static final int FORCA_INICIAL = 10;

    private String classe = "";
    private int forca = FORCA_INICIAL;
    private int nivel = 1;
    private int xp = 0;
    private boolean morto = false;
    private int run = 0;
    private Map<String, Integer> talentos = new HashMap<>();
    private CompoundTag estado = new CompoundTag();

    // Zera tudo e começa com a classe escolhida
    public void comecar(ClasseDoHeroi novaClasse, int numeroDaRun) {
        classe = novaClasse.id();
        forca = FORCA_INICIAL;
        nivel = 1;
        xp = 0;
        morto = false;
        run = numeroDaRun;
        talentos.clear();
        estado = new CompoundTag();
    }

    @Nullable
    public ClasseDoHeroi classe() {
        return ClasseDoHeroi.peloId(classe);
    }

    public int forca() {
        return forca;
    }

    public void definirForca(int novaForca) {
        forca = novaForca;
    }

    public int nivel() {
        return nivel;
    }

    public void definirNivel(int novoNivel) {
        nivel = novoNivel;
    }

    public int xp() {
        return xp;
    }

    public void definirXp(int novoXp) {
        xp = novoXp;
    }

    public boolean morto() {
        return morto;
    }

    public void definirMorto(boolean estaMorto) {
        morto = estaMorto;
    }

    public int run() {
        return run;
    }

    // ---- Talentos ----

    public int pontos(Talento talento) {
        return talentos.getOrDefault(talento.id(), 0);
    }

    public void definirPontos(Talento talento, int pontos) {
        talentos.put(talento.id(), pontos);
    }

    public int pontosGastos() {
        int total = 0;
        for (int pontos : talentos.values()) {
            total += pontos;
        }
        return total;
    }

    public int pontosLivres() {
        return Math.max(0, Talento.pontosGanhos(nivel) - pontosGastos());
    }

    // ---- Estado (efeitos que duram um tempo) ----

    public int inteiro(String chave) {
        return estado.getInt(chave);
    }

    public void definir(String chave, int valor) {
        estado.putInt(chave, valor);
    }

    public float decimal(String chave) {
        return estado.getFloat(chave);
    }

    public void definir(String chave, float valor) {
        estado.putFloat(chave, valor);
    }

    public boolean sim(String chave) {
        return estado.getBoolean(chave);
    }

    public void definir(String chave, boolean valor) {
        estado.putBoolean(chave, valor);
    }

    @Nullable
    public UUID alvo(String chave) {
        return estado.hasUUID(chave) ? estado.getUUID(chave) : null;
    }

    public void definirAlvo(String chave, UUID alvo) {
        estado.putUUID(chave, alvo);
    }

    // Efeito com tempo: fica ativo por "ticks" a partir de agora (20 ticks = 1 turno)
    public void ativarPor(String chave, long agora, int ticks) {
        estado.putLong(chave, agora + ticks);
    }

    public boolean ativo(String chave, long agora) {
        return estado.getLong(chave) > agora;
    }

    public void desativar(String chave) {
        estado.remove(chave);
    }

    // ---- Salvar e carregar ----

    public static class Serializador implements IAttachmentSerializer<CompoundTag, DadosDoHeroi> {
        @Override
        public DadosDoHeroi read(IAttachmentHolder dono, CompoundTag tag, HolderLookup.Provider registros) {
            DadosDoHeroi dados = new DadosDoHeroi();
            dados.classe = tag.getString("classe");
            dados.forca = tag.contains("forca") ? tag.getInt("forca") : FORCA_INICIAL;
            dados.nivel = Math.max(1, tag.getInt("nivel"));
            dados.xp = tag.getInt("xp");
            dados.morto = tag.getBoolean("morto");
            dados.run = tag.getInt("run");
            CompoundTag talentos = tag.getCompound("talentos");
            for (String id : talentos.getAllKeys()) {
                dados.talentos.put(id, talentos.getInt(id));
            }
            dados.estado = tag.getCompound("estado");
            return dados;
        }

        @Override
        public CompoundTag write(DadosDoHeroi dados, HolderLookup.Provider registros) {
            CompoundTag tag = new CompoundTag();
            tag.putString("classe", dados.classe);
            tag.putInt("forca", dados.forca);
            tag.putInt("nivel", dados.nivel);
            tag.putInt("xp", dados.xp);
            tag.putBoolean("morto", dados.morto);
            tag.putInt("run", dados.run);
            CompoundTag talentos = new CompoundTag();
            for (Map.Entry<String, Integer> talento : dados.talentos.entrySet()) {
                talentos.putInt(talento.getKey(), talento.getValue());
            }
            tag.put("talentos", talentos);
            tag.put("estado", dados.estado.copy());
            return tag;
        }
    }
}
