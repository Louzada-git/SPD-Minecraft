package com.louzada.pixeldungeon.item.artefato;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import com.louzada.pixeldungeon.dungeon.Andares;
import com.louzada.pixeldungeon.dungeon.PlantasDoShattered;
import com.louzada.pixeldungeon.heroi.Furtividade;
import com.louzada.pixeldungeon.item.ArtefatoAchado;
import com.louzada.pixeldungeon.item.EscolhaDeItem;
import com.louzada.pixeldungeon.item.ItemSemente;
import com.louzada.pixeldungeon.item.TipoDePlanta;
import com.louzada.pixeldungeon.registro.ModBlocos;
import com.louzada.pixeldungeon.registro.ModComponentes;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

// Sandálias da natureza (o "SandalsOfNature" do Shattered): carregam (até 100) quando o herói pisoteia grama alta,
// (3 + 2 x nível)/6 por quadradinho; e com elas a grama solta mais sementes e orvalho.
//   alimentar (agachado): as sandálias comem uma semente da mochila e passam a usar o efeito dela. Cada tipo de semente
//     só conta uma vez por nível; com 3 + 3 x nível tipos diferentes, sobem de nível (até o 3: sapatos, botas e grevas)
//   enraizar (clique direito, mirando até 3 quadradinhos): ativa ali o efeito da última semente que comeram,
//     com quem estiver no lugar. Custa de 12 a 80 de carga, conforme a planta (igual ao Shattered).
public class SandaliasDaNatureza extends ArtefatoAchado {
    private static final int ALCANCE = 3;
    private static final int T = Andares.TAMANHO_DO_QUADRADINHO;
    private static final Map<TipoDePlanta, Integer> CUSTOS = new EnumMap<>(TipoDePlanta.class);

    static {
        CUSTOS.put(TipoDePlanta.FLOR_DE_FOGO, 20);
        CUSTOS.put(TipoDePlanta.CARDO_LIGEIRO, 20);
        CUSTOS.put(TipoDePlanta.GRAMA_SOLAR, 80);
        CUSTOS.put(TipoDePlanta.MUDA_DE_GELO, 20);
        CUSTOS.put(TipoDePlanta.VINHA_TEMPESTUOSA, 20);
        CUSTOS.put(TipoDePlanta.SOFRIMUSGO, 20);
        CUSTOS.put(TipoDePlanta.MAGI_REAL, 12);
        CUSTOS.put(TipoDePlanta.TERRAIZ, 40);
        CUSTOS.put(TipoDePlanta.FLOR_ESTRELA, 40);
        CUSTOS.put(TipoDePlanta.FOLHA_FANTASMA, 12);
        CUSTOS.put(TipoDePlanta.CEGUERVA, 12);
    }

    public SandaliasDaNatureza(Properties propriedades) {
        super(propriedades);
    }

    @Override
    public String id() {
        return "sandalias_da_natureza";
    }

    @Override
    public int nivelMaximo() {
        return 3;
    }

    @Override
    public int cargasMaximas(ItemStack pilha) {
        return 100;
    }

    @Override
    protected int corDaBarra() {
        return 0x55CC55;
    }

    @Override
    protected float aproveitamentoDeFora() {
        return 2F;
    }

    // Sandálias, sapatos, botas e grevas
    @Override
    public float estagio(ItemStack pilha) {
        return nivel(pilha);
    }

    @Override
    public Component getName(ItemStack pilha) {
        int nivel = nivel(pilha);
        return nivel == 0 ? super.getName(pilha) : Component.translatable("item.pixeldungeon.sandalias_da_natureza.nome_" + nivel);
    }

    public static int custo(TipoDePlanta tipo) {
        return CUSTOS.getOrDefault(tipo, 20);
    }

    // ---- O que as sandálias guardam: "efeito atual;sementes deste nível" ----

    @Nullable
    public static TipoDePlanta efeito(ItemStack pilha) {
        String guardado = pilha.getOrDefault(ModComponentes.GUARDADO.get(), "");
        int corte = guardado.indexOf(';');
        return TipoDePlanta.peloId(corte < 0 ? guardado : guardado.substring(0, corte));
    }

