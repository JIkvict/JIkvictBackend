package org.jikvict.jikvictbackend.service

import org.apache.logging.log4j.Logger
import org.jikvict.jikvictbackend.exception.EntrySizeExceededException
import org.jikvict.jikvictbackend.exception.FileSizeExceededException
import org.jikvict.jikvictbackend.exception.FileTypeNotAllowedException
import org.jikvict.jikvictbackend.exception.InvalidZipStructureException
import org.jikvict.jikvictbackend.exception.PathTraversalAttemptException
import org.jikvict.jikvictbackend.exception.SuspiciousCompressionRatioException
import org.jikvict.jikvictbackend.exception.SuspiciousFileExtensionException
import org.jikvict.jikvictbackend.exception.ZipValidationException
import org.jikvict.jikvictbackend.model.properties.SolutionsProperties
import org.springframework.stereotype.Service
import org.springframework.validation.annotation.Validated
import org.springframework.web.multipart.MultipartFile
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
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
        val cleanedBytes = removeFilesWithSuspiciousExtensions(file.bytes)
        validateZipStructure(cleanedBytes)
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

    private fun removeFilesWithSuspiciousExtensions(zipBytes: ByteArray): ByteArray {
        val suspiciousFiles = mutableListOf<String>()
        var totalFiles = 0

        // First pass: identify files with suspicious extensions
        ZipInputStream(ByteArrayInputStream(zipBytes)).use { zipStream ->
            var entry: ZipEntry?
            while (zipStream.nextEntry.also { entry = it } != null) {
                entry?.let { zipEntry ->
                    if (!zipEntry.isDirectory) {
                        totalFiles++
                        val entryName = zipEntry.name
                        if (solutionsProperties.suspiciousExtensions.any { entryName.endsWith(it, ignoreCase = true) }) {
                            suspiciousFiles.add(entryName)
                            logger.warn("Found suspicious file extension in ZIP entry: $entryName - will be removed")
                        }
                    }
                }
                zipStream.closeEntry()
            }
        }

        // If no suspicious files found, return original bytes
        if (suspiciousFiles.isEmpty()) {
            return zipBytes
        }

        // Second pass: rebuild zip without suspicious files
        logger.info("Rebuilding ZIP archive without ${suspiciousFiles.size} suspicious file(s): $suspiciousFiles")
        val baos = ByteArrayOutputStream()
        ZipOutputStream(baos).use { zipOut ->
            ZipInputStream(ByteArrayInputStream(zipBytes)).use { zipStream ->
                var entry: ZipEntry?
                var hasValidEntries = false
                while (zipStream.nextEntry.also { entry = it } != null) {
                    entry?.let { zipEntry ->
                        val entryName = zipEntry.name
                        if (entryName !in suspiciousFiles) {
                            // Copy this entry to the new zip
                            val newEntry = ZipEntry(entryName)
                            zipOut.putNextEntry(newEntry)
                            if (!zipEntry.isDirectory) {
                                zipStream.copyTo(zipOut)
                                hasValidEntries = true
                            }
                            zipOut.closeEntry()
                        }
                    }
                    zipStream.closeEntry()
                }

                // If all files were removed, create an empty placeholder file to keep zip valid
                if (!hasValidEntries && totalFiles > 0) {
                    logger.info("All files were removed, creating empty placeholder")
                    val placeholderEntry = ZipEntry(".empty")
                    zipOut.putNextEntry(placeholderEntry)
                    zipOut.write(ByteArray(0))
                    zipOut.closeEntry()
                }
            }
        }

        logger.info("ZIP archive cleaned successfully")
        return baos.toByteArray()
    }

    private fun validateZipStructure(zipBytes: ByteArray) {
        try {
            require(zipBytes.size >= 4 && isZipFileSignatureValid(zipBytes)) {
                logger.warn("Invalid ZIP file signature detected")
                throw InvalidZipStructureException("Invalid ZIP file: File signature does not match ZIP format")
            }

            ZipInputStream(ByteArrayInputStream(zipBytes)).use { zipStream ->
                var entry: ZipEntry?
                var entriesCount = 0

                try {
                    while (zipStream.nextEntry.also { entry = it } != null) {
                        entriesCount++
                        entry?.let { zipEntry -> validateZipEntry(zipEntry) }
                        logger.info("Validated ZIP entry: ${entry?.name}")
                        zipStream.closeEntry()
                    }
                } catch (e: ZipValidationException) {
                    throw e
                } catch (e: java.util.zip.ZipException) {
                    val message = e.message ?: ""
                    if (message.contains("EXT descriptor") || message.contains("DEFLATED")) {
                        logger.warn("ZIP compatibility issue (likely from macOS): ${e.message}")
                    } else {
                        logger.error("ZIP format error: ${e.message}")
                        throw InvalidZipStructureException("Invalid ZIP structure: ${e.message}")
                    }
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
        val units =
            mapOf(
                "B" to 1L,
                "KB" to 1024L,
                "MB" to 1024L * 1024L,
                "GB" to 1024L * 1024L * 1024L,
            )

        val regex = "(\\d+)(B|KB|MB|GB)".toRegex()
        val matchResult = regex.find(sizeStr.trim())

        val result =
            matchResult?.let {
                val (value, unit) = it.destructured
                value.toLong() * (units[unit] ?: 1)
            }

        require(result != null && result > 0) {
            logger.error("Invalid size string format: '$sizeStr'")
            throw IllegalArgumentException("Invalid size format: '$sizeStr'. Expected format: <number><unit> (e.g., 10MB, 500KB)")
        }

        logger.debug("Parsed size string '$sizeStr' to $result bytes")
        return result
    }
}
