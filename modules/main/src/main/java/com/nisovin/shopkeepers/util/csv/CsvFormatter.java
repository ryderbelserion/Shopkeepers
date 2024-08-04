package com.nisovin.shopkeepers.util.csv;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.checkerframework.checker.nullness.qual.Nullable;

import com.nisovin.shopkeepers.api.internal.util.Unsafe;
import com.nisovin.shopkeepers.util.java.CollectionUtils;
import com.nisovin.shopkeepers.util.java.Lazy;
import com.nisovin.shopkeepers.util.java.StringUtils;
import com.nisovin.shopkeepers.util.java.Validate;
import com.nisovin.shopkeepers.util.logging.Log;

/**
 * Formats data as comma separated values (CSV).
 */
public class CsvFormatter {

	private static String quoteReplacement(String replacement) {
		return Matcher.quoteReplacement(replacement);
	}

	private String fieldSeparator = ",";
	private String recordSeparator = "\n";
	private boolean recordSeparatorContainsNewline = true;
	private String quote = "\""; // Can be empty
	private Pattern quotePattern = Pattern.compile(quote, Pattern.LITERAL);
	private String escapedQuote = quoteReplacement("\"\"");
	private boolean quoteAllFields = true;
	private boolean escapeNewlines = true;
	private boolean warnOnNewlines = false;
	private String nullField = ""; // Can be empty

	/**
	 * Creates a new {@link CsvFormatter}.
	 * <p>
	 * The default configuration is as follows:
	 * <ul>
	 * <li>Use commas as field separator.
	 * <li>Use the Unix newline character ({@code \n}) as record separator.
	 * <li>Quote all fields.
	 * <li>Use double quotes to quote fields.
	 * <li>Use two double quotes to escape double quotes within fields.
	 * <li>Escape newline characters (and backslashes) within fields. This ensures that each record
	 * spans a single line.
	 * <li>Use an empty String to represent <code>null</code> fields.
	 * </ul>
	 * <p>
	 * Except for the escaping of newlines, this default configuration conforms to the CSV format
	 * described by {@code https://tools.ietf.org/html/rfc4180}.
	 * <p>
	 * The default configuration can be changed after construction. However, some configurations may
	 * result in output that is usually no longer considered to be valid CSV. Such output may be
	 * difficult to parse, ambiguous, or not parsable at all.
	 */
	public CsvFormatter() {
	}

	// CONFIGURATION

	/**
	 * Sets the field separator to use.
	 * <p>
	 * The default is a comma.
	 * 
	 * @param fieldSeparator
	 *            the field separator, not <code>null</code> or empty
	 * @return this formatter
	 */
	public CsvFormatter fieldSeparator(String fieldSeparator) {
		Validate.notEmpty(fieldSeparator, "fieldSeparator is null or empty");
		this.fieldSeparator = fieldSeparator;
		return this;
	}

	/**
	 * Sets the record separator to use.
	 * <p>
	 * The default is a Unix newline character.
	 * 
	 * @param recordSeparator
	 *            the record separator, not <code>null</code> or empty
	 * @return this formatter
	 */
	public CsvFormatter recordSeparator(String recordSeparator) {
		Validate.notEmpty(recordSeparator, "recordSeparator is null or empty");
		this.recordSeparator = recordSeparator;
		recordSeparatorContainsNewline = StringUtils.containsNewline(recordSeparator);
		return this;
	}

	/**
	 * Sets the String to use when quoting fields.
	 * <p>
	 * An empty String will result in no quoting to take place in any case.
	 * <p>
	 * When you change this, you may also have to adjust {@link #escapedQuote(String)}.
	 * <p>
	 * The default is a single double quote.
	 * 
	 * @param quote
	 *            the quote String, not <code>null</code>, but can be empty
	 * @return this formatter
	 */
	public CsvFormatter quote(String quote) {
		Validate.notNull(quote, "quote is null");
		this.quote = quote;
		quotePattern = Pattern.compile(quote, Pattern.LITERAL);
		return this;
	}

	/**
	 * Sets the String that replaces quotes within fields.
	 * <p>
	 * The default is two double quotes.
	 * 
	 * @param escapedQuote
	 *            the escaped quote, not <code>null</code>, but can be empty
	 * @return this formatter
	 */
	public CsvFormatter escapedQuote(String escapedQuote) {
		Validate.notNull(escapedQuote, "escapedQuote is null");
		this.escapedQuote = quoteReplacement(escapedQuote);
		return this;
	}

