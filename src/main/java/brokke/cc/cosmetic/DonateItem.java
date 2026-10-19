package brokke.cc.cosmetic;

public enum DonateItem {
   TAL_KRUSH("[★] Талисман Крушителя", new String[]{"tal-krush", "attribute-item-tkryshitela"}, 1, 15),
   TAL_KARATEL("[★] Талисман Карателя", new String[]{"tal-karatelya", "attribute-item-tkaratela"}, 3, 14),
   TAL_GARMONIA("[★] Талисман Гармонии", new String[]{"tal-garmonii", "attribute-item-tgarmonii"}, 2, 12),
   TAL_DEDALA("[★] Талисман Дедала", new String[]{"tal-dedala", "attribute-item-tdedala"}, 4, 11),
   TAL_EHIDNA("[★] Талисман Ехидна", new String[]{"tal-ehidna", "attribute-item-texidni"}, 6, 10),
   TAL_GRAN("[★] Талисман Грани", new String[]{"tal-grani", "attribute-item-tgrani"}, 5, 7),
   TAL_TRITON("[★] Талисман Тритона", new String[]{"tal-tritona", "attribute-item-ttritona"}, 8, 2),
   TAL_FENIX("[★] Талисман Феникс", new String[]{"tal-fenixa", "attribute-item-tfeniksa"}, 7, 1),
   TAL_INFINITY(
      "Талисман INFINITY", new String[]{"Infinity", "INFINITY", "infinity", "tal_infinity", "tal-infinity", "talisman_infinity", "talisman-infinity"}, 1, 1
   ),
   TAL_ETERNITY(
      "Талисман ETERNITY", new String[]{"Eternity", "ETERNITY", "eternity", "tal_eternity", "tal-eternity", "talisman_eternity", "talisman-eternity"}, 2, 1
   ),
   TAL_STINGER("Талисман STINGER", new String[]{"Stinger", "STINGER", "stinger", "tal_stinger", "tal-stinger", "talisman_stinger", "talisman-stinger"}, 3, 1),
   TAL_SATIRI("Талисман Сатиры", new String[]{"Satir", "SATIR", "satir", "Satiri", "SATIRI", "satiri", "tal_satir", "tal-satir"}, 4, 1),
   TAL_MIF("Мифический талисман", new String[]{"MYTHICAL", "mythical", "Mythical"}, 5, 1),
   TAL_LEG("Легендарный талисман", new String[]{"LEGENDARY", "legendary", "Legendary"}, 6, 1),
   TAL_EPIC("Эпический талисман", new String[]{"EPIC", "epic", "Epic"}, 7, 1),
   TAL_DEFAULT("Обычный талисман", new String[]{"NORMAL", "normal", "Normal"}, 8, 1);

   private final String name;
   private final String[] nbt;
   private final int totem;
   private final int factor;

   DonateItem(String nullxx, String[] nullxxx, int nullxxxx, int nullxxxxx) {
      this.name = nullxx;
      this.nbt = nullxxx;
      this.totem = nullxxxx;
      this.factor = nullxxxxx;
   }

   public String getName() {
      return this.name;
   }

   public String[] getNbt() {
      return this.nbt;
   }

   public int getTotem() {
      return this.totem;
   }

   public int getFactor() {
      return this.factor;
   }
}
