package org.jikvict.jikvictbackend.service

import io.mockk.every
import io.mockk.mockk
import org.apache.logging.log4j.Logger
import org.jikvict.jikvictbackend.configuration.LoggerFactory
import org.jikvict.jikvictbackend.exception.FileSizeExceededException
import org.jikvict.jikvictbackend.exception.FileTypeNotAllowedException
import org.jikvict.jikvictbackend.exception.InvalidZipStructureException
import org.jikvict.jikvictbackend.exception.PathTraversalAttemptException
import org.jikvict.jikvictbackend.exception.SuspiciousFileExtensionException
import org.jikvict.jikvictbackend.model.properties.SolutionsProperties
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import org.springframework.mock.web.MockMultipartFile
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class ZipValidatorServiceTest {
    private val logger: Logger = LoggerFactory().createLogger(ZipValidatorServiceTest::class.java)

    private val solutionsProperties: SolutionsProperties = mockk()

    private val zipValidatorService by lazy { ZipValidatorService(solutionsProperties, logger) }

    @Test
    fun `validateZipArchive should not throw exception for valid zip file`() {
        // Given
        every { solutionsProperties.maxFileSize } returns "5MB"
        every { solutionsProperties.allowedFileTypes } returns listOf("zip")
        every { solutionsProperties.maxEntrySize } returns 10_000_000L
        every { solutionsProperties.suspiciousExtensions } returns listOf(".exe", ".dll", ".bat", ".cmd", ".sh", ".js")
        every { solutionsProperties.maxCompressionRatio } returns 10.0
        every { solutionsProperties.maxUnknownCompressionEntrySize } returns 1000L

        val zipBytes = createValidZipFile()
        val file =
            MockMultipartFile(
                "file",
                "test.zip",
                "application/zip",
                zipBytes,
            )

        // When & Then
        assertDoesNotThrow {
            zipValidatorService.validateZipArchive(file)
        }
    }

    @Test
    fun `validateZipArchive should throw FileSizeExceededException for file exceeding max size`() {
        // Given
        every { solutionsProperties.maxFileSize } returns "1B"
        every { solutionsProperties.allowedFileTypes } returns listOf("zip")
        every { solutionsProperties.maxEntrySize } returns 10_000_000L
        every { solutionsProperties.suspiciousExtensions } returns listOf(".exe", ".dll", ".bat", ".cmd", ".sh", ".js")
        every { solutionsProperties.maxCompressionRatio } returns 10.0
        every { solutionsProperties.maxUnknownCompressionEntrySize } returns 1000L

        val zipBytes = createValidZipFile()
        val file =
            MockMultipartFile(
                "file",
                "test.zip",
                "application/zip",
                zipBytes,
            )

        // When & Then
        val exception =
            assertThrows<FileSizeExceededException> {
                zipValidatorService.validateZipArchive(file)
            }

        assert(exception.message.contains("exceeds maximum allowed size"))
    }

    @Test
    fun `validateZipArchive should throw FileTypeNotAllowedException for disallowed file type`() {
        // Given
        every { solutionsProperties.maxFileSize } returns "5MB"
        every { solutionsProperties.allowedFileTypes } returns listOf("jar")
        // We need to mock these properties even though they won't be used
        every { solutionsProperties.maxEntrySize } returns 10_000_000L
        every { solutionsProperties.suspiciousExtensions } returns listOf(".exe", ".dll", ".bat", ".cmd", ".sh", ".js")
        every { solutionsProperties.maxCompressionRatio } returns 10.0
        every { solutionsProperties.maxUnknownCompressionEntrySize } returns 1000L

        val zipBytes = createValidZipFile()
        val file =
            MockMultipartFile(
                "file",
                "test.zip",
                "application/zip",
                zipBytes,
            )

        // When & Then
        val exception =
            assertThrows<FileTypeNotAllowedException> {
                zipValidatorService.validateZipArchive(file)
            }

        assert(exception.message.contains("File type .zip is not allowed"))
    }

    @Test
    fun `validateZipArchive should throw InvalidZipStructureException for invalid zip file`() {
        // Given
        every { solutionsProperties.maxFileSize } returns "5MB"
        every { solutionsProperties.allowedFileTypes } returns listOf("zip")
        // We need to mock these properties even though they won't be used
        every { solutionsProperties.maxEntrySize } returns 10_000_000L
        every { solutionsProperties.suspiciousExtensions } returns listOf(".exe", ".dll", ".bat", ".cmd", ".sh", ".js")
        every { solutionsProperties.maxCompressionRatio } returns 10.0
        every { solutionsProperties.maxUnknownCompressionEntrySize } returns 1000L

        val invalidZipBytes = "This is not a valid ZIP file".toByteArray()
        val file =
            MockMultipartFile(
                "file",
                "test.zip",
                "application/zip",
                invalidZipBytes,
            )

        // When & Then
        val exception =
            assertThrows<InvalidZipStructureException> {
                zipValidatorService.validateZipArchive(file)
            }

        assert(exception.message.contains("Invalid ZIP file"))
    }

    @Test
    fun `validateZipArchive should throw PathTraversalAttemptException for path traversal attempts`() {
        // Given
        every { solutionsProperties.maxFileSize } returns "5MB"
        every { solutionsProperties.allowedFileTypes } returns listOf("zip")
        every { solutionsProperties.maxEntrySize } returns 10_000_000L
        every { solutionsProperties.suspiciousExtensions } returns listOf(".exe", ".dll", ".bat", ".cmd", ".sh", ".js")
        every { solutionsProperties.maxCompressionRatio } returns 10.0
        every { solutionsProperties.maxUnknownCompressionEntrySize } returns 1000L

        val zipBytes = createZipFileWithPathTraversal()
        val file =
            MockMultipartFile(
                "file",
                "test.zip",
                "application/zip",
                zipBytes,
            )

        // When & Then
        val exception =
            assertThrows<PathTraversalAttemptException> {
                zipValidatorService.validateZipArchive(file)
            }

        assert(exception.message.contains("Path traversal attempt detected"))
    }

    @Test
    fun `validateZipArchive should throw SuspiciousFileExtensionException for suspicious file extension`() {
        // Given
        every { solutionsProperties.maxFileSize } returns "5MB"
        every { solutionsProperties.allowedFileTypes } returns listOf("zip")
        every { solutionsProperties.maxEntrySize } returns 10_000_000L
        every { solutionsProperties.suspiciousExtensions } returns listOf(".exe", ".dll", ".bat", ".cmd", ".sh", ".js")
        every { solutionsProperties.maxCompressionRatio } returns 10.0
        every { solutionsProperties.maxUnknownCompressionEntrySize } returns 1000L

        val zipBytes = createZipFileWithSuspiciousExtension()
        val file =
            MockMultipartFile(
                "file",
                "test.zip",
                "application/zip",
                zipBytes,
            )

        // When & Then
        val exception =
            assertThrows<SuspiciousFileExtensionException> {
                zipValidatorService.validateZipArchive(file)
            }

        assert(exception.message.contains("Suspicious file extension detected"))
    }

    private fun createZipFileWithSuspiciousExtension(): ByteArray {
        val baos = ByteArrayOutputStream()
        ZipOutputStream(baos).use { zipOut ->
            val entry = ZipEntry("malicious.exe")
            zipOut.putNextEntry(entry)
            zipOut.write("This is a malicious file content".toByteArray())
            zipOut.closeEntry()
        }
        return baos.toByteArray()
    }

    private fun createValidZipFile(): ByteArray {
        val baos = ByteArrayOutputStream()
        ZipOutputStream(baos).use { zipOut ->
            val entry = ZipEntry("test.txt")
            zipOut.putNextEntry(entry)
            zipOut.write("This is a test file content".toByteArray())
            zipOut.closeEntry()

            val entry2 = ZipEntry("folder/nested.txt")
            zipOut.putNextEntry(entry2)
            zipOut.write("This is a nested file content".toByteArray())
            zipOut.closeEntry()
        }
        return baos.toByteArray()
    }

    private fun createZipFileWithPathTraversal(): ByteArray {
        val baos = ByteArrayOutputStream()
        ZipOutputStream(baos).use { zipOut ->
            val entry = ZipEntry("../../../etc/passwd")
            zipOut.putNextEntry(entry)
            zipOut.write("This is a malicious file content".toByteArray())
            zipOut.closeEntry()
        }
        return baos.toByteArray()
    }
}
