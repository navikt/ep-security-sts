package no.nav.eessi.pensjon.security.sts

import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.HttpMethod
import org.springframework.http.ResponseEntity
import org.springframework.http.client.support.BasicAuthenticationInterceptor
import org.springframework.web.client.RestTemplate

@Disabled("Kun for testing lokalt")
internal class STSServiceIT {

    // Use your preferred localhost port with Kube Forwarder
    private val stsBaseUrl = "http://localhost:8088/rest/v1/sts/token"

    private val wellKnownRestMock = mockk<RestTemplate>()

    private val securityExchangeRestTemplate = createSecurityExchangeRestTemplate()

    private val stsService = spyk(STSService(securityExchangeRestTemplate, wellKnownRestMock))

    @BeforeEach
    fun beforeEach() {
        every {
            wellKnownRestMock.exchange(
                any<String>(),
                HttpMethod.GET,
                null,
                any<ParameterizedTypeReference<WellKnownSTS>>()
            )
        } returns ResponseEntity.ok(WellKnownSTS("issuer", stsBaseUrl, "exchangeTokenEndpoint", "jwksUri", emptyList()))

        stsService.discoveryUrl = ""
        stsService.discoverEndpoints()
    }

    @Test
    fun getSystemOidcToken() {
        val token = stsService.getSystemOidcToken()

        println(token)

        assertNotNull(token)
    }

    private fun createSecurityExchangeRestTemplate(): RestTemplate {
        return RestTemplateBuilder()
            .additionalInterceptors(
                BasicAuthenticationInterceptor(
                    System.getenv("SRV_USER"),
                    System.getenv("SRV_PWD")
                )
            )
            .build()
    }

}
