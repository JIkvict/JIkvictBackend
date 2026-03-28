package org.jikvict.jikvictbackend.annotation

import org.springframework.security.access.prepost.PreAuthorize
import java.lang.annotation.Inherited

@PreAuthorize("hasAnyRole('ADMIN', 'TEACHER') and !hasRole('TEACHER_READ_ONLY')")
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.TYPE, AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@Inherited
annotation class RWTeacher
