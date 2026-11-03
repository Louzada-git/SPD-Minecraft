# Pixel Dungeon MC (nome provisório)

O Shattered Pixel Dungeon dentro do Minecraft: não existem mais Overworld, Nether nem End. O mundo inteiro é a dungeon.

Mod para **Minecraft 1.21.1** com **NeoForge**.

## O que já tem

- O tipo de mundo padrão virou a dungeon: sem Nether, sem End e sem terreno normal.
- Andares do esgoto (1 a 4) gerados com salas, corredores e poças de água, no estilo do Shattered.
  - 1 quadradinho do Shattered = 4x4 blocos. As paredes têm 8 blocos de altura.
  - Paredes, chão e teto são inquebráveis.
- Em cima das poças, a água pinga do teto (igual às goteiras do esgoto do Shattered).
- **Grama** igual ao esgoto do Shattered: manchas de grama baixa e grama alta pelas salas. A grama alta é a do Minecraft, só que bem maior (3 blocos de altura).
  - Encostou, o quadradinho inteiro (4x4) é pisoteado e vira grama baixa. Mobs também pisoteiam (o enxame, que voa, não).
  - 1 em 6 vezes cai uma **gota de orvalho**, que voa sozinha pro **odre** do herói mais perto. Com o odre cheio, a gota cura 5% da vida.
  - A Caçadora passa pela grama alta sem derrubar (igual ao Shattered), mas ainda ganha as gotas.
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
- Cada classe começa com a sua arma do Shattered, armadura de pano (vestida), uma ração, um **odre** e o seu item único:

| Classe | Arma | Dano | Item único |
|---|---|---|---|
| Guerreiro | Espada Curta Desgastada | 1-10 | selo quebrado (na armadura) |
| Mago | Cajado do Mago | 1-6 | varinha de míssil mágico (dentro do cajado) |
| Ladino | Adaga | 1-8 | capa das sombras |
| Caçadora | Luvas Cravejadas (2x mais rápidas) | 1-5 | arco espiritual |
| Duelista | Florete (+1 de defesa) | 1-8 | habilidades das armas (tecla V) |
| Clérigo | Maça (+40% de precisão) | 1-8 | tomo sagrado |

- Regras do Shattered: força (começa em 10), nível, vida (20, +5 por nível), precisão e esquiva. Todo golpe corpo a corpo sorteia se acerta, e o dano é sorteado pela arma. A barra de XP mostra o nível do Shattered.
- A run começa quando alguém escolhe a classe. Quem entra depois escolhe a sua e começa no andar 1.
- Quem morre vira espectador. Um aliado pode trazer ele de volta usando um **ankh** (clique direito). Se todos morrerem, a run acaba e todo mundo volta pro menu.
- As armas já vêm com os ataques do Better Combat (funciona com ou sem ele instalado).

### Itens das classes

- **Selo quebrado (Guerreiro):** vem afixado na armadura de pano. Quando um golpe vai deixar o Guerreiro com metade da vida ou menos, ele ganha 5 de blindagem na hora (os corações amarelos). Depois precisa de 150 turnos pra proteger de novo. A blindagem some se ficar 5 turnos sem inimigos por perto.
- **Cajado do Mago:** já vem com a varinha de míssil mágico dentro. Clique direito lança um raio de luz branca que sempre acerta e dá 2-8 de dano mágico (a armadura não segura). Tem 4 cargas, mostradas na barrinha roxa do ícone, que voltam sozinhas (de ~33 a ~45 segundos cada).
- **Capa das sombras (Ladino):** clique direito liga e desliga a invisibilidade. Os inimigos perdem o Ladino de vista e não acordam. Ligar gasta 1 carga e depois mais 1 a cada 4 turnos. Atacar desliga a capa (e o ataque é surpresa). Começa com 3 cargas e fica mais forte com o uso (até o nível 10).
- **Arco espiritual (Caçadora):** clique direito atira uma flecha mágica (flechas infinitas). Dano de 1-6 que cresce com o nível. É um ataque físico: sorteia acerto e a armadura segura. Colado no inimigo, acerta pior.
- **Habilidades das armas (Duelista):** tecla **V**. Começa com 2 cargas (até 8 com os níveis), que voltam a cada 45-60 turnos. As cargas aparecem do lado da barra de itens.

| Arma | Habilidade |
|---|---|
| Espada curta desgastada | **Cortar:** +3 de dano, acerto garantido. Se matar, o próximo corte em 5 turnos é de graça |
| Adaga | **Furtivo:** lampeja até 5 quadradinhos pra onde está olhando e fica 2 turnos invisível |
| Luvas cravejadas | **Combinação:** acerto garantido, +3 de dano pra cada golpe que acertou antes |
| Florete | **Estocada:** avança até um inimigo a 1 quadradinho e golpeia com +5, acerto garantido |
| Maça | **Golpe pesado:** acerto garantido, atordoa 5 turnos (precisão e esquiva pela metade); de surpresa, +3 |

- **Tomo sagrado (Clérigo):** clique direito abre a lista de magias. Começa com 3 cargas e fica mais forte com o uso.

