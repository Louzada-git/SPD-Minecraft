# Pixel Dungeon MC (nome provisório)

O Shattered Pixel Dungeon dentro do Minecraft: não existem mais Overworld, Nether nem End. O mundo inteiro é a dungeon.

Mod para **Minecraft 1.21.1** com **NeoForge**.

## O que já tem

- O tipo de mundo padrão virou a dungeon: sem Nether, sem End e sem terreno normal.
- Andares do esgoto (1 a 4) gerados igual ao Shattered (o mesmo jeito de montar o mapa do jogo original):
  - A entrada, a saída, de 4 a 6 salas comuns (algumas grandes) e 1 ou 2 salas especiais. Metade dos andares é uma volta, a outra metade é um oito, com galhos e túneis entre as salas.
  - As salas do esgoto do Shattered: a dos canos (canais de água com uma calçada em volta), a do anel, a da ponte na água, a dos barris de armazenamento e a das plantas. Salas vizinhas às vezes viram uma só (a parede entre elas some).
  - **Salas especiais**, sempre trancadas: **arsenal** (uma arma e uma armadura), **tesouraria** (ouro no chão ou em baús, que a partir do andar 2 podem ser mímicos), **biblioteca** (estantes e pergaminhos; o primeiro é de identificação ou de remover maldição), **jardim** (grama alta e às vezes plantas) e **cripta** (uma armadura melhor que as do andar, +1, mas amaldiçoada).
  - Manchas de água (30%) e de grama (20%), igual ao esgoto do Shattered.
  - 1 quadradinho do Shattered = 4x4 blocos. As paredes têm 8 blocos de altura.
  - Paredes, chão e teto são inquebráveis.
- **Portas gigantes** entre as salas: ocupam o quadradinho inteiro (4 blocos de largura e 6 de altura), com o desenho da porta do Shattered. Abrem sozinhas quando alguém (herói ou inimigo) chega perto e fecham quando todo mundo sai. Fechadas, bloqueiam a visão dos inimigos.
  - **Portas trancadas** (com cadeado) guardam as salas especiais. Cada uma tem uma **chave de ferro** largada numa sala comum do mesmo andar: é só chegar perto da porta com a chave que ela destranca (e a chave é gasta). A chave só abre as portas do andar dela.
- Em cima das poças, a água pinga do teto (igual às goteiras do esgoto do Shattered).
- **Grama** igual ao esgoto do Shattered: manchas de grama baixa e grama alta pelas salas. A grama alta é a do Minecraft, só que bem maior (3 blocos de altura).
  - Encostou, o quadradinho inteiro (4x4) é pisoteado e vira musgo. Mobs também pisoteiam (o enxame, que voa, não).
  - 1 em 6 vezes cai uma **gota de orvalho**, que voa sozinha pro **odre** do herói mais perto. Com o odre cheio, a gota cura 5% da vida.
  - A Caçadora passa pela grama alta sem derrubar (igual ao Shattered), mas ainda ganha as gotas.
- Andar 5: a arena do **Goo**, com 4 pilares grossos, uma poça grande no meio (o ninho dele) e poças em volta dos pilares. A sala da saída fica do lado e só abre quando ele morre.
- Visual construído:
  - Salas com 8 blocos de altura, cornija de degraus no alto das paredes e pilares de tufo entalhado nos cantos.
  - Corredores em túnel mais baixos (6 blocos), com arcos dos dois lados.
  - Paredes de tijolo de pedra que variam sozinhas (com musgo, rachadas) e rodapé de tijolo de tufo.
  - Chão de tufo polido e lanternas penduradas por correntes no meio das salas.
  - Chão especial de tufo entalhado nas pontes e na biblioteca, estátuas de tufo entalhado, estantes de livros e pilhas de barris.
  - Moldura de tufo entalhado em volta das escadas.
- Escada de subida na entrada de cada andar e alçapão de descida na saída.
- Sem dia/noite, sem chuva e sem mobs do Minecraft nascendo.

### Tela do jogo e inventário

Igual à interface completa do Shattered. Durante a run, a barra de itens, os corações, a fome, a armadura, a experiência e os efeitos do Minecraft somem e entram os do Shattered (com as imagens do próprio Shattered):

- **Embaixo à esquerda, a barra de status:** o rosto do herói (a skin do jogador) com o nível embaixo, a **vida** em vermelho (a blindagem aparece em cinza) e a **experiência** em amarelo, com os números em cima.
  - Em cima das barras ficam os ícones dos **efeitos** (piscando quando estão acabando), o de **fome** ("Faminto" quando falta 1/3 da comida, "Morrendo de fome" quando acaba) e uma estrela com a tecla **K** quando tem ponto de talento pra gastar.
  - O rosto pisca em vermelho com pouca vida (menos de 1/3) e em amarelo por 10 segundos quando ganha um ponto de talento.
