package com.louzada.pixeldungeon.heroi;

import javax.annotation.Nullable;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.neoforged.neoforge.attachment.IAttachmentHolder;
import net.neoforged.neoforge.attachment.IAttachmentSerializer;

// Os dados do herói de cada jogador, salvos junto com o jogador.
// Valem só para a run de número "run"; numa run nova tudo começa do zero.
public class DadosDoHeroi {
    public static final int FORCA_INICIAL = 10;

    private String classe = "";
    private int forca = FORCA_INICIAL;
    private int nivel = 1;
    private int xp = 0;
    private boolean morto = false;
    private int run = 0;

    // Zera tudo e começa com a classe escolhida
    public void comecar(ClasseDoHeroi novaClasse, int numeroDaRun) {
        classe = novaClasse.id();
        forca = FORCA_INICIAL;
        nivel = 1;
        xp = 0;
        morto = false;
        run = numeroDaRun;
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
            return tag;
        }
    }
}
