package com.louzada.pixeldungeon.dungeon.geracao;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;

// Quem decide onde cada cômodo fica (o "Builder" + "RegularBuilder" do Shattered).
// Ele não desenha nada: só posiciona os retângulos encostados uns nos outros e diz quem liga com quem.
//   - um caminho principal (com a entrada e a saída) forma uma volta
//   - as outras salas saem em galhos da volta, às vezes com túneis no meio
//   - no fim, vizinhos que ainda não se ligam ganham uma ligação extra (30% de chance)
public abstract class Arquiteto {
    private static final double GRAUS = 180 / Math.PI;

    // Quanto das salas vai no caminho principal, e quantos túneis entre elas
    protected float tamanhoDoCaminho = 0.25f;
    protected float[] variacaoDoCaminho = { 0, 0, 0, 1 };
    protected float[] tuneisNoCaminho = { 2, 2, 1 };
    protected float[] tuneisNosGalhos = { 1, 1, 0 };
    protected float chanceDeLigacaoExtra = 0.30f;

    // A forma da volta (o "setLoopShape" do Shattered): o expoente e a intensidade da curva, e onde ela começa
    protected int expoenteDaCurva = 0;
    protected float intensidadeDaCurva = 1;
    protected float deslocamentoDaCurva = 0;

    protected Comodo entrada;
    protected Comodo saida;
    protected List<Comodo> caminhoPrincipal = new ArrayList<>();
    protected List<Comodo> variasLigacoes = new ArrayList<>();
    protected List<Comodo> umaLigacao = new ArrayList<>();

    // Devolve os cômodos posicionados e ligados, ou null se não deu (aí tenta de novo)
    public abstract List<Comodo> construir(List<Comodo> comodos);

    // ---- O caminho principal ----

    protected void separarComodos(List<Comodo> comodos) {
        for (Comodo comodo : comodos) {
            comodo.esvaziar();
        }
        entrada = saida = null;
        caminhoPrincipal.clear();
        umaLigacao.clear();
        variasLigacoes.clear();
        for (Comodo comodo : comodos) {
            if (comodo.ehEntrada()) {
                entrada = comodo;
            } else if (comodo.ehSaida()) {
                saida = comodo;
            } else if (comodo.maxLigacoes(Comodo.TODOS) > 1) {
                variasLigacoes.add(comodo);
            } else if (comodo.maxLigacoes(Comodo.TODOS) == 1) {
                umaLigacao.add(comodo);
            }
        }
        // As salas maiores têm mais chance de ir pro caminho principal
        pesarComodos(variasLigacoes);
        Dado.embaralhar(variasLigacoes);
        variasLigacoes = new ArrayList<>(new LinkedHashSet<>(variasLigacoes));
        Dado.embaralhar(variasLigacoes);

        int noCaminho = (int) (variasLigacoes.size() * tamanhoDoCaminho) + Dado.chances(variacaoDoCaminho);
        while (noCaminho > 0 && !variasLigacoes.isEmpty()) {
            Comodo comodo = variasLigacoes.remove(0);
            noCaminho -= comodo instanceof ComodoComum comum ? comum.valor() : 1;
            caminhoPrincipal.add(comodo);
        }
    }

    // Cada sala comum aparece na lista tantas vezes quanto o peso dela
    protected void pesarComodos(List<Comodo> comodos) {
        for (Comodo comodo : new ArrayList<>(comodos)) {
            if (comodo instanceof ComodoComum comum) {
                for (int i = 1; i < comum.pesoDasLigacoes(); i++) {
                    comodos.add(comodo);
                }
            }
        }
    }

    // O caminho de uma volta, com túneis entre as salas
    protected List<Comodo> comTuneis(List<Comodo> salas, float[][] tuneis) {
        List<Comodo> volta = new ArrayList<>();
        for (Comodo sala : salas) {
            volta.add(sala);
            int quantos = Dado.chances(tuneis[0]);
            if (quantos == -1) {
                tuneis[0] = tuneisNoCaminho.clone();
                quantos = Dado.chances(tuneis[0]);
            }
            tuneis[0][quantos]--;
            for (int j = 0; j < quantos; j++) {
                volta.add(new ComodoDeLigacao());
            }
        }
        return volta;
    }

