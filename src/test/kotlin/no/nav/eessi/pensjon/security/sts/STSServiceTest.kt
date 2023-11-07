package no.nav.eessi.pensjon.security.sts

import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.verify
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.RestTemplate

@ExtendWith(MockKExtension::class)
class STSServiceTest {

    @MockK
    private lateinit var stsRestTemplate: RestTemplate

    @MockK
    private lateinit var wellknownStsRestTemplate: RestTemplate

    private lateinit var stsService: STSService

    @BeforeEach
    fun oppStart() {
        val wellKnownResponse = WellKnownSTS(
                "issuer",
                "token",
                "endpoint",
                "jwks",
                listOf("support"))

        every {
            wellknownStsRestTemplate.exchange(
                any<String>(),
                HttpMethod.GET,
                null,
                any<ParameterizedTypeReference<WellKnownSTS>>()
            )
        } returns ResponseEntity.ok().body(wellKnownResponse)

        stsService = STSService(stsRestTemplate, wellknownStsRestTemplate, "bogus")
        stsService.discoveryUrl = "http://bogus"
    }

    @AfterEach
    fun afterEach() {
        verify(exactly = 1) {
            wellknownStsRestTemplate.exchange(
                any<String>(),
                HttpMethod.GET,
                null,
                any<ParameterizedTypeReference<WellKnownSTS>>()
            )
        }

        verify(exactly = 1) {
            stsRestTemplate.getForObject(any<String>(), SecurityTokenResponse::class.java)
        }

        confirmVerified(wellknownStsRestTemplate, stsRestTemplate)
    }

    @Test
    fun getSystemOidcToken_withValidToken() {
        val mockSecurityTokenResponse = SecurityTokenResponse(
                accessToken = "LKUITDKUo96tyfhj",
                tokenType = "sts",
                expiresIn = 10L
        )

        every {
            stsRestTemplate.getForObject(any<String>(), SecurityTokenResponse::class.java)
        } returns mockSecurityTokenResponse

        val result = stsService.getSystemOidcToken()

        assertEquals(mockSecurityTokenResponse.accessToken, result)
    }

    @Test
    fun getSystemOidcToken_withError() {
        every {
            stsRestTemplate.getForObject(any<String>(), SecurityTokenResponse::class.java)
        } throws HttpClientErrorException(HttpStatus.BAD_REQUEST)

        assertThrows<RuntimeException> {
            stsService.getSystemOidcToken()
        }
    }

}
