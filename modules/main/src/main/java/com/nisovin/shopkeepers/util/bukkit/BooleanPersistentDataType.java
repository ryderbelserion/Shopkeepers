package com.nisovin.shopkeepers.util.bukkit;

import org.bukkit.persistence.PersistentDataAdapterContext;
import org.bukkit.persistence.PersistentDataType;

import com.nisovin.shopkeepers.api.internal.util.Unsafe;

/**
 * A {@link PersistentDataType} that converts between Byte and Boolean. Any byte value > 0 is
 * considered to be <code>true</code>.
 */
// TODO Use Bukkit's PersistentDataType.BOOLEAN once we depend on 1.20+.
public class BooleanPersistentDataType implements PersistentDataType<Byte, Boolean> {

	public static final PersistentDataType<Byte, Boolean> INSTANCE = new BooleanPersistentDataType();

	private BooleanPersistentDataType() {
	}

	@Override
	public Class<Byte> getPrimitiveType() {
		return Unsafe.castNonNull(byte.class);
	}

	@Override
	public Class<Boolean> getComplexType() {
		return Unsafe.castNonNull(boolean.class);
	}

	@Override
	public Byte toPrimitive(Boolean complex, PersistentDataAdapterContext context) {
		return (byte) (complex ? 1 : 0);
	}

	@Override
	public Boolean fromPrimitive(Byte primitive, PersistentDataAdapterContext context) {
		return primitive != 0;
	}
}
