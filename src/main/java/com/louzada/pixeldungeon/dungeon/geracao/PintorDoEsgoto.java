package com.louzada.pixeldungeon.dungeon.geracao;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import com.louzada.pixeldungeon.dungeon.PlantaDoAndar.Sala;
import com.louzada.pixeldungeon.dungeon.PlantaDoAndar;
import com.louzada.pixeldungeon.dungeon.TemaDoAndar;
import com.louzada.pixeldungeon.dungeon.TipoDeArmadilha;
import com.louzada.pixeldungeon.item.Acessorios;

// Desenha os cômodos já posicionados na planta (o "RegularPainter" + "SewerPainter" do Shattered):
//   1. a planta fica do tamanho dos cômodos, com 1 quadradinho de parede em volta (no andar abismal, 2 de abismo
//      e a parede na beirada: fora dos cômodos é tudo abismo, igual ao Shattered)
//   2. cada ligação ganha uma porta num ponto sorteado da parede em comum, e cada cômodo se desenha
//   3. salas comuns vizinhas com uma parede comprida entre elas às vezes viram uma sala só (a parede some)
//   4. as portas: comum vira porta gigante, trancada vira porta trancada, túnel e água viram chão e água.
//      Igual ao Shattered, as das salas secretas ficam escondidas, e as comuns também podem ficar (andar / 20 de chance,
//      a partir do andar 2), desde que dê pra chegar nas duas salas sem passar por porta escondida.
//      No andar com segredos, a chance fica no meio do caminho até 50%, e basta que nenhum dos dois lados fique isolado.
//   5. manchas de água (30%) e de grama (20%), igual ao esgoto
//   6. as armadilhas: de 2 a 3 por andar (só a de dardos no andar 1), em chão vazio de qualquer cômodo
public class PintorDoEsgoto {
    private static final float AGUA = 0.30f;
    private static final int AGRUPAMENTO_DA_AGUA = 5;
    private static final float GRAMA = 0.20f;
    private static final int AGRUPAMENTO_DA_GRAMA = 4;

    // Devolve a planta pronta, ou null se ela passar do tamanho máximo (aí o andar é gerado de novo)
    public static PlantaDoAndar pintar(List<Comodo> comodos, int tamanhoMaximo, int andar) {
        // 1. Tamanho
        int esquerda = Integer.MAX_VALUE;
        int cima = Integer.MAX_VALUE;
        for (Comodo comodo : comodos) {
            esquerda = Math.min(esquerda, comodo.esquerda);
            cima = Math.min(cima, comodo.cima);
        }
        boolean abismal = TemaDoAndar.atual().tema() == TemaDoAndar.Tema.ABISMOS;
        int margem = abismal ? 2 : 1;
        esquerda -= margem;
        cima -= margem;
        int direita = 0;
        int baixo = 0;
        for (Comodo comodo : comodos) {
            comodo.deslocar(-esquerda, -cima);
            direita = Math.max(direita, comodo.direita);
            baixo = Math.max(baixo, comodo.baixo);
        }
        int largura = direita + margem + 1;
        int altura = baixo + margem + 1;
        if (largura > tamanhoMaximo || altura > tamanhoMaximo) {
            return null;
        }
        PlantaDoAndar planta = new PlantaDoAndar(largura, altura);
        if (abismal) {
            planta.preencherTudo(PlantaDoAndar.ABISMO);
            for (int x = 0; x < largura; x++) {
                planta.definir(x, 0, PlantaDoAndar.PAREDE);
                planta.definir(x, altura - 1, PlantaDoAndar.PAREDE);
            }
            for (int y = 0; y < altura; y++) {
                planta.definir(0, y, PlantaDoAndar.PAREDE);
                planta.definir(largura - 1, y, PlantaDoAndar.PAREDE);
            }
        }

        // 2. Portas e desenho de cada cômodo
        List<Comodo> embaralhados = new ArrayList<>(comodos);
        Dado.embaralhar(embaralhados);
        for (Comodo comodo : embaralhados) {
            if (comodo.ligados.isEmpty()) {
                return null;
            }
            porPortas(comodo);
            comodo.pintar(planta);
        }

        // As salas (o lado de dentro de tudo que não é túnel)
        for (Comodo comodo : embaralhados) {
            if (!(comodo instanceof ComodoDeLigacao)) {
                Sala sala = new Sala(comodo.esquerda + 1, comodo.cima + 1, comodo.largura() - 2, comodo.altura() - 2,
                        comodo instanceof ComodoEspecial);
                comodo.numeroDaSala = planta.registrarSala(sala);
                comodo.registrada(planta, sala);
            }
        }

        // 3 e 4. Juntar salas e desenhar as portas
        pintarPortas(planta, embaralhados, andar);

        // 5 e 6. Água, grama e armadilhas (com um sorteador separado, igual ao Shattered)
        Random sorteioDaGeracao = Dado.atual();
        Dado.usar(new Random(Dado.numeroGrande()));
        pintarAgua(planta, embaralhados);
        pintarGrama(planta, embaralhados);
        pintarArmadilhas(planta, embaralhados, andar);
        Dado.usar(sorteioDaGeracao);
        return planta;
    }

