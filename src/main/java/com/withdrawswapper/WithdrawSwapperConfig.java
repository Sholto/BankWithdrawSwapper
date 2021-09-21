package com.withdrawswapper;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("withdrawswapper")
public interface WithdrawSwapperConfig extends Config
{
	@ConfigItem(
		keyName = "itemList",
		name = "List of items to change the left click withdraw of.",
		description = "List of items you want to change the left click withdraw of."
	)
	default String itemList()
	{
		return "";
	}
}
