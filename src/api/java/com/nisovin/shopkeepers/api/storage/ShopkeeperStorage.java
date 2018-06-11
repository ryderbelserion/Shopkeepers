package com.nisovin.shopkeepers.api.storage;

/**
 * Responsible for persisting and loading the shopkeepers data.
 */
public interface ShopkeeperStorage {

	/**
	 * Requests a save of all loaded shopkeepers data.
	 * <p>
	 * The actual saving might happen instantly or delayed, depending on the 'save-instantly' setting from the config.
	 */
	public void save();

	/**
	 * Requests a delayed save of the loaded shopkeepers data.
	 * <p>
	 * This is useful for saves which might get triggered frequently, but don't necessarily need to be performed right
	 * away, even with 'save-instantly' being enabled in the config.<br>
	 * If 'save-instantly' is disabled in the config, this will act just like {@link #save()}. Otherwise it will trigger
	 * a delayed save, if there isn't one going on already. The delay might be shorter if 'save-instantly' is enabled.
	 */
	public void saveDelayed();

	/**
	 * Saves all loaded shopkeepers data. May be asynchronous.
	 * <p>
	 * If an asynchronous save is already active, this will request another save once it has finished.
	 */
	public void saveNow();

	/**
	 * Saves all loaded shopkeepers data immediately. Must not be asynchronous.
	 * <p>
	 * Note: This is blocking. If there is an asynchronous save going on currently, this will wait for it to finish.
	 */
	public void saveImmediate();
}
