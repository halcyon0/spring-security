/*
 * Copyright 2002-2018 the original author or authors.
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
package org.springframework.security.oauth2.jwt;

import java.util.Arrays;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jwt.JWT;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.JWTParser;
import com.nimbusds.jwt.SignedJWT;
import com.nimbusds.jwt.proc.DefaultJWTProcessor;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import org.springframework.http.RequestEntity;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jose.jws.JwsAlgorithms;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.AssertionsForClassTypes.assertThatCode;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.spy;
import static org.powermock.api.mockito.PowerMockito.when;
import static org.powermock.api.mockito.PowerMockito.whenNew;

/**
 * Tests for {@link NimbusJwtDecoderJwkSupport}.
 *
 * @author Joe Grandja
 * @author Josh Cummings
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({NimbusJwtDecoderJwkSupport.class, JWTParser.class})
@PowerMockIgnore("okhttp3.*")
public class NimbusJwtDecoderJwkSupportTests {
	private static final String JWK_SET_URL = "https://provider.com/oauth2/keys";
	private static final String JWS_ALGORITHM = JwsAlgorithms.RS256;

	private static final String JWK_SET = "{\"keys\":[{\"p\":\"49neceJFs8R6n7WamRGy45F5Tv0YM-R2ODK3eSBUSLOSH2tAqjEVKOkLE5fiNA3ygqq15NcKRadB2pTVf-Yb5ZIBuKzko8bzYIkIqYhSh_FAdEEr0vHF5fq_yWSvc6swsOJGqvBEtuqtJY027u-G2gAQasCQdhyejer68zsTn8M\",\"kty\":\"RSA\",\"q\":\"tWR-ysspjZ73B6p2vVRVyHwP3KQWL5KEQcdgcmMOE_P_cPs98vZJfLhxobXVmvzuEWBpRSiqiuyKlQnpstKt94Cy77iO8m8ISfF3C9VyLWXi9HUGAJb99irWABFl3sNDff5K2ODQ8CmuXLYM25OwN3ikbrhEJozlXg_NJFSGD4E\",\"d\":\"FkZHYZlw5KSoqQ1i2RA2kCUygSUOf1OqMt3uomtXuUmqKBm_bY7PCOhmwbvbn4xZYEeHuTR8Xix-0KpHe3NKyWrtRjkq1T_un49_1LLVUhJ0dL-9_x0xRquVjhl_XrsRXaGMEHs8G9pLTvXQ1uST585gxIfmCe0sxPZLvwoic-bXf64UZ9BGRV3lFexWJQqCZp2S21HfoU7wiz6kfLRNi-K4xiVNB1gswm_8o5lRuY7zB9bRARQ3TS2G4eW7p5sxT3CgsGiQD3_wPugU8iDplqAjgJ5ofNJXZezoj0t6JMB_qOpbrmAM1EnomIPebSLW7Ky9SugEd6KMdL5lW6AuAQ\",\"e\":\"AQAB\",\"use\":\"sig\",\"kid\":\"one\",\"qi\":\"wdkFu_tV2V1l_PWUUimG516Zvhqk2SWDw1F7uNDD-Lvrv_WNRIJVzuffZ8WYiPy8VvYQPJUrT2EXL8P0ocqwlaSTuXctrORcbjwgxDQDLsiZE0C23HYzgi0cofbScsJdhcBg7d07LAf7cdJWG0YVl1FkMCsxUlZ2wTwHfKWf-v4\",\"dp\":\"uwnPxqC-IxG4r33-SIT02kZC1IqC4aY7PWq0nePiDEQMQWpjjNH50rlq9EyLzbtdRdIouo-jyQXB01K15-XXJJ60dwrGLYNVqfsTd0eGqD1scYJGHUWG9IDgCsxyEnuG3s0AwbW2UolWVSsU2xMZGb9PurIUZECeD1XDZwMp2s0\",\"dq\":\"hra786AunB8TF35h8PpROzPoE9VJJMuLrc6Esm8eZXMwopf0yhxfN2FEAvUoTpLJu93-UH6DKenCgi16gnQ0_zt1qNNIVoRfg4rw_rjmsxCYHTVL3-RDeC8X_7TsEySxW0EgFTHh-nr6I6CQrAJjPM88T35KHtdFATZ7BCBB8AE\",\"n\":\"oXJ8OyOv_eRnce4akdanR4KYRfnC2zLV4uYNQpcFn6oHL0dj7D6kxQmsXoYgJV8ZVDn71KGmuLvolxsDncc2UrhyMBY6DVQVgMSVYaPCTgW76iYEKGgzTEw5IBRQL9w3SRJWd3VJTZZQjkXef48Ocz06PGF3lhbz4t5UEZtdF4rIe7u-977QwHuh7yRPBQ3sII-cVoOUMgaXB9SHcGF2iZCtPzL_IffDUcfhLQteGebhW8A6eUHgpD5A1PQ-JCw_G7UOzZAjjDjtNM2eqm8j-Ms_gqnm4MiCZ4E-9pDN77CAAPVN7kuX6ejs9KBXpk01z48i9fORYk9u7rAkh1HuQw\"}]}";
	private static final String MALFORMED_JWK_SET = "malformed";

	private static final String SIGNED_JWT = "eyJhbGciOiJSUzI1NiJ9.eyJzdWIiOiJ0ZXN0LXN1YmplY3QiLCJzY3AiOlsibWVzc2FnZTpyZWFkIl0sImV4cCI6NDY4Mzg5Nzc3Nn0.LtMVtIiRIwSyc3aX35Zl0JVwLTcQZAB3dyBOMHNaHCKUljwMrf20a_gT79LfhjDzE_fUVUmFiAO32W1vFnYpZSVaMDUgeIOIOpxfoe9shj_uYenAwIS-_UxqGVIJiJoXNZh_MK80ShNpvsQwamxWEEOAMBtpWNiVYNDMdfgho9n3o5_Z7Gjy8RLBo1tbDREbO9kTFwGIxm_EYpezmRCRq4w1DdS6UDW321hkwMxPnCMSWOvp-hRpmgY2yjzLgPJ6Aucmg9TJ8jloAP1DjJoF1gRR7NTAk8LOGkSjTzVYDYMbCF51YdpojhItSk80YzXiEsv1mTz4oMM49jXBmfXFMA";
	private static final String MALFORMED_JWT = "eyJhbGciOiJSUzI1NiJ9.eyJuYmYiOnt9LCJleHAiOjQ2ODQyMjUwODd9.guoQvujdWvd3xw7FYQEn4D6-gzM_WqFvXdmvAUNSLbxG7fv2_LLCNujPdrBHJoYPbOwS1BGNxIKQWS1tylvqzmr1RohQ-RZ2iAM1HYQzboUlkoMkcd8ENM__ELqho8aNYBfqwkNdUOyBFoy7Syu_w2SoJADw2RTjnesKO6CVVa05bW118pDS4xWxqC4s7fnBjmZoTn4uQ-Kt9YSQZQk8YQxkJSiyanozzgyfgXULA6mPu1pTNU3FVFaK1i1av_xtH_zAPgb647ZeaNe4nahgqC5h8nhOlm8W2dndXbwAt29nd2ZWBsru_QwZz83XSKLhTPFz-mPBByZZDsyBbIHf9A";
	private static final String UNSIGNED_JWT = "eyJhbGciOiJub25lIiwidHlwIjoiSldUIn0.eyJleHAiOi0yMDMzMjI0OTcsImp0aSI6IjEyMyIsInR5cCI6IkpXVCJ9.";

	private NimbusJwtDecoderJwkSupport jwtDecoder = new NimbusJwtDecoderJwkSupport(JWK_SET_URL, JWS_ALGORITHM);

	@Test
	public void constructorWhenJwkSetUrlIsNullThenThrowIllegalArgumentException() {
		assertThatThrownBy(() -> new NimbusJwtDecoderJwkSupport(null))
				.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	public void constructorWhenJwkSetUrlInvalidThenThrowIllegalArgumentException() {
		assertThatThrownBy(() -> new NimbusJwtDecoderJwkSupport("invalid.com"))
				.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	public void constructorWhenJwsAlgorithmIsNullThenThrowIllegalArgumentException() {
		assertThatThrownBy(() -> new NimbusJwtDecoderJwkSupport(JWK_SET_URL, null))
				.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	public void setRestOperationsWhenNullThenThrowIllegalArgumentException() {
		Assertions.assertThatThrownBy(() -> this.jwtDecoder.setRestOperations(null))
				.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	public void decodeWhenJwtInvalidThenThrowJwtException() {
		assertThatThrownBy(() -> this.jwtDecoder.decode("invalid"))
				.isInstanceOf(JwtException.class);
	}

	// gh-5168
	@Test
	public void decodeWhenExpClaimNullThenDoesNotThrowException() throws Exception {
		SignedJWT jwt = mock(SignedJWT.class);
		JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.parse(JWS_ALGORITHM)).build();
		when(jwt.getHeader()).thenReturn(header);

		mockStatic(JWTParser.class);
		when(JWTParser.parse(anyString())).thenReturn(jwt);

		DefaultJWTProcessor jwtProcessor = mock(DefaultJWTProcessor.class);
		whenNew(DefaultJWTProcessor.class).withAnyArguments().thenReturn(jwtProcessor);

		JWTClaimsSet jwtClaimsSet = new JWTClaimsSet.Builder().audience("resource1").build();
		when(jwtProcessor.process(any(JWT.class), eq(null))).thenReturn(jwtClaimsSet);

		NimbusJwtDecoderJwkSupport jwtDecoder = new NimbusJwtDecoderJwkSupport(JWK_SET_URL);
		assertThatCode(() -> jwtDecoder.decode("encoded-jwt")).doesNotThrowAnyException();
	}

	// gh-5457
	@Test
	public void decodeWhenPlainJwtThenExceptionDoesNotMentionClass() {
		assertThatCode(() -> this.jwtDecoder.decode(UNSIGNED_JWT))
				.isInstanceOf(JwtException.class)
				.hasMessageContaining("Unsupported algorithm of none");
	}

	@Test
	public void decodeWhenJwtIsMalformedThenReturnsStockException() throws Exception {
		try ( MockWebServer server = new MockWebServer() ) {
			server.enqueue(new MockResponse().setBody(JWK_SET));
			String jwkSetUrl = server.url("/.well-known/jwks.json").toString();
			NimbusJwtDecoderJwkSupport jwtDecoder = new NimbusJwtDecoderJwkSupport(jwkSetUrl);
			assertThatCode(() -> jwtDecoder.decode(MALFORMED_JWT))
					.isInstanceOf(JwtException.class)
					.hasMessage("An error occurred while attempting to decode the Jwt: Malformed payload");
			server.shutdown();
		}
	}

	@Test
	public void decodeWhenJwkResponseIsMalformedThenReturnsStockException() throws Exception {
		try ( MockWebServer server = new MockWebServer() ) {
			server.enqueue(new MockResponse().setBody(MALFORMED_JWK_SET));
			String jwkSetUrl = server.url("/.well-known/jwks.json").toString();
			NimbusJwtDecoderJwkSupport jwtDecoder = new NimbusJwtDecoderJwkSupport(jwkSetUrl);
			assertThatCode(() -> jwtDecoder.decode(SIGNED_JWT))
					.isInstanceOf(JwtException.class)
					.hasMessage("An error occurred while attempting to decode the Jwt: Malformed Jwk set");
			server.shutdown();
		}
	}

	@Test
	public void decodeWhenJwkEndpointIsUnresponsiveThenReturnsJwtException() throws Exception {
		try ( MockWebServer server = new MockWebServer() ) {
			server.enqueue(new MockResponse().setBody(MALFORMED_JWK_SET));
			String jwkSetUrl = server.url("/.well-known/jwks.json").toString();
			NimbusJwtDecoderJwkSupport jwtDecoder = new NimbusJwtDecoderJwkSupport(jwkSetUrl);
			assertThatCode(() -> jwtDecoder.decode(SIGNED_JWT))
					.isInstanceOf(JwtException.class)
					.hasMessageContaining("An error occurred while attempting to decode the Jwt");
			server.shutdown();
		}
	}

	// gh-5603
	@Test
	public void decodeWhenCustomRestOperationsSetThenUsed() throws Exception {
		try ( MockWebServer server = new MockWebServer() ) {
			server.enqueue(new MockResponse().setBody(JWK_SET));
			String jwkSetUrl = server.url("/.well-known/jwks.json").toString();
			NimbusJwtDecoderJwkSupport jwtDecoder = new NimbusJwtDecoderJwkSupport(jwkSetUrl);
			RestTemplate restTemplate = spy(new RestTemplate());
			jwtDecoder.setRestOperations(restTemplate);
			assertThatCode(() -> jwtDecoder.decode(SIGNED_JWT)).doesNotThrowAnyException();
			verify(restTemplate).exchange(any(RequestEntity.class), eq(String.class));
			server.shutdown();
		}
	}

	@Test
	public void decodeWhenJwtFailsValidationThenReturnsCorrespondingErrorMessage() throws Exception {
		try ( MockWebServer server = new MockWebServer() ) {
			server.enqueue(new MockResponse().setBody(JWK_SET));
			String jwkSetUrl = server.url("/.well-known/jwks.json").toString();

			NimbusJwtDecoderJwkSupport decoder = new NimbusJwtDecoderJwkSupport(jwkSetUrl);

			OAuth2Error failure = new OAuth2Error("mock-error", "mock-description", "mock-uri");

			OAuth2TokenValidator<Jwt> jwtValidator = mock(OAuth2TokenValidator.class);
			when(jwtValidator.validate(any(Jwt.class))).thenReturn(OAuth2TokenValidatorResult.failure(failure));
			decoder.setJwtValidator(jwtValidator);

			assertThatCode(() -> decoder.decode(SIGNED_JWT))
					.isInstanceOf(JwtValidationException.class)
					.hasMessageContaining("mock-description");
		}
	}

	@Test
	public void decodeWhenJwtValidationHasTwoErrorsThenJwtExceptionMessageShowsFirstError() throws Exception {
		try ( MockWebServer server = new MockWebServer() ) {
			server.enqueue(new MockResponse().setBody(JWK_SET));
			String jwkSetUrl = server.url("/.well-known/jwks.json").toString();

			NimbusJwtDecoderJwkSupport decoder = new NimbusJwtDecoderJwkSupport(jwkSetUrl);

			OAuth2Error firstFailure = new OAuth2Error("mock-error", "mock-description", "mock-uri");
			OAuth2Error secondFailure = new OAuth2Error("another-error", "another-description", "another-uri");
			OAuth2TokenValidatorResult result = OAuth2TokenValidatorResult.failure(firstFailure, secondFailure);

			OAuth2TokenValidator<Jwt> jwtValidator = mock(OAuth2TokenValidator.class);
			when(jwtValidator.validate(any(Jwt.class))).thenReturn(result);
			decoder.setJwtValidator(jwtValidator);

			assertThatCode(() -> decoder.decode(SIGNED_JWT))
					.isInstanceOf(JwtValidationException.class)
					.hasMessageContaining("mock-description")
					.hasFieldOrPropertyWithValue("errors", Arrays.asList(firstFailure, secondFailure));
		}
	}
}