    public Arquiteto formaDaVolta(int expoente, float intensidade, float deslocamento) {
        expoenteDaCurva = Math.abs(expoente);
        intensidadeDaCurva = intensidade % 1f;
        deslocamentoDaCurva = deslocamento % 0.5f;
        return this;
    }

    // O ângulo em que a sala fica, pela fração do caminho já andada (a curva deixa a volta mais "quadrada")
    protected float anguloNaVolta(float fracao) {
        fracao += deslocamentoDaCurva;
        return 360f * (float) (intensidadeDaCurva * curva(fracao) + (1 - intensidadeDaCurva) * fracao - deslocamentoDaCurva);
    }

    private double curva(double x) {
        return Math.pow(4, 2 * expoenteDaCurva) * (Math.pow((x % 0.5f) - 0.25, 2 * expoenteDaCurva + 1)) + 0.25 + 0.5 * Math.floor(2 * x);
    }

    // Põe as salas da volta uma depois da outra, e fecha a volta com túneis até a primeira
    protected boolean fazerVolta(List<Comodo> comodos, List<Comodo> volta, float anguloInicial) {
        Comodo primeira = volta.get(0);
        Comodo anterior = primeira;
        for (int i = 1; i < volta.size(); i++) {
            Comodo comodo = volta.get(i);
            float alvo = anguloInicial + anguloNaVolta(i / (float) volta.size());
            if (posicionar(comodos, anterior, comodo, alvo) == -1) {
                return false;
            }
            anterior = comodo;
            if (!comodos.contains(anterior)) {
                comodos.add(anterior);
            }
        }
        while (!anterior.ligar(primeira)) {
            ComodoDeLigacao tunel = new ComodoDeLigacao();
            if (posicionar(comodos, anterior, tunel, anguloEntre(anterior, primeira)) == -1) {
                return false;
            }
            volta.add(tunel);
            comodos.add(tunel);
            anterior = tunel;
        }
        return true;
    }

    protected static float[] centroDe(List<Comodo> volta) {
        float x = 0;
        float y = 0;
        for (Comodo comodo : volta) {
            x += (comodo.esquerda + comodo.direita) / 2f;
            y += (comodo.cima + comodo.baixo) / 2f;
        }
        return new float[] { x / volta.size(), y / volta.size() };
    }

    // ---- Os galhos ----

    protected boolean criarGalhos(List<Comodo> comodos, List<Comodo> ondeCrescer, List<Comodo> paraPendurar, float[] chancesDeTuneis) {
        int i = 0;
        int falhas = 0;
        float[] chances = chancesDeTuneis.clone();
        List<Comodo> tuneisDesteGalho = new ArrayList<>();
        while (i < paraPendurar.size()) {
            if (falhas > 100) {
                return false;
            }
            Comodo comodo = paraPendurar.get(i);
            tuneisDesteGalho.clear();
            Comodo atual = Dado.elemento(ondeCrescer);

            int tuneis = Dado.chances(chances);
            if (tuneis == -1) {
                chances = chancesDeTuneis.clone();
                tuneis = Dado.chances(chances);
            }
            chances[tuneis]--;

            for (int j = 0; j < tuneis; j++) {
                ComodoDeLigacao tunel = new ComodoDeLigacao();
                float angulo;
                int tentativas = 3;
                do {
                    angulo = posicionar(comodos, atual, tunel, anguloDoGalho(atual));
                    tentativas--;
                } while (angulo == -1 && tentativas > 0);
                if (angulo == -1) {
                    tunel.limparLigacoes();
                    for (Comodo t : tuneisDesteGalho) {
                        t.limparLigacoes();
                        comodos.remove(t);
                    }
                    tuneisDesteGalho.clear();
                    break;
                }
                tuneisDesteGalho.add(tunel);
                comodos.add(tunel);
                atual = tunel;
            }
            if (tuneisDesteGalho.size() != tuneis) {
                falhas++;
                continue;
            }

            float angulo;
            int tentativas = 10;
            do {
                angulo = posicionar(comodos, atual, comodo, anguloDoGalho(atual));
                tentativas--;
            } while (angulo == -1 && tentativas > 0);
            if (angulo == -1) {
                comodo.limparLigacoes();
                for (Comodo t : tuneisDesteGalho) {
                    t.limparLigacoes();
                    comodos.remove(t);
                }
                tuneisDesteGalho.clear();
                falhas++;
                continue;
            }

            // Os túneis e (às vezes) a sala nova também podem ganhar galhos
            for (Comodo t : tuneisDesteGalho) {
                if (Dado.ate(3) <= 1) {
                    ondeCrescer.add(t);
                }
            }
            if (comodo.maxLigacoes(Comodo.TODOS) > 1 && Dado.ate(3) == 0) {
                int peso = comodo instanceof ComodoComum comum ? comum.pesoDasLigacoes() : 1;
                for (int j = 0; j < peso; j++) {
                    ondeCrescer.add(comodo);
                }
            }
            i++;
        }
        return true;
    }

