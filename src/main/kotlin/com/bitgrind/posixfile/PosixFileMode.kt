package com.bitgrind.posixfile

import java.nio.file.LinkOption
import java.nio.file.Path
import kotlin.io.path.getPosixFilePermissions
import kotlin.io.path.isDirectory
import kotlin.io.path.isSymbolicLink

private const val FILE_TYPES = "?pc?d?b?-?l?s???"

/**
 * Read the permission (mode) value from the Filesystem for this [Path].
 *
 * Due to lack of SDK support, only directory, symlink and regular files are supported (block dev, char dev, fifo, and
 * sockets are reported as regular files). Special bits (setUid/setGid/sticky) are also unavailable. These bits are
 * always returned unset.
 */
fun Path.getPosixFileMode(): Int {
    val baseMode =
        getPosixFilePermissions(LinkOption.NOFOLLOW_LINKS).sumOf { 0x100 shr it.ordinal }
    return if (isSymbolicLink()) {
        0xa000 + baseMode
    } else if (isDirectory()) {
        0x4000 + baseMode
    } else {
        // regular file.
        // ...or possibly a character device, block device, pipe, or socket. ¯\_(ツ)_/¯
        0x8000 + baseMode
    }
}

/** Convert the file mode string into an Integer as would be reported by stat(2) within stat.st_mode */
fun String.toPosixModeInt(): Int {
    require(length == 10) { "Should have 10 characters" }
    // type
    val type =
        when (get(0)) {
            'p' -> 1
            'c' -> 2
            'd' -> 4
            'b' -> 6
            '-' -> 8
            'l' -> 10
            's' -> 12
            else -> 0
        } shl 12
    return type + substring(1).chunked(3).reversed().mapIndexed(::parseModeChars).sum()
}

private fun parseModeChars(position: Int, mode: String): Int {
    var value =
        when (mode[0]) {
            'r' -> 4 shl position * 3
            else -> 0
        }
    value +=
        when (mode[1]) {
            'w' -> 2 shl position * 3
            else -> 0
        }
    return value +
        when (mode[2]) {
            'x' -> 1 shl position * 3
            'S' -> 1 shl (9 + position)
            's' -> (1 shl 9 + position) + (1 shl position * 3)
            'T' ->
                require(position == 0) { "Sticky bit in invalid position ($position)" }
                    .let { 1 shl 9 }
            't' ->
                require(position == 0) { "Sticky bit in invalid position ($position)" }
                    .let { (1 shl 9) + 1 }
            else -> 0
        }
}

/**
 * Format a mode integer from stat.st_mode to the familiar format presented by `ls -l`.
 *
 * Handles all file types, special bits (setUid, setGid, sticky), and user/group/other permission bits.
 */
fun Int.toPosixModeString(): String {
    val type = this shr 12 and 15
    val special = this shr 9 and 7
    return StringBuilder(10)
        .also {
            it.append(FILE_TYPES[type])
            it.appendModeChars(this shr 6 and 7, special and 4 != 0, 's')
            it.appendModeChars(this shr 3 and 7, special and 2 != 0, 's')
            it.appendModeChars(this shr 0 and 7, special and 1 != 0, 't')
        }
        .toString()
}

private fun StringBuilder.appendModeChars(bits: Int, special: Boolean, specialX: Char) {
    append(if (bits and 4 != 0) 'r' else '-')
    append(if (bits and 2 != 0) 'w' else '-')
    append(
        if (bits and 1 != 0) {
            if (special) specialX else 'x'
        } else {
            if (special) specialX.uppercaseChar() else '-'
        }
    )
}
