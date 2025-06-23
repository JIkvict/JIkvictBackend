package org.jikvict.jikvictbackend.configuration

import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import org.springframework.beans.factory.InjectionPoint
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Scope

@Configuration
class CljLoggingAutoConfiguration {
    @Bean
    fun cljLoggerFactory(): LoggerFactory = LoggerFactory()

    @Bean
    @Primary
    @Scope("prototype")
    @Qualifier("logger")
    fun logger(
        loggerFactory: LoggerFactory,
        injectionPoint: InjectionPoint,
    ): Logger =
        loggerFactory.createLogger(
            injectionPoint.targetingBean(),
        )

    @Bean
    @Scope("prototype")
    @Qualifier("cljLogger")
    fun cljLogger(
        loggerFactory: LoggerFactory,
        injectionPoint: InjectionPoint,
    ): Logger =
        loggerFactory.createLogger(
            injectionPoint.targetingBean(),
        )
}

private const val SETTER_PREFIX = "set"

private fun InjectionPoint.targetingBean(): Class<*> {
    val method = this.methodParameter?.method
    val isSetter = method?.name?.startsWith(SETTER_PREFIX) ?: false

    return if (method == null || isSetter) {
        this.member.declaringClass
    } else {
        method.returnType
    }
}

class LoggerFactory {
    fun createLogger(clazz: Class<*>): Logger = LogManager.getLogger(clazz)
}
