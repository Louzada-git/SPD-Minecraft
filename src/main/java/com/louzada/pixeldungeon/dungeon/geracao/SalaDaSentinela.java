package com.louzada.pixeldungeon.dungeon.geracao;

import com.louzada.pixeldungeon.dungeon.PlantaDoAndar;
import com.louzada.pixeldungeon.dungeon.PlantaDoAndar.Premio;

// A sala da sentinela (o "SentryRoom" do Shattered): no fundo, num pedestal, uma sentinela que não se mexe nem morre. Quem pisa
// no chão especial da sala, à vista dela, leva raios. Há um caminho seguro (chão comum) junto à parede da porta, e o baú fica
// no pedestal do outro lado do chão especial; uma fileira de estátuas corta a sala. O andar ganha uma poção de rapidez.
// O prêmio (ver SaqueDoAndar): metade das vezes, um dos itens que o andar ia ter; senão, igual ao da piscina.
public class SalaDaSentinela extends ComodoEspecial {
    private Ponto sentinela;
    private float espera;

    @Override
    public int minLargura() {
        return 7;
    }

    @Override
    public int minAltura() {
        return 7;
    }

    @Override
    public void pintar(PlantaDoAndar planta) {
        Pincel.preencher(planta, this, PlantaDoAndar.PAREDE);
        Pincel.preencher(planta, this, 1, PlantaDoAndar.CHAO_ESPECIAL);
        Porta porta = entrada();
        Ponto c;
        do {
            c = centro();
        } while (c.x == porta.x || c.y == porta.y);

        Ponto tesouro = new Ponto();
        sentinela = new Ponto();
        // O tamanho do caminho perigoso, da porta até o tesouro e de volta
        int perigo = 0;
        if (porta.x == esquerda) {
            sentinela.definir(direita - 1, c.y);
            Pincel.preencher(planta, esquerda + 1, cima + 1, 1, altura() - 2, PlantaDoAndar.CHAO);
            if (porta.y > c.y) {
                tesouro.definir(esquerda + 1, (cima + 1 + c.y) / 2);
                Pincel.preencher(planta, esquerda + 1, cima + 1, 2, c.y - cima - 1, PlantaDoAndar.CHAO);
            } else {
                tesouro.definir(esquerda + 1, (baixo + c.y) / 2);
                Pincel.preencher(planta, esquerda + 1, c.y + 1, 2, baixo - c.y - 1, PlantaDoAndar.CHAO);
            }
            for (int x = direita - 3; x > esquerda; x--) {
                Pincel.definir(planta, x, c.y, PlantaDoAndar.ESTATUA);
            }
            perigo = 2 * (largura() - 5);
        } else if (porta.x == direita) {
            sentinela.definir(esquerda + 1, c.y);
            Pincel.preencher(planta, direita - 1, cima + 1, 1, altura() - 2, PlantaDoAndar.CHAO);
            if (porta.y > c.y) {
                tesouro.definir(direita - 1, (cima + 1 + c.y) / 2);
                Pincel.preencher(planta, direita - 2, cima + 1, 2, c.y - cima - 1, PlantaDoAndar.CHAO);
            } else {
                tesouro.definir(direita - 1, (baixo + 1 + c.y) / 2);
                Pincel.preencher(planta, direita - 2, c.y + 1, 2, baixo - c.y - 1, PlantaDoAndar.CHAO);
            }
            for (int x = esquerda + 3; x < direita; x++) {
                Pincel.definir(planta, x, c.y, PlantaDoAndar.ESTATUA);
            }
            perigo = 2 * (largura() - 5);
        } else if (porta.y == cima) {
            sentinela.definir(c.x, baixo - 1);
            Pincel.preencher(planta, esquerda + 1, cima + 1, largura() - 2, 1, PlantaDoAndar.CHAO);
            if (porta.x > c.x) {
                tesouro.definir((esquerda + 1 + c.x) / 2, cima + 1);
                Pincel.preencher(planta, esquerda + 1, cima + 1, c.x - esquerda - 1, 2, PlantaDoAndar.CHAO);
            } else {
                tesouro.definir((direita + c.x) / 2, cima + 1);
                Pincel.preencher(planta, c.x + 1, cima + 1, direita - c.x - 1, 2, PlantaDoAndar.CHAO);
            }
            for (int y = baixo - 3; y > cima; y--) {
                Pincel.definir(planta, c.x, y, PlantaDoAndar.ESTATUA);
            }
            perigo = 2 * (altura() - 5);
        } else if (porta.y == baixo) {
            sentinela.definir(c.x, cima + 1);
            Pincel.preencher(planta, esquerda + 1, baixo - 1, largura() - 2, 1, PlantaDoAndar.CHAO);
            if (porta.x > c.x) {
                tesouro.definir((esquerda + 1 + c.x) / 2, baixo - 1);
                Pincel.preencher(planta, esquerda + 1, baixo - 2, c.x - esquerda - 1, 2, PlantaDoAndar.CHAO);
            } else {
                tesouro.definir((direita + c.x) / 2, baixo - 1);
                Pincel.preencher(planta, c.x + 1, baixo - 2, direita - c.x - 1, 2, PlantaDoAndar.CHAO);
            }
            for (int y = cima + 3; y < baixo; y++) {
                Pincel.definir(planta, c.x, y, PlantaDoAndar.ESTATUA);
            }
            perigo = 2 * (altura() - 5);
        }
        Pincel.definir(planta, sentinela, PlantaDoAndar.PEDESTAL);
        espera = perigo / 3f + 0.1f;
        Pincel.definir(planta, tesouro, PlantaDoAndar.PEDESTAL);
        planta.adicionarPremio(tesouro.x, tesouro.y, Premio.EQUIPAMENTO_DA_SENTINELA, true);
        planta.pedir(PlantaDoAndar.ItemPedido.RAPIDEZ);
        porta.definir(Porta.Tipo.COMUM);
    }

    // A sentinela vigia o lado de dentro da sala
    @Override
    public void registrada(PlantaDoAndar planta, PlantaDoAndar.Sala sala) {
        planta.adicionarInimigo(sentinela.x, sentinela.y, PlantaDoAndar.InimigoDaSala.SENTINELA, sala, espera);
    }

    // A porta não fica bem no meio da parede
    @Override
    public boolean podeLigar(Ponto p) {
        if (!super.podeLigar(p)) {
            return false;
        }
        if (largura() % 2 == 1 && p.x == (esquerda + direita) / 2) {
            return false;
        }
        return !(altura() % 2 == 1 && p.y == (cima + baixo) / 2);
    }
}
