package com.nisovin.shopkeepers.api.storage;

/**
 * Responsible for persisting and loading the data of shopkeepers.
 */
public interface ShopkeeperStorage {

	/**
	 * Checks whether there is a pending save request, or whether there are any data changes that
	 * have not yet been persisted.
	 * <p>
	 * This method can be used to conditionally trigger a save only if it is required.
	 * <p>
	 * Even if this method returns <code>true</code>, a save might already be pending execution.
	 * However, requesting another save has no harmful effect in this case, since there can be at
	 * most one pending save request.
	 * <p>
	 * This method may return <code>false</code> even if there are unsaved data changes that are
	 * currently being saved. If the save in progress fails for some reason, the storage may become
	 * dirty again. However, it automatically requests another save in this case.
	 * 
	 * @return <code>true</code> if there is a pending save request, or if there are any unsaved
	 *         data changes
	 */
	public boolean isDirty();

	/**
	 * Triggers a {@link #saveNow() save} if there are {@link #isDirty() unsaved data changes}.
	 */
	public default void saveIfDirty() {
		if (this.isDirty()) {
			this.saveNow();
		}
	}

	/**
	 * Requests a save of the data of all shopkeepers.
	 * <p>
	 * The actual saving might happen instantly or delayed, depending on the
	 * '{@code save-instantly}' setting.
	 */
	public void save();

	/**
	 * Requests a delayed save of the data of all shopkeepers.
	 * <p>
	 * This is useful for saves that are frequently triggered, but don't necessarily need to occur
	 * right away, even with the '{@code save-instantly}' setting enabled.
	 * <p>
	 * If the '{@code save-instantly}' setting is disabled, this acts just like {@link #save()}.
	 * Otherwise, this triggers a delayed save, if there isn't one pending already. The delay of
	 * that save might be shorter than the rate at which saves occur if the '{@code save-instantly}'
	 * setting is disabled.
	 */
	public void saveDelayed();

	/**
	 * Saves the data of all shopkeepers.
	 * <p>
	 * The saving is usually done asynchronously (unless there are reasons that prevent this).
	 * <p>
	 * If another save is already in progress, the requested save will take place as soon as the
	 * current save completes. If there already is a save pending execution, the call of this method
	 * may have no effect: The pending save will persist all the data at the time it executes, so
	 * there is no need to execute another save afterwards.
	 */
	public void saveNow();

	/**
	 * Saves the data of all shopkeepers immediately, in a blocking (synchronous) fashion.
	 * <p>
	 * If another save is already in progress, this will wait for the current save to complete.
	 */
	public void saveImmediate();

	/**
	 * Requests an immediate save if there are {@link #isDirty() unsaved data changes}, and waits
	 * for any current and pending saves to complete.
	 */
	public void saveIfDirtyAndAwaitCompletion();
}
