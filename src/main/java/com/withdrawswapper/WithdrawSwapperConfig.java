package com.withdrawswapper;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("withdrawswapper")
public interface WithdrawSwapperConfig extends Config
{
	@ConfigItem(
		keyName = "itemList",
		name = "Items to change left click withdraw",
		description = "Format with the name of the item and the withdraw type separated by a ':'. Etc Knife:allbut1"
	)
	default String itemList()
	{
		return "Jug of wine:5\nKnife:10\nTokkul:allbut1\nFire rune:x";
	}
}
