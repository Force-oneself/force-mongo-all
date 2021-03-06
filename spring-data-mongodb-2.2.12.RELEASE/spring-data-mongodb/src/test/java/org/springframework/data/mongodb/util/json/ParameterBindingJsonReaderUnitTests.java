/*
 * Copyright 2019-2020 the original author or authors.
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
package org.springframework.data.mongodb.util.json;

import static org.assertj.core.api.Assertions.*;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.bson.Document;
import org.bson.codecs.DecoderContext;
import org.junit.Test;
import org.springframework.data.spel.EvaluationContextProvider;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.TypedValue;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

/**
 * Unit tests for {@link ParameterBindingJsonReader}.
 *
 * @author Christoph Strobl
 */
public class ParameterBindingJsonReaderUnitTests {

	@Test
	public void bindUnquotedStringValue() {

		Document target = parse("{ 'lastname' : ?0 }", "kohlin");
		assertThat(target).isEqualTo(new Document("lastname", "kohlin"));
	}

	@Test
	public void bindQuotedStringValue() {

		Document target = parse("{ 'lastname' : '?0' }", "kohlin");
		assertThat(target).isEqualTo(new Document("lastname", "kohlin"));
	}

	@Test
	public void bindUnquotedIntegerValue() {

		Document target = parse("{ 'lastname' : ?0 } ", 100);
		assertThat(target).isEqualTo(new Document("lastname", 100));
	}

	@Test
	public void bindMultiplePlacholders() {

		Document target = parse("{ 'lastname' : ?0, 'firstname' : '?1' }", "Kohlin", "Dalinar");
		assertThat(target).isEqualTo(Document.parse("{ 'lastname' : 'Kohlin', 'firstname' : 'Dalinar' }"));
	}

	@Test
	public void bindQuotedIntegerValue() {

		Document target = parse("{ 'lastname' : '?0' }", 100);
		assertThat(target).isEqualTo(new Document("lastname", "100"));
	}

	@Test
	public void bindValueToRegex() {

		Document target = parse("{ 'lastname' : { '$regex' : '^(?0)'} }", "kohlin");
		assertThat(target).isEqualTo(Document.parse("{ 'lastname' : { '$regex' : '^(kohlin)'} }"));
	}

	@Test
	public void bindValueToMultiRegex() {

		Document target = parse(
				"{'$or' : [{'firstname': {'$regex': '.*?0.*', '$options': 'i'}}, {'lastname' : {'$regex': '.*?0xyz.*', '$options': 'i'}} ]}",
				"calamity");
		assertThat(target).isEqualTo(Document.parse(
				"{ \"$or\" : [ { \"firstname\" : { \"$regex\" : \".*calamity.*\" , \"$options\" : \"i\"}} , { \"lastname\" : { \"$regex\" : \".*calamityxyz.*\" , \"$options\" : \"i\"}}]}"));
	}

	@Test
	public void bindMultipleValuesToSingleToken() {

		Document target = parse("{$where: 'return this.date.getUTCMonth() == ?2 && this.date.getUTCDay() == ?3;'}", 0, 1, 2,
				3, 4);
		assertThat(target)
				.isEqualTo(Document.parse("{$where: 'return this.date.getUTCMonth() == 2 && this.date.getUTCDay() == 3;'}"));
	}

	@Test
	public void bindValueToDbRef() {

		Document target = parse("{ 'reference' : { $ref : 'reference', $id : ?0 }}", "kohlin");
		assertThat(target).isEqualTo(Document.parse("{ 'reference' : { $ref : 'reference', $id : 'kohlin' }}"));
	}

	@Test
	public void bindToKey() {

		Document target = parse("{ ?0 : ?1 }", "firstname", "kaladin");
		assertThat(target).isEqualTo(Document.parse("{ 'firstname' : 'kaladin' }"));
	}

