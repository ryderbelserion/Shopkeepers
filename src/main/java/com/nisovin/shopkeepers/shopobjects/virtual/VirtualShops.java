package com.nisovin.shopkeepers.shopobjects.virtual;

import com.nisovin.shopkeepers.SKShopkeepersPlugin;

// TODO not yet used
public class VirtualShops {

	// TODO not all shopkeeper types might support virtual shops (eg. player shops require a world to locate the chest)
	// -> add a flag to ShopType and validation somewhere (shop creation and shop loading)
	// And/Or: store player shop chest world separately

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
