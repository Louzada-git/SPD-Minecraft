package com.louzada.pixeldungeon.item;

import javax.annotation.Nullable;

import com.louzada.pixeldungeon.registro.ModComponentes;

import net.minecraft.world.item.ItemStack;

// A alteração que a pedra de alteração põe numa arma ou armadura (o "Augment" do Shattered):
//   velocidade: a arma ataca 1,5x mais rápido (2/3 do tempo) com 70% do dano
//   dano: 150% do dano, mas leva 5/3 do tempo pra atacar
//   (armas de arremesso: o tempo de arremessar muda igual, e a durabilidade também, na proporção da velocidade)
//   evasão: +2 de esquiva por (2 + nível) e -1 de defesa máxima por (2 + nível)
//   defesa: o contrário (-2 de esquiva e +1 de defesa máxima por (2 + nível))
public enum Alteracao {
    VELOCIDADE("velocidade", 0.7F, 2F / 3F, 0F, 0F),
    DANO("dano", 1.5F, 5F / 3F, 0F, 0F),
    EVASAO("evasao", 1F, 1F, 2F, -1F),
    DEFESA("defesa", 1F, 1F, -2F, 1F);

    private String id;
    private float fatorDeDano;
    private float fatorDeAtraso;
    private float fatorDeEsquiva;
    private float fatorDeDefesa;

    Alteracao(String id, float fatorDeDano, float fatorDeAtraso, float fatorDeEsquiva, float fatorDeDefesa) {
        this.id = id;
        this.fatorDeDano = fatorDeDano;
        this.fatorDeAtraso = fatorDeAtraso;
        this.fatorDeEsquiva = fatorDeEsquiva;
        this.fatorDeDefesa = fatorDeDefesa;
    }

    public String id() {
        return id;
    }

    public boolean daArma() {
        return this == VELOCIDADE || this == DANO;
    }

    // O texto do botão na pedra ("+Velocidade -Dano") e o que aparece na dica do item
    public String chaveDoBotao() {
        return "item.pixeldungeon.pedra_alteracao." + id;
    }

    public String chaveDaDica() {
        return "dica.pixeldungeon.alteracao." + id;
    }

    // ---- O que o item tem ----

    @Nullable
    public static Alteracao de(ItemStack pilha) {
        String id = pilha.get(ModComponentes.ALTERACAO.get());
        for (Alteracao alteracao : values()) {
            if (alteracao.id.equals(id)) {
                return alteracao;
            }
        }
        return null;
    }

    // Põe (ou tira, com null). A arma de perto também muda a velocidade de ataque dela.
    public static void definir(ItemStack pilha, @Nullable Alteracao alteracao) {
        if (alteracao == null) {
            pilha.remove(ModComponentes.ALTERACAO.get());
        } else {
            pilha.set(ModComponentes.ALTERACAO.get(), alteracao.id);
        }
        if (pilha.getItem() instanceof ArmaDoShattered arma) {
            arma.atualizarVelocidade(pilha);
        }
    }

    // ---- Os números ----

    // O dano sorteado (antes do bônus da força), com a alteração
    public static int dano(ItemStack pilha, int dano) {
        Alteracao alteracao = de(pilha);
        return alteracao == null ? dano : Math.round(dano * alteracao.fatorDeDano);
    }

    public static float fatorDeDano(ItemStack pilha) {
        Alteracao alteracao = de(pilha);
        return alteracao == null ? 1F : alteracao.fatorDeDano;
    }

    // Quanto tempo leva pra atacar (ou arremessar), com a alteração
    public static float fatorDeAtraso(ItemStack pilha) {
        Alteracao alteracao = de(pilha);
        return alteracao == null ? 1F : alteracao.fatorDeAtraso;
    }

    // A esquiva a mais (ou a menos) da armadura
    public static int esquiva(ItemStack armadura) {
        Alteracao alteracao = de(armadura);
        return alteracao == null ? 0 : Math.round((2 + NivelDoItem.nivel(armadura)) * alteracao.fatorDeEsquiva);
    }

    // A defesa máxima a mais (ou a menos) da armadura
    public static int defesa(ItemStack armadura, int nivel) {
        Alteracao alteracao = de(armadura);
        return alteracao == null ? 0 : Math.round((2 + nivel) * alteracao.fatorDeDefesa);
    }
}