    // Cada ligação sem porta ganha uma num ponto da parede em comum que os dois aceitem
    private static void porPortas(Comodo comodo) {
        for (Comodo outro : comodo.ligados.keySet()) {
            if (comodo.ligados.get(outro) != null) {
                continue;
            }
            Retangulo i = comodo.intersecao(outro);
            List<Ponto> lugares = new ArrayList<>();
            for (Ponto p : i.pontos()) {
                if (comodo.podeLigar(p) && outro.podeLigar(p)) {
                    lugares.add(p);
                }
            }
            if (lugares.isEmpty()) {
                continue;
            }
            Comodo.Porta porta = new Comodo.Porta(Dado.elemento(lugares));
            comodo.ligados.put(outro, porta);
            outro.ligados.put(comodo, porta);
        }
    }

    private static void pintarPortas(PlantaDoAndar planta, List<Comodo> comodos, int andar) {
        // A chance de uma porta comum ficar escondida: 2/20 no andar 2 até 20/20 no andar 20
        // (no andar com segredos, puxada pra perto de 50%)
        float chanceEscondida = andar > 1 ? Math.min(1f, andar / 20f) : 0f;
        boolean segredos = TemaDoAndar.atual().tema() == TemaDoAndar.Tema.SEGREDOS;
        if (segredos) {
            chanceEscondida = (0.5f + chanceEscondida) / 2f;
        }
        Map<Comodo, Comodo> juntados = new HashMap<>();
        for (Comodo comodo : comodos) {
            for (Comodo outro : comodo.ligados.keySet()) {
                if (juntados.get(comodo) == outro || juntados.get(outro) == comodo) {
                    continue;
                }
                Comodo.Porta porta = comodo.ligados.get(outro);
                if (porta == null) {
                    continue;
                }
                if (!juntados.containsKey(comodo) && !juntados.containsKey(outro) && juntar(planta, comodo, outro, porta)) {
                    if (((ComodoComum) comodo).tamanho == ComodoComum.Tamanho.NORMAL) {
                        juntados.put(comodo, outro);
                    }
                    if (((ComodoComum) outro).tamanho == ComodoComum.Tamanho.NORMAL) {
                        juntados.put(outro, comodo);
                    }
                    continue;
                }
                if (porta.tipo == Comodo.Porta.Tipo.COMUM) {
                    porta.tipo = Comodo.Porta.Tipo.DESTRANCADA;
                    // (o sorteio só acontece quando pode dar certo, pra não mudar os andares sem portas escondidas)
                    if (chanceEscondida > 0 && Dado.decimal() < chanceEscondida) {
                        porta.tipo = Comodo.Porta.Tipo.ESCONDIDA;
                        boolean pode = segredos ? !isolado(comodos, comodo) && !isolado(comodos, outro) : alcanca(comodos, comodo, outro);
                        if (!pode) {
                            porta.tipo = Comodo.Porta.Tipo.DESTRANCADA;
                        }
                    }
                }
                byte tipo = switch (porta.tipo) {
                    case VAZIA -> PlantaDoAndar.CHAO;
                    case TUNEL -> ComodoDeLigacao.chaoDoTunel();
                    case AGUA -> PlantaDoAndar.AGUA;
                    case COMUM, DESTRANCADA -> PlantaDoAndar.PORTA;
                    case ESCONDIDA -> PlantaDoAndar.PORTA_ESCONDIDA;
                    case TRANCADA -> PlantaDoAndar.PORTA_TRANCADA;
                    case BARRICADA -> PlantaDoAndar.BARRICADA;
                    case CRISTAL -> PlantaDoAndar.PORTA_DE_CRISTAL;
                    case PAREDE -> PlantaDoAndar.PAREDE;
                };
                planta.definir(porta.x, porta.y, tipo);
            }
        }
    }

