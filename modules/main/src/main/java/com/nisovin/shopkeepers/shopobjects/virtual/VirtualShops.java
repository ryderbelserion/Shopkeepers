package com.nisovin.shopkeepers.shopobjects.virtual;

import com.nisovin.shopkeepers.SKShopkeepersPlugin;

// TODO Not yet used.
public class VirtualShops {

	// TODO Not all shopkeeper types might support virtual shops (e.g. player shops require a world to locate the
	// container currently).
	// -> Add a flag to ShopType and validation somewhere (shop creation and shop loading).
	// And/Or: Store player shop container world separately.

	private final SKShopkeepersPlugin plugin;
	private final SKVirtualShopObjectType virtualShopObjectType = new SKVirtualShopObjectType(this);

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
