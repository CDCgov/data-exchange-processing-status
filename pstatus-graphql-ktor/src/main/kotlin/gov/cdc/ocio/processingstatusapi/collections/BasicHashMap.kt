package gov.cdc.ocio.processingstatusapi.collections

import kotlin.math.absoluteValue


/**
 * The basic hash map provided her is a simpler implementation of HashMap that works with GraphQL scalars.  The
 * reason for this is that the Map family, included HashMap, LinkedHashMap, TreeMap, etc. are not compatible with
 * GraphQL as they do not provide any public constructors.  Many attempts were made to utilize these Maps for
 * taking in a JSON input to GraphQL, but this was the only solution that worked.
 *
 * @param K
 * @param V
 * @property buckets Array<MutableList<Entry<K, V>>?>
 * @property size Int
 */
class BasicHashMap<K, V> {
    private data class Entry<K, V>(val key: K, var value: V)

    private val buckets = Array<MutableList<Entry<K, V>>?>(16) { null }
    private var size = 0

    /**
     * Calculates a bucket index from the hashcode associated with the key.
     *
     * @param key K
     * @return Int
     */
    private fun getBucketIndex(key: K): Int {
        return key.hashCode().absoluteValue % buckets.size
    }

    /**
     * Puts the provide key value pair into the hashmap.  If the key is already present the provided value overwrites
     * the previous value.
     *
     * @param key K
     * @param value V
     */
    fun put(key: K, value: V) {
        val index = getBucketIndex(key)
        val bucket = buckets[index] ?: mutableListOf<Entry<K, V>>().also { buckets[index] = it }

        // Check if the key already exists
        for (entry in bucket) {
            if (entry.key == key) {
                entry.value = value // Update existing value
                return
            }
        }

        // Add new entry
        bucket.add(Entry(key, value))
        size++
    }

    /**
     * Gets the provided key value from the hashmap.  Returns null if the key isn't found.
     *
     * @param key K
     * @return V?
     */
    fun get(key: K): V? {
        val index = getBucketIndex(key)
        val bucket = buckets[index] ?: return null

        for (entry in bucket) {
            if (entry.key == key) {
                return entry.value
            }
        }
        return null // Key not found
    }

    /**
     * Removes the provided key from the hashmap.
     *
     * @param key K
     * @return Boolean
     */
    fun remove(key: K): Boolean {
        val index = getBucketIndex(key)
        val bucket = buckets[index] ?: return false

        val iterator = bucket.iterator()
        while (iterator.hasNext()) {
            val entry = iterator.next()
            if (entry.key == key) {
                iterator.remove()
                size--
                return true
            }
        }
        return false // Key not found
    }

    /**
     * Returns whether the provided key is present in the hashmap.
     *
     * @param key K
     * @return Boolean
     */
    fun containsKey(key: K): Boolean {
        return get(key) != null
    }

    /**
     * Returns the size of the hashmap; i.e. the number of keys.
     *
     * @return Int
     */
    fun size(): Int {
        return size
    }

    /**
     * Returns whether the map is empty or not.
     *
     * @return Boolean
     */
    fun isEmpty(): Boolean {
        return size == 0
    }

    /**
     * Convenience function to convert this basic hash map into the standard kotlin HashMap.
     * @return Map<K, V>
     */
    fun toHashMap(): Map<K, V> {
        val hashmap = mutableMapOf<K, V>()
        buckets.forEach { bucket ->
            if (bucket != null) {
                for (entry in bucket) {
                    when (entry.value) {
                        is BasicHashMap<*, *> -> {
                            val copy = entry.value as BasicHashMap<*, *>
                            val valueMap = copy.toHashMap()
                            @Suppress("UNCHECKED_CAST")
                            hashmap[entry.key] = valueMap as V
                        }
                        else -> hashmap[entry.key] = entry.value
                    }
                }
            }
        }
        return hashmap
    }
}