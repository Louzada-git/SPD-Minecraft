package com.louzada.pixeldungeon.item;

import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;

// Ajuda com textos longos das dicas (tooltips), que no Minecraft não quebram linha sozinhos.
public class Textos {
    private static final int LARGURA_DA_LINHA = 40;

    // Pega o texto traduzido e quebra em várias linhas de até 40 letras
    public static void adicionarQuebrado(List<Component> linhas, String chave, ChatFormatting cor) {
        String texto = Language.getInstance().getOrDefault(chave);
        StringBuilder linha = new StringBuilder();
        for (String palavra : texto.split(" ")) {
            if (linha.length() > 0 && linha.length() + 1 + palavra.length() > LARGURA_DA_LINHA) {
                linhas.add(Component.literal(linha.toString()).withStyle(cor));
                linha.setLength(0);
            }
            if (linha.length() > 0) {
                linha.append(' ');
            }
            linha.append(palavra);
        }
        if (linha.length() > 0) {
            linhas.add(Component.literal(linha.toString()).withStyle(cor));
        }
    }
}
