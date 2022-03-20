package com.nisovin.shopkeepers.testutil;

import java.util.logging.Logger;

import org.checkerframework.checker.nullness.qual.NonNull;

import com.nisovin.shopkeepers.SKShopkeepersPlugin;
import com.nisovin.shopkeepers.api.internal.ApiInternals;
import com.nisovin.shopkeepers.api.internal.InternalShopkeepersAPI;
import com.nisovin.shopkeepers.api.internal.InternalShopkeepersPlugin;
import com.nisovin.shopkeepers.internals.SKApiInternals;
import com.nisovin.shopkeepers.util.logging.Log;

/**
 * Mocks the Shopkeepers plugin functionality that is required during tests.
 */
class ShopkeepersPluginMock extends ProxyHandler<@NonNull InternalShopkeepersPlugin> {

	// Static initializer: Ensures that this is only setup once across all tests.
	static {
		// Set up the Log:
		Log.setLogger(Logger.getLogger(SKShopkeepersPlugin.class.getCanonicalName()));

		// Set up the plugin mock as API provider:
		InternalShopkeepersPlugin pluginMock = new ShopkeepersPluginMock().newProxy();
		InternalShopkeepersAPI.enable(pluginMock);
	}

	// Calling this method ensures that the static initializer is invoked.
	public static void setup() {
	}

	private ShopkeepersPluginMock() {
		super(InternalShopkeepersPlugin.class);
	}

	@Override
	protected void setupMethodHandlers() throws Exception {
		ApiInternals apiInternals = new SKApiInternals();
		this.addHandler(
				InternalShopkeepersPlugin.class.getMethod("getApiInternals"),
				(proxy, args) -> {
					return apiInternals;
				}
		);
	}
}
