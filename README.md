# [PosixFileMode.kt](src/main/kotlin/com/bitgrind/posixfile/PosixFileMode.kt)

A small set of Kotlin extension functions for interacting with full POSIX file protection mode (permission) values.



### `fun Int.toPosixModeString(): String`

Format a mode integer from stat.st_mode to the familiar format presented by `ls -l`.

Handles all file types, special bits (setUid, setGid, sticky), and user/group/other permission bits.



### `fun String.toPosixModeInt(): Int`

Convert the file mode string into an Integer as would be reported by stat(2) within stat.st_mode



### `fun Path.getPosixFileMode(): Int`

Read the permission (mode) value from the local Filesystem for this [Path].

Due to limited SDK support only directory, symlink and regular files are supported (block dev, char dev, fifo, and
sockets are reported as regular files). Special bits (setUid/setGid/sticky) are also unavailable. These bits are
always returned unset.