    // Dá pra ir de um cômodo a outro sem passar por porta escondida, trancada ou parede? (o "Graph" do Shattered,
    // que só anda por portas abertas, túneis e portas comuns)
    private static boolean alcanca(List<Comodo> comodos, Comodo de, Comodo ate) {
        return alcancaveis(de).contains(ate);
    }

    // No andar com segredos: o cômodo fica isolado se dele não der pra chegar em pelo menos 2 salas (contando ele, sem os túneis)
    private static boolean isolado(List<Comodo> comodos, Comodo de) {
        int salas = 0;
        for (Comodo comodo : alcancaveis(de)) {
            if (!(comodo instanceof ComodoDeLigacao)) {
                salas++;
            }
        }
        return salas < 2;
    }

    private static Set<Comodo> alcancaveis(Comodo de) {
        Set<Comodo> vistos = new HashSet<>();
        Deque<Comodo> fila = new ArrayDeque<>();
        fila.add(de);
        vistos.add(de);
        while (!fila.isEmpty()) {
            Comodo aqui = fila.poll();
            for (Map.Entry<Comodo, Comodo.Porta> ligacao : aqui.ligados.entrySet()) {
                Comodo.Porta porta = ligacao.getValue();
                Comodo.Porta.Tipo tipo = porta == null ? Comodo.Porta.Tipo.COMUM : porta.tipo;
                boolean passa = tipo == Comodo.Porta.Tipo.VAZIA || tipo == Comodo.Porta.Tipo.TUNEL || tipo == Comodo.Porta.Tipo.DESTRANCADA
                        || tipo == Comodo.Porta.Tipo.COMUM;
                if (passa && vistos.add(ligacao.getKey())) {
                    fila.add(ligacao.getKey());
                }
            }
        }
        return vistos;
    }

    // Derruba a parede entre duas salas comuns, a partir da porta, até onde as duas deixarem (pelo menos 3 quadradinhos)
    private static boolean juntar(PlantaDoAndar planta, Comodo comodo, Comodo outro, Ponto inicio) {
        Retangulo i = comodo.intersecao(outro);
        Retangulo pedaco;
        if (i.esquerda == i.direita) {
            int de = inicio.y;
            int ate = inicio.y;
            Ponto p = new Ponto(i.esquerda, de);
            while (de > i.cima && outro.podeJuntar(planta, comodo, p) && comodo.podeJuntar(planta, outro, p)) {
                de--;
                p.y--;
            }
            p.y = ate;
            while (ate < i.baixo && outro.podeJuntar(planta, comodo, p) && comodo.podeJuntar(planta, outro, p)) {
                ate++;
                p.y++;
            }
            if (ate - de < 3) {
                return false;
            }
            pedaco = new Retangulo(i.esquerda, de + 1, i.esquerda + 1, ate);
        } else if (i.cima == i.baixo) {
            int de = inicio.x;
            int ate = inicio.x;
            Ponto p = new Ponto(de, i.cima);
            while (de > i.esquerda && outro.podeJuntar(planta, comodo, p) && comodo.podeJuntar(planta, outro, p)) {
                de--;
                p.x--;
            }
            p.x = ate;
            while (ate < i.direita && outro.podeJuntar(planta, comodo, p) && comodo.podeJuntar(planta, outro, p)) {
                ate++;
                p.x++;
            }
            if (ate - de < 3) {
                return false;
            }
            pedaco = new Retangulo(de + 1, i.cima, ate, i.cima + 1);
        } else {
            return false;
        }
        comodo.juntar(planta, outro, pedaco, PlantaDoAndar.CHAO);
        // A parede que sumiu passa a ser da sala (mesma altura de teto)
        for (int y = pedaco.cima; y < pedaco.baixo; y++) {
            for (int x = pedaco.esquerda; x < pedaco.direita; x++) {
                planta.marcarDaSala(x, y, comodo.numeroDaSala);
            }
        }
        return true;
    }

