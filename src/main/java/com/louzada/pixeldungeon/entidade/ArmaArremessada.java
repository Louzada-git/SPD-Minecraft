package com.louzada.pixeldungeon.entidade;

import com.louzada.pixeldungeon.Turno;
import com.louzada.pixeldungeon.combate.Combate;
import com.louzada.pixeldungeon.combate.Combatente;
import com.louzada.pixeldungeon.combate.Golpe;
import com.louzada.pixeldungeon.combate.Particulas;
import com.louzada.pixeldungeon.efeito.EfeitosNoTick;
import com.louzada.pixeldungeon.heroi.DadosDoHeroi;
import com.louzada.pixeldungeon.heroi.RegrasDoHeroi;
import com.louzada.pixeldungeon.heroi.Talento;
import com.louzada.pixeldungeon.item.ArmaDeArremesso;
import com.louzada.pixeldungeon.item.ArmaDeArremesso.Especial;
import com.louzada.pixeldungeon.item.NivelDoItem;
import com.louzada.pixeldungeon.registro.ModComponentes;
import com.louzada.pixeldungeon.registro.ModEfeitos;
import com.louzada.pixeldungeon.registro.ModEntidades;
import com.louzada.pixeldungeon.registro.ModItens;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

// Uma arma de arremesso voando. Igual ao Shattered:
//   - acertar é um ataque físico do herói (precisão, armadura, talentos, maldição, identificação pelo uso)
//   - cada acerto gasta a durabilidade; quando acaba, ela quebra
//   - se gruda, fica cravada no inimigo e cai quando ele morre; senão (ou se errou) cai no chão ali
//   - o bumerangue volta pra onde foi arremessado depois de 5 turnos; o cubo de força acerta todo mundo em volta
public class ArmaArremessada extends ThrowableItemProjectile {
    // As armas cravadas num inimigo (caem quando ele morre)
    private static final String ESPETADOS = "pixeldungeon_espetados";
    // Se não bater em nada nesse tempo, cai onde está
    private static final int TEMPO_MAXIMO = 200;
    // Cubo de força: acerta quem estiver a até 1,5 quadradinho de onde caiu
    private static final double RAIO_DO_CUBO = 6.0;

    // Bumerangue: de onde foi arremessado e há quanto tempo está voltando
    private Vec3 origem = Vec3.ZERO;
    private boolean voltando = false;
    private int ticksVoltando = 0;

    public ArmaArremessada(EntityType<? extends ArmaArremessada> tipo, Level mundo) {
        super(tipo, mundo);
    }

    public ArmaArremessada(Level mundo, LivingEntity dono, ItemStack pilha) {
        super(ModEntidades.ARMA_ARREMESSADA.get(), dono, mundo);
        setItem(pilha);
        origem = dono.position();
    }

    @Override
    protected Item getDefaultItem() {
        return ModItens.PEDRA_DE_ARREMESSO.get();
    }

    // Quase reto (cai um pouco no caminho)
    @Override
    protected double getDefaultGravity() {
        return 0.01;
    }

    private Especial especial() {
        return getItem().getItem() instanceof ArmaDeArremesso arma ? arma.especial() : Especial.NENHUM;
    }

    // ---- Voo ----

    @Override
    public void tick() {
        if (voltando) {
            voltar();
            return;
        }
        super.tick();
        if (!level().isClientSide() && !isRemoved() && tickCount > TEMPO_MAXIMO) {
            cair(position());
        }
    }

    // O bumerangue volta pra onde foi arremessado em 5 turnos, atravessando as paredes
    private void voltar() {
        xo = getX();
        yo = getY();
        zo = getZ();
        noPhysics = true;
        if (level().isClientSide()) {
            setPos(position().add(getDeltaMovement()));
            return;
        }
        ticksVoltando++;
        int faltam = Turno.ticks(5) - ticksVoltando;
        Vec3 caminho = origem.add(0, 1.0, 0).subtract(position());
        if (faltam <= 0 || caminho.length() < 0.3) {
            chegou();
            return;
        }
        Vec3 passo = caminho.scale(1.0 / faltam);
        setDeltaMovement(passo);
        setPos(position().add(passo));
    }

