package com.louzada.pixeldungeon.item.artefato;

import java.util.ArrayList;
import java.util.List;

import com.louzada.pixeldungeon.combate.Particulas;
import com.louzada.pixeldungeon.item.ArtefatoAchado;
import com.louzada.pixeldungeon.item.EfeitosDePergaminho;
import com.louzada.pixeldungeon.item.EscolhaDeItem;
import com.louzada.pixeldungeon.item.Identificacao;
import com.louzada.pixeldungeon.item.ItemPergaminho;
import com.louzada.pixeldungeon.item.TipoDePergaminho;
import com.louzada.pixeldungeon.registro.ModComponentes;
import com.louzada.pixeldungeon.registro.ModEfeitos;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;

// Livro instável (o "UnstableSpellbook" do Shattered): vem cheio e recarrega com o tempo
// (1 carga a cada 120 turnos cheio, mais rápido quanto mais vazio). Cargas: 2 + 60% do nível.
//   ler (clique direito): 1 carga lê um pergaminho ao acaso (nunca melhoria nem transmutação; identificação,
//     remover maldição e mapeamento mágico saem com metade da chance). Não identifica o pergaminho nem ativa talentos.
//   alimentar (agachado): o livro pede os pergaminhos de uma lista embaralhada (2 de cada vez, já identificados);
//     cada um sobe um nível (até o 10)
public class LivroInstavel extends ArtefatoAchado {
    private static final List<TipoDePergaminho> PEDIDOS = List.of(TipoDePergaminho.IDENTIFICACAO, TipoDePergaminho.REMOVER_MALDICAO,
            TipoDePergaminho.IMAGEM_ESPELHADA, TipoDePergaminho.RECARGA, TipoDePergaminho.TELETRANSPORTE, TipoDePergaminho.CANCAO_DE_NINAR,
            TipoDePergaminho.MAPEAMENTO_MAGICO, TipoDePergaminho.FURIA, TipoDePergaminho.RETRIBUICAO, TipoDePergaminho.TERROR);

    public LivroInstavel(Properties propriedades) {
        super(propriedades);
    }

    @Override
    public String id() {
        return "livro_instavel";
    }

    @Override
    public int nivelMaximo() {
        return 10;
    }

    @Override
    public int cargasMaximas(ItemStack pilha) {
        return (int) (nivel(pilha) * 0.6F) + 2;
    }

    @Override
    protected int cargasIniciais(ItemStack pilha) {
        return cargasMaximas(pilha);
    }

    @Override
    protected int corDaBarra() {
        return 0xCC66FF;
    }

    @Override
    protected float aproveitamentoDeFora() {
        return 0.1F;
    }

    @Override
    public void aoTurno(ServerPlayer jogador, ItemStack pilha) {
        if (!pilha.has(ModComponentes.GUARDADO.get())) {
            embaralhar(pilha, jogador.getRandom());
        }
        int faltam = cargasMaximas(pilha) - cargas(pilha);
        if (faltam > 0) {
            carregar(pilha, energia(jogador) / (120F - faltam * 5F));
        }
    }

    // ---- A lista de pergaminhos que o livro pede ----

    // Igual ao Shattered: sorteia a ordem pelas chances de cada pergaminho aparecer
    public static void embaralhar(ItemStack pilha, RandomSource sorte) {
        List<TipoDePergaminho> sobrando = new ArrayList<>(PEDIDOS);
        List<String> ordem = new ArrayList<>();
        while (!sobrando.isEmpty()) {
            int total = 0;
            for (TipoDePergaminho tipo : sobrando) {
                total += tipo.peso();
            }
            int sorteado = sorte.nextInt(total);
            for (TipoDePergaminho tipo : sobrando) {
                sorteado -= tipo.peso();
                if (sorteado < 0) {
                    ordem.add(tipo.id());
                    sobrando.remove(tipo);
                    break;
                }
            }
        }
        // Já subiu de nível (veio de outro jeito)? Então pede menos
        while (ordem.size() > 10 - nivel(pilha)) {
            ordem.remove(0);
        }
        pilha.set(ModComponentes.GUARDADO.get(), String.join(",", ordem));
    }

    public static List<TipoDePergaminho> pedidos(ItemStack pilha) {
        List<TipoDePergaminho> lista = new ArrayList<>();
        String guardado = pilha.getOrDefault(ModComponentes.GUARDADO.get(), "");
        for (String id : guardado.split(",")) {
            TipoDePergaminho tipo = TipoDePergaminho.peloId(id);
            if (tipo != null) {
                lista.add(tipo);
            }
        }
        return lista;
    }

    // Os 2 primeiros da lista são os que o livro aceita agora
    public static List<TipoDePergaminho> aceitosAgora(ItemStack pilha) {
        List<TipoDePergaminho> lista = pedidos(pilha);
        return lista.subList(0, Math.min(2, lista.size()));
    }

    // ---- Ler ----

