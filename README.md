# Randomize_Chest

🇫🇷 [Français](#français) · 🇬🇧 [English](#english)

---

## Français

Plugin **Paper 26.x** qui donne à **chaque joueur son propre butin** dans les coffres générés
naturellement (donjons, villages, temples, mineshafts…), à la manière du mod Lootr.
Plusieurs joueurs peuvent ouvrir le même coffre : chacun y trouve un contenu tiré au sort rien que pour lui.

### Fonctionnement

- Un conteneur généré par le monde possède une **table de loot** tant qu'il n'a pas été ouvert.
  Le plugin mémorise cette table dans le bloc puis la retire : le vrai coffre reste vide.
- À l'ouverture, chaque joueur reçoit un **inventaire personnel** tiré depuis la table d'origine,
  sauvegardé en base SQLite. Ce qu'il laisse dedans l'attend à sa prochaine visite.
- Les **coffres posés par des joueurs ne sont jamais concernés** (ils n'ont pas de table de loot,
  et sont en plus marqués à la pose).
- Coffres, coffres piégés, tonneaux et wagonnets à coffre sont pris en charge (configurable).
- Protection contre la casse, les explosions et les entonnoirs.
- Apparence personnelle façon Lootr : chaque joueur voit le coffre « non ouvert » ou « déjà ouvert » selon ce qu'il a fait, avec vos propres textures.

### Prérequis

| Élément | Version |
|---|---|
| Serveur | Paper 26.1 ou plus récent |
| Java | 25 |
| Folia | Non pris en charge |

SQLite est téléchargé automatiquement par Paper au premier démarrage (`libraries` du `plugin.yml`).

### Installation

1. Téléchargez le `.jar` depuis les [Releases](../../releases) (ou l'onglet *Actions*).
2. Placez-le dans le dossier `plugins/` et redémarrez le serveur.
3. Ajustez `plugins/Randomize_Chest/config.yml` puis faites `/rchest reload`.

> Les coffres déjà ouverts avant l'installation du plugin ont perdu leur table de loot :
> ils restent des coffres normaux. Les nouveaux chunks et les coffres jamais ouverts sont convertis.

### Commandes et permissions

| Commande | Description |
|---|---|
| `/rchest reload` | Recharge la configuration |
| `/rchest info` | Infos sur le conteneur visé (table, nombre de joueurs) |
| `/rchest reset [joueur]` | Réinitialise le butin du conteneur visé (tous ou un joueur) |
| `/rchest scan` | Convertit les conteneurs des chunks chargés |
| `/rchest visuals refresh` | Recrée les habillages visuels dans les chunks chargés |

Alias : `/randomizechest`, `/rchest`.

| Permission | Défaut | Rôle |
|---|---|---|
| `randomizechest.use` | tous | Ouvrir les coffres à butin |
| `randomizechest.break` | op | Casser un coffre à butin en étant accroupi |
| `randomizechest.admin` | op | Commandes d'administration |

### Configuration (extrait)

```yaml
loot:
  seed-mode: random   # random | per-player | shared
  refill-hours: 0     # régénération du butin après X heures (0 = jamais)
protection:
  break: true
  explosions: true
  hoppers: true
visual:
  enabled: false
  item-model: "randomize_chest:loot_chest"               # pas encore ouvert
  opened-item-model: "randomize_chest:loot_chest_opened" # déjà ouvert
  per-block:                                             # modèles par type de bloc
    BARREL:
      item-model: "randomize_chest:loot_barrel"
      opened-item-model: "randomize_chest:loot_barrel_opened"
```

### Textures « non ouvert / déjà ouvert »

Comme avec Lootr, l'apparence d'un coffre à butin dépend du joueur qui le regarde :
un joueur qui ne l'a jamais ouvert voit le modèle `item-model`, un joueur qui l'a déjà ouvert
voit `opened-item-model`. Deux joueurs devant le même coffre peuvent donc voir deux textures différentes.

- Chaque conteneur porte deux `ItemDisplay` superposés ; le plugin montre à chaque joueur le bon
  grâce à la visibilité d'entités par joueur de Paper (aucun paquet modifié, aucune dépendance).
- L'état est mis à jour dès l'ouverture, retrouvé à la reconnexion, remis à zéro par `/rchest reset`
  et par la régénération (`refill-hours`).
- Les tonneaux sont gérés aussi, avec leurs propres modèles (`per-block`).
- Quand l'habillage est activé, l'animation vanilla du couvercle est désactivée.

Un exemple de resource pack complet (6 modèles, textures de test) se trouve dans
[`resourcepack-exemple/`](resourcepack-exemple/). Activez ensuite `visual.enabled: true` et faites `/rchest reload`.

### Compiler

```bash
gradle build
# build/libs/Randomize_Chest-<version>.jar
```

### Publier une version

1. Changez `version` dans `build.gradle.kts`.
2. Ajoutez une section `## [x.y.z]` correspondante dans `CHANGELOG.md`.
3. Poussez sur `main` : l'action GitHub compile le plugin, crée le tag `vx.y.z` et publie la release avec le `.jar` et les notes du changelog.

### Licence

Ce projet est distribué sous licence [GNU GPL v3.0](LICENSE) : vous pouvez l'utiliser, le modifier et le redistribuer, à condition de conserver la même licence et de mentionner l'origine du code.

---

## English

**Paper 26.x** plugin that gives **every player their own loot** in naturally generated containers
(dungeons, villages, temples, mineshafts…), similar to the Lootr mod.
Several players can open the same chest: each one finds contents rolled just for them.

### How it works

- A world-generated container holds a **loot table** until it is opened.
  The plugin stores that table in the block and removes it: the real chest stays empty.
- When opened, each player gets a **personal inventory** rolled from the original table and saved
  in SQLite. Whatever they leave inside is still there on their next visit.
- **Player-placed chests are never affected** (they have no loot table, and are also tagged on placement).
- Chests, trapped chests, barrels and chest minecarts are supported (configurable).
- Protection against breaking, explosions and hoppers.
- Lootr-style per-player look: each player sees the chest as "unopened" or "already opened" based on their own actions, with your own textures.

### Requirements

| Item | Version |
|---|---|
| Server | Paper 26.1 or newer |
| Java | 25 |
| Folia | Not supported |

SQLite is downloaded automatically by Paper on first start (`libraries` in `plugin.yml`).

### Installation

1. Download the `.jar` from [Releases](../../releases) (or the *Actions* tab).
2. Drop it into `plugins/` and restart the server.
3. Adjust `plugins/Randomize_Chest/config.yml` then run `/rchest reload`.

> Chests opened before the plugin was installed have already lost their loot table:
> they stay regular chests. New chunks and never-opened chests are converted.

### Commands and permissions

| Command | Description |
|---|---|
| `/rchest reload` | Reload the configuration |
| `/rchest info` | Info about the targeted container (table, player count) |
| `/rchest reset [player]` | Reset the targeted container's loot (everyone or one player) |
| `/rchest scan` | Convert containers in loaded chunks |
| `/rchest visuals refresh` | Recreate visual skins in loaded chunks |

Aliases: `/randomizechest`, `/rchest`.

| Permission | Default | Purpose |
|---|---|---|
| `randomizechest.use` | everyone | Open loot chests |
| `randomizechest.break` | op | Break a loot chest while sneaking |
| `randomizechest.admin` | op | Admin commands |

### "Unopened / opened" textures

Like Lootr, a loot chest's look depends on the player looking at it: a player who has never
opened it sees the `item-model` model, a player who already has sees `opened-item-model`.
Two players in front of the same chest can see two different textures.

- Each container carries two stacked `ItemDisplay` entities; the plugin shows each player the right
  one through Paper's per-player entity visibility (no packet tampering, no dependency).
- The state updates as soon as the chest is opened, is restored on reconnect, and is reset by
  `/rchest reset` and by refills (`refill-hours`).
- Barrels are supported too, with their own models (`per-block`).
- While the visual skin is enabled, the vanilla lid animation is turned off.

A complete resource pack example (6 models, test textures) lives in
[`resourcepack-exemple/`](resourcepack-exemple/). Then set `visual.enabled: true` and run `/rchest reload`.

### Building

```bash
gradle build
# build/libs/Randomize_Chest-<version>.jar
```

### Publishing a version

1. Change `version` in `build.gradle.kts`.
2. Add a matching `## [x.y.z]` section to `CHANGELOG.md`.
3. Push to `main`: the GitHub Action builds the plugin, creates the `vx.y.z` tag and publishes the release with the `.jar` and the changelog notes.

### License

This project is released under the [GNU GPL v3.0](LICENSE): you may use, modify and redistribute it, as long as you keep the same license and credit the original code.
