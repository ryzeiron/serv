# MarketEconomy — mod Forge

Debut de la reecriture en mod Forge (MC 1.21.10 / Forge 60.1.13), pour avoir des items
et blocs entierement nouveaux (pas des reskins de textures vanilla comme dans l'ancien
plugin Paper de `src/` a la racine du repo).

## Etat actuel

Phase 1 : les 3 items/bloc a l'origine de la bascule vers un mod sont portes en tant que
vrais items enregistres, avec leurs propres textures et modeles :

- `marketeconomy:lithium_ingot` — Lingot de Lithium
- `marketeconomy:plastic` — Plastique
- `marketeconomy:ordinateur` — bloc Ordinateur (forme laptop : base + ecran, oriente
  selon la direction du joueur au moment de la pose), craftable en combinant 3 blocs de
  fer (rangee du bas), 1 Lingot de Lithium (centre) et 1 Plastique (haut).

Phase 2 : le coeur de l'economie de marche, avec sa propre persistence (Forge n'a pas
Vault) :

- `EconomyManager` / `BankManager` — porte-monnaie et banque d'ile des joueurs,
  sauvegardes en JSON dans le dossier de la partie (`<world>/marketeconomy/`).
- `MarketManager` / `MarketItem` — meme moteur offre/demande que le plugin Paper (prix
  qui reagit aux achats/ventes recentes, pression de categorie, evenements krach/ruee
  aleatoires), avec les 11 memes items tradables.
