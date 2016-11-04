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
import java.io.FileInputStream;
import java.io.IOException;
import java.util.UUID;

import cn.nukkit.Player;
import cn.nukkit.Server;
import cn.nukkit.entity.Entity;
import cn.nukkit.entity.data.EntityMetadata;
import cn.nukkit.entity.data.Skin;
import cn.nukkit.item.Item;
import cn.nukkit.level.Location;
import cn.nukkit.level.Position;
import cn.nukkit.network.protocol.AddPlayerPacket;
import cn.nukkit.network.protocol.MovePlayerPacket;
import cn.nukkit.network.protocol.PlayerListPacket;
import cn.nukkit.network.protocol.RemoveEntityPacket;

public class Rino{
	private Main plugin;
	private Location pos;
	private long eid;
	private String levelName, name, message;
	private UUID uuid;
	private Item item;
	private Skin skin;
	
	public Rino(Main plugin, Position pos, String levelName, String username, String message, Item item, Skin skin){
		this(plugin, new Location(pos.x, pos.y, pos.z, -1 , -1, pos.level), levelName, username, message, item, skin);
	}
	
	public Rino(Main plugin, Location pos, String levelName, String username, String message, Item item, Skin skin){
		this.plugin = plugin;
		
		this.pos = pos;
		this.eid = Entity.entityCount++;
		this.levelName = levelName;
		this.name = username;
		this.message = message;
		this.uuid = UUID.randomUUID();
		this.item = item;
		this.skin = skin;
	}
	
	public void seePlayer(Player player){
		if(!player.level.getFolderName().equals(this.levelName)) return;
		
		MovePlayerPacket pk = new MovePlayerPacket();
		pk.eid = this.eid;
		pk.x = (float) this.pos.x;
		pk.y = (float) this.pos.y + 1.62F;
		pk.z = (float) this.pos.z;
		
		if(this.pos.yaw == -1 && player != null){
			double xdiff = player.x - this.pos.x;
			double zdiff = player.z - this.pos.z;
			double angle = Math.atan2(zdiff, xdiff);
			pk.headYaw = pk.yaw = (float) ((angle * 180) / Math.PI) - 90;
		}else{
			pk.headYaw = pk.yaw = (float) this.pos.yaw;
		}
		
		if(this.pos.pitch == -1 && player != null){
			double ydiff = player.y - this.pos.y;
			double dist = Math.sqrt(Math.pow((player.x - this.pos.x), 2) + Math.pow(player.z - this.pos.z, 2));
			double angle = Math.atan2(dist, ydiff);
			pk.pitch = (float) ((angle * 180) / Math.PI) - 90;
		}else{
			pk.pitch = (float) this.pos.pitch;
		}
		player.dataPacket(pk);
	}
	
	public void spawnTo(Player player){
		if(!player.level.getFolderName().equals(this.levelName)) return;
		
		AddPlayerPacket pk = new AddPlayerPacket();
		pk.uuid = this.uuid;
		pk.username = this.name;
		pk.entityRuntimeId = this.eid;
		pk.entityUniqueId = this.eid;
		pk.x = (float) this.pos.x;
		pk.y = (float) this.pos.y;
		pk.z = (float) this.pos.z;
		if(this.pos.yaw == -1 && player != null){
			double xdiff = player.x - this.pos.x;
			double zdiff = player.z - this.pos.z;
			double angle = Math.atan2(zdiff, xdiff);
			pk.yaw = (float) ((angle * 180) / Math.PI) - 90;
		}else{
			pk.yaw = (float) this.pos.yaw;
		}
		
		if(this.pos.pitch == -1 && player != null){
		}else{
			pk.pitch = (float) this.pos.pitch;
		}
		
		pk.item = this.item;
		
		pk.metadata = new EntityMetadata(){
			{
				this.putLong(Entity.DATA_FLAGS,
					1 << Entity.DATA_FLAG_ALWAYS_SHOW_NAMETAG
					^ 1 << Entity.DATA_FLAG_CAN_SHOW_NAMETAG
				);
				this.putBoolean(Entity.DATA_FLAG_NO_AI, true);
				this.putLong(Entity.DATA_LEAD_HOLDER_EID, -1);
			}
		};
		player.dataPacket(pk);
		
		PlayerListPacket listPk = new PlayerListPacket();
		listPk.type = PlayerListPacket.TYPE_ADD;
		
		String name = this.plugin.getConfig().getString("list-format", "Rino: %name%")
				.replaceAll("%name%", this.name)
				.replaceAll("%message%", this.message);
		listPk.entries = new PlayerListPacket.Entry[]{
			new PlayerListPacket.Entry(this.uuid, this.eid, name, this.skin)
		};
		player.dataPacket(listPk);
	}
	
	public void spawnToAll(){
		this.plugin.getServer().getOnlinePlayers().values().forEach(this::spawnTo);
	}
	
	public void despawnFrom(Player player){
		RemoveEntityPacket pk = new RemoveEntityPacket();
		pk.eid = this.eid;
		player.dataPacket(pk);
		
		PlayerListPacket listPk = new PlayerListPacket();
		listPk.type = PlayerListPacket.TYPE_REMOVE;
		listPk.entries = new PlayerListPacket.Entry[]{
			new PlayerListPacket.Entry(this.uuid)
		};
		player.dataPacket(listPk);
	}
	
	public void despawnFromAll(){
		this.plugin.getServer().getOnlinePlayers().values().forEach(this::despawnFrom);
	}
	
	public String getMessage(){
		return this.message;
	}
	
	public String getName(){
		return this.name;
	}
	
	public String getLevelName(){
		return this.levelName;
	}
	
	public Skin getSkin(){
		return this.skin;
	}
	
	public long getEntityId(){
		return this.eid;
	}
	
	public void setMessage(String message){
		this.message = message;
	}
	
	public Object[] getData(File file){
		return new Object[]{
				this.pos.x, this.pos.y, this.pos.z, this.levelName,
				this.name, this.message, this.item.getId(), this.item.getDamage(), file.getName(), this.skin.getModel()
		};
	}
	
	public void close(){
		this.despawnFromAll();
	}
	
	public static Rino fromObject(Main plugin, Object[] data) throws IOException{
		FileInputStream fis = new FileInputStream(new File(new File(plugin.getDataFolder(), "skins"), (String) data[8]));
		byte[] buffer = new byte[fis.available()];
		fis.read(buffer);
		fis.close();
		
		return new Rino(plugin, new Position(
				getDouble(data[0]), getDouble(data[1]), getDouble(data[2]), Server.getInstance().getLevelByName((String) data[3]) 
		), (String) data[3], (String) data[4], (String) data[5], Item.get(getInteger(data[6]), getInteger(data[7])), new Skin(buffer, (String) data[9]));
	}
	
	public static Double getDouble(Object obj){
		if(obj instanceof Integer){
			return ((Integer) obj).doubleValue();
		}
		return (Double) obj;
	}
	
	public static Integer getInteger(Object obj){
		if(obj instanceof Integer){
			return (Integer) obj;
		}
		return ((Double) obj).intValue();
	}
}