	/**
	 * Always quote all fields.
	 * <p>
	 * The default is <code>true</code>.
	 * 
	 * @return this formatter
	 * @see #quoteAllFields(boolean)
	 */
	public CsvFormatter quoteAllFields() {
		return this.quoteAllFields(true);
	}

	/**
	 * Sets whether to quote all fields.
	 * <p>
	 * If disabled, only fields that contain special characters are quoted.
	 * <p>
	 * The default is <code>true</code>.
	 * 
	 * @param quoteAllFields
	 *            <code>true</code> to quote all fields
	 * @return this formatter
	 */
	public CsvFormatter quoteAllFields(boolean quoteAllFields) {
		this.quoteAllFields = quoteAllFields;
		return this;
	}

	/**
	 * Escape newline characters (and backslashes) in fields.
	 * <p>
	 * The default is <code>true</code>.
	 * 
	 * @return this formatter
	 * @see #escapeNewlines(boolean)
	 */
	public CsvFormatter escapeNewlines() {
		return this.escapeNewlines(true);
	}

	/**
	 * Sets whether to escape newline characters (and backslashes) in fields.
	 * <p>
	 * When lines breaks are otherwise only used as record separator, this ensures that records only
	 * span a single line.
	 * <p>
	 * The default is <code>true</code>.
	 * 
	 * @param escapeNewlines
	 *            <code>true</code> to escape newlines
	 * @return this formatter
	 */
	public CsvFormatter escapeNewlines(boolean escapeNewlines) {
		this.escapeNewlines = escapeNewlines;
		return this;
	}

	/**
	 * Log a warning whenever a field contains a newline character.
	 * <p>
	 * The default is <code>false</code>.
	 * 
	 * @return this formatter
	 * @see #warnOnNewlines(boolean)
	 */
	public CsvFormatter warnOnNewlines() {
		return this.warnOnNewlines(true);
	}

	/**
	 * Sets whether to log a warning whenever a field contains a newline character.
	 * <p>
	 * The default is <code>false</code>.
	 * 
	 * @param warnOnNewlines
	 *            <code>true</code> to warn on newlines
	 * @return this formatter
	 */
	public CsvFormatter warnOnNewlines(boolean warnOnNewlines) {
		this.warnOnNewlines = warnOnNewlines;
		return this;
	}

	/**
	 * Sets the String to replace <code>null</code> fields with.
	 * <p>
	 * The default is an empty String.
	 * 
	 * @param nullField
	 *            the String to use for <code>null</code> fields, not <code>null</code>, but can be
	 *            empty
	 * @return this formatter
	 */
	public CsvFormatter nullField(String nullField) {
		Validate.notNull(nullField, "nullField is null");
		this.nullField = nullField;
		return this;
	}

	// FORMATTING

	/**
	 * Formats the given fields like a CSV record according to this formatter's configuration, but
	 * omits the {@link #recordSeparator(String) record separator} at the end.
	 * <p>
	 * The fields are converted to Strings via their {@link #toString()} method. <code>null</code>
	 * objects are replaced with the configured {@link #nullField(String) null field value}.
	 * <p>
	 * The fields are escaped via {@link #escapeField(String)} and then joined by
	 * {@link #fieldSeparator(String) field separators}.
	 * <p>
	 * Use {@link #formatRecord(Object[])} to include the {@link #recordSeparator(String) record
	 * separator} at the end.
	 * <p>
	 * This can also be used for the header of a CSV file.
	 * 
	 * @param fields
	 *            the fields, not <code>null</code>
	 * @return the CSV-formatted fields
	 */
	public String formatFields(@Nullable Object[] fields) {
		Validate.notNull(fields, "fields is null");
		return this.formatFields(Stream.<@Nullable Object>of(fields));
	}

	/**
	 * Formats the given fields like a CSV record according to this formatter's configuration, but
	 * omits the {@link #recordSeparator(String) record separator} at the end.
	 * <p>
	 * The fields are converted to Strings via their {@link #toString()} method. <code>null</code>
	 * objects are replaced with the configured {@link #nullField(String) null field value}.
	 * <p>
	 * The fields are escaped via {@link #escapeField(String)} and then joined by
	 * {@link #fieldSeparator(String) field separators}.
	 * <p>
	 * Use {@link #formatRecord(Iterable)} to include the {@link #recordSeparator(String) record
	 * separator} at the end.
	 * <p>
	 * This can also be used for the header of a CSV file.
	 * 
	 * @param fields
	 *            the fields, not <code>null</code>
	 * @return the CSV-formatted fields
	 */
	public String formatFields(Iterable<?> fields) {
		Validate.notNull(fields, "fields is null");
		// TODO Cast: Required due to a limitation of CheckerFramework
		return this.formatFields(CollectionUtils.stream(Unsafe.castNonNull(fields)));
	}

