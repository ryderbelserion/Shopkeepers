package com.nisovin.shopkeepers.shopobjects.living;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Map.Entry;

import org.bukkit.inventory.EquipmentSlot;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.nisovin.shopkeepers.api.internal.util.Unsafe;
import com.nisovin.shopkeepers.api.shopobjects.living.LivingShopEquipment;
import com.nisovin.shopkeepers.api.util.UnmodifiableItemStack;
import com.nisovin.shopkeepers.util.data.container.DataContainer;
import com.nisovin.shopkeepers.util.data.serialization.DataSerializer;
import com.nisovin.shopkeepers.util.data.serialization.InvalidDataException;
import com.nisovin.shopkeepers.util.data.serialization.bukkit.ItemStackSerializers;
import com.nisovin.shopkeepers.util.data.serialization.bukkit.MinecraftEnumSerializers;
import com.nisovin.shopkeepers.util.data.serialization.java.DataContainerSerializers;
import com.nisovin.shopkeepers.util.inventory.ItemUtils;
import com.nisovin.shopkeepers.util.java.Validate;

public class SKLivingShopEquipment implements LivingShopEquipment {

	/**
	 * A {@link DataSerializer} for values of type {@link LivingShopEquipment}.
	 */
	public static final DataSerializer<LivingShopEquipment> SERIALIZER
			= new DataSerializer<LivingShopEquipment>() {
				@Override
				public @Nullable Object serialize(LivingShopEquipment value) {
					Validate.notNull(value, "value is null");
					DataContainer equipmentData = DataContainer.create();
					for (Entry<? extends EquipmentSlot, ? extends @Nullable UnmodifiableItemStack> entry : value.getItems().entrySet()) {
						// The items are assumed to be immutable.
						equipmentData.set(entry.getKey().name(), entry.getValue());
					}
					return equipmentData.serialize();
				}

				@Override
				public LivingShopEquipment deserialize(Object data) throws InvalidDataException {
					SKLivingShopEquipment equipment = new SKLivingShopEquipment();

					DataContainer equipmentData = DataContainerSerializers.DEFAULT.deserialize(data);
					for (String equipmentSlotName : equipmentData.getKeys()) {
						EquipmentSlot equipmentSlot = MinecraftEnumSerializers.EquipmentSlots.LENIENT
								.deserialize(equipmentSlotName);

						// The item stack is assumed to be immutable and therefore does not need to
						// be copied.
						// An empty item results in no item to be equipped.
						Object itemData = Unsafe.assertNonNull(equipmentData.get(equipmentSlotName));
						UnmodifiableItemStack item = ItemStackSerializers.UNMODIFIABLE.deserialize(itemData);

						equipment.setItem(equipmentSlot, item);
					}

					return equipment;
				}
			};

	private final Map<EquipmentSlot, UnmodifiableItemStack> items
			= new EnumMap<>(EquipmentSlot.class);
	private final Map<? extends EquipmentSlot, ? extends UnmodifiableItemStack> itemsView
			= Collections.unmodifiableMap(items);

	private @Nullable Runnable changedListener;

	public SKLivingShopEquipment() {
	}

	void setChangedListener(@Nullable Runnable changedListener) {
		Validate.State.isTrue(this.changedListener == null, "changedListener already set!");
		this.changedListener = changedListener;
	}

	@Override
	public Map<? extends EquipmentSlot, ? extends UnmodifiableItemStack> getItems() {
		return itemsView;
	}

	@Override
	public @Nullable UnmodifiableItemStack getItem(EquipmentSlot slot) {
		return items.get(slot);
	}

	@Override
	public void setItem(EquipmentSlot slot, @Nullable UnmodifiableItemStack item) {
		if (ItemUtils.isEmpty(item)) {
			items.remove(slot);
		} else {
			assert item != null;
			items.put(slot, item);
		}

		// Inform the changed listener:
		if (changedListener != null) {
			changedListener.run();
		}
	}

	@Override
	public void clear() {
		items.clear();

		// Inform the changed listener:
		if (changedListener != null) {
			changedListener.run();
		}
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + itemsView.hashCode();
		return result;
	}

	@Override
	public boolean equals(@Nullable Object obj) {
		if (this == obj) return true;
		if (!(obj instanceof SKLivingShopEquipment)) return false;
		SKLivingShopEquipment other = (SKLivingShopEquipment) obj;
		if (!itemsView.equals(other.itemsView)) return false;
		return true;
	}
}
