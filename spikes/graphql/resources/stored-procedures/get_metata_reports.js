function stored_procedure(prefix) {
    var share = { count: 0, hasSchemaName : 0, isMetadataVerify: 0, partitions: {}, prefix };

    doForAll({
        filter: function limiter(record){
            if (record && record.dataStreamId === 'dex-testing') return true;
            else return false;
        },
        callback: function handleRecord(record) {
            //Keep track of this partition...
            let partitionKey = record.uploadId;
            if (share.partitions[partitionKey])
                share.partitions[partitionKey]++;
            else 
                share.partitions[partitionKey] = 1;

            share.count++;
            if (record.content.schema_name !== undefined) share.hasSchemaName++;
            if (record.content.schema_name === 'dex-metadata-verify') share.isMetadataVerify++;
        },
        finaly: function whenAllIsDone() {
            console.log("count = " + share.count + ". ");
            console.log("has schema name: " + share.hasSchemaName + ". ")
            console.log("is isMetadataVerify: " + share.isMetadataVerify + ". ")
            var parts = Object.getOwnPropertyNames(share.partitions)
            console.log("partition keys: " + parts.length + " ...");

            getContext()
                .getResponse()
                .setBody(share);
        }
    });

    //The magic function...
    //also see: https://azure.github.io/azure-cosmosdb-js-server/Collection.html
    function doForAll(task, ctoken) {

        if (!task) throw "Expected one parameter of type: { filter?: (rec?)=>boolean, callback?: (rec?) => void, finaly?: () => void }";
        //Note:
        //the "__" symbol is an alias for var collection = getContext().getCollection(); = aliased by __

        var result = getContext()
            .getCollection()
            .chain()
                .filter(task.filter || function (rec) { return true; })
                .map(task.callback || function (rec) { return undefined; })
            .value({ continuation: ctoken }, function afterBatchCallback (err, feed, options) {
                if (err) throw err;
                if (options.continuation)
                    doForAll(task, options.continuation);
                else if (task.finaly)
                    task.finaly();
            });

        if (!result.isAccepted)
            throw "catastrophic failure";
    }

}