    @Override
    public boolean usar(ServerPlayer jogador, ItemStack pilha) {
        if (jogador.hasEffect(ModEfeitos.CEGO)) {
            avisar(jogador, "mensagem.pixeldungeon.livro_instavel.cego");
            return false;
        }
        if (cargas(pilha) <= 0) {
            avisar(jogador, "mensagem.pixeldungeon.livro_instavel.sem_carga");
            return false;
        }
        definirCargas(pilha, cargas(pilha) - 1);
        TipoDePergaminho tipo = sortearPergaminho(jogador.getRandom());
        jogador.displayClientMessage(Component.translatable("mensagem.pixeldungeon.livro_instavel.leu", Component.translatable(tipo.chaveDoNome()))
                .withStyle(ChatFormatting.LIGHT_PURPLE), true);
        jogador.level().playSound(null, jogador.blockPosition(), SoundEvents.BOOK_PAGE_TURN, SoundSource.PLAYERS, 1.0F, 0.8F);
        if (tipo.escolheItem()) {
            // Identificação e remover maldição: escolhe o item (se não tiver nenhum que sirva, a carga se perde)
            EscolhaDeItem.pedirSemPergaminho(jogador, tipo);
        } else {
            EfeitosDePergaminho.ler(jogador, tipo);
        }
        return true;
    }

    // Um pergaminho pelas chances do Shattered, sem melhoria nem transmutação; identificação, remover maldição e
    // mapeamento mágico perdem metade das vezes e sorteia de novo
    public static TipoDePergaminho sortearPergaminho(RandomSource sorte) {
        int total = 0;
        for (TipoDePergaminho tipo : TipoDePergaminho.values()) {
            total += tipo.peso();
        }
        while (true) {
            int sorteado = sorte.nextInt(total);
            TipoDePergaminho escolhido = TipoDePergaminho.IDENTIFICACAO;
            for (TipoDePergaminho tipo : TipoDePergaminho.values()) {
                sorteado -= tipo.peso();
                if (sorteado < 0) {
                    escolhido = tipo;
                    break;
                }
            }
            if (escolhido == TipoDePergaminho.TRANSMUTACAO || escolhido == TipoDePergaminho.MELHORIA) {
                continue;
            }
            boolean pelaMetade = escolhido == TipoDePergaminho.IDENTIFICACAO || escolhido == TipoDePergaminho.REMOVER_MALDICAO
                    || escolhido == TipoDePergaminho.MAPEAMENTO_MAGICO;
            if (pelaMetade && sorte.nextBoolean()) {
                continue;
            }
            return escolhido;
        }
    }

    // ---- Alimentar ----

    @Override
    public boolean usarAgachado(ServerPlayer jogador, ItemStack pilha) {
        if (nivel(pilha) >= nivelMaximo()) {
            avisar(jogador, "mensagem.pixeldungeon.livro_instavel.cheio");
            return false;
        }
        if (!pilha.has(ModComponentes.GUARDADO.get())) {
            embaralhar(pilha, jogador.getRandom());
        }
        boolean abriu = EscolhaDeItem.pedirProArtefato(jogador, id(), outra -> serve(jogador, pilha, outra), (heroi, espaco) -> {
            ItemStack livro = ArtefatoAchado.equipado(heroi, LivroInstavel.class);
            if (livro != null && serve(heroi, livro, heroi.getInventory().getItem(espaco))) {
                alimentar(heroi, livro, heroi.getInventory().getItem(espaco));
            }
        });
        if (!abriu) {
            avisar(jogador, "mensagem.pixeldungeon.livro_instavel.sem_pergaminho");
        }
        return abriu;
    }

    // Um pergaminho identificado que o livro está pedindo agora
    private static boolean serve(ServerPlayer jogador, ItemStack livro, ItemStack pergaminho) {
        if (!(pergaminho.getItem() instanceof ItemPergaminho item) || !Identificacao.conhece(jogador, item.runa())) {
            return false;
        }
        return aceitosAgora(livro).contains(Identificacao.tipoDa(jogador, item.runa()));
    }

    public void alimentar(ServerPlayer jogador, ItemStack livro, ItemStack pergaminho) {
        if (!(pergaminho.getItem() instanceof ItemPergaminho item)) {
            return;
        }
        TipoDePergaminho tipo = Identificacao.tipoDa(jogador, item.runa());
        List<TipoDePergaminho> lista = pedidos(livro);
        if (!lista.remove(tipo)) {
            return;
        }
        pergaminho.shrink(1);
        while (lista.size() > nivelMaximo() - 1 - nivel(livro)) {
            lista.remove(0);
        }
        List<String> ids = new ArrayList<>();
        for (TipoDePergaminho resto : lista) {
            ids.add(resto.id());
        }
        livro.set(ModComponentes.GUARDADO.get(), String.join(",", ids));
        subirNivel(jogador, livro);
        Particulas.subindoEmVolta(jogador, Particulas.poeira(0xFF8833, 1.0F), 12, 0.05);
        jogador.level().playSound(null, jogador.blockPosition(), SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 0.6F, 1.2F);
    }

    @Override
    protected void dicaExtra(ItemStack pilha, List<Component> linhas) {
        if (nivel(pilha) >= nivelMaximo() || !pilha.has(ModComponentes.GUARDADO.get())) {
            return;
        }
        List<TipoDePergaminho> aceitos = aceitosAgora(pilha);
        if (aceitos.isEmpty()) {
            return;
        }
        Component nomes = Component.translatable(aceitos.get(0).chaveDoNome());
        if (aceitos.size() > 1) {
            nomes = Component.translatable("dica.pixeldungeon.livro_instavel.ou", nomes, Component.translatable(aceitos.get(1).chaveDoNome()));
        }
        linhas.add(Component.translatable("dica.pixeldungeon.livro_instavel.pede", nomes).withStyle(ChatFormatting.LIGHT_PURPLE));
    }
}