    public static List<TipoDePlanta> sementes(ItemStack pilha) {
        List<TipoDePlanta> lista = new ArrayList<>();
        String guardado = pilha.getOrDefault(ModComponentes.GUARDADO.get(), "");
        int corte = guardado.indexOf(';');
        if (corte >= 0) {
            for (String id : guardado.substring(corte + 1).split(",")) {
                TipoDePlanta tipo = TipoDePlanta.peloId(id);
                if (tipo != null) {
                    lista.add(tipo);
                }
            }
        }
        return lista;
    }

    private static void guardar(ItemStack pilha, @Nullable TipoDePlanta efeito, List<TipoDePlanta> sementes) {
        List<String> ids = new ArrayList<>();
        for (TipoDePlanta tipo : sementes) {
            ids.add(tipo.id());
        }
        pilha.set(ModComponentes.GUARDADO.get(), (efeito == null ? "" : efeito.id()) + ";" + String.join(",", ids));
    }

    // ---- Carregar pisoteando a grama ----

    public void aoPisotear(ServerPlayer jogador, ItemStack pilha) {
        carregar(pilha, (3F + 2F * nivel(pilha)) / 6F * energia(jogador));
    }

    // ---- Alimentar ----

    public boolean aceita(ItemStack pilha, ItemStack semente) {
        if (!(semente.getItem() instanceof ItemSemente item)) {
            return false;
        }
        return !sementes(pilha).contains(item.planta()) && (nivel(pilha) < nivelMaximo() || efeito(pilha) != item.planta());
    }

    @Override
    public boolean usarAgachado(ServerPlayer jogador, ItemStack pilha) {
        boolean abriu = EscolhaDeItem.pedirProArtefato(jogador, id(), semente -> aceita(pilha, semente), (heroi, espaco) -> {
            ItemStack sandalias = ArtefatoAchado.equipado(heroi, SandaliasDaNatureza.class);
            if (sandalias != null) {
                alimentar(heroi, sandalias, heroi.getInventory().getItem(espaco));
            }
        });
        if (!abriu) {
            avisar(jogador, "mensagem.pixeldungeon.sandalias_da_natureza.sem_semente");
        }
        return abriu;
    }

    public void alimentar(ServerPlayer jogador, ItemStack pilha, ItemStack semente) {
        if (!aceita(pilha, semente)) {
            return;
        }
        TipoDePlanta tipo = ((ItemSemente) semente.getItem()).planta();
        semente.shrink(1);
        List<TipoDePlanta> lista = sementes(pilha);
        if (nivel(pilha) < nivelMaximo()) {
            lista.add(0, tipo);
        }
        jogador.level().playSound(null, jogador.blockPosition(), SoundEvents.CROP_PLANTED, SoundSource.PLAYERS, 1.0F, 1.0F);
        if (nivel(pilha) < nivelMaximo() && lista.size() >= 3 + 3 * nivel(pilha)) {
            lista.clear();
            guardar(pilha, tipo, lista);
            subirNivel(jogador, pilha);
        } else {
            guardar(pilha, tipo, lista);
            avisar(jogador, "mensagem.pixeldungeon.sandalias_da_natureza.comeu");
        }
    }

    // ---- Enraizar ----

    @Override
    public boolean usar(ServerPlayer jogador, ItemStack pilha) {
        TipoDePlanta tipo = efeito(pilha);
        if (tipo == null) {
            avisar(jogador, "mensagem.pixeldungeon.sandalias_da_natureza.sem_efeito");
            return false;
        }
        if (cargas(pilha) < custo(tipo)) {
            avisar(jogador, "mensagem.pixeldungeon.sandalias_da_natureza.pouca_carga");
            return false;
        }
        ServerLevel mundo = jogador.serverLevel();
        Vec3 olho = jogador.getEyePosition();
        Vec3 fim = olho.add(jogador.getLookAngle().scale((ALCANCE + 1) * T));
        BlockHitResult bloco = mundo.clip(new ClipContext(olho, fim, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, jogador));
        Vec3 ate = bloco.getType() == HitResult.Type.MISS ? fim : bloco.getLocation();
        EntityHitResult criatura = ProjectileUtil.getEntityHitResult(mundo, jogador, olho, ate, new AABB(olho, ate).inflate(1.0),
                entidade -> entidade instanceof LivingEntity viva && viva.isAlive() && !entidade.isSpectator(), 0.3F);
        BlockPos lugar;
        if (criatura != null) {
            lugar = criatura.getEntity().blockPosition();
        } else if (bloco.getType() == HitResult.Type.BLOCK && bloco.getDirection() == Direction.UP) {
            lugar = bloco.getBlockPos().above();
        } else {
            avisar(jogador, "mensagem.pixeldungeon.sandalias_da_natureza.longe");
            return false;
        }
        return enraizar(jogador, pilha, lugar);
    }