    protected float anguloDoGalho(Comodo comodo) {
        return Dado.decimal(360f);
    }

    // Um ângulo sorteado puxado pro centro (de 5 sorteios, fica o mais perto do centro)
    protected static float anguloPertoDoCentro(Comodo comodo, float centroX, float centroY) {
        float paraOCentro = anguloEntrePontos((comodo.esquerda + comodo.direita) / 2f, (comodo.cima + comodo.baixo) / 2f, centroX, centroY);
        if (paraOCentro < 0) {
            paraOCentro += 360f;
        }
        float atual = Dado.decimal(360f);
        for (int i = 0; i < 4; i++) {
            float novo = Dado.decimal(360f);
            if (Math.abs(paraOCentro - novo) < Math.abs(paraOCentro - atual)) {
                atual = novo;
            }
        }
        return atual;
    }

    // No fim: acha todos os vizinhos e liga alguns que ainda não se ligam
    protected void ligacoesExtras(List<Comodo> comodos) {
        acharVizinhos(comodos);
        for (Comodo comodo : comodos) {
            for (Comodo vizinho : comodo.vizinhos) {
                if (!vizinho.ligados.containsKey(comodo) && Dado.decimal() < chanceDeLigacaoExtra) {
                    comodo.ligar(vizinho);
                }
            }
        }
    }

    // ---- Geometria ----

    protected static void acharVizinhos(List<Comodo> comodos) {
        for (int i = 0; i < comodos.size() - 1; i++) {
            for (int j = i + 1; j < comodos.size(); j++) {
                comodos.get(i).adicionarVizinho(comodos.get(j));
            }
        }
    }

