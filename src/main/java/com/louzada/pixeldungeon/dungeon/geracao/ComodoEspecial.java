package com.louzada.pixeldungeon.dungeon.geracao;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.function.Supplier;

import com.louzada.pixeldungeon.dungeon.PlantaDoAndar;

// As salas especiais (o "SpecialRoom" do Shattered): têm uma porta só, e as do mod ficam trancadas
// (a chave de ferro do andar aparece numa sala comum).
// O mod tem o arsenal, a cripta (de equipamento), o jardim, a biblioteca e a tesouraria (de consumíveis).
public abstract class ComodoEspecial extends Comodo {

    // Os tipos de sala especial, separados igual ao Shattered
    public enum Tipo {
        ARSENAL(true, Arsenal::new), CRIPTA(true, Cripta::new), JARDIM(false, Jardim::new), BIBLIOTECA(false, Biblioteca::new),
        TESOURARIA(false, Tesouraria::new);

        // De equipamento (senão, de consumíveis)
        public final boolean deEquipamento;
        private final Supplier<ComodoEspecial> criador;

        Tipo(boolean deEquipamento, Supplier<ComodoEspecial> criador) {
            this.deEquipamento = deEquipamento;
            this.criador = criador;
        }

        public ComodoEspecial criar() {
            return criador.get();
        }
    }

    private Porta entrada;

    @Override
    public int minLargura() {
        return 5;
    }

    @Override
    public int maxLargura() {
        return 10;
    }

    @Override
    public int minAltura() {
        return 5;
    }

    @Override
    public int maxAltura() {
        return 10;
    }

    @Override
    public int maxLigacoes(int lado) {
        return 1;
    }

    // A única porta da sala
    public Porta entrada() {
        if (entrada == null && !ligados.isEmpty()) {
            entrada = ligados.values().iterator().next();
        }
        return entrada;
    }

    // A porta fica trancada, e o andar ganha uma chave de ferro
    protected void trancar(PlantaDoAndar planta) {
        entrada().definir(Porta.Tipo.TRANCADA);
        planta.adicionarChaveDeFerro();
    }

    // Um quadradinho de chão livre (sem prêmio), longe das paredes.
    // Numa sala pequena e cheia, o prêmio vai pra cima de outro (igual às pilhas do Shattered).
    protected Ponto lugarLivre(PlantaDoAndar planta, byte chao) {
        for (int tentativa = 0; tentativa < 200; tentativa++) {
            Ponto lugar = aleatorio();
            if (planta.tipo(lugar.x, lugar.y) == chao && !planta.temPremioEm(lugar.x, lugar.y)) {
                return lugar;
            }
        }
        Ponto lugar = aleatorio();
        for (int tentativa = 0; tentativa < 200 && planta.tipo(lugar.x, lugar.y) != chao; tentativa++) {
            lugar = aleatorio();
        }
        return lugar;
    }

    // ---- Quais salas especiais cada andar tem ----

    // Igual ao Shattered: no começo da run, as salas especiais são embaralhadas e intercaladas
    // (uma de consumíveis, uma de equipamento...). Cada andar tem 1 ou 2 (quase sempre 2) e escolhe
    // geralmente a primeira da fila; a usada vai pro fim da fila. Aqui a run é "simulada" do andar 1 até o pedido,
    // pra cada andar sair sempre igual sem precisar guardar nada.
    public static List<Tipo> doAndar(long semente, int andar) {
        Random sorteio = new Random(semente * 15485863L + 7);
        List<Tipo> deEquipamento = new ArrayList<>();
        List<Tipo> deConsumiveis = new ArrayList<>();
        for (Tipo tipo : Tipo.values()) {
            (tipo.deEquipamento ? deEquipamento : deConsumiveis).add(tipo);
        }
        Collections.shuffle(deEquipamento, sorteio);
        Collections.shuffle(deConsumiveis, sorteio);
        List<Tipo> daRun = new ArrayList<>();
        daRun.add(deConsumiveis.remove(0));
        while (!deEquipamento.isEmpty() || !deConsumiveis.isEmpty()) {
            if (!deEquipamento.isEmpty()) {
                daRun.add(deEquipamento.remove(0));
            }
            if (!deConsumiveis.isEmpty()) {
                daRun.add(deConsumiveis.remove(0));
            }
        }

        List<Tipo> escolhidas = new ArrayList<>();
        for (int a = 1; a <= andar; a++) {
            // Andares de chefe não têm salas especiais
            if (a % 5 == 0) {
                continue;
            }
            escolhidas = new ArrayList<>();
            List<Tipo> doAndar = new ArrayList<>(daRun);
            int quantas = 1 + escolher(sorteio, new float[] { 1, 4 });
            for (int i = 0; i < quantas && !doAndar.isEmpty(); i++) {
                int indice = escolher(sorteio, new float[] { 6, 3, 1 });
                while (indice >= doAndar.size()) {
                    indice--;
                }
                Tipo tipo = doAndar.remove(indice);
                escolhidas.add(tipo);
                daRun.remove(tipo);
                daRun.add(tipo);
            }
        }
        return escolhidas;
    }

    private static int escolher(Random sorteio, float[] pesos) {
        float soma = 0;
        for (float peso : pesos) {
            soma += peso;
        }
        float valor = sorteio.nextFloat() * soma;
        for (int i = 0; i < pesos.length; i++) {
            valor -= pesos[i];
            if (valor < 0) {
                return i;
            }
        }
        return pesos.length - 1;
    }
}
