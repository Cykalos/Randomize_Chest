# Pack de ressources / Resource pack

🇫🇷 [Français](#français) · 🇬🇧 [English](#english)

---

## Français

Chaque type de conteneur a **deux modèles** : un pour les joueurs qui ne l'ont pas encore ouvert,
un pour ceux qui l'ont déjà ouvert. Chaque joueur voit le sien.

| Bloc | Non ouvert | Déjà ouvert |
|---|---|---|
| Coffre | `loot_chest` | `loot_chest_opened` |
| Coffre piégé | `loot_trapped_chest` | `loot_trapped_chest_opened` |
| Tonneau | `loot_barrel` | `loot_barrel_opened` |

### Utilisation

1. Ce pack est inclus dans le plugin et extrait dans `plugins/Randomize_Chest/resourcepack/`.
   Avec `resource-pack.mode: builtin`, le plugin l'envoie lui-même aux joueurs ; avec `disabled`,
   donnez ce dossier (ou `Randomize_Chest-pack.zip`) à votre outil de fusion (RSPM...).
2. Remplacez les textures dans `assets/randomize_chest/textures/item/`. Les PNG fournis sont de
   **simples textures de test** (unies, bordure dorée = non ouvert, grise = ouvert) pour vérifier le
   fonctionnement en jeu.
3. Vous pouvez remplacer les modèles par les vôtres (Blockbench). Les modèles `_opened` héritent
   de la géométrie du modèle de base et ne changent que la texture : supprimez la ligne `parent`
   si vous voulez une géométrie différente (couvercle entrouvert, par exemple).
4. Le `pack.mcmeta` fourni accepte les versions 26.1 et suivantes (`min_format` 84).
5. Dans `config.yml`, passez `visual.enabled: true`, puis `/rchest reload` (le pack est reconstruit et renvoyé).

### Conseils pour vos modèles

- La face avant du coffre correspond à la face **north** dans Blockbench.
  Si l'orientation ne convient pas, ajustez `visual.yaw-offset` (par pas de 90).
- Le modèle doit être **légèrement plus grand** que le bloc vanilla pour le recouvrir entièrement
  (coffre : 0.8 → 15.2, hauteur 14.2 ; tonneau : -0.05 → 16.05).
- Avec l'habillage activé, l'animation vanilla du couvercle est désactivée pour ne pas traverser le modèle.

---

## English

Each container type has **two models**: one for players who have not opened it yet,
one for players who already have. Each player sees their own.

| Block | Unopened | Opened |
|---|---|---|
| Chest | `loot_chest` | `loot_chest_opened` |
| Trapped chest | `loot_trapped_chest` | `loot_trapped_chest_opened` |
| Barrel | `loot_barrel` | `loot_barrel_opened` |

### Usage

1. This pack is bundled in the plugin and extracted to `plugins/Randomize_Chest/resourcepack/`.
   With `resource-pack.mode: builtin`, the plugin sends it to players itself; with `disabled`,
   give this folder (or `Randomize_Chest-pack.zip`) to your merging tool (RSPM...).
2. Replace the textures in `assets/randomize_chest/textures/item/`. The provided PNGs are
   **plain test textures** (gold border = unopened, grey = opened) to check that everything works in game.
3. You may replace the models with your own (Blockbench). The `_opened` models inherit the base
   model's geometry and only swap the texture: remove the `parent` line for a different geometry
   (a half-open lid, for example).
4. The provided `pack.mcmeta` accepts 26.1 and later (`min_format` 84).
5. In `config.yml`, set `visual.enabled: true`, then run `/rchest reload` (the pack is rebuilt and sent again).

### Model tips

- The chest's front matches the **north** face in Blockbench.
  If the orientation is off, adjust `visual.yaw-offset` (in steps of 90).
- The model must be **slightly larger** than the vanilla block to fully cover it
  (chest: 0.8 → 15.2, height 14.2; barrel: -0.05 → 16.05).
- With the visual skin enabled, the vanilla lid animation is turned off so it does not poke through the model.
