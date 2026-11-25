/*
 * Copyright 2002-present the original author or authors.
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

package org.springframework.beans;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import org.springframework.beans.testfixture.beans.TestBean;
import org.springframework.util.AutoPopulatingList;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Specific {@link DirectFieldAccessor} tests.
 *
 * @author Jose Luis Martin
 * @author Chris Beams
 * @author Stephane Nicoll
 */
class DirectFieldAccessorTests extends AbstractPropertyAccessorTests {

	@Override
	protected DirectFieldAccessor createAccessor(Object target) {
		return new DirectFieldAccessor(target);
	}


	@Test
	void withShadowedField() {
		final StringBuilder sb = new StringBuilder();

		TestBean target = new TestBean() {
			@SuppressWarnings("unused")
			StringBuilder name = sb;
		};

		DirectFieldAccessor dfa = createAccessor(target);
		assertThat(dfa.getPropertyType("name")).isEqualTo(StringBuilder.class);
		assertThat(dfa.getPropertyValue("name")).isEqualTo(sb);
	}

	@Test
	void autoGrowSelfPopulatingRawListWithinDefaultLimit() {
		RawListHolder target = new RawListHolder();
		DirectFieldAccessor dfa = createAccessor(target);
		dfa.setAutoGrowNestedPaths(true);
		dfa.setPropertyValue("items[255].name", "test");

		List<?> items = target.items;
		assertThat(items).hasSize(256).element(255)
				.isInstanceOfSatisfying(TestBean.class, bean -> assertThat(bean.getName()).isEqualTo("test"));
	}

	@Test
	void autoGrowSelfPopulatingRawListBeyondDefaultLimit() {
		RawListHolder target = new RawListHolder();
		DirectFieldAccessor dfa = createAccessor(target);
		dfa.setAutoGrowNestedPaths(true);
		assertThatExceptionOfType(InvalidPropertyException.class)
				.isThrownBy(() -> dfa.getPropertyValue("items[256]"))
				.withMessageContaining("Cannot get element with index 256 from List of size 0");
		assertThatExceptionOfType(InvalidPropertyException.class)
				.isThrownBy(() -> dfa.setPropertyValue("items[256].name", "test"))
				.withMessageContaining("Cannot get element with index 256 from List of size 0");
		assertThat(target.items).isEmpty();
	}

	@Test
	void autoGrowSelfPopulatingRawListBeyondDefaultLimitForNestedAccessor() {
		RawListHolderHolder target = new RawListHolderHolder();
		DirectFieldAccessor dfa = createAccessor(target);
		dfa.setAutoGrowNestedPaths(true);
		assertThatExceptionOfType(InvalidPropertyException.class)
				.isThrownBy(() -> dfa.setPropertyValue("holder.items[256].name", "test"))
				.withMessageContaining("Cannot get element with index 256 from List of size 0");
		assertThat(target.holder.items).isEmpty();
	}

	@Test
	void autoGrowSelfPopulatingRawListBeyondCustomLimit() {
		RawListHolder target = new RawListHolder();
		DirectFieldAccessor dfa = createAccessor(target);
		dfa.setAutoGrowNestedPaths(true);
		dfa.setAutoGrowCollectionLimit(300);
		dfa.setPropertyValue("items[256].name", "test");
		assertThat(target.items).hasSize(257);
		assertThatExceptionOfType(InvalidPropertyException.class)
				.isThrownBy(() -> dfa.setPropertyValue("items[300].name", "test"))
				.withMessageContaining("Cannot get element with index 300 from List of size 257");
		assertThat(target.items).hasSize(257);
	}


	@Test
	void autoGrowTypedListWithinDefaultLimit() {
		TypedListHolder target = new TypedListHolder();
		DirectFieldAccessor dfa = createAccessor(target);
		dfa.setAutoGrowNestedPaths(true);
		dfa.setPropertyValue("items[255].name", "test");
		assertThat(target.items).hasSize(256).element(255)
				.satisfies(bean -> assertThat(bean.getName()).isEqualTo("test"));
	}

	@Test
	void autoGrowTypedListBeyondDefaultLimit() {
		TypedListHolder target = new TypedListHolder();
		DirectFieldAccessor dfa = createAccessor(target);
		dfa.setAutoGrowNestedPaths(true);
		assertThatExceptionOfType(InvalidPropertyException.class)
				.isThrownBy(() -> dfa.setPropertyValue("items[256].name", "test"))
				.withMessageContaining("Cannot get element with index 256 from List of size 0");
		assertThat(target.items).isEmpty();
	}


	@SuppressWarnings("rawtypes")
	private static class RawListHolder {

		private List items = new AutoPopulatingList<>(TestBean.class);
	}


	private static class RawListHolderHolder {

		private RawListHolder holder = new RawListHolder();
	}


	private static class TypedListHolder {

		private List<TestBean> items = new ArrayList<>();
	}

}
