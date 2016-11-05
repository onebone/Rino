# Rino
NPC plugin for Nukkit

### Build
- git clone https://github.com/onebone/Rino
- mvn clean
- mvn package

### Commands
- /rino create <name>
- /rino remove <entity id>
- /rino list [page]
- /rino option [-m <message>] [-i <item>] [-n <name>] [-c]
	- `-m` `--message` Set message
	- `-i` `--item` Set item
	- `-n` `--name` Set name
	- `-c` `--clear` Clear all requests
	- ie. /rino option `-m` "This is a message" `-i` Dirt `--name` "My name"

### Permissions
- rino
	- rino.command
		- rino.command.rino
			- rino.command.rino.create
			- rino.command.rino.remove
			- rino.command.rino.list
			- rino.command.rino.message
