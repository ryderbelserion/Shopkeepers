package com.nisovin.shopkeepers.component;

import java.util.Collections;
import java.util.Set;

import org.checkerframework.checker.nullness.qual.Nullable;

import com.nisovin.shopkeepers.api.internal.util.Unsafe;

/**
 * Base class of all components.
 * <p>
 * A component provides state and/or functionality (for example in the form of
 * {@link #getProvidedServices() provided services}) that can be attached to a
 * {@link ComponentHolder}. Each component can only be attached to a single component holder at a
 * time.
 * <p>
 * Every component must have a public constructor with no arguments.
 */
public abstract class Component {

	private @Nullable ComponentHolder holder;

	/**
	 * Creates a new {@link Component}.
	 */
	public Component() {
	}

	/**
	 * Gets the {@link ComponentHolder} this component is currently attached to.
	 * 
	 * @return the component holder, or <code>null</code> if this component is not attached to any
	 *         holder currently
	 */
	public final @Nullable ComponentHolder getHolder() {
		return holder;
	}

	/**
	 * Sets the holder of this component and invokes respective callbacks.
	 * 
	 * @param holder
	 *            the new holder, can be <code>null</code> if the component has been removed from
	 *            its previous holder
	 */
	void setHolder(@Nullable ComponentHolder holder) {
		if (holder == null) {
			ComponentHolder previousHolder = Unsafe.assertNonNull(this.holder);
			this.holder = holder;
			this.onRemoved(previousHolder);
		} else {
			assert this.holder == null;
			this.holder = holder;
			this.onAdded();
		}
	}

	/**
	 * This is called when the component has been attached to a {@link ComponentHolder}.
	 * <p>
	 * Use {@link #getHolder()} to retrieve the holder.
	 */
	protected void onAdded() {
	}

	/**
	 * This is called when the component has been removed from its previous {@link ComponentHolder}.
	 * 
	 * @param holder
	 *            the previous holder, not <code>null</code>
	 */
	protected void onRemoved(ComponentHolder holder) {
	}

	// SERVICES

	/**
	 * Gets the services provided by this component.
	 * <p>
	 * This component must be assignment compatible with the provided services, i.e. only subclasses
	 * and interfaces implemented by this component can be provided as services.
	 * <p>
	 * The component's own class is always implicitly considered a provided service and is therefore
	 * not required to be included in the returned Set. However, the component's subclasses, even if
	 * those derive from {@link Component} as well, are not implicitly provided as services.
	 * <p>
	 * The returned Set is expected to be fixed, i.e. the component is not allowed to dynamically
	 * change its provided services.
	 * 
	 * @return the provided services, not <code>null</code>, can be empty
	 */
	public Set<? extends Class<?>> getProvidedServices() {
		return Collections.emptySet();
	}

	/**
	 * Called by the {@link ComponentHolder} of this component when this component has been selected
	 * as the active provider for the specified service.
	 * 
	 * @param service
	 *            the service class, not <code>null</code>
	 */
	void informServiceActivated(Class<?> service) {
		assert service != null;
		assert this.getClass() == service || this.getProvidedServices().contains(service);
		this.onServiceActivated(service);
	}

	/**
	 * Called by the {@link ComponentHolder} of this component when this component is no longer
	 * selected as the active provider for the specified service.
	 * 
	 * @param service
	 *            the service class, not <code>null</code>
	 */
	void informServiceDeactivated(Class<?> service) {
		assert service != null;
		assert this.getClass() == service || this.getProvidedServices().contains(service);
		this.onServiceDeactivated(service);
	}

	/**
	 * This is called when the {@link ComponentHolder} of this component has selected this component
	 * as the active provider for the specified service.
	 * 
	 * @param service
	 *            the service class, not <code>null</code>
	 */
	protected void onServiceActivated(Class<?> service) {
	}

	/**
	 * This is called when the {@link ComponentHolder} of this component no longer selects this
	 * component as the active provider for the specified service.
	 * <p>
	 * When the services of this component are deactivated because the component has been removed
	 * from its previous {@link ComponentHolder}, these service callbacks are invoked before
	 * {@link #onRemoved(ComponentHolder)}, and before the {@link #getHolder() current holder} is
	 * unset.
	 * 
	 * @param service
	 *            the service class, not <code>null</code>
	 */
	protected void onServiceDeactivated(Class<?> service) {
	}

	// Identity-based hashCode and equals.

	@Override
	public final int hashCode() {
		return super.hashCode();
	}

	@Override
	public final boolean equals(@Nullable Object obj) {
		return super.equals(obj);
	}
}