	@Test
	public void bindListValue() {

		//
		Document target = parse("{ 'lastname' : { $in : ?0 } }", Arrays.asList("Kohlin", "Davar"));
		assertThat(target).isEqualTo(Document.parse("{ 'lastname' : { $in : ['Kohlin', 'Davar' ]} }"));
	}

	@Test
	public void bindListOfBinaryValue() {

		//
		byte[] value = "Kohlin".getBytes(StandardCharsets.UTF_8);
		List<byte[]> args = Collections.singletonList(value);

		Document target = parse("{ 'lastname' : { $in : ?0 } }", args);
		assertThat(target).isEqualTo(new Document("lastname", new Document("$in", args)));
	}

	@Test
	public void bindExtendedExpression() {

		Document target = parse("{'id':?#{ [0] ? { $exists :true} : [1] }}", true, "firstname", "kaladin");
		assertThat(target).isEqualTo(Document.parse("{ \"id\" : { \"$exists\" : true}}"));
	}

	// {'id':?#{ [0] ? { $exists :true} : [1] }}

	@Test
	public void bindDocumentValue() {

		//
		Document target = parse("{ 'lastname' : ?0 }", new Document("$eq", "Kohlin"));
		assertThat(target).isEqualTo(Document.parse("{ 'lastname' : { '$eq' : 'Kohlin' } }"));
	}

	@Test
	public void arrayWithoutBinding() {

		//
		Document target = parse("{ 'lastname' : { $in : [\"Kohlin\", \"Davar\"] } }");
		assertThat(target).isEqualTo(Document.parse("{ 'lastname' : { $in : ['Kohlin', 'Davar' ]} }"));
	}

	@Test
	public void bindSpEL() {

		// "{ arg0 : ?#{[0]} }"
		Document target = parse("{ arg0 : ?#{[0]} }", 100.01D);
		assertThat(target).isEqualTo(new Document("arg0", 100.01D));
	}

	@Test // DATAMONGO-2315
	public void bindDateAsDate() {

		Date date = new Date();
		Document target = parse("{ 'end_date' : { $gte : { $date : ?0 } } }", date);

		assertThat(target).isEqualTo(Document.parse("{ 'end_date' : { $gte : { $date : " + date.getTime() + " } } } "));
	}

	@Test // DATAMONGO-2315
	public void bindQuotedDateAsDate() {

		Date date = new Date();
		Document target = parse("{ 'end_date' : { $gte : { $date : '?0' } } }", date);

		assertThat(target).isEqualTo(Document.parse("{ 'end_date' : { $gte : { $date : " + date.getTime() + " } } } "));
	}

	@Test // DATAMONGO-2315
	public void bindStringAsDate() {

		Date date = new Date();
		Document target = parse("{ 'end_date' : { $gte : { $date : ?0 } } }", "2019-07-04T12:19:23.000Z");

		assertThat(target).isEqualTo(Document.parse("{ 'end_date' : { $gte : { $date : '2019-07-04T12:19:23.000Z' } } } "));
	}

	@Test // DATAMONGO-2315
	public void bindNumberAsDate() {

		Long time = new Date().getTime();
		Document target = parse("{ 'end_date' : { $gte : { $date : ?0 } } }", time);

		assertThat(target).isEqualTo(Document.parse("{ 'end_date' : { $gte : { $date : " + time + " } } } "));
	}

	@Test // DATAMONGO-2418
	public void shouldNotAccessSpElEvaluationContextWhenNoSpElPresentInBindableTarget() {

		Object[] args = new Object[] { "value" };
		EvaluationContext evaluationContext = new StandardEvaluationContext() {

			@Override
			public TypedValue getRootObject() {
				throw new RuntimeException("o_O");
			}
		};

		ParameterBindingJsonReader reader = new ParameterBindingJsonReader("{ 'name':'?0' }",
				new ParameterBindingContext((index) -> args[index], new SpelExpressionParser(), evaluationContext));
		Document target = new ParameterBindingDocumentCodec().decode(reader, DecoderContext.builder().build());

		assertThat(target).isEqualTo(new Document("name", "value"));
	}

