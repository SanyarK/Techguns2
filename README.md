# Techguns2

1.12.2 Port/Rewrite of Techguns mod for Minecraft. WIP

Techguns 2 is still an early version and not well tested. Bugs may happen. Use at own risk.

Mod Information: https://github.com/pWn3d1337/Techguns2/wiki

## Military expansion

New content added on top of Techguns 2:

* **New NPCs**: Elite Soldier (T3 combat armor, SCAR/AUG/Vector/AS50), Heavy Soldier (riot armor, minigun/rocket launcher/flamethrower), Paratrooper (glides down on a parachute), Mutant Warrior (fast melee super mutant). Elite/Heavy soldiers and Mutant Warriors spawn naturally far away from the world spawn (danger level 3).
* **Bosses** (boss bar, no despawn):
  * **General** - calls in elite soldier reinforcements, switches to a rocket launcher at half health and orders airstrikes on his target when outdoors.
  * **Mutant Warlord** - giant super mutant, smashes the ground, summons mutant warriors, goes berserk at low health.
* **Attack Jet** - flies attack runs, strafes with its cannon, fires rockets when passing over its target and drops paratroopers. Random **air raids** attack players far away from spawn (configurable in the "NPC Spawn" config category).
* **Structures** (config `SpawnMilitaryExpansionStructures`):
  * **Underground Military Mine** (big) - guarded mine head, shaft to a level with barracks, armory and a mining gallery, a staircase down to the General's command center.
  * **Airfield** (big) - runway, hangar with a parked jet, control tower, barracks, fuel depot, jets take off from here.
  * **Bunker Complex** (medium) - guarded entrance, underground hall, armory and dormitory.
  * **Mutant Lair** (medium) - ruined crater with the Mutant Warlord and his warriors.
  * Existing military bases now also contain elite and heavy soldiers.
* **Military Contracts** (quests) - found in the new structures and dropped by bosses, or crafted from paper, an ink sac and a gold ingot. Right click to get a random assignment (kill soldiers, mutants, undead, aircraft, a boss or any hostiles), kills are counted while the contract is in your inventory. Right click a completed contract to get loot, experience and combat buffs (tier I-III).
* **Testing**: operators can place the new structures around themselves with `/tgstructure <underground_mine|airfield|bunker|mutant_lair>` and start an air raid with `/tgstructure airraid`.

## Building

Minecraft 1.12.2 / ForgeGradle 2.3 needs **Java 8** and **Gradle 4.10.3**: `gradle build`, the mod jar is in `build/libs`. The GitHub Actions workflow in `.github/workflows/build.yml` builds the jar on every push and uploads it as the `techguns-mod-jar` artifact.
