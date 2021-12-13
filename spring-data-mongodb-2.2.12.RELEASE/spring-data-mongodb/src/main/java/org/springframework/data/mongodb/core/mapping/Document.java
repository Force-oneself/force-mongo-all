/*
 * Copyright 2011-2020 the original author or authors.
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
package org.springframework.data.mongodb.core.mapping;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.core.annotation.AliasFor;
import org.springframework.data.annotation.Persistent;

/**
 * Identifies a domain object to be persisted to MongoDB.
 *
 * @author Jon Brisbin
 * @author Oliver Gierke
 * @author Christoph Strobl
 */
@Persistent
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE })
public @interface Document {

	/**
	 * 表示实体的文档应该存储在的集合。如果未配置，默认集合名称将从类型名称派生。
	 * 该属性支持 SpEL 表达式以基于每个操作动态计算集合.
	 * 
	 * @return the name of the collection to be used.
	 */
	@AliasFor("collection")
	String value() default "";

	/**
	 * 表示实体的文档应该存储在的集合。如果未配置，默认集合名称将从类型名称派生。
	 * 该属性支持 SpEL 表达式以基于每个操作动态计算集合.
	 * 
	 * @return the name of the collection to be used.
	 */
	@AliasFor("value")
	String collection() default "";

	/**
	 * 定义要与此文档一起使用的默认语言。
	 *
	 * @return an empty String by default.
	 * @since 1.6
	 */
	String language() default "";

	/**
	 * 定义在执行查询或创建索引时应用的​​排序规则.
	 *
	 * @return an empty {@link String} by default.
	 * @since 2.2
	 */
	String collation() default "";

}
