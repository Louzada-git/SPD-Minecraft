package com.louzada.pixeldungeon.alquimia;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import com.louzada.pixeldungeon.bloco.PoteDeAlquimia;
import com.louzada.pixeldungeon.item.ArtefatoAchado;
import com.louzada.pixeldungeon.item.artefato.KitDoAlquimista;
import com.louzada.pixeldungeon.registro.ModMenus;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

// A tela do pote de alquimia (a "AlchemyScene" do Shattered), do lado do servidor:
//   - 3 casinhas de ingredientes; as receitas dependem de quantas estão ocupadas e do que tem nelas
//   - até 3 receitas servem ao mesmo tempo (um pergaminho vira pedras ou o exótico): cada uma tem o botão "Criar"
//     dela, com o que vai sair do lado (apagadinho)
//   - "Criar": gasta os ingredientes (e a energia, se a receita pedir) e o resultado aparece na casinha da direita
//     (o da mistura anterior, se ninguém pegou, vai pro inventário)
//   - "Energizar": desfaz o que estiver nas casinhas de ingredientes em energia alquímica (só o que dá energia)
// Ao fechar, o que sobrou nas casinhas volta pro inventário.
// Aberta pelo kit do alquimista (sem pote), as receitas gastam primeiro a energia guardada no kit, igual ao Shattered.
public class MenuDaAlquimia extends AbstractContainerMenu {
    public static final int INGREDIENTES = 3;
    public static final int RECEITAS = ReceitasDaAlquimia.RECEITAS_POR_VEZ;
    public static final int RESULTADO = 3;
    public static final int PRIMEIRO_EXEMPLO = 4;
    public static final int PRIMEIRA_DO_JOGADOR = PRIMEIRO_EXEMPLO + RECEITAS;
    // Os botões: "Criar" da receita 0, 1 ou 2, e "Energizar"
    public static final int BOTAO_MISTURAR = 0;
    public static final int BOTAO_ENERGIZAR = 9;
    // Os números que a tela recebe: a energia do herói, quanto dá energizar, o custo de cada receita (-1: nenhuma)
    // e a energia guardada no kit do alquimista (-1: aberta no pote)
    public static final int DADO_ENERGIA = 0;
    public static final int DADO_ENERGIZAR = 1;
    public static final int DADO_CUSTO = 2;
    public static final int DADO_KIT = DADO_CUSTO + RECEITAS;
    public static final int DADOS = DADO_KIT + 1;

    // Onde ficam as casinhas na tela (a TelaDaAlquimia desenha em volta)
    public static final int X_DOS_INGREDIENTES = 12;
    public static final int Y_DOS_INGREDIENTES = 22;
    public static final int X_DOS_EXEMPLOS = 112;
    public static final int X_DO_RESULTADO = 150;
    public static final int Y_DO_RESULTADO = 44;
    public static final int X_DO_INVENTARIO = 8;
    public static final int Y_DO_INVENTARIO = 124;

    private final Player jogador;
    private final ContainerLevelAccess acesso;
    private final boolean doKit;
    private final SimpleContainer ingredientes = new SimpleContainer(INGREDIENTES) {
        @Override
        public void setChanged() {
            super.setChanged();
            slotsChanged(this);
        }
    };
    private final SimpleContainer resultado = new SimpleContainer(1);
    private final SimpleContainer exemplos = new SimpleContainer(RECEITAS);
    private final ContainerData dados;
    private int[] custos = { -1, -1, -1 };
    private int energizar = 0;

    // Na tela (o cliente)
    public MenuDaAlquimia(int id, Inventory inventario) {
        this(id, inventario, ContainerLevelAccess.NULL, new SimpleContainerData(DADOS));
    }

    // No servidor: "acesso" é o lugar do pote
    public MenuDaAlquimia(int id, Inventory inventario, ContainerLevelAccess acesso) {
        this(id, inventario, acesso, false);
    }

    // doKit: aberta pelo kit do alquimista (sem pote)
    public MenuDaAlquimia(int id, Inventory inventario, ContainerLevelAccess acesso, boolean doKit) {
        this(id, inventario, acesso, doKit, null);
    }

    private MenuDaAlquimia(int id, Inventory inventario, ContainerLevelAccess acesso, SimpleContainerData doCliente) {
        this(id, inventario, acesso, false, doCliente);
    }

