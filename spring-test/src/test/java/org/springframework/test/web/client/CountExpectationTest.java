/*
 * Copyright 2002-2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.test.web.client;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Tests for {@link CountExpectation}.
 *
 * @author Eugene Beschastnov
 */
public class CountExpectationTest {

	@Test
	public void testTimes() throws Exception {
		CountExpectation expectation = CountExpectation.times(3);

		assertTrue(expectation.isMoreCallsAllowed());
		assertTrue(expectation.isMoreCallsRequired());
		assertFalse(expectation.isExtraCallsAllowed());
		assertEquals(3, expectation.getAmountOfRequiredCallsLeft());

		for (int i = 0; i < 3; i++) {
			expectation.countNewCall();
		}

		assertFalse(expectation.isMoreCallsAllowed());
		assertFalse(expectation.isMoreCallsRequired());
		assertFalse(expectation.isExtraCallsAllowed());
		assertEquals(0, expectation.getAmountOfRequiredCallsLeft());

		try {
			expectation.countNewCall();
		} catch (AssertionError error) {
			assertTrue(error.getMessage(), error.getMessage().contains("No more calls expected"));
		}
	}

	@Test
	public void testAtLeast() throws Exception {
		CountExpectation expectation = CountExpectation.atLeast(3);

		assertTrue(expectation.isMoreCallsAllowed());
		assertTrue(expectation.isMoreCallsRequired());
		assertTrue(expectation.isExtraCallsAllowed());
		assertEquals(3, expectation.getAmountOfRequiredCallsLeft());

		for (int i = 0; i < 3; i++) {
			expectation.countNewCall();
		}

		assertTrue(expectation.isMoreCallsAllowed());
		assertFalse(expectation.isMoreCallsRequired());
		assertTrue(expectation.isExtraCallsAllowed());
		assertEquals(0, expectation.getAmountOfRequiredCallsLeft());

		expectation.countNewCall();
	}

	@Test
	public void testAtLeastOnce() throws Exception {
		CountExpectation expectation = CountExpectation.atLeastOnce();

		assertTrue(expectation.isMoreCallsAllowed());
		assertTrue(expectation.isMoreCallsRequired());
		assertTrue(expectation.isExtraCallsAllowed());
		assertEquals(1, expectation.getAmountOfRequiredCallsLeft());
	}

	@Test
	public void testAtMost() throws Exception {
		CountExpectation expectation = CountExpectation.atMost(3);

		assertTrue(expectation.isMoreCallsAllowed());
		assertFalse(expectation.isMoreCallsRequired());
		assertTrue(expectation.isExtraCallsAllowed());
		assertEquals(0, expectation.getAmountOfRequiredCallsLeft());

		for (int i = 0; i < 3; i++) {
			expectation.countNewCall();
		}

		assertFalse(expectation.isMoreCallsAllowed());
		assertFalse(expectation.isMoreCallsRequired());
		assertTrue(expectation.isExtraCallsAllowed());
		assertEquals(0, expectation.getAmountOfRequiredCallsLeft());

		try {
			expectation.countNewCall();
		} catch (AssertionError error) {
			assertTrue(error.getMessage(), error.getMessage().contains("No more calls expected"));
		}
	}

	@Test
	public void testNever() throws Exception {
		CountExpectation expectation = CountExpectation.never();

		assertFalse(expectation.isMoreCallsAllowed());
		assertFalse(expectation.isMoreCallsRequired());
		assertFalse(expectation.isExtraCallsAllowed());
		assertEquals(0, expectation.getAmountOfRequiredCallsLeft());

		try {
			expectation.countNewCall();
		} catch (AssertionError error) {
			assertTrue(error.getMessage(), error.getMessage().contains("No more calls expected"));
		}
	}

	@Test
	public void testAnyNumberOfTimes() throws Exception {
		CountExpectation expectation = CountExpectation.anyNumberOfTimes();

		assertTrue(expectation.isMoreCallsAllowed());
		assertFalse(expectation.isMoreCallsRequired());
		assertTrue(expectation.isExtraCallsAllowed());
		assertEquals(0, expectation.getAmountOfRequiredCallsLeft());

		for (int i = 0; i < 10; i++) {
			expectation.countNewCall();
		}

		assertTrue(expectation.isMoreCallsAllowed());
		assertFalse(expectation.isMoreCallsRequired());
		assertTrue(expectation.isExtraCallsAllowed());
		assertEquals(0, expectation.getAmountOfRequiredCallsLeft());
	}
}
