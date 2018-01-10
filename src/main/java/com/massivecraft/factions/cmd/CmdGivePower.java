package com.massivecraft.factions.cmd;

import com.massivecraft.factions.struct.Permission;
import com.massivecraft.factions.zcore.util.TL;

public class CmdGivePower extends FCommand {
	
	public CmdGivePower() {
        super();
        this.aliases.add("addpow");

        this.permission = Permission.FLY.node;
        this.disableOnLock = true;

        senderMustBePlayer = false;
        senderMustBeMember = false;
        senderMustBeModerator = false;
        senderMustBeAdmin = false;
	
	}

	@Override
	public void perform() {
		fme.alterPower(50);
	}
	
	
	@Override
	public TL getUsageTranslation() {
		return TL.COMMAND_FLY_DESCRIPTION;
	}
}
