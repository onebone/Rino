package me.onebone.rino;

/*
 * Rino: NPC plugin for Nukkit
 * Copyright (C) 2016  onebone <jyc00410@gmail.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;

import cn.nukkit.Player;
import cn.nukkit.command.Command;
import cn.nukkit.command.CommandSender;
import cn.nukkit.event.EventHandler;
import cn.nukkit.event.Listener;
import cn.nukkit.lang.TranslationContainer;
import cn.nukkit.event.player.PlayerJoinEvent;
import cn.nukkit.event.player.PlayerMoveEvent;
import cn.nukkit.event.player.PlayerTeleportEvent;
import cn.nukkit.event.server.DataPacketReceiveEvent;
import cn.nukkit.level.Level;
import cn.nukkit.level.Position;
import cn.nukkit.network.protocol.InteractPacket;
import cn.nukkit.plugin.PluginBase;
import cn.nukkit.utils.TextFormat;
import cn.nukkit.utils.Utils;

public class Main extends PluginBase implements Listener{
	private Map<String, List<Rino>> rinos;
	private Map<String, String> messages;
	
	@Override
	public void onEnable(){
		this.getServer().getPluginManager().registerEvents(this, this);
		
		this.saveDefaultConfig();
		
		File folder = new File(this.getDataFolder(), "skins");
		if(!folder.exists()){
			folder.mkdir();
		}
		
		File file = new File(this.getDataFolder(), "rino.json");
		
		rinos = new HashMap<>();
		messages = new HashMap<>();
		try{
			int count = 0;
			
			Object[][] data = new GsonBuilder().create().fromJson(Utils.readFile(file), new TypeToken<Object[][]>(){}.getType());
			for(Object[] obj : data){
				Rino rino = Rino.fromObject(this, obj);
				this.createRino(rino);
				
				count++;
			}
			
			this.getLogger().debug(count + " of Rino was installed.");
		}catch(JsonSyntaxException e){
			e.printStackTrace();
		}catch(FileNotFoundException e){
		}catch(IOException e){
			e.printStackTrace();
		}
	}
	
	@Override
	public void onDisable(){
		Gson gson = new Gson();
		
		List<Object[]> obj = new LinkedList<Object[]>();
		
		File skinFolder = new File(this.getDataFolder(), "skins");
		for(File f : skinFolder.listFiles()){
			if(f.getName().endsWith(".skin")){
				f.delete();
			}
		}
		
		int id = 0;
		for(List<Rino> list : rinos.values()){
			for(Rino rino : list){
				File skin = new File(skinFolder, "skin_" + (id++) + ".skin");
				try{
					FileOutputStream fos = new FileOutputStream(skin);
					fos.write(rino.getSkin().getData());
					fos.close();
					
					obj.add(rino.getData(skin));
				}catch(IOException e){
					e.printStackTrace();
				}
			}
		}
		
		try{
			Utils.writeFile(new File(this.getDataFolder(), "rino.json"), gson.toJson(obj));
		}catch(IOException e){
			e.printStackTrace();
		}
	}
	
	public void createRino(Rino rino){
		if(!rinos.containsKey(rino.getLevelName())){
			rinos.put(rino.getLevelName(), new ArrayList<Rino>());
		}
		
		rinos.get(rino.getLevelName()).add(rino);
	}
	
	public void removeRino(long eid){
		for(List<Rino> list : this.rinos.values()){
			for(int i = 0; i < list.size(); i++){
				Rino rino = list.get(i);
				if(rino.getEntityId() == eid){
					list.remove(i);
				}
			}
		}
	}
	
	public Rino getRinoById(long eid){
		for(List<Rino> list : this.rinos.values()){
			for(Rino rino : list){
				if(rino.getEntityId() == eid){
					return rino;
				}
			}
		}
		
		return null;
	}
	
	public List<Rino> getRino(String level){
		if(rinos.containsKey(level)){
			return rinos.get(level);
		}
		
		return new ArrayList<Rino>();
	}
	
	public List<Rino> getRino(Level level){
		return this.getRino(level.getFolderName());
	}
	
	public int countRino(){
		int count = 0;
		for(List<Rino> list : this.rinos.values()){
			count += list.size();
		}
		
		return count;
	}
	
	public List<Rino> getAllRino(){
		List<Rino> ret = new LinkedList<Rino>();
		
		for(List<Rino> list : this.rinos.values()){
			list.forEach(ret::add);
		}
		
		return ret;
	}
	
	@EventHandler
	public void onPlayerJoin(PlayerJoinEvent event){
		Player player = event.getPlayer();
		
		this.getRino(player.getLevel()).forEach((rino) -> rino.spawnTo(player));
	}
	
	@EventHandler
	public void onPlayerTeleport(PlayerTeleportEvent event){
		Level from = event.getFrom().getLevel();
		Level to = event.getTo().getLevel();
		
		if(from != to){
			Player player = event.getPlayer();
			
			this.getRino(from).forEach((rino) -> rino.despawnFrom(player));
			this.getRino(to).forEach((rino) -> rino.spawnTo(player));
		}
	}
	
	@EventHandler
	public void onPlayerMove(PlayerMoveEvent event){
		Player player = event.getPlayer();
		
		this.getRino(player.level).forEach(rino -> rino.seePlayer(player));
	}
	
	@EventHandler
	public void onDataPacketReceive(DataPacketReceiveEvent event){
		if(event.getPacket() instanceof InteractPacket){
			Player player = event.getPlayer();
			InteractPacket pk = (InteractPacket) event.getPacket();
			
			Rino rino;
			if((rino = this.getRinoById(pk.target)) != null){
				if(messages.containsKey(player.getName())){
					rino.setMessage(messages.get(player.getName()));
					messages.remove(player.getName());
					
					player.sendMessage(TextFormat.GREEN + "Message was set.");
				}else{
					String message = rino.getMessage();
					if(message != null && !message.equals("")){
						if(message.startsWith("/")){
							this.getServer().dispatchCommand(player, message.substring(1)
									.replaceAll("%player%", player.getName())); // TODO: More configuration
						}else{
							player.sendMessage(message);
						}
					}
				}
			}
		}
	}
	
	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args){
		if(command.getName().equals("rino")){
			if(args.length < 1){
				sender.sendMessage(new TranslationContainer("commands.generic.usage", command.getUsage()));
				return true;
			}
			
			args[0] = args[0].toLowerCase();
			if(args[0].equals("create")){
				if(!(sender instanceof Player)){
					sender.sendMessage(new TranslationContainer("commands.generic.ingame"));
					return true;
				}
				
				if(args.length < 2){
					sender.sendMessage(new TranslationContainer("commands.generic.usage", command.getUsage()));
					return true;
				}
				
				String name = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
				Player player = (Player) sender;
				
				Rino rino = new Rino(this, new Position(player.x, player.y, player.z, player.level), player.level.getFolderName(), name, "", player.getInventory().getItemInHand(), player.getSkin());
				this.createRino(rino);
				
				rino.spawnToAll();
			}else if(args[0].equals("remove")){
				if(args.length < 1){
					sender.sendMessage(new TranslationContainer("commands.generic.usage", command.getUsage()));
					return true;
				}
				
				long id;
				try{
					id = Long.parseLong(args[1]);
				}catch(NumberFormatException e){
					sender.sendMessage(new TranslationContainer("commands.generic.usage", command.getUsage()));
					return true;
				}
				
				List<Rino> rino = this.getAllRino();
				for(Rino r : rino){
					if(r.getEntityId() == id){
						r.close();
						
						sender.sendMessage(TextFormat.GREEN + "Removed Rino successfully.");
						this.removeRino(id);
						return true;
					}
				}
				
				sender.sendMessage(TextFormat.RED + "Rino with that entity ID does not exist.");
			}else if(args[0].equals("list")){
				List<Rino> rino = this.getAllRino();
				int count = rino.size();
				
				int page = 1;
				try{
					page = args.length > 1 ? Math.max(1, Math.min(Integer.parseInt(args[1]), count)): 1;
				}catch(NumberFormatException e){}
				
				StringBuilder output = new StringBuilder();
				output.append("Showing Rino list: (" + page + "/" + Integer.toString((count + 6) / 5) + ") \n");

				for(int n = 0; n < count;n++){
					int current = (int)Math.ceil((double)(n + 1) / 5);
					if(page == current){
						Rino r = rino.get(n);
						output.append("#" + TextFormat.AQUA + r.getEntityId() + TextFormat.WHITE + ": " + TextFormat.GREEN + r.getName() + TextFormat.WHITE + "\n");
					}else if(page < current){
						break;
					}
				}
				sender.sendMessage(output.substring(0, output.length() - 1));
			}else if(args[0].equals("message")){
				if(!(sender instanceof Player)){
					sender.sendMessage(new TranslationContainer("commands.generic.ingame"));
					return true;
				}
				
				if(messages.containsKey(sender.getName())){
					messages.remove(sender.getName());
					sender.sendMessage(TextFormat.GREEN + "Cancelled message tagging.");
					return true;
				}
				
				if(args.length < 2){
					sender.sendMessage(new TranslationContainer("commands.generic.usage", command.getUsage()));
					return true;
				}
				
				String message = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
				messages.put(sender.getName(), message);
				sender.sendMessage(TextFormat.GREEN + "Touch Rino which you want to.");
			}else{
				sender.sendMessage(new TranslationContainer("commands.generic.usage", command.getUsage()));
			}
			return true;
		}
		return false;
	}
}
