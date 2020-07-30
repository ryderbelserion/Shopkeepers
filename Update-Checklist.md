# Update checklist

## Minecraft update

* Add a new Maven module for the new Minecraft version:
	* Copy an existing module: Both the package folder, as well as inside the pom (artifactId and name).
	* Update the CraftBukkit dependency inside the pom.
	* Add the module in the root pom.
	* Update the NMSHandler class:
		* Update all NMS version specific references.
		* Check all NMS version specific code:
			* Methods or fields might no longer exist or might have been renamed. 

* Add a new build entry in '.travis.yml'.

* New mobs:
	* Test if they can be used for shopkeepers.
	* Add a note about potential issues in SKLivingShopObjectTypes.
	* If there are no severe issues, add them to the by default enabled living shop types.

* New features for new or existing shop objects (mobs, signs, etc.):
	* Consider adding them to the editor menu of that shop object.
		* Add new editor buttons and messages.

* New blocks or items:
	* Check the ItemUtils if there are any material lists that need to be updated.
		* Containers, supported containers, rails.

* If there are major differences, consider dropping support for older Minecraft versions.
	* Remove corresponding Maven modules.
	* Update Bukkit/Spigot/CraftBukkit dependencies inside the parent pom.
	* Check for legacy data migrations which could be removed now.
	* Check if there are new Bukkit features which can replace portions of the existing NMS specific code.
	* Use the EntityType enum to get the name of default enabled mobs inside the Settings.

## On every update

* Build and test the new version.
* Make sure the changelog is complete. Fill in the release date.
* Update the version ('revision') in the parent pom (remove the '-SNAPSHOT' tag).
* Commit, build and deploy.
* Add a new git tag for that version.
* Update the version in the parent pom for working on the next version. Re-add the '-SNAPSHOT' tag.
* If not yet done, add a new entry inside the changelog for the next version.
* Commit.

## Update documentation:

* Update the wiki, depending on the changes of the update:
	* Default config.
	* Permissions page.
	* Commands page.
	* Shop setup pages.
	* Images of the default shopkeeper editor.
	* 'Known issues' page.

* Update the project pages, if required: dev.bukkit.org, spigotmc.org

* Update translations repository, if there have been message changes:
	* Add a new folder for the new version.
	* Copy the default language files into it.

## Upload:

* Write changelog: Copy previous changelog, replace contents, adjust the formatting.
* Upload to dev.bukkit.org.
	* Post a comment about the update.
* Upload to spigotmc.org.

