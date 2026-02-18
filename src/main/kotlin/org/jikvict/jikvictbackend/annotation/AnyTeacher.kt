package org.jikvict.jikvictbackend.annotation

import org.springframework.security.access.prepost.PreAuthorize
import java.lang.annotation.Inherited

@PreAuthorize("hasAnyRole('ADMIN', 'TEACHER', 'TEACHER_READ_ONLY')")
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.TYPE)
@Retention(AnnotationRetention.RUNTIME)
@Inherited
annotation class AnyTeacher
