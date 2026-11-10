package com.louzada.pixeldungeon.heroi;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import javax.annotation.Nullable;

import com.louzada.pixeldungeon.item.EfeitosDePocaoExotica;
import com.louzada.pixeldungeon.item.TipoDeAnel;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.neoforged.neoforge.attachment.IAttachmentHolder;
import net.neoforged.neoforge.attachment.IAttachmentSerializer;

// Os dados do herói de cada jogador, salvos junto com o jogador.
// Valem só para a run de número "run"; numa run nova tudo começa do zero.
//
// Além da classe, nível etc., guarda:
//   - os pontos em cada talento
//   - o "estado": efeitos que duram um tempo (os buffs do Shattered), como a raiva provocada,
//     o escudo do selo, as cargas das habilidades da Duelista... Cada um tem um nome (a chave).
//   - os tipos de poção e pergaminho que ele já identificou nesta run
//   - com quem ele compartilha o que identifica (essa escolha continua de uma run pra outra)
//   - as páginas do guia de alquimia que ele já leu (também continuam de uma run pra outra)
public class DadosDoHeroi {
    public static final int FORCA_INICIAL = 10;

    // Com quem o jogador compartilha o que identifica
    public static final String COMPARTILHAR_COM_TODOS = "todos";
    public static final String COMPARTILHAR_COM_NINGUEM = "ninguem";
    public static final String COMPARTILHAR_COM_ESCOLHIDOS = "escolhidos";

    private String classe = "";
    private int forca = FORCA_INICIAL;
    private int nivel = 1;
    private int xp = 0;
    private boolean morto = false;
    private int run = 0;
    private Map<String, Integer> talentos = new HashMap<>();
    public static final String METAMORFOSE = "metamorfose:";
    private CompoundTag estado = new CompoundTag();
    // Tipos de poção e pergaminho que o jogador já sabe (ex.: "pocao:cura")
    private Set<String> conhecidos = new HashSet<>();
    private String modoDeCompartilhar = COMPARTILHAR_COM_TODOS;
    private Set<String> compartilharCom = new HashSet<>();
    // As páginas do guia de alquimia que ele já leu, um bit por página (continuam de uma run pra outra, igual ao diário do Shattered)
    private int paginasDoGuia = 0;
    // O bônus somado dos anéis que ele está usando, por tipo (não é salvo)
    private int[] bonusDosAneis = new int[0];
    private boolean[] usandoAneis = new boolean[0];

    // Zera tudo e começa com a classe escolhida
    public void comecar(ClasseDoHeroi novaClasse, int numeroDaRun) {
        classe = novaClasse.id();
        forca = FORCA_INICIAL;
        nivel = 1;
        xp = 0;
        morto = false;
        run = numeroDaRun;
        talentos.clear();
        estado = new CompoundTag();
        conhecidos.clear();
    }

    // ---- Identificação ----

    public boolean conhece(String chave) {
        return conhecidos.contains(chave);
    }

    // Devolve true se era novidade
    public boolean aprender(String chave) {
        return conhecidos.add(chave);
    }

    public String modoDeCompartilhar() {
        return modoDeCompartilhar;
    }

    public Set<String> compartilharCom() {
        return compartilharCom;
    }

    public int paginasDoGuia() {
        return paginasDoGuia;
    }

    public void definirPaginasDoGuia(int paginas) {
        paginasDoGuia = paginas;
    }

    public void definirCompartilhamento(String modo, Set<String> nomes) {
        modoDeCompartilhar = modo;
        compartilharCom = new HashSet<>(nomes);
    }

    // Esse jogador recebe o que eu identificar?
    public boolean compartilhaCom(String nomeDoOutro) {
        if (COMPARTILHAR_COM_TODOS.equals(modoDeCompartilhar)) {
            return true;
        }
        return COMPARTILHAR_COM_ESCOLHIDOS.equals(modoDeCompartilhar) && compartilharCom.contains(nomeDoOutro);
    }

    @Nullable
    public ClasseDoHeroi classe() {
        return ClasseDoHeroi.peloId(classe);
    }

