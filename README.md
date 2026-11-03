# Pixel Dungeon MC (nome provisório)

O Shattered Pixel Dungeon dentro do Minecraft: não existem mais Overworld, Nether nem End. O mundo inteiro é a dungeon.

Mod para **Minecraft 1.21.1** com **NeoForge**.

## O que já tem

- O tipo de mundo padrão virou a dungeon: sem Nether, sem End e sem terreno normal.
- Andares do esgoto (1 a 4) gerados com salas, corredores e poças de água, no estilo do Shattered.
  - 1 quadradinho do Shattered = 4x4 blocos. As paredes têm 8 blocos de altura.
  - Paredes, chão e teto são inquebráveis.
- Andar 5: a arena do **Goo**, com 4 pilares grossos, uma poça grande no meio (o ninho dele) e poças em volta dos pilares. A sala da saída fica do lado e só abre quando ele morre.
- Visual construído:
  - Salas com 8 blocos de altura, cornija de degraus no alto das paredes e pilares de tufo entalhado nos cantos.
  - Corredores em túnel mais baixos (6 blocos), com arcos dos dois lados.
  - Paredes de tijolo de pedra que variam sozinhas (com musgo, rachadas) e rodapé de tijolo de tufo.
  - Chão de tufo polido, musgo espalhado (mais perto das poças) e lanternas penduradas por correntes no meio das salas.
  - Moldura de tufo entalhado em volta das escadas.
- Escada de subida na entrada de cada andar e alçapão de descida na saída.
- Sem dia/noite, sem chuva e sem mobs do Minecraft nascendo.

### Classes e run

- Menu **Escolha Seu Herói** no estilo do Shattered, com as 6 classes e a arte de cada uma: Guerreiro, Mago, Ladino, Caçadora, Duelista e Clérigo.
- Cada classe começa com a sua arma do Shattered, armadura de pano (vestida) e uma ração:

| Classe | Arma | Dano | Especial |
|---|---|---|---|
| Guerreiro | Espada Curta Desgastada | 1-10 | |
| Mago | Cajado do Mago | 1-6 | |
| Ladino | Adaga | 1-8 | |
| Caçadora | Luvas Cravejadas | 1-5 | ataca 2x mais rápido |
| Duelista | Florete | 1-8 | |
| Clérigo | Maça | 1-8 | +40% de precisão |

- Regras do Shattered: força (começa em 10), nível, vida (20, +5 por nível), precisão e esquiva. Todo golpe corpo a corpo sorteia se acerta, e o dano é sorteado pela arma. A barra de XP mostra o nível do Shattered.
- A run começa quando alguém escolhe a classe. Quem entra depois escolhe a sua e começa no andar 1.
- Quem morre vira espectador. Um aliado pode trazer ele de volta usando um **ankh** (clique direito). Se todos morrerem, a run acaba e todo mundo volta pro menu.
- As armas já vêm com os ataques do Better Combat (funciona com ou sem ele instalado).

### Combate e inimigos

Inimigos do esgoto com os números do Shattered:

| Inimigo | Andares | Vida | Precisão | Esquiva | Dano | Armadura | XP |
|---|---|---|---|---|---|---|---|
| Rato Marsupial | 1-3 | 8 | 8 | 2 | 1-4 | 0-1 | 1 |
| Cobra de Esgoto | 1-3 | 4 | 10 | 25 | 1-4 | 0 | 2 |
| Gnoll Explorador | 2-4 | 12 | 10 | 4 | 1-6 | 0-2 | 2 |
| Caranguejo de Esgoto | 3-4 | 15 | 12 | 5 | 1-7 | 0-4 | 4 |
| Slime | 4 | 20 | 12 | 5 | 2-5 | 0 | 4 |
| Enxame de Moscas | 3-4 | 50 | 10 | 5 | 1-4 | 0 | 3 |
| Rato Albino (raro) | 1-3 | 12 | 8 | 2 | 1-4 | 0-1 | 2 |

