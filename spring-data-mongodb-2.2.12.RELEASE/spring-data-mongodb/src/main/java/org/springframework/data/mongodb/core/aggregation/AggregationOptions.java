/*
 * Copyright 2014-2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.mongodb.core.aggregation;

import java.util.Optional;

import org.bson.Document;
import org.springframework.data.mongodb.core.query.Collation;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

import com.mongodb.DBObject;

/**
 * Holds a set of configurable aggregation options that can be used within an aggregation pipeline. A list of support
 * aggregation options can be found in the MongoDB reference documentation
 * https://docs.mongodb.org/manual/reference/command/aggregate/#aggregate
 *
 * @author Thomas Darimont
 * @author Oliver Gierke
 * @author Christoph Strobl
 * @author Mark Paluch
 * @see Aggregation#withOptions(AggregationOptions)
 * @see TypedAggregation#withOptions(AggregationOptions)
 * @since 1.6
 */
public class AggregationOptions {

	private static final String BATCH_SIZE = "batchSize";
	private static final String CURSOR = "cursor";
	private static final String EXPLAIN = "explain";
	private static final String ALLOW_DISK_USE = "allowDiskUse";
	private static final String COLLATION = "collation";
	private static final String COMMENT = "comment";

	private final boolean allowDiskUse;
	private final boolean explain;
	private final Optional<Document> cursor;
	private final Optional<Collation> collation;
	private final Optional<String> comment;

	/**
	 * Creates a new {@link AggregationOptions}.
	 *
	 * @param allowDiskUse whether to off-load intensive sort-operations to disk.
	 * @param explain whether to get the execution plan for the aggregation instead of the actual results.
	 * @param cursor can be {@literal null}, used to pass additional options to the aggregation.
	 */
	public AggregationOptions(boolean allowDiskUse, boolean explain, Document cursor) {
		this(allowDiskUse, explain, cursor, null);
	}

	/**
	 * Creates a new {@link AggregationOptions}.
	 *
	 * @param allowDiskUse whether to off-load intensive sort-operations to disk.
	 * @param explain whether to get the execution plan for the aggregation instead of the actual results.
	 * @param cursor can be {@literal null}, used to pass additional options (such as {@code batchSize}) to the
	 *          aggregation.
	 * @param collation collation for string comparison. Can be {@literal null}.
	 * @since 2.0
	 */
	public AggregationOptions(boolean allowDiskUse, boolean explain, @Nullable Document cursor,
			@Nullable Collation collation) {
		this(allowDiskUse, explain, cursor, collation, null);
	}

	/**
	 * Creates a new {@link AggregationOptions}.
	 *
	 * @param allowDiskUse whether to off-load intensive sort-operations to disk.
	 * @param explain whether to get the execution plan for the aggregation instead of the actual results.
	 * @param cursor can be {@literal null}, used to pass additional options (such as {@code batchSize}) to the
	 *          aggregation.
	 * @param collation collation for string comparison. Can be {@literal null}.
	 * @param comment execution comment. Can be {@literal null}.
	 * @since 2.2
	 */
	public AggregationOptions(boolean allowDiskUse, boolean explain, @Nullable Document cursor,
			@Nullable Collation collation, @Nullable String comment) {

		this.allowDiskUse = allowDiskUse;
		this.explain = explain;
		this.cursor = Optional.ofNullable(cursor);
		this.collation = Optional.ofNullable(collation);
		this.comment = Optional.ofNullable(comment);
	}

	/**
	 * Creates a new {@link AggregationOptions}.
	 *
	 * @param allowDiskUse whether to off-load intensive sort-operations to disk.
	 * @param explain whether to get the execution plan for the aggregation instead of the actual results.
	 * @param cursorBatchSize initial cursor batch size.
	 * @since 2.0
	 */
	public AggregationOptions(boolean allowDiskUse, boolean explain, int cursorBatchSize) {
		this(allowDiskUse, explain, createCursor(cursorBatchSize), null);
	}

	/**
	 * Creates new {@link AggregationOptions} given {@link DBObject} containing aggregation options.
	 *
	 * @param document must not be {@literal null}.
	 * @return the {@link AggregationOptions}.
	 * @since 2.0
	 */
	public static AggregationOptions fromDocument(Document document) {

		Assert.notNull(document, "Document must not be null!");

		boolean allowDiskUse = document.getBoolean(ALLOW_DISK_USE, false);
		boolean explain = document.getBoolean(EXPLAIN, false);
		Document cursor = document.get(CURSOR, Document.class);
		Collation collation = document.containsKey(COLLATION) ? Collation.from(document.get(COLLATION, Document.class))
				: null;
		String comment = document.getString(COMMENT);

		return new AggregationOptions(allowDiskUse, explain, cursor, collation, comment);
	}

	/**
	 * Obtain a new {@link Builder} for constructing {@link AggregationOptions}.
	 *
	 * @return never {@literal null}.
	 * @since 2.0
	 */
	public static Builder builder() {
		return new Builder();
	}