    // A força de verdade, com o anel do poder
    public int forca() {
        return forca + bonusDoAnel(TipoDeAnel.PODER);
    }

    // A força sem os anéis (a que a poção de força aumenta)
    public int forcaBase() {
        return forca;
    }

    // ---- Anéis (recalculado a cada meio segundo, não é salvo; ver Aneis) ----

    public void definirAneis(int[] bonus, boolean[] usando) {
        bonusDosAneis = bonus;
        usandoAneis = usando;
    }

    public int bonusDoAnel(TipoDeAnel tipo) {
        return bonusDosAneis.length > tipo.ordinal() ? bonusDosAneis[tipo.ordinal()] : 0;
    }

    public boolean usaAnel(TipoDeAnel tipo) {
        return usandoAneis.length > tipo.ordinal() && usandoAneis[tipo.ordinal()];
    }

    public void definirForca(int novaForca) {
        forca = novaForca;
    }

    public int nivel() {
        return nivel;
    }

    public void definirNivel(int novoNivel) {
        nivel = novoNivel;
    }

    public int xp() {
        return xp;
    }

    public void definirXp(int novoXp) {
        xp = novoXp;
    }

    public boolean morto() {
        return morto;
    }

    public void definirMorto(boolean estaMorto) {
        morto = estaMorto;
    }

    public int run() {
        return run;
    }

    // ---- Talentos ----

    public int pontos(Talento talento) {
        return talentos.getOrDefault(talento.id(), 0);
    }

    public void definirPontos(Talento talento, int pontos) {
        talentos.put(talento.id(), pontos);
    }

    // Os talentos do herói num tier: os da classe dele, menos os que o pergaminho da metamorfose trocou por outros
    // (a troca fica no estado: "metamorfose:<talento da classe>" = o talento que entrou no lugar)
    public List<Talento> talentos(int tier) {
        List<Talento> lista = new ArrayList<>();
        ClasseDoHeroi minha = classe();
        if (minha == null) {
            return lista;
        }
        for (Talento daClasse : Talento.daClasse(minha, tier)) {
            Talento novo = Talento.peloId(estado.getString(METAMORFOSE + daClasse.id()));
            lista.add(novo != null ? novo : daClasse);
        }
        return lista;
    }

    public boolean temTalento(Talento talento) {
        return talentos(talento.tier()).contains(talento);
    }

    // Troca um talento do herói por outro do mesmo tier (os pontos passam pro novo)
    public void metamorfosear(Talento antigo, Talento novo) {
        ClasseDoHeroi minha = classe();
        if (minha == null) {
            return;
        }
        for (Talento daClasse : Talento.daClasse(minha, antigo.tier())) {
            Talento noLugar = Talento.peloId(estado.getString(METAMORFOSE + daClasse.id()));
            if ((noLugar != null ? noLugar : daClasse) == antigo) {
                int pontos = pontos(antigo);
                talentos.remove(antigo.id());
                if (pontos > 0) {
                    talentos.put(novo.id(), pontos);
                }
                estado.putString(METAMORFOSE + daClasse.id(), novo.id());
                return;
            }
        }
    }

    public int pontosGastos(int tier) {
        int total = 0;
        for (Map.Entry<String, Integer> talento : talentos.entrySet()) {
            Talento qual = Talento.peloId(talento.getKey());
            if (qual != null && qual.tier() == tier) {
                total += talento.getValue();
            }
        }
        return total;
    }

    // Os pontos do tier que ainda dá pra gastar
    public int pontosLivres(int tier) {
        return Math.max(0, Talento.pontosGanhos(tier, nivel, EfeitosDePocaoExotica.pontosDaInspiracao(this, tier)) - pontosGastos(tier));
    }

    // Os pontos livres de todos os tiers
    public int pontosLivres() {
        int total = 0;
        for (int tier = 1; tier <= Talento.TIERS; tier++) {
            total += pontosLivres(tier);
        }
        return total;
    }

    // ---- Estado (efeitos que duram um tempo) ----

