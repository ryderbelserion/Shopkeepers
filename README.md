<p align="center">
  <img src="https://github.com/Shopkeepers/Shopkeepers-Wiki/wiki/images/logos/shopkeepers_logo_small_with_text.png?raw=true" alt="Shopkeepers logo"/>
</p>

Shopkeepers [![Build Status](https://github.com/Shopkeepers/Shopkeepers/actions/workflows/build.yml/badge.svg?branch=master)](https://github.com/Shopkeepers/Shopkeepers/actions/workflows/build.yml)
===========

Shopkeepers is a Bukkit/[Spigot](https://www.spigotmc.org/wiki/spigot/) plugin that allows you to set up custom villager shopkeepers that sell exactly what you want them to sell and for what price. 
You can set up admin shops, which have infinite supply, and you can also set up player shops, which pull supply from a container.

**BukkitDev Page**: https://dev.bukkit.org/projects/shopkeepers  
**Wiki**: https://github.com/Shopkeepers/Shopkeepers-Wiki/wiki  
**Translations** : https://github.com/Shopkeepers/Translations/  
**Issue Tracker**: https://github.com/Shopkeepers/Shopkeepers/issues  
**Discord Server**: https://discord.gg/d9NKd5z  
**Source code**: https://github.com/Shopkeepers/Shopkeepers/  

Maven repository for releases: https://nexus.lichtspiele.org/repository/releases/  
Maven repository for dev builds (snapshots): https://nexus.lichtspiele.org/repository/snapshots/  

If the above Maven repository is currently not available, you can also use Jitpack: https://jitpack.io/#Shopkeepers/Shopkeepers/  
Maven Jitpack snapshots: https://jitpack.io/#Shopkeepers/Shopkeepers/master-SNAPSHOT  

Cloning and Building
----------------

This section assumes that you have [Git](https://git-scm.com/) installed.

We use Maven to compile and build the plugin. This repository comes with bash scripts to automatically install the required version of Maven, build the required [Spigot](https://www.spigotmc.org/wiki/spigot/) dependencies, and then use Maven to build Shopkeepers and produce a plugin and an API jar. To build Shopkeepers, just execute the following commands from within a bash console. If you are on Windows, you can install [Git-for-Windows](https://gitforwindows.org/) and then execute these commands from within the "Git Bash".

```
git clone https://github.com/Shopkeepers/Shopkeepers.git
cd Shopkeepers
./installSpigotDependencies.sh
./build.sh
```

If everything went well, the `target` folder will contain a plugin jar that you can install on your server, as well as an API jar that can be used to by other plugin developers to develop addons.

Pull Requests & Contributing
----------

To import the project into your favorite Java IDE, refer to your IDE's respective documentation on how to import Maven projects. For example, in Eclipse you can find this under **Import > Maven > Existing Maven Projects**. Select the root Shopkeepers folder and import all the Maven projects found by Eclipse.

The `root` project contains several module projects. The most important ones are:
* `main`: This contains the core plugin code.
* `api`: This contains all API code.
* And several modules for the NMS / CraftBukkit version specific code of the supported server versions.

Shopkeepers requires serveral Spigot and CraftBukkit dependencies. The easiest way to automatically build and install these dependencies into your local Maven repository is to run the included `./installSpigotDependencies.sh` script.

To then build the project from within your IDE, refer to your IDE's respective documentation on how to build Maven projects. For Eclipse, right click the root project and select **Run As > Maven install**. We require Java 16 to build.

For more information on creating pull requests and contributing code to the project see [Contributing](CONTRIBUTING.md).
