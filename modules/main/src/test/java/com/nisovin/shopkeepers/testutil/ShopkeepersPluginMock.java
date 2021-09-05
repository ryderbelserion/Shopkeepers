package com.nisovin.shopkeepers.testutil;

import com.nisovin.shopkeepers.api.internal.ApiInternals;
import com.nisovin.shopkeepers.api.internal.InternalShopkeepersAPI;
import com.nisovin.shopkeepers.api.internal.InternalShopkeepersPlugin;
import com.nisovin.shopkeepers.internals.SKApiInternals;

/**
 * Mocks the Shopkeepers plugin functionality that is required during tests.
 */
class ShopkeepersPluginMock extends ProxyHandler<InternalShopkeepersPlugin> {

	// Static initializer: Ensures that this is only setup once across all tests.
	static {
		// Setup the plugin mock as API provider:
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
		this.addHandler(InternalShopkeepersPlugin.class.getMethod("getApiInternals"), (proxy, args) -> {
			return apiInternals;
		});
	}
}