- Commandes `/market` (liste des prix en chat), `/buy <item> [quantite]`,
  `/sell [quantite]` (vend l'item tenu en main), `/banque <solde|deposer|retirer>
  [montant]`.
- Recalcul des prix toutes les 60s (comme `price-update-interval` dans l'ancien
  config.yml), via le tick serveur.

Phase 3 : systeme de metiers + capacites du Hacker (la raison d'etre de la detection de
manipulation/primes/ecoutes laissee de cote en phase 2 — elles reviennent ici) :

- `JobType` / `PlayerJob` / `JobManager` — Hacker (niveau max 10) et Mineur (niveau max
  45), xp et niveaux persistes en JSON, memes formules que le plugin Paper.
- `MarketManager` recupere la detection de manipulation de marche, les primes
  (`/prime <joueur>`) et les ecoutes (wiretap) du Hacker.
- `HackerAbilityService` : mêmes 5 capacites que le plugin Paper (`/hack market`,
  `/hack price <item> <up|down>`, `/hack scramble`, `/hack wiretap <joueur>`,
  `/hack banque <joueur>`), memes couts/cooldowns/formules par niveau.
- `/metier` : choix de metier et affichage de la progression, avec des liens cliquables
  dans le chat en attendant un vrai menu GUI.
- Le bloc Ordinateur ouvre maintenant un "terminal" au clic droit : les capacites Hacker
  s'affichent en liens cliquables (pré-remplissent la commande dans le chat).

Phase 4 : le metier Mineur et ses mines. Contrairement au plugin Paper (qui reskinnait
du calcite en "minerai de lithium" et bundlait 4 fichiers de structure .nbt generes par
un script Python), le mod a un vrai bloc `marketeconomy:lithium_ore` et genere les mines
directement en Java (`Level#setBlock` en boucle) plutot que de placer une structure NBT
statique :

- `marketeconomy:lithium_ore` — vrai minerai (texture roche + eclats violets), donne un
  Lingot de Lithium via sa table de butin.
- `MineGenerator` — porte fidelement `structures/build_mines.py` (memes tables de
  minerai/densites/graines par palier, memes filons de lithium, meme plateforme d'entree,
  muret et torches) en generation procedurale directe.
- `MineManager` — genere/enregistre les mines (persistees en JSON), regeneration toutes
  les 25 min (comme `mines.regen-minutes` dans l'ancien config.yml), reseau de
  teleportation entre paliers (etiquettes "Mine n°X" flottantes via ArmorStand,
  detection de plaque de pression via le tick joueur faute d'evenement Forge dedie).
- `MineEvents` — meme gating que le plugin Paper (il faut être Mineur au niveau minimum
  du palier pour casser un minerai) et meme xp par minerai casse.
- `/mine spawn <1-4>` (permission niveau 2, comme `/gamemode`) genere une mine à la
  position du joueur.

Phase 5 : structures et reseau de teleportation fixe. Bonne nouvelle : le format NBT de
structure Minecraft (DataVersion/size/palette/blocks/entities) est le meme cote
Paper et cote Forge/vanilla — les structures deja generees pour le plugin Paper
(`structures/*.nbt` a la racine du repo) sont directement reutilisees, pas regenerees :

- `data/marketeconomy/structure/{spawn_hub,spawn_castle,pvp_island,place_du_marche}.nbt`
  — copiees telles quelles. Ce sont de grosses structures statiques (le chateau de spawn
  fait ~700k blocs) qu'un admin place une seule fois avec la commande vanilla
  `/place structure marketeconomy:<nom>`, puis enregistre l'emplacement avec `/sethub` ou
  `/setpvp` — exactement le meme flux que sous Paper, aucun code Java necessaire pour
  celles-la.
- `data/marketeconomy/structure/starter_island.nbt` — copiee aussi, mais celle-ci EST
  posee par du code (`IslandManager`, via `StructureTemplateManager`) car chaque joueur
  doit en recevoir une automatiquement, sur une grille espacee de 400 blocs (comme
  l'original), avec le meme coffre de depart rempli (sapin, pain, pierre, bois, seaux).
- `WarpManager` — persistence JSON du hub et de l'ile PvP (position + dimension + yaw/pitch).
- Commandes `/spawn`, `/pvp`, `/sethub`, `/setpvp` (permission niveau 2), `/ile`.

Phase 6 : journal boursier, contrats a terme, reputation :

- `MarketManager` genere de nouveau des titres d'actualite a chaque recalcul de prix
  (plus gros mouvement de prix du cycle, stabilisation apres un evenement) ; `/journal`
  les affiche en chat (pas de livre ecrit — le format des livres a change avec les
  composants de donnees en 1.20.5+, evite ici par prudence).
- `FuturesContract` / `FuturesItem` : contrats a terme LONG/SHORT sur un item du marche,
  materialises par un item PAPIER echangeable (tagge via le composant `CustomData`,
  successeur du PDC Bukkit) qui se regle au prix constate a l'echeance et s'encaisse en
  clic droit. Commande `/futures <long|short> <item> <mise> <minutes>` et `/futures list`.
  La variante "Graine Spéculative" (item alternatif fantaisie pour le meme contrat) n'est
  pas portee, seul le Contrat Scellé (papier) existe.
- `ReputationManager` / `PlayerReputation` : score de confiance par joueur, gagne a
  chaque achat/vente, avec des titres de prestige (Négociant → Marchand → Grand Marchand
  → Magnat du Marché) affiches en prefixe d'equipe. Contrairement a Bukkit (scoreboard
  personnalisable par joueur), le vanilla n'a qu'un seul Scoreboard partage par le
  serveur — plus simple ici, pas besoin de le repousser sur chaque spectateur.
  Les multiplicateurs prix (`getBuyMultiplier`/`getSellMultiplier`) et le systeme
  d'accueil des PNJ (`buildGreeting`) sont portes mais pas encore branches, faute de
  systeme de PNJ marchands (voir plus bas).

Phase 7 : PNJ marchands avec memoire. Meme principe que le reste du portage : le clic droit
sur un villageois ouvre un menu en chat cliquable (liens `[Acheter]`/`[Vendre]`/`[Tout
vendre]`) plutot qu'un inventaire graphique.

- `VillagerTrade` — meme correspondance metier de villageois → categorie du marche que le
  plugin Paper (fermier/pêcheur/boucher → consommables, forgerons → minerais, etc.).
- `VillagerEvents` — clic droit sur un villageois : accueil selon la reputation
  (`buildGreeting`), blocage si le joueur est "hostile" pour ce PNJ, sinon liste des
  items de sa categorie avec prix ajustes par la reputation (`getBuyMultiplier`/
  `getSellMultiplier`) et liens cliquables. Tuer un villageois penalise la reputation du
  tueur.
- `VillagerCommands` — `/villagerbuy`, `/villagersell`, `/villagersellall <categorie>`
  (equivalent des clics du menu graphique original), pas destinees a etre tapees a la main.

Phase 8 : menus GUI graphiques (inventaires cliquables), pour `/market`, `/metier` et le
terminal Hacker (le marchand PNJ reste en chat cliquable pour l'instant, meme approche
applicable si besoin).

- `DisplayMenu` — menu "coffre" generique et reutilisable : une grille d'icones cliquables
  au-dessus de l'inventaire du joueur. Astuce qui evite tout code cote client : en sous-
  classant directement `ChestMenu` et en lui passant un `MenuType.GENERIC_9xN` vanilla, le
  client affiche automatiquement l'ecran de coffre standard (deja enregistre par le jeu) —
  pas besoin d'enregistrer un `MenuType` ni d'ecrire un `Screen` custom. Les clics sur la
  grille du haut sont intercepted (`clicked()` surchargee) pour executer une action au lieu
  de deplacer l'item ; les clics sur l'inventaire du joueur (bas de l'ecran) fonctionnent
  normalement.
- `MarketScreenGUI` — une icone par item du marche (prix/stock/tendance en lore), clic
  gauche achete 1, clic droit vend 1. `/market` l'ouvre desormais directement.
- `JobMenuGUI` — une icone par metier (Hacker/Mineur), avec le niveau et l'xp en lore si
  deja actif ; cliquer choisit ce metier. `/metier` l'ouvre desormais directement.
- `HackerTerminalGUI` — les 5 capacites du Hacker en icones ; "brouiller la trace"
  s'execute directement au clic, les autres (qui ont besoin d'un item ou d'un joueur en
  argument) ferment le menu et suggerent la commande a completer dans le chat. Remplace
  l'ancien `HackerTerminal` tout-en-chat (supprime).

Phase 9 : HUD. Le plugin Paper affichait une barre laterale scoreboard personnelle (un
`Scoreboard` par joueur, propre a l'API Bukkit). Le vanilla n'a qu'un seul `Scoreboard`
partage par tout le serveur : une vraie barre laterale personnalisee par joueur
demanderait soit d'envoyer des paquets de score bruts directement sur la connexion de
chaque joueur (contournant le `Scoreboard` partage), soit un canal reseau custom — deux
API plus recentes et plus incertaines que tout ce qui a ete utilise jusqu'ici, evitees
par prudence. A la place, `HudManager` affiche les memes infos (solde, banque, metier,
tendance marche, ile) condensees sur une seule ligne en **barre d'action**
(`Player#displayClientMessage`, methode vanilla simple et stable), rafraichie toutes les
2s. `/hud [on|off]` bascule l'affichage (persiste en JSON, comme l'original).

Phase 10 : mini-jeu du terminal Hacker. `HackTerminalMinigame` reutilise `DisplayMenu`
pour la grille 3x3 (memes regles que le plugin Paper : un noeud correct cache, 3 essais,
indice "brûlant/chaud/tiède/froid" base sur la distance de Manhattan, recompense + xp en
cas de reussite, xp de consolation en cas d'echec). Accessible via `/hack terminal` et
depuis une 6e icone dans `HackerTerminalGUI`.

Phase 11 : derniers details — Boussole du Marchand et menu graphique du marchand PNJ.

- `MerchantCompassItem` / `MerchantCompassTracker` : une vraie boussole (pas une
  lodestone) dont l'aiguille est reorientee toutes les 2s vers le villageois le plus
  proche de la categorie actuellement la plus "en solde" (meme calcul de ratio prix
  actuel/prix de base que le plugin Paper), via le composant `LODESTONE_TRACKER` avec
  `tracked=false`. Donnee par `/marketitem boussole`. La variante "Graine Spéculative"
  du plugin Paper reste non portee (doublon fantaisie du contrat a terme papier, voir
  `/futures`).
- `VillagerTradeGUI` : le marchand PNJ utilise maintenant `DisplayMenu` comme les autres
  menus (une icone par item, clic gauche achete, clic droit vend, icone "Tout vendre" en
  dernier slot), remplace l'ancien menu en chat cliquable. `VillagerCommands` (devenu
  inutile) est supprime.

Avec cette phase, l'intégralité du plugin Paper (contenu, gameplay, interface graphique)
est portee dans le mod Forge — plus aucune fonctionnalite majeure ne manque.

Points d'API Forge 1.21 recents utilises ici sans pouvoir etre compiles/verifies dans ce
sandbox (a checker en premier en cas d'erreur de compilation) :
- `ChestMenu`'s protected constructor `(MenuType<?>, int, Inventory, Container, int)` dans
  `DisplayMenu` (utilise en le sous-classant directement — le point le plus risque de la
  phase 8, jamais verifie par compilation ici).
- `OrdinateurBlock#useWithoutItem` (le clic droit sans item special sur un bloc a ete
  scinde de `use()` vers `useWithoutItem`/`useItemOn` autour de la 1.20.5).
- `TickEvent.ServerTickEvent` / `TickEvent.PlayerTickEvent` dans `ServerEvents` et
  `MineEvents` (events de tick historiques de Forge, verifier qu'ils n'ont pas ete
  remplaces par des variantes `.Pre`/`.Post` dans le Forge exact utilise).
- `BlockEvent.BreakEvent` importe depuis `net.minecraftforge.event.level` (le package a
  ete renomme depuis `net.minecraftforge.event.world` a un moment de la 1.20.x).
- `StructureTemplateManager#get(ResourceLocation)` / `StructureTemplate#placeInWorld(...)`
  dans `IslandManager` (API de placement de structure, stable depuis longtemps mais pas
  verifiee ici faute de compilation).
- `DataComponents.CUSTOM_DATA`/`CUSTOM_NAME`/`LORE` dans `FuturesItem` (le systeme de
  composants de donnees qui remplace l'ancien NBT d'ItemStack depuis la 1.20.5).
- `PlayerTeam#setPlayerPrefix` / `Scoreboard#addPlayerTeam` dans `ReputationManager`.
- `VillagerData#getProfession()` dans `VillagerEvents`/`MerchantCompassTracker` : suppose
  qu'il renvoie un `Holder<VillagerProfession>` (d'ou l'appel `.value()`) plutot qu'un
  `VillagerProfession` direct — a verifier en premier si ces classes ne compilent pas.
- `DataComponents.LODESTONE_TRACKER` / `LodestoneTracker` / `GlobalPos.of(...)` dans
  `MerchantCompassItem` (meme famille de composants que `FuturesItem`, mais jamais
  utilisee ailleurs dans ce mod pour un objet de suivi comme la boussole).

## Build

Ce sandbox n'a pas acces reseau aux depots Minecraft/Forge (`files.minecraftforge.net`,
`maven.minecraftforge.net`, etc. sont bloques par la politique reseau de l'environnement),
donc le projet n'a jamais ete compile ici — exactement comme pour le plugin Paper, qui
n'a jamais ete compile dans ce sandbox non plus (`repo.papermc.io` est bloque aussi).

Pour compiler et lancer en local :

```
cd forge-mod
./gradlew build          # genere build/libs/marketeconomy-1.0.0.jar
./gradlew runServer       # lance un serveur de test avec le mod charge
```

(Le premier lancement telecharge Minecraft/Forge/les mappings, prevoir une connexion
internet normale et quelques minutes.)

Le jar compile va dans `mods/` d'un serveur Forge 1.21.10.