    public int inteiro(String chave) {
        return estado.getInt(chave);
    }

    public void definir(String chave, int valor) {
        estado.putInt(chave, valor);
    }

    public float decimal(String chave) {
        return estado.getFloat(chave);
    }

    public void definir(String chave, float valor) {
        estado.putFloat(chave, valor);
    }

    public boolean sim(String chave) {
        return estado.getBoolean(chave);
    }

    public void definir(String chave, boolean valor) {
        estado.putBoolean(chave, valor);
    }

    @Nullable
    public UUID alvo(String chave) {
        return estado.hasUUID(chave) ? estado.getUUID(chave) : null;
    }

    public void definirAlvo(String chave, UUID alvo) {
        estado.putUUID(chave, alvo);
    }

    // Efeito com tempo: fica ativo por "ticks" a partir de agora (40 ticks = 1 turno)
    public void ativarPor(String chave, long agora, int ticks) {
        estado.putLong(chave, agora + ticks);
    }

    public boolean ativo(String chave, long agora) {
        return estado.getLong(chave) > agora;
    }

    public void desativar(String chave) {
        estado.remove(chave);
    }

    // ---- Salvar e carregar ----

    public static class Serializador implements IAttachmentSerializer<CompoundTag, DadosDoHeroi> {
        @Override
        public DadosDoHeroi read(IAttachmentHolder dono, CompoundTag tag, HolderLookup.Provider registros) {
            DadosDoHeroi dados = new DadosDoHeroi();
            dados.classe = tag.getString("classe");
            dados.forca = tag.contains("forca") ? tag.getInt("forca") : FORCA_INICIAL;
            dados.nivel = Math.max(1, tag.getInt("nivel"));
            dados.xp = tag.getInt("xp");
            dados.morto = tag.getBoolean("morto");
            dados.run = tag.getInt("run");
            CompoundTag talentos = tag.getCompound("talentos");
            for (String id : talentos.getAllKeys()) {
                dados.talentos.put(id, talentos.getInt(id));
            }
            dados.estado = tag.getCompound("estado");
            dados.conhecidos = lerLista(tag, "conhecidos");
            if (tag.contains("compartilhar")) {
                dados.modoDeCompartilhar = tag.getString("compartilhar");
            }
            dados.compartilharCom = lerLista(tag, "compartilhar_com");
            dados.paginasDoGuia = tag.getInt("paginas_do_guia");
            return dados;
        }

        @Override
        public CompoundTag write(DadosDoHeroi dados, HolderLookup.Provider registros) {
            CompoundTag tag = new CompoundTag();
            tag.putString("classe", dados.classe);
            tag.putInt("forca", dados.forca);
            tag.putInt("nivel", dados.nivel);
            tag.putInt("xp", dados.xp);
            tag.putBoolean("morto", dados.morto);
            tag.putInt("run", dados.run);
            CompoundTag talentos = new CompoundTag();
            for (Map.Entry<String, Integer> talento : dados.talentos.entrySet()) {
                talentos.putInt(talento.getKey(), talento.getValue());
            }
            tag.put("talentos", talentos);
            tag.put("estado", dados.estado.copy());
            tag.put("conhecidos", escreverLista(dados.conhecidos));
            tag.putString("compartilhar", dados.modoDeCompartilhar);
            tag.put("compartilhar_com", escreverLista(dados.compartilharCom));
            tag.putInt("paginas_do_guia", dados.paginasDoGuia);
            return tag;
        }

        private static Set<String> lerLista(CompoundTag tag, String nome) {
            Set<String> conjunto = new HashSet<>();
            ListTag lista = tag.getList(nome, Tag.TAG_STRING);
            for (int i = 0; i < lista.size(); i++) {
                conjunto.add(lista.getString(i));
            }
            return conjunto;
        }

        private static ListTag escreverLista(Set<String> conjunto) {
            ListTag lista = new ListTag();
            for (String texto : conjunto) {
                lista.add(StringTag.valueOf(texto));
            }
            return lista;
        }
    }
}
