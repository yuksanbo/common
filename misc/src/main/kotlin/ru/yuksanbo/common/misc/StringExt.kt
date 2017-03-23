package ru.yuksanbo.common.misc

fun String.extract(preAnchor: String, postAnchor: String? = null): String? {
    val preIdx = this.indexOf(preAnchor)
    if (preIdx == -1) return null
    val preEndIdx = preIdx + preAnchor.length

    val postIdx: Int
    if (postAnchor != null) {
        postIdx = this.indexOf(postAnchor, startIndex = preEndIdx)
        if (postIdx == -1) return null
    } else {
        postIdx = this.length
    }

    return this.substring(preEndIdx, postIdx)
}