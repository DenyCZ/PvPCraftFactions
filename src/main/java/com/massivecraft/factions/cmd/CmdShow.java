package com.massivecraft.factions.cmd;

import com.massivecraft.factions.Conf;
import com.massivecraft.factions.Faction;
import com.massivecraft.factions.P;
import com.massivecraft.factions.struct.Permission;
import com.massivecraft.factions.zcore.util.TL;
import com.massivecraft.factions.zcore.util.TagReplacer;
import com.massivecraft.factions.zcore.util.TagUtil;
import mkremins.fanciful.FancyMessage;
import org.bukkit.ChatColor;

import java.util.ArrayList;
import java.util.List;

public class CmdShow extends FCommand {

    List<String> defaults = new ArrayList<>();

    public CmdShow() {
        this.aliases.add("show");
        this.aliases.add("who");

        // add defaults to /f show in case config doesnt have it
        defaults.add(" ");
        defaults.add(ChatColor.BOLD + "" + ChatColor.WHITE + "        Faction " + ChatColor.GREEN + "{faction}");
        defaults.add(" ");
        defaults.add(ChatColor.YELLOW + "" + ChatColor.BOLD + "    *" +ChatColor.WHITE + " Owner » " + ChatColor.YELLOW+ "{leader}");
        defaults.add(ChatColor.YELLOW + "" + ChatColor.BOLD + "    *" + ChatColor.WHITE + " Description »");
        defaults.add(ChatColor.YELLOW + "     {description}");
        defaults.add(ChatColor.YELLOW + "" + ChatColor.BOLD + "    *" + ChatColor.WHITE + " Land / Power / Maxpower " + ChatColor.YELLOW + "»  {chunks} / {power} / {maxPower}");
        defaults.add(ChatColor.YELLOW + "" + ChatColor.BOLD + "    *" + ChatColor.WHITE + " Faction kills " + ChatColor.YELLOW +"» {faction-kills}");
        defaults.add(" ");
        defaults.add(" TODO: Faction Level ");
        defaults.add(" TODO: FACTION EXP");
        defaults.add(" ");
        //TODO: FACTION LEVEL
        //TODO: EXPERIENCE
        defaults.add(ChatColor.YELLOW + "    *" + ChatColor.WHITE + " Allies " + ChatColor.YELLOW + "» ({allies}/{max-allies}): {allies-list}");
        defaults.add(ChatColor.YELLOW + "    *" + ChatColor.WHITE + " Enemies " + ChatColor.YELLOW + "» ({enemies}/{max-enemies}): {enemies-list}");
        defaults.add(ChatColor.YELLOW + "    *" + ChatColor.WHITE + " Online "+ ChatColor.YELLOW + "» ({online}/{members}): {online-list}");
        defaults.add(ChatColor.YELLOW + "    *" + ChatColor.WHITE + " Offline "+ ChatColor.YELLOW + "» ({offline}/{members}): {offline-list}");
        defaults.add(" ");

        this.optionalArgs.put("faction tag", "yours");

        this.permission = Permission.SHOW.node;
        this.disableOnLock = false;

        senderMustBeMember = false;
        senderMustBeModerator = false;
        senderMustBeAdmin = false;
    }

    @Override
    public void perform() {
        Faction faction = myFaction;
        if (this.argIsSet(0)) {
            faction = this.argAsFaction(0);
        }
        if (faction == null) {
            return;
        }

        if (fme != null && !fme.getPlayer().hasPermission("factions.show.bypassexempt")
                && P.p.getConfig().getStringList("show-exempt").contains(faction.getTag())) {
            msg(TL.COMMAND_SHOW_EXEMPT);
            return;
        }

        // if economy is enabled, they're not on the bypass list, and this command has a cost set, make 'em pay
        if (!payForCommand(Conf.econCostShow, TL.COMMAND_SHOW_TOSHOW, TL.COMMAND_SHOW_FORSHOW)) {
            return;
        }

        List<String> show = defaults;

        if (!faction.isNormal()) {
            String tag = faction.getTag(fme);
            // send header and that's all
            String header = show.get(0);
            if (TagReplacer.HEADER.contains(header)) {
                msg(p.txt.titleize(tag));
            } else {
                msg(p.txt.parse(TagReplacer.FACTION.replace(header, tag)));
            }
            return; // we only show header for non-normal factions
        }

        for (String raw : show) {
            String parsed = TagUtil.parsePlain(faction, fme, raw); // use relations
            if (parsed == null) {
                continue; // Due to minimal f show.
            }
            if (TagUtil.hasFancy(parsed)) {
                List<FancyMessage> fancy = TagUtil.parseFancy(faction, fme, parsed);
                if (fancy != null) {
                    sendFancyMessage(fancy);
                }
                continue;
            }
            if (!parsed.contains("{notFrozen}") && !parsed.contains("{notPermanent}")) {
                if (parsed.contains("{ig}")) {
                    // replaces all variables with no home TL
                    parsed = parsed.substring(0, parsed.indexOf("{ig}")) + TL.COMMAND_SHOW_NOHOME.toString();
                }
                if (parsed.contains("%")) {
                    parsed = parsed.replaceAll("%", ""); // Just in case it got in there before we disallowed it.
                }
                msg(p.txt.parse(parsed));
            }
        }
    }

    @Override
    public TL getUsageTranslation() {
        return TL.COMMAND_SHOW_COMMANDDESCRIPTION;
    }

}