	@Test // DATAMONGO-2476
	public void bindUnquotedParameterInArray() {

		Document target = parse("{ 'name' : { $in : [?0] } }", "kohlin");
		assertThat(target).isEqualTo(new Document("name", new Document("$in", Collections.singletonList("kohlin"))));
	}

	@Test // DATAMONGO-2476
	public void bindMultipleUnquotedParameterInArray() {

		Document target = parse("{ 'name' : { $in : [?0,?1] } }", "dalinar", "kohlin");
		assertThat(target).isEqualTo(new Document("name", new Document("$in", Arrays.asList("dalinar", "kohlin"))));
	}

	@Test // DATAMONGO-2476
	public void bindUnquotedParameterInArrayWithSpaces() {

		Document target = parse("{ 'name' : { $in : [ ?0 ] } }", "kohlin");
		assertThat(target).isEqualTo(new Document("name", new Document("$in", Collections.singletonList("kohlin"))));
	}

	@Test // DATAMONGO-2476
	public void bindQuotedParameterInArray() {

		Document target = parse("{ 'name' : { $in : ['?0'] } }", "kohlin");
		assertThat(target).isEqualTo(new Document("name", new Document("$in", Collections.singletonList("kohlin"))));
	}

	@Test // DATAMONGO-2476
	public void bindQuotedMulitParameterInArray() {

		Document target = parse("{ 'name' : { $in : ['?0,?1'] } }", "dalinar", "kohlin");
		assertThat(target)
				.isEqualTo(new Document("name", new Document("$in", Collections.singletonList("dalinar,kohlin"))));
	}

	@Test // DATAMONGO-2523
	public void bindSpelExpressionInArrayCorrectly/* closing bracket must not have leading whitespace! */() {

		Document target = parse("{ $and : [?#{ [0] == null  ? { '$where' : 'true' } : { 'v1' : { '$in' : {[0]} } } }]}", 1);

		assertThat(target).isEqualTo(Document.parse("{\"$and\": [{\"v1\": {\"$in\": [1]}}]}"));
	}

	@Test // DATAMONGO-2545
	public void shouldABindArgumentsViaIndexInSpelExpressions() {

		Object[] args = new Object[] { "yess", "nooo" };
		StandardEvaluationContext evaluationContext = (StandardEvaluationContext) EvaluationContextProvider.DEFAULT
				.getEvaluationContext(args);

		ParameterBindingJsonReader reader = new ParameterBindingJsonReader(
				"{ 'isBatman' : ?#{ T(" + this.getClass().getName() + ").isBatman() ? [0] : [1] }}",
				new ParameterBindingContext((index) -> args[index], new SpelExpressionParser(), evaluationContext));
		Document target = new ParameterBindingDocumentCodec().decode(reader, DecoderContext.builder().build());

		assertThat(target).isEqualTo(new Document("isBatman", "nooo"));
	}

	@Test // DATAMONGO-2545
	public void shouldAllowMethodArgumentPlaceholdersInSpelExpressions/*becuase this worked before*/() {

		Object[] args = new Object[] { "yess", "nooo" };
		StandardEvaluationContext evaluationContext = (StandardEvaluationContext) EvaluationContextProvider.DEFAULT
				.getEvaluationContext(args);

		ParameterBindingJsonReader reader = new ParameterBindingJsonReader(
				"{ 'isBatman' : ?#{ T(" + this.getClass().getName() + ").isBatman() ? '?0' : '?1' }}",
				new ParameterBindingContext((index) -> args[index], new SpelExpressionParser(), evaluationContext));
		Document target = new ParameterBindingDocumentCodec().decode(reader, DecoderContext.builder().build());

		assertThat(target).isEqualTo(new Document("isBatman", "nooo"));
	}