    // O maior retângulo livre em volta de um ponto (sem entrar em nenhum cômodo já posto)
    protected static Retangulo acharEspacoLivre(Ponto inicio, List<Comodo> colisao, int tamanhoMaximo) {
        Retangulo espaco = new Retangulo(inicio.x - tamanhoMaximo, inicio.y - tamanhoMaximo, inicio.x + tamanhoMaximo, inicio.y + tamanhoMaximo);
        List<Comodo> batendo = new ArrayList<>(colisao);
        do {
            // Tira os vazios e os que não estão dentro do espaço
            Iterator<Comodo> it = batendo.iterator();
            while (it.hasNext()) {
                Comodo comodo = it.next();
                if (comodo.vazio() || Math.max(espaco.esquerda, comodo.esquerda) >= Math.min(espaco.direita, comodo.direita)
                        || Math.max(espaco.cima, comodo.cima) >= Math.min(espaco.baixo, comodo.baixo)) {
                    it.remove();
                }
            }

            // O mais perto do ponto
            Comodo maisPerto = null;
            float menorDistancia = Integer.MAX_VALUE;
            Ponto diferenca = new Ponto();
            for (Comodo comodo : batendo) {
                diferenca.definir(0, 0);
                boolean dentro = true;
                if (inicio.x <= comodo.esquerda) {
                    dentro = false;
                    diferenca.x = comodo.esquerda - inicio.x;
                } else if (inicio.x >= comodo.direita) {
                    dentro = false;
                    diferenca.x = inicio.x - comodo.direita;
                }
                if (inicio.y <= comodo.cima) {
                    dentro = false;
                    diferenca.y = comodo.cima - inicio.y;
                } else if (inicio.y >= comodo.baixo) {
                    dentro = false;
                    diferenca.y = inicio.y - comodo.baixo;
                }
                if (dentro) {
                    espaco.definir(inicio.x, inicio.y, inicio.x, inicio.y);
                    return espaco;
                }
                if (diferenca.comprimento() < menorDistancia) {
                    menorDistancia = diferenca.comprimento();
                    maisPerto = comodo;
                }
            }

            if (maisPerto != null) {
                // Encolhe o espaço o mínimo possível pra não bater nesse cômodo
                int perdaNaLargura = Integer.MAX_VALUE;
                if (maisPerto.esquerda >= inicio.x) {
                    perdaNaLargura = (espaco.direita - maisPerto.esquerda) * (espaco.altura() + 1);
                } else if (maisPerto.direita <= inicio.x) {
                    perdaNaLargura = (maisPerto.direita - espaco.esquerda) * (espaco.altura() + 1);
                }
                int perdaNaAltura = Integer.MAX_VALUE;
                if (maisPerto.cima >= inicio.y) {
                    perdaNaAltura = (espaco.baixo - maisPerto.cima) * (espaco.largura() + 1);
                } else if (maisPerto.baixo <= inicio.y) {
                    perdaNaAltura = (maisPerto.baixo - espaco.cima) * (espaco.largura() + 1);
                }
                if (perdaNaLargura < perdaNaAltura || perdaNaLargura == perdaNaAltura && Dado.ate(2) == 0) {
                    if (maisPerto.esquerda >= inicio.x && maisPerto.esquerda < espaco.direita) {
                        espaco.direita = maisPerto.esquerda;
                    }
                    if (maisPerto.direita <= inicio.x && maisPerto.direita > espaco.esquerda) {
                        espaco.esquerda = maisPerto.direita;
                    }
                } else {
                    if (maisPerto.cima >= inicio.y && maisPerto.cima < espaco.baixo) {
                        espaco.baixo = maisPerto.cima;
                    }
                    if (maisPerto.baixo <= inicio.y && maisPerto.baixo > espaco.cima) {
                        espaco.cima = maisPerto.baixo;
                    }
                }
                batendo.remove(maisPerto);
            } else {
                batendo.clear();
            }
        } while (!batendo.isEmpty());
        return espaco;
    }

    // O ângulo (em graus, 0 = pra cima) entre os centros de dois cômodos
    protected static float anguloEntre(Comodo de, Comodo para) {
        return anguloEntrePontos((de.esquerda + de.direita) / 2f, (de.cima + de.baixo) / 2f, (para.esquerda + para.direita) / 2f,
                (para.cima + para.baixo) / 2f);
    }

    protected static float anguloEntrePontos(float deX, float deY, float paraX, float paraY) {
        double m = (paraY - deY) / (paraX - deX);
        float angulo = (float) (GRAUS * (Math.atan(m) + Math.PI / 2.0));
        if (deX > paraX) {
            angulo -= 180f;
        }
        return angulo;
    }

