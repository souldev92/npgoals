package me.souldev.npgoals;


import org.bukkit.event.Listener;

import org.bukkit.plugin.java.JavaPlugin;

import java.sql.*;


public final class Npgoals extends JavaPlugin implements Listener {



    private Connection connection;
    private String host,database,username,password;
    private int port;
    @Override
    public void onEnable() {
        // Plugin startup logic
        getLogger().info("NPGoals starting");
        loadConfig();
        mySqlSetup();
        this.getServer().getPluginManager().registerEvents(new MySqlCon(),this);
        new MySqlCon().checkQuestDb();




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
        getLogger().info("NPGoals stopping");
    }


    public Connection getConnection(){
        return connection;
    }
    public void setConnection(Connection connection){
        this.connection = connection;
    }


}



