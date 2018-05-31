package com.nisovin.shopkeepers.api.storage;

/**
 * Responsible for persisting the shopkeepers data.
 */
public interface ShopkeeperStorage {

	/**
	 * Requests a save of all the loaded shopkeepers data.
	 * 
	 * <p>
	 * The actual saving might happen instantly or delayed, depending on the 'save-instantly' setting from the config.
	 * </p>
	 */
	public void save();

	/**
	 * Requests a delayed save of all the loaded shopkeepers data.
	 * 
	 * <p>
	 * This is useful for saves which might get triggered frequently, but don't necessarily need to be performed right
	 * away, even with 'save-instantly' being enabled in the config.<br>
	 * If 'save-instantly' is disabled in the config, this will act just like {@link #save()}. Otherwise it will trigger
	 * a delayed save, if there isn't one going on already. The delay might be shorter if 'save-instantly' is enabled.
	 * </p>
	 */
	public void saveDelayed();

	/**
	 * Instantly saves the shopkeepers data of all loaded shopkeepers to file.
	 * File IO is going to happen asynchronous.
	 */
	public void saveReal();
}
