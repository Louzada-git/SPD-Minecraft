package com.louzada.pixeldungeon.dungeon.geracao;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import com.louzada.pixeldungeon.dungeon.PlantaDoAndar;

// Um cômodo do andar (o "Room" do Shattered): um retângulo que inclui as paredes em volta.
// Dois cômodos vizinhos dividem a mesma parede, e uma porta liga os dois por ela.
// Cada tipo de cômodo diz o tamanho que pode ter, quantas portas aceita e como se desenha.
public abstract class Comodo extends Retangulo {
    public static final int TODOS = 0;
    public static final int ESQUERDA = 1;
    public static final int CIMA = 2;
    public static final int DIREITA = 3;
    public static final int BAIXO = 4;

    // Os que encostam neste (dividem um pedaço de parede de pelo menos 2 quadradinhos)
    public List<Comodo> vizinhos = new ArrayList<>();
    // Os que estão ligados a este, e a porta de cada ligação (null até a porta ser posta)
    public LinkedHashMap<Comodo, Porta> ligados = new LinkedHashMap<>();
    // O número da sala na planta (pra juntar salas vizinhas)
    int numeroDaSala = -1;

    // ---- Tamanho ----

    public int minLargura() {
        return -1;
    }

    public int maxLargura() {
        return -1;
    }

    public int minAltura() {
        return -1;
    }

    public int maxAltura() {
        return -1;
    }

    public boolean definirTamanho() {
        return definirTamanho(minLargura(), maxLargura(), minAltura(), maxAltura());
    }

    // Sorteia o tamanho, mas sem passar do espaço que tem
    public boolean definirTamanhoComLimite(int largura, int altura) {
        if (largura < minLargura() || altura < minAltura()) {
            return false;
        }
        definirTamanho();
        if (largura() > largura || altura() > altura) {
            redimensionar(Math.min(largura(), largura) - 1, Math.min(altura(), altura) - 1);
        }
        return true;
    }

    protected boolean definirTamanho(int minL, int maxL, int minA, int maxA) {
        if (minL < minLargura() || maxL > maxLargura() || minA < minAltura() || maxA > maxAltura() || minL > maxL || minA > maxA) {
            return false;
        }
        redimensionar(Dado.normalEntre(minL, maxL) - 1, Dado.normalEntre(minA, maxA) - 1);
        return true;
    }

    // O cômodo conta as duas paredes: um cômodo de largura 5 vai da esquerda até esquerda + 4
    @Override
    public int largura() {
        return super.largura() + 1;
    }

    @Override
    public int altura() {
        return super.altura() + 1;
    }

    // ---- Pontos ----

    // Da porta (na parede), "n" quadradinhos pra dentro
    public Ponto pontoDentro(Ponto de, int n) {
        Ponto passo = new Ponto(de);
        if (de.x == esquerda) {
            passo.deslocar(+n, 0);
        } else if (de.x == direita) {
            passo.deslocar(-n, 0);
        } else if (de.y == cima) {
            passo.deslocar(0, +n);
        } else if (de.y == baixo) {
            passo.deslocar(0, -n);
        }
        return passo;
    }

    public Ponto aleatorio() {
        return aleatorio(1);
    }

    // Um quadradinho sorteado, a pelo menos "margem" das bordas
    public Ponto aleatorio(int margem) {
        return new Ponto(Dado.entre(esquerda + margem, direita - margem), Dado.entre(cima + margem, baixo - margem));
    }

    // Do lado de dentro (sem as paredes)?
    public boolean dentro(Ponto p) {
        return p.x > esquerda && p.y > cima && p.x < direita && p.y < baixo;
    }

    @Override
    public Ponto centro() {
        return new Ponto((esquerda + direita) / 2 + (((direita - esquerda) % 2) == 1 ? Dado.ate(2) : 0),
                (cima + baixo) / 2 + (((baixo - cima) % 2) == 1 ? Dado.ate(2) : 0));
    }

    // ---- Ligações ----

    public int minLigacoes(int lado) {
        return lado == TODOS ? 1 : 0;
    }

    public int maxLigacoes(int lado) {
        return lado == TODOS ? 16 : 4;
    }

    public int ligacoesAtuais(int lado) {
        if (lado == TODOS) {
            return ligados.size();
        }
        int total = 0;
        for (Comodo outro : ligados.keySet()) {
            Retangulo i = intersecao(outro);
            if (lado == ESQUERDA && i.largura() == 0 && i.esquerda == esquerda) {
                total++;
            } else if (lado == CIMA && i.altura() == 0 && i.cima == cima) {
                total++;
            } else if (lado == DIREITA && i.largura() == 0 && i.direita == direita) {
                total++;
            } else if (lado == BAIXO && i.altura() == 0 && i.baixo == baixo) {
                total++;
            }
        }
        return total;
    }

    public int ligacoesRestantes(int lado) {
        if (ligacoesAtuais(TODOS) >= maxLigacoes(TODOS)) {
            return 0;
        }
        return maxLigacoes(lado) - ligacoesAtuais(lado);
    }

