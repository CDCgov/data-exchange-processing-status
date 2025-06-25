package gov.cdc.ocio.types.extensions

/**
 * Renames a key in the mutable map. If the old key exists and does not
 * equal the new key, the associated value is moved to the new key.
 *
 * @param oldKey The key to be renamed.
 * @param newKey The new key to be created.
 */
fun <K, V> MutableMap<K, V>.renameKey(oldKey: K, newKey: K) {
    if (oldKey != newKey && this.containsKey(oldKey)) {
        val value = this.remove(oldKey)
        if (value != null || this.containsKey(oldKey)) {
            this[newKey] = value!!
        }
    }
}
