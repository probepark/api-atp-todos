package com.apt.todos.config

import com.github.mongobee.Mongobee
import io.github.jhipster.config.JHipsterConstants
import io.github.jhipster.domain.util.JSR310DateConverters.DateToZonedDateTimeConverter
import io.github.jhipster.domain.util.JSR310DateConverters.DurationToLongConverter
import io.github.jhipster.domain.util.JSR310DateConverters.ZonedDateTimeToDateConverter
import org.slf4j.LoggerFactory
import org.springframework.cloud.Cloud
import org.springframework.cloud.CloudException
import org.springframework.cloud.config.java.AbstractCloudConfig
import org.springframework.cloud.service.common.MongoServiceInfo
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.core.convert.converter.Converter
import org.springframework.data.mongodb.MongoDbFactory
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.convert.MongoCustomConversions
import org.springframework.data.mongodb.core.mapping.event.ValidatingMongoEventListener
import org.springframework.data.mongodb.repository.config.EnableReactiveMongoRepositories
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean

@Configuration
@EnableReactiveMongoRepositories("com.apt.todos.repository")
@Profile(JHipsterConstants.SPRING_PROFILE_CLOUD)
class CloudDatabaseConfiguration : AbstractCloudConfig() {

    private val log = LoggerFactory.getLogger(javaClass)

    @Bean
    fun mongoFactory(): MongoDbFactory = connectionFactory().mongoDbFactory()

    @Bean
    fun validator() = LocalValidatorFactoryBean()

    @Bean
    fun validatingMongoEventListener() = ValidatingMongoEventListener(validator())

    @Bean
    fun customConversions() =
        MongoCustomConversions(
            mutableListOf<Converter<*, *>>(
                DateToZonedDateTimeConverter.INSTANCE,
                ZonedDateTimeToDateConverter.INSTANCE,
                DurationToLongConverter.INSTANCE
            )
        )

    @Bean
    fun mongobee(mongoDbFactory: MongoDbFactory, mongoTemplate: MongoTemplate, cloud: Cloud): Mongobee {
        log.debug("Configuring Cloud Mongobee")
        val matchingServiceInfos = cloud.getServiceInfos(MongoDbFactory::class.java)

        if (matchingServiceInfos.size != 1) {
            throw CloudException("No unique service matching MongoDbFactory found. Expected 1, found $matchingServiceInfos.size")
        }
        val info = matchingServiceInfos[0] as MongoServiceInfo
        return Mongobee(info.uri).apply {
            setDbName(mongoDbFactory.db.name)
            setMongoTemplate(mongoTemplate)
            // package to scan for migrations
            setChangeLogsScanPackage("com.apt.todos.config.dbmigrations")
            isEnabled = true
        }
    }
}
