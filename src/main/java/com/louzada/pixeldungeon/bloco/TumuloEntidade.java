package com.louzada.pixeldungeon.bloco;

import java.util.ArrayList;
import java.util.List;

import com.louzada.pixeldungeon.registro.ModBlocos;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

// O que fica guardado dentro de um túmulo (ver Tumulo)
public class TumuloEntidade extends BlockEntity {
    private static final int ESPACOS = 4;
    private NonNullList<ItemStack> itens = NonNullList.withSize(ESPACOS, ItemStack.EMPTY);

    public TumuloEntidade(BlockPos posicao, BlockState estado) {
        super(ModBlocos.ENTIDADE_DO_TUMULO.get(), posicao, estado);
    }

    public void guardar(List<ItemStack> novos) {
        itens = NonNullList.withSize(ESPACOS, ItemStack.EMPTY);
        for (int i = 0; i < novos.size() && i < ESPACOS; i++) {
            itens.set(i, novos.get(i).copy());
        }
        setChanged();
    }

    public List<ItemStack> tirarTudo() {
        List<ItemStack> tirados = new ArrayList<>();
        for (ItemStack item : itens) {
            if (!item.isEmpty()) {
                tirados.add(item);
            }
        }
        itens = NonNullList.withSize(ESPACOS, ItemStack.EMPTY);
        setChanged();
        return tirados;
    }

    public List<ItemStack> itens() {
        List<ItemStack> dentro = new ArrayList<>();
        for (ItemStack item : itens) {
            if (!item.isEmpty()) {
                dentro.add(item);
            }
        }
        return dentro;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registros) {
        super.saveAdditional(tag, registros);
        ContainerHelper.saveAllItems(tag, itens, registros);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registros) {
        super.loadAdditional(tag, registros);
        itens = NonNullList.withSize(ESPACOS, ItemStack.EMPTY);
        ContainerHelper.loadAllItems(tag, itens, registros);
    }
}
