package com.apt.todos.config.apidoc

import io.github.jhipster.config.JHipsterConstants
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Profile
import springfox.documentation.builders.PathSelectors
import springfox.documentation.builders.RequestHandlerSelectors
import springfox.documentation.spi.DocumentationType
import springfox.documentation.spring.web.plugins.Docket
import springfox.documentation.swagger2.annotations.EnableSwagger2WebFlux

@Primary
@Profile(JHipsterConstants.SPRING_PROFILE_SWAGGER)
@Configuration
@EnableSwagger2WebFlux
class SwaggerConfiguration {

    @Bean
    fun api() = Docket(DocumentationType.SWAGGER_2)
            .select()
            .apis(RequestHandlerSelectors.any())
            .paths(PathSelectors.any())
            .build()
}
