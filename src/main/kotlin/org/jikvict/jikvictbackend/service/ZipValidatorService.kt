package org.jikvict.jikvictbackend.service

import org.apache.logging.log4j.Logger
import org.jikvict.jikvictbackend.model.properties.SolutionsProperties
import org.jikvict.jikvictbackend.exception.EntrySizeExceededException
import org.jikvict.jikvictbackend.exception.FileSizeExceededException
import org.jikvict.jikvictbackend.exception.FileTypeNotAllowedException
import org.jikvict.jikvictbackend.exception.InvalidZipStructureException
import org.jikvict.jikvictbackend.exception.PathTraversalAttemptException
import org.jikvict.jikvictbackend.exception.SuspiciousCompressionRatioException
import org.jikvict.jikvictbackend.exception.SuspiciousFileExtensionException
import org.jikvict.jikvictbackend.exception.ZipValidationException
import org.springframework.stereotype.Service
import org.springframework.validation.annotation.Validated
import org.springframework.web.multipart.MultipartFile
import java.io.ByteArrayInputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty

@Service
@Validated
class ZipValidatorService(
    private val solutionsProperties: SolutionsProperties,
    private val logger: Logger,
) {
    fun validateZipArchive(file: MultipartFile) {
        validateFileSize(file)
        validateFileType(file)
        validateZipStructure(file)
        logger.info("ZIP validation completed successfully")
    }

    private fun validateFileSize(file: MultipartFile) {
        val maxSize = parseSize(solutionsProperties.maxFileSize)
        logger.debug("Validating file size: ${file.size} bytes against limit of $maxSize bytes")

        require(file.size <= maxSize) {
            logger.warn("File size validation failed: ${file.size} bytes exceeds limit of $maxSize bytes")
            throw FileSizeExceededException("ZIP file exceeds maximum allowed size of ${solutionsProperties.maxFileSize}")
        }

        logger.debug("File size validation passed")
    }

    private fun validateFileType(file: MultipartFile) {
        val fileExtension = file.originalFilename?.substringAfterLast('.', "")

        require(fileExtension in solutionsProperties.allowedFileTypes) {
            logger.warn("File type validation failed: .$fileExtension is not in allowed types: ${solutionsProperties.allowedFileTypes}")
            throw FileTypeNotAllowedException("File type .$fileExtension is not allowed. Allowed types: ${solutionsProperties.allowedFileTypes}")
        }

        logger.debug("File type validation passed")
    }

    private fun validateZipStructure(file: MultipartFile) {
        try {
            require(file.bytes.size >= 4 && isZipFileSignatureValid(file.bytes)) {
                logger.warn("Invalid ZIP file signature detected")
                throw InvalidZipStructureException("Invalid ZIP file: File signature does not match ZIP format")
            }

            ZipInputStream(ByteArrayInputStream(file.bytes)).use { zipStream ->
                var entry: ZipEntry?
                var entriesCount = 0

                try {
                    while (zipStream.nextEntry.also { entry = it } != null) {
                        entriesCount++
                        entry?.let { zipEntry -> validateZipEntry(zipEntry) }
                        zipStream.closeEntry()
                    }
                } catch (e: ZipValidationException) {
                    throw e
                } catch (e: Exception) {
                    logger.error("Error processing ZIP entries", e)
                    throw InvalidZipStructureException("Invalid ZIP structure: ${e.message}")
                }

                logger.info("ZIP structure validation completed. Total entries: $entriesCount")
            }
        } catch (e: ZipValidationException) {
            throw e
        } catch (e: Exception) {
            logger.error("Error validating ZIP file", e)
            throw InvalidZipStructureException("Invalid ZIP file: ${e.message}")
        }
    }

    /**
     * Checks if the file has a valid ZIP signature.
     * ZIP files start with the bytes: PK\x03\x04 (0x50 0x4B 0x03 0x04)
     *
     * @param bytes The file bytes to check
     * @return true if the file has a valid ZIP signature
     */
    private fun isZipFileSignatureValid(bytes: ByteArray): Boolean {
        if (bytes.size < 4) return false

        // Check for ZIP file signature (PK\x03\x04)
        return bytes[0] == 0x50.toByte() &&
            bytes[1] == 0x4B.toByte() &&
            bytes[2] == 0x03.toByte() &&
            bytes[3] == 0x04.toByte()
    }

    private fun validateZipEntry(zipEntry: ZipEntry) {
        if (zipEntry.isDirectory) {
            logger.debug("Directory entry validation passed")
            return
        }

        val entryName = zipEntry.name
        logger.debug("Processing ZIP entry: $entryName, size: ${zipEntry.size}, compressed: ${zipEntry.compressedSize}")

        require(!entryName.contains("..")) {
            logger.warn("Path traversal attempt detected in ZIP entry: $entryName")
            throw PathTraversalAttemptException("Security violation: Path traversal attempt detected in entry: $entryName")
        }

        require(zipEntry.size <= solutionsProperties.maxEntrySize) {
            logger.warn("Suspiciously large entry detected: $entryName, size: ${zipEntry.size}")
            throw EntrySizeExceededException("Security violation: Entry size exceeds limit of ${solutionsProperties.maxEntrySize} bytes: $entryName")
        }

        require(!solutionsProperties.suspiciousExtensions.any { entryName.endsWith(it, ignoreCase = true) }) {
            logger.warn("Suspicious file extension detected in ZIP entry: $entryName")
            throw SuspiciousFileExtensionException("Security violation: Suspicious file extension detected in entry: $entryName")
        }

        if (zipEntry.size > 0 && zipEntry.compressedSize > 0) {
            val ratio = zipEntry.size.toDouble() / zipEntry.compressedSize.toDouble()
            logger.debug("Compression ratio for $entryName: $ratio (size: ${zipEntry.size}, compressed: ${zipEntry.compressedSize})")

            require(ratio <= solutionsProperties.maxCompressionRatio) {
                logger.warn("Suspicious compression ratio detected: $ratio for entry: $entryName")
                throw SuspiciousCompressionRatioException("Security violation: Suspicious compression ratio detected in entry: $entryName")
            }
        } else if (zipEntry.size > 0 && zipEntry.compressedSize <= 0) {
            logger.warn("Entry has size but no compressed size information: $entryName, size: ${zipEntry.size}")

            require(zipEntry.size <= solutionsProperties.maxUnknownCompressionEntrySize) {
                logger.warn("Entry with unknown compression ratio exceeds size limit: $entryName")
                throw SuspiciousCompressionRatioException("Security violation: Entry with unknown compression ratio exceeds size limit: $entryName")
            }
        }

        logger.debug("Entry validation passed: $entryName")
    }

    private fun parseSize(
        @NotBlank @NotEmpty sizeStr: String,
    ): Long {
        // Handle special cases
        if (sizeStr == "1B") return 1L
        if (sizeStr == "0B") return 0L

        val units =
            mapOf(
                "B" to 1L,
                "KB" to 1024L,
                "MB" to 1024L * 1024L,
                "GB" to 1024L * 1024L * 1024L,
            )

        val regex = "(\\d+)([BKMG]B)".toRegex()
        val matchResult = regex.find(sizeStr.trim())

        val result =
            matchResult?.let {
                val (value, unit) = it.destructured
                value.toLong() * (units[unit] ?: 1)
            }

        require(result != null && result > 0) {
            logger.error("Invalid size string format: '$sizeStr'")
            throw FileSizeExceededException("Invalid size format: '$sizeStr'. Expected format: <number><unit> (e.g., 10MB, 500KB)")
        }

        logger.debug("Parsed size string '$sizeStr' to $result bytes")
        return result
    }
}
