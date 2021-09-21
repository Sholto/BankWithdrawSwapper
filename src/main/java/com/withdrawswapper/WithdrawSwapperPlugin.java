package com.withdrawswapper;

import com.google.common.collect.ArrayListMultimap;
import com.google.inject.Provides;

import javax.inject.Inject;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.MenuAction;
import net.runelite.api.MenuEntry;
import net.runelite.api.events.ClientTick;
import net.runelite.api.events.MenuOpened;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.util.Text;

import java.util.List;
import java.util.Locale;

@Slf4j
@PluginDescriptor(
        name = "Left-click Withdraw Swapper"
)
public class WithdrawSwapperPlugin extends Plugin {
    @Inject
    private Client client;

    @Inject
    private WithdrawSwapperConfig config;

    // Stores the names of items which we want to switch the left click withdraw of.
    private List<String> itemList;

    private final ArrayListMultimap<String, Integer> optionIndexes = ArrayListMultimap.create();

    public enum WithdrawAmount {
        One("Withdraw-1"),
        Five("Withdraw-5"),
        Ten("Withdraw-10"),
        Hundred("Withdraw-100"),
        X("Withdraw-x"),
        All("Withdraw-all"),
        AllButOne("Withdraw-all-but-1");

        private String value;

        WithdrawAmount(String value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return this.value;
        }

        public String lowerCase(){
            return this.value.toLowerCase();
        }
    }

    @Override
    protected void startUp() {
        itemList = Text.fromCSV(config.itemList().toLowerCase());

        log.info("Example started!");
    }

    @Override
    protected void shutDown() {
        itemList.clear();
        log.info("Example stopped!");
    }

    @Provides
    WithdrawSwapperConfig provideConfig(ConfigManager configManager) {
        return configManager.getConfig(WithdrawSwapperConfig.class);
    }

    //@Subscribe
    public void onMenuOptionClicked(MenuOptionClicked event)
    {
        //log.info("Menu clicked: " + event.toString());

        MenuEntry[] menuEntries = client.getMenuEntries();
        for (MenuEntry entry: menuEntries) {
            log.info("Menu entry: " + entry.toString());
        }
    }

    @Subscribe
    public void onClientTick(ClientTick clientTick) {
        // The menu is not rebuilt when it is open, so don't swap or else it will
        // repeatedly swap entries
        if (client.getGameState() != GameState.LOGGED_IN || client.isMenuOpen()) {
            return;
        }
        MenuEntry[] menuEntries = client.getMenuEntries();
        swapMenuEntry(menuEntries);
    }

    private void swapMenuEntry(MenuEntry[] menuEntries) {
        //try {
            if (itemList == null || menuEntries == null || itemList.size() == 0) {
                return;
            }

            MenuEntry entry = menuEntries[menuEntries.length - 1];
            final String option = Text.removeTags(entry.getOption()).toLowerCase();
            final String target = Text.removeTags(entry.getTarget()).toLowerCase();

            // Checks to see if the menu option is a bank withdraw.
            Boolean isWithdraw = false;
            for (WithdrawAmount w : WithdrawAmount.values()){
                if (option.equals((w.lowerCase())))
                    isWithdraw = true;
            }
            if (!isWithdraw)
                return;

            for (String item : itemList) {
                if (item.equals(target)) {
                    replace(menuEntries, WithdrawAmount.Five, target, true);
                    return;
                }
            }
        //} catch (Exception ignored) {
            // ignored
        //}
    }

    private void replace(MenuEntry[] entries, WithdrawAmount option, String target, boolean strict){
        int index = findIndex(entries, option.lowerCase(), target, strict);
        if (index < 0) return;
        MenuEntry entry = entries[index];
        entry.setType(MenuAction.CC_OP.getId());
        entries[entries.length - 1] = entry;
        //entry2.
        client.setMenuEntries(entries);
    }

    private int findIndex(MenuEntry[] entries, String option, String target, boolean strict) {
        for (int i = 0; i < entries.length; i++) {
            MenuEntry entry = entries[i];
            String entryOption = Text.removeTags(entry.getOption()).toLowerCase();
            String entryTarget = Text.removeTags(entry.getTarget()).toLowerCase();

            if (strict) {
                if (entryOption.equals(option) && entryTarget.equals(target)) {
                    return i;
                }
            } else {
                if (entryOption.contains(option.toLowerCase()) && entryTarget.equals(target)) {
                    return i;
                }
            }
        }
        return -1;
    }

    @Subscribe
    private void onConfigChanged(ConfigChanged event) {
        if (event.getGroup().equals("withdrawswapper")) {
            itemList = Text.fromCSV(config.itemList().toLowerCase());
        }
    }
}
