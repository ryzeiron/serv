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

Pas encore porte : les menus GUI graphiques (inventaires cliquables — `/market`, `/metier`
et le terminal Hacker restent en chat cliquable pour l'instant), le HUD, le mini-jeu de
`/hack terminal`, les contrats a terme, le journal boursier, la reputation et les PNJ
marchands.

Points d'API Forge 1.21 recents utilises ici sans pouvoir etre compiles/verifies dans ce
sandbox (a checker en premier en cas d'erreur de compilation) :
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

Tout le reste du plugin Paper (GUIs graphiques, HUD, reputation, PNJ marchands, contrats
a terme, journal...) reste a reecrire dans ce mod — c'est un gros chantier qui sera fait
par etapes.

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