    private static void pintarAgua(PlantaDoAndar planta, List<Comodo> comodos) {
        // (com a moita de musgo, às vezes o andar fica cheio de água: ver TemaDoAndar)
        float agua = TemaDoAndar.atual().tema() == TemaDoAndar.Tema.AGUA ? 0.85f : AGUA;
        boolean[] lago = Mancha.gerar(planta.largura(), planta.altura(), agua, AGRUPAMENTO_DA_AGUA, true);
        for (Comodo comodo : comodos) {
            for (Ponto p : comodo.pontosComAgua()) {
                // (as armadilhas das salas especiais e secretas ficam: no Shattered, a água só cai em chão vazio)
                if (lago[p.x + p.y * planta.largura()] && planta.tipo(p.x, p.y) == PlantaDoAndar.CHAO && planta.grama(p.x, p.y) == PlantaDoAndar.SEM_GRAMA
                        && !planta.temArmadilha(p.x, p.y)) {
                    planta.definir(p.x, p.y, PlantaDoAndar.AGUA);
                }
            }
        }
    }

    // Quanto mais vizinhos com grama, mais chance de ser grama alta (de 1 em 12 até 9 em 12)
    private static void pintarGrama(PlantaDoAndar planta, List<Comodo> comodos) {
        int largura = planta.largura();
        // (com a moita de musgo, às vezes o andar fica cheio de grama: ver TemaDoAndar)
        float quantaGrama = TemaDoAndar.atual().tema() == TemaDoAndar.Tema.GRAMA ? 0.80f : GRAMA;
        boolean[] grama = Mancha.gerar(largura, planta.altura(), quantaGrama, AGRUPAMENTO_DA_GRAMA, true);
        List<Ponto> comGrama = new ArrayList<>();
        for (Comodo comodo : comodos) {
            for (Ponto p : comodo.pontosComGrama()) {
                if (grama[p.x + p.y * largura] && planta.tipo(p.x, p.y) == PlantaDoAndar.CHAO && planta.grama(p.x, p.y) == PlantaDoAndar.SEM_GRAMA
                        && !planta.temPremioEm(p.x, p.y) && !planta.temArmadilha(p.x, p.y)) {
                    comGrama.add(p);
                }
            }
        }
        for (Ponto p : comGrama) {
            int vizinhos = 1;
            for (int dy = -1; dy <= 1; dy++) {
                for (int dx = -1; dx <= 1; dx++) {
                    if ((dx != 0 || dy != 0) && planta.dentro(p.x + dx, p.y + dy) && grama[p.x + dx + (p.y + dy) * largura]) {
                        vizinhos++;
                    }
                }
            }
            planta.definirGrama(p.x, p.y, Dado.decimal() < vizinhos / 12f ? PlantaDoAndar.GRAMA_ALTA : PlantaDoAndar.GRAMA);
        }
    }

