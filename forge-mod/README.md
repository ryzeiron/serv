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

Pas encore portes dans cette phase (ils dependent de systemes pas encore reecrits, et
seront ajoutes avec leurs propres phases) : detection de manipulation de marche, primes,
ecoutes du metier Hacker, contrats a terme, journal boursier, menu GUI du marche.

Tout le reste du plugin Paper (metiers Hacker/Mineur, mines, GUIs, HUD, structures de
spawn/iles/PvP, reputation, PNJ marchands...) reste a reecrire dans ce mod — c'est un
gros chantier qui sera fait par etapes.

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
