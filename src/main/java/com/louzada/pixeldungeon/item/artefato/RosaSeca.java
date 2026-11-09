package com.louzada.pixeldungeon.item.artefato;

import java.util.ArrayList;
import java.util.List;

import com.louzada.pixeldungeon.combate.Particulas;
import com.louzada.pixeldungeon.dungeon.MissaoDoFantasma;
import com.louzada.pixeldungeon.entidade.FantasmaAliado;
import com.louzada.pixeldungeon.entidade.MobDoShattered;
import com.louzada.pixeldungeon.heroi.DadosDoHeroi;
import com.louzada.pixeldungeon.heroi.Furtividade;
import com.louzada.pixeldungeon.heroi.RegrasDoHeroi;
import com.louzada.pixeldungeon.item.ArmaDoShattered;
import com.louzada.pixeldungeon.item.ArmaduraDoShattered;
import com.louzada.pixeldungeon.item.ArtefatoAchado;
import com.louzada.pixeldungeon.item.CajadoDoMago;
import com.louzada.pixeldungeon.item.EscolhaDeItem;
import com.louzada.pixeldungeon.item.Equipamento;
import com.louzada.pixeldungeon.item.NivelDoItem;
import com.louzada.pixeldungeon.registro.ModEntidades;
import com.louzada.pixeldungeon.run.DadosDaRun;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

// Rosa seca (o "DriedRose" do Shattered): só funciona depois de completar a missão do fantasma triste.
// Vem cheia (100); sem o fantasma, recarrega em 500 turnos. Sobe de nível (até o 10) com as pétalas secas.
//   chamar (clique direito, carga cheia): o fantasma aparece do lado do herói e luta junto (ver FantasmaAliado).
//     Enquanto ele vive, a rosa não carrega: cura o fantasma (a vida toda em 500 turnos).
//   dar ordens (clique direito, com o fantasma por perto): mirando num inimigo, ele ataca; no chão, guarda o lugar;
//     no nada (no céu, longe), volta a seguir o herói
//   equipar (agachado): dar uma arma ou uma armadura pro fantasma (que ele tenha força pra usar e que você saiba
//     que não é amaldiçoada); escolher a própria rosa devolve o que ele tem
public class RosaSeca extends ArtefatoAchado {
    private static final double ALCANCE = 48;
    private static final String CURA = "rosa_cura";

    public RosaSeca(Properties propriedades) {
        super(propriedades);
    }

    @Override
    public String id() {
        return "rosa_seca";
    }

    @Override
    public int nivelMaximo() {
        return 10;
    }

    @Override
    public int cargasMaximas(ItemStack pilha) {
        return 100;
    }

    @Override
    protected int cargasIniciais(ItemStack pilha) {
        return 100;
    }

    @Override
    protected int corDaBarra() {
        return 0xCC6688;
    }

    // O desenho da rosa: mais cheia no +4 e no +9
    @Override
    public float estagio(ItemStack pilha) {
        int nivel = nivel(pilha);
        return nivel >= 9 ? 2 : nivel >= 4 ? 1 : 0;
    }

    public static int forcaDoFantasma(ItemStack pilha) {
        return 13 + nivel(pilha) / 2;
    }

    // ---- O que o fantasma leva (guardado na rosa: a arma e a armadura) ----