- **Embaixo à direita, a barra de ferramentas:** os botões de procurar, esperar e mochila do Shattered e, à esquerda deles, as **9 casinhas** da barra de itens. A do item na mão fica mais clara, com borda amarela. O item da mão esquerda aparece numa casinha solta do lado. A Duelista ganha uma etiqueta em cima com as cargas da habilidade.
- **Inventário (tecla E):** abre igual ao do Shattered: a barra de ferramentas sobe e embaixo dela aparece o painel do inventário. O jogo continua aparecendo atrás (sem escurecer) e fica **pausado** enquanto o inventário está aberto (no single player).
  - Em cima, igual ao Shattered: a casinha da **armadura**, a da mão esquerda, as 2 casinhas de **anel** e o ouro; embaixo, as 27 casinhas da mochila. O capacete do Minecraft não existe na run (as pernas e os pés viraram as casinhas de anel).
  - À esquerda, o painel do herói: a skin em 3D olhando pro mouse, a classe, o nível, a força e o andar.
  - As casinhas mudam de cor igual ao Shattered: cinza claro no que está equipado, avermelhado no que se sabe que é amaldiçoado e arroxeado no que ainda não foi identificado (armas, poções e pergaminhos).
  - Passar o mouse nos efeitos, na fome, nas barras, no ouro e nos botões mostra o que eles são. Clicar no herói (ou apertar **K**) abre os talentos; clicar na mochila fecha o inventário.
  - Não tem a mesa de trabalho 2x2 (o Shattered não tem).
- No criativo, o inventário continua o do Minecraft (pra pegar os itens). Se a tela for estreita demais pra caber tudo embaixo, a barra de status vai pro canto de cima.

### Classes e run

- Menu **Escolha Seu Herói** no estilo do Shattered, com as 6 classes e a arte de cada uma: Guerreiro, Mago, Ladino, Caçadora, Duelista e Clérigo.
- Cada classe começa com a sua arma do Shattered, armadura de pano (vestida), uma ração, um **odre** e o seu item único:

| Classe | Arma | Dano | Item único |
|---|---|---|---|
| Guerreiro | Espada Curta Desgastada | 1-10 | selo quebrado (na armadura) e 3 pedras de arremesso |
| Mago | Cajado do Mago | 1-6 | varinha de míssil mágico (dentro do cajado) |
| Ladino | Adaga | 1-8 | capa das sombras e 3 facas de arremesso |
| Caçadora | Luvas Cravejadas (2x mais rápidas) | 1-5 | arco espiritual |
| Duelista | Florete (+1 de defesa) | 1-8 | habilidades das armas (tecla V) e 2 agulhas de arremesso |
| Clérigo | Maça (+40% de precisão) | 1-8 | tomo sagrado |

- Regras do Shattered: força (começa em 10), nível, vida (20, +5 por nível), precisão e esquiva. Todo golpe corpo a corpo sorteia se acerta, e o dano é sorteado pela arma. A barra de XP mostra o nível do Shattered.
- **1 turno = 2 segundos.** Tudo que no Shattered conta turnos (efeitos, recargas, fome, renascimento dos inimigos...) usa esse tempo.
- **Poções** são bebidas na hora. **Comer** também é na hora, mas o herói fica comendo (sem atacar nem usar itens) pelos mesmos turnos do Shattered: 3 turnos (1 na baga e na ração de suprimentos).
- Com pouca fome, o herói continua correndo normalmente (o Minecraft não deixa mais lento).
- A run começa quando alguém escolhe a classe. Quem entra depois escolhe a sua e começa no andar 1.
- Quem morre vira espectador. Um aliado pode trazer ele de volta usando um **ankh** (clique direito). Se todos morrerem, a run acaba e todo mundo volta pro menu.
- As armas já vêm com os ataques do Better Combat (funciona com ou sem ele instalado).

### Itens das classes

- **Selo quebrado (Guerreiro):** vem afixado na armadura de pano. Quando um golpe vai deixar o Guerreiro com metade da vida ou menos, ele ganha 5 de blindagem na hora (os corações amarelos). Depois precisa de 150 turnos pra proteger de novo. A blindagem some se ficar 5 turnos sem inimigos por perto.
- **Cajado do Mago:** já vem com a varinha de míssil mágico dentro. Clique direito lança um raio de luz branca que sempre acerta e dá 2-8 de dano mágico (a armadura não segura). Tem 4 cargas, mostradas na barrinha roxa do ícone, que voltam sozinhas (de ~33 a ~45 turnos cada).
- **Capa das sombras (Ladino):** clique direito liga e desliga a invisibilidade. Os inimigos perdem o Ladino de vista e não acordam. Ligar gasta 1 carga e depois mais 1 a cada 4 turnos. Atacar desliga a capa (e o ataque é surpresa). Começa com 3 cargas e fica mais forte com o uso (até o nível 10).
- **Arco espiritual (Caçadora):** clique direito atira uma flecha mágica (flechas infinitas). Dano de 1-6 que cresce com o nível. É um ataque físico: sorteia acerto e a armadura segura. Colado no inimigo, acerta pior.
- **Habilidades das armas (Duelista):** tecla **V**. Começa com 2 cargas (até 8 com os níveis), que voltam a cada 45-60 turnos. As cargas aparecem numa etiqueta em cima da barra de ferramentas.

| Armas | Habilidade |
|---|---|
| Espada curta desgastada, espada curta, espada, espada larga | **Cortar:** +3/+4/+5/+7 de dano, acerto garantido. Se matar, o próximo corte em 5 turnos é de graça |
| Adaga, punhal, lâmina assassina | **Furtivo:** lampeja até 5/4/3 quadradinhos pra onde está olhando e fica 2 turnos invisível |
| Luvas cravejadas, sai | **Combinação:** acerto garantido, +3/+4 de dano pra cada golpe que acertou antes |
| Florete, katana | **Estocada:** avança até um inimigo a 1 quadradinho e golpeia com +5/+8, acerto garantido |
| Maça, machadinha, clava, martelo de guerra | **Golpe pesado:** acerto garantido, atordoa 5 turnos (precisão e esquiva pela metade); de surpresa, +3/+4/+5/+6 |

