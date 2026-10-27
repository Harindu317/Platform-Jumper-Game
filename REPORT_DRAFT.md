# JUMPBRO — IT22013 Assignment Report Draft

**University:** General Sir John Kotelawala Defence University  
**Module:** IT22013 Data Structures and Algorithms  
**Game:** JUMPBRO  
**Group members:** Add names and student IDs in a separate table before submission.

## 1. Introduction
JUMPBRO is a 2D platform game implemented in Java using Swing and AWT. The player moves across platforms, collects coins, avoids hazards and reaches a finish flag. The project demonstrates data structures and algorithms in a practical interactive program.

## 2. Game Concept and Objectives
The objective is to finish progressively challenging levels while collecting coins and keeping at least one life. The game includes a main menu, keyboard controls, pause/resume, score and coin counters, lives, and end screens. Anyone can play immediately without requiring an account.

## 3. Design Overview and Architecture
`Main` starts the Swing application. `GameFrame` owns the window and switches between screens. `MenuPanel` presents navigation. `GamePanel` runs a Swing Timer, reads keyboard input, updates game state, detects collisions, and draws the scene. `Player`, `Platform`, `Coin`, and `Obstacle` model game objects. `Level` contains level layouts. `GameEventManager` processes events. `ProgressManager` saves and loads player level unlock progression and high scores.

Data flow: keyboard input -> player update -> collision detection -> queue events -> event processing -> score/lives/level changes -> repaint.

## 4. Data Structures Used
**Arrays:** Each `Level` stores platforms, coins and obstacles in arrays. Arrays provide indexed access and are straightforward for fixed level layouts. Access by index is O(1); scanning all objects is O(n).

**Queue:** `GameEventManager` uses `Queue<GameEvent>` with a `LinkedList` implementation. `offer()` adds an event, `poll()` removes the oldest event, and `isEmpty()` checks whether events remain. This is FIFO (First In, First Out). Queue insertion/removal is typically O(1).

## 5. Sorting and Searching Algorithms
**Bubble Sort:** `Leaderboard.bubbleSort()` compares adjacent records and swaps them when the earlier score is lower, producing descending score order. Whole `Record` objects are swapped, so player names remain paired with scores. Worst/average time complexity O(n²); best case O(n) with the early-stop flag; extra space O(1).

**Linear Search:** `searchByName()` checks records one by one, ignoring name case. `searchByScore()` checks for an exact score. If no match is found, the methods return `null`. Time complexity O(n), extra space O(1).

## 6. Recursive Function
`Level.levelsRemaining(int completedLevel)` has a base case: when `completedLevel >= 15` it returns 0, meaning every level is finished. Otherwise it returns `1 + levelsRemaining(completedLevel + 1)`. The game uses it after each level to decide between "next level unlocked" and "all 15 levels complete". The recursion is bounded by the 15 levels and does not run the game loop recursively. Time and stack space are O(k), where k is the number of levels still to play (at most 15).

## 7. Implementation Details
The game uses a Swing `Timer` to update at approximately 16 ms intervals. The player has horizontal movement, gravity, and a jump trigger that only fires while grounded. Platform collision checks the player's downward crossing of platform tops. Coins are marked collected before a queue event is enqueued, preventing duplicate awards. Obstacles are marked triggered before damage events are enqueued. The camera follows the player horizontally. The leaderboard is stored in a text file under the user's home directory.

## 8. Testing Methodology and Test Cases
**These are proposed tests; do not mark them as passed until the group runs them locally.**

| Test | Procedure | Expected result |
|---|---|---|
| Launch | Run `platformjumper.Main` | Main menu appears |
| Start | Click Start Game | Level 1 appears |
| Movement | Press A/D and arrows | Character moves horizontally |
| Jump | Press Space while grounded | Character jumps and falls under gravity |
| No air-jump | Press jump repeatedly while airborne | No additional jump begins before landing |
| Coin | Touch an uncollected coin | Coin disappears and score increases once |
| Hazard | Touch any hazard (spike, cactus, bird, asteroid, laser, UFO, crystal, meteor, alien) | One life is lost; player respawns at the last checkpoint |
| Pit | Fall below the screen | One life is lost and player respawns |
| Pause | Press P, then P again | Gameplay freezes, then resumes |
| Level completion | Reach the finish flag | Next level loads, or victory appears after Level 15 |
| Leaderboard | Finish or lose and enter a name | Score is saved locally |
| Sorting | Add multiple different scores | Highest score appears first |
| Search | Search existing and missing names | Match appears, or clear no-match message |
| Restart/menu | Press Enter or M at end screen | Game restarts or returns to menu |

## 9. Output Screenshots to Capture
Capture your own screenshots after running the project:
1. Main menu.
2. Level 1 with score/lives HUD and coins.
3. Level 2 showing increased difficulty.
4. Level 3 and finish flag.
5. Paused screen.
6. Game Over screen.
7. Victory screen.
8. Leaderboard with sorted scores and a search result.

Do not insert fabricated screenshots.

## 10. Challenges and Solutions
Potential challenges include preventing repeated collision events, keeping player records paired with scores during sorting, making keyboard focus reliable, and ensuring level progression does not recurse indefinitely. The implementation marks coins/hazards before enqueueing events, swaps whole records, requests keyboard focus when entering gameplay, and uses a base case for recursion. Confirm these behaviors during local testing.

## 11. Conclusion
JUMPBRO combines a playable Java Swing game with arrays, a FIFO queue, and recursion. It offers a concrete demonstration of how DSA concepts can support game state management and event processing. The group should run the test plan, record actual outcomes, capture screenshots and update this report before submission.

## 12. References
- General Sir John Kotelawala Defence University, *IT22013 Data Structures and Algorithms — Group Assignment Specification*, Intake 42, uploaded assignment handout.
- Oracle Java documentation: Swing and AWT APIs, https://docs.oracle.com/javase/
