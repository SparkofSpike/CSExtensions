# CSExtensions
### *Crackshot's addon for Paper 1.8.8, another "CrackshotPlus"*

[![Java](https://img.shields.io/badge/Java-8%2B-orange)](https://java.com)
[![CrackShot](https://img.shields.io/badge/CrackShot-Required-red)](https://www.spigotmc.org/resources/48399)
[![MythicMobs](https://img.shields.io/badge/MythicMobs-Optional-purple)](https://mythicmobs.net)

Remember back in 2015 when you were running a 1.8.8 server and desperately wanted those fancy CrackShotPlus features everyone was talking about? Then serveral years later, you actually tried CSP and realized it's held together by duct tape, prayers, and the tears of innocent developers?

To be honest, Crackshot is a great plugin. I admire its creator. However, considering the current situation, its functions are really not satisfactory. CrackshotPlus is an extremely outstanding addon. Unfortunately, it was also limited by the times. If you want to experience more powerful functions, you have to give up the old version 1.8.8. But I don't want to do that!

Yeah. That's why this exists.

CSExtensions is my mental breakdown turned into a plugin. It adds actual useful features to CrackShot without making you want to throw your computer out the window. Well, maybe just a little.

---

## 📦 **What This Actually Does**

The plugin splits into two parts because I'm not a complete madman (debatable):

### 🔫 **Part 1: cse_guns.yml - Making CrackShot Less Shitty**

This is where we fix what Shampaggon broke. Every weapon in your CrackShot config can now have:

**Trails System** - Remember when you wanted cool particle effects following your bullets? Now you can have them. Flames, smoke, critical hits, colored particles, circle trails, straight trails, trails that go through walls... I went a little overboard. Each trail is fully customizable with multiple effects per weapon, adjustable length and density, color support, and even advanced circular patterns with radius control. You can even decide whether they pass through walls, players, or nothing at all.

**Projectile Enhancements** - Make your bullets actually do what you want. Hide them completely for those "magic" weapons that leave no trace. Remove knockback entirely because it messes up combo shots and annoys players. Let bullets penetrate through multiple enemies with configurable damage reduction so you can feel like a real badass mowing down a line of targets. Make them bounce off walls - and if you're feeling lucky, enable auto-aim to track the nearest target after each bounce. Or go full sci-fi with homing missiles that actually track targets with adjustable range, angle, and turn speed.

**Health Adjustment** - Want a weapon that heals you on kill? Drains your health when you shoot? Instantly kills you for using it? All possible. Set up self-healing on kills, headshots, or reloads. Add target damage with kill credit options. Create constant healing over time effects. I don't judge your playstyle.

**Bar Message Groups** - Different reload bars for different weapon groups. Snipers get a fancy long bar, pistols get a quick simple one. If you have ever used CrackshotPlus, you will easily understand what I'm saying.

**MythicMobs Integration** - Trigger MM skills when you shoot, reload, get a headshot, land a critical hit, attack, take damage, hit a block, or even on a timer. Use any targeter you want - the shooter, the victim, the hit location, or trigger the skill on yourself. Yes, it all works. No, I didn't test every single combination but hey, that's what production servers are for. Don't ask why this needed to exist. I had a reason at 3 AM and it made perfect sense at the time.
---

### 💍 **Part 2: accessories.yml - The Thing I Actually Had Fun Writing**

This is where I went completely off the rails. Accessories are like CrackShot attachments but better, because I said so. Players can equip them for passive bonuses, and they're fully configurable in ways that will either delight you or make you question my sanity.

**Complex Condition System** - Want a ring that gives +10 damage only when the weapon is NOT fire or ice type, BUT it IS magic type, AND you're below half health, AND it's a headshot? Go for it. The condition system supports AND, OR, and NOT operators with full parenthesis support for complex logic. It actually works, I swear. Just remember that YAML thinks exclamation marks and ampersands are special characters, so wrap your whole expression in parentheses and everything will be fine.

**Multiple Modifiers** - Modify damage, reload speed, and bullet spread with different operators. Use ADD to flat increase values, MUL for percentage multipliers, or FIX to set exact numbers. There's also a FLAT operator that I never properly tested, so maybe don't use that one unless you're feeling adventurous.

**Conflict Groups** - Prevent players from wearing both an Attack Ring and a Mega Attack Ring because balance is apparently a thing server owners care about. Set a limit, define which accessories conflict, and customize the error message when someone tries to break the rules. "Nice try, cheater" is the default but you can make it as polite or as passive-aggressive as you want.

**Health Adjustment** - Increase max health. Or decrease it. Make your players tankier or make them suffer. Positive numbers add hearts, negative numbers remove them. There's a safety mechanism to prevent health from going below zero because even I have limits.

**Weight System** - Weight affects movement speed. Weight of 1 slows players down by 100% - they basically can't move. Weight of -1 speeds them up by 100% - they become Sonic the Hedgehog. Why can't you configure it more precisely with custom values? Because I got lazy writing that part. Deal with it.

**MythicMobs Integration** - Yes, accessories can trigger MythicMobs skills too. Because why should guns have all the fun? Wear a cursed ring that damages you over time, or a holy amulet that heals you when you kill someone. The possibilities are endless and barely tested.

---

## ⚠️ **Things You Should Know (The "I'm Not Fixing This" Section)**

**The Return Feature is Broken** - Yeah, about that projectile return thing... don't use it. It has bugs. Like, actual bugs. Not "this is a feature" bugs. I tried to fix it, got frustrated, and decided to keep it as a monument to my failure. If you enable it and your projectiles start flying backward through time or duplicating themselves or chasing after the shooter instead of returning nicely, you were warned. It stays in the config as a reminder that some battles aren't worth fighting.

**Too many Bugs** - After all, it's just a plugin I wrote by myself and it hasn't undergone any rigorous testing. It might cause your server to freeze, make your computer explode, or trigger the Third World War or even cause the Earth to fall into a black hole. Who knows? However, if anyone really discovers a bug, please contact me. Maybe I can fix it? But anyway, since the code is open source, you can also fix it yourself, lol.

---

## 🎮 **Commands (Because Every Plugin Needs These)**

| Command | Permission | What It Does |
|---------|------------|--------------|
| `/cse` | None | Shows help (wow, helpful) |
| `/cse reload` | csextensions.reload | Reloads configs when you inevitably break them |
| `/cse give <id> [player] [amount]` | csextensions.give | Gives accessories like the admin you are |
| `/cse list` | csextensions.list | Shows all accessories because you forgot the IDs |
| `/cse weapons` | csextensions.weapons | Lists configured weapons with their trail status |
| `/cse trails <weapon>` | csextensions.trails | Shows trail config for a specific weapon |
| `/cse debug` | csextensions.debug | Toggles debug mode (prints way too much in console) |
| `/cse particles` | csextensions.debug | Lists all available particles in console |
| `/cse test` | csextensions.debug | Only used by me.Maybe make your server explode |

---

## 🔧 **Installation (It's Not Hard)**

1. Put CSExtensions.jar in your plugins folder
2. Put CrackShot.jar in your plugins folder (it literally won't work without it)
3. Optionally put MythicMobs.jar if you want those features
4. Restart your server or reload - I'm not your mom, do what you want
5. Edit plugins/CSExtensions/config.yml to enable/disable modules
6. Configure your weapons in cse_guns.yml (the file generates with examples)
7. Create accessories in accessories.yml (also generates with examples)
8. Input "/cse reload" and pray that the solar system won't be destroyed as a result
9. Question your life choices that led you to read this far
10. Understand that the author is a complete idiot
11. Realize it's too late to turn back now
12. Then realize that you are an idiot too

---

## 📁 **Configuration Overview**

The plugin generates three main files on first run:

**config.yml** - The boring one. Enable or disable modules, toggle debug mode, set global defaults. You'll look at it once and never touch it again.

**cse_guns.yml** - This is where the magic happens. Each weapon from your CrackShot config gets its own section here. Add trails, projectile effects, health adjustments, and MythicMobs triggers. The file comes with commented examples showing every single option. I was nice for approximately 15 minutes while writing those comments.

**accessories.yml** - The fun one. Define custom accessories with names, materials, lore, and effects. Set up damage multipliers with complex conditions, conflict groups to prevent overpowered combinations, health adjustments, weight values, and even MythicMobs triggers. Go wild.

---

## ❓ **FAQ (Actually no one asked)**

**Q: Will this work on 1.16+?**  
A: Probably not. I wrote this for 1.8.8 and never tested anything else. If it works on newer versions, that's a happy accident. If it doesn't, that's expected behavior.

**Q: Can you add [feature]?**  
A: No. Or maybe? I don't know. Lol.

**Q: But what if I pay you?**  
A: YESYESYES!

**Q: Why did you write this plugin?**  
A: Insomnia, wine, coffein, and a burning hatred for poorly maintained alternatives. Also because I could.

**Q: Is it production-ready?**  
A: Define production. Define ready. Define is. Some servers have been running it for months without issues. Some servers caught fire immediately. Results may vary.

**Q: The Return feature is broken, why keep it?**  
A: As a monument to my failures and a warning to others who would tread that path.

**Q: Do you accept pull requests?**  
A: Sure, but I probably won't merge them. Feel free to fork your own version.

---

## 📜 **License**

Do whatever you want with it. Fork it, modify it, sell it, burn it, print it out and use it as wallpaper or toiletpaper. I genuinely don't care. If you make millions off this plugin, please buy me a wine. If you break your server, buy yourself a wine. We all need wine to live.

---

*If you actually read this entire README, congratulations. You now know more about my mental state than my therapist does. The plugin probably works. Probably. Open an issue if it doesn't, but don't expect an immediate response. Or any response. Actually just assume there will be no response and be pleasantly surprised if there is.*
