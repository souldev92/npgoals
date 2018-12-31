package me.souldev.npgoals;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import java.sql.*;
public final class Npgoals extends JavaPlugin implements Listener {
    private Connection connection;
    private String host,database,username,password;
    private int port;
    MySqlCon sqlCon;
    @Override
    public void onEnable() {
        // Plugin startup logic
        getLogger().info("NPGoals starting");
        loadConfig();
        mySqlSetup();
        sqlCon = new MySqlCon();
        this.getServer().getPluginManager().registerEvents(sqlCon,this);
        sqlCon.checkQuestDb();
    }
    public void loadConfig(){
        getConfig().options().copyDefaults(true);
        saveConfig();
    }
    public void mySqlSetup()
    {
        host = this.getConfig().getString("host");
        port = this.getConfig().getInt("port");
        database = this.getConfig().getString("database");
        username = this.getConfig().getString("username");
        password = this.getConfig().getString("password");
        try {
            synchronized (this) {
                if (getConnection() != null && !getConnection().isClosed()) {
                    return;
                }
                Class.forName("com.mysql.jdbc.Driver");
                setConnection(DriverManager.getConnection("jdbc:mysql://"+this.host+":"+this.port + "/" +
                        this.database, this.username, this.password));

                getLogger().info("Database connected");
            }
        }catch (SQLException e){
            e.printStackTrace();
        }catch (ClassNotFoundException e){
            e.printStackTrace();
        }
    }
    @Override
    public void onDisable() {
        // Plugin shutdown logic
        for(PlayerQuest pquest: sqlCon.playerQuests)
        try{
            PreparedStatement statement = getConnection().prepareStatement("UPDATE goals SET collected = ? where uuid = ? and goal = ? and " +
                    "target = ?");
            statement.setInt(1,pquest.collected);
            statement.setString(2,pquest.uuid.toString());
            statement.setString(3,pquest.questtype);
            statement.setString(4,pquest.questtarget);
            statement.executeUpdate();
        }catch (SQLException e)
        {
            e.printStackTrace();
        }
        getLogger().info("NPGoals stopping");
    }
    public Connection getConnection(){
        return connection;
    }
    public void setConnection(Connection connection){
        this.connection = connection;
    }
}