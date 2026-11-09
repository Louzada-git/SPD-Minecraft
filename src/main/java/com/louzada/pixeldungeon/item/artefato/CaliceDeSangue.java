package com.louzada.pixeldungeon.item.artefato;

import com.louzada.pixeldungeon.combate.Particulas;
import com.louzada.pixeldungeon.combate.Sorteio;
import com.louzada.pixeldungeon.heroi.ArmaduraDePedra;
import com.louzada.pixeldungeon.heroi.DadosDoHeroi;
import com.louzada.pixeldungeon.heroi.RegrasDoHeroi;
import com.louzada.pixeldungeon.item.ArmaduraDoShattered;
import com.louzada.pixeldungeon.item.ArtefatoAchado;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

// Cálice de sangue (o "ChaliceOfBlood" do Shattered): não tem cargas.
//   furar (clique direito, duas vezes pra confirmar): o herói se fere, ceil(3 + 2,5 x nível²) a floor(7 + 3,5 x nível²)
//     (a armadura e a armadura de pedra seguram uma parte, mas pelo menos 1), e o cálice sobe um nível (até o 10)
//   equipado: o herói se cura mais rápido. No Shattered, a cura natural é 1 de vida a cada 10 turnos, e o cálice
//     encurta isso pra 10 - (1,33 + 0,667 x nível) turnos (15% mais rápido no +0, 5x no +10); aqui o cálice dá essa diferença
//     (o Minecraft já cura sozinho com a fome cheia). Morrendo de fome, não cura.
public class CaliceDeSangue extends ArtefatoAchado {
    private static final String CONFIRMAR = "calice_confirmar";
    private static final String CURA = "calice_cura";

    public CaliceDeSangue(Properties propriedades) {
        super(propriedades);
    }

    @Override
    public String id() {
        return "calice_de_sangue";
    }

    @Override
    public int nivelMaximo() {
        return 10;
    }

    @Override
    public int cargasMaximas(ItemStack pilha) {
        return 0;
    }

    // O desenho: 1 até o +1, 2 do +2 ao +5, 3 do +6 em diante
    @Override
    public float estagio(ItemStack pilha) {
        int nivel = nivel(pilha);
        return nivel >= 6 ? 2 : nivel >= 2 ? 1 : 0;
    }

    public static int danoMinimo(int nivel) {
        return (int) Math.ceil(3 + 2.5F * nivel * nivel);
    }

    public static int danoMaximo(int nivel) {
        return (int) Math.floor(7 + 3.5F * nivel * nivel);
    }

    @Override
    public boolean usar(ServerPlayer jogador, ItemStack pilha) {
        int nivel = nivel(pilha);
        if (nivel >= nivelMaximo()) {
            avisar(jogador, "mensagem.pixeldungeon.calice_de_sangue.cheio");
            return false;
        }
        DadosDoHeroi dados = RegrasDoHeroi.dados(jogador);
        long agora = jogador.level().getGameTime();
        if (!dados.ativo(CONFIRMAR, agora)) {
            // Igual ao Shattered, avisa antes (e diz se pode matar)
            dados.ativarPor(CONFIRMAR, agora, 60);
            boolean podeMatar = jogador.getHealth() + jogador.getAbsorptionAmount() <= danoMaximo(nivel);
            jogador.displayClientMessage(Component.translatable(podeMatar ? "mensagem.pixeldungeon.calice_de_sangue.confirmar_perigo"
                    : "mensagem.pixeldungeon.calice_de_sangue.confirmar", danoMinimo(nivel), danoMaximo(nivel))
                    .withStyle(podeMatar ? ChatFormatting.RED : ChatFormatting.GOLD), false);
            return true;
        }
        dados.desativar(CONFIRMAR);
        furar(jogador, pilha);
        return true;
    }

    public void furar(ServerPlayer jogador, ItemStack pilha) {
        int nivel = nivel(pilha);
        float dano = Sorteio.normal(jogador.getRandom(), danoMinimo(nivel), danoMaximo(nivel));
        dano = ArmaduraDePedra.segurar(jogador, dano);
        ItemStack peito = jogador.getItemBySlot(EquipmentSlot.CHEST);
        if (peito.getItem() instanceof ArmaduraDoShattered armadura) {
            dano -= armadura.sortearDefesa(peito, jogador.getRandom(), RegrasDoHeroi.dados(jogador).forca());
        }
        dano = Math.max(1, Math.round(dano));
        Particulas.subindoEmVolta(jogador, Particulas.poeira(0x660000, 1.2F), 4 + (int) dano / 10, 0.05);
        jogador.level().playSound(null, jogador.blockPosition(), SoundEvents.WITHER_HURT, SoundSource.PLAYERS, 0.5F, 1.4F);
        jogador.invulnerableTime = 0;
        jogador.hurt(jogador.damageSources().magic(), dano);
        if (jogador.isAlive()) {
            subirNivel(jogador, pilha);
        }
    }

    // Recarga de fora: igual ao Shattered, cura na hora o que curaria em 5 turnos
    @Override
    public void recarregarDeFora(ServerPlayer jogador, ItemStack pilha, float quanto) {
        if (jogador.getFoodData().getFoodLevel() <= 0 || jogador.getHealth() >= jogador.getMaxHealth()) {
            return;
        }
        float atraso = (10F - (1.33F + nivel(pilha) * 0.667F)) / quanto;
        float cura = 5F / atraso;
        if (jogador.getRandom().nextFloat() < cura % 1) {
            cura++;
        }
        if (cura >= 1) {
            jogador.heal((int) cura);
        }
    }

    // Equipado: a cura a mais
    @Override
    public void aoTurno(ServerPlayer jogador, ItemStack pilha) {
        if (jogador.getHealth() >= jogador.getMaxHealth() || jogador.getFoodData().getFoodLevel() <= 0) {
            return;
        }
        DadosDoHeroi dados = RegrasDoHeroi.dados(jogador);
        float atraso = 10F - (1.33F + nivel(pilha) * 0.667F);
        float cura = dados.decimal(CURA) + (1F / atraso - 1F / 10F);
        if (cura >= 1) {
            jogador.heal((int) cura);
            cura -= (int) cura;
        }
        dados.definir(CURA, cura);
    }
}
