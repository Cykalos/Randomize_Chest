# Changelog

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