    // Ativa o efeito da semente no lugar (com quem estiver no quadradinho)
    public boolean enraizar(ServerPlayer jogador, ItemStack pilha, BlockPos lugar) {
        TipoDePlanta tipo = efeito(pilha);
        if (tipo == null || cargas(pilha) < custo(tipo)) {
            return false;
        }
        int distancia = Math.max(Math.abs(Math.floorDiv(lugar.getX(), T) - Math.floorDiv(jogador.getBlockX(), T)),
                Math.abs(Math.floorDiv(lugar.getZ(), T) - Math.floorDiv(jogador.getBlockZ(), T)));
        if (distancia > ALCANCE) {
            avisar(jogador, "mensagem.pixeldungeon.sandalias_da_natureza.longe");
            return false;
        }
        ServerLevel mundo = jogador.serverLevel();
        LivingEntity quem = quemEstaNoQuadradinho(mundo, lugar);
        // A planta nasce e se ativa na hora: o capim alto ali sai, e o que mais tinha no chão (o musgo) fica
        BlockState antes = mundo.getBlockState(lugar);
        if (antes.is(ModBlocos.CAPIM_ALTO.get())) {
            mundo.removeBlock(lugar.above(), false);
            antes = mundo.getBlockState(lugar);
        }
        PlantasDoShattered.ativar(mundo, lugar, tipo, quem);
        if (!antes.isAir() && !antes.is(ModBlocos.CAPIM_ALTO.get()) && mundo.getBlockState(lugar).isAir()) {
            mundo.setBlock(lugar, antes, 3);
        }
        definirCargas(pilha, cargas(pilha) - custo(tipo));
        Furtividade.revelar(jogador);
        mundo.playSound(null, lugar, SoundEvents.CROP_PLANTED, SoundSource.BLOCKS, 1.0F, 0.8F);
        return true;
    }

    // Quem está no quadradinho do lugar (o mais perto do meio dele)
    @Nullable
    private static LivingEntity quemEstaNoQuadradinho(ServerLevel mundo, BlockPos lugar) {
        int x0 = Math.floorDiv(lugar.getX(), T) * T;
        int z0 = Math.floorDiv(lugar.getZ(), T) * T;
        AABB caixa = new AABB(x0, lugar.getY() - 1, z0, x0 + T, lugar.getY() + 3, z0 + T);
        Vec3 meio = new Vec3(x0 + T / 2.0, lugar.getY(), z0 + T / 2.0);
        LivingEntity melhor = null;
        for (LivingEntity viva : mundo.getEntitiesOfClass(LivingEntity.class, caixa, outra -> outra.isAlive() && !outra.isSpectator())) {
            if (melhor == null || viva.position().distanceToSqr(meio) < melhor.position().distanceToSqr(meio)) {
                melhor = viva;
            }
        }
        return melhor;
    }

    @Override
    protected void dicaExtra(ItemStack pilha, List<Component> linhas) {
        TipoDePlanta tipo = efeito(pilha);
        if (tipo != null) {
            linhas.add(Component.translatable("dica.pixeldungeon.sandalias_da_natureza.efeito",
                    Component.translatable("item.pixeldungeon." + tipo.nomeDaSemente()), custo(tipo)).withStyle(ChatFormatting.GREEN));
        }
        if (nivel(pilha) < nivelMaximo()) {
            linhas.add(Component.translatable("dica.pixeldungeon.sandalias_da_natureza.sementes", sementes(pilha).size(), 3 + 3 * nivel(pilha))
                    .withStyle(ChatFormatting.GRAY));
        }
    }
}