	/**
	 * Enables writing to temporary files. When set to true, aggregation stages can write data to the _tmp subdirectory in
	 * the dbPath directory.
	 *
	 * @return {@literal true} if enabled.
	 */
	public boolean isAllowDiskUse() {
		return allowDiskUse;
	}

	/**
	 * Specifies to return the information on the processing of the pipeline.
	 *
	 * @return {@literal true} if enabled.
	 */
	public boolean isExplain() {
		return explain;
	}

	/**
	 * The initial cursor batch size, if available, otherwise {@literal null}.
	 *
	 * @return the batch size or {@literal null}.
	 * @since 2.0
	 */
	@Nullable
	public Integer getCursorBatchSize() {

		if (cursor.filter(val -> val.containsKey(BATCH_SIZE)).isPresent()) {
			return cursor.get().get(BATCH_SIZE, Integer.class);
		}

		return null;
	}

	/**
	 * Specify a document that contains options that control the creation of the cursor object.
	 *
	 * @return never {@literal null}.
	 */
	public Optional<Document> getCursor() {
		return cursor;
	}

	/**
	 * Get collation settings for string comparison.
	 *
	 * @return never {@literal null}.
	 * @since 2.0
	 */
	public Optional<Collation> getCollation() {
		return collation;
	}

	/**
	 * Get the comment for the aggregation.
	 *
	 * @return never {@literal null}.
	 * @since 2.2
	 */
	public Optional<String> getComment() {
		return comment;
	}

	/**
	 * Returns a new potentially adjusted copy for the given {@code aggregationCommandObject} with the configuration
	 * applied.
	 *
	 * @param command the aggregation command.
	 * @return
	 */
	Document applyAndReturnPotentiallyChangedCommand(Document command) {

		Document result = new Document(command);

		if (allowDiskUse && !result.containsKey(ALLOW_DISK_USE)) {
			result.put(ALLOW_DISK_USE, allowDiskUse);
		}

		if (explain && !result.containsKey(EXPLAIN)) {
			result.put(EXPLAIN, explain);
		}

		if (!result.containsKey(CURSOR)) {
			cursor.ifPresent(val -> result.put(CURSOR, val));
		}

		if (!result.containsKey(COLLATION)) {
			collation.map(Collation::toDocument).ifPresent(val -> result.append(COLLATION, val));
		}

		return result;
	}

	/**
	 * Returns a {@link Document} representation of this {@link AggregationOptions}.
	 *
	 * @return never {@literal null}.
	 */
	public Document toDocument() {

		Document document = new Document();
		document.put(ALLOW_DISK_USE, allowDiskUse);
		document.put(EXPLAIN, explain);

		cursor.ifPresent(val -> document.put(CURSOR, val));
		collation.ifPresent(val -> document.append(COLLATION, val.toDocument()));
		comment.ifPresent(val -> document.append(COMMENT, val));

		return document;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return toDocument().toJson();
	}

	static Document createCursor(int cursorBatchSize) {
		return new Document("batchSize", cursorBatchSize);
	}

	/**
	 * A Builder for {@link AggregationOptions}.
	 *
	 * @author Thomas Darimont
	 * @author Mark Paluch
	 */
	public static class Builder {

		private boolean allowDiskUse;
		private boolean explain;
		private @Nullable Document cursor;
		private @Nullable Collation collation;
		private @Nullable String comment;

		/**
		 * Defines whether to off-load intensive sort-operations to disk.
		 *
		 * @param allowDiskUse use {@literal true} to allow disk use during the aggregation.
		 * @return this.
		 */
		public Builder allowDiskUse(boolean allowDiskUse) {

			this.allowDiskUse = allowDiskUse;
			return this;
		}

		/**
		 * Defines whether to get the execution plan for the aggregation instead of the actual results.
		 *
		 * @param explain use {@literal true} to enable explain feature.
		 * @return this.
		 */
		public Builder explain(boolean explain) {

			this.explain = explain;
			return this;
		}

		/**
		 * Additional options to the aggregation.
		 *
		 * @param cursor must not be {@literal null}.
		 * @return this.
		 */
		public Builder cursor(Document cursor) {

			this.cursor = cursor;
			return this;
		}

		/**
		 * Define the initial cursor batch size.
		 *
		 * @param batchSize use a positive int.
		 * @return this.
		 * @since 2.0
		 */
		public Builder cursorBatchSize(int batchSize) {

			this.cursor = createCursor(batchSize);
			return this;
		}

		/**
		 * Define collation settings for string comparison.
		 *
		 * @param collation can be {@literal null}.
		 * @return this.
		 * @since 2.0
		 */
		public Builder collation(@Nullable Collation collation) {

			this.collation = collation;
			return this;
		}

		/**
		 * Define a comment to describe the execution.
		 *
		 * @param comment can be {@literal null}.
		 * @return this.
		 * @since 2.2
		 */
		public Builder comment(@Nullable String comment) {

			this.comment = comment;
			return this;
		}

		/**
		 * Returns a new {@link AggregationOptions} instance with the given configuration.
		 *
		 * @return new instance of {@link AggregationOptions}.
		 */
		public AggregationOptions build() {
			return new AggregationOptions(allowDiskUse, explain, cursor, collation, comment);
		}
	}
}
