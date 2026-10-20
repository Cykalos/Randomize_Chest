# Exemple de resource pack / Resource pack example

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

1. Copiez le dossier `assets/randomize_chest` dans votre resource pack (compatible avec une fusion RSPM).
2. Remplacez les textures dans `assets/randomize_chest/textures/item/`. Les PNG fournis sont de
   **simples textures de test** (unies, bordure dorée = non ouvert, grise = ouvert) pour vérifier le
   fonctionnement en jeu.
3. Vous pouvez remplacer les modèles par les vôtres (Blockbench). Les modèles `_opened` héritent
   de la géométrie du modèle de base et ne changent que la texture : supprimez la ligne `parent`
   si vous voulez une géométrie différente (couvercle entrouvert, par exemple).
4. Utilisez le `pack.mcmeta` de votre pack existant.
5. Dans `config.yml`, passez `visual.enabled: true`, puis `/rchest reload`.

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

1. Copy the `assets/randomize_chest` folder into your resource pack (works with an RSPM merge).
2. Replace the textures in `assets/randomize_chest/textures/item/`. The provided PNGs are
   **plain test textures** (gold border = unopened, grey = opened) to check that everything works in game.
3. You may replace the models with your own (Blockbench). The `_opened` models inherit the base
   model's geometry and only swap the texture: remove the `parent` line for a different geometry
   (a half-open lid, for example).
4. Use the `pack.mcmeta` from your existing pack.
5. In `config.yml`, set `visual.enabled: true`, then run `/rchest reload`.

### Model tips

- The chest's front matches the **north** face in Blockbench.
  If the orientation is off, adjust `visual.yaw-offset` (in steps of 90).
- The model must be **slightly larger** than the vanilla block to fully cover it
  (chest: 0.8 → 15.2, height 14.2; barrel: -0.05 → 16.05).
- With the visual skin enabled, the vanilla lid animation is turned off so it does not poke through the model.