    private static NonNullList<ItemStack> equipamento(ItemStack rosa) {
        NonNullList<ItemStack> lista = NonNullList.withSize(2, ItemStack.EMPTY);
        rosa.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY).copyInto(lista);
        return lista;
    }

    public static ItemStack arma(ItemStack rosa) {
        return equipamento(rosa).get(0);
    }

    public static ItemStack armadura(ItemStack rosa) {
        return equipamento(rosa).get(1);
    }

    private static void guardarEquipamento(ItemStack rosa, ItemStack arma, ItemStack armadura) {
        rosa.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(List.of(arma, armadura)));
    }

    // ---- Todo turno ----

    @Override
    public void aoTurno(ServerPlayer jogador, ItemStack pilha) {
        FantasmaAliado fantasma = FantasmaAliado.doDono(jogador);
        if (fantasma == null) {
            carregar(pilha, energia(jogador) / 5F);
            return;
        }
        // Com o fantasma vivo, a rosa cura ele: a vida toda em 500 turnos
        if (fantasma.getHealth() < fantasma.getMaxHealth()) {
            DadosDoHeroi dados = RegrasDoHeroi.dados(jogador);
            float cura = dados.decimal(CURA) + fantasma.getMaxHealth() / 500F * energia(jogador);
            if (cura >= 1) {
                fantasma.curarPelaRosa((int) cura);
                cura -= (int) cura;
            }
            dados.definir(CURA, cura);
        }
    }

    // Recarga de fora: sem o fantasma, 4 de carga por turno; com ele, cura
    @Override
    public void recarregarDeFora(ServerPlayer jogador, ItemStack pilha, float quanto) {
        FantasmaAliado fantasma = FantasmaAliado.doDono(jogador);
        if (fantasma == null) {
            carregar(pilha, 4 * quanto);
        } else {
            fantasma.curarPelaRosa(Math.round((1 + nivel(pilha) / 3F) * quanto));
        }
    }

    // ---- Chamar e dar ordens ----

    @Override
    public boolean usar(ServerPlayer jogador, ItemStack pilha) {
        if (!MissaoDoFantasma.completa(DadosDaRun.pegar(jogador.serverLevel().getServer()))) {
            avisar(jogador, "mensagem.pixeldungeon.rosa_seca.sem_missao");
            return false;
        }
        FantasmaAliado fantasma = FantasmaAliado.doDono(jogador);
        if (fantasma != null) {
            return dirigir(jogador, fantasma);
        }
        if (cargas(pilha) < cargasMaximas(pilha)) {
            avisar(jogador, "mensagem.pixeldungeon.rosa_seca.sem_carga");
            return false;
        }
        return chamar(jogador, pilha) != null;
    }

    // O fantasma aparece do lado do herói (a carga vai toda)
    public FantasmaAliado chamar(ServerPlayer jogador, ItemStack pilha) {
        ServerLevel mundo = jogador.serverLevel();
        FantasmaAliado fantasma = ModEntidades.FANTASMA_ALIADO.get().create(mundo);
        if (fantasma == null) {
            return null;
        }
        Vec3 lado = jogador.getLookAngle().multiply(1, 0, 1).normalize().scale(1.5);
        Vec3 lugar = jogador.position().add(lado);
        if (!mundo.noCollision(fantasma, fantasma.getDimensions(fantasma.getPose()).makeBoundingBox(lugar))) {
            lugar = jogador.position();
        }
        fantasma.moveTo(lugar.x, lugar.y, lugar.z, jogador.getYRot(), 0.0F);
        fantasma.setPersistenceRequired();
        fantasma.receber(jogador, pilha);
        fantasma.setHealth(fantasma.getMaxHealth());
        mundo.addFreshEntity(fantasma);
        definirCargas(pilha, 0);
        definirParcial(pilha, 0);
        Furtividade.revelar(jogador);
        Particulas.subindoEmVolta(fantasma, ParticleTypes.SOUL, 16, 0.05);
        mundo.playSound(null, fantasma.blockPosition(), SoundEvents.ALLAY_AMBIENT_WITH_ITEM, SoundSource.PLAYERS, 1.0F, 0.7F);
        DadosDoHeroi dados = RegrasDoHeroi.dados(jogador);
        if (!dados.sim("rosa_ja_chamou")) {
            dados.definir("rosa_ja_chamou", true);
            fantasma.falar("fantasma_aliado.pixeldungeon.ola", Component.translatable(dados.classe().chaveDoNome()));
        } else {
            fantasma.falar("fantasma_aliado.pixeldungeon.esgoto_" + (1 + jogador.getRandom().nextInt(6)));
        }
        return fantasma;
    }

    // Mirando: num inimigo, ataca; no chão, guarda o lugar; no nada, segue
    public boolean dirigir(ServerPlayer jogador, FantasmaAliado fantasma) {
        ServerLevel mundo = jogador.serverLevel();
        Vec3 olho = jogador.getEyePosition();
        Vec3 fim = olho.add(jogador.getLookAngle().scale(ALCANCE));
        BlockHitResult bloco = mundo.clip(new ClipContext(olho, fim, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, jogador));
        Vec3 ate = bloco.getType() == HitResult.Type.MISS ? fim : bloco.getLocation();
        EntityHitResult criatura = ProjectileUtil.getEntityHitResult(mundo, jogador, olho, ate, new AABB(olho, ate).inflate(1.0),
                entidade -> entidade instanceof MobDoShattered inimigo && inimigo.isAlive() && !inimigo.aliado(), 0.3F);
        if (criatura != null) {
            fantasma.atacar(criatura.getEntity());
        } else if (bloco.getType() == HitResult.Type.BLOCK) {
            BlockPos lugar = bloco.getBlockPos().relative(bloco.getDirection());
            fantasma.guardar(lugar);
        } else {
            fantasma.seguir();
        }
        mundo.playSound(null, jogador.blockPosition(), SoundEvents.ALLAY_AMBIENT_WITHOUT_ITEM, SoundSource.PLAYERS, 0.6F, 0.8F);
        return true;
    }

    // ---- Equipar o fantasma ----

    // Serve pro fantasma: arma de perto ou armadura, que não seja única (o cajado do Mago), que se saiba que não é
    // amaldiçoada e que ele tenha força pra usar. A própria rosa também serve: é como devolver o que ele tem.
    public static boolean serve(ItemStack rosa, ItemStack pilha) {
        if (pilha.getItem() instanceof RosaSeca) {
            return !arma(rosa).isEmpty() || !armadura(rosa).isEmpty();
        }
        boolean tipoCerto = (pilha.getItem() instanceof ArmaDoShattered && !(pilha.getItem() instanceof CajadoDoMago))
                || pilha.getItem() instanceof ArmaduraDoShattered;
        if (!tipoCerto || !(pilha.getItem() instanceof Equipamento equipamento)) {
            return false;
        }
        return NivelDoItem.maldicaoConhecida(pilha) && !NivelDoItem.amaldicoado(pilha)
                && equipamento.forcaNecessaria(pilha) <= forcaDoFantasma(rosa);
    }

    @Override
    public boolean usarAgachado(ServerPlayer jogador, ItemStack pilha) {
        if (!MissaoDoFantasma.completa(DadosDaRun.pegar(jogador.serverLevel().getServer()))) {
            avisar(jogador, "mensagem.pixeldungeon.rosa_seca.sem_missao");
            return false;
        }
        boolean abriu = EscolhaDeItem.pedirProArtefato(jogador, id(), outra -> serve(pilha, outra), (heroi, espaco) -> {
            ItemStack rosa = ArtefatoAchado.equipado(heroi, RosaSeca.class);
            if (rosa != null) {
                equipar(heroi, rosa, heroi.getInventory().getItem(espaco));
            }
        });
        if (!abriu) {
            jogador.displayClientMessage(Component.translatable("mensagem.pixeldungeon.rosa_seca.nada_pra_dar", forcaDoFantasma(pilha))
                    .withStyle(ChatFormatting.GRAY), true);
        }
        return abriu;
    }

    // Dá a arma ou a armadura pro fantasma (o que ele tinha volta pra mochila); a rosa devolve tudo
    public void equipar(ServerPlayer jogador, ItemStack rosa, ItemStack item) {
        if (!serve(rosa, item)) {
            return;
        }
        ItemStack arma = arma(rosa);
        ItemStack armadura = armadura(rosa);
        List<ItemStack> devolver = new ArrayList<>();
        if (item.getItem() instanceof RosaSeca) {
            devolver.add(arma);
            devolver.add(armadura);
            arma = ItemStack.EMPTY;
            armadura = ItemStack.EMPTY;
        } else if (item.getItem() instanceof ArmaDoShattered) {
            devolver.add(arma);
            arma = item.split(1);
        } else {
            devolver.add(armadura);
            armadura = item.split(1);
        }
        guardarEquipamento(rosa, arma, armadura);
        for (ItemStack volta : devolver) {
            if (!volta.isEmpty() && !jogador.getInventory().add(volta)) {
                jogador.drop(volta, false);
            }
        }
        avisar(jogador, "mensagem.pixeldungeon.rosa_seca.equipou");
        jogador.level().playSound(null, jogador.blockPosition(), SoundEvents.ARMOR_EQUIP_GENERIC.value(), SoundSource.PLAYERS, 1.0F, 1.0F);
    }

    @Override
    protected void dicaExtra(ItemStack pilha, List<Component> linhas) {
        ItemStack arma = arma(pilha);
        ItemStack armadura = armadura(pilha);
        if (!arma.isEmpty()) {
            linhas.add(Component.translatable("dica.pixeldungeon.rosa_seca.arma", arma.getHoverName()).withStyle(ChatFormatting.GRAY));
        }
        if (!armadura.isEmpty()) {
            linhas.add(Component.translatable("dica.pixeldungeon.rosa_seca.armadura", armadura.getHoverName()).withStyle(ChatFormatting.GRAY));
        }
        linhas.add(Component.translatable("dica.pixeldungeon.rosa_seca.forca", forcaDoFantasma(pilha)).withStyle(ChatFormatting.GRAY));
    }
}
