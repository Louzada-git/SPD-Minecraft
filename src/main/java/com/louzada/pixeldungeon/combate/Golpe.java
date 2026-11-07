package com.louzada.pixeldungeon.combate;

import net.minecraft.world.item.ItemStack;

// Um ataque de alguém em alguém, com tudo que muda o resultado. Monta assim:
//   Golpe.de(atacante, alvo).arma(espada).bonus(5).acertoGarantido()
// e o Combate resolve. Os números que o Combate descobre (surpresa, iluminado) ficam guardados aqui
// pra quem bate e quem apanha poderem usar.
public class Golpe {
    private Combatente atacante;
    private Combatente alvo;
    // A arma usada (vazia = mão vazia, ou um inimigo)
    private ItemStack arma = ItemStack.EMPTY;
    // O dano que o Minecraft calculou (só pra quem não é do Shattered)
    private float danoDoMinecraft = 1;
    // Força do golpe do Minecraft (golpe com a barra de ataque cheia = 1; crítico = 1,5)
    private float forca = 1;
    private float multiplicador = 1;
    private float bonus = 0;
    private boolean acertoGarantido = false;
    private boolean aDistancia = false;
    // Descobertos pelo Combate
    private boolean surpresa = false;
    private boolean iluminado = false;

    private Golpe(Combatente atacante, Combatente alvo) {
        this.atacante = atacante;
        this.alvo = alvo;
    }

    public static Golpe de(Combatente atacante, Combatente alvo) {
        return new Golpe(atacante, alvo);
    }

    // ---- Montar ----

    public Golpe arma(ItemStack pilha) {
        arma = pilha;
        return this;
    }

    public Golpe danoDoMinecraft(float dano) {
        danoDoMinecraft = dano;
        return this;
    }

    public Golpe forca(float proporcao) {
        forca = proporcao;
        return this;
    }

    public Golpe multiplicador(float vezes) {
        multiplicador = vezes;
        return this;
    }

    public Golpe bonus(float dano) {
        bonus = dano;
        return this;
    }

    public Golpe acertoGarantido() {
        acertoGarantido = true;
        return this;
    }

    // Flechas e armas de arremesso: de longe acertam melhor, colado no inimigo pior
    public Golpe aDistancia() {
        aDistancia = true;
        return this;
    }

    void descobrir(boolean surpresa, boolean iluminado) {
        this.surpresa = surpresa;
        this.iluminado = iluminado;
    }

    // ---- Ler ----

    public Combatente atacante() {
        return atacante;
    }

    public Combatente alvo() {
        return alvo;
    }

    public ItemStack arma() {
        return arma;
    }

    public float danoDoMinecraft() {
        return danoDoMinecraft;
    }

    public float forca() {
        return forca;
    }

    public float multiplicador() {
        return multiplicador;
    }

    public float bonus() {
        return bonus;
    }

    public boolean garantido() {
        return acertoGarantido;
    }

    public boolean deLonge() {
        return aDistancia;
    }

    public boolean surpresa() {
        return surpresa;
    }

    public boolean iluminado() {
        return iluminado;
    }
}
