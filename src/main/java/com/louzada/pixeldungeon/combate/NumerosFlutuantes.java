package com.louzada.pixeldungeon.combate;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

// Textos que sobem em cima dos personagens e somem, igual ao Shattered:
// dano em vermelho/laranja, "desviou" em branco, XP em roxo...
// Usa as "text displays" do próprio Minecraft, então funciona sem nada no jogo do jogador.
public class NumerosFlutuantes {
    // Marca pra reconhecer os textos do mod (e apagar os que ficaram salvos se o mundo fechou no meio)
    private static final String MARCA = "pixeldungeon_numero";
    private static final int DURACAO = 20;
    private static final double SUBIDA_POR_TICK = 0.04;

    public static final String VERMELHO = "#ff4444";
    public static final String LARANJA = "#ffaa33";
    public static final String BRANCO = "#ffffff";
    public static final String AMARELO = "#ffff44";
    public static final String ROXO = "#cc77ff";
    public static final String VERDE = "#55ff55";

    private record Numero(Entity texto, int[] ticksRestantes) {
    }

    private static List<Numero> ativos = new ArrayList<>();

    // Mostra um texto em cima de alguém (números e símbolos)
    public static void mostrar(Entity emCimaDe, String texto, String cor) {
        criar(emCimaDe, "{\"text\":\"" + texto.replace("\"", "") + "\",\"color\":\"" + cor + "\",\"bold\":true}");
    }

    // Mostra um texto do arquivo de idioma (cada jogador vê na língua dele)
    public static void mostrarTraduzido(Entity emCimaDe, String chave, String cor) {
        criar(emCimaDe, "{\"translate\":\"" + chave + "\",\"color\":\"" + cor + "\",\"bold\":true}");
    }

    private static void criar(Entity emCimaDe, String textoEmJson) {
        if (!(emCimaDe.level() instanceof ServerLevel mundo)) {
            return;
        }
        CompoundTag tag = new CompoundTag();
        tag.putString("id", "minecraft:text_display");
        tag.putString("text", textoEmJson);
        tag.putString("billboard", "center");
        // O Minecraft sempre lê o alinhamento; sem ele, cada número solta um aviso no log
        tag.putString("alignment", "center");
        tag.putInt("background", 0);
        tag.putBoolean("shadow", true);
        tag.putInt("teleport_duration", 2);
        ListTag marcas = new ListTag();
        marcas.add(StringTag.valueOf(MARCA));
        tag.put("Tags", marcas);

        Entity entidade = EntityType.loadEntityRecursive(tag, mundo, criada -> criada);
        if (entidade == null) {
            return;
        }
        double desvio = (mundo.getRandom().nextDouble() - 0.5) * 0.4;
        entidade.moveTo(emCimaDe.getX() + desvio, emCimaDe.getY() + emCimaDe.getBbHeight() + 0.3, emCimaDe.getZ() + desvio);
        ativos.add(new Numero(entidade, new int[] { DURACAO }));
        mundo.addFreshEntity(entidade);
    }

    // A cada tick, os textos sobem um pouquinho; quando o tempo acaba, somem
    public static void aoTickDoServidor(ServerTickEvent.Post evento) {
        if (ativos.isEmpty()) {
            return;
        }
        List<Numero> continuam = new ArrayList<>();
        for (Numero numero : ativos) {
            numero.ticksRestantes()[0]--;
            if (numero.ticksRestantes()[0] <= 0 || numero.texto().isRemoved()) {
                numero.texto().discard();
            } else {
                Entity texto = numero.texto();
                texto.setPos(texto.getX(), texto.getY() + SUBIDA_POR_TICK, texto.getZ());
                continuam.add(numero);
            }
        }
        ativos = continuam;
    }

    // Textos que ficaram salvos no mundo (o mundo fechou no meio da animação) não voltam
    public static void aoEntrarNoMundo(EntityJoinLevelEvent evento) {
        if (evento.getLevel().isClientSide() || !evento.getEntity().getTags().contains(MARCA)) {
            return;
        }
        for (Numero numero : ativos) {
            if (numero.texto() == evento.getEntity()) {
                return;
            }
        }
        evento.setCanceled(true);
    }
}
