package com.louzada.pixeldungeon.cliente;

import net.minecraft.ChatFormatting;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

// Os textos do Shattered marcam palavras importantes com _assim_: elas ficam amarelas
public class TextoDoShattered {

    public static Component destacado(String chave) {
        String texto = Language.getInstance().getOrDefault(chave);
        MutableComponent resultado = Component.empty();
        String[] partes = texto.split("_", -1);
        for (int i = 0; i < partes.length; i++) {
            MutableComponent parte = Component.literal(partes[i]);
            if (i % 2 == 1) {
                parte = parte.withStyle(ChatFormatting.YELLOW);
            }
            resultado.append(parte);
        }
        return resultado;
    }
}
