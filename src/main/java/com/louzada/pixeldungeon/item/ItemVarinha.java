package com.louzada.pixeldungeon.item;

import java.util.List;
import java.util.Random;

import com.louzada.pixeldungeon.Turno;
import com.louzada.pixeldungeon.heroi.Furtividade;
import com.louzada.pixeldungeon.heroi.RegrasDoHeroi;
import com.louzada.pixeldungeon.heroi.Talento;
import com.louzada.pixeldungeon.heroi.Talentos;
import com.louzada.pixeldungeon.heroi.TalentosDoTier2;
import com.louzada.pixeldungeon.registro.ModComponentes;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

// Uma varinha do Shattered achada na dungeon. Igual ao Shattered:
//   - clique direito lança (gasta 1 turno e as cargas; as cargas voltam sozinhas)
//   - cargas: as do tipo (2, ou 3 no míssil mágico) + 1 por nível, até 10
//   - vem sem identificar (o nível e as cargas ficam escondidos) e se identifica depois de 10 disparos
//     (a intuição do estudioso do Mago acelera pra 3x, e com +2 identifica no primeiro)
//   - 30% vêm amaldiçoadas: a magia sai aleatória (descobre na primeira vez que lançar)
//   - o Mago pode colocar a varinha no cajado dele: agachado, com o cajado na outra mão, clique direito
public class ItemVarinha extends Item implements Equipamento {
    public static final float USOS_PARA_IDENTIFICAR = 10;
    private static final int COR_DA_BARRA = 0xB07CFF;

    private TipoDeVarinha tipo;

    public ItemVarinha(Properties propriedades, TipoDeVarinha tipo) {
        super(propriedades.stacksTo(1));
        this.tipo = tipo;
    }

    public TipoDeVarinha tipo() {
        return tipo;
    }

    // Varinhas não pedem força
    @Override
    public int tier() {
        return 1;
    }

    @Override
    public Maldicao.Alvo alvoDasMaldicoes() {
        return Maldicao.Alvo.VARINHA;
    }

    // ---- Cargas ----

    public static int cargasMaximas(ItemStack pilha) {
        return pilha.getItem() instanceof ItemVarinha varinha ? varinha.tipo.cargasMaximas(NivelDoItem.nivel(pilha), 0) : 0;
    }

    public static int cargas(ItemStack pilha) {
        return Cargas.cargas(pilha, cargasMaximas(pilha));
    }

    public static void recarregar(ItemStack pilha, long agora) {
        Cargas.recarregar(pilha, agora, cargasMaximas(pilha), Varinha::ticksParaCarregar);
    }

    // Pergaminho de recarga: enche tudo na hora
    public static void encher(ItemStack pilha) {
        Cargas.somar(pilha, cargasMaximas(pilha), cargasMaximas(pilha));
    }

    // Uma vez por turno, no servidor, vê se encheu alguma carga
    @Override
    public void inventoryTick(ItemStack pilha, Level mundo, Entity dono, int espaco, boolean naMao) {
        if (!mundo.isClientSide() && Turno.virou(dono)) {
            recarregar(pilha, mundo.getGameTime());
        }
    }

    // ---- Achadas na dungeon ----

    // Igual ao Shattered: +0 (2/3), +1 (4/15), +2 (1/15); 30% amaldiçoadas; vem sem identificar
    public static ItemStack sortearDoChao(ItemStack pilha, Random sorteio) {
        int nivel = 0;
        if (sorteio.nextInt(3) == 0) {
            nivel++;
            if (sorteio.nextInt(5) == 0) {
                nivel++;
            }
        }
        NivelDoItem.definirNivel(pilha, nivel);
        if (sorteio.nextFloat() < 0.3F) {
            pilha.set(ModComponentes.MALDICAO.get(), Maldicao.CAOTICA.id());
        }
        pilha.set(ModComponentes.IDENTIFICADO.get(), false);
        pilha.set(ModComponentes.MALDICAO_CONHECIDA.get(), false);
        pilha.set(ModComponentes.USOS_PARA_IDENTIFICAR.get(), USOS_PARA_IDENTIFICAR);
        return pilha;
    }

    // ---- Lançar ----

