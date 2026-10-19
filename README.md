# Ramdomize_Chest

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
- Habillage visuel optionnel (ItemDisplay + modèle de resource pack) pour reconnaître les coffres à butin.

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
3. Ajustez `plugins/Ramdomize_Chest/config.yml` puis faites `/rchest reload`.

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

Alias : `/ramdomizechest`, `/rchest`.

| Permission | Défaut | Rôle |
|---|---|---|
| `ramdomizechest.use` | tous | Ouvrir les coffres à butin |
| `ramdomizechest.break` | op | Casser un coffre à butin en étant accroupi |
| `ramdomizechest.admin` | op | Commandes d'administration |

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
  item-model: "ramdomize_chest:loot_chest"
```

### Texture personnalisée

Un exemple de resource pack se trouve dans [`resourcepack-exemple/`](resourcepack-exemple/).
Le plugin pose un `ItemDisplay` portant le modèle `item-model` sur chaque coffre à butin.

### Compiler

```bash
gradle build
# build/libs/Ramdomize_Chest-<version>.jar
```

### Licence

GPL-3.0.

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
- Optional visual skin (ItemDisplay + resource pack model) to recognise loot chests.

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
3. Adjust `plugins/Ramdomize_Chest/config.yml` then run `/rchest reload`.

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

Aliases: `/ramdomizechest`, `/rchest`.

| Permission | Default | Purpose |
|---|---|---|
| `ramdomizechest.use` | everyone | Open loot chests |
| `ramdomizechest.break` | op | Break a loot chest while sneaking |
| `ramdomizechest.admin` | op | Admin commands |

### Custom texture

A resource pack example lives in [`resourcepack-exemple/`](resourcepack-exemple/).
The plugin places an `ItemDisplay` carrying the `item-model` model on each loot chest.

### Building

```bash
gradle build
# build/libs/Ramdomize_Chest-<version>.jar
```

### License

GPL-3.0.
