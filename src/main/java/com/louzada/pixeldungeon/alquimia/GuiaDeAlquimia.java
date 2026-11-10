package com.louzada.pixeldungeon.alquimia;

import com.louzada.pixeldungeon.heroi.DadosDoHeroi;
import com.louzada.pixeldungeon.heroi.RegrasDoHeroi;

import net.minecraft.server.level.ServerPlayer;

// O guia de alquimia (o "ALCHEMY_GUIDE" do Shattered): as 5 páginas do esgoto, cada uma explica um grupo de receitas.
// As páginas ficam nos laboratórios (uma por laboratório). Igual ao diário do Shattered, cada jogador guarda as que já leu
// de uma run pra outra; o guia abre no pote de alquimia (botão "Guia") e quando o herói lê uma página.
public class GuiaDeAlquimia {
    // Na ordem do Shattered (as chaves dos textos: "guia.pixeldungeon.alquimia.<id>.titulo" e ".texto")
    public static final String[] PAGINAS = { "pocoes", "pedras", "energia_e_comida", "pocoes_exoticas", "pergaminhos_exoticos" };

    // As páginas que o herói já leu (um bit por página)
    public static int conhecidas(DadosDoHeroi dados) {
        return dados.paginasDoGuia();
    }

    public static boolean conhece(int paginas, int pagina) {
        return (paginas >> pagina & 1) == 1;
    }

    // Lê uma página nova: ela vira a primeira que o herói ainda não tem. Devolve qual foi (-1 = ele já tem todas).
    public static int lerProxima(ServerPlayer jogador) {
        DadosDoHeroi dados = RegrasDoHeroi.dados(jogador);
        for (int pagina = 0; pagina < PAGINAS.length; pagina++) {
            if (!conhece(dados.paginasDoGuia(), pagina)) {
                dados.definirPaginasDoGuia(dados.paginasDoGuia() | 1 << pagina);
                return pagina;
            }
        }
        return -1;
    }

    public static String chaveDoTitulo(int pagina) {
        return "guia.pixeldungeon.alquimia." + PAGINAS[pagina] + ".titulo";
    }

    public static String chaveDoTexto(int pagina) {
        return "guia.pixeldungeon.alquimia." + PAGINAS[pagina] + ".texto";
    }
}