- **Tomo sagrado (Clérigo):** clique direito abre a lista de magias (em linhas de 5). Começa com 3 cargas e fica mais forte com o uso.

| Magia | Custo | O que faz |
|---|---|---|
| Luz guia | 1 | raio de 2-8 de dano que ilumina o alvo: o próximo golpe físico nele acerta com certeza |
| Arma celestial | 2 | +2 de dano mágico em cada golpe por 50 turnos |
| Armadura sagrada | 1 | bloqueia mais 1 de dano em cada golpe por 50 turnos |
| Intuição divina (talento) | 3/2 | diz se o item na outra mão é amaldiçoado |
| Escudo de luz (talento) | 1 | 2-4 / 3-6 de armadura contra o inimigo na mira por 5 turnos |
| Recuperar inscrição (talento) | 3 (6 na transmutação) | repete o último pergaminho lido em 10/300 turnos (menos o de aprimoramento), uma vez por pergaminho |
| Raio solar (talento) | 1 | raio de 4-8 / 6-12 de dano que cega por 4/6 turnos; acertando de novo enquanto o alvo está cego, paralisa |
| Sentido divino (talento) | 2 | vê os inimigos a 8/12 quadradinhos através das paredes por 50 turnos |
| Bênção (talento) | 1 | no aliado na mira: 10/15 turnos de bênção e 10/15 de cura (a sobra vira blindagem); sem aliado, no Clérigo: 6/10 turnos de bênção e 10/15 de blindagem |

- **Odre (todas as classes):** guarda até 20 gotas de orvalho. Clique direito bebe: cada gota cura 5% da vida, e só gasta as gotas que precisa. A barrinha azul do ícone mostra quantas gotas tem.

### Armas

Além das armas iniciais, 10 armas do Shattered, com os mesmos números. Elas aparecem pelos andares (no esgoto: 75% tier 2, 20% tier 3, 4% tier 4, 1% tier 5).

| Arma | Tier | Dano | Força | Especial |
|---|---|---|---|---|
| Espada curta | 2 | 2-15 | 12 | |
| Machadinha | 2 | 2-12 | 12 | +32% de precisão |
| Punhal | 2 | 2-12 | 12 | mais forte de surpresa |
| Espada | 3 | 3-20 | 14 | |
| Clava | 3 | 3-16 | 14 | +28% de precisão |
| Sai | 3 | 3-10 | 14 | ataca 2x mais rápido |
| Katana | 4 | 4-20 | 16 | 0-3 de defesa extra |
| Lâmina assassina | 4 | 4-20 | 16 | mais forte de surpresa |
| Espada larga | 5 | 5-30 | 18 | |
| Martelo de guerra | 5 | 5-24 | 18 | +20% de precisão |

- Igual ao Shattered, cada ponto de força que falta deixa a arma bem menos precisa, e a Duelista não consegue usar a habilidade. Os heróis começam com 10 de força; cada **poção de força** dá +1 (caem 2 no esgoto).
- **Nível:** o pergaminho de melhoria dá +1 de nível: +1 no dano mínimo, +(tier + 1) no máximo (as armas rápidas ganham metade) e um pouco menos de força necessária. Na armadura, +1 de defesa mínima e +tier na máxima. No cajado do Mago e nas varinhas, +1 carga (até 10) e uma magia mais forte.
- **Identificação:** as armas achadas na dungeon vêm sem identificar: o nome não mostra o nível, e a dica mostra os números "típicos" (+0). Elas vêm +0 (75%), +1 (20%) ou +2 (5%). A arma se identifica sozinha depois de 20 golpes; a intuição de veterano (Guerreiro) acelera pra 1,75x / 2,5x, e a intuição de aventureiro (Duelista) pra 2,5x / na hora de segurar.
- **Maldições:** 30% das armas achadas são amaldiçoadas. Segurar a arma revela se ela tem maldição. A intuição divina do Clérigo também revela. O pergaminho de remover maldição tira, e o de melhoria tira 1 vez em 3.

| Maldição | O que faz |
|---|---|
| Irritante | 1 em 20 golpes: a arma grita e chama todos os inimigos do andar |
| De deslocamento | 1 em 12 golpes: o inimigo é teletransportado pra outro lugar do andar |
| Ofuscante | 1 em 10 golpes: um clarão cega quem bateu (10 turnos) e quem estava vendo (5) |
| Polarizante | metade dos golpes dá 50% a mais de dano; a outra metade não dá nada |
| Sacrificante | 1 em 10 golpes: quem bate sangra (mais, quanto mais vida tiver) |
| Inconstante | 1 em 4 golpes: a precisão cai pra um quinto por 10 turnos (acertar desfaz) |

### Armas de arremesso

As 15 armas de arremesso do Shattered, com os mesmos números. Clique direito arremessa uma (gasta 1 turno). Elas aparecem pelos andares em conjuntos de 3, com o mesmo sorteio de tier das armas.

