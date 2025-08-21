package gov.cdc.ocio.database

import gov.cdc.ocio.database.persistence.Collection
import mu.KotlinLogging


/**
 * Convenient base class for any processing status queries.
 *
 * @property collection Collection
 * @property logger KLogger
 * @property collectionName String
 * @property cVar String
 * @property cPrefix String
 * @property openBkt Char
 * @property closeBkt Char
 * @property timeFunc Function1<Long, String>
 * @constructor
 */
open class QueryBuilder(
    protected val collection: Collection
) {
    protected val logger = KotlinLogging.logger {}

    protected val collectionName = collection.collectionNameForQuery
    protected val cVar = collection.collectionVariable
    protected val cPrefix = collection.collectionVariablePrefix
    protected val openBkt = collection.openBracketChar
    protected val closeBkt = collection.closeBracketChar
    protected val timeFunc = collection.timeConversionForQuery
    protected val cElFunc = collection.collectionElementForQuery

    /**
     * Prepares a list of [Any] type that has a toString() implementation that needs to be joined by a comma separated
     * list in preparation for a query.  Elements are also surrounded by single quotes.
     *
     * @param items List<Any>
     * @return String
     */
    protected fun listForQuery(items: List<Any>) = items.joinToString(", ") { "'$it'" }
}
