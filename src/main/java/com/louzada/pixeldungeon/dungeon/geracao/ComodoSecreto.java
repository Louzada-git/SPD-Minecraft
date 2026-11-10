package com.louzada.pixeldungeon.dungeon.geracao;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.function.Supplier;

import com.louzada.pixeldungeon.dungeon.PlantaDoAndar;

// As salas secretas (o "SecretRoom" do Shattered): salas especiais com a porta escondida, que só aparece
// procurando (ou com o pergaminho de mapeamento mágico). Igual ao Shattered:
//   - cada região tem 2 salas secretas (mais 0 a 1 nas regiões seguintes), espalhadas pelos andares; o andar 1 não tem
//   - a ordem dos tipos é embaralhada por run, e cada sala costuma ser a primeira da fila (60% / 30% / 10%)
// O mod tem 7 das 12 do Shattered: as que precisam de coisas que o mod ainda não tem (alquimia, pedras rúnicas,
// poço mágico, bombas de mel e abismo com chaves douradas) ficam pra depois.
public abstract class ComodoSecreto extends ComodoEspecial {

    public enum Tipo {
        JARDIM(JardimSecreto::new), BIBLIOTECA(BibliotecaSecreta::new), DESPENSA(DespensaSecreta::new), TESOURO(TesouroSecreto::new),
        LABIRINTO(LabirintoSecreto::new), ARSENAL(ArsenalSecreto::new), INVOCACAO(InvocacaoSecreta::new);

        private final Supplier<ComodoSecreto> criador;

        Tipo(Supplier<ComodoSecreto> criador) {
            this.criador = criador;
        }

        public ComodoSecreto criar() {
            return criador.get();
        }
    }

    // Salas secretas por região: a parte inteira sempre, a quebrada é a chance de uma a mais
    private static final float[] SECRETAS_POR_REGIAO = { 2f, 2.25f, 2.5f, 2.75f, 3.0f };

    // A porta fica escondida (em vez de trancada) e o andar fica sabendo que tem um segredo
    protected void esconder(PlantaDoAndar planta) {
        entrada().definir(Porta.Tipo.ESCONDIDA);
        planta.contarSalaSecreta();
    }

    // As salas secretas de um andar. Igual ao ComodoEspecial.doAndar, a run é "simulada" do andar 1 até o pedido
    // (com um sorteador só dela), pra cada andar sair sempre igual sem precisar guardar nada.
    public static List<Tipo> secretasDoAndar(long semente, int andar) {
        Random sorteio = new Random(semente * 7919L + 13);
        int[] daRegiao = new int[SECRETAS_POR_REGIAO.length];
        for (int i = 0; i < daRegiao.length; i++) {
            daRegiao[i] = (int) SECRETAS_POR_REGIAO[i];
            if (sorteio.nextFloat() < SECRETAS_POR_REGIAO[i] % 1f) {
                daRegiao[i]++;
            }
        }
        List<Tipo> fila = new ArrayList<>(List.of(Tipo.values()));
        Collections.shuffle(fila, sorteio);

        List<Tipo> escolhidas = new ArrayList<>();
        for (int a = 1; a <= andar; a++) {
            escolhidas = new ArrayList<>();
            // Andares de chefe não são gerados assim (não têm salas secretas)
            if (a % 5 == 0) {
                continue;
            }
            int quantas = quantasNoAndar(sorteio, daRegiao, a);
            for (int i = 0; i < quantas; i++) {
                int indice = escolher(sorteio, new float[] { 6, 3, 1 });
                while (indice >= fila.size()) {
                    indice--;
                }
                Tipo tipo = fila.remove(indice);
                escolhidas.add(tipo);
                fila.add(tipo);
            }
        }
        return escolhidas;
    }

    // O "secretsForFloor" do Shattered: divide as que faltam na região pelos andares que faltam (o último leva o resto)
    private static int quantasNoAndar(Random sorteio, int[] daRegiao, int andar) {
        int regiao = andar / 5;
        if (andar == 1 || regiao >= daRegiao.length) {
            return 0;
        }
        float media = daRegiao[regiao] / (float) (5 - andar % 5);
        int quantas = sorteio.nextFloat() < media % 1f ? (int) Math.ceil(media) : (int) Math.floor(media);
        daRegiao[regiao] -= quantas;
        return quantas;
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