- Quando o andar é criado, os inimigos nascem **dormindo** (8 no andar 1). Eles acordam quando notam um herói: quanto mais perto, mais rápido.
- **Ataque surpresa:** bater num inimigo dormindo ou que não está de olho em você sempre acerta. A adaga bate quase no dano máximo.
- A cobra esquiva demais: o jeito é pegar ela de surpresa. O caranguejo é rápido. O slime amortece golpes fortes.
- O **enxame de moscas** voa e se divide em dois a cada golpe que leva (os pedaços não dão XP).
- O **rato albino** aparece no lugar de 1 em cada 50 ratos. Metade das mordidas fazem sangrar.
- Caranguejos (1 em 6) e ratos albinos (sempre) deixam **carne misteriosa**: mata metade da fome de uma ração, mas pode queimar, travar as pernas, envenenar ou deixar lento.
- Quem dorme solta um "z" de vez em quando; quem nota você mostra um "!". Depois de apanhar, o inimigo ganha uma **barra de vida** em cima da cabeça.
- Cada inimigo tem os seus sons (por enquanto, sons de bichos do Minecraft).
- **Armadura:** cada golpe recebido perde de 0 até a defesa da armadura (pano = 0-2). O florete dá 1 de defesa extra.
- O dano é sorteado mais pro meio do intervalo, igual ao Shattered.
- Números de dano sobem em cima de quem apanhou (laranja nos inimigos, vermelho nos heróis), "desviou" quando erra, "✦" no ataque surpresa e "+XP" ao matar. A barra de ação mostra a vida que sobrou no alvo.
- A cada 50 segundos, se o andar tiver menos inimigos que o limite, nasce mais um longe dos heróis (no andar 1 não nasce mais).
- Mais jogadores = inimigos com mais vida (+50% por jogador a mais).
- Herói acima do nível máximo de um inimigo não ganha XP com ele, igual ao Shattered.
- Os inimigos usam modelos e texturas do Minecraft por enquanto. Na aba do criativo tem um ovo de cada um pra testar.

### Goo, o chefe do esgoto

| Chefe | Andar | Vida | Precisão | Esquiva | Dano | Armadura | XP |
|---|---|---|---|---|---|---|---|
| Goo | 5 | 100 | 10 (15 furioso) | 8 (12 furioso) | 1-8 (1-12 furioso) | 0-2 | 10 |

- Dorme no ninho, no meio da arena. Quando acorda grita "GULP-GULP!" e aparece a **barra de vida de chefe** no alto da tela.
- **Na água ele se cura** 1 de vida por segundo (até dormindo). O segredo é tirar ele da poça.
- **Golpe carregado:** 1 em cada 5 ataques (1 em 2 furioso) ele bombeia em vez de bater: aparece "!!!", ele incha e um círculo de gosma no chão mostra o alcance (2 quadradinhos). Depois de 2 segundos solta um golpe com **3x o dano e 2x a precisão**. Quem sai do círculo antes faz ele desistir.
- Com metade da vida ou menos fica **furioso**: bate mais forte, acerta mais, esquiva mais e bombeia mais vezes. A barra fica vermelha. Se ele se curar na água acima da metade, acalma.
- 1 em cada 3 golpes deixa **muco cáustico**: 1 de dano por segundo durante 20 segundos. Entrar na água lava na hora.
- Enquanto ele luta, o andar fica **selado**: ninguém sobe a escada. A luta acaba se não sobrar nenhum herói vivo perto dele.
- Quando morre deixa 2 a 4 **bolhas de gosma** e a saída do andar abre. Todo mundo no andar vê "Chefe Derrotado". A prisão (andar 6 em diante) ainda não existe, então o alçapão avisa que o caminho está bloqueado.
- Mais jogadores = mais vida, igual aos outros inimigos.

