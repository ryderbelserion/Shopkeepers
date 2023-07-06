package com.nisovin.shopkeepers.api.events;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.Plugin;

import com.nisovin.shopkeepers.api.ShopkeepersAPI;

/**
 * Gets called very early during the startup of the Shopkeepers plugin.
 * <p>
 * This can be used to make registrations that are used by the Shopkeepers plugin during startup
 * (initially and on every reload of the plugin). In case your plugin has Shopkeepers as a
 * dependency, your plugin will likely get loaded after the Shopkeepers plugin got loaded, and
 * enabled after the Shopkeepers plugin got enabled. In that case, since plugins can only register
 * event listeners after they got enabled, you may also want to apply any registrations during your
 * plugin's {@link Plugin#onLoad() loading phase} (<b>after</b> checking that the ShopkeepersAPI has
 * already been {@link ShopkeepersAPI#isEnabled() enabled}).
 * 
 * @deprecated The Shopkeepers plugin, and thereby also the API, is not yet fully enabled and ready
 *             to be used at this point. This event is only supposed to be used by plugins that need
 *             to hook into Shopkeepers startup process and know what they are doing.
 */
@Deprecated
public class ShopkeepersStartupEvent extends Event {

	/**
	 * Creates a new {@link ShopkeepersStartupEvent}.
	 */
	public ShopkeepersStartupEvent() {
	}

	private static final HandlerList handlers = new HandlerList();

	@Override
	public HandlerList getHandlers() {
		return handlers;
	}

	/**
	 * Gets the {@link HandlerList} of this event.
	 * 
	 * @return the handler list
	 */
	public static HandlerList getHandlerList() {
		return handlers;
	}
}
