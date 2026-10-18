# Changelog

All notable changes to Sky2M are listed here, newest first.

## Unreleased

### Added
- Sky2M emblems can be pictures: emblems staff upload on the website are downloaded once (kept in `config/sky2m/emblems`) and drawn at text height wherever emblems show (chat, tab list, name tags, `/s2 cosmetics`), and the new Fish emblem (🐟) shows like the other symbols.
- Mining Commissions > Highlight Event (off by default): in the Dwarven Mines, outlines the goblins and golems of a Goblin Raid, and outlines and boxes the Raffle Tickets on the ground during a Raffle, only while that event is running. Both colours can be changed.
- Crystal Hollows Waypoints: Auto-Find From Nametags (off by default) marks King Yolkar, Odawa, Xalx, Corleone, the Key Guardian and the Jungle Temple as soon as their nametags load, and Share Finds In Party Chat (off by default) posts each place you find yourself once per lobby in party chat, where Sky2M and Skyblocker turn it into a waypoint.
- New installs start from Sky2M's recommended settings and HUD layout (bundled defaults) instead of everything off at the top-left. Only when there's no Sky2M config yet: existing players, and players carrying settings over from SkyBalls, keep theirs.
- Boss Spawn Alert (Slayers > Boss Fight, on by default): "BOSS SPAWNED" with your boss and tier (e.g. "Tarantula Broodfather V") and a sound the moment the sidebar says "Slay the boss!" for your quest, so the spawn isn't missed while AFK, even when the boss is off-screen or out of range; your boss's nametag appearing triggers it if the sidebar doesn't. When the boss is off-screen the subtitle says where it is ("→ to your right, 23 m"). Pick the sound (Anvil, Bell, Pling, Level Up, Wither Spawn, Dragon Growl) and volume, and optionally repeat it every 2 seconds until the boss is on-screen (up to 10 seconds).
- Rival picker in `/s2 leaderboard`: above a board's standings, "Closest above you" lists the 1-3 players ranked just above you with their value and how far ahead they are, each with a one-click [Rival] button to make them your rival on that board ([Rival ✓] to undo). When you're not on the board it shows the bottom of the list as "Closest to beat". It shows fewer players, or hides, when the screen is small.
- Weekly Rival Recap (Misc > Collections, on by default): the first time you're on SkyBlock each week after the weekly boards reset (Monday 00:00 UTC), chat says who gained more last week on each board you have a rival on, e.g. "Wheat: you +1.2M vs Grimbly +900k — you won by 300k". Click a board's name for its standings. Sky2M keeps the latest weekly standings it sees for your rivals' boards (checked every 30 minutes on SkyBlock) in `config/sky2m/rival-weekly.json`; with no data for the week, nothing is said.
- Rival HUD (Misc > Collections > Rival HUD, off by default; move it in /s2 gui): while you farm, mine, forage, fish or gather a collection, shows that collection's Sky2M board against your rivals on it — your rank and amount, theirs, and how far ahead or behind you are. It follows what you're doing: the block you break (crops; ores, gemstones, Mithril, Hard Stone, Tungsten, Umber, Glacite and other mining blocks; logs), the crop your farming tool is for, the fishing item you last caught while fishing, or the collection the Collection Tracker sees you gather, so farming Wheat shows the Wheat board and switching to Cactus shows Cactus. Hidden when you have no rival on that board; the standings refresh every 5 minutes while it's showing.
- Rival Alerts (Misc > Collections, next to Rival HUD, on by default; needs the Rival HUD on): a title and a sound when a rival passes you, or you pass a rival, on the board the Rival HUD is showing — at most once a minute per rival and board.
- Settings codes (`/s2 config export`, `/s2 config import [code]`, or the Export/Import Settings Code buttons in General): copy all your Sky2M settings as one code to share, and load someone's code from the clipboard after an [Apply] click in chat. Your current settings are backed up to `config/sky2m/config-backups` first (the last 10 are kept); versions seen, warning mutes and your cloud-sync slot aren't shared and are kept as they are.
- Performance Warnings (General, on by default): when one Sky2M HUD or in-world renderer takes over 1 ms a frame for 15 seconds in a row, chat names it with its cost, an [Open settings] button and [Don't warn again]. Once per feature per session.
- Flag Kicked Players (Party Finder Stats, next to Played Before, on by default): when someone you kicked before joins your Party Finder group or your party, chat warns "⚠ Name — you kicked them 2× before (last: 3 days ago)" with your note on them and [Kick] and [History] buttons. Kicks from the [KICK] buttons and `/p kick` now count too, once Hypixel says the player was removed.
- Auto-Note Kicks (Party Finder Stats, off by default): the first time you kick someone who joined your Party Finder group, a note "Kicked from M7 on Oct 18, 2026" is saved on them, unless they already have one.
- Sack value history (Misc > Sacks Value > Value History, on by default; needs Sacks Value): your sacks' total value saved once a day per profile (the latest value of the day) in `config/sky2m/sack-value-history.json`, and drawn as a graph with `/s2 sacks history` or the Open Value Graph button. The Networth History graph has a button to switch between the two.
- `/s2 selftest`: checks that NEU items, SkyHanni slayer drop lists and island graphs, item prices, the Sky2M server connection, location detection and the config loaded, with a pass/fail line and reason each, and a [Copy] button for bug reports (with your Sky2M and Minecraft versions).
- Error Reporter (General, on by default): when a Sky2M HUD, chat feature or in-world renderer hits an error, chat says "⚠ Sky2M: <feature> hit an error (…)" with [Report to staff], [Copy] and [Ignore]. Reporting asks first, then files a bug report (like `/s2 bugreport`, so staff can reply) with the error and stack trace, the `/s2 selftest` report, your Sky2M and Minecraft versions and recent game log errors. Once per error per session and at most one message every 2 minutes; `/s2 errors` lists this session's errors.
- HUD Overview (`/s2 huds`, or General > HUD Overview): every Sky2M HUD with whether it's on, its saved position and scale, and whether it's off-screen, with [Reset] (back to its default position and scale) and [Edit] (opens `/s2 gui`) per HUD, a search box and an Open /s2 gui button. Click a HUD's name for its settings. Searching a HUD's name in the settings finds HUD Overview. It sits in General > HUDs, a dropdown with an Edit HUDs (/s2 gui) button.
- T5 phase times (Slayers > Boss Fight > Slayer HUDs, off by default): for Tier 5 Tarantula and Tier 5 Revenant, the Boss Phase Display shows hits left while the nametag shows hits, the current phase's time, and each finished phase with the downtime between phases (while the boss's nametag is gone, e.g. Tarantula Broodfather to Conjoined Brood). T5 Phase Summary (off by default) says at the kill "T5 Tara: 41.2s (phase 1 18.1s · downtime 3.4s · phase 2 19.7s)". The Phase Timer HUD and its chat summary now list downtime too.
- Slayer RNG Meter Display (Slayers > Loot & Profit, off by default), ported from SkyHanni: a HUD for your quest's slayer with the selected RNG Meter drop, its progress ("Scythe Blade: 63.2% (XP / needed)") and about how many bosses are left. The drop and its needed XP are read from Maddox's RNG Meter menu (open it once), the progress from the "RNG Meter - … Stored XP" line after each boss, and it starts again from 0 when the drop comes. Saved per slayer in `config/sky2m/slayer-rng-meter.json`.
- Slayer profit trackers: a new "Slayer Pace" summary line (add it in the tracker's Summary Lines) with this session's bosses per hour and profit per boss, and your best session for that slayer (sessions with at least 5 bosses and 10 minutes active). Bests are saved in `config/sky2m/slayer-pace-bests.json`.
- Fairy Soul finder (Misc > Waypoints, Trackers & Sounds > Fairy Souls): outlines the Fairy Souls you haven't collected on your island using NEU's soul coordinates, SkyHanni style — the nearest ones you still need are highlighted, with distance labels, and they draw through walls. Clicking right on a soul marks it found (or not found again with another click) and so does Hypixel's pickup message; a HUD counts the island's souls ("Fairy Souls 45/81 — Hub") and the count is said in chat when you arrive. One set of found souls is saved for all profiles in `config/sky2m/fairy-souls.json`. `/s2 souls` lists every island, and `/s2 souls mark`, `unmark`, `markall`, `unmarkall` mark souls around you.
- Shopping list (/s2 shopping): keep what you still want to get — `/s2 shopping add <item> [amount]` (also `... bale 12` or `... ×24`), `/s2 shopping` shows the list in chat with a tick one, reset, and remove button per line, `/s2 shopping clear` empties it. With Auto Pickups on, what lands in your inventory goes into each entry's counter on its own and says when it's done. A HUD (Misc > Shopping List, move it in /s2 gui) shows what's still missing. Saved in `config/sky2m/shopping.json`.
- Rivals (`/s2 rival add <name> <board>`, `/s2 rival remove <name> [board]`, or click a player in `/s2 leaderboard`'s standings): a rival is set per board. On those boards they're highlighted in orange with how far ahead or behind you they are, and you're told in chat when one passes you.
- Title screen "S2" button: a small button beside Multiplayer (and the Hypixel button, when on) opens the Sky2M settings without joining a world; closing them goes back to the title screen.
- Leaderboard GUI: bare `/s2 leaderboard` opens a full-screen leaderboard laid out like `/s2 collections`. Collections, Slayer XP, Kill Times and Events tabs along the top, a search box, and category filters (Farming, Mining, Combat, Foraging, Fishing, Rift, Other) for collections. The boards are listed on the left with their item icons, and the picked board's standings are a Rank / Player / Value table on the right with your own row highlighted and your rank shown above it. Boards you've looked at open instantly the next time. `/s2 leaderboard <name>` and the other chat lookups still answer in chat as before.
- Profit trackers: click an item in the tracker (shown over your inventory or any menu) to change how many you have or remove it. The change applies to Session, Today and Total together and to today's `/s2 session`. In `/s2 session`, click a drop to edit or remove it for that day.
- Setup (`/s2 setup`): pick what you play (Dungeons, Slayers, Farming & Garden, Mining, Fishing, Foraging, Hunting, Diana & Mayors) and the areas you don't are switched off as a whole. Their settings are kept and come back when you turn the area on again, and each area has a Reset to Sky2M's defaults. The first time you join, a [Set up Sky2M] button is offered in chat.
- Performance (`/s2 perf`): which Sky2M HUDs, in-world renderers and chat listeners take the most time each frame, as time per frame and share of the frame, with calls per second and the slowest call. Updates every 5 seconds; click a HUD to find its settings.
- Weekly leaderboards: an All Time / This Week switch on `/s2 leaderboard`'s Collections and Slayer XP tabs shows what everyone gained since Monday (00:00 UTC), so newer players can compete too.
- Command aliases (`/s2 alias add <name> <command>`, `/s2 alias remove <name>`, `/s2 alias list`): your own shortcut commands, with anything typed after them added on (or put where `{args}` is). Built in: `/f1`-`/f7` and `/m1`-`/m7` join that Catacombs floor, `/e` the Entrance, and `/k1`-`/k5` that Kuudra tier (Basic, Hot, Burning, Fiery, Infernal); your own alias with the same name replaces them.
- Player notes (`/s2 note <player> <text>`, `/s2 notes`, `/s2 note remove <player>`): your own note on a player ("good M7 tank"), shown in chat when they join your party or Party Finder group, or talk in chat (at most once every 10 minutes per player).
- Party Finder history (Party Finder Stats > Played Before, on by default): when someone you've seen before joins your group, how many dungeon runs you played together (from Run History), how many finished, your best together, when you last played, and how often they joined your groups and you kicked them. `/s2 pfhistory` lists recent players; `/s2 pfhistory <name>` shows one, with your note.
- Keybinds (`/s2 keybinds`): every key Sky2M uses (its settings, its Controls and Command Keys macros) in one searchable table, with keys that something else also uses in red and what that is. Click a settings key to open it.
- Screenshot gallery (`/s2 screenshots`): your screenshots folder with previews, newest first. Pick one to upload it, copy or re-send its link in Sky2M chat (uploads are remembered), open the folder or the website's Screenshots page.
- `/s2 session`: a Copy button copies the day (or the open tracker) as text for Discord: total, profit per hour, playtime and each tracker with its top drops.
- Lobby Finder (`/s2 lobby`): the Sky2M players in your lobby (same Hypixel server), and who's elsewhere on your island in another lobby, each clickable to invite to your party. The Lobby Finder HUD (Sky2M Online > Friends & Who) keeps the list on screen. Only players who share their location with you show.
- Per-floor Party Finder requirements (`/s2 pfreq`): a table of F1-F7 and M1-M7 with their own slowest S+, minimum Catacombs level, secrets and magical power. With Per-Floor Requirements on (Party Finder Stats settings), auto kick uses the row for the floor you're queued for or in (the Party Finder menu also remembers Master Mode now); floors without their own use the usual requirements.
- `/s2 session` Days view: a chart of your daily profit over the last 30 days, with your best day in gold. Hover a bar for its date and profit, click it to open that day.
- Profit Goal (each profit tracker's Settings): a goal in millions for the period the tracker shows, with a progress line on the HUD and a title and chat message when you reach it.
- Shared inventories (`[inv]`) show what each item is worth in its tooltip and the whole inventory's value under it (Bazaar or lowest BIN, for the base item).
- Bug reports (`/s2 bugreport`) can attach game info: your Sky2M and Minecraft versions, where you are on SkyBlock and the latest warnings and errors from your game log (never chat, and without your user folder name). On by default for bug reports, with an Attach game info button to turn it off.
- Leap Menu: left-clicking with Spirit Leap or Infinileap in a dungeon opens the leap menu too, like right-clicking.
- Shared name tags (Misc > Visual > Own Name Tag): the line above your head is shared with other Sky2M users, who see it above you. Add your own Custom Text (up to 40 characters), and turn on Show Collection Leaderboard with a Collection ID to show that collection's amount and your placement.

### Changed
- Dungeon sim: the sidebar no longer starts with "killer560's personal testing sim" or ends with a Discord invite, and the tab list's header says "Dungeon Sim" with no Discord link (it still says it's a local practice world, not Hypixel).
- Name tag above your head: the leaderboard now has its own line at the top, then networth and skill average, then your name. Pick the board by right-clicking it in `/s2 leaderboard` (collections, slayer XP or kill times; right-click again to stop; the board shows an aqua ⬆ in the list) instead of typing a Collection ID, which is gone from the settings. The line is gold, silver or bronze when you're #1-#3, aqua otherwise. Other Sky2M players' tags show the same two lines.
- What's New is no longer shown on a fresh install's first run, only after an update; the setting is now called Show What's New on Update. The last version you ran is kept in `config/sky2m/last-version.txt`, so importing settings can't hide or repeat it.
- `/s2 gui` position editor: the category and Done buttons sit together in a row at the top centre (wrapping onto more rows in a narrow window), with the title and help text under them. Clicks on a button always go to the button, even over a HUD.
- Rivals are per leaderboard: pick a rival on one board or several (e.g. Wheat and Cactus) with `/s2 rival add <name> <board>` (Tab completes the boards; `all` for every board) or by clicking them in `/s2 leaderboard` on the board that's open. The orange highlight, the ahead/behind numbers and the "passed you" messages only apply on those boards, and boards you have a rival on are orange in the list. `/s2 rival remove <name> [board]` drops one board or all of them, and `/s2 rival list` shows each rival's boards. In the standings, hovering a player shows which boards they're your rival on, and right-click removes them from every board. Rivals saved before stay rivals on all boards (the old file is kept as `rivals-v1.json`); clicking one on a board makes them your rival on just that board.
- Screenshots uploaded with the Sky2M host (the default) appear on shadowisabot.com/screenshots with your name and head. The mod gets a one-time token from your logged-in Sky2M connection first (waiting up to 6 seconds) and sends it with the upload, so the website knows it's yours.
- What's New (after an update, or `/s2 whatsnew`) also lists the changes and fixes since your last version under the new features, and is offered when an update only has fixes.
- `/s2 session`'s breakdown of the previous day is also shown when you play through midnight, not only when you start the game on a new day.
- `[item]` and `[inv]` in Hypixel chat show the real item: its reforge, recomb rarity, enchants, stars and skull, as if hovering over it (the sharer's Sky2M stores it on the Sky2M server; without the server it falls back to the plain item as before). Clicking an `[inv]` opens the full inventory with every item's real tooltip, and the rest of a message with an item keeps its colours.
- `/ahs <item>` and `/bzs <item>` (for example `/ahs grand`) search for it straight away instead of opening the search box; on their own they still open it.
- Command Autofill for `/bz`: suggestions are the items' Bazaar names (`enchanted diamond`), with a hyphen only where the name has one, instead of product IDs. IDs still match while typing.
- Party Finder Stats: the floor PBs follow the floor of the group you join or create in the Party Finder (F5/M5 when queued for M5), not F7/M7, and stats show sooner by asking both profile sources at once and taking the first answer.
- `/pv` loads other players faster: when SkyBlockPV is slow, Sky2M's proxy is asked after 2.5 seconds (was 5) and whichever answers first is shown, instead of waiting for SkyBlockPV to finish. SkyBlockPV's fuller data still replaces it for your next look. The networth history keeps using SkyBlockPV first.
- Chat Tabs are drawn under the command suggestions, like Current Chat Display, and clicks go to the suggestions while you type a command.

### Fixed
- Fix players with a Sky2M emblem or supporter symbol losing their head in SkyHanni's compact tab list: in the tab list the symbols now go after the name instead of in front of it.
- Custom Scoreboard's Sowdust line no longer jumps around while farming: Hypixel shortens it to "30.1M (+798)" while you gain sowdust, which was read as 30.1 with huge fake changes. It now keeps the exact amount (from the tab list when shown, else the last exact one) with Hypixel's gain beside it.
- Sweep Display no longer errors in `/s2 gui` before the tab list has shown your sweep (it says "Sweep: ?").
- Slayer cost "unknown" with instant restarts: Hypixel can take the spawn fee when the boss spawns rather than at "SLAYER QUEST STARTED!", so the purse is checked 3 seconds after the quest start and, if no fee showed, again 3 seconds after "SLAYER BOSS! ... spawned!". The per-kill "Profit" line uses the cost of the quest the boss belonged to, even when the next quest started in the same moment.
- Slayer cost "unknown": the purse is now also read from the sidebar's team lines, so it's found when a scoreboard mod (SkyHanni's custom scoreboard) takes the sidebar over, and lines starting with Hypixel's padding symbols still count. This also fixes mob kill coins in the slayer trackers and other features that read the purse or scoreboard values. A spawn fee Hypixel takes from your bank ("Took 100k coins from your bank...") now counts too, not only auto-slayer's: within 10 seconds of the quest starting. Each quest logs how its cost was worked out ("[Sky2M] Slayer quest cost: ...").
- A Sky2M feature that keeps failing (e.g. a HUD erroring every frame) no longer writes a log line every time: the first error is logged with its full stack trace, then only a count once a minute. A custom HUD that errors no longer shifts the HUDs drawn after it, and one that errors in `/s2 gui` no longer breaks the editor.
- Slayer Boss Profit (the "T5 Tara Profit" chat line and HUD) now agrees with the slayer profit trackers: only that boss's own slayer drops count (nothing until the drop lists load, shown as "Drop list not loaded" in the hover), items moved in or out of a Sack menu don't count, and the quest cost is what you actually paid or auto-slayer's bank charge ("unknown" when neither was seen) instead of a guessed standard price.
- Hit Colour with Only Entities You Hit: mobs hit by right-click weapons (Soul Whip, Flaming Flay...) now get the colour too. Those hit at range without an attack, so they kept the red flash; a mob hurt in front of you (within 20 blocks) in the 3 seconds after you right-click now counts as your hit.
- Slayer profit trackers now work like SkyHanni's: each counts while you have its slayer's quest and are in one of that slayer's areas (SkyHanni's areas, e.g. Spider Mound, Arachne's Burrow, Burning Desert for Tarantula), and only that slayer's own drops (SkyHanni's list), not other slayers' or mob items. Mob kill coins skip gains just after closing a menu (sales), the spawn cost is what you actually paid (Aatrox's discount included), auto-slayer's bank charge counts as the spawn cost, and a boss counts for every "SLAYER QUEST COMPLETE!" wherever you are. Items moved in or out of a Sack menu don't count.
- Rare drops: item names ending in brackets keep them (no more "Enchanted Book (Flash I"), so their price is found.
- Big Drop Animation: drops with no known price no longer say "is worth 0 coins!" when the threshold is 0.
- RNG Drop Totem Animation: dyes now play it.
- Rare drop settings: the Big Drop Animation's threshold and the RNG Drop Totem Animation are explained as separate options.
- Config migration: two settings ("What's New" acknowledged and the mouse lock) are migrated again; a stray condition above them skipped them unless an unrelated key also existed.
- Carry Trackers: a bad answer from the Elite leaderboards no longer disables the tracker and Elite ranks until the game restarts (the request retries after a minute).
- Diana rare mobs: cocooning your mob no longer tells the party "Defeated a <mob>!" a moment before the right "Cocooned a <mob>!".
- Hunting: invisibug highlights no longer keep showing for a moment after you leave Galatea or SkyBlock.
- Item Cooldowns: the ready sound no longer plays for timers that run out after you switch the feature off.
- Server Info: the PING line falls back to your tab-list ping like the rest of the HUD, instead of showing "--" after game start.
- `/s2 who`: the invisible "+Friend" button spot disappears for players whose account Sky2M can't see (or yourself).
- Scatha tracker: leaving the Crystal Hollows clears the "waiting for the burrow pet" state, so a later unrelated drop can't be credited to the old burrow.
- `/s2 leaderboard`: collection and slayer XP boards no longer say "the Sky2M server hasn't answered yet" when it had (fixed on the Sky2M server).
- `/s2 leaderboard`: only Hypixel's real collections are listed, with their proper names (Sunflower, Raw Cod, Spruce Log...). Duplicates such as Double Plant 1/4/5 and Red Rose 1/2 are gone (fixed on the Sky2M server).
- Collection icons in `/s2 leaderboard` and `/s2 collections`: Raw Salmon, Pufferfish, Tropical Fish, Cocoa Beans, Red Sand and the Spruce, Birch, Jungle and Dark Oak Log collections show their item instead of nothing (their old variant ids aren't SkyBlock items, so the vanilla item is used).
- Slayer kill times: Atoned Horror and Conjoined Brood are no longer separate boards. They're Tier 5 Revenant and Tarantula, so they count as Revenant Horror V and Tarantula Broodfather V (your saved times are merged, keeping the faster one), and `/s2 leaderboard` only lists the six slayer bosses.
- Farming RNG drop HUD: drops other players paste in chat ("Guild > Name: RARE CROP! Crystalized Moonlight") no longer show as yours; only Hypixel's own drop messages count.
- Fishing Profit (and every other profit tracker and `/s2 session`) no longer counts bait such as Spooky Bait or Glowy Chum Bait: taking bait out of your sacks or bait bag isn't profit. Bait already counted is removed from saved totals and past days.
- Bug reports, suggestions and feedback (`/s2 bugreport`, `/s2 suggest`, `/s2 feedback`) are sent again: the report's type overwrote the packet's own type, so the Sky2M server didn't recognise the report.
- `[inv]` showed the wrong owner (`[b2m3sbf's Inventory]` instead of `[2m3s's Inventory]`): colour codes are removed before reading the sender's name, which is checked against the tab list and, with the Sky2M server, taken from the sharer's login.
- Copy RNG Drops and the rare drop features only react to your own drops, not other players' drops announced in the Dungeon Hub: a line with another player's rank and name, "Name has obtained/found...", or the name of someone in your lobby is skipped.
- Copy Garden Drops now really controls crops: rare crops and other Garden drops are copied only when it's on. Copy Rare Drops and Copy RNG Drops used to copy them regardless.
- Slayer PBs: Tarantula personal bests are cleared once (on this update and on the Sky2M leaderboard), since times saved before the phase fix could be a single phase (a 3.1s "PB").
- Leaderboard GUI: collections and slayer XP lists load again. The Sky2M server now answers the mod's board-list request (it used to reply "Choose collection or slayer."), so the grouping under Farming/Mining/Combat/Foraging/Fishing works in the GUI and the `/s2 leaderboard collection` and `/s2 leaderboard slayer xp` chat browsing shows the boards again, without that confusing reply. The GUI also no longer sits on a permanent Loading when the server is slow: event boards, slayer kill times, the timeout note and Refresh keep working while the collection list is unavailable.
- Leaderboard GUI: when a standings answer doesn't come, the screen quietly asks the server again twice before falling back to the "hasn't answered" note, and the server now says so in its log when it receives and answers a stat leaderboard request. `/s2 lbdebug status` also shows what the GUI has actually received (board list, a collection/slayer standings answer and event standings, each with age).

## 1.0.2 — 2026-10-16

### Added
- Settings search and category filters: use `/s2 settings [filter]` or `/s2 gui <filter>` to find settings quickly. The HUD editor now has All, Mining, Dungeons, Combat, Pets, Trackers and Other filters, so you can focus on the HUDs you want to arrange.
- Session Tracker (`/s2 session`): one section per day instead of one per game session, with a Total tab and a tab for every profit tracker (all six slayers, Farming, Fishing, Foraging, Mining and Mythological Ritual). The Days list shows each day's total; click a day for its tracker breakdown. Daily history records the built-in profit trackers in the background even when their HUDs are off, keeps the day across game restarts, and recalculates values using the latest prices.
- Slayer Boss Profit: see the profit or loss after a slayer boss, with a breakdown of drops, their values, quest cost and net result, plus a HUD summary.
- Command Autofill improvements: more helpful in-game suggestions for supported Bazaar and auction/item commands, with normalized matching for spaces, underscores and hyphens.
- Permanent stat leaderboards: `/s2 leaderboard collection` browses collection rankings, `/s2 leaderboard collection <item>` shows a collection's top players, and `/s2 leaderboard slayer xp <boss>` shows slayer XP standings. Existing event leaderboards and slayer kill-time PB boards remain available.

### Changed
- Zealot Tracker now prefers Hypixel's TAB-list pity counter for “Since last eye” when it is available, keeping the counter aligned with Hypixel's value even when local entity tracking misses kills.
- Updated Zealot Tracker's eye-drop kill count handling so a Summoning Eye drop does not incorrectly reset the cumulative total-kills counter.
- Improved Party Finder Stats and related party commands, including the player-stat display and kick-list handling.
- Refined profit/session tracker presentation and tracking so session data is retained and displayed more consistently.

### Fixed
- Slayer timers/PBs: a missing boss nametag no longer counts as a kill. Phase summaries finish on Hypixel's confirmed boss-slain/cocoon message, and kill PB timing uses an observed 0-health line or the confirmed kill message. This prevents Tarantula's Conjoined Brood transition from recording a tiny false phase/PB; phase state resets when a new slayer quest starts or fails.
- Copy RNG Drops: rare crops and other Garden drops are no longer copied by the RNG-only toggle; use Copy Garden Drops for those.
- Item Pickup Log: sack entries use repository-matched item IDs for better icons and no longer have the `[Sack]` name prefix. Inventory changes are ignored while container menus are open, preventing false pickup/drop entries from menus such as the Experimentation Table.
- Bazaar Notifications: fully filled (100%) orders are removed from the outbid watcher instead of continuing to trigger outdated alerts.
- Hit Colour: fixed the backwards opacity scale: 0 now shows no flash and 100 the full colour.
- Sacks Value: stopped guessing Bazaar item IDs from unmatched sack names. A guessed ID could point to a different product and apply its price to the wrong item, inflating the total. Unknown items are now skipped rather than assigned a potentially unrelated price.
- Improved category filtering in the HUD editor and settings navigation to make enabled HUDs easier to find and manage.
- Enchanted Book Labels now show each single-enchantment book's numeric level in the bottom corner as well as its abbreviated name at the top.
- Item Protection now blocks protected items from clicks in identifiable NPC sell/shop/trade menus, in addition to blocking drops.
- Improved command suggestion handling for supported Hypixel commands to avoid replacing or suggesting the wrong argument text.

## 1.0.1 — 2026-10-12

### Added
- Crop Fever Effects (Farming > Garden > Crop Fever Effects), from SkyOcean: when you catch the CROP FEVER, a fanfare and a jackpot sound, a looping note block tune, the screen cycling through colours (six speeds) and coins raining down, until it's cured. /s2 cropfever start and stop to try them.
- Item List (Misc > Inventory & Storage > Item List), like SkyBlock Item List: every SkyBlock item, attribute, enchantment, mob, NPC, pet, potion and rune in a panel beside your inventory and SkyBlock menus. Search it (@pets wolf, a | b, a & b, #ID), filter it by type, and type sums in the search bar (2.5m*3, bz(ENCHANTED_DIAMOND)*64). Click an item, or press R over any item, for its recipes (crafting, Forge, Kat upgrades, NPC shops, mob drops); U for what uses it, Backspace to go back; right-click for its wiki page. F adds items and recipes to the Favourites panel on the other side, and Ctrl and click pins a favourite recipe at the top. Ctrl+O hides it. It moves its items out of the way of other panels and potion effects. Prices show in the items' tooltips. Off by default; the items come from the NotEnoughUpdates repo (about 22 MB, downloaded once).
- Custom Scoreboard (Misc > HUDs > Custom Scoreboard), like j10a1n15's Custom Scoreboard: your own sidebar, set up in its own editor with a live preview (the Open Editor button, or /s2 scoreboard). Pick which lines show and their order (title, lobby, date, time, island, area, purse, bank, motes, bits, copper, gems, powder, pet, party, objective, slayer and more), and an Events list in its own order that shows what's going on (a dungeon run, Kuudra, the Garden, contests, the Mining events, the tab list's event and more, with anything unknown kept under Other). Placed like the mod: right and centred by default, growing from there as lines come and go, or anywhere you drag it in /s2 gui. Number changes like "(+12,000)", five number layouts, your own colour and name for each number line (Purse, Bank, Bits...), short or long numbers, a rounded background with a four-colour border, a minimum size and margin, your own picture or GIF from config/sky2m/scoreboard, your own title and footer, and lines you can hover and click with chat open. Hypixel's sidebar is hidden while it's on.
- Ability Timers (Misc > Items & Tooltips > Ability Timers), from Killer560's Mod: up to five countdowns you name and time yourself, each started by its own key when you use the ability, in a HUD.
- Rag Axe (Dungeons > Timers and Alerts > Rag Axe), from Killer560's Mod: spots your Ragnarock Axe casts like Odin and times the channel, the Strength buff and the cooldown (less Mage's reduction), with alerts, the Strength you got, and "rag now" prompts before the moments of F5/M5 Livid, F7/M7 Storm and Necron, and M7's Wither King and dragon spawns, with NoammAddons' rag tune.
- Custom Mage Beam (Dungeons > Custom Mage Beam), from Killer560's Mod and NoammAddons: each Mage beam drawn as one clean beam in your colour and width, with the firework particles hidden.
- Terminal Timers (Dungeons > Timers and Alerts > Terminal Timers), from Odin via Killer560's Mod: in F7/M7 P3 every terminal, device and lever line gets its section and phase time, the section times and total are posted when the core opens, your own solve time for each terminal, and a HUD.
- Run History (Dungeons > Timers and Alerts > Run History), like Killer560's Mod: every dungeon run saved (floor, time, splits, P3 terminal times, score, secrets, crypts, deaths, class and party), a summary in chat, personal best announcements, and /s2 runs to look back through them by floor.
- Termism (/s2 termism), from Killer560's Mod: practise F7/M7 terminals on fake ones laid out like the real menus (every kind, Melody too), with a timer, your best time for each, and a solution you can turn on to learn them.
- Best Friends (Sky2M Online > Best Friends), from Killer560's Mod: how long you've partied with each player and how many dungeon runs you finished together on each floor, in the new Party Time tab of /s2 friends (or /s2 bestfriends).
- Party Dungeon Data (Dungeons > Party Dungeon Data), from Killer560's Mod: teammates running Sky2M share what they know about the run through the Sky2M server (the Mimic and Prince dead, blood, secrets, crypts, deaths, P3 terminals, room secrets, M7 dragons, Melody), so the score calculator counts a Mimic or Prince killed out of your sight. Off by default, with a HUD. Also one-time warnings when another mod does the same job as a Sky2M feature you have on.
- Team Melody (Dungeons > Team Melody), from Killer560's Mod: during F7/M7 P3, each teammate's Melody progress from their Sky2M, the Cross-Mod Bridge or Odin and NoammAddons users' "Melody 2/4" party messages; optionally send yours the same way.
- Cross-Mod Bridge (Dungeons > Cross-Mod Bridge), from Killer560's Mod: talks to Odin's, NoammAddons' and Devonian's own party sockets so dungeon data (Melody, map rooms, wither and blood doors, Mimic, Prince, M7 dragon spawns and deaths, room secrets) flows both ways between Sky2M users and theirs. Each one is off by default, Hypixel only, never used when you have that mod installed, and only ever as your own account. /s2 bridge shows how they're doing.
- Dungeon Sim (Dungeons > Dungeon Sim, /s2 sim, or an optional title screen button), from Killer560's Mod: practise dungeons in a singleplayer world of your own. Load any of the 135 rooms or generate a whole floor, with secrets, keys and doors, mobs, the Mimic, all ten puzzles, a /start run timer, the sidebar and score, classes and your loadout, room-by-room cycling with filters, and its own map. Everything in it stays inside the Sim's world. Killer560's cheat-build parts (auto-clear, auto routes and the Fairy Door path) aren't in it.
- Profile Viewer: /s2 pv <player> (and /pv if you turn it on) opens anyone's SkyBlock profile, laid out like NEU's: skills and stats, Dungeons, Slayers, every inventory (with the new wardrobe), Pets, Collections, Mining and Foraging (HOTM, powder, crystals, forge, Glacite, HOTF), Bestiary, Crimson Isle, Museum, Rift, Farming and Garden, Networth (valued with Estimated Item Value), Senither and Lily Weight, and Misc. The pages after Pets are ported from Killer560's Mod.
- Terminal Progress (Dungeons > Floor 7 > Terminal Progress): during F7/M7 P3, a HUD with the section you're on and who did each terminal, lever and device ("Name 3/7"), and an optional title each time. The HUD is on by default.
- Area Waypoints (/s2w or /s2 wp, Misc > Waypoints, Trackers & Sounds), from Odin: waypoints that only show in one dungeon boss phase (F1-F5 bosses, each F6 and F7 phase), Kuudra tier or island, as a box (In: between two corners) or a circle (At: one block and a radius) with a label. Visual ones are only drawn; Proximity ones run a command when you walk in, Click ones when you click a block inside. A manager to search areas, switch, edit and delete waypoints, an Add/Edit form with a colour picker, and import/export in Odin's format.
- Ordered Waypoints (/sho, Mining > Ordered Waypoints), from SkyHanni: load a Coleweight or Skytils route from your clipboard and its waypoints show one after another as you reach them, with skip, skipto, unskip, add, delete, save, erase, export and setup mode, and Mineshaft routes that load themselves.
- Bonus score messages (Dungeons > Score > Bonus Score Messages), from Skyblocker: "Mimic dead!", "Prince dead!" and "Bat dead!" in party chat once a run (not if a teammate already said it), Kill Titles when the Mimic, a Prince or a bonus bat dies, and the crypts reminder once the score reaches a threshold.
- Death Tracker (Dungeons > Death Tracker): who died, what killed them and the score it cost, in a HUD and chat, with deaths counted across runs (/s2 deaths).
- Archer & Tank Counters (Dungeons): arrows you've shot and damage you've taken this run, in HUDs and a summary after the run.
- Key & Door Alerts (Dungeons): a title when a Wither or Blood Key is picked up and by whom, who opened each door and how long after the key, and a key holder HUD.
- M7 Dragon Arrows and Splits (Dungeons > Floor 7): arrows around your crosshair pointing at each spawning dragon with its countdown, and how long each dragon took to kill, with your best per colour.
- Catacombs Calculator (/s2 catacalc [level] [floor or XP]): how many runs until a Catacombs level, using the XP your last run of that floor gave.
- Duplicate Class Warning (Dungeons): parties in the Party Finder that already have your class are marked red, and you're warned when someone joins with a class that's taken.
- Scatha Tracker (Mining): worm and Scatha spawns, worms since your last Scatha, your Scatha rate, Scatha pets by rarity and the spawn cooldown, with spawn alerts.
- Slayer Phase Timer (Slayers > Boss Fight > Phase Timer): how long each hit phase and damage phase of your boss took.
- Voidgloom Laser Warning and Timer (Slayers > Enderman), like SkyHanni.
- Mayor & Election (Mayors > Mayor & Election, /s2 mayor): the mayor and minister with their perks and who's winning the election, in a HUD and chat.
- Damage Numbers (Combat): short damage numbers (1.2M) keeping their colours, hiding them, and a HUD adding up the damage around you.
- Low Health & Mana (Combat): a warning when either drops below a percentage.
- Carry Tracker (/s2 carry): who you're carrying, how many are done (dungeon runs and slayer bosses count themselves) and what they owe, in a HUD.
- Networth History (/s2 networth): your networth saved every few hours and drawn as a graph.
- Auction Price Guard (Misc > Inventory & Storage): stops a click that would list an item far below its lowest BIN or buy one far above it, until you click again.
- Item Quality (Misc > Items & Tooltips), like SkyblockAddons: dungeon items' quality and floor on the slot and in the tooltip.
- Chat Filters (Chat): hide lobby joins, ads, Watchdog lines, warping lines, Mystery Boxes, ability spam and your own filters.
- Clickable Names (Chat): click a player's name on their message to open their profile.
- Chat Tabs, Search & Timestamps (Chat): All, Party, Guild and Private tabs, searching your chat (Ctrl+F), and the time on each message.
- Custom Sounds (Misc): your own sounds for leaping, teleporting, dying and kills.
- Swing In Place (Misc > Visual > Held Item > Settings): your first-person item still swings, but doesn't slide across half the screen while it does, like other mods with every swing setting at 0. Only how it looks.
- Sky2M Achievements: achievements for dungeon runs, S+ runs, slayer bosses, Scathas, carries, dragons, kills, deaths, networth, time played and more. /s2 achievements now opens one screen for all achievements, with Overview (your stats and progress), Dungeons, Slayers, Mining, Combat, Diana and Sky2M tabs.
- Hunting (new category), from SkyHanni with its 9.1 beta additions: the Shard Tracker (track the hovered shard with a key in your Hunting Box, see how many you have and need to max it, import SkyShards recipes with /s2 importskyshards), the Hunting Profit Tracker (shards caught, their value, profit per hour), the Lasso and Fusion displays, Fusion Keybinds, Prevent Huntrap Misclick and the Shulker Finder (a route to the nearest Hideonleaf or Hideonsun spot). Galatea Mob Highlights for Birries, Hideonleaf, Hideonsun and Invisibugs. Safari: biome names in the centre, the Hideyho Finder (a route past every spot he can hide), hiding thrown and dropped Critter Capsules, and a Shard Checklist of the shards you've caught this run.
- Own Name Tag (Misc > Visual > Own Name Tag): see your own name tag in third person (F5), with an optional "NW 37.47B   Skill Avg 52.4" line above it.
- Sacks Value (Misc > HUDs > Sacks Value), like SkyHanni: what everything in your sacks is worth and how many items they hold, as a range from insta-sell to sell offer. Bazaar insta-sells are taken off by themselves, so you don't have to reopen your sacks. /s2 resetsacks forgets them.
- Server Info Display icons (Misc > HUDs > Server Info Display > Show Icons, on by default): small pixel icons before ping, TPS, the time and FPS.
- Chest Spending History (Dungeons > Chest Profit > Spending History, on by default): every reward chest you open is saved with its cost (coins, key, Kismet reroll), contents and profit. /s2 chesthistory opens it, filtered by floor, chest and where you opened it, with totals per floor and chest; /s2 chesthistory chat gives a summary in chat.
- Gemstone Tracker (Mining > Gemstone Tracker): gemstones mined per type as Flawed (or per tier) with their value and rate, pristine procs, and Glossy Gemstones from the Mining Fiesta. /s2 resetgemstones resets it.
- Drill Fuel HUD (Mining), like SkyblockAddons: your drill's fuel as an amount, a percentage or both, coloured by how much is left, with an optional low fuel warning.
- Hit Colour (Misc > Visual > Hit Colour), like Lunar Client's Hit Color: the colour and strength of the flash a mob or player gets when hit, for players, mobs or only the ones you hit.
- Warn About Key Conflicts (General, on by default): says in chat when two features use the same key (Sky2M settings, your Controls and other mods' keys, Command Keys macros). Keys that only work in one menu don't count against keys used in the world. /s2 keyconflicts lists them.
- Boss Phases (Dungeons > Boss Phases): every F1-F7 and M1-M7 boss phase timed on a HUD with your record next to each and the whole fight, "new record" messages, Livid, Storm and Goldor attack timers, and an optional title, sound or party message when the phase changes. Slayer bosses can alert when they switch between hit and damage phases. /s2 resetbossrecords clears the records.
- Sign Autofill (Misc > Inventory & Storage > Sign Autofill): Bazaar buy order and sell offer prices come filled in (top order +0.1, best offer -0.1), custom amounts remember your last one, and Create BIN Auction suggests the lowest BIN minus 1 coin, 1% or 5%. It's selected, so Done uses it and typing replaces it; nothing is sent for you.
- Command Autofill (Chat > Command Autofill): /bz and /bazaar suggest every Bazaar item (enchanted_diamond, enchanted_diamond_block...), /ahs suggests item names and /viewrecipe item ids, with Tab like normal command suggestions.
- Spam Filters (Chat > Spam Filters), from SkyHanni: 32 kinds of spam (lobby, warping, kill combos, Bazaar and AH, powder mining chests, winter gifts, fire sales and more), each shown, hidden or moved to the Side Chat.
- Side Chat (Chat > Side Chat): a second chat window for messages you move out of the main chat, Pristine procs by default. It fades like chat, scrolls with the mouse wheel while chat is open, and its messages can be hovered and clicked. Move it in /s2 gui.
- Keep Chat Scroll Position (Chat, on by default): scrolled up in chat, new messages, the compactor, tabs and search no longer move what you're reading; it goes back down when you scroll down, send a message or close chat.
- Toggle Sprint HUD Text (Misc > Toggle Sprint): write your own text for the toggle sprint HUD, with & colours and {key}.
- Report Threads: bug reports, suggestions and feedback are now conversations. /s2 reports shows yours (and, for admins, everyone's, with filters); open one to read and reply. Admins can mark a report read, answered or closed, or delete it. When someone replies you get "<name> Has Responded To Your Bug Report: ..." in chat straight away, or the next time you join if you were offline; click it to open the thread. Admins are the owner and any staff ticked as a reports admin in the website's Staff section. Needs the updated Sky2M server; until then reports are sent the old way.

### Changed
- The profile viewer has a new look: a rounded dark panel with a blue accent, tab tiles, the main page's stats as cards, roomier skill bars (overflow levels in purple), and real icons for pets and every sack item (from NEU's items).
- The profile viewer shows overflow levels past Hypixel's max, like SkyHanni: skills, Catacombs and classes, the garden level and crop milestones (Misc > Profile Viewer > Overflow Levels, on by default; off shows Hypixel's max for everything). Skill Average still uses the capped levels.
- Networth (the profile viewer's Networth page and /s2 networth) is now worked out like SkyHelper, with a Java port of SkyHelper-Networth and its prices, so it matches the bots and sites that use it (it was billions low on big profiles). Profiles now load from SkyBlockPV's backend first, which is up to date, and /s2 networth includes the museum.
- The Cross-Mod Bridge (Dungeons > Cross-Mod Bridge) is on by default, with its Odin, NoammAddons and Devonian sockets, and is turned on once for existing settings too.
- Discord Rich Presence works with nothing to set up: Sky2M has its own Discord application built in, like SkyHanni, so the Discord App ID setting is gone.
- Faster and smaller: the jar is about 8 MB smaller (oversized chat emojis are shrunk to their real size, unused files removed), prices and the item list are downloaded once and shared instead of up to seven times, and the Commission HUD, active pet highlight, RNG HUD, Item Pickup Log and chat image previews do much less work each frame. Chat image previews are kept smaller and the oldest ones are let go. Dungeon rooms load when you first go to the Dungeon Hub or a dungeon instead of at startup.
- /pv opens much faster: the name lookup and the login run at the same time, names are remembered (players in your lobby need no lookup), the backup server is tried after 5 seconds instead of 20, your skin and name show while the rest loads, and networth is worked out in the background. Opening the Networth page no longer freezes the game.
- Item List recipes (R and U) show on their own background instead of over the inventory, and only SkyBlock recipes: plain Minecraft crafting recipes are gone, while drops, Forge, Kat, NPC shops and uses in SkyBlock recipes stay.
- The Runecrafting tab and its Valuable Rune Alert are removed.
- Code clean-up: unused code and imports removed and the dead cheat-build switches in the Dungeon Sim taken out, with nothing changing in game.

### Fixed
- Bazaar & AH Search (Misc > Inventory & Storage) never opened: it looked for the wrong line on Hypixel's search sign. It's now Skyblocker's search overlay: searches longer than 15 letters work, your last search is filled in, enchantments and pets are suggested, the Auction House has Max Pet Level and dungeon star buttons, last searches can be removed, and /ahs and /bzs open it too.
- Profit Trackers: supercrafting (or crafting) with items you'd tracked counted what you made as new profit on top, without taking off what it used up. Crafted items now take the place of the items they were made from, whether those came from your sacks or your inventory, and crafting with items from before you started tracking adds nothing.
- Logging in to the Sky2M server could count as successful when the server never answered: it now counts as failed after 10 seconds and the next try logs in again normally.
- After the connection to the Sky2M server dropped, the friends list, who's online, the party finder and your cosmetics kept showing old data until it reconnected. They now show offline and ask again once it's back.

## 1.0 — 2026-10-10

The first Sky2M release: SkyBalls is now Sky2M. Commands are /s2 and /s2m (/sb and /skyballs are gone), chat messages start with [S2M], and your SkyBalls settings, keybinds and data are copied over the first time you start it. Remove the old SkyBalls jar from your mods folder.

### Added
- Terminal Solver Enabled (Dungeons > F7/M7 > Terminals and Devices, on by default): switch the terminal solver off, so terminals look and click like normal.
- SkyBlock Only (General), from Killer560's Mod: one switch that pauses every Sky2M feature outside SkyBlock (the lobby, other games and servers). Singleplayer still works, and there's a few seconds' grace while switching servers so nothing flickers.
- Custom Crosshair (Misc > Visual > Custom Crosshair), from Killer560's Mod: design your own crosshair (cross, T, X, dot, circle, cross and circle, or a picture), every size, colour (chroma too), outline and rotation, colours when looking at a mob or block, and spread when you move, sprint, jump or swing. Import a Valorant crosshair code, a CS2 share code, a KXH1 code or a PNG from your clipboard, export yours as a code, or start from a preset.
- Pack Disabler (Misc > Items & Tooltips), from Killer560's Mod: SkyBlock items drawn the way they looked before Hypixel's 2026 resource pack, as their old vanilla item (so your own resource packs draw them), their old head, or a new texture for items that never had one. Hypixel's pack still loads; only item looks change. Off while Noamm's PackDisabler is installed.
- Revert Master Stars (Misc > Items & Tooltips), like Killer560's Mod: Master Stars shown the old way, as that many of the item's stars in red instead of the new numbered star.
- GIF Player (Misc > HUDs > GIF Player), from Killer560's Mod: every GIF in config/sky2m/gifs plays on loop as its own HUD you can move and resize in /s2 gui, with a speed setting, a size limit and a list of GIFs to hide.
- DVD Screensaver (Misc > Visual > DVD Screensaver), from Killer560's Mod: text or a GIF bouncing around your screen like an old DVD player, with up to 10 at once, new colours on walls or corners, and a sound and message when it hits a corner exactly.
- Trail (Misc > Visual > Trail), from Killer560's Mod: a trail of squares behind you as you move, in one colour on the ground and another in the air, with its length, square size, lifetime and fade.
- Auto Join SkyBlock (Misc > Hypixel & Commands), like SkyHanni: when you join Hypixel you go straight into SkyBlock instead of staying in the main lobby. Only once per join, so /lobby still works.
- Livid Finder (Dungeons > Floors 4-6, on by default), from SkyHanni: once the wool above the arena shows which Livid is real, she's boxed and labelled with a line from your crosshair (not while you're blind). It can also hide the wrong Livids and force the highlight colour. Replaces Odin's Livid Solver.
- Enchanted Book Labels (Misc > Items & Tooltips, on by default): an abbreviated enchantment name in the top corner and its numeric level in the bottom corner (for example, Legion V shows the label and 5), so you can tell books apart without hovering. Books with several enchantments aren't labelled.
- Discord Rich Presence (Misc > Discord & Screenshots, on by default): your Discord profile shows you're playing Sky2M, with where you are on SkyBlock and for how long. Needs the Discord app open; Show Location in Discord turns the location off.
- Collection Portfolio (Misc > Collections, on by default): your collections are saved every 30 minutes on SkyBlock so /s2 collections can show how much each one went up over the last day, week or month. Needs your Collections API on.
- Livid Solver (Dungeons, F5/M5), from Odin: once the wool above the arena shows which Livid is real, a chat line names her and she's boxed in a colour you choose (not while you're blind), plus a HUD counting down her invulnerability.
- Terracotta Timer (Dungeons, F6/M6), from Odin: seconds until each terracotta respawns, over its flower pot.
- Blessing Display (Dungeons), from Odin: a HUD with the dungeon's blessings and their levels from the tab list, each with its own toggle and colour. Move it in /s2 gui.
- Relic Place Timer (Dungeons > F7/M7 > Dragons and Relics), from NoammAddons: once all five relics are placed, who placed each one and how long after the Wither King appeared, with your PB for your relic.
- Spawn Timer also shows your priority dragon's timer big in the middle of the screen, like NoammAddons.
- Garden Visitors (Farming > Visitors), from SkyHanni: the Visitor Shopping List (what your visitors need, with prices, how many you have in your sacks, "Craftable!" and the items new visitors usually want; also in the visitor's menu, the Bazaar, your inventory and number signs, and optionally in the Bazaar Alley and farming areas), the Visitor Timer (visitors waiting and time until the next one, with the sixth visitor estimate, a warning and an optional ping), prices in the offer's tooltip (each item, coins per copper and per Garden Experience, and the base crops needed), the Reward Warning (a chat message and a warning over the visitor for rare rewards, and blocking refusing or accepting by reward, copper price, coin loss or never-accepted visitors; hold the bypass key to click anyway), status highlights on the visitors (new, items ready, accepted, refused), arrival title and chat message, hidden visitor chat and Hypixel's arrival message, compact reward messages, an accept hotkey, a Supercraft button, Drops Statistics, Charmed Visitors, the Jacob/Anita NPC fix, and highlighting or blocking visitors outside the Garden. Visitor names, rarities and wanted items come from SkyHanni's repo.
- Enchant Parsing (Misc > Enchant Parsing), from SkyHanni: enchants in tooltips coloured by level (perfect enchants in moving chroma by default, then great, good and poor), ultimate enchants bold, sorted, laid out Normal, Compressed or Stacked, with stacking enchants' progress and an option to hide enchant descriptions. Enchant levels come from SkyHanni's repo.
- Stash Messages (Chat > Stash Messages), from SkyHanni: Hypixel's stash reminder becomes one line you can click to pick up (or view) your stash, repeats of the same counts are hidden, and so is "One or more items didn't fit in your inventory...".
- Hook & Bobber (Fishing), from SkyHanni: the Fishing Bobber Timer (how long your bobber has been out, from the cast or from when it lands) and the Fishing Hook Display (Hypixel's reel-in countdown and "!!!" bigger on your screen, with the original hidden). Move and resize them in /s2 gui.
- Bestiary Overlay (Combat > Bestiary Overlay), from SkyHanni: in the Bestiary menu, each family's kills and progress to max or to the next tier (hover for the details), sorted by global progress, total kills or kills needed, and each category's families found and completed, with clickable Number Format, Display Type, Number Type (Roman or normal) and Hide Maxed buttons. Maxed families and categories are highlighted green, and if Overall Progress is hidden the overlay asks you to turn it on and highlights its Eye of Ender red. Move and resize it in /s2 gui.
- Highlight Withers (Dungeons > Mobs), like NoammAddons' Wither ESP but not ESP: a box around the F7/M7 boss withers you can see, hidden behind walls, in a colour you choose.
- Highlight Teammates (Dungeons > Mobs): your dungeon teammates glow in their class colour, the same colours as the Leap Menu.
- Highlight Active Pet (Misc > Pets): your summoned pet is highlighted in the Pets menu. Its colour can be changed.
- Sack amounts are remembered (from the sack menus and the "[Sacks]" messages) for the visitor shopping list.
- Protect Item key (Options > Controls > Sky2M, unbound by default): runs /s2 protect on the item you're holding, or on the item under your mouse in any menu.
- Estimated Item Value (Misc > Item Price Tooltip, on by default), like SkyHanni's: below the lowest BIN and 3 day average, the item's price plus what's applied to it: recombobulator, hot and fuming potato books, enchantments, master stars, gemstones, Art of War and Art of Peace, scrolls, dyes, skins, rune, drill parts, Wood Singularity, Farming for Dummies, Polarvoid, Transmission Tuners, Mana Disintegrators, Jalapeño, Etherwarp, Book of Stats, Divan's Powder Coating and talisman enrichment, each at its bazaar insta-buy price or lowest BIN.
- Bazaar Order Colours (Misc > Bazaar, on by default), like Bazaar Utils: in your Bazaar orders menu each order's slot is green when it's the best price, yellow when another order matches its price, and red when it's been outbid. Fully filled orders aren't coloured.
- Highlight Infested Plots (Farming > Garden > Pest Finder, on by default): plots with pests are red in Configure Plots (/desk).
- Plot Teleport Panel (Farming > Garden > Pest Finder, on by default), like Skyblocker's Garden Plots widget: in the Garden, a map of your plots to the right of your inventory. Click a plot to /plottp there; plots with pests are red with their pest count, locked plots grey and the plot you're in outlined, and "TP to Pests" warps to the nearest plot with pests.
- Shard Level Up Highlight (Misc, on by default): attribute shards you own enough of to level up or unlock are highlighted green in the Hunting Box and the Attribute Menu (open the Hunting Box first so the menu knows how many you have).
- Show Pet Level (Misc > Pets, on by default): each pet's level on its slot in the Pets menu, in gold when it's maxed.
- Spirit Bear (Dungeons > Timers and Alerts, on by default), from Odin: in the F4 and M4 boss, a HUD with the spirits killed (25 on F4, 30 on M4), then the countdown until the Spirit Bear spawns once the last one dies, then "Alive!". Move it in /s2 gui.
- What's New (General > What's New After Updates, on by default), like SkyHanni: after Sky2M updates, a chat message with a [What's New] button opens a list of the new features, each with an ON/OFF toggle and a button to its settings, plus All On / All Off. /s2 whatsnew opens it any time.
- Party Finder Stats (Dungeons > Party Finder Stats), from Odin's Better Party Finder: when someone joins your group from the Party Finder, their Catacombs level, class levels, secrets (and per run), watcher kills, floor times (hover Normal or Master), magical power, armour (hover each piece) and missing key items (Wither Blade, Terminator, Golden and Ender Dragon) are shown in chat, only to you, with a [KICK] button. Optional Auto Kick by S+ time on a floor, secrets, Catacombs level and magical power, remembering who it kicked. /s2 cata <name> shows anyone's.
- Auto Requeue (Dungeons > Auto Requeue), from NoammAddons: when a run ends, the party goes into another run of the same floor after a delay you set, unless someone left. /s2 requeue cancel skips the next one.
- Item Pickup Log (Misc > HUDs > Item Pickup Log), from SkyHanni: a HUD of the items you just picked up (+) and lost (-), sacks included, with SkyHanni's options: compact lines and numbers, sacks, shards, coins, the coin value of what you picked up, alignment, layout and how long lines stay. Moving items around your inventory isn't logged.
- Translator (Chat > Translator), from SkyHanni: click a player's chat message to translate it into your language. The translation only shows in your chat; nothing is sent. /s2 translate <language> <message> translates your own message and copies it, ready to paste.
- Bazaar & AH Search (Misc > Inventory & Storage), like Skyblocker: clicking Search in the Bazaar or Auction House opens a search box instead of the sign, suggesting item names as you type (only bazaar items in the Bazaar) and your last searches.
- Inventory HUD (Misc > HUDs > Inventory HUD), from Killer560's Mod: your inventory as a HUD you can move and resize in /s2 gui.
- Slayer Minibosses (Slayers, on by default): the slayer HUD also lists the minibosses near you, and "SLAYER BOSS" or "SLAYER MINIBOSS" is written above their name and health.
- Right-click a HUD in /s2 gui to open its settings, like SkyHanni. Middle-click (or shift and right-click) turns its background on or off.
- Discord App ID (Misc > Discord & Screenshots): Discord Rich Presence needs a Discord application's ID to show anything; paste yours here.

### Changed
- The /s2 menu is reorganised: the categories are in a clearer order (General, Chat, Online, Dungeons, Slayers, Combat, Mayors, Farming, Mining, Fishing, ...) and their settings are grouped into dropdowns and sub-dropdowns. Misc is split into Items & Tooltips, Inventory & Storage, HUDs, Collections, Visual, Hypixel & Commands and Discord & Screenshots; Puzzle Solvers has a dropdown per puzzle, and Secrets, Timers, Score, Mobs, Chest Profit, Diana, the slayers, Fishing and more are grouped the same way. Your settings are kept.
- Offer Sharing Found Hotspots (Fishing > Alerts > Hotspots & Wormholes) is now off by default (and switched off once on update), and its chat message has a [Turn off] button.
- Shard Level Up Highlight remembers your shard amounts between sessions and matches shards by item, so the Attribute Menu works without opening the Hunting Box every time. If it doesn't know your amounts yet, it says to open the Hunting Box once.
- Performance: storage pages are decoded once instead of every few seconds (the recipe HUD and storage searches stuttered with a lot of storage), and the scoreboard, tab list, sidebar and HUD text are read with less work every tick and frame.
- Show Pet Level has its own Show Overflow Pet Level toggle (Misc > Pets), instead of following the Pet Display's Overflow Pet Levels setting.
- Door Highlight shows wither and blood doors through walls (filled and outlined), and only once they show on your dungeon map, like Odin: a door appears when a room next to it is opened.
- Spirit Bear, the Livid Solver, the Terracotta Timer and the Blessing Display are on the Dungeons page itself, so you don't have to look through the sub-categories for them.
- M7 dragon priority follows NoammAddons for the Minister update, where dragons spawn one after another: your priority is the dragon that spawns first, and the new Solo Priority (Healer or Tank) takes the second one, on the first two dragons only unless First Dragon Only is off. The Split Power and Easy Power settings are gone.
- Dungeon chest profit is now NoFrills' Dungeon Chest Value and Croesus Solver: the reward chest's value over the chest, Croesus's runs coloured unopened, rerolled (Kismet Feather), opened or opened with a key, with their floor on them, and in a run's chests the most profitable one highlighted (pink for very valuable chests or a dye) and the second best too (aqua when it's still worth a Dungeon Chest Key), with each chest's value in its tooltip. Its colours and the high-profit threshold can be changed.
- Goldor's death tick timer counts every 60 ticks (it was 50, which drifted), starts from Storm's death too and stops when the core opens, like Odin. The Goldor Tick Period setting is gone.
- /s2 protect marks protected items with a smaller star only; the green box around them is gone.
- The Deployable Timers HUD works without Item Cooldowns turned on, and also times Warning and Alert Flares.
- SkyBalls is now Sky2M: commands are /s2 and /s2m, chat messages start with [S2M], and the old /sb, /skyballs and /sbc commands are gone (/sbc is /s2c). Settings, keybinds and data in config/skyballs are copied to config/sky2m once.
- Update checks look at the Sky2M releases on GitHub (2m3s/Sky2M-26.2 and 2m3s/Sky2M-26.3).
- Party commands (!warp, !pt, !f7, ...) are always run, even when Sky2M thinks someone else leads: if you don't, Hypixel says so instead of the command silently doing nothing.
- Deployable Timers finds power orbs and flares placed near you (yours or anyone's), like SkyblockAddons, instead of only ones you placed.
- Slot Locking: binds can be finished by clicking the second slot, bound slots get a border, and Always Show Binds shows every bind in your inventory (Killer560's).

### Fixed
- /s2 recipe showed 0/0 for every ingredient when you already had the item: the ingredients now show what crafting it takes, with "You already have N" above them.
- Server Info Display: TPS showed "--" on Hypixel.
- Tooltip Scroll: the mouse wheel didn't move tooltips that fit on screen, like normal item tooltips. Those now move when the screen isn't using the wheel.
- Storage overlay: the mouse wheel didn't scroll it while another mod's item list (SkyBlock Item List) was under the mouse.
- The game lagged when a bestiary message (or any burst of chat) came in: compact chat compared every new line with your whole chat history (huge with More Chat History) and rebuilt the chat when the bestiary's two separator lines matched. It now only looks at the last 5 seconds and never compacts separator lines, and nickname replacement skips names that aren't in the message.
- Terminals: the vanilla terminal menu no longer shows for a split second when a terminal opens or Hypixel reopens it.
- Starred mob boxes no longer go on teammates standing next to a starred mob (often the Mage and Tank).
- /s2 log showed the 26.2 changelog for 26.3 versions.
- Deployable Timers HUD never showed power orbs (Overflux, Plasmaflux, Mana Flux, Radiant) or flares: they have no cooldown in their lore, so the timer was never started. Flares now last their real 3 minutes (the SOS Flare showed 30s), and power orbs' timers follow the time left on the orb's name tag.
- Bazaar order notifications compared buy orders with the lowest sell offer (and sell offers with the top buy order), so buy orders were nearly always called outbid. Hypixel's API lists buy orders under "sell_summary"; they're now read the right way round.
- The boss-target hotkey could target someone else's slayer boss; the slayer HUD now follows the boss you targeted.
- Hypixel Button didn't show over custom menus (Sky2M's and other mods' screens); it now draws on top of them.
- Terracotta Timer (F6/M6): the timer stayed on the flower pot; it's above the terracotta once it's grown.
- Discord Rich Presence never showed: it had no Discord application to connect to. Set Discord App ID.

## SkyBalls 1.5 — 2026-10-06

Everything from the Minecraft 26.3 build (its 1.0 to 1.1 and what came after), so both builds have the same features, plus the new features below.

### Added
- Livid Solver (Dungeons, F5/M5), from Odin: once the wool above the arena shows which Livid is real, a chat line names her and she's boxed in a colour you choose (not while you're blind), plus a HUD counting down her invulnerability.
- Terracotta Timer (Dungeons, F6/M6), from Odin: seconds until each terracotta respawns, over its flower pot.
- Blessing Display (Dungeons), from Odin: a HUD with the dungeon's blessings and their levels from the tab list, each with its own toggle and colour. Move it in /s2 gui.
- Relic Place Timer (Dungeons > F7/M7 > Dragons and Relics), from NoammAddons: once all five relics are placed, who placed each one and how long after the Wither King appeared, with your PB for your relic.
- Spawn Timer also shows your priority dragon's timer big in the middle of the screen, like NoammAddons.
- Garden Visitors (Farming > Visitors), from SkyHanni: the Visitor Shopping List (what your visitors need, with prices, how many you have in your sacks, "Craftable!" and the items new visitors usually want; also in the visitor's menu, the Bazaar, your inventory and number signs, and optionally in the Bazaar Alley and farming areas), the Visitor Timer (visitors waiting and time until the next one, with the sixth visitor estimate, a warning and an optional ping), prices in the offer's tooltip (each item, coins per copper and per Garden Experience, and the base crops needed), the Reward Warning (a chat message and a warning over the visitor for rare rewards, and blocking refusing or accepting by reward, copper price, coin loss or never-accepted visitors; hold the bypass key to click anyway), status highlights on the visitors (new, items ready, accepted, refused), arrival title and chat message, hidden visitor chat and Hypixel's arrival message, compact reward messages, an accept hotkey, a Supercraft button, Drops Statistics, Charmed Visitors, the Jacob/Anita NPC fix, and highlighting or blocking visitors outside the Garden. Visitor names, rarities and wanted items come from SkyHanni's repo.
- Enchant Parsing (Misc > Enchant Parsing), from SkyHanni: enchants in tooltips coloured by level (perfect enchants in moving chroma by default, then great, good and poor), ultimate enchants bold, sorted, laid out Normal, Compressed or Stacked, with stacking enchants' progress and an option to hide enchant descriptions. Enchant levels come from SkyHanni's repo.
- Stash Messages (Chat > Stash Messages), from SkyHanni: Hypixel's stash reminder becomes one line you can click to pick up (or view) your stash, repeats of the same counts are hidden, and so is "One or more items didn't fit in your inventory...".
- Hook & Bobber (Fishing), from SkyHanni: the Fishing Bobber Timer (how long your bobber has been out, from the cast or from when it lands) and the Fishing Hook Display (Hypixel's reel-in countdown and "!!!" bigger on your screen, with the original hidden). Move and resize them in /s2 gui.
- Bestiary Overlay (Combat > Bestiary Overlay), from SkyHanni: in the Bestiary menu, each family's kills and progress to max or to the next tier (hover for the details), sorted by global progress, total kills or kills needed, and each category's families found and completed, with clickable Number Format, Display Type, Number Type (Roman or normal) and Hide Maxed buttons. Maxed families and categories are highlighted green, and if Overall Progress is hidden the overlay asks you to turn it on and highlights its Eye of Ender red. Move and resize it in /s2 gui.
- Highlight Withers (Dungeons > Mobs), like NoammAddons' Wither ESP but not ESP: a box around the F7/M7 boss withers you can see, hidden behind walls, in a colour you choose.
- Highlight Teammates (Dungeons > Mobs): your dungeon teammates glow in their class colour, the same colours as the Leap Menu.
- Highlight Active Pet (Misc > Pets): your summoned pet is highlighted in the Pets menu. Its colour can be changed.
- Sack amounts are remembered (from the sack menus and the "[Sacks]" messages) for the visitor shopping list.
- Warp to infested plots (Farming > Garden > Pest Finder), from SkyHanni: a Teleport Hotkey and /shtpinfested (/s2tpinfested if SkyHanni is installed) warp you to the nearest plot with pests with /plottp. Optionally even when you're already in one, or back to the Garden when there are no pests. Which plots have pests is tracked from the sidebar, the tab list's Pests widget, the Configure Plots menu and the pest spawn, kill and "no pests" messages; open Configure Plots once so renamed plots and the Barn are known.
- Mute Overflow Drop Sound (Farming > Garden, on by default): no nether portal sound with "OVERFLOW! Your ... has just dropped a Tool Exp Capsule!".
- Loadout Highlight (Misc, on by default), from SkyHanni: the loadout you have equipped is highlighted in the Loadouts menu, in a colour you choose.
- Party commands: !pt, !promote and !demote take the start of a name (!pt nix transfers to NixJussid), and there's a new !kick (!kick nix kicks NixJussid, never you), from the party members Sky2M has seen in party messages and /p list. If several members start with it, it says so in party chat instead.
- Pest Spawn alert (Farming > Garden > Pest Spawn), from SkyHanni: a title naming how many pests spawned and in which plot, Hypixel's spawn message kept, replaced by a compact one you can click to /plottp there, or hidden, and the spawn sound kept, muted, replaced by your own sound or by the Plumber tune.
- Skill Progress is now SkyHanni's full version (Skill Progress settings, off by default): the progress display with level, skill icon, XP gained and progress or percentage and actions left, aligned over a progress bar (plain colour, chroma, or textured like the XP bar with five extra textures); the Skill ETA display with XP/hour, time to the next level or your goal, and the session timer (click to reset); the All Skills display (hover for XP, click to open /skills); overflow levels past the cap, with the overflow level-up message; custom goal levels with /shskills goal <skill> <level> (also /shskills levelwithxp and xpforlevel; /sbskills if SkyHanni is installed); overflow and goal progress in the Your Skills menu's tooltips; XP from Jerry Boxes, gifts and Lily-splosions; and hiding the skill XP in the action bar.

### Removed
- The old Pest Cooldown HUD and "Pests Spawned!" alert; the Pest Spawn Timer and Pest Spawn alert replace them, and keep your Pest Cooldown Time.

## SkyBalls 1.0.1 — 2026-10-04

### Fixed
- Sky2M chat, ranks, login, bug reports, crash reports, screenshots, the event calendar, capes and Discord linking use the new server, shadowisabot.com (tastyfish.org no longer runs it, which caused "Couldn't check for bug reports: ... malformed JSON").
- Nickname colours now show for you and other players in Sky2M chat, Hypixel chat, tab and nametags: a nickname update with an empty (null) custom colour or font used to be dropped entirely, so the nickname appeared without its colour. Colour names are also read in any spelling ("Dark Blue", "dark_blue", "DarkBlue"), custom colours with or without "#", and a "Plain" nickname with no colour keeps the colour the name had.
- When the server turns Discord down, the Discord screen says so with the server's reason instead of staying on "Checking Discord connection...", and a failed Discord DM gives a clear message.

## SkyBalls 1.0 — 2026-10-03

The Minecraft 26.3 build starts again at 1.0, and checks for updates on its own releases (2m3s/sky2m-26.3).

### Added
- Skill Progress (Skills), from SkyHanni: the skill you're getting XP in with its level, a progress bar, percent and XP to the next level, XP per hour and the time until you level up.
- Party Coord Waypoints (Mayors > Diana, on by default), like SkyHanni: coordinates a party member sends ("x: -30, y: 87, z: 126") get a beacon waypoint with their name and distance, until you reach it or for a minute.
- Dungeon Chest Profit (Dungeons > Chest Profit): a reward chest's value minus its cost above the chest; at Croesus, each chest's profit with the best one highlighted, and the list of runs tinted green (unopened), yellow (one chest opened) or red (all claimed).
- Highlight Bats (Dungeons > Mobs), and /s2 debug starred to see what the starred mob highlight finds.
- Blood Camp Beam Time: the mob kill countdown (and its box) turns red at the time to shoot your Mage beam, 0.2s by default, like Odin and NoammAddons.
- /pt Name runs /p transfer Name. Minecraft's advancement and recipe popups are always hidden.
- Hide Block Break Particles (Misc > Random), like SkyHanni's: no particles when a block breaks, yours or anyone's, and none flying off the block you're mining.
- Protect Item key (Options > Controls > Sky2M, unbound by default): runs /s2 protect on the item you're holding, or on the item under your mouse in any menu.
- Estimated Item Value (Misc > Item Price Tooltip, on by default), like SkyHanni's: below the lowest BIN and 3 day average, the item's price plus what's applied to it: recombobulator, hot and fuming potato books, enchantments, master stars, gemstones, Art of War and Art of Peace, scrolls, dyes, skins, rune, drill parts, Wood Singularity, Farming for Dummies, Polarvoid, Transmission Tuners, Mana Disintegrators, Jalapeño, Etherwarp, Book of Stats, Divan's Powder Coating and talisman enrichment, each at its bazaar insta-buy price or lowest BIN.
- Bazaar Order Colours (Misc > Bazaar, on by default), like Bazaar Utils: in your Bazaar orders menu each order's slot is green when it's the best price, yellow when another order matches its price, and red when it's been outbid. Fully filled orders aren't coloured.
- Highlight Infested Plots (Farming > Garden > Pest Finder, on by default): plots with pests are red in Configure Plots (/desk).
- Plot Teleport Panel (Farming > Garden > Pest Finder, on by default), like Skyblocker's Garden Plots widget: in the Garden, a map of your plots to the right of your inventory. Click a plot to /plottp there; plots with pests are red with their pest count, locked plots grey and the plot you're in outlined, and "TP to Pests" warps to the nearest plot with pests.
- Shard Level Up Highlight (Misc, on by default): attribute shards you own enough of to level up or unlock are highlighted green in the Hunting Box and the Attribute Menu (open the Hunting Box first so the menu knows how many you have).
- Show Pet Level (Misc > Pets, on by default): each pet's level on its slot in the Pets menu, in gold when it's maxed.
- Spirit Bear (Dungeons > Timers and Alerts, on by default), from Odin: in the F4 and M4 boss, a HUD with the spirits killed (25 on F4, 30 on M4), then the countdown until the Spirit Bear spawns once the last one dies, then "Alive!". Move it in /s2 gui.
- What's New (General > What's New After Updates, on by default), like SkyHanni: after Sky2M updates, a chat message with a [What's New] button opens a list of the new features, each with an ON/OFF toggle and a button to its settings, plus All On / All Off. /s2 whatsnew opens it any time.

### Changed
- Show Pet Level has its own Show Overflow Pet Level toggle (Misc > Pets), instead of following the Pet Display's Overflow Pet Levels setting.
- Door Highlight shows wither and blood doors through walls (filled and outlined), and only once they show on your dungeon map, like Odin: a door appears when a room next to it is opened.
- Spirit Bear, the Livid Solver, the Terracotta Timer and the Blessing Display are on the Dungeons page itself, so you don't have to look through the sub-categories for them.
- M7 dragon priority follows NoammAddons for the Minister update, where dragons spawn one after another: your priority is the dragon that spawns first, and the new Solo Priority (Healer or Tank) takes the second one, on the first two dragons only unless First Dragon Only is off. The Split Power and Easy Power settings are gone.
- Dungeon chest profit is now NoFrills' Dungeon Chest Value and Croesus Solver: the reward chest's value over the chest, Croesus's runs coloured unopened, rerolled (Kismet Feather), opened or opened with a key, with their floor on them, and in a run's chests the most profitable one highlighted (pink for very valuable chests or a dye) and the second best too (aqua when it's still worth a Dungeon Chest Key), with each chest's value in its tooltip. Its colours and the high-profit threshold can be changed.
- Goldor's death tick timer counts every 60 ticks (it was 50, which drifted), starts from Storm's death too and stops when the core opens, like Odin. The Goldor Tick Period setting is gone.
- Diana Profit Tracker is laid out like SBO's Diana loot tracker: every rare drop always listed with its count, lootshares, rate per mob it drops from and value, then your other drops, coins, burrows, mobs, profit (and per hour) and playtime.
- Arrow Counter only shows while you hold a bow or crossbow (Arrow Counter Only With Bow, on by default).
- Shorter dropdown labels in the settings, so they're no longer shrunk to fit (e.g. "Own + Party", "Insta-Sell", "24h + sec", "In Quest"). What each choice means is in the option's description.
- [item] in plain chat now reads "[Heroic Hyperion](sb:HYPERION)" instead of a coded blob, so players without Sky2M can read it; Sky2M players still see the hoverable item. Old [item] links still work.
- /s2 protect marks protected items with a smaller star only; the green box around them is gone.
- The Deployable Timers HUD works without Item Cooldowns turned on, and also times Warning and Alert Flares.

### Fixed
- Server Info Display: TPS showed "--" on Hypixel.
- Tooltip Scroll: the mouse wheel didn't move tooltips that fit on screen, like normal item tooltips. Those now move when the screen isn't using the wheel.
- Storage overlay: the mouse wheel didn't scroll it while another mod's item list (SkyBlock Item List) was under the mouse.
- The game lagged when a bestiary message (or any burst of chat) came in: compact chat compared every new line with your whole chat history (huge with More Chat History) and rebuilt the chat when the bestiary's two separator lines matched. It now only looks at the last 5 seconds and never compacts separator lines, and nickname replacement skips names that aren't in the message.
- Terminals: the vanilla terminal menu no longer shows for a split second when a terminal opens or Hypixel reopens it.
- Starred mob boxes no longer go on teammates standing next to a starred mob (often the Mage and Tank).
- /s2 protect's box and star no longer draw over tooltips and the item on your cursor.
- Pet Display: a pet favourited in the Pets menu (⭐) never showed its held item on the icon, as its name didn't match the tab list's.
- Settings search (and other MoulConfig text boxes) couldn't be typed in on 26.3.
- !pt Name transfers the party to Name; leader commands work when the game started while you were already in a party (it checks /p list).
- Starred mob boxes no longer need a clear line of sight to the mob's head (they're hidden behind walls anyway).
- Party Commands (!warp, !pt, !f7, !fps, !ping, the Diana ones...) didn't answer anyone with a Hypixel emblem (☘, ☣...) before their name, so they never worked for you. Your own !warp, !allinvite and !f7 / !m7 work too now while you're leader.
- Blaze puzzle solver: blazes' names now have a symbol before "Blaze", which it didn't expect, so it found none. The kill order now follows where the blazes are (low in the room: highest HP first), like Skyblocker.
- Pet Display and other item icons: items Hypixel lists with old 1.8 names (Raw Fish, Ink Sack dyes, Lily Pad, Dandelion, wool, glass...) showed as barriers or the wrong item; newer paper-based items (Winding Ivy, Sloth Claws...) use Hypixel's model or a downloaded icon instead of paper. A held "Saddle" is the pet item, not the plain saddle.
- Shared items (Sky2M chat, [item], [inv]) were drawn as paper even when their icon was available.
- [inv]: clicking an item in a shared inventory shows it big with its tooltip (Esc goes back to the inventory).
- [item] with a pet, enchanted book or rune (ids with ";") showed as raw text instead of the item.
- /s2 who: a presence update from the server no longer empties the online list, and the screen says so when the server doesn't answer instead of loading forever. Errors from the Sky2M server are now shown instead of being dropped silently.
- Deployable Timers HUD never showed power orbs (Overflux, Plasmaflux, Mana Flux, Radiant) or flares: they have no cooldown in their lore, so the timer was never started. Flares now last their real 3 minutes (the SOS Flare showed 30s), and power orbs' timers follow the time left on the orb's name tag.
- Bazaar order notifications compared buy orders with the lowest sell offer (and sell offers with the top buy order), so buy orders were nearly always called outbid. Hypixel's API lists buy orders under "sell_summary"; they're now read the right way round.

## SkyBalls 1.4.5 — 2026-10-04

### Added
- Pest Spawn Timer (Farming > Garden > Pest Spawn Timer, on by default, while holding a farming tool), from SkyHanni: time since the last pest spawned, the pest cooldown and the average time pests take to spawn (leaving out spawns where you were AFK). The cooldown counts down the Pest Cooldown Time you set from each pest spawn; it doesn't read the tab list. Optional warnings a few seconds before the cooldown ends and when it's over (title, chat and a sound you choose), repeating until you open your wardrobe or loadouts, and a chat message with how long each spawn took. Show it only while holding a farming tool, vacuum or lasso.
- Pest Spawn alert (Farming > Garden > Pest Spawn), from SkyHanni: a title naming how many pests spawned and in which plot, Hypixel's spawn message kept, replaced by a compact one you can click to /plottp there, or hidden, and the spawn sound kept, muted, replaced by your own sound or by the Plumber tune.

### Removed
- The old Pest Cooldown HUD and "Pests Spawned!" alert; the Pest Spawn Timer and Pest Spawn alert replace them, and keep your Pest Cooldown Time.

## SkyBalls 1.4.1 — 2026-10-04

### Fixed
- Sky2M chat, ranks, login, bug reports, crash reports, screenshots, the event calendar, capes and Discord linking use the new server, shadowisabot.com (tastyfish.org no longer runs it, which caused "Couldn't check for bug reports: ... malformed JSON").
- Nickname colours now show for you and other players in Sky2M chat, Hypixel chat, tab and nametags: a nickname update with an empty (null) custom colour or font used to be dropped entirely, so the nickname appeared without its colour. Colour names are also read in any spelling ("Dark Blue", "dark_blue", "DarkBlue"), custom colours with or without "#", and a "Plain" nickname with no colour keeps the colour the name had.
- When the server turns Discord down, the Discord screen says so with the server's reason instead of staying on "Checking Discord connection...", and a failed Discord DM gives a clear message.

## SkyBalls 1.4 — 2026-10-03

### Added
- Inquisitor Gamble (Diana, on by default): when an Inquisitor you dug up dies or you lootshare one, it gets shot on screen. A crosshair sways over its heart while a heartbeat speeds up, then the gun fires: through the heart with "CHIMERA!" if a Chimera dropped, past the body with "MISSED!" if not. Nothing spoils it: Sky2M's HUDs are hidden while it plays, and the Chimera chat line, drop announcer, rare drop animations and achievement popups wait until it's over. /s2 inqgamble hit|miss to preview it.
- Item Protect (/s2 protect): protect the held item from dropping; protection follows its UUID as it moves, and a star marks protected items. Slot binding remains available separately.
- Inventory Buttons (Misc), ported from Skyblocker's Quick Navigation: up to 14 creative-inventory tabs above and below every SkyBlock menu (Skills, Collections, Pets, Armor Sets, Sacks, Accessories, Storage, Home, Garden, Hub, Dungeon Hub, Auction House, Bazaar, Craft). The tab for the menu you're in is drawn selected; warps need a double click. Each tab's icon, menu title, command and tooltip can be changed.
- Wardrobe and Loadout Hotkeys (Misc): configurable keys equip slots on the current Armor Sets or Loadouts page, like SkyHanni.
- Item Sharing: [item] adds the held item to messages in plain chat and in /pc, /gc, /ac, /cc, /oc, /msg and /r, as well as Sky2M chat. [inv] shares your whole inventory, hotbar and armour as one clickable [Name's Inventory] that opens it in a menu for other Sky2M users ([brag] does the same).
- Bazaar notifications (Misc > Bazaar): track orders from setup messages or the Manage Orders screen, notify in chat when an order becomes outbid, and play sounds for outbid and filled orders.
- Storage Value Breakdown: show an estimated total and the most valuable contents when an Ender Chest page or backpack is open.
- Deployable Timers HUD: countdowns for active Power Orbs and SOS Flare, separate from item cooldowns.
- Zealot Tracker uptime: counts active farming time from confirmed kills and includes a 10-second grace period after the last kill.
- Slayer Boss Phase target keybind: cycle the health/phase HUD through nearby players' slayer bosses while personal kill timing continues to follow your own.
- Experimental Table Prevent Misclicks: block incorrect Chronomatron and Ultrasequencer clicks, including early clicks before the pattern is ready. On by default (also for existing settings).
- Fishing, ported from Feesh (replaces the old fishing stats, hook timer, bait and rare creature alert):
  - Alerts when you or your party catch or cocoon a sea creature from a list you choose (own or own and party), and when a fishing boss kills you or a party member.
  - Party messages for your catches, cocoons and deaths, and for your rare drops (with Magic Find).
  - Rare drop alerts (own by default) with the item's price in the title.
  - Compact catch messages ("DOUBLE HOOK! A Yeti has spawned!") with rarity colours or gradients and your own double hook and catch templates.
  - Alerts for new Trophy Fish and Trophy Frogs, deployables about to run out (pick which), pets reaching max level with their estimated price, hotspots and wormholes closing, the Fishing Festival ending (with your shark counts and personal bests) and "Lootshare!" in party chat.
  - Hotspot sharing: coming near a hotspot offers buttons to share it to party or all chat.
  - Sea creature highlights in their rarity colour, never through walls.
- Profit Trackers, ported from Skysoft: Farming, Fishing, Foraging, Mining, Mythological Ritual and all six slayers, each shown while you do it, in Skysoft's layout and colours: item icons, quantities that light up when they change, coins, quest costs, Kernels, actions, profit per hour and uptime, for this session, today or in total. With a menu open, click Display Mode, Price Source and Reset (with Cancel / Confirm) and scroll the items. The Farming Profit Tracker's settings and totals move over.
- Pet Display: Skysoft's pet icon, your pet's head (or skin) with its held item, on a circle in its rarity colour inside a level progress ring.
- Room Clear Alert (Dungeons > Secrets), from Odin: "Room Cleared!" when your room gets its white checkmark and "Room Complete!" when all its secrets are done.
- Secret Chime and Secret Boxes (Dungeons > Secrets), from Odin's Secret Clicked: a sound and a gold highlight box for a few seconds when you get a secret (a chest, lever, wither essence or redstone key, a dungeon item picked up, or a secret bat killed). The box turns red if the chest was locked. The sound, pitch, volume, box colours and time can be changed, with Play Chime to preview it.
- Storm PY Timer (Dungeons > Timers and Alerts), from Odin's Tick Timers: when Storm calls his lightning, a HUD counts down the 95 ticks to when to crush him under the purple pillar.

### Changed
- Dungeon secret waypoints default to outline-only rendering, and the NoammAddons terminal solver is the default.
- Pest spawn timer and pest-spawn alerts follow SkyHanni's parsing and notification behavior.
- Pet Display caches the active pet's tab-list icon so it remains visible across tab-row refreshes.

### Fixed
- Spirit Leap menu: the Spirit Leap chest no longer shows behind Sky2M's leap menu (and the inventory tabs and Profit Tracker stay off it).
- Blaze puzzle solver (higher or lower) works again: it waited for a roof marker the blaze rooms don't have, so it never started.
- Quiz solver: leaving the room while a question was being asked no longer loses the answer, and questions asked while you're outside the room are still read.
- Party Commands: !help no longer disconnects you. It sent every Diana command in one message, which was over Minecraft's 256-character limit; now it's a short list.
- Keep Terrain Loaded no longer drops and reloads the terrain at random: the island and server it keeps terrain for are now fixed for the whole visit once read, instead of being re-read from the tab list and scoreboard (which flicker) every tick.
- Keep Terrain Loaded: chunks are saved to the terrain cache on a background thread instead of the game's (it stuttered joining an island), cached terrain loads twice as fast, and when Hypixel rebuilds the world on the same island and server (dying, for example) the cached terrain comes back at once instead of after two seconds.
- Performance: HUDs that draw themselves (Collection Tracker, Profit Trackers, Crystal Hollows map) work out their size and contents once per tick instead of several times a frame; the Profit Tracker no longer re-checks your activity 11 times a frame; item price lookups in inventory scans no longer copy every item's data; the Hypixel check run for every item drawn is worked out once per connection; the pet icon's rings use fewer draw calls.
- Chat image previews now extract URLs from nested hover content and link styles, so hovering a preview works across more chat components.
- Case Opening's GOLD GOLD GOLD sound is registered correctly, so it plays when a Legendary-or-better winner starts spinning.
- Disabled Zealot Tracker no longer scans nearby entities; Slayer Boss Phase searches nearby owner tags spatially instead of walking every rendered entity.

## SkyBalls 1.3.9.5 — 2026-09-30

### Added
- Diana Mob Tracker, from SkyHanni's Mythological Creature Tracker: how many of each mythological creature you dug up with their share of the total, your lootshared Inquisitors, King Minos, Manticores and Sphinxes (from SBO), mobs per hour and how many creatures since each rare one, for this session, this mayor term or all time (/s2 mobtracker session|season|alltime|reset). Hypixel's "You dug out a ..." message gets how many creatures it's been since the last one of that kind.
- Diana Rare Drop Announcer, from SBO: your RNG drops (Chimera, Shimmering Wool, Manti-core, Fateful Stinger, Brain Food, Daedalus Stick, Minos Relic, Crown of Greed, Mythological Dye, Myth the Fish, Braided Griffin Feather) in chat with this season's count, the lootshare count and price, as a title, and sent to party chat (pick which drops). Also "Took 12 Inquisitors to get Chimera!", "Took 240 mobs and 1h 2m to get an Inquisitor!" and b2b messages, and optionally "Cocooned a ...!" in party chat. Lootshared drops and rare mobs are counted separately.
- Diana Mob HP, from SBO: the health of the Diana mobs near you and King Minos's hits left on a HUD, with an optional "HP LOW!" alert for rare mobs.
- Diana Achievements, from SBO: SBO's Diana achievements (b2b Chimera, 5k burrows in one event, 100 Inquisitors since Chimera, max bestiaries, Crown of Avarice and more) with a title and chat message, and /s2 achievements to see them (/s2 achievements backtrack checks your saved seasons). Repeat Each Event lets them be earned again every season.
- Diana party commands, from SBO (Party Commands → Diana Commands, on when Party Commands is): !chim, !chimls, !inq, !king, !manti, !sphinx, !relic, !stick, !core, !wool, !food, !stinger, !feathers, !mobs, !burrows, !profit, !playtime, !mf, !stats <you>, !help and !since <chim|inq|relic|stick|king|manti|core|wool|food|...>, answered from this season's Diana mob, drop and profit trackers. Also SBO's !demote, !carrot and !time, and !promote / !demote can name a player.

### Changed
- Diana Profit Tracker: keeps counting burrows and time while the mob tracker, achievements or Diana party commands are on, even with its HUD off.
- Diana: Rare Mob Sharing has a Shared Mobs section to pick which rare mobs are sent and received: Minos Inquisitors, King Minos and Manticores are on by default and Sphinxes can be turned on. It replaces the All Rare Mobs setting, and other rare mobs are no longer shared.

### Fixed
- Keep Terrain Loaded could crash the game with Sodium ("RenderRegion.getResources() is null"): when the chunk storage shrank (like when the scoreboard's server line went missing for a moment during a restart warning), chunks were forgotten without being unloaded, so Sodium kept drawing them. They're unloaded properly now, and a moment without the server line no longer turns the feature off and on.
- Leaderboards: drops that name the enchantment in the text, like "RARE DROP! Enchanted Book (Chimera 1) (+304 ✯ Magic Find)", now count (as "Chimera I") instead of being missed.
- Diana: Siamese Lynx Highlight outlines the lynx the angry villager particles are over. It used to find the cats through their nametags and often picked the other one.

## SkyBalls 1.3.9 — 2026-09-30

### Added
- Smart Disconnect (Misc, on by default): clicking Disconnect in the pause menu asks first, with Cancel and a red Disconnect button.
- Server Info Display (Misc), from Skysoft: your FPS, the server's TPS, your ping and the time (Skysoft's Real Time Display, 12- or 24-hour, with or without seconds) on a HUD, as one display (vertical or horizontal) or a separate one for each, with text labels, symbols or values only, and a colour for each.
- Hide Status Effects (Misc, on by default), from Skysoft: hides potion effects beside inventories and in the top-right of the screen.
- Keep Terrain Loaded (Misc), from Skysoft: on SkyBlock islands, terrain you've visited stays loaded out to your render distance, past Hypixel's view distance, and is remembered on disk for next time. Islands can be left out. Off when Bobby is installed.
- Boss Highlight (Slayers, on by default), from Skysoft: a box (or a glowing outline) on your slayer boss and the minibosses you spawned while you can see them (not through walls), with a line to your boss or the closest miniboss.
- Egg Hits Display (Slayers, on by default), from NoFrills: while you fight a Tarantula Broodfather, how many hits each egg sack still needs, shown big on the egg.
- Etherwarp Overlay (Misc, on by default), from Odin: while you sneak with an etherwarp item, a box on the block you'd teleport to, in red when it would fail.
- Farming Profit Tracker (Farming → Profit Tracker), from Skysoft: in the Garden, what your crops and Garden drops were worth, Bountiful coins, Kernels, pests vacuumed, total profit, profit per hour and your farming time (paused when you stop farming), for this session, today or in total (/s2 farmingtracker session|today|total|reset). Replenish and compactors don't make crops count twice.
- Party Commands, from Odin: !f1-!f7, !m1-!m7 and !t1-!t5 join that floor or Kuudra tier while you're leader, and !fps, !ping and !tps answer in party chat whoever is leader (a moment later, so Hypixel doesn't say "Woah slow down"). Each has its own toggle.

### Removed
- The Sky2M casino: /s2 casino, its games, the daily reward and the Daily Reward Reminder setting.
- Diana's Own Mob Alerts: no more title across the screen when you dig up a rare mob yourself.

### Changed
- The slayer profit chat line names the boss you killed: "[S2M] T5 Tara Profit: -52.0k".
- Diana: only Minos Inquisitors, Manticores and King Minos glow; other rare mobs (Minotaurs and the rest) no longer get a purple outline. Manticores are now shared and received with All Rare Mobs off.
- Diana: Siamese Lynx Highlight (on by default) outlines the lynx you can hit (the one with angry villager particles) in green.
- Diana: an arrow guess is only dropped for showing no burrow particles after 2.5 s in range, and only with Close Burrow Detection on, so it no longer vanishes just before the real burrow shows up (or when detection is off).
- Diana warp keys: the guess warp goes to your newest guess instead of the nearest waypoint (a burrow you'd already found next to you blocked it), the rare mob warp no longer needs your spade in hand, and both say in chat why they didn't warp.

## SkyBalls 1.3.8 — 2026-09-29

### Added
- Diana burrows (Mayors → Diana → Burrows), from SkyBlock Overhaul (SBO):
  - Close Burrow Detection: burrows near you are found from their particles and shown as Start, Mob or Treasure burrows. /s2 clearburrows clears the waypoints.
  - Arrow Guess: after each burrow, the arrow's particles point to the next one.
  - Spade Guess: where your spade's trail lands.
  - Waypoints are a coloured block border (guesses purple, mob burrows red, treasure gold, start green; all changeable), with an optional beacon beam in the same colour, and dig progress ([1/2]) to the right, left, above or below the name.
  - Multi Guesses keeps earlier guesses when a new one comes in, and Keep Waypoints on World Change keeps them when you change servers.
- Diana Warp keys (Options → Controls → Key Binds → Sky2M), from SBO: warp to the warp closest to your next burrow or guess, or to the newest rare mob your party shared. Pick which warps can be used under Mayors → Diana → Diana Warp.
- Sphinx Solver (Mayors → Diana), from SBO: the Sphinx's answers show in chat with the right one in green, and clicking anywhere while chat is open answers it. There's also a key for it (use at your own risk).
- Diana Profit Tracker (Mayors → Diana → Profit Tracker): what each Diana drop was worth, the coins you dug out, burrows dug, total profit, profit per burrow and per hour, and the time you spent, for this session, this mayor term or all time (/s2 dianatracker session|season|alltime|reset). Move it in /s2 hud.
- Lobby Compromised (Mayors → Diana), from Skysoft: an alert when too many players who aren't in your party join your lobby while you do Diana, as a title and optionally in party chat.
- Copy RNG Drops (Combat → Rare Drops): copies only RNG drops to your clipboard (PRAY TO RNGESUS and RNG METER drops, and slayer, Diana and fishing RNG items), for when you don't want every rare drop copied.
- Copy Slayer Drops and Copy Garden Drops (Combat → Rare Drops): copy only slayer drops (anything on a slayer's drop list), or only Garden drops (RARE CROP drops and anything else that drops in the Garden, like pest drops).

### Changed
- The jumpscare now has a 1 in 1,000 chance each time you run /s2: it plays, then the menu opens, instead of a random chance every second while playing. Still never in dungeons or Kuudra.
- One Sky2M category in Options → Controls → Key Binds: the Command Keys binds are in it now, with the new Diana keys.
- Pest Highlight draws a box around each pest (seen through crops and walls) instead of outlining its invisible Silverfish or Bat and the armor stand it wears.
- New Mayors category in the sidebar, with Diana under it: the Diana rare mob sharing and lootshare settings moved there from Combat (your settings move with them).
- Screenshot uploads go through the Sky2M server to ImgBB (links last 30 days), like Skysoft's. If that doesn't work they go to Catbox instead. Catbox (permanent) and Uguu (3 hours) can still be picked under Misc → Screenshots.

### Removed
- 0x0.st as a screenshot host: it has stopped taking uploads.

## SkyBalls 1.3.7 — 2026-09-29

### Added
- Pet HUD shows your pet's held item (Pets → Pets Display → Show Held Item). The tab list doesn't show it, so it's learned from the Pets menu (open it once), "Your pet is now holding ..." and Autopet messages, and remembered per pet.
- Overflow pet level ups in chat again (NopoMod's): "[S2M] Your Golden Dragon leveled up to level 158!" when your pet reaches the next overflow level. Needs Overflow Pet Levels on (Pets).
- Boss Profit (Slayers): after each slayer boss, "Profit: +50k" (or "-10k") in chat, and a Boss Profit HUD that shows the boss's profit when it dies and hides 2 seconds after its drops are in. Only that boss's drops count (SkyHanni's slayer drop lists), not mob drops or other items picked up meanwhile. Hover the chat line for each drop and its value, the drops' total, the quest's cost and the total. The HUD shows the moment the boss dies and fills in as the drops arrive (sack drops come with Hypixel's "[Sacks]" message); the drops are read from the items the boss shows on the ground, your inventory and your sacks, and the chat line goes out once they've landed (or the "[Sacks]" message with them comes). The cost is Hypixel's price for the boss and tier, or half of it with Aatrox's Slashed Pricing when your purse shows that.
- Sign Calculator (Misc), from Skyblocker: on SkyBlock's number signs (auction prices, bazaar amounts) the number you typed shows above the sign, e.g. `15m = 15,000,000`, and sums like `2.5m*3` are worked out and sent as the number.
- RNG Drop Totem Animation (Combat → Rare Drops), from Skyblocker: big RNG drops pop up like a Totem of Undying, with particles and a sound.
- Miniboss Alert (Slayers): a title and a ding when one of your slayer minibosses spawns (from Hypixel's "SLAYER MINI-BOSS ... has spawned!", so other players' minibosses don't count).
- RNG Meter Value (Slayers): in a slayer or dungeon RNG Meter menu, each drop shows its price and how many coins one Slayer XP (or Score) of meter progress is worth, e.g. a 10M drop needing 100k XP shows "1 XP = 100 coins". Above the menu, "Best Profit" names the drop worth the most per point.
- Slayer time messages (SkyHanni's): how long your boss took to kill, timed to the moment it dies (its health hits 0 or it's gone; a cocooned boss when it's cocooned), not to Hypixel's chat message, your personal best for that boss and tier on the same line, e.g. "It took 34.52s to kill Revenant Horror V (PB: 30.10s)" or "(NEW PERSONAL BEST! Previous: 36.00s)", and how long the whole quest took. Each can be turned off, and there's a compact style.
- The slayer boss phase display shows your Tarantula's egg sacs (time left and hits) under its health.
- Slayer kill time leaderboard: your slayer personal bests are shared with the Sky2M leaderboard (Slayers → Personal Best → Share Slayer PBs, on by default; bests from before this update are sent the next time you connect). `/s2 leaderboard slayer` shows every boss and tier with its record holder and your time and rank (click a row for its top 10), and `/s2 leaderboard slayer <boss>` shows the top 10 for one boss, e.g. `Revenant Horror V`. Without a connection it shows your own times. The board is also in Discord (`/leaderboard slayer`).
- Slayer PB Leaderboard HUD (Slayers → PB Leaderboard HUD): the kill time standings for your current or last slayer boss (or one you pin), your place, and while your boss is alive a live timer against your PB and the #1 time. Shows during a slayer quest by default (or always, or on slayer islands), with 3 to 10 rows; move it in `/s2 hud`. Offline it keeps the last standings it got.
- Enderman Slayer (Slayers → Enderman), from SkyHanni: highlights the Yang Glyph (beacon) while a Voidgloom holds it, throws it and after it lands, with a timer until it explodes, plus an optional title warning and line to it; highlights Nukekubi Fixation skulls in gold with an optional line to each; phase numbers (1/3, or 1/6 for tier IV) in the Slayer Boss Phase HUD; hides the smoke, flame and witch particles around endermen in The End; and a line to your Voidgloom Seraph.
- Blaze Slayer (Slayers → Blaze), from SkyHanni: outlines the Inferno Demonlord and its demons in their Hellion Shield's colour with the shield above them; a dagger HUD with your Twilight and Firedust attunements that can mark the one matching the nearest shield (Hypixel's attunement title is hidden while it shows); hides the "Strike using the ... attunement" and "Your hit was reduced by Hellion Shield!" messages; a Fire Pits warning for tiers III and IV; phase numbers in the Slayer Boss Phase HUD; Clear View, which hides particles and fireballs within 10 blocks of a boss; and a Fire Pillar countdown HUD (move it in `/s2 hud`).
- Diana Rare Mobs (Combat → Diana Rare Mobs), from Skysoft. Rare Mob Sharing: when you dig up a Minos Inquisitor or King Minos (or every rare mob, with All Rare Mobs) its coordinates go out in party chat, and rare mobs your party shares get a waypoint, a title and a line from your crosshair. Lootshare Helper: your hits on a shared rare mob are counted, and once you've done 1% of its health (enough to lootshare it) "Lootsharing" over the mob turns from red to cyan and "Loot share secured!" goes out in party chat. Party members who secured it get a cyan ✓ above their head, and whoever spawned the mob a pink one. It also draws the 30 block lootshare radius. Uses the same party messages as Skysoft, so both mods see each other's shares and checkmarks. Damage only counts with a Griffin pet out.
- Party Finder Announcements (Sky2M Online → S2M Chat, on by default): when someone opens a new `/s2 pf` listing, the Sky2M server announces it in chat (who is looking for how many more, for what, and their note) with a [JOIN] button (not on your own listings). Turn it off to stop seeing them.

### Changed
- Settings reorganised. Your settings move with them.
  - Like SkyHanni, categories can have sub-categories in the sidebar, shown when you open the category: Slayers has Blaze and Enderman, Dungeons has one per section, and Misc has Pets (the Pets tab is gone).
  - Dungeons → F7/M7 holds the M7 dragons and relics, the terminal and device solvers, and the 3x3 platform highlight.
  - Misc: Screenshots, Storage Overlay, Collection Tracker (with its Elite rank) and Toggle Sprint (with its HUD) are each one dropdown, and Price Paid is in Item Price Tooltip with the lowest BIN and 3 day average.
  - In every category the dropdowns come before the single toggles; Kills Since Rare Drop is last in Slayers.
- Screenshot uploads go to 0x0.st (kept for at least 30 days) instead of Litterbox. Uguu (3 hours) and Catbox (permanent) can be picked under Misc → Screenshots.
- Warp Shortcuts use SkyHanni's and Skysoft's short warp commands: every warp name works without /warp (/dhub, /crypts, /garden, ...) and tab-completes, and in the Garden /home, /barn and /tp <plot> work like SkyHanni's. It applies straight away, and the list of warps to set up is gone.
- New installs start with almost every feature off: turn on what you want in /s2. Options inside a feature keep their defaults, and SBC chat stays on. Existing settings don't change.
- /s2 gui only shows the HUDs whose feature is turned on.
- Performance: the slayer boss scan only runs during a slayer quest and skips non-boss nametags early; starred mob highlighting checks every 4 ticks; SkyBlock item ids are cached instead of copying each item's data; the pickaxe ability reads the tab list every 4 ticks; settings are checked for changes every 5 seconds outside menus (and saved when the game closes); two chat regexes are built once.
- Museum tooltip: your donations are also read from Hypixel's museum API (through SBC) when you join and every 10 minutes, so you don't have to open the museum menus first.
- The jumpscare lasts 1 second.
- `/s2 leaderboard` lists the running event boards plus a clickable Slayer PBs entry, and tab-completes slayer bosses. Event boards keep their names: one called "slayer" still opens that board.

### Fixed
- Pest Highlight didn't find any pests: Hypixel's pests are now an invisible Silverfish or Bat under the pest's name, with a new icon, and the name can be a text display. Pests are found by their name again, and the pest count on the scoreboard is read with the new icon.
- /storage: the mouse wheel scrolled a little, then stopped once an item came under the mouse. Block Scrolling on Items was still on for settings saved before 1.3.4; it is switched off once (you can turn it back on).
- The PB HUD's live timer froze while a Tier 5 Tarantula swapped phases. It now keeps running until the boss's health hits 0.
- Slayer PBs: a Tier 5 Tarantula was two bosses ("Tarantula Broodfather V", then "Conjoined Brood"), so the PB HUD switched to an empty board at the start of each fight. Both phases are now one boss, Tarantula Broodfather V, and older Conjoined Brood times move over to it.
- Screenshot upload: when the host failed (e.g. Litterbox's HTTP 500) its whole error web page was pasted into chat. It now tries once more, then says what went wrong in one line with a [Retry] button.
- Storage overview: smooth-scrolling mice and touchpads barely scrolled (each small wheel step was rounded to nothing).
- Storage overlay and overview: Retain Scroll Position was lost the first time you opened them after starting the game.

### Removed
- Auto Welcome.

## SkyBalls 1.3.6 — 2026-09-28

### Removed
- The SS skip helper (start-click counter and limit) from the Simon Says solver.
- The accessory helper next to the Accessory Bag.
- The profile viewer (`/s2 pv`), and the Profile buttons in `/s2 who` and the friends list.
- The Hide Menu terminal option: the vanilla terminal menu is now always hidden while a solver shows (Melody too, unless its solver is off).

### Fixed
- Simon Says: the SS skip reordering could highlight the wrong buttons and block the right one; a quick correct click on the next button was blocked until the server showed the previous press. Every highlighted button now shows its number in the sequence.
- Terminals: the NoammAddons style looked the same as Odin's. It now draws like NoammAddons: only the terminal's own cells in a compact grid, with its title, padding and border.
- Cocoon alerts now fire for Special Zealots and other elusive mobs (and names with an apostrophe).
- Items shared in Sky2M chat show their real icon when clicked (off Hypixel too) instead of paper or a barrier.
- Copy Rare Drops (and the big drop animation) now work for every rare drop line: VERY RARE, CRAZY RARE, INSANE, PRAY TO RNGESUS, PET and RNG METER drops, the Garden's RARE CROP lines, RARE REWARD, OUTSTANDING CATCH and Diana's "You dug out a ..." drops.
- The slayer boss phase display only shows your boss's health (or its hits), not nearby pets or damage numbers.

### Changed
- The accessory tooltip is now a 1:1 port of Skyblocker's: it also remembers recombobulated accessories, and shows nothing until your SkyBlock profile is known. As in Skyblocker, "↑ Upgradable" means you own that accessory and a higher tier of it exists.

## SkyBalls 1.3.5 — 2026-09-28

### Added
- Museum tooltip (from Skyblocker): item tooltips say whether the item is donated to your museum, and which museum category it's in. Look through your Museum's category menus once and it's remembered per profile.
- Accessory tooltip (from Skyblocker): accessory tooltips say whether you're missing it, already have it, or whether it's an upgrade or downgrade of the one you have from the same family, with the tiers, e.g. "✦ Upgrade (2→3/4)". Look through each page of your Accessory Bag once and it's remembered per profile.

- Sky2M Online (new settings category): replies and a ↩ button in Sky2M chat, highlighted mentions with a ping, @name completion, emoji reactions (hover a message with chat open), item sharing (`/s2 share`, the Share Item key, `[item]`), and clear notices when a message is refused or you're muted.
- `/s2 ignore`, `/s2 unignore`, `/s2 friend add|remove|accept|deny`, and `/s2 friends` with requests and privacy settings. Popups when friends come online.
- `/s2 who` is now a screen with nicknames, ranks, badges, friends, location, AFK, time online and mod version. Your area, server and AFK status are shared (you can turn this off).
- Casino: coinflip, dice, roulette, slots and higher/lower next to blackjack, plus the daily reward (with a reminder), stats and leaderboards (`/s2 casino`, `/s2 casino daily`).
- Profile viewer: `/s2 pv [player] [profile]`.
- Gradient and Chroma nickname styles, badges and supporter symbols before names, capes, and `/s2 cosmetics`.
- Update notices from the Sky2M server (required ones on every join and on the title screen), announcements and the message of the day, feature flags, crash reports (can be turned off) and settings cloud sync (`/s2 settings`).
- Item cooldowns on item slots, the event calendar with reminders (`/s2 calendar`), the accessory helper next to the Accessory Bag, and pest highlight in the Garden.
- `/s2 help` lists every command.

### Changed
- Copy Rare Drops shows what was copied in chat, using Copy Chat's preview settings.
- Sky2M chat logs in on every connection and reconnects with a growing delay, saying "SBC offline" once instead of on every retry. Casino actions are queued to the server's limit of 5 a second.
- Performance: nicknames in nametags and the tab list are worked out once per name change instead of every frame, and HUD text is built once per tick instead of every frame.

### Removed
- The Slayer Tracker HUD.

### Fixed
- The slayer boss phase display never showed when you had a nickname set, because it looked for your nickname instead of your username on the boss's "Spawned by" line. It also no longer needs the sidebar to show your slayer quest, and keeps the colours of multi-line nametags.

## SkyBalls 1.3.4.5 — 2026-09-27

### Fixed
- "Took N bosses to drop" said 0 bosses the first time you got a drop. It now counts every boss since tracking started, and waits a second before counting so the kill that gave the drop is included (as in NopoMod). Drops in the Rift count for Vampire slayer. After you change worlds, a drop only counts if that slayer has given it before, until you kill another boss.
- Leaderboards and the casino wait for the server to confirm your Minecraft login before sending anything. A leaderboard drop the server refused because you weren't logged in is sent again once after logging back in, so it isn't lost.

## SkyBalls 1.3.4 — 2026-09-27

### Added
- Discord leaderboards: leaderboards run from the Sky2M Discord (for example "most Summoning Eyes this week") count your drops and other chat events through the mod automatically. Enchanted book drops count by their enchantment (a Chimera leaderboard counts Chimera books), and a drop counts once even if Hypixel sends it twice. /s2 leaderboard lists the running ones; /s2 leaderboard <name> shows the standings.

### Changed
- The [S2M] in front of Sky2M messages is light purple instead of dark green.

### Fixed
- Hovering an image link in chat shows just the image, without the link's hover text on top.
- Storage overlay: the mouse wheel still didn't scroll over items whose tooltip is taller than the screen (Tooltip Scroll took it). In the storage overlay and /s2 storageoverview the wheel now always scrolls the storage; Tooltip Scroll's keys still move long tooltips.

## SkyBalls 1.3.3 — 2026-09-27

### Added
- /s2 search is now a port of SkyOcean's item search (MIT): category buttons (All, Storage, Island, Museum, Inventory), sorting by Amount, Price or Rarity (up or down), a $ button with the total value of what's listed, and copies of the same item shown as one entry with the total count. The tooltip lists where each copy is. Clicking an item opens its Ender Chest page or backpack, warps to your island and draws a rainbow box around its chest, or warps to the museum; the item then stays highlighted in menus for a minute (it used to flash for one frame).
- /s2 search remembers the chests on your private island: open a chest and its items are saved (per profile, like your storage). Breaking the chest or turning it into a Minion Chest forgets it.
- /s2 search remembers what's in your museum: browse the museum's category menus in the Hub and the items kept there are saved. Items shown as not donated or taken out are forgotten. Museum items don't count for the craft helper or the portfolio.
- Nicknames now live on shadowisabot.com (run /website in the Sky2M Discord to get a login code) and show for your own name and everyone else's. Once your account is linked there, /s2 nick sets it in game too: /s2 nick <name>, /s2 nick <colour> <name>, /s2 nick rainbow <name>, /s2 nick #hex <name> or /s2 nick off. Without a linked account /s2 nick doesn't show. /s2 togglenick, the nickname editor and the Nickname settings are removed, and everyone's nickname always shows.
- /s2 discord: the link to the Sky2M Discord.
- /s2 casino: blackjack with play money. Everyone starts with $100; bet, then Hit, Stand or Double (blackjack pays 3:2, the dealer stands on 17). If you go broke you get $100 again a day later. A leaderboard beside the table ranks everyone with the mod by most money. The SBC chat server holds the balances and deals the cards, and your Minecraft login is checked, so nobody can edit their money.
- Held Item Model is replaced by a port of Skysoft's Held Item (Misc > Held Item, or /s2 helditem): an editor over the game where you drag the item to move it (right-drag for depth), scroll to resize it, and use sliders for position, scale, swing speed and rotation, for every item (Global) or just the one in your hand (This Item), with undo and redo. New: an Item Only swing style (the item swings, not your arm), switching an item to its vanilla texture instead of Hypixel's, and the Held Item Update Fix (on by default: the item doesn't dip when Hypixel updates it, and mining isn't interrupted). Your old settings and items saved with /s2 helditem save are copied over; check them in the editor, since positions are measured differently. Swing X/Y/Z, Swing Rotation, No Swing Animation and No Re-equip Animation are gone.
- SBC chat rank announcements: when someone is given a rank prefix or has it removed, SBC chat shows "Steve Has Been Granted [VIP] By Console" (or "Steve's [VIP] Prefix Has Been Removed By Console") with the prefix in its exact colour. Hidden when SBC chat is hidden.

### Fixed
- Mouse Lock now works like Skyblocker's: it locks with every farming tool (all Theoretical Hoe tiers including Sunflower and Wild Rose, Fungi Cutter, Cactus Knife, Melon and Pumpkin Dicers, Coco Chopper, Gardening Hoes and Axes, Binghoe), goes by the SkyBlock id so renamed tools still lock, and pauses in the Garden's barn. Before, it never locked with Theoretical Hoes (it looked for the tool's id in the item's name) or renamed tools.
- Storage overlay: pages opened after you searched (like /ec 8) didn't show, and searching could hide Ender Chest pages until the search changed.
- Storage overlay: searching left the open page's items in the top left corner, over the other pages.
- Storage overlay: the mouse wheel didn't scroll while over an item. Block Scrolling on Items is now off by default, and Tooltip Scroll no longer takes the wheel from it.
- Storage overlay: the scroll bar keeps the knob under the mouse when clicked or dragged, and reaches the top and bottom.
- Tooltip Scroll took the mouse wheel whenever any tooltip showed (settings, lists, menus). It now only moves tooltips too big to fit on screen.
- /s2 search did nothing off Hypixel, and could be closed again by the chat closing.

## SkyBalls 1.3.2.5 — 2026-09-27

### Added
- Hypixel item emojis in every chat (all, party, guild, private and Sky2M chat; everyone with Sky2M sees the icons), like the SkyHelper Discord: :summoning_eye:, :hyperion:, :enchanted_diamond: (any SkyBlock item id in lower case) show the item's real SkyBlock icon; hover it for the name. The icons are downloaded once from Coflnet and kept in config/sky2m/item-icons, so they don't need Hypixel's resource pack (without it the Summoning Eye is just paper). Chat > Hypixel Item Emojis.
- Emoji autocomplete shows each emoji's picture next to its name, includes the item emojis, and can be turned off (Chat > Emoji Autocomplete).

### Fixed
- Typing ":" in chat lagged: it listed every emoji. The list is now up to 50 matches (names starting with what you typed first).
- Storage overlay Dark Mode (Misc > Storage Overlay Settings): the backgrounds, slots and scroll bar are drawn dark, with a Dark Mode Shade to pick how dark.

## SkyBalls 1.3.2 — 2026-09-27

### Added
- /s2 opens where you left it: the same category, scroll position and open sections, until you restart the game.
- RNG HUD colours: Farming > Farming RNG HUD has Drop Colour, Pet Rarity Colours and Price Colour; Misc > Item Notification has Use Item Rarity Colour and Name Colour.

### Changed
- Update notifications are checked every minute while you play (was every 10 minutes), so a new release shows in chat within a minute. Checks that find nothing new don't count toward GitHub's limit.
- Current Chat Display ("Chat: All") is drawn under the command and emoji suggestions instead of over them.
- Storage overlay is now Firmament's, ported from Firmament (GPL-3.0-or-later, textures CC-BY-4.0): one scrollable view of every Ender Chest page and backpack with your inventory, a search box (matching pages only, results highlighted) and "Edit Pages" for the normal Storage menu. The open page is the real menu, so clicking, dragging, shift-click and tooltips work as normal, and it stays open while Hypixel switches pages (no mouse jump). New settings under Misc > Storage Overlay Settings (columns, height, scroll, outline, highlight colours). New commands: /s2 storage, /s2 storageoverview, /s2 storagename enderchest|backpack <page> [name]. Pages saved before still show.
- Sky2M is now licensed GPL-3.0-or-later (needed for the Firmament code).
- Messages from Discord in Sky2M chat start with the green [S2M] like the rest, then a blue [Discord]: "[S2M] [Discord] [name]: message".

### Fixed
- Item Notification stopped counting (and hid) with a personal compactor putting items into your sacks, with Check Sacks off: each new enchanted item that came and went within 10 seconds was taken for the same one. Only a quick swap (under half a second) is ignored now, once.
- Storage overlay: hovering an item didn't show its name or tooltip (the overlay drew after the game had already drawn this frame's tooltips).
- Collection Tracker went up by millions when you supercrafted (Enchanted Gold Blocks into your sacks were counted, the Gold Ingots taken out weren't). Each "[Sacks]" message is now counted per collection with what went in and what came out together.
- Collection Tracker counted items taken out of your sacks as gathered: they landed in your inventory and looked like a pickup. When the "[Sacks]" message shows them coming out, that pickup is taken back.

## SkyBalls 1.3.1 — 2026-09-27

### Fixed
- Item Notification (and the Collection Tracker) counted items after you opened and closed a menu such as your sacks: Hypixel resends your inventory a moment after a menu closes. They now wait a second after a menu closes or you change area, and items taken out of your sacks (the Sacks menu or /gfs) don't count.
- Putting items back into your sacks showed them as a drop (twice) and added them to the Collection Tracker again (a stack too many). Hypixel puts the same list on several parts of the "[Sacks]" message and it was read for each; each list is now read once, what the message takes out is subtracted, and items that just left your inventory aren't a drop.
- Storage overlay: moving an item could throw it on the ground (releasing the mouse reached the hidden menu, which saw a click outside it). Switching pages no longer jumps the mouse to the middle of the screen and back.

## SkyBalls 1.3.0 — 2026-09-27

### Added
- Hypixel Button (Misc, on by default): a Hypixel button on the title screen, next to Multiplayer, that joins play.hypixel.net in one click.
- Held Item Model > No Re-equip Animation (on by default): the held item doesn't dip and roll back up when Hypixel updates it or you switch items. This was the "roll across the screen" that happened sometimes even with Swing X, Y and Z at 0.

### Changed
- Built-in positional messages are class specific again (on F7/M7): Py Stand Here and Mage Stop for Mage, Arch for Archer, Healer and SS for Healer, Tank for Tank.
- Platform Highlight (3x3) shows whenever you're on F7/M7 (it waited for Goldor's chat line and often didn't show).
- Pickaxe Ability HUD and ready alert only show on the mining islands (Gold Mine, Deep Caverns, Dwarven Mines, Crystal Hollows, Mineshafts), not in the End or on the Crimson Isle.
- Update notifications are checked every 10 minutes while you play (not only when you join, at most every 3 hours), so a new release shows in chat without restarting.
- One RNG HUD: Item Notification items now show in the Farming RNG HUD (renamed RNG HUD) instead of a HUD of their own, and an item already shown as an RNG drop isn't listed twice.
- Collection Tracker: your inventory and sack messages are a live guess on top of the API number, and each API refresh only replaces what it covers, so the total no longer jumps back while the API catches up.
- Storage overlay: pages listed in the Storage menu that aren't saved yet show as boxes you can click to open, and the normal menu shows until there's something to put on the overlay.

### Fixed
- "At SS" was sent again every time you stepped back onto the spot; it's sent once per run.
- Simon Says solver didn't work: it waited for the floor and Skyblocker's boss-room detection; it now works anywhere in a dungeon (it only reacts to the Simon Says blocks).
- Copy Chat could copy a line a couple of rows above the one under the mouse; it now uses where chat really drew each line.
- Copy Chat's key set to right click (or another mouse button) did nothing.
- /s2 search, the Storage Search key and saving storage pages did nothing when your server address had a port (play.hypixel.net:25565) or until Hypixel said your Profile ID again; the profile is now remembered and any Hypixel address works. Pages saved before still show.
- Collection Tracker counted too much: items picked up and then moved into your sacks were counted again from the "[Sacks]" message, and a compactor adding the enchanted item a tick before removing the 160 base items counted 160 each time. Gains now count once they've stayed for half a second, and a sack message only adds what didn't just come out of your inventory.
- Item Notification could show the same drop twice (Hypixel swapping the stack out and back, or the drop also going into your sacks).

## SkyBalls 1.2.9 — 2026-09-26

### Added
- Terminals > Hide Menu (on by default): while a terminal is being solved, only the solver shows. The chest, its items, your inventory and item tooltips are hidden, like other terminal solvers. The Odin style shows the terminal's name above the solver, and Melody keeps its lanes and buttons.
- Score Time Message (Dungeons > Score, on by default): like NoammAddons, a chat message when the run reaches 270 and 300 score with how long it took and the floor, e.g. "300 score reached in 6m 12s || M7." It no longer needs the 270/300 alerts on, and a run that jumps straight past 300 gets both messages.
- Summoning Eye drops show how many Zealot kills that eye took, on Hypixel's drop message: "RARE DROP! Summoning Eye (Kills: 100)". The Zealot that dropped it is counted too.
- Held Item Model > Swing Rotation: how far the item tilts and rolls when you swing (1 = vanilla, 0 = it doesn't turn). Swing X, Y and Z only move the item, so at 0/0/0 it stayed in place but still did the big forward roll; lower Swing Rotation to shrink that.

### Removed
- The NoammAddons dungeon map (and its Map Style, checkmark, name, scale and colour options). The dungeon map is always the Skyblocker map now.

### Changed
- Click in order! colours are now Numbers Next / Second / Third under Terminals (green, yellow and red by default).
- The 1 in 10,000 jumpscare can't happen in Kuudra runs either (it already couldn't in dungeons), or while Sky2M hasn't read your location yet.
- Copy Chat has one setting, Copy Message Key: any key or mouse button (right click by default) copies the message under the mouse. Right-Click To Copy, Shift+right-click and Copy Line Key are gone.
- The "GOLD GOLD GOLD" sound is louder again.

### Fixed
- The dungeon floor still showed as "-" for some players. Sky2M now also reads the sidebar straight from the scoreboard's teams (like NoammAddons), so it's found even when a scoreboard mod hides or replaces the sidebar, and doesn't take the Dungeon Hub's queue line for the floor. The backup that reads "[MVP++] Name entered The Catacombs, Floor VII!" now matches the message (it has lines of dashes around it) and is kept on the way into the run, and the Floor 7 bosses talking also set floor 7. This fixes F7/M7 positional messages, the Score Display, Platform Highlight and the device solvers when the sidebar can't be read.
- Held Item Model: Swing X/Y/Z and Swing Speed were skipped for players who also run another mod with swing animations (NoammAddons, Odin, ...), so the item still moved across the screen. Sky2M now applies them last. If it still moves, turn off the other mod's animations.
- Arrow Align misclick protection: when more than one layout fitted, frames that were already right could still be clicked. Only the first layout that fits is used now.
- Nicknames above heads (yours and other Sky2M users') kept the plain name with no colour or font. The nick is now put into the player name tag right where it's made, like NoammAddons does for its badges, and also into every entity's name tag, so it shows however the tag was made (vanilla or another mod). A nickname that contains the real username as a word is no longer replaced twice, and text before a skipped partial match (like "2m3sx") is no longer dropped from the name.
- Platform Highlight (3x3) didn't show: it needed the floor from the sidebar and could be switched off by a moment where the tab list area wasn't read. It now uses the F7/M7 detection from chat and the boss, turns on from any Goldor line (or Storm's death) in case the first was missed, and Healer Only uses the Your Class setting.
- Zealot Tracker sometimes counted one Summoning Eye twice, and pasting "RARE DROP! Summoning Eye" into chat counted as a drop. Only Hypixel's own drop line counts now: it has to start the message and "RARE DROP!" has to be bold like Hypixel writes it, which players can't do. The same line arriving twice at once counts once. The tracker also saves when the game closes.
- Copy Chat sometimes copied the message above the one you clicked. It now finds the line under the mouse with the same maths as vanilla chat.
- Quiz solver didn't highlight the answer when it couldn't find the room's corner block; it now uses the dungeon room matching to place the answers.

## SkyBalls 1.2.8.5 — 2026-09-26

### Changed
- Click in order! terminal: the next three numbers are green, yellow and red (they were three greens that were hard to tell apart), with the Odin and NoammAddons solvers. Settings still on the old greens are updated; Numbers 1-3 can still be changed.
- Dungeon Case Opening: the "GOLD GOLD GOLD" sound is much louder (about 4x), and plays as soon as the spin starts when it is going to land on a gold (Legendary or better) item, instead of when it stops.

### Fixed
- The dungeon floor wasn't recognised (`/s2 debug` showed "Dungeon floor: -"), so F7/M7 positional messages (Py Stand Here, Mage Stop, SS, ...) didn't show, and anything else that needs the floor (Score Display, dungeon map, device solvers) could break. Sky2M now finds the SkyBlock sidebar with any scoreboard mod, even one that puts its own sidebar in its place, reads the floor even when one of Hypixel's hidden padding characters sits inside "(F7)", and falls back to the "... entered The Catacombs, Floor VII!" message when a run starts. `/s2 debug` also shows the sidebar's title and location line.
- Terminal Solver: terminals could open as the normal menu with no solver, because the solver waited for Sky2M to know you were in a dungeon. It now starts for any terminal on Hypixel (their names only exist in F7/M7).
- Melody (Odin style): with the menu's items hidden, only the lit slots were drawn, so the board and buttons couldn't be seen. Like Odin's Melody GUI, every lane and button now has a background cell (Melody Background colour), with the column, pointer and button to press lit on top.
- Arrow Align Block Wrong Clicks still didn't block anything: the solver only ran once the dungeon floor was known. It now runs next to the Arrow Align board in any dungeon, and wrong clicks are stopped before they're sent, the same way as Odin.

## SkyBalls 1.2.8 — 2026-09-26

### Added
- Dungeon Case Opening at Croesus (Dungeons > Case Opening > Croesus, on by default): the chests you open at Croesus spin too, with that run's floor drops in the reel, like SkyOcean. Hide Contents In Croesus (on by default) hides what's in those chests in the run's menu so the spin isn't spoiled.

### Changed
- Zealot Tracker: in Total mode the kills line says "Total Kills:" (Session mode still says "Kills:"), and the lines are now kills, Since last eye, then Summoning Eyes.
- Storage Overlay: the top row of each page (the glass panes, Go Back arrow, Close barrier and page buttons) isn't shown any more, only your items.

### Fixed
- Dungeon Case Opening: the chest could still be seen through the spin, and its tooltips drawn over it; the chest is now hidden until the spin ends.
- Terminal Solver: the vanilla terminal showed through. With NoammAddons the real menu isn't drawn behind the panel any more; with Odin the terminal's own items and their tooltips are hidden and only the solution shows, like Odin.
- Arrow Align Block Wrong Clicks: each click on a frame was counted twice, so the last click a frame needed could be blocked as wrong. Clicks are counted once, and frames that are already right can't be clicked (hold shift to click anyway), like Odin.

## SkyBalls 1.2.7 — 2026-09-26

### Fixed
- With a scoreboard mod like CustomScoreboard, Sky2M couldn't read the sidebar, so the dungeon floor and time were unknown: the Score Display stayed at 0, the NoammAddons map drew rooms at the wrong size and place, and F7/M7 positional messages didn't show. It now finds the SkyBlock sidebar either way.

## SkyBalls 1.2.6 — 2026-09-26

### Added
- Join commands (Misc > Join Commands): `/f0` (Entrance) to `/f7` and `/m1` to `/m7` join that Catacombs floor, and `/t1` to `/t5` join Kuudra (Basic, Hot, Burning, Fiery, Infernal).
- Held Item Model: Swing X, Swing Y and Swing Z. Set them to 0 and the item still rotates when you swing but doesn't move across the screen, like NoammAddons.
- Screenshot Sharing (Misc): after F2, the "Saved screenshot as ..." message has an [Upload] button. It uploads the picture to Litterbox (or Catbox) and gives you the link with [Send in /s2c] and [Copy Link]; the link previews when hovered.

### Changed
- Positional messages: the built-in spots (Py Stand Here, Mage Stop, Arch/Healer/Tank Stand Here, SS) show on F7 and M7 while Built-in Waypoints is on, instead of only for your class in the right boss phase.
- Terminals and Devices: the Odin Terminal Solver switch is now a Terminal Solver dropdown, like SkyHanni's: Odin, NoammAddons or Skyblocker Highlights. NoammAddons shows its big centred panel with the terminal's name, a Scale setting, three slot styles (Rect, Bordered Rect, Button) and its colours; you click on the panel, and rubix picks the right mouse button for you.

### Fixed
- Dungeon Case Opening showed almost only enchanted books; the reel now mixes in the floor's drops (Necron's Handle, Giant's Sword, Shadow Fury, master stars, ...) with the chest's real items, like SkyOcean.
- Teleport maze solver could send you round in a loop back to the pad you came from; the pad you leave now counts as used, like Odin's solver.
- The game could crash when entering a dungeon (the dungeon map crashed while logging a player it couldn't match). The same hidden problem in waypoint groups is fixed too.
- Storage Overlay: opening /ec showed the real Ender Chest page underneath the overlay; now only the overlay shows.
- Messages from the Sky2M chat bot showed as "[SkyJew]: [SJ] ..."; they show as Sky2M and [S2M].
- Hovering an ImgBB link in chat (like Skysoft's "Screenshot uploaded: https://ibb.co/...") showed no preview. Links to image pages on ImgBB, Imgur, Gyazo, Lightshot and Postimages now preview the picture on the page.
- Update notifications check the renamed Sky2M GitHub page (they stopped arriving after the rename).

## SkyBalls 1.2.5 — 2026-09-26

### Changed
- SkyJew is now **Sky2M**: `/s2`, `/sky2m`, `/s2c` for Sky2M chat, and [S2M] in chat (the old `/s2` commands are gone). The [S2M] in Sky2M chat is dark green, like Hypixel's "Guild >". Your settings carry over automatically. **Delete the old SkyJew jar** when you install this one, or both will load.
- Ranks ([OWNER], [TESTER], ...) only show in Sky2M chat, and Chat > Custom Chat > Show Ranks turns them off completely. Rank changes made on the website show up right away when the server announces them.
- The Collection Tracker is just two lines now: "Collection: 12,345,678" (with the item's icon), and the next player above you on the Elite leaderboard with how far ahead of you they are.
- The Pickaxe Ability HUD reads Hypixel's Pickaxe Ability tab list widget, like SkyHanni, instead of guessing from chat. Turn that widget on in Hypixel's tab list settings.
- The Enchanting Runes nickname font is made of letters now (the Unicode enchanting table alphabet), so it shows in every tab list, including other mods'.
- Leap menu: press 1-4 (changeable) to leap to the teammate in that box; each box shows its key.
- Chat image previews work with Discord, Imgur and Gyazo links, show "Loading image..." while downloading, and sit above the link's tooltip.

### Added
- `/s2 bugreport`, `/s2 suggest` and `/s2 feedback`: send a bug report, suggestion or feedback (a title and a description) to the Sky2M team.
- Dungeon Case Opening (Dungeons > Case Opening, off by default): Obsidian and Bedrock chests open like a CS2 case (SkyOcean's Dungeon Gambling) and stop on the best item. Legendary or better plays "GOLD GOLD GOLD".
- Item Notification (Misc > Item Notification): SkyOcean's Sack Notification as a HUD. When an item on your list goes into your sacks or inventory it shows like the farming RNG HUD ("5x Enchanted Diamond 8.5k"). The list is edited in its own window with item name suggestions (EDIT, or `/s2 itemnotify`).
- Storage Overlay (Misc, on by default): every Ender Chest page and backpack at once in /storage and in any page, like Firmament. Click a page's name to open it; the open page and your inventory work as normal.
- Slot Locking & Binding (Misc): press L over a slot to lock it (it can't be clicked, moved or dropped), and B to bind a hotbar slot to an inventory slot so a shift-click swaps them (Odin's Slot Binds).
- Copy Chat (Chat > Copy Chat): right-click a chat message to copy it, Shift+right-click for one line, with a preview (NoFrills' Chat Tweaks). Rank prefixes aren't copied.
- `/s2 toggle <setting>` turns any setting on or off, `/s2 togglenick <player>` hides or shows someone's nickname for you, `/s2 who` lists who is online with the mod, and `/s2 disableall` (or `/s2disableall`) turns everything off after you click to confirm.
- No Swing Animation (Misc > Held Item Model, off by default), like NoFrills.
- S2M Chat Ping (Chat > Custom Chat, off by default): a ping when someone sends a message in /s2c.
- Farming RNG HUD: Epic and Legendary slug pets from pests, shown in their rarity colour ("1x Legendary Slug Pet") at a set price of 500k (Epic) and 5m (Legendary).

### Fixed
- `/s2 nick`: picking a preset colour also updates the colour picker and hex box.
- Calendar dates are also added from the menu's own tooltip, so another mod's tooltip code can't stop them showing.
- Mouse Reset puts the cursor in the middle of the screen (it went near the top-left at GUI scales above 1).
- `/s2 log` said the changelog wasn't available; it shows it again.
- No Swing Animation didn't do anything; it works now.

## SkyBalls 1.2.4 — 2026-09-26

### Added
- Update notifications: when a newer SkyJew is out you get "New SkyJew Mod Version 1.2.4 --> 1.2.6" in chat (always the newest one), with a Download link (Misc > Update Notifications).
- Auto Welcome (Misc > Auto Welcome): put players on your list and SkyJew welcomes them when they come online, in guild chat or with /msg, with your own message ({name} is their name). It can also welcome new guild members. Manage the list with `/s2 welcome add|remove <name>` and `/s2 welcome list`.

### Changed
- SkyJew's chat messages start with [SJ] instead of [SkyJew].
- SkyJew ranks ([OWNER], [TESTER] and the ones set on tastyfish.org) now show in front of those players' names in every chat, not only /sjc (Chat > Custom Chat > Ranks In All Chat).
- The Collection Tracker HUD copies SkyHanni's Crop Milestones display, for any collection: "Collection Milestones", the item's icon with your collection tier ("Cobblestone 11➜12"), your progress in that tier ("12,345/20,000"), the time to the next tier, Items/Hour and the percentage, then your Elite rank and how much you need to pass the next player. `/s2 trackcollection <item> [goal]` pins one collection with an optional goal (e.g. `/s2 trackcollection wheat 10m`), like SkyHanni's /shtrackcollection; `/s2 trackcollection` follows what you gather again and `/s2 trackcollection stop` hides it.
- Ranked players can use their own name as a nickname.

### Fixed
- `/s2 nick` Font button: the ◀ arrow went forward too; it now goes back.
- The Enchanting Runes and Illager Runes nickname fonts showed broken characters for anything they don't have (numbers and symbols in the enchanting one); those now use the normal font.
- Rune prices: the lowest BIN list only has runes someone is selling right now, so most rune levels had no price. The price tooltip and Portfolio now fall back to the 3 day average and then to the last price seen (with how long ago), and Portfolio knows the names of runes that are in the 3 day average too.
- Nicknames (with their font) now show in tab lists drawn by other mods (SkyHanni, Skyblocker) too.
- The `/s2 nick` menu couldn't be closed: Esc and Done sent you back to the chat box, or did nothing when the name wasn't allowed. Esc now always closes (a name that isn't allowed just isn't saved) and Done closes once the name is allowed.

## SkyBalls 1.2.3 — 2026-09-26

### Changed
- Dungeon map: new NoammAddons (Legit) style, now the default. Rooms are redrawn in clean colours with checkmarks, room names or secret counts, bordered player heads with optional names (Holding Leap / Always), and optional extra info under the map. Every colour and size can be changed. The old look is still there as Map Style: Skyblocker.
- Score Display now uses NoammAddons' score calculation, read straight from the tab list and sidebar, so it no longer depends on the dungeon start being detected. Optional Detailed mode and Force Paul. The 270/300 alerts use it too.
- Terminals: Odin's terminal solvers for all six terminals, with misclick blocking (clicks on wrong slots are ignored), first click protection and client prediction. Skyblocker's highlights are still available with Odin Terminal Solver off.
- Devices: Odin's Simon Says, Arrow Align and Sharp Shooter (i4) solvers. Simon Says and Arrow Align block wrong clicks (hold Shift to click anyway).
- Blood Camp now follows Odin: a head is only tracked once the Watcher throws it, so the heads hanging on the walls no longer throw off the landing boxes. Adds a position box moved ahead by your ping, box colours and size, offset, spawn tick, interpolation and the Watcher bar (blood mobs left in the boss bar).
- Goldor tick timer counts 50 ticks by default (adjustable).
- `/s2 nick` has a new screen laid out like `/s2 custom`: name and toggles, colour swatches with Rainbow and a custom colour picker, and live TAB / chat / nametag previews.
- `/s2 recipe` only uses the Recipe HUD now; the panel next to the inventory is gone.
- HUDs keep their place relative to the screen when you change GUI scale or window size, instead of piling up at an edge. Storage search fits on the screen at high GUI scale.
- All boxes, lines and text in the world are drawn every frame, so they follow moving mobs and your camera smoothly.
- Last Breath RELEASE plays a bell by default (Bell, Note Block Bell, Ding, XP Orb or None) at full volume.
- Compact chat stacks repeats onto the newest message at the bottom instead of the old one higher up.
- Price tooltips: items sold on the bazaar show the bazaar insta-buy and insta-sell price where the lowest BIN and 3 day average would be (for the whole sack in the Sacks menu).
- The settings title shows the installed version (e.g. SkyJew Mod v1.2.3).
- `/s2 log` opens a changelog window instead of printing in chat: arrows (or the left and right keys) switch versions, and the list scrolls.
- Launchers show the mod as "SkyJew by 2m3s", with 2m3s's head as its icon.

### Added
- Dungeon routes folder: put any route file in `config/skyjew/dungeon route/` (Stella / SkyJew, SecretRoutes or Dungeon Rooms Mod style). Rooms without a route there use Stella's routes. Changes load automatically; `/s2 route reload` and `/s2 route folder` too.
- Water Board: Skyblocker's water path and lever previews, next to Odin's solver.
- SS skip helper: counts your Simon Says start button clicks above the button and blocks clicks past the limit (default 4).
- Starred mob highlight colour, fill and line width.
- Scrollable tooltips: scroll to move long tooltips (Shift for sideways).
- Toggle Sprint, with a key in Controls and a small HUD.
- "Your Class" option for the built-in positional waypoints, if your class isn't detected.
- `/s2 waypoints`: Skyblocker's waypoint editor, with groups per island, ordered waypoints (`/s2 waypoints ordered next|previous|reset`) and Skyblocker / Skytils / Coleweight import and export.
- `/s2 log`: the changes in the version you have installed, in game.
- Crystal Hollows waypoints, like Skyblocker: places you find are marked (Mines of Divan, Jungle Temple, Goblin Queen's Den, ...), coordinates in chat become waypoints, and `/s2 crystalwaypoints add|share|remove|clear`.
- Pickaxe ability HUD: cooldown of Mining Speed Boost, Pickobulus and the other abilities, with a ready alert.
- Warp shortcuts: `/dhub` instead of `/warp dhub`, and the same for every warp in Misc > Warp Shortcut List.
- Price Paid tooltip, like NoFrills: items you buy on the auction house show what you paid for them.
- Collection tracker, like SkyHanni's farming display but for every collection: while you mine, farm, forage or fish it shows your collection, what you've gained this session (and per hour) and your rank on the Elite collection leaderboard, with how much you need to pass the next player. Enchanted items from compactors count as the items they're made of (Enchanted Cobblestone = 160 Cobblestone).
- /s2 nick fonts: a Font button in the nickname menu with Bold, Italic, Small Caps, Full Width, Bubble, Script, Fraktur, Double Struck, Monospace, Sans Bold, Minecraft's enchanting and illager runes and Uniform. The nickname filter also sees through look-alike letters now.
- Custom Chat > Show SJ Chat: turn off to hide other players' `/sjc` messages.
- SJ chat rank prefixes, tied to the accounts themselves and managed from the tastyfish.org admin page (the mod checks for changes every 5 minutes). [OWNER] and [TESTER] are built in. Nicknames that copy a ranked player's name or a rank are not allowed.
- Portfolio: auction house and bazaar prices have their own buttons (AH: Lowest BIN / 3-day avg, BZ: Sell / Buy price), so all four combinations work.
- Portfolio: new rows get today's lowest BIN (or bazaar buy price) as their buy price; you can still change it.
- Portfolio: click any column header to sort by it, click again to reverse; the sorted column shows an arrow.
- Portfolio: item icons next to names, columns sized to fit so Qty and Buy each no longer overlap, sales can be removed from the Sold list, and the summary shows profit per hour (each row's profit divided by the hours since you added it).

### Fixed
- Portfolio: runes (e.g. Barkshatter Rune III) showed "?" as their price. Runes, pets and enchanted books are now counted and priced by their market ID, and rune names can be typed in.
- Water Board: the line to the next lever jumped around while moving. Only Odin's solver draws it now (Skyblocker's solver drew a second line), and it starts from the camera.
- Built-in positional waypoints (Py Stand Here, Mage Stop, ...) didn't show: your class is now read from any tab line with your name and remembered for the run, even as a ghost.
- Sharp Shooter (i4) solver didn't work.
- Pet display was blank when the server address had a port or wasn't exactly hypixel.net, and when Hypixel put icons around "[Lvl N]".
- Calendar real-time dates didn't show: calendar menus and dates written in tooltips are matched more loosely.
- Slayer tracker and boss phase HUDs didn't work: Hypixel's padding emoji in the sidebar broke the matching, and boss nametags are now text displays.
- Last Breath, Ice Spray, masks, arrows, farming tools and other items renamed with `/s2 custom` are still recognised.
- The 1/10,000 jumpscare no longer happens in dungeons.
- Storm pad tick timer was one tick short; it now counts down to 0 on the pad tick like Odin (the Goldor timer too).
- Superpairs solver didn't show the cards you had revealed; they now stay visible on their slots.
- Scrollable tooltips are now Skysoft's Tooltip Scroll: smooth panning, WASD / Page Up / Page Down keys, a reset key, speed and smoothness settings (Misc > Tooltip Scroll). It steps aside when Skysoft itself is installed.

### Removed
- Farming Profit Tracker (the Skysoft port).

## SkyBalls 1.2.2 — 2026-09-25

### Added
- Leap Menu (Dungeons): an Odin-style Spirit Leap menu with one box per teammate. Each class has its own colour (Archer orange, Mage blue, Berserk red, Healer purple, Tank green) and corner, and both can be changed.
- Blood Camp (Dungeons): Watcher move prediction with a Move Timer HUD and a "Kill Mobs" title, and kill timers that box where each blood mob lands with a countdown until it spawns.
- Door and key highlight: wither and blood doors are outlined green when your team has the key and red when locked; dropped keys are outlined and announced.
- Score (Dungeons): 270 and 300 score titles, sounds and chat messages, a separate party-chat toggle and editable message for each (sent as "[SJ] ..."), and a Score Display HUD with score, secrets, crypts, deaths and mimic/prince.
- Positional messages: `/s2 posmsg add here|at|in ...` sends a party message when you reach a spot, like Odin. Built-in waypoints include "Py Stand Here" at 95, 165.5, 94.4 during Storm on floor 7 (Mage only), "Mage Stop" at 34, 169, 65 during Storm (Mage only), "Arch Stand Here" at 102-104, 168, 49 during Storm (Archer only), "Tank Stand Here" at 109, 170, 93 during Storm (Tank only), "Healer Stand Here After Lighting" at 58, 169, 66 during Storm (Healer only), and "SS" during Goldor until Necron (Healer only): the block at 109, 120, 93 is highlighted and standing at 108, 120, 93 sends "At SS" to party chat once. Radius messages draw an Odin-style ring.
- `/s2 route import`: imports routes from your clipboard or a file (Stella / SkyJew format, or SecretRoutes files).
- Last Breath release cue: a sound and RELEASE title once you have charged Last Breath for 5 server ticks (adjustable).
- Misc > Random: Low Fire (lowers the burning overlay) and Hide Explosions.
- Platform Highlight (3x3) (Dungeons): one box over the floor 7 3x3 platform (x 53-55, z 113-115) from when Goldor starts, like NoFrills, with Healer Only, style and colour options.
- Pristine Record (Mining): remembers your highest pristine proc, overall and per gemstone, and alerts on a new PB. `/s2 pristine` lists them.
- Portfolio (`/s2 portfolio`): track items you own (e.g. pet skins) with live prices. Quantities are counted from your inventory and every Ender Chest / backpack page you have opened, plus an extra amount you can type. Set a buy price to see profit/loss. Auction and bazaar sales of tracked items can be logged (asking first or automatically), value history is saved every 30 minutes with an in-game graph, and Export CSV writes portfolio.csv, history.csv and sold.csv for Excel.
- `/s2 recipe` now works like SkyOcean's craft helper: the panel next to your inventory has -/+ buttons for the amount (Shift 10, Ctrl 64), and a movable Recipe HUD shows the item and the base ingredients you still need while you play (Misc > Recipe HUD).
- Misc > Player Size: make yourself, other players, or both smaller or bigger (client side), like Odin.
- Misc > Held Item Model: move, rotate and scale the item in your hand and change swing speed, like Skysoft; `/s2 helditem save` stores settings for one item.

### Changed
- Splits now work like Odin's: Blood Open, Blood Clear, Portal Entry, each boss phase and Total, with Boss Entry, tick times, per-floor personal bests and a "took" message after each split.
- Mask timers now follow Odin: Spirit, Bonzo and Phoenix always show invincibility time, cooldown or ready, counted in server ticks, with your Bonzo cooldown read from the mask. Optional party announce.
- Nicknames now also replace your name in Hypixel's name lines above heads, not just the vanilla nametag.
- Starred mob boxes no longer show through walls.
- The puzzle solvers now use Odin's: Ice Fill (with optional shorter paths), Boulder, Creeper Beams, Three Weirdos, Quiz, Teleport Maze, Water Board (with optional faster solutions) and Blaze. Tic Tac Toe and Silverfish keep their solvers.

### Fixed
- SkyJew now reads chat straight from the server, so it still sees [BOSS] and [NPC] lines when another mod hides them. This was why Simon Says, splits, boss tick timers, the dungeon score and the map did not start.
- Simon Says also turns on near the device on floor 7 if the Maxor message was missed.
- Terminal solver highlights are drawn with the slots, so mods that hide tooltips in terminals no longer hide them.
- The dungeon map now updates whenever Hypixel sends a map update, not only when a room is identified.
- The dungeon floor is read correctly from the sidebar ("Floor pattern doesn't match").
- Beacon beams from mods built for Minecraft 26.1 (such as Odin's quiz and terminal beams) no longer show the missing texture.
- The "Kills Since Rare Drop" setting was not saved.
- Storage search (`/s2 search`) kept one storage for every profile, so your ironman showed your main profile's items. Each SkyBlock profile now has its own storage (reopen your Ender Chest and backpacks once per profile).

## SkyBalls 1.2.1 — 2026-09-25

### Fixed
- The Zealot Tracker could count zealots other players killed right next to you. A kill now only counts when you also get the Combat XP for it.
- The Item Price Tooltip didn't show NPC sell prices. The lines are now in the order NPC Sell Price, Lowest BIN Price, 3 Day Avg. Price, and the settings are in the same order.

## SkyBalls 1.2 — 2026-09-25

### Added
- Commands are no longer case-sensitive: `/SJ GUI` works the same as `/s2 gui`.
- Item Price Tooltip (Misc): 3 day average price, lowest BIN and NPC sell price on SkyBlock items, like Skyblocker.
- Current chat display: shows which chat you're typing in (All, Party, Guild, Officer, Co-op, a private conversation or SkyJew chat) above the chat box, like SkyHanni.
- Ultrasequencer order numbers: every slot shows its place in the order, with the next one in yellow.

### Changed
- The zealot counter is now a Zealot Tracker in the style of SkyHanni's trackers: kills, Summoning Eyes and kills since your last eye. Click Total / This Session while your inventory is open to switch. Only your own kills count, including Hyperion multi-kills.
- HUD elements no longer have a background by default. Right-click any HUD in `/s2 gui` to turn its background on.
- In every settings section, dropdowns are now listed above the single toggles.
- The Enchanting tab has moved into Misc as the Experimental Table section. Your settings carry over.
- Settings now save as soon as you change them, not only when the settings screen closes.
- The "no custom skin" barrier is now the first slot in the helmet skin picker.
- In the helmet skin picker, animated heads only animate when you hover or select them.
- The README is much shorter.

### Removed
- The Alchemy tab and the Alchemy 50 Estimate HUD.
- The Hunting tab (Safari Critter Tracker and Hunting Box Value), for now.

### Fixed
- Animated helmet skins (Celestial Necron/Storm/Goldor, Golden Dragon Swap Plushies and every other animated head) couldn't be applied: selecting one cleared the skin instead. They now apply, wait for each frame to load instead of flashing Steve, and Cancel restores them properly. Skins that fail to load are retried.
- The zealot counter counted other players' kills.
- The item rarity style went back to Square after restarting the game.
- The Chronomatron solver didn't highlight anything.
- Calendar real-time dates didn't show in the calendar menus.

## SkyBalls 1.1 — 2026-09-25

### Added

**General**
- `/s2 gui` can now move and resize every HUD element, not just a few. Elements snap to the screen edges and can't be dragged off screen.
- Right-click any HUD in `/s2 gui` to turn its background on or off.
- `/s2 keys` now uses a full port of the CommandKeys mod (keybinds, macros and profiles). Your old command keys are migrated automatically.
- `/s2 custom` has been rebuilt as a port of Skyblocker's item customisation: custom names, dyes (including animated dyes), armor trims and helmet textures.
- `/s2 recipe <item> [amount]` shows a SkyOcean-style craft helper. It shows the full recipe tree and what you still need, counting items in your storage. It also has `/s2 recipe amount <n>` and `/s2 recipe clear`.
- Item Rarity is now a dropdown with style and opacity options. It works in your inventory, in containers and on the hotbar.
- SkyBlock calendar time and a date calculator, ported from Skyblocker.
- Nicknames now show above players' heads and in the tab list for everyone using SkyJew. Hovering a nickname shows the player's real name. Offensive nicknames are blocked.
- SJ chat now shows each player's SkyBlock level in its level colour. Hovering a name shows the real name, and clicking starts a `/msg`.
- `/sjc <message>` sends a message to SJ chat. `/chat sj` switches your chat to SJ chat, and `/chat <anything else>` switches back.

**Pets**
- The pet HUD now shows overflow levels automatically from the tab list, like NopoMod. You no longer need to open the Pets menu. It also shows progress to the next level.
- Pet display background option.

**Combat**
- Arrow counter: your selected arrow and how many are left in your quiver.
- Zealot counter: zealot kills, kills since your last Summoning Eye and eyes dropped.
- Legion display: how many players are in Legion range.
- Cocoon alert for slayer bosses, slayer minibosses, elusive mobs and important bosses.
- Rare drops: copy drop messages and show an animation for valuable drops.

**Slayer**
- Slayer tracker: progress to spawning the boss, XP to your next level and session totals.
- Boss phase display showing your boss's nametag lines (Voidgloom hits, Inferno attunement and so on).

**Garden**
- Yaw, pitch and facing direction HUD (Garden only).
- Pest cooldown, blocks-per-second and special drop animation.
- Mouse lock can be set to only work in the Garden.

**Other skills**
- Fishing, mining (Crystal Hollows map, Mineshaft timer), foraging, alchemy, enchanting, runecrafting and hunting features.

**Dungeons** (ported from Skyblocker)
- Dungeon map with player heads and room names.
- Puzzle solvers: Tic Tac Toe, Three Weirdos, Creeper Beams, Water Board, Blaze, Boulder, Ice Fill, Silverfish, Trivia and Teleport Maze.
- Secret waypoints.
- Floor 7 terminal and device solvers.
- Splits, tick timers, mask timers and an M7 debuff alert.
- Highlight starred mobs: draws a box around ✯ mobs, visible through walls.
- Secret routes in Stella's format. Rooms without your own route use Stella's routes.
  - `/s2 route start` records a route automatically: secrets, levers, etherwarps, superbooms, pearls and mined blocks. `/s2 route stop` saves it.
  - `/s2 route next` / `back` step through a route, and `/s2 route clear` deletes your route for the current room.
  - All your routes are saved in one file. `/s2 export` writes them to `routes-export.json` and copies them to your clipboard.

**Party**
- Party commands.

### Changed
- The notes screen (`/s2 notes`) has been rebuilt with a proper multi-line editor and autosave.
- The cocoon alert no longer has a mob list text box. It alerts for important mobs only.
- The commission HUD now looks like Skyblocker's. Its background toggle is now saved.

### Removed
- The "you have been ratted" start-up screen and its sound.
- The slayer profit tracker, for now.

### Fixed
- HUDs saved off screen (such as the zealot counter) now appear on screen.
- The zealot counter never counted kills.
- The arrow counter didn't detect your arrows.
- Pet level was shown twice in the pet HUD.
- Item rarity backgrounds didn't show in the player inventory.
- Calendar time was wrong.
- `/s2 search` still showed the first page's items on later pages.
- Image link previews in SJ chat stopped showing on hover.
- Other players' nicknames in the tab list.
- A crash on start-up when items were created too early.
- A crash from nicknames being applied repeatedly in the tab list.

## SkyBalls 1 — 2026-09-25

First release.

- Farming RNG tracker and HUD.
- Storage search (`/s2 search`).
- Custom item tools (`/s2 custom`).
- Command keys (`/s2 keys`).
- Notes (`/s2 notes`).
- Main `/s2` GUI.
- Version checker.
