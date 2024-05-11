package com.bitgrind.posixfile

import com.google.common.jimfs.Configuration
import com.google.common.jimfs.Jimfs
import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.ArgumentsProvider
import org.junit.jupiter.params.provider.ArgumentsSource
import org.junit.jupiter.params.provider.ValueSource
import java.nio.file.FileSystem
import java.nio.file.LinkOption
import java.nio.file.Path
import java.nio.file.attribute.GroupPrincipal
import java.nio.file.attribute.PosixFilePermissions
import java.nio.file.attribute.UserPrincipal
import java.util.stream.Stream
import kotlin.io.path.createDirectory
import kotlin.io.path.createFile
import kotlin.io.path.createSymbolicLinkPointingTo
import kotlin.io.path.setAttribute

const val testDirectoryName = "directory"
const val testSymlinkName = "symlink"
const val testFileName = "file"

class PosixFileModeTest {

    @ParameterizedTest
    @ValueSource(strings = ["xxx", "0123456789012345"])
    fun throwsOnWrongLength(input: String) {
        assertThrows<IllegalArgumentException> {  input.toPosixModeInt() }
    }

    @ParameterizedTest
    @ValueSource(strings = ["------T---", "------t---", "---T------", "---t------"])
    fun throwsOnMisplacedStickyBit(input: String) {
        assertThrows<IllegalArgumentException> {  input.toPosixModeInt() }
    }

    @ParameterizedTest
    @ValueSource(strings = ["**********", "$2lb46d/<%" ])
    fun invalidCharsAreTreatedAsZero(input: String) {
         assertEquals(0, input.toPosixModeInt())
    }

    @ParameterizedTest
    @ArgumentsSource(PosixModePairProvider::class)
    fun toPosixModeString(intPerms: Int, stringPerms: String) {
        assertEquals(stringPerms, intPerms.toPosixModeString(), "Integer mode to string represenation")
    }

    @ParameterizedTest
    @ArgumentsSource(PosixModePairProvider::class)
    fun toPosixModeInt(intPerms: Int, stringPerms: String) {
        assertEquals(intPerms, stringPerms.toPosixModeInt(), "String representation ($stringPerms) to Integer mode")
    }

    class PosixModePairProvider : ArgumentsProvider {
        override fun provideArguments(context: ExtensionContext): Stream<Arguments> {
            return listOf(
                0x11a4 to "prw-r--r--",
                0x11ed to "prwxr-xr-x",
                0x2180 to "crw-------",
                0x2190 to "crw--w----",
                0x21a0 to "crw-r-----",
                0x21a4 to "crw-r--r--",
                0x21b0 to "crw-rw----",
                0x21b4 to "crw-rw-r--",
                0x21b6 to "crw-rw-rw-",
                0x41ed to "drwxr-xr-x",
                0x43ff to "drwxrwxrwt",
                0x4fff to "drwsrwsrwt",
                0x61b0 to "brw-rw----",
                0x8180 to "-rw-------",
                0x81a0 to "-rw-r-----",
                0x81a4 to "-rw-r--r--",
                0x81ed to "-rwxr-xr-x",
                0x8000 to "----------",
                0x8008 to "------x---",
                0x8201 to "---------t",
                0x8200 to "---------T",
                0x8400 to "------S---",
                0x8408 to "------s---",
                0x8800 to "---S------",
                0x8840 to "---s------",
                0x8f53 to "-r-s-wS-wt",
                0x8f31 to "-r-SrwS--t",
                0xa1ff to "lrwxrwxrwx",
                0xc1b6 to "srw-rw-rw-",
                0xc1ff to "srwxrwxrwx",
                0xce00 to "s--S--S--T",
            ).map { (intValue, stringValue) ->
                Arguments.of(intValue, stringValue)
            }.stream()
        }
    }

    private val testFileSystem = createTestFileSystem()

    @Test
    fun getPosixFileModeFromDirectory() {
        assertPosixMode(testDirectoryName, "drwxrwxr-x")
    }

    @Test
    fun getPosixFileModeFromFile() {
        assertPosixMode(testFileName, "-rwxr-xr-x")
    }

    @Test
    fun getPosixFileModeFromSymlink() {
        assertPosixMode(testSymlinkName, "lrwxrwxrwx")
    }

    private fun assertPosixMode(name: String, expectedMode: String) {
        val mode = testFileSystem.getPath(name).getPosixFileMode().toPosixModeString()
        assertThat(mode).isEqualTo(expectedMode)
    }

    private fun createTestFileSystem(): FileSystem {
        val config = Configuration.unix().toBuilder()
            .setAttributeViews("basic", "owner", "posix", "unix")
            .setWorkingDirectory("/")
            .build()
        val fs: FileSystem = Jimfs.newFileSystem(config)

        val dir: Path = fs.getPath(testDirectoryName)
        dir.createDirectory()
        dir.setAttribute("owner:owner", UserPrincipal { "root" })
        dir.setAttribute("posix:group", GroupPrincipal { "root" })
        dir.setAttribute("posix:permissions", PosixFilePermissions.fromString("rwxrwxr-x"))

        val file: Path = fs.getPath(testFileName)
        file.createFile()
        file.setAttribute("owner:owner", UserPrincipal { "root" })
        file.setAttribute("posix:group", GroupPrincipal { "root" })
        file.setAttribute("posix:permissions", PosixFilePermissions.fromString("rwxr-xr-x"))

        val sym: Path = fs.getPath(testSymlinkName)
        sym.createSymbolicLinkPointingTo(file)
        sym.setAttribute("posix:permissions", PosixFilePermissions.fromString("rwxrwxrwx"), LinkOption.NOFOLLOW_LINKS)
        return fs
    }
}