    // Chegou de volta: se o herói estiver ali (a até 1 quadradinho), vai pra mochila dele; senão cai no chão
    private void chegou() {
        ItemStack pilha = getItem().copy();
        if (getOwner() instanceof Player dono && dono.isAlive() && dono.position().distanceTo(origem) <= 4.0 && dono.getInventory().add(pilha)) {
            level().playSound(null, dono.getX(), dono.getY(), dono.getZ(), SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 0.4F, 1.4F);
            if (dono instanceof ServerPlayer jogador) {
                ArmaDeArremesso.juntarPilhas(jogador);
            }
            discard();
            return;
        }
        cair(origem.add(0, 0.5, 0));
    }

    // Passa pelos outros heróis (sem fogo amigo)
    @Override
    protected boolean canHitEntity(Entity alvo) {
        if (voltando || (alvo instanceof Player && getOwner() instanceof Player)) {
            return false;
        }
        return super.canHitEntity(alvo);
    }

    // ---- Acertos ----

    @Override
    protected void onHitEntity(EntityHitResult resultado) {
        super.onHitEntity(resultado);
        if (level().isClientSide() || isRemoved() || voltando) {
            return;
        }
        if (especial() == Especial.EM_AREA) {
            explodirEmVolta(resultado.getLocation());
            return;
        }
        if (!(resultado.getEntity() instanceof LivingEntity alvo)) {
            cair(resultado.getLocation());
            return;
        }
        boolean acertou = golpear(alvo);
        boolean quebrou = acertou && desgastar();
        terminar(alvo, acertou, quebrou, alvo.position().add(0, 0.3, 0));
    }

    @Override
    protected void onHitBlock(BlockHitResult resultado) {
        super.onHitBlock(resultado);
        if (level().isClientSide() || isRemoved() || voltando) {
            return;
        }
        // Volta um pouquinho, pra não cair dentro da parede
        Vec3 lugar = resultado.getLocation().subtract(getDeltaMovement().normalize().scale(0.3));
        if (especial() == Especial.EM_AREA) {
            explodirEmVolta(lugar);
            return;
        }
        playSound(SoundEvents.ARROW_HIT, 0.5F, 0.8F);
        terminar(null, false, false, lugar);
    }

    // Um ataque físico do herói em quem a arma bateu. Devolve se acertou.
    private boolean golpear(LivingEntity alvo) {
        ItemStack pilha = getItem();
        if (!(getOwner() instanceof ServerPlayer jogador)) {
            alvo.hurt(damageSources().thrown(this, getOwner()), 2);
            return true;
        }
        Golpe golpe = Golpe.de(Combatente.de(jogador), Combatente.de(alvo)).arma(pilha).aDistancia();
        float dano = Combate.resolver(golpe);
        if (dano == Combate.ERROU) {
            return false;
        }
        Combate.aplicar(alvo, dano, damageSources().thrown(this, jogador));
        playSound(SoundEvents.ARROW_HIT, 0.7F, 1.2F);
        DadosDoHeroi dados = RegrasDoHeroi.dados(jogador);
        // Igual ao Shattered: acertar revela se o conjunto é amaldiçoado
        if (!NivelDoItem.maldicaoConhecida(pilha)) {
            pilha.remove(ModComponentes.MALDICAO_CONHECIDA.get());
            if (NivelDoItem.amaldicoado(pilha)) {
                jogador.displayClientMessage(Component.translatable("mensagem.pixeldungeon.arremesso_amaldicoado").withStyle(ChatFormatting.DARK_PURPLE), false);
            }
        }
        // Intuição de sobrevivente +2 (Caçadora): identifica ao acertar
        if (dados.pontos(Talento.INTUICAO_DE_SOBREVIVENTE) >= 2) {
            NivelDoItem.identificar(jogador, pilha);
        }
        int nivel = NivelDoItem.nivel(pilha);
        switch (especial()) {
            // Boleadeiras: aleijado por 5 turnos
            case ALEIJA -> alvo.addEffect(new MobEffectInstance(ModEfeitos.ALEIJADO, Turno.ticks(5)));
            // Tomahawk: sangramento de 3-6 (+meio/+1 por nível)
            case SANGRA -> {
                float minimo = ArmaDeArremesso.sangramentoMinimo(nivel);
                float maximo = ArmaDeArremesso.sangramentoMaximo(nivel);
                EfeitosNoTick.sangrar(alvo, Math.round(minimo + (maximo - minimo) * (random.nextFloat() + random.nextFloat()) / 2.0F));
            }
            default -> {
            }
        }
        return true;
    }

