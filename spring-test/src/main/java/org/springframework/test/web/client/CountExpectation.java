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

/**
 * The expectation of how many times request may be called.
 *
 * @author Eugene Beschastnov
 * @since 4.3
 */
public class CountExpectation {

	private int calls = 0;
	private int atLeast = 0;
	private Integer atMost = null; // null is infinity here

	/**
	 * Require request to be called at least {@code times}.
	 * @return the expectation
	 */
	public static CountExpectation atLeast(int times) {
		CountExpectation expectation = new CountExpectation();
		expectation.atLeast = times;
		return expectation;
	}

	/**
	 * Require request to be called at most {@code times}.
	 * @return the expectation
	 */
	public static CountExpectation atMost(int times) {
		CountExpectation expectation = new CountExpectation();
		expectation.atMost = times;
		return expectation;
	}

	/**
	 * Require request to be called at least once.
	 * @return the expectation
	 */
	public static CountExpectation atLeastOnce() {
		return atLeast(1);
	}

	/**
	 * Require request to be never called.
	 * @return the expectation
	 */
	public static CountExpectation never() {
		return atMost(0);
	}

	/**
	 * Allow request to be called any number of times.
	 * @return the expectation
	 */
	public static CountExpectation anyNumberOfTimes() {
		return atLeast(0);
	}

	/**
	 * Specify the amount of times request expected.
	 * @return the expectation
	 */
	public static CountExpectation times(int times) {
		CountExpectation expectation = new CountExpectation();
		expectation.atLeast = times;
		expectation.atMost = times;
		return expectation;
	}

	private CountExpectation() {}

	/**
	 * Check if more calls allowed with this expectation.
	 * @return {@code true} if more calls allowed
	 */
	public boolean isMoreCallsAllowed() {
		return this.atMost == null || this.atMost > this.calls;
	}

	/**
	 * Check if more calls required with this expectation.
	 * @return {@code true} if more calls required
	 */
	public boolean isMoreCallsRequired() {
		return this.atLeast > this.calls;
	}

	/**
	 * Check if the amount of allowed calls is more than the amount of required ones,
	 * i.e. if the number of expected calls is not strict.
	 * @return {@code true} if extra calls allowed
	 */
	public boolean isExtraCallsAllowed() {
		return this.atMost == null || this.atMost > this.atLeast;
	}

	/**
	 * Count the new call.
	 */
	public void countNewCall() {
		if (!isMoreCallsAllowed()) {
			throw new AssertionError("No more calls expected: " + this);
		}
		calls++;
	}

	/**
	 * @return the amount of remaining required calls
	 */
	public int getAmountOfRequiredCallsLeft() {
		return Integer.max(0, this.atLeast - this.calls);
	}

	/**
	 * Combine expectations as {@code and}.
	 * @return the combined expectation
	 */
	public CountExpectation and(CountExpectation other) {
		CountExpectation expectation = new CountExpectation();
		expectation.atLeast = Integer.max(this.atLeast, other.atLeast);

		if (this.atMost == null) {
			expectation.atMost = other.atMost;
		} else if (other.atMost == null) {
			expectation.atMost = this.atMost;
		} else {
			expectation.atMost = Integer.min(this.atMost, other.atMost);
		}

		return expectation;
	}

	/**
	 * Combine expectations as {@code or}.
	 * @return the combined expectation
	 */
	public CountExpectation or(CountExpectation other) {
		CountExpectation expectation = new CountExpectation();
		expectation.atLeast = Integer.min(this.atLeast, other.atLeast);

		if (this.atMost == null || other.atMost == null) {
			expectation.atMost = null;
		} else {
			expectation.atMost = Integer.max(this.atMost, other.atMost);
		}

		return expectation;
	}

	@Override
	public String toString() {
		return "CountExpectation{atLeast=" + atLeast +", atMost=" + (atMost == null ? "INF" : atMost) + "}";
	}
}