	@Test // DATAMONGO-2545
	public void shouldAllowMethodArgumentPlaceholdersInQuotedSpelExpressions/*because this worked before*/() {

		Object[] args = new Object[] { "yess", "nooo" };
		StandardEvaluationContext evaluationContext = (StandardEvaluationContext) EvaluationContextProvider.DEFAULT
				.getEvaluationContext(args);

		ParameterBindingJsonReader reader = new ParameterBindingJsonReader(
				"{ 'isBatman' : \"?#{ T(" + this.getClass().getName() + ").isBatman() ? '?0' : '?1' }\" }",
				new ParameterBindingContext((index) -> args[index], new SpelExpressionParser(), evaluationContext));
		Document target = new ParameterBindingDocumentCodec().decode(reader, DecoderContext.builder().build());

		assertThat(target).isEqualTo(new Document("isBatman", "nooo"));
	}

	@Test // DATAMONGO-2545
	public void evaluatesSpelExpressionDefiningEntireQuery() {

		Object[] args = new Object[] {};
		StandardEvaluationContext evaluationContext = (StandardEvaluationContext) EvaluationContextProvider.DEFAULT
				.getEvaluationContext(args);
		evaluationContext.setRootObject(new DummySecurityObject(new DummyWithId("wonderwoman")));

		String json = "?#{  T(" + this.getClass().getName()
				+ ").isBatman() ? {'_class': { '$eq' : 'region' }} : { '$and' : { {'_class': { '$eq' : 'region' } }, {'user.supervisor':  principal.id } } } }";

		ParameterBindingJsonReader reader = new ParameterBindingJsonReader(json,
				new ParameterBindingContext((index) -> args[index], new SpelExpressionParser(), evaluationContext));
		Document target = new ParameterBindingDocumentCodec().decode(reader, DecoderContext.builder().build());

		assertThat(target)
				.isEqualTo(new Document("$and", Arrays.asList(new Document("_class", new Document("$eq", "region")),
						new Document("user.supervisor", "wonderwoman"))));
	}

	@Test // DATAMONGO-2571
	public void shouldParseRegexCorrectly() {

		Document target = parse("{ $and: [{'fieldA': {$in: [/ABC.*/, /CDE.*F/]}}, {'fieldB': {$ne: null}}]}");
		assertThat(target)
				.isEqualTo(Document.parse("{ $and: [{'fieldA': {$in: [/ABC.*/, /CDE.*F/]}}, {'fieldB': {$ne: null}}]}"));
	}

	@Test // DATAMONGO-2571
	public void shouldParseRegexWithPlaceholderCorrectly() {

		Document target = parse("{ $and: [{'fieldA': {$in: [/?0.*/, /CDE.*F/]}}, {'fieldB': {$ne: null}}]}", "ABC");
		assertThat(target)
				.isEqualTo(Document.parse("{ $and: [{'fieldA': {$in: [/ABC.*/, /CDE.*F/]}}, {'fieldB': {$ne: null}}]}"));
	}

	@Test // DATAMONGO-2633
	public void shouldParseNestedArrays() {

		Document target = parse("{ 'stores.location' : { $geoWithin: { $centerSphere: [ [ ?0, 48.799029 ] , ?1 ] } } }",
				1.948516D, 0.004D);
		assertThat(target).isEqualTo(Document
				.parse("{ 'stores.location' : { $geoWithin: { $centerSphere: [ [ 1.948516, 48.799029 ] , 0.004 ] } } }"));
	}

	private static Document parse(String json, Object... args) {

		ParameterBindingJsonReader reader = new ParameterBindingJsonReader(json, args);
		return new ParameterBindingDocumentCodec().decode(reader, DecoderContext.builder().build());
	}

	// DATAMONGO-2545
	public static boolean isBatman() {
		return false;
	}

	@Data
	@AllArgsConstructor
	public static class DummySecurityObject {
		DummyWithId principal;
	}

	@Data
	@AllArgsConstructor
	public static class DummyWithId {
		String id;
	}

}
