# Changelog

## [1.4.0] — 2026-10-22

### 🇫🇷 Ajouté
- Animation du couvercle des coffres et coffres piégés habillés : 10 images sur 0,5 seconde, avec la même accélération que le couvercle vanilla. Chaque joueur voit le couvercle s'ouvrir avec sa propre texture (non ouvert / déjà ouvert).

### 🇫🇷 Modifié
- Les modèles du pack déjà extrait sont mis à jour automatiquement pour l'animation ; vos textures `.png` ne sont pas touchées.
- Un couvercle resté ouvert après un arrêt brutal du serveur est refermé au chargement.

### 🇬🇧 Added
- Lid animation for skinned chests and trapped chests: 10 frames over 0.5 seconds, with the same easing as the vanilla lid. Each player sees the lid open with their own texture (unopened / opened).

### 🇬🇧 Changed
- Models in the already extracted pack are updated automatically for the animation; your `.png` textures are left untouched.
- A lid left open after a server crash is closed again when it loads.

## [1.3.1] — 2026-10-22

### 🇫🇷 Corrigé
- Les sons d'ouverture et de fermeture des coffres et des tonneaux sont de nouveau joués quand l'habillage visuel est activé.

### 🇬🇧 Fixed
- Chest and barrel open/close sounds are played again while the visual skin is enabled.

## [1.3.0] — 2026-10-22

### 🇫🇷 Modifié
- Les coffres et coffres piégés à butin utilisent désormais le rendu du coffre vanilla : leurs textures ont le même format que celles de Minecraft (64×64, `textures/entity/chest/`). Une texture de coffre vanilla modifiée fonctionne telle quelle.
- Les habillages sont légèrement agrandis pour recouvrir le bloc sans scintillement.
- Les habillages existants sont recréés automatiquement avec la nouvelle orientation.
- Les modèles du pack déjà extrait sont mis à jour automatiquement ; vos textures `.png` ne sont jamais écrasées.

### 🇫🇷 Supprimé
- Anciens modèles en cube et textures 16×16 des coffres et coffres piégés (supprimés du pack extrait lors de la mise à jour).

### 🇬🇧 Changed
- Loot chests and trapped chests now use the vanilla chest renderer: their textures use the same format as Minecraft's (64×64, `textures/entity/chest/`). An edited vanilla chest texture works as is.
- Visual skins are slightly enlarged to cover the block without flickering.
- Existing visual skins are recreated automatically with the new orientation.
- Models in the already extracted pack are updated automatically; your `.png` textures are never overwritten.

### 🇬🇧 Removed
- Old cube models and 16×16 textures for chests and trapped chests (removed from the extracted pack during the update).

## [1.2.0] — 2026-10-20

### 🇫🇷 Ajouté
- Pack de ressources inclus dans le `.jar`, extrait dans `plugins/Randomize_Chest/resourcepack/` sans écraser les fichiers existants.
- Serveur web intégré (`resource-pack.mode: builtin`) qui envoie le pack aux joueurs à la connexion, en plus des autres packs du serveur.
- Mode `disabled` pour les serveurs qui fusionnent leurs packs (RSPM...) ; le zip `Randomize_Chest-pack.zip` reste généré.
- Options `port`, `bind`, `public-host`, `public-port`, `required` et `prompt`.
- Commande `/rchest pack [resend]` (lien, SHA-1, taille, renvoi).
- Avertissement en console quand un joueur n'arrive pas à télécharger le pack.

### 🇫🇷 Modifié
- `/rchest reload` reconstruit le pack et le renvoie aux joueurs s'il a changé.
- Le dossier `resourcepack-exemple/` du dépôt devient `resourcepack/`, avec un `pack.mcmeta` pour 26.1+.

### 🇬🇧 Added
- Resource pack bundled in the `.jar`, extracted to `plugins/Randomize_Chest/resourcepack/` without overwriting existing files.
- Built-in web server (`resource-pack.mode: builtin`) that sends the pack to players on join, on top of the server's other packs.
- `disabled` mode for servers that merge their packs (RSPM...); the `Randomize_Chest-pack.zip` file is still generated.
- `port`, `bind`, `public-host`, `public-port`, `required` and `prompt` options.
- `/rchest pack [resend]` command (link, SHA-1, size, resend).
- Console warning when a player fails to download the pack.

### 🇬🇧 Changed
- `/rchest reload` rebuilds the pack and sends it again to players if it changed.
- The repository's `resourcepack-exemple/` folder becomes `resourcepack/`, with a `pack.mcmeta` for 26.1+.

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
