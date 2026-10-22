# Pack de ressources / Resource pack

🇫🇷 [Français](#français) · 🇬🇧 [English](#english)

---

## Français

Chaque type de conteneur a **deux apparences** : une pour les joueurs qui ne l'ont pas encore ouvert,
une pour ceux qui l'ont déjà ouvert. Chaque joueur voit la sienne.

Ce pack est inclus dans le plugin et extrait dans `plugins/Randomize_Chest/resourcepack/`.
Avec `resource-pack.mode: builtin`, le plugin l'envoie lui-même aux joueurs ; avec `disabled`,
donnez ce dossier (ou `Randomize_Chest-pack.zip`) à votre outil de fusion (RSPM...).
Après avoir modifié une texture, faites `/rchest reload` : le pack est reconstruit et renvoyé.

### Coffres et coffres piégés : textures au format vanilla

Les coffres reprennent la **forme du coffre vanilla**, en deux parties (corps + couvercle animé) : leurs textures ont exactement le même format
que `textures/entity/chest/normal.png` de Minecraft (image **64×64** contenant le couvercle, le corps
et la serrure). Partez de la texture vanilla et modifiez-la.

| Fichier (dans `assets/randomize_chest/textures/entity/chest/`) | Affiché aux joueurs qui… |
|---|---|
| `loot_chest.png` | n'ont pas encore ouvert le coffre |
| `loot_chest_opened.png` | l'ont déjà ouvert |
| `loot_trapped_chest.png` | n'ont pas encore ouvert le coffre piégé |
| `loot_trapped_chest_opened.png` | l'ont déjà ouvert |

### Tonneaux : textures 16×16

Les tonneaux utilisent un cube simple : une image **16×16** appliquée sur chaque face.

| Fichier (dans `assets/randomize_chest/textures/item/`) | Affiché aux joueurs qui… |
|---|---|
| `loot_barrel.png` | n'ont pas encore ouvert le tonneau |
| `loot_barrel_opened.png` | l'ont déjà ouvert |

### Bon à savoir

- Les textures fournies sont de **simples textures de test** (couleurs unies) à remplacer.
- Une mise à jour du plugin n'écrase jamais vos fichiers `.png`.
- Si un coffre apparaît tourné à l'envers, réglez `visual.yaw-offset: 180` dans `config.yml`.
- Le `pack.mcmeta` fourni accepte les versions 26.1 et suivantes (`min_format` 84).
- Le couvercle s'anime de façon fluide à l'ouverture et à la fermeture : il pivote autour de sa charnière, animé directement par le jeu. Les modèles `models/item/chest/` reproduisent la forme du coffre vanilla : ne modifiez que les textures.

---

## English

Each container type has **two looks**: one for players who have not opened it yet,
one for players who already have. Each player sees their own.

This pack is bundled in the plugin and extracted to `plugins/Randomize_Chest/resourcepack/`.
With `resource-pack.mode: builtin`, the plugin sends it to players itself; with `disabled`,
give this folder (or `Randomize_Chest-pack.zip`) to your merging tool (RSPM...).
After changing a texture, run `/rchest reload`: the pack is rebuilt and sent again.

### Chests and trapped chests: vanilla-format textures

Chests reproduce the **vanilla chest shape**, in two parts (body + animated lid): their textures use exactly the same format as Minecraft's
`textures/entity/chest/normal.png` (a **64×64** image holding the lid, body and latch).
Start from the vanilla texture and edit it.

| File (in `assets/randomize_chest/textures/entity/chest/`) | Shown to players who… |
|---|---|
| `loot_chest.png` | have not opened the chest yet |
| `loot_chest_opened.png` | already opened it |
| `loot_trapped_chest.png` | have not opened the trapped chest yet |
| `loot_trapped_chest_opened.png` | already opened it |

### Barrels: 16×16 textures

Barrels use a plain cube: one **16×16** image applied to every face.

| File (in `assets/randomize_chest/textures/item/`) | Shown to players who… |
|---|---|
| `loot_barrel.png` | have not opened the barrel yet |
| `loot_barrel_opened.png` | already opened it |

### Good to know

- The provided textures are **plain test textures** (solid colours) meant to be replaced.
- A plugin update never overwrites your `.png` files.
- If a chest shows up facing backwards, set `visual.yaw-offset: 180` in `config.yml`.
- The provided `pack.mcmeta` accepts 26.1 and later (`min_format` 84).
- The lid opens and closes smoothly: it pivots around its hinge, animated by the game itself. The `models/item/chest/` models reproduce the vanilla chest shape: only edit the textures.
