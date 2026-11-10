package com.louzada.pixeldungeon.item;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import com.louzada.pixeldungeon.heroi.GerenciadorDoHeroi;
import com.louzada.pixeldungeon.heroi.RegrasDoHeroi;
import com.louzada.pixeldungeon.registro.ModComponentes;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

// Os artefatos que se acha pela dungeon (o "Artifact" do Shattered). Cada um carrega e sobe de nível do seu jeito, mas todos:
//   - só funcionam equipados: na mão (o clique direito usa; agachado, a outra ação) ou na casinha de artefato (a outra mão)
//   - guardam no próprio item as cargas (e a parte da próxima), o nível e a experiência
//   - mostram as cargas na barrinha do ícone
//   - cada tipo só aparece uma vez por run
// (A capa das sombras e o tomo sagrado são artefatos das classes e ficam à parte.)
public abstract class ArtefatoAchado extends Item {

    protected ArtefatoAchado(Properties propriedades) {
        super(propriedades.stacksTo(1));
    }

    // O nome no código e nos textos (chifre_da_fartura...)
    public abstract String id();

    public abstract int nivelMaximo();

    // 0 = não tem cargas (o cálice)
    public abstract int cargasMaximas(ItemStack pilha);

    protected int corDaBarra() {
        return 0xFFCC44;
    }

    // Com quantas cargas o artefato aparece (a ampulheta e a chave vêm cheias; as correntes, com 5)
    protected int cargasIniciais(ItemStack pilha) {
        return 0;
    }

    // Qual desenho mostrar (0 = o primeiro). O cálice, o chifre e as sandálias mudam de desenho.
    public float estagio(ItemStack pilha) {
        return 0;
    }

    // ---- Números guardados no item ----

    public static int nivel(ItemStack pilha) {
        return pilha.getOrDefault(ModComponentes.NIVEL.get(), 0);
    }

    public static int cargas(ItemStack pilha) {
        Integer cargas = pilha.get(ModComponentes.CARGAS.get());
        if (cargas != null) {
            return cargas;
        }
        return pilha.getItem() instanceof ArtefatoAchado artefato ? artefato.cargasIniciais(pilha) : 0;
    }

    public static void definirCargas(ItemStack pilha, int cargas) {
        pilha.set(ModComponentes.CARGAS.get(), Math.max(0, cargas));
    }

    public static float parcial(ItemStack pilha) {
        return pilha.getOrDefault(ModComponentes.CARGA_PARCIAL.get(), 0F);
    }

    public static void definirParcial(ItemStack pilha, float parcial) {
        pilha.set(ModComponentes.CARGA_PARCIAL.get(), parcial);
    }

    public static int experiencia(ItemStack pilha) {
        return pilha.getOrDefault(ModComponentes.EXPERIENCIA.get(), 0);
    }

    public static void definirExperiencia(ItemStack pilha, int experiencia) {
        pilha.set(ModComponentes.EXPERIENCIA.get(), Math.max(0, experiencia));
    }

    // Soma carga (a parte quebrada fica guardada); não passa do máximo
    public void carregar(ItemStack pilha, float quanto) {
        int maximo = cargasMaximas(pilha);
        int cargas = cargas(pilha);
        if (cargas >= maximo) {
            definirParcial(pilha, 0);
            return;
        }
        float parcial = parcial(pilha) + quanto;
        while (parcial >= 1 && cargas < maximo) {
            cargas++;
            parcial--;
        }
        if (cargas >= maximo) {
            parcial = 0;
        }
        definirCargas(pilha, cargas);
        definirParcial(pilha, parcial);
    }

    // Sobe um nível (até o máximo), com a mensagem do artefato
    public void subirNivel(Player jogador, ItemStack pilha) {
        if (nivel(pilha) >= nivelMaximo()) {
            return;
        }
        pilha.set(ModComponentes.NIVEL.get(), nivel(pilha) + 1);
        jogador.displayClientMessage(Component.translatable("mensagem.pixeldungeon." + id() + ".subiu").withStyle(ChatFormatting.GREEN), false);
        jogador.level().playSound(null, jogador.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.6F, 1.4F);
    }

    // O anel da energia: os artefatos recarregam 1,175x mais rápido por bônus (amaldiçoado, mais devagar)
    protected static float energia(ServerPlayer jogador) {
        return (float) Math.pow(1.175, RegrasDoHeroi.dados(jogador).bonusDoAnel(TipoDeAnel.ENERGIA));
    }

    protected static void avisar(Player jogador, String chave) {
        jogador.displayClientMessage(Component.translatable(chave).withStyle(ChatFormatting.GRAY), true);
    }

    // ---- Os artefatos equipados (nas duas mãos) ----

    public static List<ItemStack> equipados(Player jogador) {
        List<ItemStack> lista = new ArrayList<>();
        if (jogador.getMainHandItem().getItem() instanceof ArtefatoAchado) {
            lista.add(jogador.getMainHandItem());
        }
        if (jogador.getOffhandItem().getItem() instanceof ArtefatoAchado) {
            lista.add(jogador.getOffhandItem());
        }
        return lista;
    }

