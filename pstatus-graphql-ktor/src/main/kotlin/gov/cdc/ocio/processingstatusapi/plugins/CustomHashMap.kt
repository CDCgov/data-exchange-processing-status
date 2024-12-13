package gov.cdc.ocio.processingstatusapi.plugins

import kotlin.math.absoluteValue


class CustomHashMap<K, V> {
    private data class Entry<K, V>(val key: K, var value: V)

    private val buckets = Array<MutableList<Entry<K, V>>?>(16) { null }
    private var size = 0

    private fun getBucketIndex(key: K): Int {
        return key.hashCode().absoluteValue % buckets.size
    }

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

    fun containsKey(key: K): Boolean {
        return get(key) != null
    }

    fun size(): Int {
        return size
    }

    fun isEmpty(): Boolean {
        return size == 0
    }

    fun toHashMap(): Map<K, V> {
        val hashmap = mutableMapOf<K, V>()
        buckets.forEach { bucket ->
            if (bucket != null) {
                for (entry in bucket) {
                    when (entry.value) {
                        is CustomHashMap<*, *> -> {
                            val copy = entry.value as CustomHashMap<*, *>
                            val valueMap = copy.toHashMap()
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