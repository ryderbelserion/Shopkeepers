package com.nisovin.shopkeepers.commands.lib.argument.filter;

import org.checkerframework.checker.nullness.qual.Nullable;

import com.nisovin.shopkeepers.commands.lib.CommandInput;
import com.nisovin.shopkeepers.commands.lib.argument.CommandArgument;
import com.nisovin.shopkeepers.commands.lib.context.CommandContextView;
import com.nisovin.shopkeepers.text.Text;
import com.nisovin.shopkeepers.util.java.Validate;

/**
 * A filter that tests parsed arguments.
 *
 * @param <T>
 *            the type of the filtered parsed arguments
 */
public abstract class ArgumentFilter<T> {

	private static final ArgumentFilter<@Nullable Object> ACCEPT_ANY = new ArgumentFilter<@Nullable Object>() {
		@Override
		public boolean test(
				CommandInput input,
				CommandContextView context,
				@Nullable Object value
		) {
			return true;
		}
	};

	/**
	 * Gets a {@link ArgumentFilter} that accepts any values.
	 * 
	 * @param <T>
	 *            the type of the filtered parsed arguments
	 * @return the argument filter, not <code>null</code>
	 */
	@SuppressWarnings("unchecked")
	public static <T> ArgumentFilter<T> acceptAny() {
		return (ArgumentFilter<T>) ACCEPT_ANY;
	}

	/**
	 * Evaluates this filter on the given argument.
	 * 
	 * @param input
	 *            the command input, not <code>null</code>
	 * @param context
	 *            the context which stores the parsed argument values, not <code>null</code>
	 * @param value
	 *            the parsed argument value
	 * @return {@code true} if the argument value is accepted by this filter
	 */
	public abstract boolean test(
			CommandInput input,
			CommandContextView context,
			T value
	);

	/**
	 * Gets the 'invalid argument' error message for the given parsed but declined value.
	 * <p>
	 * When overriding this method, consider using {@link CommandArgument#getDefaultErrorMsgArgs()}
	 * for the common message arguments.
	 * <p>
	 * Consider using an {@link ArgumentRejectedException} when using the returned message for an
	 * exception.
	 * 
	 * @param argument
	 *            the argument using this filter, not <code>null</code>
	 * @param argumentInput
	 *            the argument input, not <code>null</code>
	 * @param value
	 *            the corresponding parsed but declined value
	 * @return the error message
	 */
	public Text getInvalidArgumentErrorMsg(
			CommandArgument<?> argument,
			String argumentInput,
			T value
	) {
		return argument.getInvalidArgumentErrorMsg(argumentInput);
	}

	/**
	 * Gets a {@link ArgumentRejectedException} that uses this filter's
	 * {@link #getInvalidArgumentErrorMsg(CommandArgument, String, Object) invalid argument error
	 * message}.
	 * 
	 * @param argument
	 *            the argument using this filter, not <code>null</code>
	 * @param argumentInput
	 *            the argument input, not <code>null</code>
	 * @param value
	 *            the corresponding parsed but declined value
	 * @return the {@link ArgumentRejectedException}
	 */
	public ArgumentRejectedException rejectedArgumentException(
			CommandArgument<?> argument,
			String argumentInput,
			T value
	) {
		return new ArgumentRejectedException(
				argument,
				this.getInvalidArgumentErrorMsg(argument, argumentInput, value)
		);
	}

	/**
	 * Returns a composed {@link ArgumentFilter} that filters by both this and the given other
	 * filter. The composed filter is short-circuiting: If an argument value is rejected by this
	 * filter, the given other filter is not invoked.
	 * 
	 * @param other
	 *            the other filter to AND-compose with this filter, not <code>null</code>
	 * @return the composed filter
	 */
	public ArgumentFilter<T> and(ArgumentFilter<? super T> other) {
		Validate.notNull(other, "other");
		return new ArgumentFilter<T>() {
			@Override
			public boolean test(CommandInput input, CommandContextView context, T value) {
				return ArgumentFilter.this.test(input, context, value)
						&& other.test(input, context, value);
			}
		};
	}
}