| Arma | Tier | Dano | Usos | Especial |
|---|---|---|---|---|
| Pedra de arremesso | 1 | 2-5 | 5 | não gruda |
| Faca de arremesso | 1 | 2-6 | 5 | mais forte de surpresa |
| Agulha de arremesso | 1 | 2-5 | 12 | |
| Lança de pesca | 2 | 4-10 | 8 | |
| Bastão de arremesso | 2 | 4-8 | 12 | não gruda |
| Shuriken | 2 | 4-8 | 5 | sai na hora, uma vez a cada 20 turnos |
| Lança de arremesso | 3 | 6-15 | 8 | |
| Kunai | 3 | 6-12 | 8 | mais forte de surpresa |
| Boleadeiras | 3 | 4-9 | 5 | aleija por 5 turnos |
| Javelin | 4 | 8-20 | 8 | |
| Tomahawk | 4 | 6-16 | 5 | faz sangrar 3-6 |
| Bumerangue pesado | 4 | 8-16 | 5 | volta pra onde foi arremessado depois de 5 turnos |
| Tridente | 5 | 10-25 | 8 | |
| Martelo de arremesso | 5 | 10-20 | 12 | não gruda |
| Cubo de força | 5 | 10-30 | 5 | acerta todo mundo perto de onde cai |

- Igual ao Shattered, acertam 1,5x melhor de longe e pela metade coladas no inimigo (o arco espiritual agora também ganha esse bônus de longe). Pedem 1 de força a menos que as armas do mesmo tier.
- **Durabilidade:** o conjunto se desgasta a cada acerto; depois dos usos da tabela, uma delas quebra. A barrinha embaixo do ícone mostra quanto falta. Cada nível multiplica os usos por 1,5, e com 100 usos ou mais o conjunto dura pra sempre. As que voltam pra mochila se juntam de novo ao conjunto.
- A maioria fica cravada no inimigo e cai no chão quando ele morre; as que não grudam (e as que erram) caem no chão ali.
- Vêm sem identificar e podem ser amaldiçoadas, igual às armas, mas se identificam com 10 acertos. A maldição só aparece quando uma delas acerta alguém.

### Varinhas

10 das 13 varinhas do Shattered, com os mesmos números (faltam a de terra viva, a de proteção e a de corrupção). Clique direito lança (gasta 1 turno). Elas aparecem pelos andares; todos os tipos têm a mesma chance.

| Varinha | Cargas | O que faz |
|---|---|---|
| Míssil mágico | 3 | 2-8 de dano (+1/+2 por nível) |
| Relâmpago | 2 | 5-10 (+1/+5), que pula pros inimigos a 1 quadradinho (2 na água); o dano se divide entre eles |
| Desintegração | 2 | 2-8 (+1/+4), atravessa inimigos e paredes por 6 + 2 x nível quadradinhos; mais forte pra cada inimigo e parede no caminho |
| Rajada de fogo | 2 | cone de fogo que gasta 1 a 3 cargas: quanto mais, maior e mais forte (com 2 aleija, com 3 paralisa) |
| Corrosão | 2 | nuvem de gás corrosivo: o dano começa em 2 + nível e cresce a cada turno |
| Onda de choque | 2 | 1-3 (+1/+3) em volta de onde explode, e os inimigos voam pra longe (batendo na parede, se machucam e ficam paralisados) |
| Geada | 2 | 2-8 (+1/+5) e deixa o alvo resfriado (mais devagar); mais forte na água |
| Luz prismática | 2 | 1-5 (+1/+3) e pode cegar |
| Transfusão | 2 | num aliado, gasta 5% da sua vida pra curá-lo; num inimigo, encanta por 5 turnos (não ataca) e dá blindagem |
| Maturação | 2 | cone de grama alta que enraíza; gasta 1 a 3 cargas |

- **Cargas:** as do tipo + 1 por nível, até 10. Voltam sozinhas, mais rápido quanto mais vazia. O pergaminho de recarga enche todas.
- **Identificação:** vêm +0 (2/3), +1 (4/15) ou +2 (1/15), sem identificar: o nível e as cargas ficam escondidos. Se identificam depois de 10 disparos.
- **Amaldiçoadas:** 30% vêm amaldiçoadas ("caóticas"): a magia sai aleatória (fogo, gelo, gás, grama, teletransporte ou nada). Descobre na primeira vez que lançar.
- **Cajado do Mago:** agachado, com a varinha na mão e o cajado na outra, clique direito (duas vezes, pra confirmar) põe a varinha dentro do cajado. O cajado dá 1 carga a mais, fica com o maior nível dos dois (+1 se a varinha for melhor e o cajado já tiver nível), e a varinha antiga se perde.

### Anéis

Os 12 anéis do Shattered, com os mesmos números. Igual às poções, cada gema esconde um tipo diferente em cada run ("anel de rubi" pode ser o da pressa). Clique direito põe o anel numa das 2 casinhas de anel do inventário; dá pra tirar pelo inventário.

| Anel | Efeito por bônus |
|---|---|
| Precisão | acerta 1,3x |
| Evasão | desvia 1,125x |
| Fúria | ataca 1,09x mais rápido |
| Pressa | anda 1,15x mais rápido |
| Poder | +1 de força e +3,5% de vida máxima |
| Força | soco de uma arma do tier da força do herói; com arma de perto, +1 de dano |
| Tiro certeiro | armas de arremesso e o arco espiritual dão mais dano (+1 nível), e as de arremesso duram 1,2x |
| Energia | varinhas, cajado e artefatos carregam 1,175x mais rápido |
| Elementos | dano de fogo, gelo, raio, gás, veneno e magia cai pra 0,825x |
| Tenacidade | quanto mais ferido, menos dano (até 0,85x com a vida quase no fim) |
| Fortuna | a cada 0-20 inimigos, cai um prêmio a mais (a cada 5-10 prêmios, um equipamento) |
| Arcano | deixa encantamentos e glifos mais fortes (o mod ainda não tem; por enquanto não faz nada) |

