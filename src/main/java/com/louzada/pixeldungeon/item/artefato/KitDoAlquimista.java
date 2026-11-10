package com.louzada.pixeldungeon.item.artefato;

import java.util.ArrayList;
import java.util.List;

import com.louzada.pixeldungeon.Turno;
import com.louzada.pixeldungeon.alquimia.EnergiaAlquimica;
import com.louzada.pixeldungeon.alquimia.MenuDaAlquimia;
import com.louzada.pixeldungeon.item.ArtefatoAchado;
import com.louzada.pixeldungeon.item.EscolhaDeOpcao;
import com.louzada.pixeldungeon.registro.ModComponentes;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;

// Kit de ferramentas do alquimista (o "AlchemistsToolkit" do Shattered): a alquimia sem precisar de pote.
//   misturar (clique direito): abre a tela da alquimia; as receitas gastam primeiro a energia guardada no kit, depois a do herói
//   energizar (agachado): cada 6 de energia do herói sobe um nível do kit (até o 10)
//   equipado, o kit guarda energia conforme o herói ganha experiência: (2 + nível) por nível de herói
//   quando é equipado, ele precisa esquentar antes de usar: 100 turnos no nível 0, (10 - nível)² turnos depois (no 10, nada)
public class KitDoAlquimista extends ArtefatoAchado {
    public static final int ENERGIA_POR_NIVEL = 6;
    private static final int COR_DA_BARRA = 0xFF8844;

    public KitDoAlquimista(Properties propriedades) {
        super(propriedades);
    }

    @Override
    public String id() {
        return "kit_do_alquimista";
    }

    @Override
    public int nivelMaximo() {
        return 10;
    }

    // A energia guardada não tem limite (não usa a barrinha de cargas)
    @Override
    public int cargasMaximas(ItemStack pilha) {
        return 0;
    }

    public static float aquecimento(ItemStack pilha) {
        return pilha.getOrDefault(ModComponentes.AQUECIMENTO.get(), 0F);
    }

    public static boolean pronto(ItemStack pilha) {
        return aquecimento(pilha) <= 0;
    }

    // A energia guardada no kit
    public static int energiaGuardada(ItemStack pilha) {
        return cargas(pilha);
    }

    // ---- Esquentar e guardar energia ----

    @Override
    public void aoTurno(ServerPlayer jogador, ItemStack pilha) {
        long agora = jogador.level().getGameTime();
        Long antes = pilha.get(ModComponentes.EQUIPADO_EM.get());
        if (antes == null || agora - antes > 2L * Turno.TICKS || agora < antes) {
            // Acabou de ser equipado: começa a esquentar
            pilha.set(ModComponentes.AQUECIMENTO.get(), nivel(pilha) >= nivelMaximo() ? 0F : 100F);
        } else if (aquecimento(pilha) > 0) {
            float turnos = (float) Math.pow(nivelMaximo() - nivel(pilha), 2);
            float falta = nivel(pilha) >= nivelMaximo() ? 0 : aquecimento(pilha) - 100F / turnos;
            pilha.set(ModComponentes.AQUECIMENTO.get(), Math.max(0, falta));
        }
        pilha.set(ModComponentes.EQUIPADO_EM.get(), agora);
    }

    // Imune à magia, o kit não esquenta (mas continua equipado)
    @Override
    public void aoTurnoSemMagia(ServerPlayer jogador, ItemStack pilha) {
        pilha.set(ModComponentes.EQUIPADO_EM.get(), jogador.level().getGameTime());
    }

    @Override
    public void aoGanharXp(ServerPlayer jogador, ItemStack pilha, float porcao) {
        guardar(pilha, (2 + nivel(pilha)) * porcao * energia(jogador));
    }

    @Override
    public void recarregarDeFora(ServerPlayer jogador, ItemStack pilha, float quanto) {
        guardar(pilha, 0.25F * quanto);
    }

    private void guardar(ItemStack pilha, float quanto) {
        float parcial = parcial(pilha) + quanto;
        int energia = energiaGuardada(pilha);
        while (parcial >= 1) {
            energia++;
            parcial--;
        }
        definirCargas(pilha, energia);
        definirParcial(pilha, parcial);
    }

    // Gasta a energia do kit primeiro; devolve quanto ainda falta (pra gastar do herói)
    public static int gastar(ItemStack pilha, int quanto) {
        int doKit = Math.min(energiaGuardada(pilha), quanto);
        definirCargas(pilha, energiaGuardada(pilha) - doKit);
        return quanto - doKit;
    }

