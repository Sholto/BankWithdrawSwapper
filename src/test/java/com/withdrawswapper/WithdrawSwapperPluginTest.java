package com.withdrawswapper;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class WithdrawSwapperPluginTest
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(WithdrawSwapperPlugin.class);
		RuneLite.main(args);
	}
}