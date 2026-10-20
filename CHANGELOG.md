# Changelog

## [1.1.0] — 2026-10-20

### 🇫🇷 Ajouté
- Apparence par joueur façon Lootr : un modèle « non ouvert » (`item-model`) et un modèle « déjà ouvert » (`opened-item-model`) ; chaque joueur voit celui qui correspond à ce qu'il a fait.
- Modèles par type de bloc (`visual.per-block`), avec des valeurs par défaut pour les coffres piégés et les tonneaux.
- Option `visual.yaw-offset` pour corriger l'orientation d'un modèle.
- Exemple de resource pack complet : 6 modèles (coffre, coffre piégé, tonneau × 2 états) et textures de test.
- Fichier `LICENSE` (GNU GPL v3.0), également inclus dans le `.jar`.

### 🇫🇷 Modifié
- Les habillages se recréent automatiquement au chargement des chunks et après `/rchest reload`.
- Les tonneaux habillés ne sont plus rendus sombres (lumière prise au-dessus du bloc).
- L'animation vanilla du couvercle est désactivée quand l'habillage est actif.
- Les nouvelles options sont ajoutées automatiquement à un `config.yml` existant.
- Les habillages de la version 1.0.0 sont remplacés automatiquement.

### 🇬🇧 Added
- Lootr-style per-player look: an "unopened" model (`item-model`) and an "opened" model (`opened-item-model`); each player sees the one matching their own actions.
- Per-block models (`visual.per-block`), with defaults for trapped chests and barrels.
- `visual.yaw-offset` option to fix a model's orientation.
- Complete resource pack example: 6 models (chest, trapped chest, barrel × 2 states) and test textures.
- `LICENSE` file (GNU GPL v3.0), also bundled in the `.jar`.

### 🇬🇧 Changed
- Visual skins are recreated automatically when chunks load and after `/rchest reload`.
- Skinned barrels are no longer rendered dark (light is sampled above the block).
- The vanilla lid animation is turned off while the visual skin is active.
- New options are added automatically to an existing `config.yml`.
- Visual skins from version 1.0.0 are replaced automatically.

## [1.0.0] — 2026-10-19

### 🇫🇷 Ajouté
- Butin personnel par joueur dans les conteneurs générés naturellement (coffres, coffres piégés, tonneaux).
- Prise en charge des wagonnets à coffre (mineshafts).
- Les conteneurs posés par des joueurs sont exclus ; un coffre posé ne peut pas former de double coffre avec un coffre à butin.
- Sauvegarde SQLite des inventaires personnels (écritures asynchrones).
- Modes de tirage : `random`, `per-player`, `shared`, et régénération optionnelle après X heures.
- Protection contre la casse, les explosions et les entonnoirs.
- Animation d'ouverture du couvercle.
- Habillage visuel optionnel via ItemDisplay et modèle de resource pack, avec un exemple de pack.
- Commandes `/rchest reload | info | reset | scan | visuals refresh`.

### 🇬🇧 Added
- Per-player loot in naturally generated containers (chests, trapped chests, barrels).
- Chest minecart support (mineshafts).
- Player-placed containers are excluded; a placed chest cannot form a double chest with a loot chest.
- SQLite storage for personal inventories (asynchronous writes).
- Roll modes: `random`, `per-player`, `shared`, plus optional refill after X hours.
- Protection against breaking, explosions and hoppers.
- Lid opening animation.
- Optional visual skin through ItemDisplay and a resource pack model, with an example pack.
- `/rchest reload | info | reset | scan | visuals refresh` commands.
