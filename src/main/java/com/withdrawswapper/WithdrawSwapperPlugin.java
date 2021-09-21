/*
 * Copyright (c) 2021, Sholto B. <bolotofski@gmail.com>
 * All rights reserved.
 *
 * Credits to Adam <Adam@sigterm.info> for elements of code from Menu Entry Swapper
 * https://github.com/runelite/runelite/blob/master/runelite-client/src/main/java/net/runelite/client/plugins/menuentryswapper/
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.withdrawswapper;

import com.google.inject.Provides;

import javax.inject.Inject;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.MenuAction;
import net.runelite.api.MenuEntry;
import net.runelite.api.events.MenuEntryAdded;
import net.runelite.api.widgets.WidgetID;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.menuentryswapper.ShiftWithdrawMode;
import net.runelite.client.util.Text;

import java.util.HashMap;

@Slf4j
@PluginDescriptor(
        name = "Bank Withdraw Swapper"
)
public class WithdrawSwapperPlugin extends Plugin {
    @Inject
    private Client client;

    @Inject
    private WithdrawSwapperConfig config;

    // Stores the names of items which we want to switch the left click withdraw of and the withdraw amount.
    private HashMap<String, ShiftWithdrawMode> items;

    @Override
    protected void startUp() {
        decodeItemString(config.itemList());
    }

    @Override
    protected void shutDown() {
        items.clear();
    }

    @Provides
    WithdrawSwapperConfig provideConfig(ConfigManager configManager) {
        return configManager.getConfig(WithdrawSwapperConfig.class);
    }

    @Subscribe
    public void onMenuEntryAdded(MenuEntryAdded menuEntryAdded)
    {
        // This swap needs to happen prior to drag start on click, which happens during
        // widget ticking and prior to our client tick event. This is because drag start
        // is what builds the context menu row which is what the eventual click will use
        String targetName = Text.removeTags(menuEntryAdded.getTarget()).toLowerCase();
        if (menuEntryAdded.getType() == MenuAction.CC_OP.getId() && menuEntryAdded.getIdentifier() == 1
                && menuEntryAdded.getOption().startsWith("Withdraw") && items.containsKey(targetName))
        {
            // Get the withdraw amount for the given item.
            ShiftWithdrawMode shiftWithdrawMode = items.get(targetName);
            final int widgetGroupId = WidgetInfo.TO_GROUP(menuEntryAdded.getActionParam1());
            final int actionId, opId;
            if (widgetGroupId == WidgetID.CHAMBERS_OF_XERIC_STORAGE_UNIT_PRIVATE_GROUP_ID || widgetGroupId == WidgetID.CHAMBERS_OF_XERIC_STORAGE_UNIT_SHARED_GROUP_ID)
            {
                actionId = MenuAction.CC_OP.getId();
                opId = shiftWithdrawMode.getIdentifierChambersStorageUnit();
            }
            else
            {
                actionId = shiftWithdrawMode.getMenuAction().getId();
                opId = shiftWithdrawMode.getIdentifier();
            }
            bankModeSwap(actionId, opId);
        }
    }

    private void bankModeSwap(int entryTypeId, int entryIdentifier)
    {
        MenuEntry[] menuEntries = client.getMenuEntries();
        for (int i = menuEntries.length - 1; i >= 0; --i)
        {
            MenuEntry entry = menuEntries[i];
            if (entry.getType() == entryTypeId && entry.getIdentifier() == entryIdentifier)
            {
                // Raise the priority of the op so it doesn't get sorted later
                entry.setType(MenuAction.CC_OP.getId());

                menuEntries[i] = menuEntries[menuEntries.length - 1];
                menuEntries[menuEntries.length - 1] = entry;

                client.setMenuEntries(menuEntries);
                break;
            }
        }
    }

    private void decodeItemString(String itemString){
        items = new HashMap<String, ShiftWithdrawMode>();
        try{
            // Split newline characters, should only be \n ideally.
            String[] itemPairs = itemString.split("\\r?\\n|\\r");
            for (String item : itemPairs){
                if (item.isEmpty()) continue;
                String[] pair = item.split(":");
                if (pair.length != 2) continue;
                String itemName = pair[0].trim().toLowerCase();
                String withdrawAmount = pair[1].trim().toLowerCase();
                ShiftWithdrawMode withdrawMode;
                switch (withdrawAmount) {
                    case "1":  withdrawMode = ShiftWithdrawMode.WITHDRAW_1;
                        break;
                    case "5":  withdrawMode = ShiftWithdrawMode.WITHDRAW_5;
                        break;
                    case "10":  withdrawMode = ShiftWithdrawMode.WITHDRAW_10;
                        break;
                    case "x":  withdrawMode = ShiftWithdrawMode.WITHDRAW_X;
                        break;
                    case "all":  withdrawMode = ShiftWithdrawMode.WITHDRAW_ALL;
                        break;
                    case "allbut1":  withdrawMode = ShiftWithdrawMode.WITHDRAW_ALL_BUT_1;
                        break;
                    default: withdrawMode = null;
                        break;
                }
                if (withdrawMode == null) continue;
                items.put(itemName, withdrawMode);
            }
        }
        catch(Exception ignore){

        }
    }

    @Subscribe
    private void onConfigChanged(ConfigChanged event) {
        if (event.getGroup().equals("withdrawswapper")) {
            decodeItemString(config.itemList());
        }
    }
}