	/**
	 * Formats the given fields as described by {@link #formatFields(Object[])} and
	 * {@link #formatFields(Iterable)}.
	 * 
	 * @param fields
	 *            the fields, not <code>null</code>
	 * @return the CSV-formatted fields
	 */
	private String formatFields(Stream<?> fields) {
		assert fields != null;
		return fields
				.<@Nullable String>map(StringUtils::toStringOrNull)
				.map(this::escapeField)
				.collect(Collectors.joining(fieldSeparator));
	}

	/**
	 * Formats the given fields as described by {@link #formatFields(Object[])} and includes the
	 * {@link #recordSeparator(String) record separator} at the end.
	 * <p>
	 * This can also be used for the header of a CSV file.
	 * 
	 * @param fields
	 *            the fields, not <code>null</code>
	 * @return the CSV record
	 */
	public String formatRecord(@Nullable Object[] fields) {
		return this.formatFields(fields) + recordSeparator;
	}

	/**
	 * Formats the given fields as described by {@link #formatFields(Iterable)} and includes the
	 * {@link #recordSeparator(String) record separator} at the end.
	 * <p>
	 * This can also be used for the header of a CSV file.
	 * 
	 * @param fields
	 *            the fields, not <code>null</code>
	 * @return the CSV record
	 */
	public String formatRecord(Iterable<?> fields) {
		return this.formatFields(fields) + recordSeparator;
	}

	/**
	 * Escapes the given field data according to the configuration of this formatter.
	 * <p>
	 * If the field is <code>null</code>, it is replaced with the {@link #nullField(String) null
	 * field value}. If {@link #escapeNewlines() enabled}, newlines and backslashes are escaped.
	 * Then, if the configured {@link #quote(String) quote} is not empty, the field data is quoted
	 * if at least one of the following applies:
	 * <ul>
	 * <li>Quoting of {@link #quoteAllFields() all fields} is enabled.
	 * <li>The field contains the {@link #quote(String) quote}.
	 * <li>The field contains {@link #escapeNewlines() unescaped} newlines.
	 * <li>The field contains the {@link #fieldSeparator(String) field separator}.
	 * <li>The {@link #recordSeparator(String) record separator} does not contain a newline and the
	 * field contains the {@link #recordSeparator(String) record separator}.
	 * </ul>
	 * Any occurrences of the (non-empty) {@link #quote(String) quote} are replaced with the
	 * {@link #escapedQuote(String) escaped quote}.
	 * 
	 * @param field
	 *            the field data, can be <code>null</code>
	 * @return the escaped field data, not <code>null</code>
	 */
	public String escapeField(@Nullable String field) {
		String nonNullField = (field != null) ? field : nullField;
		String escaped = nonNullField;

		// Note: This also checks the nullField for newlines.
		Lazy<Boolean> containsNewline = new Lazy<>(() -> StringUtils.containsNewline(nonNullField));
		if (warnOnNewlines && containsNewline.get()) {
			Log.warning("CSV field contains a newline character! " + escaped);
		}

		if (escapeNewlines && (containsNewline.get() || escaped.contains("\\"))) {
			// Note: Even if the field's newlines were already escaped externally, possibly even
			// using the same backslash escape sequences, we still have to escape backslashes
			// another time here. Otherwise, a CSV parser that unescapes these characters again,
			// because it assumes that they were originally escaped by the CSV formatter, will not
			// be able to reproduce the field's original data, which may cause issues downstream if
			// this field data is expected to not contain newlines.
			// This is for example the case if the field data is in the Json format: The Json format
			// requires newline characters to be escaped. If a CSV reader unescapes these
			// characters, the resulting Json data will no longer be valid.
			escaped = StringUtils.escapeNewlinesAndBackslash(escaped);
		}

		if (!quote.isEmpty()) {
			boolean containsQuote = escaped.contains(quote);
			if (quoteAllFields
					|| containsQuote
					|| (!escapeNewlines && containsNewline.get())
					|| escaped.contains(fieldSeparator)
					|| (!recordSeparatorContainsNewline && escaped.contains(recordSeparator))) {
				if (containsQuote) {
					escaped = quotePattern.matcher(escaped).replaceAll(escapedQuote);
				}
				escaped = quote + escaped + quote;
			}
		}

		return escaped;
	}
}