    private MenuDaAlquimia(int id, Inventory inventario, ContainerLevelAccess acesso, boolean doKit, SimpleContainerData doCliente) {
        super(ModMenus.ALQUIMIA.get(), id);
        this.jogador = inventario.player;
        this.acesso = acesso;
        this.doKit = doKit;
        this.dados = doCliente != null ? doCliente : new ContainerData() {
            @Override
            public int get(int qual) {
                if (qual >= DADO_CUSTO && qual < DADO_KIT) {
                    return custos[qual - DADO_CUSTO];
                }
                return switch (qual) {
                    case DADO_ENERGIA -> jogador instanceof ServerPlayer heroi ? EnergiaAlquimica.energiaDe(heroi) : 0;
                    case DADO_ENERGIZAR -> energizar;
                    case DADO_KIT -> energiaDoKit();
                    default -> 0;
                };
            }

            @Override
            public void set(int qual, int valor) {
            }

            @Override
            public int getCount() {
                return DADOS;
            }
        };
        for (int i = 0; i < INGREDIENTES; i++) {
            addSlot(new Slot(ingredientes, i, X_DOS_INGREDIENTES, Y_DOS_INGREDIENTES + i * 22));
        }
        addSlot(new Slot(resultado, 0, X_DO_RESULTADO, Y_DO_RESULTADO) {
            @Override
            public boolean mayPlace(ItemStack pilha) {
                return false;
            }
        });
        // O que cada receita vai fazer: só pra mostrar (a tela desenha apagadinho; ninguém pega nem põe)
        for (int i = 0; i < RECEITAS; i++) {
            addSlot(new Slot(exemplos, i, X_DOS_EXEMPLOS, Y_DOS_INGREDIENTES + i * 22) {
                @Override
                public boolean mayPlace(ItemStack pilha) {
                    return false;
                }

                @Override
                public boolean mayPickup(Player quem) {
                    return false;
                }

                @Override
                public boolean isActive() {
                    return false;
                }
            });
        }
        for (int linha = 0; linha < 3; linha++) {
            for (int coluna = 0; coluna < 9; coluna++) {
                addSlot(new Slot(inventario, 9 + linha * 9 + coluna, X_DO_INVENTARIO + coluna * 18, Y_DO_INVENTARIO + linha * 18));
            }
        }
        for (int coluna = 0; coluna < 9; coluna++) {
            addSlot(new Slot(inventario, coluna, X_DO_INVENTARIO + coluna * 18, Y_DO_INVENTARIO + 58));
        }
        addDataSlots(dados);
    }

    // A energia do herói
    public int energia() {
        return dados.get(DADO_ENERGIA);
    }

    // A energia guardada no kit do alquimista (-1: a tela é do pote)
    public int energiaNoKit() {
        return dados.get(DADO_KIT);
    }

    // Toda a energia que dá pra gastar (a do herói, mais a do kit)
    public int energiaTotal() {
        return energia() + Math.max(0, energiaNoKit());
    }

    // O kit equipado (null: a tela é do pote, ou ele não está mais nas mãos)
    @Nullable
    private ItemStack kit() {
        return doKit ? ArtefatoAchado.equipado(jogador, KitDoAlquimista.class) : null;
    }

    private int energiaDoKit() {
        ItemStack kit = kit();
        return kit == null ? -1 : KitDoAlquimista.energiaGuardada(kit);
    }

    // Gasta a energia da receita: primeiro a do kit, depois a do herói
    private boolean gastar(ServerPlayer heroi, int custo) {
        ItemStack kit = kit();
        if (kit == null || custo <= 0) {
            return EnergiaAlquimica.gastar(heroi, custo);
        }
        if (KitDoAlquimista.energiaGuardada(kit) + EnergiaAlquimica.energiaDe(heroi) < custo) {
            return false;
        }
        return EnergiaAlquimica.gastar(heroi, KitDoAlquimista.gastar(kit, custo));
    }

    // O custo da receita (-1: não tem essa receita)
    public int custo(int receita) {
        return dados.get(DADO_CUSTO + receita);
    }

    // O custo da primeira receita
    public int custo() {
        return custo(0);
    }

    public int energiaAoEnergizar() {
        return dados.get(DADO_ENERGIZAR);
    }

    public ItemStack exemplo(int receita) {
        return exemplos.getItem(receita);
    }

    public ItemStack exemplo() {
        return exemplo(0);
    }

    // ---- O que a receita vai fazer ----

    private Ingredientes ingredientes(ServerPlayer heroi) {
        List<ItemStack> casinhas = new ArrayList<>();
        for (int i = 0; i < INGREDIENTES; i++) {
            casinhas.add(ingredientes.getItem(i));
        }
        return new Ingredientes(heroi, casinhas);
    }

    @Override
    public void slotsChanged(Container mudou) {
        super.slotsChanged(mudou);
        atualizar();
    }

    // Recalcula as receitas, os custos, os exemplos e quanto dá energizar (só no servidor; a tela recebe pronto)
    public void atualizar() {
        if (!(jogador instanceof ServerPlayer heroi)) {
            return;
        }
        Ingredientes atuais = ingredientes(heroi);
        List<ReceitaDeAlquimia> receitas = ReceitasDaAlquimia.achar(atuais);
        for (int i = 0; i < RECEITAS; i++) {
            ReceitaDeAlquimia receita = i < receitas.size() ? receitas.get(i) : null;
            custos[i] = receita == null ? -1 : receita.custo(atuais);
            exemplos.setItem(i, receita == null ? ItemStack.EMPTY : receita.exemplo(atuais));
        }
        int total = 0;
        for (ItemStack pilha : atuais.pilhas()) {
            total += ReceitasDaAlquimia.energia(heroi, pilha);
        }
        energizar = total;
        broadcastChanges();
    }

