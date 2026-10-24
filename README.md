# Platform Jumper — Enhanced Java Swing Edition

A 2D platform game for the KDU IT22013 Data Structures & Algorithms assignment. The updated edition includes a neon arcade main menu, a dedicated leaderboard screen with a top-three podium, searchable score table, gradients/parallax scenery, polished coins and platforms, and the original DSA demonstrations.

## Requirements
- JDK 17 or newer recommended.
- Visual Studio Code with **Extension Pack for Java** (or another Java IDE).
- No external libraries, APIs, images, or database required.

## Run in VS Code (Windows PowerShell)
1. Extract the ZIP file.
2. In VS Code, choose **File → Open Folder** and open `PlatformJumper`.
3. Open **Terminal → New Terminal** in the project root.
4. Run:

```powershell
javac -d out src/platformjumper/*.java
java -cp out platformjumper.Main
```

## Run in NetBeans
1. Create a Java Application project named `PlatformJumper`.
2. Add the `.java` files in `src/platformjumper` to the `platformjumper` package.
3. Set `platformjumper.Main` as the main class and run.

## Worlds and obstacles
The 15 levels are split into three worlds (one row each on the level-select screen). Each world has its own look, physics and hazards, and levels get longer and denser as you progress.

| Levels | World | Physics | Hazards |
|---|---|---|---|
| 1-5 | Earth (desert) | Normal gravity | Cacti, patrolling birds |
| 6-10 | Space | Floaty jumps | Bobbing asteroids, blinking lasers, patrolling UFOs |
| 11-15 | Moon | Low gravity (long, high jumps) | Crystals, falling meteors (with warning), crawling aliens |

Levels are longer than before, and flags along the way are checkpoints: after losing a life you respawn at the last flag you reached.

## Controls
- Left/Right arrows or A/D: move.
- Space or Up: jump.
- P or Escape: pause/resume.
- B, Backspace or the BACK button (top right): leave the level and return to level select.
- After Game Over/Victory: Enter restarts, M returns to menu.

## Data structures and algorithms
- Arrays/lists store level objects and leaderboard records.
- `GameEventManager` uses a queue to process game events in order.
- `Leaderboard.bubbleSort()` manually sorts scores from high to low.
- `LeaderboardPanel` search filters player names and score text by scanning records (Linear Search).
- `Level.levelsRemaining()` demonstrates recursive level progression (15 levels).
- Players must sign in (`UserManager`, salted PBKDF2 passwords, Linear Search by username). Progress and best scores are saved per account; levels unlock in order.

Scores are saved locally to `${user.home}/.platform-jumper-scores.txt`.
