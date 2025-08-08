package org.jikvict.docker.util

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.attribute.PosixFilePermission

fun Path.grantAllPermissions() {
    Files.setPosixFilePermissions(
        this,
        setOf(
            PosixFilePermission.OWNER_READ,
            PosixFilePermission.OWNER_WRITE,
            PosixFilePermission.OWNER_EXECUTE,
            PosixFilePermission.GROUP_READ,
            PosixFilePermission.GROUP_EXECUTE,
            PosixFilePermission.OTHERS_READ,
            PosixFilePermission.OTHERS_EXECUTE,
        ),
    )
}
