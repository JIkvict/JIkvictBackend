package org.jikvict.docker.consumer

import org.testcontainers.containers.output.OutputFrame
import java.nio.file.Path
import java.util.function.Consumer

fun interface MountedFilesConsumer : Consumer<Collection<Path>>
fun interface ExitCodeConsumer : Consumer<Int>
fun interface LogsConsumer : Consumer<OutputFrame>
