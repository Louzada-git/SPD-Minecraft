package com.louzada.pixeldungeon.item;

import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;

// Ajuda com textos longos das dicas (tooltips), que no Minecraft não quebram linha sozinhos.
public class Textos {
    private static final int LARGURA_DA_LINHA = 40;

    // Pega o texto traduzido e quebra em várias linhas de até 40 letras.
    // Os números vão no lugar dos %s do texto; os "_" de destaque (das telas) somem.
    public static void adicionarQuebrado(List<Component> linhas, String chave, ChatFormatting cor, Object... numeros) {
        String texto = Language.getInstance().getOrDefault(chave);
        if (numeros.length > 0) {
            texto = String.format(texto, numeros);
        }
        texto = texto.replace("_", "");
        // Cada parágrafo (separado por \n) é quebrado sozinho
        for (String paragrafo : texto.split("\n")) {
            if (paragrafo.isBlank()) {
                linhas.add(Component.empty());
                continue;
            }
            StringBuilder linha = new StringBuilder();
            for (String palavra : paragrafo.split(" ")) {
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
}