    // As armadilhas do andar (o "paintTraps" do Shattered):
    //   - de 2 a 3 (o meio é mais comum), mas no máximo 1 a cada 5 quadradinhos livres
    //   - só em chão vazio (sem grama, água, prêmio, porta, escada ou outra armadilha), em qualquer cômodo que deixe
    //   - as que evitam lugares fechados (dardos, portal) preferem quadradinhos abertos pros dois lados,
    //     e nenhuma armadilha fica do lado de outra nesses lugares abertos
    //   - todas começam escondidas, menos as que não dá pra esconder (a de dardos)
    private static void pintarArmadilhas(PlantaDoAndar planta, List<Comodo> comodos, int andar) {
        int quantas = Dado.normalEntre(2, 3 + andar / 5);
        int largura = planta.largura();
        boolean[] visto = new boolean[largura * planta.altura()];
        List<Ponto> livres = new ArrayList<>();
        for (Comodo comodo : comodos) {
            for (Ponto p : comodo.pontosComArmadilha()) {
                if (!planta.dentro(p.x, p.y) || visto[p.x + p.y * largura]) {
                    continue;
                }
                visto[p.x + p.y * largura] = true;
                if (planta.tipo(p.x, p.y) == PlantaDoAndar.CHAO && planta.grama(p.x, p.y) == PlantaDoAndar.SEM_GRAMA
                        && !planta.temPremioEm(p.x, p.y) && !planta.temArmadilha(p.x, p.y)) {
                    livres.add(p);
                }
            }
        }
        // Aberto: dá pra passar pra cima ou pra baixo, e pra esquerda ou pra direita
        List<Ponto> abertos = new ArrayList<>();
        for (Ponto p : livres) {
            boolean vertical = passavelPraArmadilha(planta, p.x, p.y - 1) || passavelPraArmadilha(planta, p.x, p.y + 1);
            boolean horizontal = passavelPraArmadilha(planta, p.x - 1, p.y) || passavelPraArmadilha(planta, p.x + 1, p.y);
            if (vertical && horizontal) {
                abertos.add(p);
            }
        }
        quantas = Math.min(quantas, livres.size() / 5);
        TipoDeArmadilha[] tipos = TipoDeArmadilha.doAndar(andar);
        float[] pesos = TipoDeArmadilha.pesosDoAndar(andar);
        int[][] lados = { { 0, -1 }, { 1, 0 }, { 0, 1 }, { -1, 0 } };
        // O mecanismo de armadilha (acessório): no andar de armadilhas, 5x mais (as que sobram ficam à vista), e uma parte
        // das escondidas fica à vista em qualquer andar (ver TemaDoAndar)
        TemaDoAndar.Decisao tema = TemaDoAndar.atual();
        int total = tema.tema() == TemaDoAndar.Tema.ARMADILHAS ? 5 * quantas : quantas;
        float chanceAVista = Acessorios.chanceDeArmadilhaAVista(tema.mecanismo());
        float somaAVista = 0;
        for (int i = 0; i < total && !livres.isEmpty(); i++) {
            TipoDeArmadilha tipo = tipos[Dado.chances(pesos)];
            Ponto lugar = tipo.evitaLugaresFechados() && !abertos.isEmpty() ? Dado.elemento(abertos) : Dado.elemento(livres);
            livres.remove(lugar);
            abertos.remove(lugar);
            for (int[] lado : lados) {
                abertos.remove(new Ponto(lugar.x + lado[0], lugar.y + lado[1]));
            }
            somaAVista += chanceAVista;
            boolean aVista = i >= quantas || somaAVista >= 1;
            if (somaAVista >= 1 && i < quantas) {
                somaAVista--;
            }
            planta.colocarArmadilha(lugar.x, lugar.y, tipo, aVista);
        }
    }

    // A porta trancada não conta como passagem (no Shattered ela é sólida)
    private static boolean passavelPraArmadilha(PlantaDoAndar planta, int x, int y) {
        return planta.passavel(x, y) && planta.tipo(x, y) != PlantaDoAndar.PORTA_TRANCADA;
    }
}
