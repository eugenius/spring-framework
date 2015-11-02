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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.springframework.http.client.ClientHttpRequest;
import org.springframework.util.Assert;

/**
 * The combined expectation of {@link RequestMatcher}'s and {@link CountExpectation}.
 *
 * @author Eugene Beschastnov
 * @since 4.3
 */
public class RequestExpectation implements ResponseActions {

	private final List<RequestMatcher> requestMatchers = new ArrayList<RequestMatcher>();

	private CountExpectation countExpectation = null;

	private ResponseCreator responseCreator;

	public RequestExpectation(RequestMatcher requestMatcher) {
		Assert.notNull(requestMatcher, "RequestMatcher is required");
		this.requestMatchers.add(requestMatcher);
	}

	@Override
	public ResponseActions andExpect(RequestMatcher requestMatcher) {
		Assert.notNull(requestMatcher, "RequestMatcher is required");
		this.requestMatchers.add(requestMatcher);
		return this;
	}

	@Override
	public ResponseActions andExpectIt(CountExpectation countExpectation) {
		Assert.notNull(countExpectation, "CountExpectation is required");
		if (this.countExpectation == null) {
			this.countExpectation = countExpectation;
		} else {
			this.countExpectation = this.countExpectation.and(countExpectation);
		}
		return this;
	}

	@Override
	public ResponseActions times(int times) {
		andExpectIt(CountExpectation.times(times));
		return this;
	}

	@Override
	public ResponseActions anyNumberOfTimes() {
		andExpectIt(CountExpectation.anyNumberOfTimes());
		return this;
	}

	CountExpectation getCountExpectation() {
		return this.countExpectation;
	}

	ResponseCreator getResponseCreator() {
		return responseCreator;
	}

	@Override
	public void andRespond(ResponseCreator responseCreator) {
		Assert.notNull(responseCreator, "ResponseCreator is required");
		this.responseCreator = responseCreator;
		if (this.countExpectation == null) {
			times(1);
		}
	}

	public void check(ClientHttpRequest request) throws IOException {
		if (this.requestMatchers.isEmpty()) {
			throw new AssertionError("No request expectations to execute");
		}

		if (this.responseCreator == null) {
			throw new AssertionError("No ResponseCreator was set up. Add it after request expectations, "
					+ "e.g. MockRestServiceServer.expect(requestTo(\"/foo\")).andRespond(withSuccess())");
		}

		if (!this.countExpectation.isMoreCallsAllowed()) {
			throw new AssertionError("No further requests expected: " + this);
		}

		for (RequestMatcher requestMatcher : this.requestMatchers) {
			requestMatcher.match(request);
		}
	}

	@Override
	public String toString() {
		return "RequestExpectation{" +
			   "requestMatchers=" + this.requestMatchers +
			   ", countExpectation=" + this.countExpectation +
			   '}';
	}
}
