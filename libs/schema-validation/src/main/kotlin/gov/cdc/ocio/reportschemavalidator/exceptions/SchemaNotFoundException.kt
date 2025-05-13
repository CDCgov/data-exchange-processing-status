package gov.cdc.ocio.reportschemavalidator.exceptions

class SchemaNotFoundException(filePath: String): Exception("schema file at $filePath not found")