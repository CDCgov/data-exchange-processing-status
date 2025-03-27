package cache

import gov.cdc.ocio.database.persistence.Collection

class MockCollection : Collection {

    private val items = mutableMapOf<String, Any>()
    override fun <T> getItem(id: String, classType: Class<T>?): T? {
        return items[id] as T
    }

    override fun <T> queryItems(query: String?, classType: Class<T>?): List<T> {
        return items.values.map { it as T }
    }

    override fun <T> createItem(id: String, item: T, classType: Class<T>, partitionKey: String?): Boolean {
        items[id] = item as Any
        return true
    }

    override fun deleteItem(itemId: String?, partitionKey: String?): Boolean {
        items.remove(itemId)
        return true
    }

    override val collectionVariable = "r"
    override val collectionVariablePrefix = "r."
    override val openBracketChar = '['
    override val closeBracketChar = ']'
    override val collectionNameForQuery = ""
    override val collectionElementForQuery = { name: String -> name }
}