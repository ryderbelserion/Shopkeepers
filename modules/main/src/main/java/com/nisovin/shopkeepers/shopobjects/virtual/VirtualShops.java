package com.nisovin.shopkeepers.shopobjects.virtual;

import com.nisovin.shopkeepers.SKShopkeepersPlugin;
import com.nisovin.shopkeepers.api.internal.util.Unsafe;

// TODO Not yet used.
public class VirtualShops {

	// TODO Not all shopkeeper types might support virtual shops (e.g. player shops require a world
	// to locate the container currently).
	// -> Add a flag to ShopType and validation somewhere (shop creation and shop loading).
	// And/Or: Store player shop container world separately.

	private final SKShopkeepersPlugin plugin;
	private final SKVirtualShopObjectType virtualShopObjectType = new SKVirtualShopObjectType(
			Unsafe.initialized(this)
	);

	public VirtualShops(SKShopkeepersPlugin plugin) {
		this.plugin = plugin;
	}

	public void onEnable() {
	}

	public void onDisable() {
	}

	public SKVirtualShopObjectType getSignShopObjectType() {
		return virtualShopObjectType;
	}
}
