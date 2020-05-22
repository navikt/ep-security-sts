package no.nav.eessi.pensjon.security.sts

import com.nhaarman.mockitokotlin2.doThrow
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.whenever
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentMatchers
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.RestTemplate
import java.lang.RuntimeException

@ExtendWith(MockitoExtension::class)
class STSServiceTest {

    @Mock
    private lateinit var stsRestTemplate: RestTemplate

    @Mock
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
        val re = ResponseEntity.ok().body(wellKnownResponse)

        whenever(wellknownStsRestTemplate.exchange(
                ArgumentMatchers.anyString(),
                eq(HttpMethod.GET),
                eq(null),
                eq(typeRef<WellKnownSTS>()))
        ).thenReturn(re)


        stsService = STSService(stsRestTemplate, wellknownStsRestTemplate)
        stsService.discoveryUrl = "http://bogus"
        stsService.initMetrics()
    }

    @Test
    fun getSystemOidcToken_withValidToken() {

        val mockSecurityTokenResponse = SecurityTokenResponse(
                accessToken = "LKUITDKUo96tyfhj",
                tokenType = "sts",
                expiresIn = 10L
        )

        val response = ResponseEntity.ok().body(mockSecurityTokenResponse)

        whenever(stsRestTemplate.exchange(
                ArgumentMatchers.anyString(),
                eq(HttpMethod.GET),
                eq(null),
                eq(typeRef<SecurityTokenResponse>()))
        ).thenReturn(response)

        val result = stsService.getSystemOidcToken()

        assertEquals(mockSecurityTokenResponse.accessToken, result)
    }

    @Test
    fun getSystemOidcToken_withError() {

        whenever(stsRestTemplate.exchange(
                ArgumentMatchers.anyString(),
                eq(HttpMethod.GET),
                eq(null),
                eq(typeRef<SecurityTokenResponse>()))
        ).doThrow(HttpClientErrorException(HttpStatus.BAD_REQUEST))

        assertThrows<RuntimeException> {
            stsService.getSystemOidcToken()
        }
    }

}
