package com.nisovin.shopkeepers.component;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.nisovin.shopkeepers.api.internal.util.Unsafe;
import com.nisovin.shopkeepers.util.java.Validate;

/**
 * Manages a collection of attached {@link Component}s.
 * <p>
 * Attached components can be {@link #get(Class) queried} by their class. Only one component
 * instance can be added for any particular component class.
 * <p>
 * Additionally, attached components can register {@link Component#getProvidedServices() provided
 * services}. Services can be of any type, including other component types. Every component
 * implicitly provides its own type as a service. When multiple components provide the same service,
 * the most recently added component is chosen as the active provider of that service.
 */
public class ComponentHolder {

	private final Map<Class<? extends Component>, Component> components = new LinkedHashMap<>();
	private final Collection<? extends Component> componentsView = Collections.unmodifiableCollection(components.values());

	private final Map<Class<?>, Component> services = new HashMap<>();

	/**
	 * Creates a new {@link ComponentHolder}.
	 */
	public ComponentHolder() {
	}

	// COMPONENTS

	/**
	 * Gets all currently attached components, in the order in which they were added.
	 * 
	 * @return an unmodifiable view on the currently attached components, not <code>null</code>
	 */
	public final Collection<? extends Component> getComponents() {
		return componentsView;
	}

	/**
	 * Gets the component of the given type.
	 * 
	 * @param <C>
	 *            the component type
	 * @param componentClass
	 *            the component class, not <code>null</code>
	 * @return the component, or <code>null</code> if there is none
	 */
	public final <C extends @Nullable Component> @Nullable C get(
			Class<? extends @NonNull C> componentClass
	) {
		return Unsafe.cast(components.get(componentClass)); // Can be null
	}

	/**
	 * Gets the component of the given type, creating it if necessary.
	 * 
	 * @param <C>
	 *            the component type
	 * @param componentClass
	 *            the component class, not <code>null</code>
	 * @return the component, not <code>null</code>
	 * @throws RuntimeException
	 *             if the component of the given type is missing but cannot be created
	 */
	public final <C extends Component> @NonNull C getOrAdd(
			Class<? extends @NonNull C> componentClass
	) {
		@NonNull C component = Unsafe.castNonNull(components.computeIfAbsent(
				componentClass,
				this::createComponent
		));
		if (component.getHolder() == null) {
			this.onComponentAdded(component);
		}
		return component;
	}

	/**
	 * Adds the given component.
	 * <p>
	 * This replaces any previous component of the same type.
	 * 
	 * @param <C>
	 *            the component type
	 * @param component
	 *            the component, not <code>null</code>
	 */
	public final <C extends Component> void add(@NonNull C component) {
		Validate.notNull(component, "component is null");
		Validate.isTrue(component.getHolder() == null,
				"component is already attached to some holder");
		Component previousComponent = components.put(component.getClass(), component);
		if (previousComponent != null) {
			this.onComponentRemoved(previousComponent);
		}
		this.onComponentAdded(component);
	}

	/**
	 * Removes the component of the given type.
	 * 
	 * @param <C>
	 *            the component type
	 * @param componentClass
	 *            the component class, not <code>null</code>
	 * @return the previous component, or <code>null</code> if there was none
	 */
	public final <C extends @Nullable Component> @Nullable C remove(
			Class<? extends @NonNull C> componentClass
	) {
		@Nullable C component = Unsafe.cast(components.remove(componentClass));
		if (component != null) {
			this.onComponentRemoved(component);
		}
		return component;
	}

	/**
	 * Removes the given component.
	 * 
	 * @param <C>
	 *            the component type
	 * @param component
	 *            the component, not <code>null</code>
	 * @return the given component, not <code>null</code>
	 */
	public final <C extends @Nullable Component> @NonNull C remove(@NonNull C component) {
		Validate.notNull(component, "component is null");
		Class<? extends @NonNull C> componentClass = Unsafe.castNonNull(component.getClass());
		if (components.remove(componentClass, component)) {
			this.onComponentRemoved(component);
		}
		return component;
	}

	private <C extends @Nullable Component> @NonNull C createComponent(
			Class<? extends @NonNull C> componentClass
	) {
		Validate.notNull(componentClass, "componentClass is null");
		try {
			return componentClass.getDeclaredConstructor().newInstance();
		} catch (Exception e) {
			throw new RuntimeException("Failed to create component of type "
					+ componentClass.getName(), e);
		}
	}

	private void onComponentAdded(Component component) {
		assert component != null && component.getHolder() == null;
		component.setHolder(this);
		this.updateServicesOnComponentAdded(component);
	}

	private void onComponentRemoved(Component component) {
		assert component != null && component.getHolder() == this;
		this.updateServicesOnComponentRemoved(component);
		component.setHolder(null);
	}

	// SERVICES

	/**
	 * Gets the component that provides the specified service.
	 * 
	 * @param <S>
	 *            the service type
	 * @param service
	 *            the service class, not <code>null</code>
	 * @return the service provider, or <code>null</code> if there is none
	 */
	public final <S> @Nullable S getService(Class<? extends @NonNull S> service) {
		return Unsafe.cast(services.get(service)); // Can be null
	}

	private void updateServicesOnComponentAdded(Component component) {
		assert component != null && component.getHolder() == this;
		// The given component is the most recently added component. It therefore overrides all
		// previously registered
		// service providers.
		this.setServiceProvider(component.getClass(), component);
		component.getProvidedServices().forEach(service -> {
			this.setServiceProvider(service, component);
		});
	}

	private void updateServicesOnComponentRemoved(Component component) {
		assert component != null;
		this.unsetServiceProvider(component.getClass(), component);
		component.getProvidedServices().forEach(service -> {
			this.unsetServiceProvider(service, component);
		});
	}

	private void setServiceProvider(Class<?> service, Component provider) {
		assert service != null && provider != null && provider.getHolder() == this;
		Validate.isTrue(service.isAssignableFrom(provider.getClass()),
				() -> "component of type " + provider.getClass()
						+ " is not assignment compatible with service " + service);
		Component previousProvider = services.put(service, provider);
		if (previousProvider != null) {
			previousProvider.informServiceDeactivated(service);
		}
		provider.onServiceActivated(service);
	}

	private void unsetServiceProvider(Class<?> service, Component provider) {
		assert service != null && provider != null;
		if (services.remove(service, provider)) {
			provider.informServiceDeactivated(service);

			// Find a new provider:
			Component newProvider = this.findServiceProvider(service, provider);
			if (newProvider != null) {
				services.put(service, newProvider);
				newProvider.informServiceActivated(service);
			}
		}
	}

	private @Nullable Component findServiceProvider(Class<?> service, @Nullable Component ignore) {
		assert service != null;
		// The most recently added component that provides the service is the active service
		// provider:
		Component provider = null;
		for (Component component : this.getComponents()) {
			if (component == ignore) continue;
			if (component.getClass() == service
					|| component.getProvidedServices().contains(service)) {
				provider = component;
			}
		}
		return provider;
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

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append(this.getClass().getName());
		builder.append(" [components=");
		builder.append(components);
		builder.append("]");
		return builder.toString();
	}
}
