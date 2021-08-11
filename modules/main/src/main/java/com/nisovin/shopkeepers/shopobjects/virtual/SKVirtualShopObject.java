package com.nisovin.shopkeepers.shopobjects.virtual;

import java.util.List;

import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;

import com.nisovin.shopkeepers.api.shopkeeper.ShopCreationData;
import com.nisovin.shopkeepers.api.shopobjects.virtual.VirtualShopObject;
import com.nisovin.shopkeepers.shopkeeper.AbstractShopkeeper;
import com.nisovin.shopkeepers.shopobjects.AbstractShopObject;
import com.nisovin.shopkeepers.ui.editor.Button;

// TODO Not yet used.
public class SKVirtualShopObject extends AbstractShopObject implements VirtualShopObject {

	protected final VirtualShops virtualShops;

	protected SKVirtualShopObject(VirtualShops virtualShops, AbstractShopkeeper shopkeeper, ShopCreationData creationData) {
		super(shopkeeper, creationData);
		this.virtualShops = virtualShops;
	}

	@Override
	public SKVirtualShopObjectType getType() {
		return virtualShops.getSignShopObjectType();
	}

	@Override
	public void load(ConfigurationSection configSection) {
		super.load(configSection);
	}

	@Override
	public void save(ConfigurationSection configSection) {
		super.save(configSection);
	}

	// ACTIVATION

	@Override
	public boolean isSpawned() {
		return false; // Virtual shops are not present in any world
	}

	@Override
	public boolean isActive() {
		return false;
	}

	@Override
	public String getId() {
		return null;
	}

	@Override
	public boolean spawn() {
		return false;
	}

	@Override
	public void despawn() {
	}

	@Override
	public Location getLocation() {
		return null;
	}

	@Override
	public Location getTickVisualizationParticleLocation() {
		return null;
	}

	// TICKING

	@Override
	public void tick() {
		super.tick();
	}

	// NAMING

	@Override
	public void setName(String name) {
	}

	@Override
	public String getName() {
		return null;
	}

	// EDITOR ACTIONS

	@Override
	public List<Button> createEditorButtons() {
		return super.createEditorButtons();
	}
}