    // Cubo de força: um ataque em cada inimigo em volta de onde caiu (gasta a durabilidade uma vez só)
    private void explodirEmVolta(Vec3 lugar) {
        for (LivingEntity alvo : level().getEntitiesOfClass(LivingEntity.class, new AABB(lugar, lugar).inflate(RAIO_DO_CUBO),
                criatura -> criatura.isAlive() && !(criatura instanceof Player) && !(criatura instanceof ImagemEspelhada))) {
            golpear(alvo);
        }
        if (level() instanceof ServerLevel mundo) {
            Particulas.explosao(mundo, ParticleTypes.CLOUD, lugar.x, lugar.y, lugar.z, 20, 1.2, 0.05);
        }
        level().playSound(null, lugar.x, lugar.y, lugar.z, SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 0.5F, 1.6F);
        boolean quebrou = desgastar();
        terminar(null, false, quebrou, lugar);
    }

    // Gasta a durabilidade do conjunto. Devolve true se ela quebrou.
    private boolean desgastar() {
        if (!(getOwner() instanceof ServerPlayer jogador) || !(getItem().getItem() instanceof ArmaDeArremesso arma)) {
            return false;
        }
        ItemStack pilha = getItem();
        float gasto = arma.gastoPorUso(pilha, RegrasDoHeroi.dados(jogador));
        if (gasto <= 0) {
            return false;
        }
        float sobra = ArmaDeArremesso.durabilidade(pilha) - gasto;
        if (sobra <= 0) {
            jogador.displayClientMessage(Component.translatable("mensagem.pixeldungeon.arremesso_quebrou").withStyle(ChatFormatting.RED), false);
            level().playSound(null, getX(), getY(), getZ(), SoundEvents.ITEM_BREAK, SoundSource.PLAYERS, 0.8F, 1.0F);
            return true;
        }
        ArmaDeArremesso.definirDurabilidade(pilha, sobra);
        if (sobra <= gasto) {
            jogador.displayClientMessage(Component.translatable("mensagem.pixeldungeon.arremesso_vai_quebrar").withStyle(ChatFormatting.YELLOW), false);
        }
        return false;
    }

    // Depois de bater: quebrou, volta (bumerangue), fica cravada no inimigo ou cai no chão
    private void terminar(LivingEntity alvo, boolean acertou, boolean quebrou, Vec3 lugar) {
        if (quebrou) {
            discard();
            return;
        }
        if (especial() == Especial.VOLTA) {
            voltando = true;
            ticksVoltando = 0;
            setDeltaMovement(Vec3.ZERO);
            return;
        }
        boolean gruda = getItem().getItem() instanceof ArmaDeArremesso arma && arma.gruda();
        if (acertou && gruda && alvo != null && alvo.isAlive() && alvo instanceof MobDoShattered) {
            espetar(alvo, getItem().copy());
            discard();
            return;
        }
        cair(lugar);
    }

    // Vira um item no chão (sem sumir com o tempo, igual aos itens da dungeon)
    private void cair(Vec3 lugar) {
        ItemEntity noChao = new ItemEntity(level(), lugar.x, lugar.y, lugar.z, getItem().copy());
        noChao.setDeltaMovement(0, 0.1, 0);
        noChao.setUnlimitedLifetime();
        level().addFreshEntity(noChao);
        discard();
    }

    // ---- Armas cravadas nos inimigos ----

    public static void espetar(LivingEntity alvo, ItemStack pilha) {
        CompoundTag dados = alvo.getPersistentData();
        ListTag lista = dados.getList(ESPETADOS, Tag.TAG_COMPOUND);
        lista.add(pilha.save(alvo.registryAccess()));
        dados.put(ESPETADOS, lista);
    }

    // Quantas armas estão cravadas nele
    public static int espetados(LivingEntity alvo) {
        return alvo.getPersistentData().getList(ESPETADOS, Tag.TAG_COMPOUND).size();
    }

    // O inimigo morreu: as armas cravadas nele caem
    public static void soltarEspetados(LivingEntity morto) {
        CompoundTag dados = morto.getPersistentData();
        if (!dados.contains(ESPETADOS)) {
            return;
        }
        ListTag lista = dados.getList(ESPETADOS, Tag.TAG_COMPOUND);
        for (int i = 0; i < lista.size(); i++) {
            ItemStack.parse(morto.registryAccess(), lista.getCompound(i)).ifPresent(pilha -> {
                ItemEntity noChao = morto.spawnAtLocation(pilha);
                if (noChao != null) {
                    noChao.setUnlimitedLifetime();
                }
            });
        }
        dados.remove(ESPETADOS);
    }
}
