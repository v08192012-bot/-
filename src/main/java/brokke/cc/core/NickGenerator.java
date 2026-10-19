package brokke.cc.core;

import brokke.cc.manager.AccountManager;
import java.util.Random;

public class NickGenerator {
   private static final Random RANDOM = new Random();
   private static final String[] ADJECTIVES = new String[]{
      "Black",
      "Dark",
      "White",
      "Red",
      "Blue",
      "Green",
      "Silver",
      "Golden",
      "Neon",
      "Cyber",
      "Frost",
      "Cold",
      "Frozen",
      "Storm",
      "Night",
      "Shadow",
      "Silent",
      "Ghost",
      "Holy",
      "Grim",
      "Wild",
      "Brave",
      "Swift",
      "Epic",
      "Grand",
      "Royal",
      "Noble",
      "Mad",
      "Pure",
      "True",
      "Sharp",
      "Fast",
      "Hard",
      "Fierce",
      "Lucky",
      "Crazy",
      "Savage",
      "Bright",
      "Blood",
      "Magic",
      "Mystic",
      "Alpha",
      "Omega",
      "Mega",
      "Ultra",
      "Super",
      "Elite",
      "Prime",
      "Apex",
      "Atomic",
      "Astral",
      "Chaos",
      "Vivid",
      "Static",
      "Solar",
      "Lunar",
      "Toxic",
      "Hyper",
      "Iron",
      "Gold",
      "Crystal",
      "Ruby",
      "Diamond",
      "Crimson",
      "Emerald",
      "Phantom",
      "Ancient",
      "Secret",
      "Velvet",
      "Spectral"
   };
   private static final String[] NOUNS = new String[]{
      "Panda",
      "Knight",
      "Wolf",
      "Fox",
      "Bear",
      "Hawk",
      "Eagle",
      "Lion",
      "Tiger",
      "Dragon",
      "Viper",
      "Blade",
      "Sword",
      "Shield",
      "Arrow",
      "Bow",
      "Spear",
      "Axe",
      "Hammer",
      "Strike",
      "Hunter",
      "Rider",
      "Walker",
      "Runner",
      "Seeker",
      "Slayer",
      "Killer",
      "Fighter",
      "Master",
      "Lord",
      "King",
      "Prince",
      "Hero",
      "Ghost",
      "Phantom",
      "Specter",
      "Shadow",
      "Ninja",
      "Samurai",
      "Warrior",
      "Storm",
      "Flame",
      "Frost",
      "Fire",
      "Ice",
      "Thunder",
      "Spark",
      "Flash",
      "Blast",
      "Pulse",
      "Star",
      "Moon",
      "Sun",
      "Comet",
      "Meteor",
      "Nova",
      "Vortex",
      "Apex",
      "Titan",
      "Raptor",
      "Raven",
      "Cobra",
      "Shark",
      "Falcon",
      "Golem",
      "Demon",
      "Angel",
      "Wizard",
      "Reaper",
      "Panther",
      "Jaguar",
      "Cheetah",
      "Badger",
      "Otter",
      "Lynx",
      "Bison",
      "Mammoth",
      "Kraken",
      "Hydra",
      "Griffin"
   };
   private static final String[] STANDALONE = new String[]{
      "Vortex",
      "Eclipse",
      "Specter",
      "Phantom",
      "Inferno",
      "Chronos",
      "Tempest",
      "Cobalt",
      "Zenith",
      "Nemesis",
      "Valkyrie",
      "Obsidian",
      "Titanium",
      "Cataclysm",
      "Paradox",
      "Mirage",
      "Horizon",
      "Abyss",
      "Glitch",
      "Reaper",
      "Blaze",
      "Nexus",
      "Titan",
      "Phoenix",
      "Striker",
      "Cosmo",
      "Raptor",
      "Samurai",
      "Sentinel",
      "Colossus",
      "Avalanche",
      "Overlord",
      "Dominator",
      "Wanderer",
      "Crusader",
      "Immortal",
      "Predator",
      "Enigma",
      "Quantum",
      "Requiem",
      "NightHunter",
      "StarGazer",
      "FireStorm",
      "IceBreaker",
      "SoulSeeker",
      "BloodHound",
      "SkyWalker",
      "MoonShadow",
      "SunStriker",
      "WindRunner",
      "StormRider",
      "ShadowWalker",
      "DragonSlayer",
      "IronHeart"
   };
   private static final String[] PREFIXES = new String[]{"The", "Mr", "Sir", "Dr", "Lord", "King", "Real", "Its", "Just", "Only", "Captain", "Master"};
   private static final String[] SUFFIXES = new String[]{
      "Pro", "God", "Top", "PvP", "Dev", "King", "Lord", "Master", "Boss", "Hero", "Prime", "One", "Elite", "Main"
   };
   private static final String[] TRANSLIT = new String[]{
      "Tasher",
      "Legenda",
      "Metkiy",
      "Lovkiy",
      "Prizrak",
      "Sokol",
      "Grom",
      "Veter",
      "Knyaz",
      "Bumer",
      "Koldun",
      "Kobra",
      "Bratik",
      "Keksik",
      "Shmel",
      "Volk",
      "Medved",
      "Barsik",
      "Kotik",
      "Rytsar",
      "Vityaz",
      "Bogatyr",
      "Molot",
      "Klinok",
      "Strela",
      "Ohotnik",
      "Drakon",
      "Pobeda",
      "Sever",
      "Burya"
   };

