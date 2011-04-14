package com.bukkit.vicwhiten.moblimiter;

import java.util.ArrayList;
import java.util.List;

//import org.anjocaido.groupmanager.GroupManager;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Creature;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerListener;
import org.bukkit.event.entity.EntityListener;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.util.config.Configuration;
import org.bukkit.entity.Squid;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.Animals;
import org.bukkit.entity.CreatureType;
import org.bukkit.Location;
import java.util.Random;
import java.util.Queue;
import java.util.LinkedList;

//import com.nijikokun.bukkit.Permissions.Permissions;



public class MobLimiter extends JavaPlugin
{

	private final MobLimiterEntityListener entityListener = new MobLimiterEntityListener(this);
	public MobLimiterPlayerListener pls = new MobLimiterPlayerListener();
	public int mobMax;
	public int animalCreep;
	public int mobCreep;
	public Configuration config;
//	public GroupManager gm;
//	public Permissions perm;
	Random rand = new Random();

	public void onDisable()
	{
		PluginDescriptionFile pdfFile = this.getDescription();
        System.out.println(pdfFile.getName()+" version "+pdfFile.getVersion()+" is disabled!");
	}

	public void onEnable()
	{

		PluginManager pm = getServer().getPluginManager();
		
/*		Plugin p = this.getServer().getPluginManager().getPlugin("GroupManager");
		if (p != null) {
			if (!this.getServer().getPluginManager().isPluginEnabled(p)) {
				this.getServer().getPluginManager().enablePlugin(p);
			}
			gm = (GroupManager) p;
		} 
		
		p = this.getServer().getPluginManager().getPlugin("Permissions");
		if (p != null) {
			if (!this.getServer().getPluginManager().isPluginEnabled(p)) {
				this.getServer().getPluginManager().enablePlugin(p);
			}
			perm = (Permissions) p;
		} */
 
		config = this.getConfiguration();
		setupMobMax();
		pm.registerEvent(Event.Type.CREATURE_SPAWN, this.entityListener, Event.Priority.Normal, this);
		pm.registerEvent(Event.Type.PLAYER_MOVE, this.pls, Event.Priority.Normal, this);
		getCommand("moblimiter").setExecutor(new MobLimiterCommand(this));
		
        PluginDescriptionFile pdfFile = this.getDescription();
        System.out.println(pdfFile.getName()+" version "+pdfFile.getVersion()+" is enabled!");
	}
	
	public int getMobAmount(World world)
	{
		int sum=0;
		List<LivingEntity> mobs = world.getLivingEntities();
		for(int j=0; j<mobs.size(); j++)
		{
			if(mobs.get(j) instanceof Squid)
				sum++;
		}
		return sum;
	}
	
	public void purgeMobs(World world)
	{
		List<LivingEntity> mobs = world.getLivingEntities();
		for(int j=0; j<mobs.size(); j++)
		{
			if(Creature.class.isInstance(mobs.get(j)))
			{
				LivingEntity mob = mobs.remove(j);
				if(mob instanceof Squid) {
					mob.remove();
					j--;
				}
			}
		}
	}
	
	public void setupMobMax()
	{
		config.load();

		mobMax = config.getInt("mob-max", -1);
		config.setProperty("mob-max", mobMax);

		animalCreep = config.getInt("animal-creep", 10);
		config.setProperty("animal-creep", animalCreep);

		mobCreep = config.getInt("mob-creep", 90);
		config.setProperty("mob-creep", mobCreep);

		config.save();
	}
	
	public void setMobMax(int newMax)
	{
		mobMax = newMax;
		config.setProperty("mob-max", newMax);
		config.save();
	}
	
	public int getMobMax()
	{
		return mobMax;
	}
	
    public boolean checkPermission(Player player, String permission)
    {
    	if(player.isOp())
    	{
    		return true;
    	}else return false;
    }

	private class MobLimiterPlayerListener extends PlayerListener {

		public Queue<Location> spawnq;

		public MobLimiterPlayerListener() {
			spawnq = new LinkedList<Location>();
		}

		public void onPlayerMove(PlayerMoveEvent event) {
			int n=0;
			while(!spawnq.isEmpty()) {
				Location loc = spawnq.remove();
				loc.getWorld().spawnCreature(loc, CreatureType.CREEPER);
				n++;
			}
			if(n>0)
				System.out.println(n+" creeped");
		}
	}
	
	private class MobLimiterEntityListener extends EntityListener {

		private MobLimiter plugin;

		public MobLimiterEntityListener(MobLimiter plug) {
			plugin = plug;
		}
		
		public void onCreatureSpawn(CreatureSpawnEvent event)
		{	
			if(event.getEntity() instanceof Squid) {
				if(plugin.mobMax > -1 && plugin.getMobAmount(event.getEntity().getWorld()) >= plugin.mobMax)
				{
					event.setCancelled(true);
				}
			} else if(event.getEntity().getWorld().getName().equals("desert")) {
				if(event.getEntity() instanceof Creeper) return;
				boolean creepo = false;
				if(event.getEntity() instanceof Animals)
					if(rand.nextInt(100) < animalCreep) creepo=true;
				else
					if(rand.nextInt(100) < mobCreep) creepo=true;

				if(creepo) {
//					event.getEntity().getWorld().spawnCreature(event.getEntity().getLocation(), CreatureType.CREEPER);
					plugin.pls.spawnq.add(event.getEntity().getLocation().clone());
					event.setCancelled(true);
				}	
			}
		}

	}
	

	
	private class MobLimiterCommand implements CommandExecutor 
	{
	    private final MobLimiter plugin;

	    public MobLimiterCommand(MobLimiter plugin) {
	        this.plugin = plugin;
	    }

	    public boolean onCommand(CommandSender sender, 
	    		Command command, 
	    		String label, String[] args) 
	    {
	    	boolean permission = false;
	    	try{
	    		permission = checkPermission((Player)sender, "moblimiter.sexMax");
	    	}catch(Exception E)
	    	{
	    		permission = true;
	    	}
	    	if(!permission)
	    	{
	    		sender.sendMessage(ChatColor.RED + "You do not have the permissions to do this");
	    		return true;
	    	}
	    	if(args.length < 1)
	    	{
	    		return false;
	    	}
	    	//setMax command
	    	if(args.length == 2 && args[0].compareTo("setmax") == 0)
	    	{
	    		try{
	    			int newMax = Integer.parseInt(args[1]);
	    			if(newMax >=-1)
	    			{
	    				plugin.setMobMax(newMax);
	    				sender.sendMessage("MobMax set to " + newMax);
	    				return true;
	    			}else return false;
	    		}catch(Exception e){
	    			return false;
	    		}
	    	}
	    	//purge command
	    	if(args.length == 1 && args[0].compareTo("purge") == 0)
	    	{
	    		try{
	    		Player p = (Player)sender;
		    	plugin.purgeMobs(p.getWorld());
		    	sender.sendMessage("All mobs purged.");
		    	return true;
		    	}catch(Exception E){
		    		sender.sendMessage("Must be run ingame!");
		    		return true;
		    	}
	    	}
	    	//max command
	    	if(args.length == 1 && args[0].compareTo("max") == 0)
	    	{
	    		sender.sendMessage("Mob Max: " + mobMax);
	    		return true;
	    	}
	    	return false;
	    }	
	}
	
}
