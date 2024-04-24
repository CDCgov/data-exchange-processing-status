export const typeDefs = `
  interface ReportType {
    schema_name: String
    schema_version: String
  }

  type Tuple {
    name: String!
    value: String!
  }

  type MetadataVerifyReport implements ReportType {
    schema_name: String
    schema_version: String
    filename: String
    metadata: [Tuple]
  }

  type UploadStatusReport implements ReportType {
    schema_name: String
    schema_version: String
    offset: Float
    size: Float
    # v1 fields
    meta_destination_id: String
    meta_ext_event: String
    # v2 fields
    data_stream_id: String
    data_stream_route: String
  }

  type UnknownReport implements ReportType {
    schema_name: String
    schema_version: String
  }

  # Define the Report type
  type Report {
    id: ID
    uploadId: String
    reportId: String
    dataStreamId: String
    dataStreamRoute: String
    stageName: String
    timestamp: Float
    contentType: String
    content: ReportType
  }

  type Query {
    report(id: ID!): Report
    reports(first: Int, offset: Int, dataStreamId: String, dataStreamRoute: String): [Report]
  }

  type MetadataReport {
    count: Float
    hasSchemaName: Float
    isMetadataVerify: Float
  }
  
  # Query type for getting metadata report from stored procedure
  type Query {
    metadataReport: MetadataReport
  }
`;