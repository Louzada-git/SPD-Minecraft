package com.louzada.pixeldungeon.registro;

import java.util.function.Supplier;

import com.louzada.pixeldungeon.PixelDungeon;
import com.louzada.pixeldungeon.heroi.DadosDoHeroi;

import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

// Dados extras que o mod "pendura" nos jogadores (e que ficam salvos com eles).
public class ModAnexos {
    public static final DeferredRegister<AttachmentType<?>> ANEXOS = DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, PixelDungeon.MODID);

    // Classe, força, nível, XP... de cada jogador
    public static final Supplier<AttachmentType<DadosDoHeroi>> HEROI = ANEXOS.register("heroi",
            () -> AttachmentType.builder(() -> new DadosDoHeroi())
                    .serialize(new DadosDoHeroi.Serializador())
                    .copyOnDeath()
                    .build());
}
