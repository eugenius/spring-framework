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
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import org.springframework.http.HttpMethod;
import org.springframework.http.client.AsyncClientHttpRequest;
import org.springframework.http.client.AsyncClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.mock.http.client.MockAsyncClientHttpRequest;
import org.springframework.test.web.client.match.MockRestRequestMatchers;
import org.springframework.test.web.client.response.MockRestResponseCreators;
import org.springframework.util.Assert;
import org.springframework.web.client.AsyncRestTemplate;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.support.RestGatewaySupport;

/**
 * <strong>Main entry point for client-side REST testing</strong>. Used for tests
 * that involve direct or indirect (through client code) use of the
 * {@link RestTemplate}. Provides a way to set up fine-grained expectations
 * on the requests that will be performed through the {@code RestTemplate} and
 * a way to define the responses to send back removing the need for an
 * actual running server.
 *
 * <p>Below is an example:
 * <pre class="code">
 * import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
 * import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
 * import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
 *
 * ...
 *
 * RestTemplate restTemplate = new RestTemplate()
 * MockRestServiceServer mockServer = MockRestServiceServer.createServer(restTemplate);
 *
 * mockServer.expect(requestTo("/hotels/42")).andExpect(method(HttpMethod.GET))
 *     .andRespond(withSuccess("{ \"id\" : \"42\", \"name\" : \"Holiday Inn\"}", MediaType.APPLICATION_JSON));
 *
 * Hotel hotel = restTemplate.getForObject("/hotels/{id}", Hotel.class, 42);
 * &#47;&#47; Use the hotel instance...
 *
 * mockServer.verify();
 * </pre>
 *
 * <p>To create an instance of this class, use {@link #createServer(RestTemplate)}
 * and provide the {@code RestTemplate} to set up for the mock testing.
 *
 * <p>After that use {@link #expect(RequestMatcher)} and fluent API methods
 * {@link ResponseActions#andExpect(RequestMatcher) andExpect(RequestMatcher)} and
 * {@link ResponseActions#andRespond(ResponseCreator) andRespond(ResponseCreator)}
 * to set up request expectations and responses, most likely relying on the default
 * {@code RequestMatcher} implementations provided in {@link MockRestRequestMatchers}
 * and the {@code ResponseCreator} implementations provided in
 * {@link MockRestResponseCreators} both of which can be statically imported.
 *
 * <p>At the end of the test use {@link #verify()} to ensure all expected
 * requests were actually performed.
 *
 * <p>Note that because of the fluent API offered by this class (and related
 * classes), you can typically use the Code Completion features (i.e.
 * ctrl-space) in your IDE to set up the mocks.
 *
 * <p><strong>Credits:</strong> The client-side REST testing support was
 * inspired by and initially based on similar code in the Spring WS project for
 * client-side tests involving the {@code WebServiceTemplate}.
 *
 * @author Craig Walls
 * @author Rossen Stoyanchev
 * @author Eugene Beschastnov
 * @since 3.2
 */
public class MockRestServiceServer {

	private final List<RequestExpectation> expectations = new ArrayList<RequestExpectation>();

 	private final List<ClientHttpRequest> actualRequests = new ArrayList<ClientHttpRequest>();

	private boolean inAnyOrder = false;

	private boolean explainMoreOnError = false;


	/**
	 * Private constructor.
	 * @see #createServer(RestTemplate)
	 * @see #createServer(RestGatewaySupport)
	 */
	private MockRestServiceServer() {
	}


	/**
	 * Create a {@code MockRestServiceServer} and set up the given
	 * {@code RestTemplate} with a mock {@link ClientHttpRequestFactory}.
	 * @param restTemplate the RestTemplate to set up for mock testing
	 * @return the created mock server
	 */
	public static MockRestServiceServer createServer(RestTemplate restTemplate) {
		Assert.notNull(restTemplate, "'restTemplate' must not be null");
		MockRestServiceServer mockServer = new MockRestServiceServer();
		RequestMatcherClientHttpRequestFactory factory = mockServer.new RequestMatcherClientHttpRequestFactory();
		restTemplate.setRequestFactory(factory);
		return mockServer;
	}

	/**
	 * Create a {@code MockRestServiceServer} and set up the given
	 * {@code AsyRestTemplate} with a mock {@link AsyncClientHttpRequestFactory}.
	 * @param asyncRestTemplate the AsyncRestTemplate to set up for mock testing
	 * @return the created mock server
	 */
	public static MockRestServiceServer createServer(AsyncRestTemplate asyncRestTemplate) {
		Assert.notNull(asyncRestTemplate, "'asyncRestTemplate' must not be null");
		MockRestServiceServer mockServer = new MockRestServiceServer();
		RequestMatcherClientHttpRequestFactory factory = mockServer.new RequestMatcherClientHttpRequestFactory();
		asyncRestTemplate.setAsyncRequestFactory(factory);
		return mockServer;
	}

	/**
	 * Create a {@code MockRestServiceServer} and set up the given
	 * {@code RestGatewaySupport} with a mock {@link ClientHttpRequestFactory}.
	 * @param restGateway the REST gateway to set up for mock testing
	 * @return the created mock server
	 */
	public static MockRestServiceServer createServer(RestGatewaySupport restGateway) {
		Assert.notNull(restGateway, "'gatewaySupport' must not be null");
		return createServer(restGateway.getRestTemplate());
	}

	public MockRestServiceServer withRequestsInAnyOrder() {
		this.inAnyOrder = true;
		return this;
	}

	public MockRestServiceServer explainMoreOnError() {
		this.explainMoreOnError = true;
		return this;
	}