- **Bônus:** o nível do anel + 1. Dois anéis iguais somam.
- **Identificação:** vêm +0 (2/3), +1 (4/15) ou +2 (1/15), sem identificar. Usando, o anel se identifica (o tipo e o nível) depois de 1 nível de experiência.
- **Amaldiçoados:** 30% vêm amaldiçoados: o bônus fica de 0 a -2 (o efeito ao contrário), e o anel gruda no dedo. Pôr revela a maldição.

### Armaduras

5 armaduras do Shattered, com os mesmos números. Elas aparecem pelos andares com o mesmo sorteio de tier das armas, e vêm sem identificar (+0, +1 ou +2, e 30% amaldiçoadas).

| Armadura | Tier | Defesa | Força |
|---|---|---|---|
| Armadura de pano | 1 | 0-2 | 10 |
| Armadura de couro | 2 | 0-4 | 12 |
| Armadura de malha | 3 | 0-6 | 14 |
| Armadura de escamas | 4 | 0-8 | 16 |
| Armadura de placas | 5 | 0-10 | 18 |

- Cada golpe recebido perde um valor sorteado de 0 até a defesa. Cada nível dá +1 na defesa mínima e +tier na máxima.
- Igual ao Shattered, cada ponto de força que falta tira 2 de defesa, deixa a esquiva bem menor e o herói **anda mais devagar** (as armas pesadas demais atacam mais devagar).
- A armadura se identifica sozinha depois de 20 golpes recebidos (a intuição de veterano do Guerreiro acelera, e com +2 identifica na hora de vestir).
- **Armadura amaldiçoada:** vestir revela a maldição, e aí ela **gruda no corpo**: não dá pra tirar até remover a maldição.

| Maldição | O que faz |
|---|---|
| Antientropia | 1 em 8 golpes recebidos: congela tudo em volta, mas põe fogo em quem veste (a não ser na água) |
| Corrosão | 1 em 10 golpes recebidos: espirra muco cáustico em volta (e em quem veste) |
| De deslocamento | 1 em 20 golpes recebidos: teletransporta quem veste pra outro lugar do andar (e o golpe não pega) |
| Metabolizante | 1 em 6 golpes recebidos: troca fome por vida |
| Supercrescimento | 1 em 20 golpes recebidos: uma planta qualquer nasce e é ativada em quem veste |
| Fedor | 1 em 8 golpes recebidos: solta uma nuvem de gás tóxico em volta |

### Ouro, baús e mímicos

- Cada andar (menos o do chefe) tem de 3 a 5 itens espalhados (60% / 30% / 10%), mais uma ração. Cada item é **ouro** (10), **poção** (8), **pergaminho** (8), **arma** (2), **armadura** (2) ou **semente** (1), com os pesos do Shattered (varinhas e anéis ainda não existem). As salas especiais têm os prêmios delas, e cada porta trancada tem a sua chave de ferro. Em cada região também caem 2 poções de força e 3 pergaminhos de melhoria, espalhados pelos andares.
- **Ouro:** uma pilha tem de 30 + 10 por andar até 60 + 20 por andar moedas. Encostou, as moedas vão direto pra bolsa (não ocupam o inventário), sobe um "+N" dourado e o total aparece no inventário, do lado do equipamento. Cada herói tem o seu ouro, pras lojas que vão vir.
- De cada 20 itens, 4 ficam num **baú** (um baú comum do Minecraft, com o item no meio), 1 fica dentro de um **mímico** (a partir do andar 2) e o resto fica largado no chão. Onde tem item, a grama alta é aparada. Os itens nunca somem do chão.
- Ninguém consegue quebrar um baú (só no criativo).
- **Mímico:** por fora é igualzinho a um baú. Quem tenta abrir ou bate nele leva uma mordida que sempre acerta (2 + 2 por andar de dano), e ele vira um baú com dentes que persegue o herói, abrindo e fechando a tampa. Quando morre, solta o item que guardava e mais um prêmio (ouro, uma arma, uma armadura, armas de arremesso ou um anel). Herói invisível não leva a mordida.

| Inimigo | Vida | Precisão | Esquiva | Dano | Armadura | XP |
|---|---|---|---|---|---|---|
| Mímico (nível = andar) | (1 + nível) × 6 | 6 + nível | 2 + nível / 2 | 1 + nível a 2 + 2 × nível | 0 a 1 + nível / 2 | 0 |

### Poções, pergaminhos e plantas

- Igual ao Shattered, em cada run as **cores das poções** e as **runas dos pergaminhos** são sorteadas de novo: numa run a poção escarlate é de cura, na outra é de gás tóxico. Beber, ler ou ver o efeito revela o tipo pro resto da run, e o nome muda na hora.
- **Compartilhar:** no botão "Compartilhar" da tela de talentos (K), cada jogador escolhe se o que ele descobre vai pra todo mundo, pra ninguém ou só pra quem ele marcar.
- Poções: clique direito bebe na hora, agachado + clique direito arremessa. Quem já sabe que a poção é ruim arremessa com o clique direito.