    // ---- Misturar ----

    @Override
    public boolean usar(ServerPlayer jogador, ItemStack pilha) {
        if (!pronto(pilha)) {
            avisar(jogador, "mensagem.pixeldungeon.kit_do_alquimista.esquentando");
            return false;
        }
        jogador.openMenu(new SimpleMenuProvider((id, inventario, quem) -> new MenuDaAlquimia(id, inventario, ContainerLevelAccess.NULL, true),
                Component.translatable("item.pixeldungeon.kit_do_alquimista")));
        return true;
    }

    // ---- Energizar: 6 de energia por nível ----

    @Override
    public boolean usarAgachado(ServerPlayer jogador, ItemStack pilha) {
        if (nivel(pilha) >= nivelMaximo()) {
            avisar(jogador, "mensagem.pixeldungeon.kit_do_alquimista.cheio");
            return false;
        }
        int energia = EnergiaAlquimica.energiaDe(jogador);
        if (energia < ENERGIA_POR_NIVEL) {
            avisar(jogador, "mensagem.pixeldungeon.kit_do_alquimista.sem_energia");
            return false;
        }
        int niveis = Math.min(nivelMaximo() - nivel(pilha), energia / ENERGIA_POR_NIVEL);
        List<Component> opcoes = new ArrayList<>();
        opcoes.add(Component.translatable("tela.pixeldungeon.kit_do_alquimista.um_nivel", ENERGIA_POR_NIVEL));
        if (niveis > 1) {
            opcoes.add(Component.translatable("tela.pixeldungeon.kit_do_alquimista.todos", ENERGIA_POR_NIVEL * niveis, niveis));
        }
        EscolhaDeOpcao.pedir(jogador, Component.translatable("item.pixeldungeon.kit_do_alquimista"),
                Component.translatable("tela.pixeldungeon.kit_do_alquimista.energizar"), pilha.copy(), opcoes, (heroi, escolha) -> {
                    ItemStack kit = ArtefatoAchado.equipado(heroi, KitDoAlquimista.class);
                    if (kit != null && escolha >= 0) {
                        energizar(heroi, kit, escolha == 0 ? 1 : niveis);
                    }
                });
        return true;
    }

    // Sobe "niveis" níveis gastando 6 de energia do herói por nível (se ainda tiver)
    public void energizar(ServerPlayer jogador, ItemStack kit, int niveis) {
        int podem = Math.min(niveis, Math.min(nivelMaximo() - nivel(kit), EnergiaAlquimica.energiaDe(jogador) / ENERGIA_POR_NIVEL));
        if (podem <= 0 || !EnergiaAlquimica.gastar(jogador, podem * ENERGIA_POR_NIVEL)) {
            return;
        }
        for (int i = 0; i < podem; i++) {
            subirNivel(jogador, kit);
        }
        jogador.level().playSound(null, jogador.blockPosition(), SoundEvents.GENERIC_DRINK, SoundSource.PLAYERS, 1.0F, 1.0F);
    }

    // ---- Aparência ----

    // A barrinha mostra quanto falta pra esquentar
    @Override
    public boolean isBarVisible(ItemStack pilha) {
        return !pronto(pilha);
    }

    @Override
    public int getBarWidth(ItemStack pilha) {
        return Math.round(13.0F * (100F - Math.min(100F, aquecimento(pilha))) / 100F);
    }

    @Override
    public int getBarColor(ItemStack pilha) {
        return COR_DA_BARRA;
    }

    @Override
    public boolean shouldCauseReequipAnimation(ItemStack antes, ItemStack depois, boolean trocouDeEspaco) {
        return trocouDeEspaco || !ItemStack.isSameItem(antes, depois);
    }

    @Override
    protected void dicaExtra(ItemStack pilha, List<Component> linhas) {
        linhas.add(Component.translatable("dica.pixeldungeon.kit_do_alquimista.energia", energiaGuardada(pilha)).withStyle(ChatFormatting.GRAY));
        if (!pronto(pilha)) {
            linhas.add(Component.translatable("dica.pixeldungeon.kit_do_alquimista.esquentando", Math.max(0, 100 - (int) aquecimento(pilha)))
                    .withStyle(ChatFormatting.GRAY));
        }
    }
}
