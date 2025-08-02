package org.jikvict.jikvictbackend.configuration

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import org.hibernate.cfg.AvailableSettings
import org.hibernate.type.format.jackson.JacksonJsonFormatMapper
import org.jikvict.testing.model.TestSuiteResult
import org.springframework.boot.autoconfigure.orm.jpa.HibernatePropertiesCustomizer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import jakarta.annotation.PostConstruct

@Configuration
class JacksonConfiguration(
    private val objectMapper: ObjectMapper,
) {
    abstract class TestSuiteResultMixin {
        @Suppress("unused")
        @get:JsonIgnore
        abstract val percentageEarned: Double
    }

    @PostConstruct
    fun customObjectMapper() {
        objectMapper
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .addMixIn(TestSuiteResult::class.java, TestSuiteResultMixin::class.java)
    }

    @Bean
    fun jsonFormatMapperCustomizer(objectMapper: ObjectMapper?): HibernatePropertiesCustomizer =
        HibernatePropertiesCustomizer { properties ->
            properties[AvailableSettings.JSON_FORMAT_MAPPER] = JacksonJsonFormatMapper(objectMapper)
        }
}
