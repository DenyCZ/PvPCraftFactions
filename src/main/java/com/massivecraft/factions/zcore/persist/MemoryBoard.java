package com.massivecraft.factions.zcore.persist;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.massivecraft.factions.*;
import com.massivecraft.factions.struct.Relation;
import com.massivecraft.factions.util.AsciiCompass;
import com.massivecraft.factions.util.LazyLocation;
import org.bukkit.ChatColor;
import org.bukkit.World;

import java.util.*;
import java.util.Map.Entry;


public abstract class MemoryBoard extends Board {

    public class MemoryBoardMap extends HashMap<FLocation, String> {
        private static final long serialVersionUID = -6689617828610585368L;

        Multimap<String, FLocation> factionToLandMap = HashMultimap.create();

        @Override
        public String put(FLocation floc, String factionId) {
            String previousValue = super.put(floc, factionId);
            if (previousValue != null) {
                factionToLandMap.remove(previousValue, floc);
            }

            factionToLandMap.put(factionId, floc);
            return previousValue;
        }

        @Override
        public String remove(Object key) {
            String result = super.remove(key);
            if (result != null) {
                FLocation floc = (FLocation) key;
                factionToLandMap.remove(result, floc);
            }

            return result;
        }

        @Override
        public void clear() {
            super.clear();
            factionToLandMap.clear();
        }

        public int getOwnedLandCount(String factionId) {
            return factionToLandMap.get(factionId).size();
        }

        public void removeFaction(String factionId) {
            Collection<FLocation> flocations = factionToLandMap.removeAll(factionId);
            for (FLocation floc : flocations) {
                super.remove(floc);
            }
        }
    }

    public MemoryBoardMap flocationIds = new MemoryBoardMap();

    //----------------------------------------------//
    // Get and Set
    //----------------------------------------------//
    public String getIdAt(FLocation flocation) {
        if (!flocationIds.containsKey(flocation)) {
            return "0";
        }

        return flocationIds.get(flocation);
    }

    public Faction getFactionAt(FLocation flocation) {
        return Factions.getInstance().getFactionById(getIdAt(flocation));
    }

    public void setIdAt(String id, FLocation flocation) {
        clearOwnershipAt(flocation);

        if (id.equals("0")) {
            removeAt(flocation);
        }

        flocationIds.put(flocation, id);
    }

    public void setFactionAt(Faction faction, FLocation flocation) {
        setIdAt(faction.getId(), flocation);
    }

    public void removeAt(FLocation flocation) {
        Faction faction = getFactionAt(flocation);
        Iterator<LazyLocation> it = faction.getWarps().values().iterator();
        while (it.hasNext()) {
            if (flocation.isInChunk(it.next().getLocation())) {
                it.remove();
            }
        }
        clearOwnershipAt(flocation);
        flocationIds.remove(flocation);
    }

    public Set<FLocation> getAllClaims(String factionId) {
        Set<FLocation> locs = new HashSet<>();
        for (Entry<FLocation, String> entry : flocationIds.entrySet()) {
            if (entry.getValue().equals(factionId)) {
                locs.add(entry.getKey());
            }
        }
        return locs;
    }

    public Set<FLocation> getAllClaims(Faction faction) {
        return getAllClaims(faction.getId());
    }

    // not to be confused with claims, ownership referring to further member-specific ownership of a claim
    public void clearOwnershipAt(FLocation flocation) {
        Faction faction = getFactionAt(flocation);
        if (faction != null && faction.isNormal()) {
            faction.clearClaimOwnership(flocation);
        }
    }

    public void unclaimAll(String factionId) {
        Faction faction = Factions.getInstance().getFactionById(factionId);
        if (faction != null && faction.isNormal()) {
            faction.clearAllClaimOwnership();
            faction.clearWarps();
        }
        clean(factionId);
    }

    public void unclaimAllInWorld(String factionId, World world) {
        for (FLocation loc : getAllClaims(factionId)) {
            if (loc.getWorldName().equals(world.getName())) {
                removeAt(loc);
            }
        }
    }

    public void clean(String factionId) {
        flocationIds.removeFaction(factionId);
    }

    // Is this coord NOT completely surrounded by coords claimed by the same faction?
    // Simpler: Is there any nearby coord with a faction other than the faction here?
    public boolean isBorderLocation(FLocation flocation) {
        Faction faction = getFactionAt(flocation);
        FLocation a = flocation.getRelative(1, 0);
        FLocation b = flocation.getRelative(-1, 0);
        FLocation c = flocation.getRelative(0, 1);
        FLocation d = flocation.getRelative(0, -1);
        return faction != getFactionAt(a) || faction != getFactionAt(b) || faction != getFactionAt(c) || faction != getFactionAt(d);
    }

    // Is this coord connected to any coord claimed by the specified faction?
    public boolean isConnectedLocation(FLocation flocation, Faction faction) {
        FLocation a = flocation.getRelative(1, 0);
        FLocation b = flocation.getRelative(-1, 0);
        FLocation c = flocation.getRelative(0, 1);
        FLocation d = flocation.getRelative(0, -1);
        return faction == getFactionAt(a) || faction == getFactionAt(b) || faction == getFactionAt(c) || faction == getFactionAt(d);
    }

