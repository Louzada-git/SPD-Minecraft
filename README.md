# Pixel Dungeon MC (nome provisório)

O Shattered Pixel Dungeon dentro do Minecraft: não existem mais Overworld, Nether nem End. O mundo inteiro é a dungeon.

Mod para **Minecraft 1.21.1** com **NeoForge**.

## O que já tem

- O tipo de mundo padrão virou a dungeon: sem Nether, sem End e sem terreno normal.
- Andares do esgoto (1 a 4) gerados com salas, corredores e poças de água, no estilo do Shattered.
  - 1 quadradinho do Shattered = 4x4 blocos. As paredes têm 8 blocos de altura.
  - Paredes, chão e teto são inquebráveis.
- Andar 5: a arena do chefe, com 4 pilares grossos (o Goo vem depois).
- Visual construído:
  - Salas com 8 blocos de altura, cornija de degraus no alto das paredes e pilares de tufo entalhado nos cantos.
  - Corredores em túnel mais baixos (6 blocos), com arcos dos dois lados.
  - Paredes de tijolo de pedra que variam sozinhas (com musgo, rachadas) e rodapé de tijolo de tufo.
  - Chão de tufo polido, musgo espalhado (mais perto das poças) e lanternas penduradas por correntes no meio das salas.
  - Moldura de tufo entalhado em volta das escadas.
- Escada de subida na entrada de cada andar e alçapão de descida na saída.
- A run fica salva no mundo, e quem entra no meio começa no andar 1.
- Sem dia/noite, sem chuva e sem mobs do Minecraft nascendo.

As texturas ainda são do próprio Minecraft (provisórias).

## Como rodar no Linux

### 1. Instalar o Java 21

Escolha o comando da sua distro:

```bash
# Ubuntu, Mint, Pop!_OS, Debian
sudo apt install openjdk-21-jdk

# Fedora
sudo dnf install java-21-openjdk-devel

# Arch, Manjaro
sudo pacman -S jdk21-openjdk
```

Pra conferir, `java -version` tem que mostrar 21 (ou mais).

### 2. Abrir o jogo com o mod

Dentro da pasta do projeto:

```bash
./gradlew runClient
```

Na primeira vez ele baixa o Minecraft, o NeoForge e tudo que precisa. Isso demora alguns minutos. Nas próximas vezes é bem mais rápido.

### 3. Testar

1. Crie um **mundo novo**. O tipo padrão já é a dungeon.
2. Ligue **Permitir comandos (cheats)** pra poder usar os comandos de teste.
3. Você aparece no andar 1. Ache o **alçapão de madeira** no chão (é a saída) e clique com o botão direito pra descer.
4. A **escada de pedra** é a entrada: clique com o botão direito pra subir.

Comandos de teste:

| Comando | O que faz |
|---|---|
| `/dungeon andar 3` | Vai direto pro andar 3 (de 1 a 5) |
| `/dungeon novarun` | Começa uma run nova e manda todo mundo pro andar 1 |

### Gerar o .jar do mod

```bash
./gradlew build
```

O arquivo fica em `build/libs/pixeldungeon-0.1.0.jar`. É esse que vai na pasta `mods` de um Minecraft com NeoForge.

### Se der erro

Copie a mensagem de erro inteira (principalmente as linhas com `error:`) e mande pro Claude.

## O que vem depois

1. **Classe e regras:** menu do Shattered (escolha de herói), Guerreiro, força, precisão e esquiva. A run começa quando alguém escolhe a classe, e quem morre vira espectador até alguém usar um ankh.
2. **Itens:** armas e armaduras com força mínima, poções e pergaminhos desconhecidos (cor e runa aleatórias por run), pergaminho de melhoria, comida, e opção no menu pra compartilhar o que foi identificado.
3. **Mobs do esgoto:** com modelos do Minecraft e textura trocada, e mais vida quanto mais gente estiver jogando.
4. **Goo:** o chefe do andar 5.

Depois disso: prisão, cavernas, cidade dos anões e salões demoníacos. O mod vai ser feito pra jogar junto com o Better Combat.

## Como o código está organizado

```
src/main/java/com/louzada/pixeldungeon/
  PixelDungeon.java            classe principal, liga tudo
  registro/                    blocos, itens e aba do criativo
  bloco/                       escadas de subida e descida
  dungeon/
    Andares.java               medidas e onde cada andar fica no mundo
    PlantaDoAndar.java         o mapa do andar em quadradinhos
    GeradorDeAndar.java        sorteia salas e corredores
    ConstrutorDeAndar.java     transforma o mapa em blocos
  run/
    DadosDaRun.java            o que fica salvo no mundo
    GerenciadorDaRun.java      começar run, construir andar, levar jogador
    EventosDaRun.java          servidor ligando, jogador entrando e renascendo
    ComandosDaDungeon.java     comandos de teste
```

## Créditos e licença

Baseado no [Shattered Pixel Dungeon](https://github.com/00-Evan/shattered-pixel-dungeon), de Evan Debenham, que por sua vez é baseado no Pixel Dungeon, de Watabou.

Este mod é software livre sob a licença **GPLv3**, a mesma do Shattered Pixel Dungeon (veja o arquivo `LICENSE`).