	/**
	 * Set up a new HTTP request expectation. The returned {@link ResponseActions}
	 * is used to set up further expectations and to define the response.
	 * <p>This method may be invoked multiple times before starting the test, i.e. before
	 * using the {@code RestTemplate}, to set up expectations for multiple requests.
	 * @param requestMatcher a request expectation, see {@link MockRestRequestMatchers}
	 * @return used to set up further expectations or to define a response
	 */
	public ResponseActions expect(RequestMatcher requestMatcher) {
		Assert.state(this.actualRequests.isEmpty(),
				"Can't add more expected requests with test already underway");
		RequestExpectation request = new RequestExpectation(requestMatcher);
		this.expectations.add(request);
		return request;
	}

	/**
	 * Verify that all expected requests set up via
	 * {@link #expect(RequestMatcher)} were indeed performed.
	 * @throws AssertionError when some expectations were not met
	 */
	public void verify() {
		if (this.expectations.isEmpty() || onlySkippableExpectationsLeft()) {
			return;
		}
		throw new AssertionError(getVerifyMessage());
	}

	private boolean onlySkippableExpectationsLeft() {
		for (RequestExpectation expectation : this.expectations) {
			if (expectation.getCountExpectation().isMoreCallsRequired()) {
				return false;
			}
		}
		return true;
	}

	private String getVerifyMessage() {
		return "Further request(s) expected\n" + getRequestsInfo();
	}

	private String getRequestsInfo() {
		StringBuilder sb = new StringBuilder();
		if (this.actualRequests.size() > 0) {
			sb.append("The following ");
		}
		sb.append(this.actualRequests.size()).append(" out of ");
		sb.append(this.actualRequests.size() + getAmountOfRemainingRequiredCalls());
		if (isExtraCallsAllowed()) {
				sb.append("+");
		}

		sb.append(" were executed\nActual:");
		if (this.actualRequests.size() > 0) {
			sb.append("\n");
			for (ClientHttpRequest request : this.actualRequests) {
				sb.append(request.toString()).append("\n");
			}
		} else {
			sb.append(" none\n");
		}

		sb.append("More expected:");
		if (this.expectations.size() > 0) {
			sb.append("\n");
			for (RequestExpectation expectation : this.expectations) {
				sb.append(expectation.toString()).append("\n");
			}
		} else {
			sb.append(" none\n");
		}

		return sb.toString();
	}

	private boolean isExtraCallsAllowed() {
		for (RequestExpectation expectation : expectations) {
			if (expectation.getCountExpectation().isExtraCallsAllowed()) {
				return true;
			}
		}
		return false;
	}

	private int getAmountOfRemainingRequiredCalls() {
		int requiredCalls = 0;
		for (RequestExpectation expectation : this.expectations) {
			CountExpectation countExpectation = expectation.getCountExpectation();
			requiredCalls += countExpectation.getAmountOfRequiredCallsLeft();
		}
		return requiredCalls;
	}

	/**
	 * Mock ClientHttpRequestFactory that creates {@link RequestMatcherClientHttpRequest}'s.
	 */
	private class RequestMatcherClientHttpRequestFactory
			implements ClientHttpRequestFactory, AsyncClientHttpRequestFactory {

		@Override
		public ClientHttpRequest createRequest(URI uri, HttpMethod httpMethod) throws IOException {
			return new RequestMatcherClientHttpRequest(uri, httpMethod);
		}

		@Override
		public AsyncClientHttpRequest createAsyncRequest(URI uri, HttpMethod httpMethod) throws IOException {
			return new RequestMatcherClientHttpRequest(uri, httpMethod);
		}
	}

	/**
	 * A specialization of {@code MockClientHttpRequest} that matches the request
	 * against a set of expectations, via {@link RequestExpectation} instances. The
	 * expectations are checked when the request is executed. This class also uses a
	 * {@link ResponseCreator} to create the response.
	 */
	private class RequestMatcherClientHttpRequest extends MockAsyncClientHttpRequest {

		public RequestMatcherClientHttpRequest(URI uri, HttpMethod httpMethod) {
			super(httpMethod, uri);
		}

		@Override
		public ClientHttpResponse executeInternal() throws IOException {
			Assert.notNull(getURI(), "'uri' must not be null");
			Assert.notNull(getMethod(), "'httpMethod' must not be null");

			RequestExpectation expectation = matchExpectation();

			if (expectation == null) {
				throw new AssertionError("No further requests expected: HTTP " + getMethod() + " " + getURI() +
										 (explainMoreOnError ? "\n" + getRequestsInfo() : ""));
			}

			CountExpectation countExpectation = expectation.getCountExpectation();
			countExpectation.countNewCall();
			if (!countExpectation.isMoreCallsAllowed()) {
				MockRestServiceServer.this.expectations.remove(expectation);
			}

			MockRestServiceServer.this.actualRequests.add(this);
			setResponse(expectation.getResponseCreator().createResponse(this));
			return super.executeInternal();
		}

		private RequestExpectation matchExpectation() throws IOException {
			RequestExpectation expectation = null;

			if (!inAnyOrder) {
				if (!MockRestServiceServer.this.expectations.isEmpty()) {
					expectation = MockRestServiceServer.this.expectations.get(0);
					try {
						expectation.check(this);
					} catch (AssertionError error) {
						if (expectation.getCountExpectation().isMoreCallsRequired()) {
							throw error;
						} else {
							MockRestServiceServer.this.expectations.remove(expectation);
							return matchExpectation();
						}
					}
				}
			} else {
				for (RequestExpectation expectationToCheck : MockRestServiceServer.this.expectations) {
					try {
						expectationToCheck.check(this);
						expectation = expectationToCheck;
					} catch (AssertionError error) {
						continue;
					}
					break;
				}
			}
			return expectation;
		}
	}

}
