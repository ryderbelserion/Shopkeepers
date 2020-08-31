<p align="center">
  <img src="https://github.com/Shopkeepers/Shopkeepers-Wiki/wiki/images/logos/shopkeepers_logo_small_with_text.png?raw=true" alt="Shopkeepers logo"/>
</p>

Shopkeepers [![Build Status](https://travis-ci.com/Shopkeepers/Shopkeepers.svg?branch=master)](https://travis-ci.com/Shopkeepers/Shopkeepers)
===========

Shopkeepers is a Bukkit plugin which allows you to set up custom villager shopkeepers that sell exactly what you want them to sell and for what price. 
You can set up admin shops, which have infinite supply, and you can also set up player shops, which pull supply from a container.

**BukkitDev Page**: https://dev.bukkit.org/projects/shopkeepers  
**Wiki**: https://github.com/Shopkeepers/Shopkeepers-Wiki/wiki  
**Translations** : https://github.com/Shopkeepers/Translations/  
**Issue Tracker**: https://github.com/Shopkeepers/Shopkeepers/issues  
**Discord Server**: https://discord.gg/d9NKd5z  
**Source code**: https://github.com/Shopkeepers/Shopkeepers/  

Maven repository for releases: https://nexus.lichtspiele.org/repository/releases/  
Maven repository for dev builds (snapshots): https://nexus.lichtspiele.org/repository/snapshots/  

Pull Requests & Contributing
----------

See [Contributing](CONTRIBUTING.md)

Build with Maven
----------------

This is the recommended and easy way to compile the plugin yourself and/or help to contribute to it.  
Just check out the project to your machine and import it in Eclipse with **Import > Maven > Existing Maven Project**.  
Then make sure that you have all Spigot and CraftBukkit dependencies installed in your local Maven repository. The simplest way to do this is to run the included **./installSpigotDependencies.sh** script.  
Afterwards just right click the imported project and select **Run As > Maven install**.

Build without Maven
-------------------

If you really want to do it the old school way, you're free to import the project in Eclipse with **Import > General > Existing Project into Workspace**. You'll find that the project will instantly show some errors because it's missing its dependencies. You also need to make sure that you'll include the provided modules (NMSHandlers).

**Here's how you do that:**
* After importing the project right click on it and select **Properties**
* Under **Java Build Path > Source** click on **Add Folder...** and add all provided modules:
  * modules/v1_6_R3/src/main/java
  * modules/v1_7_R1/src/main/java
  * ...
* Under **Java Build Path > Library** click on **Add External JARs...** and add the needed Spigot.jar files for the modules above. Information on how to get them: https://www.spigotmc.org/wiki/spigot/
* After that you can create the plugin for example by right-clicking the project and selecting **Export > Java > JAR file** or another recommended way.

