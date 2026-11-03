package com.louzada.pixeldungeon.combate;

import org.joml.Vector3f;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;

// Partículas usadas no combate e nos efeitos, sempre mandadas pelo servidor (todo mundo por perto vê).
public class Particulas {
    // Cor do sangue no Shattered (cada inimigo pode ter a sua)
    public static final int SANGUE_PADRAO = 0xBB0000;

    // Poeirinha colorida (cor no formato 0xRRGGBB)
    public static DustParticleOptions poeira(int cor, float tamanho) {
        return new DustParticleOptions(new Vector3f(((cor >> 16) & 0xFF) / 255.0F, ((cor >> 8) & 0xFF) / 255.0F, (cor & 0xFF) / 255.0F), tamanho);
    }

    // Igual ao Shattered: quem apanha espirra sangue da cor dele (mais dano, mais sangue)
    public static void sangue(Entity alvo, int cor, float dano) {
        if (!(alvo.level() instanceof ServerLevel mundo)) {
            return;
        }
        int quantidade = Math.min(16, 3 + Math.round(dano));
        double largura = alvo.getBbWidth() * 0.35;
        mundo.sendParticles(poeira(cor, 1.2F), alvo.getX(), alvo.getY(0.6), alvo.getZ(), quantidade,
                largura, alvo.getBbHeight() * 0.25, largura, 0.0);
    }

    // Faíscas do ataque surpresa
    public static void faiscas(Entity alvo) {
        if (alvo.level() instanceof ServerLevel mundo) {
            double largura = alvo.getBbWidth() * 0.4;
            mundo.sendParticles(ParticleTypes.CRIT, alvo.getX(), alvo.getY(0.6), alvo.getZ(), 14, largura, alvo.getBbHeight() * 0.3, largura, 0.4);
        }
    }

    // Partículas que sobem em volta de alguém (subir de nível, curar...)
    public static void subindoEmVolta(Entity alvo, ParticleOptions tipo, int quantidade, double velocidade) {
        if (!(alvo.level() instanceof ServerLevel mundo)) {
            return;
        }
        RandomSource sorteio = mundo.getRandom();
        for (int i = 0; i < quantidade; i++) {
            double angulo = sorteio.nextDouble() * Math.PI * 2;
            double raio = alvo.getBbWidth() * (0.5 + sorteio.nextDouble() * 0.4);
            // Com quantidade 0, o Minecraft usa os 3 números seguintes como direção do movimento
            mundo.sendParticles(tipo, alvo.getX() + Math.cos(angulo) * raio, alvo.getY() + sorteio.nextDouble() * alvo.getBbHeight(),
                    alvo.getZ() + Math.sin(angulo) * raio, 0, 0.0, 1.0, 0.0, velocidade);
        }
    }

    // Uma explosão de partículas num ponto (míssil mágico acertando, gosma espirrando...)
    public static void explosao(ServerLevel mundo, ParticleOptions tipo, double x, double y, double z, int quantidade, double espalhar, double velocidade) {
        mundo.sendParticles(tipo, x, y, z, quantidade, espalhar, espalhar, espalhar, velocidade);
    }
}
