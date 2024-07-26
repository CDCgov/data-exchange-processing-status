package gov.cdc.ocio.processingstatusapi.models

import java.util.ArrayList

/**
 * Defines the data stream which is used in determining access permissions.
 *
 * @property id Int
 * @property code String?
 * @property name String?
 * @property route String?
 * @property jurisdictions ArrayList<String>?
 */
class DataStream {
    var id: Int = 0
    var code: String? = null
    var name: String? = null
    var route: String? = null
    var jurisdictions: ArrayList<String>? = null
}