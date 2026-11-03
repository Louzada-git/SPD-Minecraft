package com.louzada.pixeldungeon.item;

import com.louzada.pixeldungeon.registro.ModComponentes;

import net.minecraft.world.item.ItemStack;

// Cargas guardadas dentro de um item (cajado do Mago, capa das sombras, tomo sagrado).
// As cargas voltam sozinhas com o tempo. Pra não ficar mexendo no item toda hora, ele só guarda
// quando a próxima carga começou a encher; as que já encheram são contadas na hora de usar.
public class Cargas {

    // Quantos ticks leva pra encher uma carga, sabendo quantas estão faltando
    public interface TempoDeRecarga {
        long ticks(int faltando);
    }

    public static int cargas(ItemStack pilha, int maximo) {
        return Math.min(maximo, pilha.getOrDefault(ModComponentes.CARGAS.get(), maximo));
    }

    // Conta as cargas que encheram desde a última vez
    public static void recarregar(ItemStack pilha, long agora, int maximo, TempoDeRecarga tempo) {
        if (pilha.has(ModComponentes.RECARGA_PAUSADA.get())) {
            return;
        }
        int cargas = cargas(pilha, maximo);
        if (cargas >= maximo) {
            pilha.remove(ModComponentes.INICIO_DA_RECARGA.get());
            return;
        }
        Long inicio = pilha.get(ModComponentes.INICIO_DA_RECARGA.get());
        if (inicio == null || inicio > agora) {
            pilha.set(ModComponentes.INICIO_DA_RECARGA.get(), agora);
            return;
        }
        long comeco = inicio;
        int novas = cargas;
        while (novas < maximo && agora - comeco >= tempo.ticks(maximo - novas)) {
            comeco += tempo.ticks(maximo - novas);
            novas++;
        }
        if (novas != cargas) {
            pilha.set(ModComponentes.CARGAS.get(), novas);
            if (novas >= maximo) {
                pilha.remove(ModComponentes.INICIO_DA_RECARGA.get());
            } else {
                pilha.set(ModComponentes.INICIO_DA_RECARGA.get(), comeco);
            }
        }
    }

    // Gasta cargas (se estava cheio, a recarga começa agora)
    public static void gastar(ItemStack pilha, long agora, int maximo, int quantas) {
        int cargas = cargas(pilha, maximo);
        if (cargas >= maximo && !pilha.has(ModComponentes.RECARGA_PAUSADA.get())) {
            pilha.set(ModComponentes.INICIO_DA_RECARGA.get(), agora);
        }
        pilha.set(ModComponentes.CARGAS.get(), Math.max(0, cargas - quantas));
    }

    // Ganha cargas na hora (ração de suprimentos na capa, por exemplo)
    public static void somar(ItemStack pilha, int maximo, int quantas) {
        int novas = Math.min(maximo, cargas(pilha, maximo) + quantas);
        pilha.set(ModComponentes.CARGAS.get(), novas);
        if (novas >= maximo) {
            pilha.remove(ModComponentes.INICIO_DA_RECARGA.get());
        }
    }

    // Para de recarregar, guardando o quanto a próxima carga já tinha enchido
    public static void pausar(ItemStack pilha, long agora) {
        Long inicio = pilha.get(ModComponentes.INICIO_DA_RECARGA.get());
        pilha.set(ModComponentes.RECARGA_PAUSADA.get(), inicio == null ? 0L : Math.max(0L, agora - inicio));
        pilha.remove(ModComponentes.INICIO_DA_RECARGA.get());
    }

    // Volta a recarregar de onde parou
    public static void retomar(ItemStack pilha, long agora) {
        Long progresso = pilha.get(ModComponentes.RECARGA_PAUSADA.get());
        if (progresso == null) {
            return;
        }
        pilha.remove(ModComponentes.RECARGA_PAUSADA.get());
        pilha.set(ModComponentes.INICIO_DA_RECARGA.get(), agora - progresso);
    }

    // Barrinha embaixo do ícone (13 pixels = cheio)
    public static int larguraDaBarra(ItemStack pilha, int maximo) {
        return Math.round(13.0F * cargas(pilha, maximo) / Math.max(1, maximo));
    }
}