   public static String a() {
      for (int var0 = 0; var0 < 1000; var0++) {
         String var1 = a(var0);
         String var6 = a(var1);
         if (b(var6) && !AccountManager.a(var6)) {
            AccountManager.b(var6);
            return var6;
         }
      }

      long var5 = System.currentTimeMillis() % 100000L;

      while (true) {
         String var2 = RANDOM.nextBoolean()
            ? ADJECTIVES[RANDOM.nextInt(ADJECTIVES.length)] + "_" + NOUNS[RANDOM.nextInt(NOUNS.length)]
            : STANDALONE[RANDOM.nextInt(STANDALONE.length)];
         String var3 = "_" + var5;
         String var4 = a(a(var2, 16 - var3.length()) + var3);
         if (!AccountManager.a(var4)) {
            AccountManager.b(var4);
            return var4;
         }

         var5++;
      }
   }

   private static String a(int var0) {
      int var1 = var0 > 300 ? RANDOM.nextInt(9) : var0 % 8;

      return switch (var1) {
         case 0 -> ADJECTIVES[RANDOM.nextInt(ADJECTIVES.length)] + "_" + NOUNS[RANDOM.nextInt(NOUNS.length)];
         case 1 -> ADJECTIVES[RANDOM.nextInt(ADJECTIVES.length)] + NOUNS[RANDOM.nextInt(NOUNS.length)];
         case 2 -> PREFIXES[RANDOM.nextInt(PREFIXES.length)] + "_" + NOUNS[RANDOM.nextInt(NOUNS.length)];
         case 3 -> PREFIXES[RANDOM.nextInt(PREFIXES.length)] + NOUNS[RANDOM.nextInt(NOUNS.length)];
         case 4 -> NOUNS[RANDOM.nextInt(NOUNS.length)] + "_" + SUFFIXES[RANDOM.nextInt(SUFFIXES.length)];
         case 5 -> NOUNS[RANDOM.nextInt(NOUNS.length)] + SUFFIXES[RANDOM.nextInt(SUFFIXES.length)];
         case 6 -> STANDALONE[RANDOM.nextInt(STANDALONE.length)] + (RANDOM.nextInt(4) == 0 ? "_" + (1 + RANDOM.nextInt(99)) : "");
         case 7 -> TRANSLIT[RANDOM.nextInt(TRANSLIT.length)] + (RANDOM.nextInt(3) == 0 ? "_" + (1 + RANDOM.nextInt(99)) : "");
         default -> ADJECTIVES[RANDOM.nextInt(ADJECTIVES.length)] + "_" + NOUNS[RANDOM.nextInt(NOUNS.length)] + "_" + (1 + RANDOM.nextInt(99));
      };
   }

   private static String a(String var0, int var1) {
      if (var0 == null) {
         return "";
      } else {
         return var0.length() <= var1 ? var0 : var0.substring(0, Math.max(1, var1));
      }
   }

   private static String a(String var0) {
      if (var0 == null) {
         return "Player" + RANDOM.nextInt(1000);
      }

      String var1 = var0.replaceAll("[^a-zA-Z0-9_]", "");
      if (var1.length() > 16) {
         var1 = var1.substring(0, 16);
      }

      if (var1.length() < 3) {
         var1 = var1 + (100 + RANDOM.nextInt(900));
         if (var1.length() > 16) {
            var1 = var1.substring(0, 16);
         }
      }

      return var1;
   }

   private static boolean b(String var0) {
      return var0 != null && var0.length() >= 3 && var0.length() <= 16 && var0.matches("^[a-zA-Z0-9_]+$");
   }
}
