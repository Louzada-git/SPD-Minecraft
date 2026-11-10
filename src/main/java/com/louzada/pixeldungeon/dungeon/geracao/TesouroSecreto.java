package com.louzada.pixeldungeon.dungeon.geracao;

import com.louzada.pixeldungeon.dungeon.PlantaDoAndar;
import com.louzada.pixeldungeon.dungeon.PlantaDoAndar.Premio;
import com.louzada.pixeldungeon.dungeon.TipoDeArmadilha;

// O tesouro secreto (o "SecretHoardRoom" do Shattered): ouro espalhado em metade do chão da sala
// (no total, mais ou menos o mesmo que 8 pilhas de ouro), e metade do chão com armadilhas à vista
// (de desmoronamento ou de dardos envenenados), até embaixo do ouro. As armadilhas comuns do andar não caem aqui.
public class TesouroSecreto extends ComodoSecreto {

    @Override
    public void pintar(PlantaDoAndar planta) {
        Pincel.preencher(planta, this, PlantaDoAndar.PAREDE);
        Pincel.preencher(planta, this, 1, PlantaDoAndar.CHAO);
        TipoDeArmadilha armadilha = Dado.ate(2) == 0 ? TipoDeArmadilha.DESMORONAMENTO : TipoDeArmadilha.DARDOS_VENENOSOS;
        int pilhas = ((largura() - 2) * (altura() - 2)) / 2;
        for (int i = 0; i < pilhas; i++) {
            Ponto lugar = lugarLivre(planta, PlantaDoAndar.CHAO);
            planta.adicionarPremio(lugar.x, lugar.y, Premio.OURO_DO_TESOURO, false);
        }
        for (Ponto p : pontos()) {
            if (Dado.ate(2) == 0 && planta.tipo(p.x, p.y) == PlantaDoAndar.CHAO) {
                planta.colocarArmadilha(p.x, p.y, armadilha, true);
            }
        }
        esconder(planta);
    }

    @Override
    public boolean podePorArmadilha(Ponto p) {
        return false;
    }
}