As texturas dos blocos ainda são do próprio Minecraft (provisórias). As artes do menu e os ícones dos itens são do Shattered.

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
3. Escolha o herói no menu e clique em **Começar**. Você aparece no andar 1.
4. Ache o **alçapão de madeira** no chão (é a saída) e clique com o botão direito pra descer.
5. A **escada de pedra** é a entrada: clique com o botão direito pra subir.

Comandos:

| Comando | O que faz |
|---|---|
| `/heroi` | Abre o menu de escolha de herói (se você fechou sem escolher) |
| `/dungeon andar 3` | Vai direto pro andar 3 (de 1 a 5) |
| `/dungeon xp 20` | Ganha 20 de XP do Shattered (pra testar os níveis) |
| `/dungeon novarun` | Acaba a run atual e manda todo mundo de volta pro menu |

### Gerar o .jar do mod

```bash
./gradlew build
```

O arquivo fica em `build/libs/pixeldungeon-1.1.jar`. É esse que vai na pasta `mods` de um Minecraft com NeoForge.

### Se der erro

Copie a mensagem de erro inteira (principalmente as linhas com `error:`) e mande pro Claude.

## O que vem depois

1. **Itens:** poções e pergaminhos desconhecidos (já começados), os itens únicos das classes (selo do Guerreiro, varinha do Mago, capa do Ladino, arco da Caçadora, habilidade do florete, tomo do Clérigo), armadura de pano, armas e armaduras com força mínima, poções e pergaminhos desconhecidos (cor e runa aleatórias por run), pergaminho de melhoria, comida, e opção no menu pra compartilhar o que foi identificado.
2. **Texturas próprias** dos inimigos e dos blocos.

Depois disso: prisão, cavernas, cidade dos anões e salões demoníacos. O mod vai ser feito pra jogar junto com o Better Combat.

## Como o código está organizado

```
src/main/java/com/louzada/pixeldungeon/
  PixelDungeon.java            classe principal, liga tudo
  registro/                    blocos, itens, aba do criativo e dados do jogador
  bloco/                       escadas e blocos de decoração
  item/                        armas, armadura, ankh e bolha de gosma
  entidade/                    os inimigos do Shattered e o Goo
  efeito/                      efeitos do Shattered (muco cáustico)
  combate/                     sorteio de dano e números flutuantes
  heroi/
    ClasseDoHeroi.java         as 6 classes e o kit de cada uma
    DadosDoHeroi.java          classe, força, nível e XP de cada jogador
    RegrasDoHeroi.java         contas de vida, precisão, esquiva e XP
    EventosDeCombate.java      acerto, esquiva e dano das armas
  rede/                        pacotes do menu (servidor <-> jogador)
  cliente/
    TelaEscolhaDeHeroi.java    o menu Escolha Seu Herói
    PixelDungeonCliente.java   desenho dos inimigos
  dungeon/
    Andares.java               medidas e onde cada andar fica no mundo
    PlantaDoAndar.java         o mapa do andar em quadradinhos
    GeradorDeAndar.java        sorteia salas e corredores
    ConstrutorDeAndar.java     transforma o mapa em blocos
    PopulacaoDoAndar.java      inimigos de cada andar e renascimento
  run/
    DadosDaRun.java            o que fica salvo no mundo
    GerenciadorDaRun.java      começar run, construir andar, levar jogador
    EventosDaRun.java          servidor ligando, jogador entrando e morrendo
    ComandosDaDungeon.java     /heroi e comandos de teste
```

## Créditos e licença

Baseado no [Shattered Pixel Dungeon](https://github.com/00-Evan/shattered-pixel-dungeon), de Evan Debenham, que por sua vez é baseado no Pixel Dungeon, de Watabou.

As artes do menu de herói, os bonecos dos heróis, os ícones dos itens e os textos das classes e itens vêm do Shattered Pixel Dungeon, também sob GPLv3.

Este mod é software livre sob a licença **GPLv3**, a mesma do Shattered Pixel Dungeon (veja o arquivo `LICENSE`).
