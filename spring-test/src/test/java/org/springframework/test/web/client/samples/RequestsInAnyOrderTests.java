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

package org.springframework.test.web.client.samples;

import org.junit.Before;
import org.junit.Test;

import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import static org.junit.Assert.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

/**
 * Tests for requests to {@link MockRestServiceServer} which can be called in any order.
 *
 * @author Eugene Beschastnov
 */
public class RequestsInAnyOrderTests {

	private MockRestServiceServer mockServer;

	private RestTemplate restTemplate;

	@Before
	public void setup() {
		this.restTemplate = new RestTemplate();
		this.mockServer = MockRestServiceServer.createServer(this.restTemplate)
				.withRequestsInAnyOrder()
				.explainMoreOnError();
	}

	@Test
	public void simpleRequests() throws Exception {
		this.mockServer.expect(requestTo("/number/1")).andExpect(method(HttpMethod.GET))
				.andRespond(withSuccess("1", MediaType.TEXT_PLAIN));

		this.mockServer.expect(requestTo("/number/2")).andExpect(method(HttpMethod.GET))
				.andRespond(withSuccess("2", MediaType.TEXT_PLAIN));

		this.mockServer.expect(requestTo("/number/3")).andExpect(method(HttpMethod.GET))
				.andRespond(withSuccess("3", MediaType.TEXT_PLAIN));

		assertEquals("2", this.restTemplate.getForObject("/number/2", String.class));
		assertEquals("1", this.restTemplate.getForObject("/number/1", String.class));
		assertEquals("3", this.restTemplate.getForObject("/number/3", String.class));

		mockServer.verify();
	}

	@Test
	public void complexRequests() throws Exception {
		this.mockServer.expect(requestTo("/number/1")).andExpect(method(HttpMethod.GET))
				.anyNumberOfTimes()
				.andRespond(withSuccess("1", MediaType.TEXT_PLAIN));

		this.mockServer.expect(requestTo("/number/2")).andExpect(method(HttpMethod.GET))
				.times(2)
				.andRespond(withSuccess("2", MediaType.TEXT_PLAIN));

		this.mockServer.expect(requestTo("/number/3")).andExpect(method(HttpMethod.GET))
				.andRespond(withSuccess("3", MediaType.TEXT_PLAIN));

		assertEquals("1", this.restTemplate.getForObject("/number/1", String.class));
		assertEquals("2", this.restTemplate.getForObject("/number/2", String.class));
		assertEquals("1", this.restTemplate.getForObject("/number/1", String.class));
		assertEquals("2", this.restTemplate.getForObject("/number/2", String.class));
		assertEquals("3", this.restTemplate.getForObject("/number/3", String.class));
		assertEquals("1", this.restTemplate.getForObject("/number/1", String.class));

		mockServer.verify();
	}
}