    // Tenta pôr o próximo cômodo encostado no anterior, na direção do ângulo pedido.
    // Devolve o ângulo de verdade entre os dois, ou -1 se não coube.
    protected static float posicionar(List<Comodo> colisao, Comodo anterior, Comodo proximo, float angulo) {
        angulo %= 360f;
        if (angulo < 0) {
            angulo += 360f;
        }
        float centroX = (anterior.esquerda + anterior.direita) / 2f;
        float centroY = (anterior.cima + anterior.baixo) / 2f;

        // A reta y = m*x + b que sai do centro do anterior nesse ângulo
        double m = Math.tan(angulo / GRAUS + Math.PI / 2.0);
        double b = centroY - m * centroX;

        // Onde a reta cruza a parede do anterior
        Ponto inicio;
        int lado;
        if (Math.abs(m) >= 1) {
            if (angulo < 90 || angulo > 270) {
                lado = Comodo.CIMA;
                inicio = new Ponto((int) Math.round((anterior.cima - b) / m), anterior.cima);
            } else {
                lado = Comodo.BAIXO;
                inicio = new Ponto((int) Math.round((anterior.baixo - b) / m), anterior.baixo);
            }
        } else {
            if (angulo < 180) {
                lado = Comodo.DIREITA;
                inicio = new Ponto(anterior.direita, (int) Math.round(m * anterior.direita + b));
            } else {
                lado = Comodo.ESQUERDA;
                inicio = new Ponto(anterior.esquerda, (int) Math.round(m * anterior.esquerda + b));
            }
        }
        if (lado == Comodo.CIMA || lado == Comodo.BAIXO) {
            inicio.x = Math.max(anterior.esquerda + 1, Math.min(inicio.x, anterior.direita - 1));
        } else {
            inicio.y = Math.max(anterior.cima + 1, Math.min(inicio.y, anterior.baixo - 1));
        }

        // Quanto espaço tem ali
        Retangulo espaco = acharEspacoLivre(inicio, colisao, Math.max(proximo.maxLargura(), proximo.maxAltura()));
        if (!proximo.definirTamanhoComLimite(espaco.largura() + 1, espaco.altura() + 1)) {
            return -1;
        }

        // O centro ideal do próximo, pela reta
        if (lado == Comodo.CIMA) {
            float alvoY = anterior.cima - (proximo.altura() - 1) / 2f;
            float alvoX = (float) ((alvoY - b) / m);
            proximo.posicionar(Math.round(alvoX - (proximo.largura() - 1) / 2f), anterior.cima - (proximo.altura() - 1));
        } else if (lado == Comodo.BAIXO) {
            float alvoY = anterior.baixo + (proximo.altura() - 1) / 2f;
            float alvoX = (float) ((alvoY - b) / m);
            proximo.posicionar(Math.round(alvoX - (proximo.largura() - 1) / 2f), anterior.baixo);
        } else if (lado == Comodo.DIREITA) {
            float alvoX = anterior.direita + (proximo.largura() - 1) / 2f;
            float alvoY = (float) (m * alvoX + b);
            proximo.posicionar(anterior.direita, Math.round(alvoY - (proximo.altura() - 1) / 2f));
        } else {
            float alvoX = anterior.esquerda - (proximo.largura() - 1) / 2f;
            float alvoY = (float) (m * alvoX + b);
            proximo.posicionar(anterior.esquerda - (proximo.largura() - 1), Math.round(alvoY - (proximo.altura() - 1) / 2f));
        }

        // Ajeita pra encostar o suficiente pra ter porta e não sair do espaço livre
        if (lado == Comodo.CIMA || lado == Comodo.BAIXO) {
            if (proximo.direita < anterior.esquerda + 2) {
                proximo.deslocar(anterior.esquerda + 2 - proximo.direita, 0);
            } else if (proximo.esquerda > anterior.direita - 2) {
                proximo.deslocar(anterior.direita - 2 - proximo.esquerda, 0);
            }
            if (proximo.direita > espaco.direita) {
                proximo.deslocar(espaco.direita - proximo.direita, 0);
            } else if (proximo.esquerda < espaco.esquerda) {
                proximo.deslocar(espaco.esquerda - proximo.esquerda, 0);
            }
        } else {
            if (proximo.baixo < anterior.cima + 2) {
                proximo.deslocar(0, anterior.cima + 2 - proximo.baixo);
            } else if (proximo.cima > anterior.baixo - 2) {
                proximo.deslocar(0, anterior.baixo - 2 - proximo.cima);
            }
            if (proximo.baixo > espaco.baixo) {
                proximo.deslocar(0, espaco.baixo - proximo.baixo);
            } else if (proximo.cima < espaco.cima) {
                proximo.deslocar(0, espaco.cima - proximo.cima);
            }
        }

        if (proximo.ligar(anterior)) {
            return anguloEntre(anterior, proximo);
        }
        return -1;
    }
}
