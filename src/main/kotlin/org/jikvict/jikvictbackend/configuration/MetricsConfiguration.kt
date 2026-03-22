package org.jikvict.jikvictbackend.configuration

import io.micrometer.core.instrument.Gauge
import io.micrometer.core.instrument.MeterRegistry
import org.jikvict.jikvictbackend.model.response.PendingStatus
import org.jikvict.jikvictbackend.repository.AssignmentRepository
import org.jikvict.jikvictbackend.repository.AssignmentResultRepository
import org.jikvict.jikvictbackend.repository.TaskStatusRepository
import org.jikvict.jikvictbackend.repository.UserRepository
import org.springframework.context.annotation.Configuration
import jakarta.annotation.PostConstruct

@Configuration
class MetricsConfiguration(
    private val meterRegistry: MeterRegistry,
    private val taskStatusRepository: TaskStatusRepository,
    private val assignmentRepository: AssignmentRepository,
    private val assignmentResultRepository: AssignmentResultRepository,
    private val userRepository: UserRepository,
) {
    @PostConstruct
    fun registerGauges() {
        // Queue depth: tasks currently pending
        Gauge.builder("jikvict.tasks.pending") {
            taskStatusRepository.findAllByStatus(PendingStatus.PENDING).size.toDouble()
        }
            .description("Number of solution verification tasks currently pending in the queue")
            .register(meterRegistry)

        // Total submitted results
        Gauge.builder("jikvict.submissions.total") {
            assignmentResultRepository.count().toDouble()
        }
            .description("Total number of solution submissions stored in the system")
            .register(meterRegistry)

        // Total registered users
        Gauge.builder("jikvict.users.total") {
            userRepository.count().toDouble()
        }
            .description("Total number of registered users")
            .register(meterRegistry)

        // Active (non-closed) assignments
        Gauge.builder("jikvict.assignments.total") {
            assignmentRepository.count().toDouble()
        }
            .description("Total number of assignments")
            .register(meterRegistry)
    }
}