    // ---- Os botões ----

    @Override
    public boolean clickMenuButton(Player quem, int botao) {
        if (!(quem instanceof ServerPlayer heroi)) {
            return false;
        }
        boolean fez = false;
        if (botao >= BOTAO_MISTURAR && botao < BOTAO_MISTURAR + RECEITAS) {
            fez = misturar(heroi, botao - BOTAO_MISTURAR);
        } else if (botao == BOTAO_ENERGIZAR) {
            fez = energizar(heroi);
        }
        if (fez) {
            atualizar();
        }
        return fez;
    }

    // Mistura a primeira receita
    public boolean misturar(ServerPlayer heroi) {
        return misturar(heroi, 0);
    }

    // Mistura essa receita (0 a 2), se der (precisa da energia). O resultado da mistura anterior, se ainda estiver lá,
    // vai pro inventário.
    public boolean misturar(ServerPlayer heroi, int qual) {
        Ingredientes atuais = ingredientes(heroi);
        List<ReceitaDeAlquimia> receitas = ReceitasDaAlquimia.achar(atuais);
        ReceitaDeAlquimia receita = qual < receitas.size() ? receitas.get(qual) : null;
        if (receita == null || !gastar(heroi, receita.custo(atuais))) {
            return false;
        }
        ItemStack anterior = resultado.removeItemNoUpdate(0);
        if (!anterior.isEmpty() && !heroi.getInventory().add(anterior)) {
            heroi.drop(anterior, false);
        }
        resultado.setItem(0, receita.misturar(atuais));
        for (int i = 0; i < INGREDIENTES; i++) {
            if (ingredientes.getItem(i).isEmpty()) {
                ingredientes.setItem(i, ItemStack.EMPTY);
            }
        }
        ingredientes.setChanged();
        efeito(heroi, true);
        return true;
    }

    // Desfaz em energia tudo que estiver nas casinhas de ingredientes e der energia
    public boolean energizar(ServerPlayer heroi) {
        int total = 0;
        for (int i = 0; i < INGREDIENTES; i++) {
            total += ReceitasDaAlquimia.energizar(heroi, ingredientes.getItem(i));
            if (ingredientes.getItem(i).isEmpty()) {
                ingredientes.setItem(i, ItemStack.EMPTY);
            }
        }
        if (total <= 0) {
            return false;
        }
        EnergiaAlquimica.ganhar(heroi, total);
        ingredientes.setChanged();
        efeito(heroi, false);
        return true;
    }

    // O barulho e as bolhas no pote
    private void efeito(ServerPlayer heroi, boolean misturou) {
        acesso.execute((mundo, posicao) -> {
            if (mundo instanceof ServerLevel servidor) {
                servidor.sendParticles(misturou ? ParticleTypes.WITCH : ParticleTypes.ELECTRIC_SPARK, posicao.getX() + 1.0, posicao.getY() + 1.0,
                        posicao.getZ() + 1.0, 20, 0.5, 0.3, 0.5, 0.05);
            }
        });
        heroi.level().playSound(null, heroi.blockPosition(), misturou ? SoundEvents.BREWING_STAND_BREW : SoundEvents.AMETHYST_BLOCK_RESONATE,
                SoundSource.BLOCKS, 1.0F, 1.0F);
    }

    // ---- Mexer nas casinhas ----

    @Override
    public ItemStack quickMoveStack(Player quem, int indice) {
        Slot casinha = slots.get(indice);
        if (!casinha.hasItem() || (indice >= PRIMEIRO_EXEMPLO && indice < PRIMEIRA_DO_JOGADOR)) {
            return ItemStack.EMPTY;
        }
        ItemStack pilha = casinha.getItem();
        ItemStack antes = pilha.copy();
        if (indice < PRIMEIRA_DO_JOGADOR) {
            if (!moveItemStackTo(pilha, PRIMEIRA_DO_JOGADOR, slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else if (!moveItemStackTo(pilha, 0, INGREDIENTES, false)) {
            return ItemStack.EMPTY;
        }
        if (pilha.isEmpty()) {
            casinha.setByPlayer(ItemStack.EMPTY);
        } else {
            casinha.setChanged();
        }
        if (pilha.getCount() == antes.getCount()) {
            return ItemStack.EMPTY;
        }
        casinha.onTake(quem, pilha);
        return antes;
    }

    @Override
    public boolean stillValid(Player quem) {
        if (doKit) {
            return kit() != null;
        }
        return acesso.evaluate((mundo, posicao) -> mundo.getBlockState(posicao).getBlock() instanceof PoteDeAlquimia
                && quem.distanceToSqr(posicao.getX() + 0.5, posicao.getY() + 0.5, posicao.getZ() + 0.5) <= 100, true);
    }

    @Override
    public void removed(Player quem) {
        super.removed(quem);
        exemplos.clearContent();
        clearContainer(quem, ingredientes);
        clearContainer(quem, resultado);
    }
}
