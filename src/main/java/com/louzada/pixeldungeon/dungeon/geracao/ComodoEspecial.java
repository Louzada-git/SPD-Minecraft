package com.louzada.pixeldungeon.dungeon.geracao;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.function.Supplier;

import com.louzada.pixeldungeon.dungeon.PlantaDoAndar;
import com.louzada.pixeldungeon.dungeon.TemaDoAndar;

// As salas especiais (o "SpecialRoom" do Shattered): têm uma porta só (às vezes trancada: a chave de ferro do andar aparece
// numa sala comum). São as 19 do Shattered até a prisão: 9 de equipamento e 10 de consumíveis (ver Tipo), mais o laboratório
// (um por região, no andar 3 ou 4) e a sala do poço (no andar logo abaixo de um piso frágil).
public abstract class ComodoEspecial extends Comodo {

    // Os tipos de sala especial, separados igual ao Shattered
    public enum Tipo {
        // De equipamento
        PISO_FRAGIL(true, false, false, PisoFragil::new), CRIPTA(true, false, false, Cripta::new), PISCINA(true, false, true, Piscina::new),
        ARSENAL(true, false, false, Arsenal::new), SALA_DA_SENTINELA(true, false, true, SalaDaSentinela::new),
        SALA_DA_ESTATUA(true, false, false, SalaDaEstatua::new), COFRE_DE_CRISTAL(true, true, false, CofreDeCristal::new),
        ESCOLHA_DE_CRISTAL(true, true, false, EscolhaDeCristal::new), SALA_DO_SACRIFICIO(true, false, false, SalaDoSacrificio::new),
        // De consumíveis
        SALA_DAS_PEDRAS(false, false, false, SalaDasPedras::new), JARDIM(false, false, false, Jardim::new), BIBLIOTECA(false, false, false, Biblioteca::new),
        DEPOSITO(false, false, true, Deposito::new), TESOURARIA(false, false, false, Tesouraria::new), POCO_MAGICO(false, false, false, PocoMagico::new),
        SALA_DE_GAS_TOXICO(false, false, true, SalaDeGasToxico::new), SALA_DO_FOGO_MAGICO(false, false, true, SalaDoFogoMagico::new),
        ARMADILHAS(false, false, true, SalaDasArmadilhas::new), CAMINHO_DE_CRISTAL(false, true, false, CaminhoDeCristal::new),
        // Estas não entram na fila: o laboratório aparece uma vez por região, sempre como a primeira sala especial do andar,
        // e a sala do poço aparece (a mais) no andar logo abaixo de um piso frágil
        LABORATORIO(false, false, false, Laboratorio::new), SALA_DO_POCO(false, true, false, SalaDoPoco::new);

        // De equipamento (senão, de consumíveis)
        public final boolean deEquipamento;
        // Usa chaves de cristal (só uma dessas por andar)
        public final boolean deCristal;
        // Dá uma poção pro andar (só uma dessas por andar)
        public final boolean daPocao;
        private final Supplier<ComodoEspecial> criador;

        Tipo(boolean deEquipamento, boolean deCristal, boolean daPocao, Supplier<ComodoEspecial> criador) {
            this.deEquipamento = deEquipamento;
            this.deCristal = deCristal;
            this.daPocao = daPocao;
            this.criador = criador;
        }

        public ComodoEspecial criar() {
            return criador.get();
        }

        // Entra na fila das salas da run?
        public boolean naFila() {
            return this != LABORATORIO && this != SALA_DO_POCO;
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
    // geralmente a primeira da fila; a usada vai pro fim da fila. Num andar, só uma sala de chaves de cristal e só uma que dá
    // poção; o piso frágil não aparece logo antes do chefe. Aqui a run é "simulada" do andar 1 até o pedido,
    // pra cada andar sair sempre igual sem precisar guardar nada.
    // O laboratório (o "labRoomNeeded" do Shattered): um por região, no andar 3 (metade das vezes) ou no 4.
    // A sala do poço: no andar logo abaixo de um piso frágil, a mais.
    public static List<Tipo> doAndar(long semente, int andar) {
        Random sorteio = new Random(semente * 15485863L + 7);
        List<Tipo> deEquipamento = new ArrayList<>();
        List<Tipo> deConsumiveis = new ArrayList<>();
        for (Tipo tipo : Tipo.values()) {
            if (tipo.naFila()) {
                (tipo.deEquipamento ? deEquipamento : deConsumiveis).add(tipo);
            }
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
        int laboratorios = 0;
        int andarDoPoco = -1;
        for (int a = 1; a <= andar; a++) {
            // Andares de chefe não têm salas especiais
            if (a % 5 == 0) {
                continue;
            }
            escolhidas = new ArrayList<>();
            List<Tipo> doAndar = new ArrayList<>(daRun);
            // (no andar extenso, o máximo (2) e mais uma, igual ao Shattered)
            int quantas = TemaDoAndar.daSemente(semente, a) == TemaDoAndar.Tema.GRANDE ? 3 : 1 + escolher(sorteio, new float[] { 1, 4 });
            int regiao = 1 + a / 5;
            boolean laboratorio = false;
            if (regiao > laboratorios && (a % 5 == 4 || (a % 5 == 3 && sorteio.nextInt(2) == 0))) {
                laboratorios++;
                laboratorio = true;
            }
            for (int i = 0; i < quantas; i++) {
                Tipo tipo;
                if (a == andarDoPoco) {
                    andarDoPoco = -1;
                    tipo = Tipo.SALA_DO_POCO;
                    quantas++;
                } else if (laboratorio) {
                    laboratorio = false;
                    tipo = Tipo.LABORATORIO;
                } else {
                    // (cair do piso frágil no andar do chefe não dá)
                    if ((a + 1) % 5 == 0) {
                        doAndar.remove(Tipo.PISO_FRAGIL);
                    }
                    if (doAndar.isEmpty()) {
                        break;
                    }
                    int indice = escolher(sorteio, new float[] { 6, 3, 1 });
                    while (indice >= doAndar.size()) {
                        indice--;
                    }
                    tipo = doAndar.get(indice);
                    if (tipo == Tipo.PISO_FRAGIL) {
                        andarDoPoco = a + 1;
                    }
                }
                escolhidas.add(tipo);
                doAndar.remove(tipo);
                if (tipo.deCristal) {
                    doAndar.removeIf(outro -> outro.deCristal);
                }
                if (tipo.daPocao) {
                    doAndar.removeIf(outro -> outro.daPocao);
                }
                if (daRun.remove(tipo)) {
                    daRun.add(tipo);
                }
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
