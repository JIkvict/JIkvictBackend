package org.jikvict.jikvictbackend.annotation

import org.springframework.security.access.prepost.PreAuthorize
import java.lang.annotation.Inherited

@PreAuthorize("hasRole('TEACHER')")
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.TYPE)
@Retention(AnnotationRetention.RUNTIME)
@Inherited
annotation class OnlyTeacher