    @Override
    public InteractionResultHolder<ItemStack> use(Level mundo, Player jogador, InteractionHand mao) {
        ItemStack pilha = jogador.getItemInHand(mao);
        if (!(jogador instanceof ServerPlayer heroi)) {
            return InteractionResultHolder.success(pilha);
        }
        // Agachado, com o cajado do Mago na outra mão: põe a varinha no cajado
        ItemStack outraMao = jogador.getItemInHand(mao == InteractionHand.MAIN_HAND ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND);
        if (heroi.isShiftKeyDown() && outraMao.getItem() instanceof CajadoDoMago) {
            CajadoDoMago.pedirParaColocar(heroi, outraMao, pilha);
            return InteractionResultHolder.success(pilha);
        }
        long agora = mundo.getGameTime();
        recarregar(pilha, agora);
        int cargas = cargas(pilha);
        if (cargas <= 0) {
            heroi.displayClientMessage(Component.translatable("mensagem.pixeldungeon.varinha_sem_carga").withStyle(ChatFormatting.GRAY), true);
            mundo.playSound(null, heroi.getX(), heroi.getY(), heroi.getZ(), SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 0.5F, 1.4F);
            return InteractionResultHolder.fail(pilha);
        }
        // Bateria de escudo (Mago): agachado, as cargas viram blindagem
        if (TalentosDoTier2.bateriaDeEscudo(heroi, pilha, cargas, cargasMaximas(pilha))) {
            heroi.getCooldowns().addCooldown(this, Turno.TICKS);
            return InteractionResultHolder.success(pilha);
        }
        int usadas;
        if (NivelDoItem.amaldicoado(pilha)) {
            // Igual ao Shattered: a varinha amaldiçoada se revela na primeira vez e a magia sai aleatória
            if (!NivelDoItem.maldicaoConhecida(pilha)) {
                pilha.remove(ModComponentes.MALDICAO_CONHECIDA.get());
                heroi.displayClientMessage(Component.translatable("mensagem.pixeldungeon.varinha_amaldicoada").withStyle(ChatFormatting.DARK_PURPLE), false);
            }
            EfeitosDeVarinha.lancarAmaldicoada(heroi);
            usadas = 1;
        } else {
            usadas = tipo.cargasPorDisparo(cargas);
            // Poder inscrito: depois de ler um pergaminho, alguns disparos saem com +2 níveis
            EfeitosDeVarinha.lancar(heroi, tipo, NivelDoItem.nivel(pilha) + TalentosDoTier2.niveisInscritos(heroi), usadas);
        }
        Talentos.aoUsarVarinha(heroi, cargas, usadas, false);
        Furtividade.revelar(heroi);
        // Identifica com o uso (intuição do estudioso +2: na hora)
        if (!NivelDoItem.identificado(pilha)) {
            if (RegrasDoHeroi.dados(heroi).pontos(Talento.INTUICAO_DO_ESTUDIOSO) >= 2) {
                NivelDoItem.identificar(heroi, pilha);
            } else {
                NivelDoItem.aoUsar(heroi, pilha);
            }
        }
        Cargas.gastar(pilha, agora, cargasMaximas(pilha), usadas);
        heroi.getCooldowns().addCooldown(this, Turno.TICKS);
        return InteractionResultHolder.success(pilha);
    }

    // ---- Aparência ----

    @Override
    public Component getName(ItemStack pilha) {
        return NivelDoItem.nome(pilha, super.getName(pilha));
    }

    // A barrinha mostra as cargas (só depois de identificada)
    @Override
    public boolean isBarVisible(ItemStack pilha) {
        return NivelDoItem.identificado(pilha);
    }

    @Override
    public int getBarWidth(ItemStack pilha) {
        return Cargas.larguraDaBarra(pilha, cargasMaximas(pilha));
    }

    @Override
    public int getBarColor(ItemStack pilha) {
        return COR_DA_BARRA;
    }

    // Mudar as cargas não faz a varinha "piscar" na mão
    @Override
    public boolean shouldCauseReequipAnimation(ItemStack antes, ItemStack depois, boolean trocouDeEspaco) {
        return trocouDeEspaco || !ItemStack.isSameItem(antes, depois);
    }

    @Override
    public void appendHoverText(ItemStack pilha, TooltipContext contexto, List<Component> linhas, TooltipFlag opcoes) {
        boolean conhecida = NivelDoItem.identificado(pilha);
        int nivel = conhecida ? NivelDoItem.nivel(pilha) : 0;
        linhas.add(Component.translatable("dica.pixeldungeon.varinha.lancar").withStyle(ChatFormatting.AQUA));
        efeito(tipo, nivel, conhecida ? cargas(pilha) : tipo.cargasMaximas(0, 0), linhas);
        if (conhecida) {
            linhas.add(Component.translatable("dica.pixeldungeon.cargas", cargas(pilha), cargasMaximas(pilha)).withStyle(ChatFormatting.LIGHT_PURPLE));
        } else {
            linhas.add(Component.translatable("dica.pixeldungeon.varinha.cargas_desconhecidas").withStyle(ChatFormatting.LIGHT_PURPLE));
        }
        linhas.add(Component.translatable("dica.pixeldungeon.varinha.colocar").withStyle(ChatFormatting.DARK_AQUA));
        NivelDoItem.dicaDeIdentificacao(pilha, linhas);
        Textos.adicionarQuebrado(linhas, tipo.chaveDaDescricao(), ChatFormatting.DARK_GRAY);
    }

    // As linhas da dica que dizem o que a varinha faz nesse nível (também usadas pelo cajado do Mago)
    public static void efeito(TipoDeVarinha tipo, int nivel, int cargas, List<Component> linhas) {
        int usadas = tipo.cargasPorDisparo(cargas);
        Object[] numeros = switch (tipo) {
            case CORROSAO -> new Object[] { 2 + nivel };
            case TRANSFUSAO -> new Object[] { 3 * nivel, 5 + nivel };
            case MATURACAO -> new Object[] { usadas };
            case RAJADA_DE_FOGO -> new Object[] { usadas, tipo.danoMinimo(nivel, usadas), tipo.danoMaximo(nivel, usadas) };
            case DESINTEGRACAO -> new Object[] { tipo.danoMinimo(nivel, 1), tipo.danoMaximo(nivel, 1), 6 + 2 * nivel };
            default -> new Object[] { tipo.danoMinimo(nivel, 1), tipo.danoMaximo(nivel, 1) };
        };
        Textos.adicionarQuebrado(linhas, tipo.chaveDoEfeito(), ChatFormatting.LIGHT_PURPLE, numeros);
    }
}
