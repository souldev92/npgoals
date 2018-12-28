package me.souldev.npgoals;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.scheduler.BukkitRunnable;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;
import java.util.UUID;

public class MySqlCon implements Listener {
    private Npgoals plugin = Npgoals.getPlugin(Npgoals.class);
    private String[] monsters = {"zombie", "skeleton", "spider", "enderman", "creeper", "ghast", "wither_skeleton"};
    private String[] blocks = {"stone", "dirt", "log:0", "log:1", "log:2", "log:3", "log2:0", "log2:1", "netherrack"};
    private String[] smelts = {"iron_ingot", "gold_ingot", "cooked_chicken", "cooked_beef",
            "cooked_porkchop", "cooked_mutton", "cooked_rabbit", "cooked_potato"};
    private String[] potions = {"8197", "8193", "8196", "8200", "8194", "8201"};

    @EventHandler
    private void onJoin(PlayerJoinEvent event) {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, new Runnable() {
            @Override
            public void run() {
                Player player = event.getPlayer();
                createPlayer(player.getUniqueId(), player);
                giveQuests(player.getUniqueId(), player);
            }
        });
// tactical comment for testing
    }

    private void giveQuests(UUID uuid, Player player) {

        try {
            PreparedStatement statement = plugin.getConnection().prepareStatement("SELECT * FROM" +
                    " GOALS WHERE UUID=?");

            statement.setString(1, uuid.toString());
            ResultSet results = statement.executeQuery();
            if (!results.next()) {
                plugin.getLogger().info("Quests not found, gon make some");
                getDailyQuests(player);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }


    private boolean playerExists(UUID uuid) {

        try {
            PreparedStatement statement = plugin.getConnection().prepareStatement("SELECT * " +
                    "FROM PLAYERS WHERE UUID=?");
            statement.setString(1, uuid.toString());

            ResultSet results = statement.executeQuery();
            if (results.next()) {
                plugin.getServer().broadcastMessage("Player found");
                return true;
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        plugin.getServer().broadcastMessage("Player not found");
        return false;
    }

    private void createPlayer(final UUID uuid, Player player) {
        try {
            PreparedStatement statement = plugin.getConnection().prepareStatement("SELECT * " +
                    "FROM PLAYERS WHERE UUID=?");
            statement.setString(1, uuid.toString());
            ResultSet results = statement.executeQuery();
            results.next();
            if (!playerExists((uuid))) {
                PreparedStatement insert = plugin.getConnection().prepareStatement("INSERT INTO players (UUID,PLAYERNAME)" +
                        " VALUES (?,?)");
                insert.setString(1, uuid.toString());
                insert.setString(2, player.getName());
                insert.executeUpdate();

                plugin.getServer().broadcastMessage("Player inserted");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void getDailyQuests(Player p) {
        ArrayList<Quest> questList = new ArrayList<>(3);
        ArrayList<String> goalArray = new ArrayList<>(4);
        goalArray.add("killmob");
        goalArray.add("breakblock");
        goalArray.add("smeltitem");
        goalArray.add("brewpotion");
        Collections.shuffle(goalArray);
        Random random = new Random();
        for (int i = 0; i < 3; i++) {
            switch (goalArray.get(i)) {
                case "killmob":

                    String mob = monsters[random.nextInt(monsters.length)];
                    int mobAmount;
                    switch (mob) {
                        case "zombie":
                        case "skeleton":
                        case "spider":
                        case "creeper":
                            mobAmount = random.nextInt(10) + 10;
                            break;
                        case "enderman":
                        case "ghast":
                        case "wither_skeleton":
                            mobAmount = random.nextInt(5) + 5;
                            break;
                        default:
                            mobAmount = 5;
                            break;
                    }
                    questList.add(new Quest(goalArray.get(i), mob, mobAmount));
                    break;
                case "breakblock":

                    String block = blocks[random.nextInt(blocks.length)];
                    int blockAmount = random.nextInt(30) + 30;
                    questList.add(new Quest(goalArray.get(i), block, blockAmount));
                    break;
                case "smeltitem":
                    String itemToSmelt = smelts[random.nextInt(smelts.length)];
                    int smeltAmount;
                    switch (itemToSmelt) {
                        case "gold_ingot":
                            smeltAmount = (random.nextInt(2) + 1) * 8;
                            break;
                        case "iron_ingot":
                            smeltAmount = (random.nextInt(4) + 1) * 8;
                            break;
                        default:
                            smeltAmount = (random.nextInt(8) + 1) * 8;
                    }
                    questList.add(new Quest(goalArray.get(i), itemToSmelt, smeltAmount));
                    break;
                case "brewpotion":
                    String potion = potions[random.nextInt(potions.length)];
                    int potionAmount = 3;
                    questList.add(new Quest(goalArray.get(i), potion, potionAmount));
                    break;
                default:
                    questList.add(new Quest("breakblock", "stone", 64));

            }
        }

        for (Quest q : questList) {
            try {
                PreparedStatement statement = plugin.getConnection().prepareStatement("INSERT INTO " +
                        "goals (uuid,playername, goal, target, amount, iscompleted) VALUES (?,?,?,?,?,0)");
                statement.setString(1, p.getUniqueId().toString());
                statement.setString(2, p.getName());
                statement.setString(3, q.questType);
                statement.setString(4, q.target);
                statement.setString(5, Integer.toString(q.amount));
                statement.executeUpdate();

            } catch (SQLException e) {
                e.printStackTrace();
            }
        }



    }

    public void checkQuestDb() {
        plugin.getLogger().info("Starting the quest checkup loop!");
        new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    PreparedStatement statement = plugin.getConnection().prepareStatement("SELECT * FROM " +
                            "goalsreset WHERE isreset = 1");
                    ResultSet result = statement.executeQuery();
                    if (result.next()) {
                        plugin.getServer().broadcastMessage("Resetting daily quests!");
                        PreparedStatement statement1 = plugin.getConnection().prepareStatement("UPDATE goalsreset " +
                                "SET isreset = 0 WHERE isreset = 1");
                        statement1.executeUpdate();
                        for (Player online:plugin.getServer().getOnlinePlayers())
                        {
                            giveQuests(online.getUniqueId(),online);
                        }
                        return;
                    }

                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }.runTaskTimer(plugin, 0, 200);

    }
    public void pushPlayerQuests(Player p)
    {

    }
}
class Quest{
    String questType;
    String target;
    int amount;
    Quest(String questType, String target, int amount)
    {
        this.amount = amount;
        this.questType=questType;
        this.target=target;
    }
}
class PlayerQuest{
    UUID uuid;
    String quest1;
    String quest2;
    String quest3;
    String target1;
    String target2;
    String target3;
    int goal1;
    int goal2;
    int goal3;
    int col1;
    int col2;
    int col3;
}