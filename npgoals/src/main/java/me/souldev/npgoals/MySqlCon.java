package me.souldev.npgoals;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BrewingStand;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.inventory.BrewEvent;
import org.bukkit.event.inventory.FurnaceExtractEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.potion.PotionBrewer;
import org.bukkit.scheduler.BukkitRunnable;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

public class MySqlCon implements Listener {
    private Npgoals plugin = Npgoals.getPlugin(Npgoals.class);
    private String[] monsters = {"zombie", "skeleton", "spider", "enderman", "creeper", "ghast", "wither_skeleton"};
    private String[] blocks = {"stone", "dirt", "log:0", "log:1", "log:2", "log:3", "log2:0", "log2:1", "netherrack"};
    private String[] smelts = {"iron_ingot", "gold_ingot", "cooked_chicken", "cooked_beef",
            "cooked_porkchop", "cooked_mutton", "cooked_rabbit", "cooked_potato"};
    private String[] potions = {"8197", "8193", "8196", "8200", "8194", "8201"};
    public ArrayList<PlayerQuest> playerQuests = new ArrayList<>();
    @EventHandler
    private void onJoin(PlayerJoinEvent event) {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, new Runnable() {
            @Override
            public void run() {
                Player player = event.getPlayer();
                createPlayer(player.getUniqueId(), player);
                giveQuests(player.getUniqueId(), player);
                fetchQuests(player);
            }
        });
    }

    @EventHandler
    private void onFurnaceTake(FurnaceExtractEvent event)
    {
        Player player = event.getPlayer();

        Material item = event.getItemType();
        int itemcount = event.getItemAmount();
        plugin.getLogger().info(item.toString());
        for (PlayerQuest pquest:playerQuests)
        {
            if (pquest.uuid.equals(player.getUniqueId()) && pquest.questtype.equals("smeltitem") && pquest.questtarget.equalsIgnoreCase(item.toString()) &&
                    !pquest.isCompleted)
            {
                pquest.collected = pquest.collected + itemcount;
                if (pquest.collected >= pquest.tocollect)
                {
                    pquest.isCompleted = true;
                    player.sendRawMessage(ChatColor.BLUE + "Congrats you finished quest");
                    try {
                        PreparedStatement statement = plugin.getConnection().prepareStatement("UPDATE goals SET collected = ?, iscompleted = 1 where uuid = ? and goal = ? and " +
                                "target = ?");
                        statement.setInt(1,pquest.collected);
                        statement.setString(2,player.getUniqueId().toString());
                        statement.setString(3,pquest.questtype);
                        statement.setString(4,pquest.questtarget);
                        statement.executeUpdate();
                        PreparedStatement statement1 = plugin.getConnection().prepareStatement("UPDATE players SET dailycompleted = dailycompleted + 1 where uuid = ?");
                        statement1.setString(1,player.getUniqueId().toString());
                        statement1.executeUpdate();
                    }catch (SQLException e)
                    {
                        e.printStackTrace();
                    }


                }

            }
        }




    }
    @EventHandler
    private  void onLeave(PlayerQuitEvent event)
    {
        UUID uuid = event.getPlayer().getUniqueId();
        Iterator itr = playerQuests.iterator();

        while (itr.hasNext())
        {
            PlayerQuest pquest = (PlayerQuest)itr.next();
            if (pquest.uuid.equals(uuid))
            {
                try{
                    PreparedStatement statement = plugin.getConnection().prepareStatement("UPDATE goals SET collected = ? where uuid = ? and goal = ? and " +
                            "target = ?");
                    statement.setInt(1,pquest.collected);
                    statement.setString(2,uuid.toString());
                    statement.setString(3,pquest.questtype);
                    statement.setString(4,pquest.questtarget);
                    statement.executeUpdate();

                }catch (SQLException e)
                {
                    e.printStackTrace();
                }
                itr.remove();
            }
        }
    }

    private void fetchQuests(Player player)
    {
        try {
            PreparedStatement statement = plugin.getConnection().prepareStatement("SELECT uuid,goal,target,collected,amount,iscompleted " +
                "FROM goals WHERE UUID=?");
            statement.setString(1,player.getUniqueId().toString());
            ResultSet results = statement.executeQuery();
            while(results.next())
            {
                playerQuests.add(new PlayerQuest(UUID.fromString(results.getString(1)),results.getString(2),results.getString(3),results.getInt(4),
                        results.getInt(5),results.getBoolean(6)));
            }

        }catch (SQLException e)
        {
            e.printStackTrace();
        }
    }
    @EventHandler
    private void onBreakBlock(BlockBreakEvent event)
    {
        Player player = event.getPlayer();
        Block block = event.getBlock();
        
        for (PlayerQuest pquest:playerQuests)
        {
            if (pquest.uuid.equals(player.getUniqueId()) && pquest.questtype.equals("breakblock") && pquest.questtarget.equalsIgnoreCase(block.getType().toString()) &&
            !pquest.isCompleted)
            {
                pquest.collected++;
                if (pquest.collected >= pquest.tocollect)
                {
                    pquest.isCompleted = true;
                    player.sendRawMessage(ChatColor.BLUE + "Congrats you finished quest");
                    try {
                        PreparedStatement statement = plugin.getConnection().prepareStatement("UPDATE goals SET collected = ?, iscompleted = 1 where uuid = ? and goal = ? and " +
                                "target = ?");
                        statement.setInt(1,pquest.collected);
                        statement.setString(2,player.getUniqueId().toString());
                        statement.setString(3,pquest.questtype);
                        statement.setString(4,pquest.questtarget);
                        statement.executeUpdate();
                        PreparedStatement statement1 = plugin.getConnection().prepareStatement("UPDATE players SET dailycompleted = dailycompleted + 1 where uuid = ?");
                        statement1.setString(1,player.getUniqueId().toString());
                        statement1.executeUpdate();
                    }catch (SQLException e)
                    {
                        e.printStackTrace();
                    }


                }

            }
        }
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
    @EventHandler
    private void onFish(PlayerFishEvent fishEvent)
    {
        if(fishEvent.getCaught() != null)
            plugin.getLogger().info(fishEvent.getCaught().getName());


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
                            fetchQuests(online);
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
    public Quest(String questType, String target, int amount)
    {
        this.amount = amount;
        this.questType=questType;
        this.target=target;
    }
}
class PlayerQuest{
    UUID uuid;
    String questtype;
    String questtarget;
    int collected;
    int tocollect;
    boolean isCompleted;
    public PlayerQuest(UUID uuid, String questtype, String questtarget, int collected, int tocollect,boolean isCompleted)
    {
        this.collected=collected;
        this.questtarget=questtarget;
        this.questtype=questtype;
        this.tocollect=tocollect;
        this.uuid=uuid;
        this.isCompleted = isCompleted;
    }
}