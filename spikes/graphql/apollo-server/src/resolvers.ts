export const resolvers = {
    ReportType: {
      __resolveType(report: { schema_name: string; }) {
        switch (report.schema_name) {
          case 'dex-metadata-verify': return 'MetadataVerifyReport';
          case 'upload': return 'UploadStatusReport';
          default: return 'UnknownReport';
        }
      },
    },
    Query: {
      report: async (_: any, { id }: any, { dataSources }: any) => {
        var sqlQuery = `SELECT * from c WHERE c.id = '${id}'`;
        // var results = await dataSources.reportsAPI.findOneById(id);
        // return results.resource;
        var results = await dataSources.reportsAPI.findManyByQuery(sqlQuery);
        return results.resources[0];
      },
      reports: async (_: any, { first, offset }: any, { dataSources }: any) => {
        console.log("first: " + first);
        console.log("offset: " + offset);
        var sqlQuery = "SELECT * from c";
        var offsetVal = 0;
        if (typeof offset != "undefined") {
          offsetVal = offset;
        }
        sqlQuery += " offset " + offsetVal;
        if (typeof first != "undefined") {
          sqlQuery += " limit " + first;
        }
        console.log("sqlQuery = " + sqlQuery);
        var results = await dataSources.reportsAPI.findManyByQuery(sqlQuery);
        return results.resources;
      },
      metadataReport: async (_: any, { id }: any, { dataSources }: any) => {  
        console.log("id:" + id);
        var storedProcedure = dataSources.reportsAPI.container.scripts.storedProcedure("get_metadata_reports");
        var results = await storedProcedure.execute();
        return results.resource;
      },
    },
  };