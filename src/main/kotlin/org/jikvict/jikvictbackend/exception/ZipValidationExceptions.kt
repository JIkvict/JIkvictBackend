package org.jikvict.jikvictbackend.exception

/**
 * Base exception for all ZIP validation failures
 */
sealed class ZipValidationException(
    message: String,
) : RuntimeException(message)

/**
 * Exception thrown when the file size exceeds the maximum allowed size
 */
class FileSizeExceededException(
    message: String,
) : ZipValidationException(message)

/**
 * Exception thrown when the file type is not allowed
 */
class FileTypeNotAllowedException(
    message: String,
) : ZipValidationException(message)

/**
 * Exception thrown when the ZIP file structure is invalid
 */
class InvalidZipStructureException(
    message: String,
) : ZipValidationException(message)

/**
 * Exception thrown when a path traversal attempt is detected in a ZIP entry
 */
class PathTraversalAttemptException(
    message: String,
) : ZipValidationException(message)

/**
 * Exception thrown when an entry size exceeds the maximum allowed size
 */
class EntrySizeExceededException(
    message: String,
) : ZipValidationException(message)

/**
 * Exception thrown when a suspicious file extension is detected
 */
class SuspiciousFileExtensionException(
    message: String,
) : ZipValidationException(message)

/**
 * Exception thrown when a suspicious compression ratio is detected
 */
class SuspiciousCompressionRatioException(
    message: String,
) : ZipValidationException(message)
