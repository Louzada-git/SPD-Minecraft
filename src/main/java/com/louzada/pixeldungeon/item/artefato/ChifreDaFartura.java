package com.louzada.pixeldungeon.item.artefato;

import java.util.List;

import com.louzada.pixeldungeon.Turno;
import com.louzada.pixeldungeon.heroi.DadosDoHeroi;
import com.louzada.pixeldungeon.heroi.RegrasDoHeroi;
import com.louzada.pixeldungeon.heroi.Talentos;
import com.louzada.pixeldungeon.heroi.TalentosDoTier2;
import com.louzada.pixeldungeon.item.ArtefatoAchado;
import com.louzada.pixeldungeon.item.EscolhaDeItem;
import com.louzada.pixeldungeon.registro.ModEfeitos;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;

// Chifre da fartura (o "HornOfPlenty" do Shattered): se enche de comida conforme o herói ganha experiência.
//   cargas: até 5 + nível/2; cada nível de herói ganho enche 5 x (0,25 + 0,125 x nível) cargas
//   lanchar (clique direito, a ação de sempre no Shattered): 1 carga mata um quinto da fome toda
//     (no Shattered, 90 de 450; aqui 3 da barra de fome)
//   guardar (agachado): o chifre come uma comida da mochila; cada ração inteira de comida guardada sobe um nível (até o 10)
// (No Minecraft, 1 ponto da barra de fome vale 30 de comida do Shattered: a ração dá 10.)
public class ChifreDaFartura extends ArtefatoAchado {
    private static final int FOME_POR_CARGA = 3;
    private static final int COMIDA_POR_NIVEL = 300;
    private static final int COMIDA_POR_PONTO = 30;

    public ChifreDaFartura(Properties propriedades) {
        super(propriedades);
    }

    @Override
    public String id() {
        return "chifre_da_fartura";
    }

    @Override
    public int nivelMaximo() {
        return 10;
    }

    @Override
    public int cargasMaximas(ItemStack pilha) {
        return 5 + nivel(pilha) / 2;
    }

    @Override
    protected int corDaBarra() {
        return 0xCC8833;
    }

    // O desenho enche com as cargas: 2, 5 e 8
    @Override
    public float estagio(ItemStack pilha) {
        int cargas = cargas(pilha);
        return cargas >= 8 ? 3 : cargas >= 5 ? 2 : cargas >= 2 ? 1 : 0;
    }

    @Override
    public void aoGanharXp(ServerPlayer jogador, ItemStack pilha, float porcao) {
        carregar(pilha, 5 * porcao * (0.25F + 0.125F * nivel(pilha)));
    }

    // Lanchar: 1 carga
    @Override
    public boolean usar(ServerPlayer jogador, ItemStack pilha) {
        if (cargas(pilha) <= 0) {
            avisar(jogador, "mensagem.pixeldungeon.chifre_da_fartura.vazio");
            return false;
        }
        definirCargas(pilha, cargas(pilha) - 1);
        jogador.getFoodData().eat(FOME_POR_CARGA, 0.5F);
        DadosDoHeroi dados = RegrasDoHeroi.dados(jogador);
        Talentos.aoComer(jogador, dados);
        TalentosDoTier2.aoComer(jogador, dados);
        int turnos = TalentosDoTier2.turnosPraComer(jogador, pilha, 3);
        if (turnos > 0) {
            jogador.addEffect(new MobEffectInstance(ModEfeitos.COMENDO, Turno.ticks(turnos), 0, false, false, true));
        }
        jogador.level().playSound(null, jogador.blockPosition(), SoundEvents.GENERIC_EAT, SoundSource.PLAYERS, 1.0F, 1.0F);
        return true;
    }

    // Guardar: escolher uma comida da mochila
    @Override
    public boolean usarAgachado(ServerPlayer jogador, ItemStack pilha) {
        if (nivel(pilha) >= nivelMaximo()) {
            avisar(jogador, "mensagem.pixeldungeon.chifre_da_fartura.cheio");
            return false;
        }
        boolean abriu = EscolhaDeItem.pedirProArtefato(jogador, id(), outra -> outra.has(DataComponents.FOOD), (heroi, espaco) -> {
            ItemStack chifre = ArtefatoAchado.equipado(heroi, ChifreDaFartura.class);
            if (chifre != null) {
                guardar(heroi, chifre, heroi.getInventory().getItem(espaco));
            }
        });
        if (!abriu) {
            avisar(jogador, "mensagem.pixeldungeon.chifre_da_fartura.sem_comida");
        }
        return abriu;
    }

    // O chifre come a comida: cada 300 de comida (uma ração) é um nível
    public void guardar(ServerPlayer jogador, ItemStack chifre, ItemStack comida) {
        FoodProperties alimento = comida.get(DataComponents.FOOD);
        if (alimento == null || nivel(chifre) >= nivelMaximo()) {
            return;
        }
        comida.shrink(1);
        int guardada = experiencia(chifre) + alimento.nutrition() * COMIDA_POR_PONTO;
        while (guardada >= COMIDA_POR_NIVEL && nivel(chifre) < nivelMaximo()) {
            guardada -= COMIDA_POR_NIVEL;
            subirNivel(jogador, chifre);
        }
        definirExperiencia(chifre, nivel(chifre) >= nivelMaximo() ? 0 : guardada);
        jogador.displayClientMessage(Component.translatable("mensagem.pixeldungeon.chifre_da_fartura.guardou").withStyle(ChatFormatting.GRAY), true);
        jogador.level().playSound(null, jogador.blockPosition(), SoundEvents.GENERIC_EAT, SoundSource.PLAYERS, 0.8F, 0.7F);
    }

    @Override
    protected void dicaExtra(ItemStack pilha, List<Component> linhas) {
        if (nivel(pilha) < nivelMaximo()) {
            linhas.add(Component.translatable("dica.pixeldungeon.chifre_da_fartura.guardada", experiencia(pilha), COMIDA_POR_NIVEL)
                    .withStyle(ChatFormatting.GRAY));
        }
    }
}