    // Dá pra pôr uma porta nesse ponto? (na parede, mas não no canto)
    public boolean podeLigar(Ponto p) {
        return (p.x == esquerda || p.x == direita) != (p.y == cima || p.y == baixo);
    }

    public boolean podeLigar(int lado) {
        return ligacoesRestantes(lado) > 0;
    }

    public boolean podeLigar(Comodo outro) {
        // A entrada e a saída nunca ficam coladas
        if (ehSaida() && outro.ehEntrada() || ehEntrada() && outro.ehSaida()) {
            return false;
        }
        Retangulo i = intersecao(outro);
        boolean achouPonto = false;
        for (Ponto p : i.pontos()) {
            if (podeLigar(p) && outro.podeLigar(p)) {
                achouPonto = true;
                break;
            }
        }
        if (!achouPonto) {
            return false;
        }
        if (i.largura() == 0 && i.esquerda == esquerda) {
            return podeLigar(ESQUERDA) && outro.podeLigar(DIREITA);
        } else if (i.altura() == 0 && i.cima == cima) {
            return podeLigar(CIMA) && outro.podeLigar(BAIXO);
        } else if (i.largura() == 0 && i.direita == direita) {
            return podeLigar(DIREITA) && outro.podeLigar(ESQUERDA);
        } else if (i.altura() == 0 && i.baixo == baixo) {
            return podeLigar(BAIXO) && outro.podeLigar(CIMA);
        }
        return false;
    }

    // Dá pra derrubar a parede entre este e o outro nesse ponto? (só as salas comuns deixam)
    public boolean podeJuntar(PlantaDoAndar planta, Comodo outro, Ponto p) {
        return false;
    }

    public void juntar(PlantaDoAndar planta, Comodo outro, Retangulo pedaco, byte tipo) {
        Pincel.preencher(planta, pedaco, tipo);
    }

    public boolean adicionarVizinho(Comodo outro) {
        if (vizinhos.contains(outro)) {
            return true;
        }
        Retangulo i = intersecao(outro);
        if ((i.largura() == 0 && i.altura() >= 2) || (i.altura() == 0 && i.largura() >= 2)) {
            vizinhos.add(outro);
            outro.vizinhos.add(this);
            return true;
        }
        return false;
    }

    public boolean ligar(Comodo outro) {
        if ((vizinhos.contains(outro) || adicionarVizinho(outro)) && !ligados.containsKey(outro) && podeLigar(outro)) {
            ligados.put(outro, null);
            outro.ligados.put(this, null);
            return true;
        }
        return false;
    }

    public void limparLigacoes() {
        for (Comodo vizinho : vizinhos) {
            vizinho.vizinhos.remove(this);
        }
        vizinhos.clear();
        for (Comodo ligado : ligados.keySet()) {
            ligado.ligados.remove(this);
        }
        ligados.clear();
    }

    public boolean ehEntrada() {
        return false;
    }

    public boolean ehSaida() {
        return false;
    }

    // ---- Desenho ----

    public abstract void pintar(PlantaDoAndar planta);

    public boolean podeTerAgua(Ponto p) {
        return true;
    }

    public boolean podeTerGrama(Ponto p) {
        return true;
    }

    // Dá pra sortear uma armadilha nesse quadradinho? (o "canPlaceTrap" do Shattered)
    public boolean podePorArmadilha(Ponto p) {
        return true;
    }

    // Os quadradinhos (inclusive as paredes) onde a água, a grama e as armadilhas do andar podem cair
    public List<Ponto> pontosComAgua() {
        List<Ponto> pontos = new ArrayList<>();
        for (Ponto p : pontos()) {
            if (podeTerAgua(p)) {
                pontos.add(p);
            }
        }
        return pontos;
    }

    public List<Ponto> pontosComGrama() {
        List<Ponto> pontos = new ArrayList<>();
        for (Ponto p : pontos()) {
            if (podeTerGrama(p)) {
                pontos.add(p);
            }
        }
        return pontos;
    }

    public List<Ponto> pontosComArmadilha() {
        List<Ponto> pontos = new ArrayList<>();
        for (Ponto p : pontos()) {
            if (podePorArmadilha(p)) {
                pontos.add(p);
            }
        }
        return pontos;
    }

    // ---- Portas ----

    // A porta de uma ligação. O tipo só sobe (uma sala pede porta comum, a outra pede trancada: fica trancada).
    public static class Porta extends Ponto {
        public enum Tipo {
            // Nada (só chão), túnel, água, porta comum (vira destrancada ou escondida), destrancada, escondida, trancada, parede
            VAZIA, TUNEL, AGUA, COMUM, DESTRANCADA, ESCONDIDA, TRANCADA, PAREDE
        }

        public Tipo tipo = Tipo.VAZIA;

        public Porta(Ponto p) {
            super(p);
        }

        public void definir(Tipo novo) {
            if (novo.compareTo(tipo) > 0) {
                tipo = novo;
            }
        }
    }
}
