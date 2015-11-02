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

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import static org.junit.Assert.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

/**
 * Tests for repeatable requests to {@link MockRestServiceServer} which reuses same
 * expectations.
 *
 * @author Eugene Beschastnov
 */
public class RepeatableRequestTests {

	private MockRestServiceServer mockServer;

	private RestTemplate restTemplate;

	@Before
	public void setup() {
		this.restTemplate = new RestTemplate();
		this.mockServer = MockRestServiceServer.createServer(this.restTemplate);
	}

	@Test
	public void repeatableRequestsWithSingleOnes() {

		this.mockServer.expect(requestTo("/number")).andExpect(method(HttpMethod.GET))
				.times(3)
				.andRespond(withSuccess("1", MediaType.TEXT_PLAIN));

		this.mockServer.expect(requestTo("/number")).andExpect(method(HttpMethod.GET))
				.andRespond(withSuccess("2", MediaType.TEXT_PLAIN));

		this.mockServer.expect(requestTo("/number")).andExpect(method(HttpMethod.GET))
				.times(2)
				.andRespond(withSuccess("4", MediaType.TEXT_PLAIN));

		for (int i = 0; i < 3; i++) {
			assertEquals("1", this.restTemplate.getForObject("/number", String.class));
		}

		assertEquals("2", this.restTemplate.getForObject("/number", String.class));

		for (int i = 0; i < 2; i++) {
			assertEquals("4", this.restTemplate.getForObject("/number", String.class));
		}

		mockServer.verify();
	}

	@Test
	public void verify() {

		this.mockServer.expect(requestTo("/number")).andExpect(method(HttpMethod.GET))
				.times(5)
				.andRespond(withSuccess("1", MediaType.TEXT_PLAIN));

		this.mockServer.expect(requestTo("/number")).andExpect(method(HttpMethod.GET))
				.andRespond(withSuccess("2", MediaType.TEXT_PLAIN));

		this.mockServer.expect(requestTo("/number")).andExpect(method(HttpMethod.GET))
				.times(2)
				.andRespond(withSuccess("4", MediaType.TEXT_PLAIN));

		this.restTemplate.getForObject("/number", String.class);
		this.restTemplate.getForObject("/number", String.class);

		try {
			this.mockServer.verify();
		}
		catch (AssertionError error) {
			assertTrue(error.getMessage(), error.getMessage().contains("2 out of 8 were executed"));
		}
	}

	@Test
	public void repeatableAnyNumberOfTimesWithFollowingRegularRequest() {

		this.mockServer.expect(requestTo("/number")).andExpect(method(HttpMethod.GET))
				.anyNumberOfTimes()
				.andRespond(withSuccess("1", MediaType.TEXT_PLAIN));

		this.mockServer.expect(requestTo("/number/2")).andExpect(method(HttpMethod.GET))
				.andRespond(withSuccess("2", MediaType.TEXT_PLAIN));

		for (int i = 0; i < 10; i++) {
			assertEquals("1", this.restTemplate.getForObject("/number", String.class));
		}

		assertEquals("2", this.restTemplate.getForObject("/number/2", String.class));

		mockServer.verify();
	}

	@Test
	public void skipRepeatableAnyNumberOfTimes() {

		this.mockServer.expect(requestTo("/number")).andExpect(method(HttpMethod.GET))
				.anyNumberOfTimes()
				.andRespond(withSuccess("1", MediaType.TEXT_PLAIN));

		this.mockServer.expect(requestTo("/number/2")).andExpect(method(HttpMethod.GET))
				.anyNumberOfTimes()
				.andRespond(withSuccess("2", MediaType.TEXT_PLAIN));

		this.mockServer.expect(requestTo("/number/4")).andExpect(method(HttpMethod.GET))
				.andRespond(withSuccess("4", MediaType.TEXT_PLAIN));

		assertEquals("4", this.restTemplate.getForObject("/number/4", String.class));

		mockServer.verify();
	}

	@Test
	public void verifyWithRepeatableAnyNumberOfTimes() {

		this.mockServer.expect(requestTo("/number")).andExpect(method(HttpMethod.GET))
				.andRespond(withSuccess("1", MediaType.TEXT_PLAIN));

		this.mockServer.expect(requestTo("/number")).andExpect(method(HttpMethod.GET))
				.andRespond(withSuccess("2", MediaType.TEXT_PLAIN));

		this.mockServer.expect(requestTo("/number")).andExpect(method(HttpMethod.GET))
				.anyNumberOfTimes()
				.andRespond(withSuccess("4", MediaType.TEXT_PLAIN));

		this.mockServer.expect(requestTo("/number")).andExpect(method(HttpMethod.GET))
				.anyNumberOfTimes()
				.andRespond(withSuccess("8", MediaType.TEXT_PLAIN));

		this.restTemplate.getForObject("/number", String.class);

		try {
			this.mockServer.verify();
		}
		catch (AssertionError error) {
			assertTrue(error.getMessage(), error.getMessage().contains("1 out of 2+ were executed"));
		}
	}

	@Test
	public void anyNumberOfTimesWithHeader() {

		this.mockServer.expect(requestTo("/number"))
				.andExpect(method(HttpMethod.GET)).andExpect(header("Header", "value"))
				.anyNumberOfTimes()
				.andRespond(withSuccess("4", MediaType.TEXT_PLAIN));

		HttpHeaders headers = new HttpHeaders();
		headers.add("Header", "value");

		this.restTemplate.exchange("/number", HttpMethod.GET, new HttpEntity<>(headers), String.class);

		this.mockServer.verify();
	}
}