| Poção | Beber | Arremessar |
|---|---|---|
| Força | +1 de força pra sempre | espirra sem efeito |
| Cura | cura 80% da vida + 14 aos poucos e tira os efeitos ruins | espirra sem efeito |
| Visão mental | os inimigos do andar brilham através das paredes por 20 turnos | espirra sem efeito |
| Gélida | congela em volta de quem bebeu | congela por 10 turnos quem estiver perto |
| Chama líquida | fogo em volta de quem bebeu | fogo |
| Gás tóxico | nuvem em volta de quem bebeu | nuvem: 1 + andar/5 de dano por turno |
| Pressa | 3x mais rápido por 20 turnos | espirra sem efeito |
| Invisibilidade | invisível por 20 turnos | espirra sem efeito |
| Levitação | dá pra voar por 20 turnos | nuvem de confusão |
| Gás paralítico | nuvem em volta de quem bebeu | nuvem: paralisa por 10 turnos |
| Pureza | imune a gases por 20 turnos | some com os gases e o fogo em volta |
| Experiência | ganha um nível | espirra sem efeito |

| Pergaminho | O que faz |
|---|---|
| Aprimoramento | escolhe uma arma ou armadura: +1 de nível |
| Identificação | escolhe um item: revela tudo dele |
| Remover maldição | escolhe uma arma: tira a maldição |
| Imagem espelhada | 2 cópias do herói lutam do lado dele (somem com qualquer golpe) |
| Recarga | enche as cargas do cajado do Mago e das varinhas e dá 30 turnos de recarga (eles continuam carregando mais rápido) |
| Teletransportação | leva pra outro lugar do andar, longe de onde estava |
| Acalento | os inimigos visíveis (e quem leu) dormem depois de 5 turnos; o herói dormindo cura 1 por turno |
| Mapeamento mágico | diz pra que lado e a quantos quadradinhos fica a saída, e colunas de luz marcam a saída e a entrada |
| Fúria | todos os inimigos do andar vêm atrás de quem leu; os visíveis atacam qualquer um por 5 turnos |
| Retribuição | dano nos inimigos visíveis (mais, quanto menos vida o leitor tiver); cega todo mundo |
| Terror | os inimigos visíveis fogem por 20 turnos |
| Transmutação | escolhe um item: uma arma vira outra do mesmo tier, uma semente vira outra, poções e pergaminhos viram outro tipo |

- **Sementes:** clique direito no chão planta; no ar, arremessa (a planta nasce onde a semente cair). A grama alta solta uma semente 1 vez em 25. A planta vale pelo quadradinho inteiro: quem pisar nele ativa o efeito e a planta some (quem voa não ativa).

| Planta | O que faz em quem pisa |
|---|---|
| Grama-solar | cura até a vida máxima, devagar, enquanto não sair do lugar |
| Folha-fantasma | teletransporta pra outro lugar do andar |
| Muda-de-gelo | congela quem estiver perto |
| Flor-de-fogo | fogo |
| Sofrimusgo | veneno (5 + 2/3 do andar turnos; o dano vai caindo) |
| Cardo-ligeiro | o tempo para em volta: os inimigos perto ficam parados por 5 turnos |
| Ceguerva | cego e aleijado por 10 turnos |
| Vinha-tempestuosa | vertigem por 10 turnos |
| Terraíz | armadura de raízes enquanto não sair do lugar |
| Magi-real | tira os efeitos ruins |
| Flor-estrela | abençoado: +25% de precisão e esquiva por 30 turnos |

### Talentos

- Tecla **K** abre a tela de talentos, no estilo do Shattered, com uma linha por tier. Do nível 2 ao 6, cada nível dá 1 ponto do tier 1 (5 no total); o tier 2 libera no nível 7, e do 7 ao 12 cada nível dá 1 ponto dele (6 no total). Cada talento vai até +2.
- Tier 1, com os números do Shattered:

| Classe | Talentos |
|---|---|
| Guerreiro | refeição saudável, intuição de veterano (identifica armas mais rápido), raiva provocada, determinação de ferro |
| Mago | refeição empoderadora, intuição do estudioso (identifica varinhas 3x mais rápido, e no primeiro disparo com +2), magia prolongada, barreira de apoio |
| Ladino | rações guardadas (baús com rações de suprimentos), intuição do ladrão (identifica anéis 2x mais rápido e descobre o tipo ao pôr; com +2, identifica ao pôr e descobre o tipo ao pegar), golpe surpresa, sombras protetoras |
| Caçadora | recompensa da natureza (bagas na grama alta), intuição de sobrevivente (identifica armas de arremesso 3x mais rápido, e ao acertar com +2), ataque consecutivo, auxílio da natureza (pele de árvore quando uma planta é ativada perto dela) |
| Duelista | refeição fortalecedora, intuição de aventureiro (identifica armas mais rápido, e na hora com +2), golpe paciente (ficar parado 1 turno antes de bater), barreira agressiva |
| Clérigo | magias saciadas, intuição divina, luz abrasadora, escudo de luz |

- Tier 2, também com os números do Shattered:

| Classe | Talentos |
|---|---|
| Guerreiro | estômago de ferro (comer leva 1 turno e protege do dano), tenacidade líquida (blindagem ao usar poção), transferência rúnica*, ímpeto letal (golpe mortal deixa o próximo ataque pronto), projéteis improvisados (poção ou semente arremessada cega) |
| Mago | refeição energética (recarga do cajado), poder inscrito (pergaminho dá +2 níveis nos próximos disparos), preservação de varinha (a varinha antiga do cajado volta em +0: uma vez com +1, e de novo a cada nível com +2), visão arcana (quem leva o disparo brilha através das paredes), bateria de escudo (usar o cajado ou uma varinha agachado troca as cargas por blindagem) |
| Ladino | refeição mística (recarga da capa e do tomo; rações de suprimentos na hora), furtividade inscrita (pergaminho deixa invisível), grande busca*, passos silenciosos, previsão do ladino* |
| Caçadora | refeição revigorante (pressa; bagas na hora), natureza líquida (poção faz grama alta e enraíza os inimigos perto), passos rejuvenescedores (grama alta onde pisa), sentidos apurados (vê através das paredes perto dela), projéteis duráveis (armas de arremesso duram +50%/+75%) |
| Duelista | refeição focada (carga de arma), agilidade líquida (poção dá esquiva e precisão), recarga de arma, pressa letal (golpe mortal com habilidade dá pressa forte), empunhar ágil (trocar de arma não perde o ataque carregado) |
| Clérigo | refeição iluminada (carga do tomo), recuperar inscrição, raio solar, sentido divino, bênção (as 4 últimas são magias novas do tomo) |

\* Esses ainda não fazem nada, porque dependem de coisas que o mod ainda não tem: glifos (transferência rúnica), a ação de procurar (grande busca) e salas secretas (previsão do ladino). A tela de talentos avisa isso.
- "Visão mental" (ver através das paredes) faz os inimigos brilharem com o contorno do Minecraft.

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
- Gnolls deixam **ouro** metade das vezes. O slime às vezes deixa uma **arma de tier 2** (1 em 5 na primeira, e cada uma que já caiu deixa a próxima 4x mais difícil). A cobra deixa uma **semente** 1 vez em 4, e o enxame às vezes deixa uma **poção de cura** (no máximo 5 por run).
- Herói com mais de 2 níveis acima do nível máximo de um inimigo não ganha mais itens dele, igual ao Shattered.
- Quem dorme solta um "z" de vez em quando; quem nota você mostra um "!". Depois de apanhar, o inimigo ganha uma **barra de vida** em cima da cabeça.
- Cada inimigo tem os seus sons (por enquanto, sons de bichos do Minecraft).
- **Partículas:** todo golpe espirra sangue da cor do Shattered (vermelho; verde no slime; amarelado no caranguejo; esverdeado no enxame; gosma preta no Goo). O ataque surpresa solta faíscas, e subir de nível faz um brilho subir em volta do herói.
- **Armadura:** cada golpe recebido perde de 0 até a defesa da armadura (pano = 0-2). O florete e a katana dão defesa extra.
- **Invisível:** os inimigos que já estavam caçando continuam indo na direção do herói invisível, mas não conseguem atacar.
- O dano é sorteado mais pro meio do intervalo, igual ao Shattered.
- Números de dano sobem em cima de quem apanhou (laranja nos inimigos, vermelho nos heróis), "desviou" quando erra, "✦" no ataque surpresa e "+XP" ao matar. A barra de ação mostra a vida que sobrou no alvo.
- A cada 50 turnos, se o andar tiver menos inimigos que o limite, nasce mais um longe dos heróis (no andar 1 não nasce mais).
- Mais jogadores = inimigos com mais vida (+50% por jogador a mais).
- Herói acima do nível máximo de um inimigo não ganha XP com ele, igual ao Shattered.
- Os inimigos são os **desenhos animados do próprio Shattered**: uma placa que sempre vira pra câmera, com as animações de parado, andando, atacando e morrendo do jogo original. O desenho vira pro lado em que o inimigo anda e fica vermelho quando apanha. Na aba do criativo tem um ovo de cada um pra testar.

### Goo, o chefe do esgoto

| Chefe | Andar | Vida | Precisão | Esquiva | Dano | Armadura | XP |
|---|---|---|---|---|---|---|---|
| Goo | 5 | 100 | 10 (15 furioso) | 8 (12 furioso) | 1-8 (1-12 furioso) | 0-2 | 10 |

- Dorme no ninho, no meio da arena. Quando acorda grita "GULP-GULP!" e aparece a **barra de vida de chefe** no alto da tela.
- **Na água ele se cura** 1 de vida por turno (até dormindo). O segredo é tirar ele da poça.
- **Golpe carregado:** 1 em cada 5 ataques (1 em 2 furioso) ele bombeia em vez de bater: aparece "!!!", ele incha e sobe fumaça escura de todo o chão que o golpe vai pegar (2 quadradinhos), com um círculo marcando a borda. Depois de 2 turnos solta um golpe com **3x o dano e 2x a precisão**. Quem sai do círculo antes faz ele desistir.
- Com metade da vida ou menos fica **furioso**: bate mais forte, acerta mais, esquiva mais e bombeia mais vezes. A barra fica vermelha e ele fica espirrando pedaços de gosma. Se ele se curar na água acima da metade, acalma.
- 1 em cada 3 golpes deixa **muco cáustico**: 1 de dano por turno durante 20 turnos. Entrar na água lava na hora.
- Enquanto ele luta, o andar fica **selado**: ninguém sobe a escada. A luta acaba se não sobrar nenhum herói vivo perto dele.
- Quando morre deixa 2 a 4 **bolhas de gosma** e a saída do andar abre. Todo mundo no andar vê "Chefe Derrotado". A prisão (andar 6 em diante) ainda não existe, então o alçapão avisa que o caminho está bloqueado.
- Mais jogadores = mais vida, igual aos outros inimigos.

As texturas dos blocos ainda são do próprio Minecraft (provisórias), menos as portas. As artes do menu, os ícones dos itens, as portas e os desenhos dos inimigos são do Shattered.

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