| Magia | Custo | O que faz |
|---|---|---|
| Luz guia | 1 | raio de 2-8 de dano que ilumina o alvo: o próximo golpe físico nele acerta com certeza |
| Arma celestial | 2 | +2 de dano mágico em cada golpe por 50 turnos |
| Armadura sagrada | 1 | bloqueia mais 1 de dano em cada golpe por 50 turnos |
| Intuição divina (talento) | 3/2 | diz se o item na outra mão é amaldiçoado |
| Escudo de luz (talento) | 1 | 2-4 / 3-6 de armadura contra o inimigo na mira por 5 turnos |

- **Odre (todas as classes):** guarda até 20 gotas de orvalho. Clique direito bebe: cada gota cura 5% da vida, e só gasta as gotas que precisa. A barrinha azul do ícone mostra quantas gotas tem.

### Talentos

- Tecla **K** abre a tela de talentos, no estilo do Shattered. Do nível 2 ao 6, cada nível dá 1 ponto (5 no total), e cada talento vai até +2.
- Os 4 primeiros talentos de cada classe, com os números do Shattered:

| Classe | Talentos |
|---|---|
| Guerreiro | refeição saudável, intuição de veterano*, raiva provocada, determinação de ferro |
| Mago | refeição empoderadora, intuição do estudioso*, magia prolongada, barreira de apoio |
| Ladino | rações guardadas (baús com rações de suprimentos), intuição do ladrão*, golpe surpresa, sombras protetoras |
| Caçadora | recompensa da natureza (bagas na grama alta), intuição de sobrevivente*, ataque consecutivo, auxílio da natureza* |
| Duelista | refeição fortalecedora, intuição de aventureiro*, golpe paciente (ficar parado 1 turno antes de bater), barreira agressiva |
| Clérigo | magias saciadas, intuição divina, luz abrasadora, escudo de luz |

\* Esses ainda não fazem nada, porque dependem de coisas que o mod ainda não tem: itens não identificados (as intuições) e plantas (auxílio da natureza). A tela de talentos avisa isso.

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
- **Partículas:** todo golpe espirra sangue da cor do Shattered (vermelho; verde no slime; amarelado no caranguejo; esverdeado no enxame; gosma preta no Goo). O ataque surpresa solta faíscas, e subir de nível faz um brilho subir em volta do herói.
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
- **Golpe carregado:** 1 em cada 5 ataques (1 em 2 furioso) ele bombeia em vez de bater: aparece "!!!", ele incha e sobe fumaça escura de todo o chão que o golpe vai pegar (2 quadradinhos), com um círculo marcando a borda. Depois de 2 segundos solta um golpe com **3x o dano e 2x a precisão**. Quem sai do círculo antes faz ele desistir.
- Com metade da vida ou menos fica **furioso**: bate mais forte, acerta mais, esquiva mais e bombeia mais vezes. A barra fica vermelha e ele fica espirrando pedaços de gosma. Se ele se curar na água acima da metade, acalma.
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
| tecla `K` | Tela de talentos |
| tecla `V` | Habilidade da arma (Duelista) |
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

1. **Itens:** poções e pergaminhos desconhecidos (já começados, com cor e runa aleatórias por run), pergaminho de melhoria, itens não identificados, sementes e plantas (pra os talentos que faltam), e opção no menu pra compartilhar o que foi identificado.
2. **Texturas próprias** dos inimigos e dos blocos.

Depois disso: prisão, cavernas, cidade dos anões e salões demoníacos. O mod vai ser feito pra jogar junto com o Better Combat.

## Como o código está organizado

```
src/main/java/com/louzada/pixeldungeon/
  PixelDungeon.java            classe principal, liga tudo
  registro/                    blocos, itens, aba do criativo e dados do jogador
  bloco/                       escadas e blocos de decoração
  item/                        armas, armadura, itens das classes, cargas e artefatos
  entidade/                    os inimigos do Shattered, o Goo, o míssil mágico, a luz guia e a flecha espiritual
  efeito/                      efeitos do Shattered (muco cáustico)
  combate/                     sorteio de dano, números flutuantes e partículas
  heroi/
    ClasseDoHeroi.java         as 6 classes e o kit de cada uma
    DadosDoHeroi.java          classe, força, nível e XP de cada jogador
    RegrasDoHeroi.java         contas de vida, precisão, esquiva e XP
    EventosDeCombate.java      acerto, esquiva e dano dos golpes
    CombateDoHeroi.java        golpe do herói (normal, habilidades e flechas)
    Talento.java / Talentos.java    os talentos e o que cada um faz
    Escudos.java               blindagem e o selo do Guerreiro
    HabilidadesDaDuelista.java      as habilidades das armas
    MagiasDoClerigo.java       as magias do tomo sagrado
  rede/                        pacotes do menu (servidor <-> jogador)
  cliente/
    TelaEscolhaDeHeroi.java    o menu Escolha Seu Herói
    TelaDeTalentos.java        a tela de talentos (K)
    TelaDoTomo.java            as magias do Clérigo
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
