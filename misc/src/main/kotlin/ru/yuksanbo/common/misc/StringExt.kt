package ru.yuksanbo.common.misc

/**
 * Alternative to regex group extraction for simple cases (x10 faster).
 * @param preAnchor if null, extract starting at `this[0]`
 * @param postAnchor if null, extract until `this[this.length]`
 * @return substring between [preAnchor] and [postAnchor]
 */
fun String.extract(preAnchor: String? = null, postAnchor: String? = null): String? {
    val preIdx: Int
    if (preAnchor != null) {
        preIdx = this.indexOf(preAnchor)
        if (preIdx == -1) return null
    } else {
        preIdx = 0
    }
    val preEndIdx = preIdx + (preAnchor?.length ?: 0)

    val postIdx: Int
    if (postAnchor != null) {
        postIdx = this.indexOf(postAnchor, startIndex = preEndIdx)
        if (postIdx == -1) return null
    } else {
        postIdx = this.length
    }

    return this.substring(preEndIdx, postIdx)
}