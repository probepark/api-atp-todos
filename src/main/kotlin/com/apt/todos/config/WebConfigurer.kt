package com.apt.todos.config

import com.fasterxml.jackson.databind.ObjectMapper
import io.github.jhipster.config.JHipsterConstants
import io.github.jhipster.config.JHipsterProperties
import io.github.jhipster.web.filter.reactive.CachingHttpHeadersFilter
import java.util.concurrent.TimeUnit
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.web.reactive.ResourceHandlerRegistrationCustomizer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.core.annotation.Order
import org.springframework.web.cors.reactive.CorsWebFilter
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource
import org.springframework.web.reactive.config.ResourceHandlerRegistry
import org.springframework.web.reactive.config.WebFluxConfigurer
import org.springframework.web.reactive.result.method.annotation.ArgumentResolverConfigurer
import org.springframework.web.server.WebExceptionHandler
import org.zalando.problem.spring.webflux.advice.ProblemExceptionHandler
import org.zalando.problem.spring.webflux.advice.ProblemHandling

/**
 * Configuration of web application with Servlet 3.0 APIs.
 */
@Configuration
class WebConfigurer(
    private val jHipsterProperties: JHipsterProperties
) : WebFluxConfigurer {

    private val log = LoggerFactory.getLogger(javaClass)

    @Bean
    fun corsFilter(): CorsWebFilter {
        val source = UrlBasedCorsConfigurationSource()
        val config = jHipsterProperties.cors
        if (config.allowedOrigins != null && config.allowedOrigins!!.isNotEmpty()) {
            log.debug("Registering CORS filter")
            source.apply {
                registerCorsConfiguration("/api/**", config)
                registerCorsConfiguration("/management/**", config)
                registerCorsConfiguration("/v2/api-docs", config)
            }
        }
        return CorsWebFilter(source)
    }

    // TODO: remove when this is supported in spring-data / spring-boot
    override fun configureArgumentResolvers(configurer: ArgumentResolverConfigurer) {
        configurer.addCustomResolver(ReactiveSortHandlerMethodArgumentResolver(),
            ReactivePageableHandlerMethodArgumentResolver())
    }

    @Bean
    @Order(-2) // The handler must have precedence over WebFluxResponseStatusExceptionHandler and Spring Boot's ErrorWebExceptionHandler
    fun problemExceptionHandler(mapper: ObjectMapper, problemHandling: ProblemHandling): WebExceptionHandler {
        return ProblemExceptionHandler(mapper, problemHandling)
    }

    @Bean
    fun registrationCustomizer(): ResourceHandlerRegistrationCustomizer {
        // Disable built-in cache control to use our custom filter instead
        return ResourceHandlerRegistrationCustomizer { registration -> registration.setCacheControl(null) }
    }

    @Bean
    @Profile(JHipsterConstants.SPRING_PROFILE_PRODUCTION)
    fun cachingHttpHeadersFilter(): CachingHttpHeadersFilter {
        // Use a cache filter that only match selected paths
        return CachingHttpHeadersFilter(TimeUnit.DAYS.toMillis(jHipsterProperties.http.cache.timeToLiveInDays.toLong()))
    }

    override fun addResourceHandlers(registry: ResourceHandlerRegistry) {
        registry.addResourceHandler("/swagger-ui.html**")
            .addResourceLocations("classpath:/META-INF/resources/")

        registry.addResourceHandler("/webjars/**")
            .addResourceLocations("classpath:/META-INF/resources/webjars/")
    }
}
