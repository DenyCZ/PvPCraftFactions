package com.massivecraft.factions.cmd;

import com.massivecraft.factions.struct.Permission;
import com.massivecraft.factions.zcore.util.TL;

public class CmdFly extends FCommand {

	public CmdFly() {
        super();
        this.aliases.add("fly");

        this.permission = Permission.FLY.node;
        this.disableOnLock = true;

        senderMustBePlayer = true;
        senderMustBeMember = true;
        senderMustBeModerator = false;
        senderMustBeAdmin = false;
	
	}

	@Override
	public void perform() {
		if(fme.isInOwnTerritory()) {
			if(!fme.isFlying()) {
				fme.getPlayer().setAllowFlight(true);
				fme.getPlayer().setFlying(true);
				fme.getPlayer().sendMessage(TL.COMMAND_FLY_ENABLED.toString());
			} else {
				fme.getPlayer().setAllowFlight(false);
				fme.getPlayer().setFlying(false);
				fme.getPlayer().sendMessage(TL.COMMAND_FLY_DISABLED.toString());
			}
		} else {
			fme.getPlayer().sendMessage(TL.COMMAND_FLY_ERROR_TERRITORY.toString());
		}
	}
	
	
	@Override
	public TL getUsageTranslation() {
		return TL.COMMAND_FLY_DESCRIPTION;
	}

}
