package com.louzada.pixeldungeon.registro;

import java.util.function.Supplier;

import com.louzada.pixeldungeon.PixelDungeon;
import com.louzada.pixeldungeon.heroi.DadosDoHeroi;
import com.mojang.serialization.Codec;

import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

// Dados extras que o mod "pendura" nos jogadores e nos baús (e que ficam salvos com eles).
public class ModAnexos {
    public static final DeferredRegister<AttachmentType<?>> ANEXOS = DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, PixelDungeon.MODID);

    // Classe, força, nível, XP... de cada jogador
    public static final Supplier<AttachmentType<DadosDoHeroi>> HEROI = ANEXOS.register("heroi",
            () -> AttachmentType.builder(() -> new DadosDoHeroi())
                    .serialize(new DadosDoHeroi.Serializador())
                    .copyOnDeath()
                    .build());

    // Baú que na verdade é um mímico (parece um baú comum, mas morde quem tenta abrir)
    public static final Supplier<AttachmentType<Boolean>> MIMICO = ANEXOS.register("mimico",
            () -> AttachmentType.builder(() -> false)
                    .serialize(Codec.BOOL)
                    .build());

    // Baú de cristal (só abre com a chave de cristal do andar)
    public static final Supplier<AttachmentType<Boolean>> BAU_DE_CRISTAL = ANEXOS.register("bau_de_cristal",
            () -> AttachmentType.builder(() -> false)
                    .serialize(Codec.BOOL)
                    .build());
}
