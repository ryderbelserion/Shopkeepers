package com.nisovin.shopkeepers.testutil;

import org.bukkit.inventory.ItemStack;

import com.nisovin.shopkeepers.api.ShopkeepersAPI;
import com.nisovin.shopkeepers.api.ShopkeepersPlugin;
import com.nisovin.shopkeepers.util.inventory.SKUnmodifiableItemStack;

/**
 * Mocks the Shopkeepers plugin functionality that is required during tests.
 */
class ShopkeepersPluginMock extends ProxyHandler<ShopkeepersPlugin> {

	// Static initializer: Ensures that this is only setup once across all tests.
	static {
		// Setup the plugin mock as API provider:
		ShopkeepersPlugin pluginMock = new ShopkeepersPluginMock().newProxy();
		ShopkeepersAPI.enable(pluginMock);
	}

	// Calling this method ensures that the static initializer is invoked.
	public static void setup() {
	}

	private ShopkeepersPluginMock() {
		super(ShopkeepersPlugin.class);
	}

	@Override
	protected void setupMethodHandlers() throws Exception {
		this.addHandler(ShopkeepersPlugin.class.getMethod("createUnmodifiableItemStack", ItemStack.class), (proxy, args) -> {
			return SKUnmodifiableItemStack.of((ItemStack) args[0]);
		});
	}
}
