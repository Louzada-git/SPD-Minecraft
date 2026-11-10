package com.louzada.pixeldungeon.alquimia;

import com.louzada.pixeldungeon.combate.NumerosFlutuantes;
import com.louzada.pixeldungeon.heroi.DadosDoHeroi;
import com.louzada.pixeldungeon.heroi.RegrasDoHeroi;
import com.louzada.pixeldungeon.rede.RedeDoHeroi;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

// A energia alquímica do Shattered: cada herói tem a sua (zera numa run nova). Ela vem dos cristais de energia
// e de energizar itens no pote de alquimia, e é gasta nas receitas que pedem energia.
// O total aparece no inventário, do lado do ouro (em azul), e na tela da alquimia.
public class EnergiaAlquimica {
    // Onde fica a energia do herói (no estado do DadosDoHeroi)
    public static final String ENERGIA = "energia";
    public static final String AZUL = "#44ccff";

    public static int energiaDe(DadosDoHeroi dados) {
        return dados.inteiro(ENERGIA);
    }

    public static int energiaDe(ServerPlayer jogador) {
        return energiaDe(RegrasDoHeroi.dados(jogador));
    }

    // Ganhou energia (o "+N" azul sobe em cima do herói)
    public static void ganhar(ServerPlayer jogador, int quanto) {
        if (quanto <= 0) {
            return;
        }
        DadosDoHeroi dados = RegrasDoHeroi.dados(jogador);
        dados.definir(ENERGIA, energiaDe(dados) + quanto);
        NumerosFlutuantes.mostrar(jogador, "+" + quanto, AZUL);
        jogador.level().playSound(null, jogador.getX(), jogador.getY(), jogador.getZ(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.0F,
                1.2F + jogador.getRandom().nextFloat() * 0.2F);
        RedeDoHeroi.enviarEstado(jogador, false);
    }

    // Gasta a energia, se tiver (devolve false se não tinha o bastante)
    public static boolean gastar(ServerPlayer jogador, int quanto) {
        DadosDoHeroi dados = RegrasDoHeroi.dados(jogador);
        if (energiaDe(dados) < quanto) {
            return false;
        }
        if (quanto > 0) {
            dados.definir(ENERGIA, energiaDe(dados) - quanto);
            RedeDoHeroi.enviarEstado(jogador, false);
        }
        return true;
    }
}
