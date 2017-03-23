package ru.yuksanbo.common.misc

/* https://gist.github.com/fabiomsr/845664a9c7e92bafb6fb0ca70d4e44fd/ */

private val HEX_CHARS = "0123456789ABCDEF".toCharArray()

private fun byteToHex(buffer: StringBuffer, b: Byte) {
    val octet = b.toInt()
    val firstIndex = (octet and 0xF0).ushr(4)
    val secondIndex = octet and 0x0F
    buffer.append(HEX_CHARS[firstIndex])
    buffer.append(HEX_CHARS[secondIndex])
}

fun ByteArray.toHex(): String {
    val result = StringBuffer()

    forEach {
        byteToHex(result, it)
    }

    return result.toString()
}

private val bytesPerRow = 32
private val bytesPerBlock = 8
private val blocksPerRow = bytesPerRow / bytesPerBlock
private val printableChars = """abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789`~!@#$%^&*()_+[]{};':",./<>? """

fun ByteArray.toVerboseHex(): String {
    val result = StringBuffer()

    result.append("\n")

    for (row in 0 until (Math.ceil(this.size.toDouble() / bytesPerRow).toInt())) {
        for (block in 0 until blocksPerRow) {
            for (i in 0 until bytesPerBlock) {
                val realIndex = row * bytesPerRow + block * bytesPerBlock + i
                if (realIndex < this.size) {
                    byteToHex(result, this[realIndex])
                } else {
                    result.append("  ")
                }
                result.append(" ")
            }
            result.append("  ")
        }
        result.append("    ")
        for (i in 0 until bytesPerRow) {
            val realIndex = row * bytesPerRow + i
            if (realIndex < this.size) {
                val ch = this[realIndex].toChar()
                result.append(if (printableChars.contains(ch)) ch else '.')
            }
        }
        result.append("\n")
    }

    return result.toString()
}