### Fotos do teste

A cada push, o teste do GitHub também abre o jogo de verdade (sem monitor), entra no mundo como Guerreiro e tira fotos da tela do jogo e do inventário (com as dicas do mouse e em dois tamanhos de interface), de uma porta comum, de uma trancada e dos inimigos. Elas ficam no branch `fotos`.

### Se der erro

Copie a mensagem de erro inteira (principalmente as linhas com `error:`) e mande pro Claude.

## O que vem depois

1. **Texturas próprias** dos blocos.
2. **Mais itens:** glifos, as varinhas de terra viva, proteção e corrupção, poções e pergaminhos exóticos, alquimia e encantamentos.
3. **O resto do esgoto do Shattered:** portas escondidas (que precisam ser procuradas), armadilhas e as salas que precisam de abismo.

Depois disso: prisão, cavernas, cidade dos anões e salões demoníacos. O mod vai ser feito pra jogar junto com o Better Combat.

## Como o código está organizado

```
src/main/java/com/louzada/pixeldungeon/
  PixelDungeon.java            classe principal, liga tudo
  registro/                    blocos, itens, aba do criativo e dados do jogador
  bloco/                       escadas, portas gigantes, plantas e blocos de decoração
  item/                        armas, armas de arremesso, varinhas, anéis, armadura, poções, pergaminhos, sementes, identificação e maldições
  entidade/                    os inimigos do Shattered, o Goo, o mímico, a imagem espelhada, o míssil mágico, as nuvens de gás...
  efeito/                      efeitos do Shattered (veneno, paralisia, cura... e o que cada um faz por turno)
  combate/
    Combate.java               o golpe do Shattered, igual pra todo mundo (acerto, dano, armadura, reações)
    Golpe.java                 quem bate, quem apanha, a arma e os bônus de um golpe
    Combatente.java            os números de cada um (o herói, os inimigos, a imagem espelhada)
    HeroiEmCombate.java        os números e as reações do herói (arma, armadura, talentos, maldições)
    EventosDeCombate.java      liga o dano do Minecraft ao combate do Shattered
    (e o sorteio, os números flutuantes e as partículas)
  heroi/
    ClasseDoHeroi.java         as 6 classes e o kit de cada uma
    DadosDoHeroi.java          classe, força, nível e XP de cada jogador
    RegrasDoHeroi.java         contas de vida, precisão, esquiva e XP
    PesoDoEquipamento.java     armadura e arma pesadas demais deixam mais lento
    Talento.java / Talentos.java    os talentos e o que os do tier 1 fazem
    TalentosDoTier2.java       o que os talentos do tier 2 fazem
    Escudos.java               blindagem e o selo do Guerreiro
    Aneis.java                 o que os anéis que o herói está usando fazem
    HabilidadesDaDuelista.java      as habilidades das armas
    MagiasDoClerigo.java       as magias do tomo sagrado
  rede/                        pacotes do menu (servidor <-> jogador)
  cliente/
    HudDoHeroi.java            a tela do jogo do Shattered (some com a do Minecraft durante a run)
    PainelDeStatus.java        a barra de status: rosto, vida, experiência e efeitos
    BarraDeFerramentas.java    a barra de ferramentas com as 9 casinhas
    TelaDoInventario.java      o inventário do Shattered
    TelaEscolhaDeHeroi.java    o menu Escolha Seu Herói
    TelaDeTalentos.java        a tela de talentos (K)
    TelaDoTomo.java            as magias do Clérigo
    RenderizadorDeSprite.java  os inimigos com os desenhos animados do Shattered
    SpriteDoShattered.java     as folhas de desenho e as animações de cada inimigo
    PixelDungeonCliente.java   liga os desenhos e as telas
  dungeon/
    Andares.java               medidas e onde cada andar fica no mundo
    PlantaDoAndar.java         o mapa do andar em quadradinhos
    GeradorDeAndar.java        gera o mapa de cada andar (os comuns pela geração do Shattered, o do chefe é a arena)
    geracao/                   a geração do Shattered: os cômodos, o arquiteto (volta ou oito) e o pintor do esgoto
    ConstrutorDeAndar.java     transforma o mapa em blocos (com as portas gigantes)
    PopulacaoDoAndar.java      inimigos de cada andar e renascimento
    SaqueDoAndar.java          o que fica espalhado pelo andar
    Baus.java                  baús e mímicos disfarçados
    PlantasDoShattered.java    o que cada planta faz
    Teleporte.java             teletransporte pra outro lugar do andar
  run/
    DadosDaRun.java            o que fica salvo no mundo
    GerenciadorDaRun.java      começar run, construir andar, levar jogador
    EventosDaRun.java          servidor ligando, jogador entrando e morrendo
    ComandosDaDungeon.java     /heroi e comandos de teste
```

## Créditos e licença

Baseado no [Shattered Pixel Dungeon](https://github.com/00-Evan/shattered-pixel-dungeon), de Evan Debenham, que por sua vez é baseado no Pixel Dungeon, de Watabou.

As artes do menu de herói, os bonecos dos heróis, os ícones dos itens, as portas, os desenhos dos inimigos, a interface e os textos das classes e itens vêm do Shattered Pixel Dungeon, também sob GPLv3. A geração dos andares é uma tradução do código do Shattered.

Este mod é software livre sob a licença **GPLv3**, a mesma do Shattered Pixel Dungeon (veja o arquivo `LICENSE`).