    /**
     * Checks if there is another faction within a given radius other than Wilderness. Used for HCF feature that
     * requires a 'buffer' between factions.
     *
     * @param flocation - center location.
     * @param faction   - faction checking for.
     * @param radius    - chunk radius to check.
     * @return true if another Faction is within the radius, otherwise false.
     */
    public boolean hasFactionWithin(FLocation flocation, Faction faction, int radius) {
        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                if (x == 0 && z == 0) {
                    continue;
                }

                FLocation relative = flocation.getRelative(x, z);
                Faction other = getFactionAt(relative);

                if (other.isNormal() && other != faction) {
                    return true;
                }
            }
        }
        return false;
    }


    //----------------------------------------------//
    // Cleaner. Remove orphaned foreign keys
    //----------------------------------------------//

    public void clean() {
        Iterator<Entry<FLocation, String>> iter = flocationIds.entrySet().iterator();
        while (iter.hasNext()) {
            Entry<FLocation, String> entry = iter.next();
            if (!Factions.getInstance().isValidFactionId(entry.getValue())) {
                P.p.log("Board cleaner removed " + entry.getValue() + " from " + entry.getKey());
                iter.remove();
            }
        }
    }

    //----------------------------------------------//
    // Coord count
    //----------------------------------------------//

    public int getFactionCoordCount(String factionId) {
        return flocationIds.getOwnedLandCount(factionId);
    }

    public int getFactionCoordCount(Faction faction) {
        return getFactionCoordCount(faction.getId());
    }

    public int getFactionCoordCountInWorld(Faction faction, String worldName) {
        String factionId = faction.getId();
        int ret = 0;
        for (Entry<FLocation, String> entry : flocationIds.entrySet()) {
            if (entry.getValue().equals(factionId) && entry.getKey().getWorldName().equals(worldName)) {
                ret += 1;
            }
        }
        return ret;
    }

    //----------------------------------------------//
    // Map generation
    //----------------------------------------------//

    
    /**
     * The map is relative to a coord and a faction north is in the direction of decreasing x east is in the direction
     * of decreasing z
     */
    public ArrayList<String> getMap(Faction faction, FLocation flocation, double inDegrees) {
        ArrayList<String> ret = new ArrayList<>();
        Faction factionLoc = getFactionAt(flocation);
        ret.add(P.p.txt.titleize("(" + flocation.getCoordString() + ") " + factionLoc.getTag(faction)));

        int halfWidth = 21 / 2;
        int halfHeight = 21 / 2;
        FLocation topLeft = flocation.getRelative(-halfWidth, -halfHeight);
        int width = halfWidth * 2 + 1;
        int height = halfHeight * 2 + 1;

        Map<String, Character> fList = new HashMap<>();

        //Get the compass
        ArrayList<String> asciiCompass = AsciiCompass.getAsciiCompass(inDegrees, ChatColor.RED, P.p.txt.parse("<a>"));

        // For each row
        for (int dz = 0; dz < height; dz++) {
            // Draw and add that row
            StringBuilder row = new StringBuilder();
            for (int dx = 0; dx < width+16; dx++) {
                if (dx == halfWidth && dz == halfHeight) {
                    row.append(ChatColor.AQUA + "█"); //Hráč je zobrazen
                } else {
                	if(dx < 21 && dz < 21) {
	                    FLocation flocationHere = topLeft.getRelative(dx, dz);
	                    Faction factionHere = getFactionAt(flocationHere);
	                    Relation relation = faction.getRelationTo(factionHere);
	                    if (factionHere.isWilderness()) {
	                        row.append(ChatColor.GRAY + "█"); //Wilderness faction
	                    } else if (factionHere.isSafeZone()) {
	                        row.append(ChatColor.GOLD + "█"); //SafeZone
	                    } else if (factionHere.isWarZone()) {
	                        row.append(ChatColor.DARK_RED + "█"); //WarZone
	                    } else if (factionHere.isKoTH()) {
	                    	row.append(ChatColor.YELLOW + "█"); //KoTH
	                    } else if (factionHere == faction ||
	                            factionHere == factionLoc ||
	                            relation.isAtLeast(Relation.ALLY) ||
	                            (Conf.showNeutralFactionsOnMap && relation.equals(Relation.NEUTRAL)) ||
	                            (Conf.showEnemyFactionsOnMap && relation.equals(Relation.ENEMY))) {
	                        row.append(factionHere.getColorTo(faction)).append("").append("█");
	                    } else {
	                        row.append(ChatColor.GRAY + "█");
	                    }
                	} else {
                	}
                }
            }

            row.append(getMapLine(dz, asciiCompass));

            ret.add(row.toString());
        }

        // Add the faction key
        if (Conf.showMapFactionKey) {
            StringBuilder fRow = new StringBuilder();
            for (String key : fList.keySet()) {
                fRow.append(String.format("%s%s: %s ", ChatColor.GRAY, fList.get(key), key));
            }
            ret.add(fRow.toString());
        }

        return ret;
    }

    private String getMapLine(int dz, ArrayList<String> compass) {

        StringBuilder sb = new StringBuilder();
        sb.append("          ");

        switch(dz) {
            case 4:
                return "               "+ compass.get(0);
            case 5:
                return "               "+ compass.get(1);
            case 6:
                return "               "+ compass.get(2);
            case 8:
                sb.append(ChatColor.AQUA + "█ You"); break;
            case 9:
                sb.append(ChatColor.RED + "█ WarZone"); break;
            case 10:
                sb.append(ChatColor.GOLD + "█ SafeZone"); break;
            case 11:
                sb.append(ChatColor.GRAY + "█ Wilderness"); break;
            case 12:
                sb.append( ChatColor.YELLOW + "█ KoTH"); break;
            case 13:
                sb.append(ChatColor.LIGHT_PURPLE + "█ Ally"); break;
            case 14:
                sb.append(ChatColor.DARK_PURPLE + "█ Truce"); break;
            case 15:
                sb.append(ChatColor.WHITE + "█ Neutral"); break;
            case 16:
                sb.append(ChatColor.RED + "█ Enemy"); break;
        }

        return sb.toString();
    }
    public abstract void convertFrom(MemoryBoard old);
}