    @Nullable
    public static ItemStack equipado(Player jogador, Class<? extends ArtefatoAchado> tipo) {
        for (ItemStack pilha : equipados(jogador)) {
            if (tipo.isInstance(pilha.getItem())) {
                return pilha;
            }
        }
        return null;
    }

    // ---- Os ganchos chamados pelo herói (pros artefatos equipados) ----

    // Imune à magia (pergaminho antimagia), os artefatos não funcionam nem recarregam
    public static void turnoDoHeroi(ServerPlayer jogador) {
        boolean semMagia = EfeitosDePergaminhoExotico.semMagia(jogador);
        for (ItemStack pilha : equipados(jogador)) {
            if (semMagia) {
                ((ArtefatoAchado) pilha.getItem()).aoTurnoSemMagia(jogador, pilha);
            } else {
                ((ArtefatoAchado) pilha.getItem()).aoTurno(jogador, pilha);
            }
        }
    }

    public static void xpDoHeroi(ServerPlayer jogador, float porcao) {
        if (EfeitosDePergaminhoExotico.semMagia(jogador)) {
            return;
        }
        for (ItemStack pilha : equipados(jogador)) {
            ((ArtefatoAchado) pilha.getItem()).aoGanharXp(jogador, pilha, porcao);
        }
    }

    public static void recargaDeFora(ServerPlayer jogador, float quanto) {
        if (EfeitosDePergaminhoExotico.semMagia(jogador)) {
            return;
        }
        for (ItemStack pilha : equipados(jogador)) {
            ((ArtefatoAchado) pilha.getItem()).recarregarDeFora(jogador, pilha, quanto);
        }
    }

    // ---- Ganchos ----

    // Uma vez por turno, equipado (recarregar com o tempo, efeitos que ficam ligados)
    public void aoTurno(ServerPlayer jogador, ItemStack pilha) {
    }

    // Uma vez por turno, equipado, mas com o herói imune à magia (o artefato fica parado)
    public void aoTurnoSemMagia(ServerPlayer jogador, ItemStack pilha) {
    }

    // O herói ganhou XP (porcao = que parte de um nível)
    public void aoGanharXp(ServerPlayer jogador, ItemStack pilha, float porcao) {
    }

    // Carga que vem de fora (a recarga de artefato da refeição mística do Ladino): no Shattered, cada artefato
    // aproveita de um jeito (quanto = turnos de recarga; a maioria ganha 1/4 de carga por turno)
    public void recarregarDeFora(ServerPlayer jogador, ItemStack pilha, float quanto) {
        carregar(pilha, aproveitamentoDeFora() * quanto);
    }

    protected float aproveitamentoDeFora() {
        return 0.25F;
    }

    // Clique direito. Devolve se fez alguma coisa.
    public abstract boolean usar(ServerPlayer jogador, ItemStack pilha);

    // Agachado e clique direito (a outra ação, se tiver)
    public boolean usarAgachado(ServerPlayer jogador, ItemStack pilha) {
        return usar(jogador, pilha);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level mundo, Player jogador, InteractionHand mao) {
        ItemStack pilha = jogador.getItemInHand(mao);
        if (!(jogador instanceof ServerPlayer heroi)) {
            return InteractionResultHolder.success(pilha);
        }
        if (!GerenciadorDoHeroi.emJogo(heroi)) {
            return InteractionResultHolder.pass(pilha);
        }
        if (EfeitosDePergaminhoExotico.bloqueado(heroi, "item")) {
            return InteractionResultHolder.fail(pilha);
        }
        boolean usou = heroi.isShiftKeyDown() ? usarAgachado(heroi, pilha) : usar(heroi, pilha);
        return usou ? InteractionResultHolder.success(pilha) : InteractionResultHolder.fail(pilha);
    }

    // ---- Aparência ----

    @Override
    public boolean isBarVisible(ItemStack pilha) {
        return cargasMaximas(pilha) > 0;
    }

    @Override
    public int getBarWidth(ItemStack pilha) {
        return Math.round(13.0F * Math.min(cargas(pilha), cargasMaximas(pilha)) / Math.max(1, cargasMaximas(pilha)));
    }

    @Override
    public int getBarColor(ItemStack pilha) {
        return corDaBarra();
    }

    @Override
    public void appendHoverText(ItemStack pilha, TooltipContext contexto, List<Component> linhas, TooltipFlag opcoes) {
        linhas.add(Component.translatable("dica.pixeldungeon.artefato_nivel", nivel(pilha), nivelMaximo()).withStyle(ChatFormatting.GRAY));
        if (cargasMaximas(pilha) > 0) {
            linhas.add(Component.translatable("dica.pixeldungeon.artefato_cargas", Math.min(cargas(pilha), cargasMaximas(pilha)), cargasMaximas(pilha))
                    .withStyle(ChatFormatting.GRAY));
        }
        dicaExtra(pilha, linhas);
        Textos.adicionarQuebrado(linhas, "dica.pixeldungeon." + id() + ".uso", ChatFormatting.AQUA);
        Textos.adicionarQuebrado(linhas, "item.pixeldungeon." + id() + ".descricao", ChatFormatting.DARK_GRAY);
    }

    // Uma linha a mais na dica (o que o artefato guarda, por exemplo)
    protected void dicaExtra(ItemStack pilha, List<Component> linhas) {
    }
}
