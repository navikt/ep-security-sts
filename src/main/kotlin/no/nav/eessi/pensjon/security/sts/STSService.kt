package no.nav.eessi.pensjon.security.sts

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.HttpMethod
import org.springframework.web.util.UriComponentsBuilder
import javax.annotation.PostConstruct
import no.nav.eessi.pensjon.metrics.MetricsHelper
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import org.springframework.web.client.*
import java.lang.RuntimeException


inline fun <reified T : Any> typeRef(): ParameterizedTypeReference<T> = object : ParameterizedTypeReference<T>() {}

class SecurityTokenResponse(
        @JsonProperty("access_token")
        val accessToken: String,
        @JsonProperty("token_type")
        val tokenType: String,
        @JsonProperty("expires_in")
        val expiresIn: Long
)

class WellKnownSTS(
        @JsonProperty("issuer")
        val issuer: String,
        @JsonProperty("token_endpoint")
        val tokenEndpoint: String,
        @JsonProperty("exchange_token_endpoint")
        val exchangeTokenEndpoint: String,
        @JsonProperty("jwks_uri")
        val jwksUri: String,
        @JsonProperty("subject_types_supported")
        val subjectTypesSupported: List<String>
)
/**
 * Denne STS tjenesten benyttes ved kall mot nye REST tjenester sånn som Aktørregisteret
 *
 * @param metricsHelper Usually injected by Spring Boot, can be set manually in tests - no way to read metrics if not set.
 */
@Component
class STSService(
        private val securityTokenExchangeBasicAuthRestTemplate: RestTemplate,
        private val wellKnownStsRestTemplate: RestTemplate = RestTemplate(),
        @Autowired(required = false) private val metricsHelper: MetricsHelper = MetricsHelper(SimpleMeterRegistry())
) {

    private val logger = LoggerFactory.getLogger(STSService::class.java)

    @Value("\${securityTokenService.discoveryUrl}")
    lateinit var discoveryUrl: String

    lateinit var wellKnownSTS: WellKnownSTS

    private lateinit var disoverSTS: MetricsHelper.Metric
    private lateinit var getSystemOidcToken: MetricsHelper.Metric

    @PostConstruct
    fun initMetrics() {
        disoverSTS = metricsHelper.init("disoverSTS")
        getSystemOidcToken = metricsHelper.init("getSystemOidcToken")

        disoverSTS.measure {
            try {
                logger.info("Henter STS endepunkter fra well.known " + discoveryUrl)
                    wellKnownSTS = wellKnownStsRestTemplate.exchange(discoveryUrl,
                            HttpMethod.GET,
                            null,
                            typeRef<WellKnownSTS>()).body!!
            } catch(ex: HttpStatusCodeException) {
                logger.error("En feil oppstod under service discovery av STS ex: $ex body: ${ex.responseBodyAsString}")
                throw RuntimeException("En feil oppstod under service discovery av STS ex: ${ex.message} body: ${ex.responseBodyAsString}")
            } catch(ex: Exception) {
                logger.error("En feil oppstod under service discovery av STS ex: $ex")
                throw RuntimeException("En feil oppstod under service discovery av STS ex: ${ex.message}")
            }
        }
    }

    fun getSystemOidcToken(): String {
        return getSystemOidcToken.measure {
            try {
                val uri = UriComponentsBuilder.fromUriString(wellKnownSTS.tokenEndpoint)
                        .queryParam("grant_type", "client_credentials")
                        .queryParam("scope", "openid")
                        .build().toUriString()

                logger.info("Kaller STS for å bytte username/password til OIDC token")
                val responseEntity = securityTokenExchangeBasicAuthRestTemplate.exchange(uri,
                        HttpMethod.GET,
                        null,
                        typeRef<SecurityTokenResponse>())

                logger.debug("SecurityTokenResponse ${mapAnyToJson(responseEntity)} ")
                responseEntity.body!!.accessToken
            } catch(ex: HttpStatusCodeException) {
                logger.error("En feil oppstod under bytting av username/password til OIDC token ex: $ex body: ${ex.responseBodyAsString}")
                throw RuntimeException("En feil oppstod under bytting av username/password til OIDC token ex: ${ex.message} body: ${ex.responseBodyAsString}")
            } catch(ex: Exception) {
                logger.error("En feil oppstod under bytting av username/password til OIDC token ex: $ex")
                throw RuntimeException("En feil oppstod under bytting av username/password til OIDC token ex: ${ex.message}")
            }
        }
    }

    private fun mapAnyToJson(data: Any): String {
        return jacksonObjectMapper()
                .writerWithDefaultPrettyPrinter()
                .writeValueAsString(data)
    }

}