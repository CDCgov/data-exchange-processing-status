# PS API Helpful Queries
Collection of helpful CosmosDB SQL queries.

## Counts Queries

### Report counts by schema name and stage name
```roomsql
select
    count(1) as counts, r.content.schema_name, r.stageName
    from Reports r where
    r._ts >= 1713916800 and r._ts <= 1714003199 and r.dataStreamId = 'aims-celr' and r.dataStreamRoute = 'hl7'
    group by r.content.schema_name, r.stageName
```
Sample output:
```json
[
    {
        "counts": 62,
        "schema_name": "dex-file-copy",
        "stageName": "dex-routing"
    },
    {
        "counts": 6357,
        "schema_name": "DEX HL7v2 REDACTOR",
        "stageName": "REDACTOR"
    },
    {
        "counts": 6113,
        "schema_name": "upload",
        "stageName": "dex-upload"
    },
    {
        "counts": 6089,
        "schema_name": "DEX HL7v2 RECEIVER",
        "stageName": "RECEIVER"
    },
    {
        "counts": 6417,
        "schema_name": "DEX HL7v2 HL7-JSON-LAKE-TRANSFORMER",
        "stageName": "HL7-JSON-LAKE-TRANSFORMER"
    },
    {
        "counts": 6381,
        "schema_name": "DEX HL7v2 STRUCTURE-VALIDATOR",
        "stageName": "STRUCTURE-VALIDATOR"
    },
    {
        "counts": 14527,
        "schema_name": "dex-metadata-verify",
        "stageName": "dex-metadata-verify"
    },
    {
        "counts": 6495,
        "schema_name": "DEX HL7v2 LAKE-SEGMENTS-TRANSFORMER",
        "stageName": "LAKE-SEGMENTS-TRANSFORMER"
    },
    {
        "counts": 7277,
        "schema_name": "dex-file-copy",
        "stageName": "dex-file-copy"
    }
]
```

### Unique Upload IDs for "upload"
```roomsql
select count(unqiueUploadCounts) as uploadCounts
    from (SELECT distinct r.uploadId FROM Reports r where
    r.dataStreamId='aims-celr' and
    r.dataStreamRoute='hl7' and
    r.content.schema_name = 'upload' and
    r._ts >= 1712620800 and r._ts <= 1712707199) as unqiueUploadCounts
```
Sample output:
```json
[
    {
        "uploadCounts": 12313
    }
]
```

### Check for duplicate uploadIds
```roomsql
select d.uploadId, d.tot_count from
    (SELECT r.uploadId, count(1) as tot_count FROM r
    where
    r.dataStreamId='aims-celr' and
    r.dataStreamRoute='hl7' and 
    r.content.schema_name = 'upload' and
    r._ts >= 1712620800 and r._ts <= 1712707199
    group by r.uploadId) as d
    where d.tot_count>1
```
> **_NOTE:_** If the system is operating as expected, this should return no results. However, if there is an issue you may see something like the following.
```json
[
  {
    "uploadId": "4e8e2764b59abc45c6d8229ede879a06",
    "tot_count": 2
  }
]
```

### Count of failed structure validations
```roomsql
select
    count(not contains(upper(r.content.summary.current_status), 'VALID_MESSAGE') ? 1 : undefined) as invalid
    from Reports r
    where r.content.schema_name = 'DEX HL7v2 STRUCTURE-VALIDATOR' and
    r._ts >= 1710302400 and r._ts <= 1710388799
```
Sample output:
```json
[
  {
    "invalid": 791
  }
]
```

### Check for duplicate filenames
```roomsql
select d.filename, d.tot_count from(
    SELECT r.content.metadata.filename, count(1) as tot_count
    FROM Reports r
    where
    r.dataStreamId='aims-celr' and
    r.dataStreamRoute='hl7' and
    r.content.schema_name = 'upload'
    group by r.content.metadata.filename) as d where d.tot_count>1
```
Sample output:
```json
[
    {
        "filename": "InterPartner~CELR~DC~AIMSPlatform~Prod~Prod~20240308092005~STOP~CVS_COVID_DC_20231214085952.hl7(2).hl7",
        "tot_count": 2
    },
    {
        "filename": "10KB-test-file",
        "tot_count": 2
    },
    {
        "filename": "10KB-test-file",
        "tot_count": 2
    },
    {
        "filename": "InterPartner~CELR~OK~AIMSPlatform~Prod~Prod~20240318010038~STOP~SouthWestern_2024-03-17_01:11:32.hl7",
        "tot_count": 2
    },
    {
        "filename": "10KB-test-file",
        "tot_count": 2
    },
    {
        "filename": "10KB-test-file",
        "tot_count": 4
    },
    {
        "filename": "10KB-test-file",
        "tot_count": 3
    },
    {
        "filename": "InterPartner~CELR~DC~AIMSPlatform~Prod~Prod~20240308092016~STOP~CVS_COVID_DC_20230702211312.hl7(2).hl7",
        "tot_count": 2
    },
    {
        "filename": "InterPartner~CELR~DC~AIMSPlatform~Prod~Prod~20240309071824~STOP~CVS_COVID_DC_20230629165030.hl7.hl7",
        "tot_count": 2
    },
    {
        "filename": "InterPartner~CELR~DC~AIMSPlatform~Prod~Prod~20240310072008~STOP~CVS_COVID_DC_20230629165246.hl7(9).hl7",
        "tot_count": 2
    },
    {
        "filename": "InterPartner~CELR~DC~AIMSPlatform~Prod~Prod~20240308091859~STOP~CVS_COVID_DC_20230919090015.hl7(22).hl7",
        "tot_count": 2
    },
    {
        "filename": "InterPartner~CELR~DC~AIMSPlatform~Prod~Prod~20240326080124~STOP~CVS_COVID_DC_20230417085826.hl7(43).hl7",
        "tot_count": 2
    },
    {
        "filename": "10KB-test-file",
        "tot_count": 2
    },
    {
        "filename": "10KB-test-file",
        "tot_count": 2
    },
    {
        "filename": "10KB-test-file",
        "tot_count": 2
    },
    {
        "filename": "10KB-test-file",
        "tot_count": 2
    },
    {
        "filename": "10KB-test-file",
        "tot_count": 2
    },
    {
        "filename": "InterPartner~CELR~DC~AIMSPlatform~Prod~Prod~20240309072154~STOP~CVS_COVID_DC_20240127085951.hl7(21).hl7",
        "tot_count": 2
    },
    {
        "filename": "10KB-test-file",
        "tot_count": 2
    },
    {
        "filename": "10KB-test-file",
        "tot_count": 2
    },
    {
        "filename": "10KB-test-file",
        "tot_count": 2
    }
]
```
### Get rollup counts
```roomsql
select
    r.content.schema_name, r.content.schema_version, count(r.stageName) as counts, r.stageName
    from Reports r where r.dataStreamId = 'aims-celr' and r.dataStreamRoute = 'hl7' and r._ts >= 1714262400 and r._ts <= 1714348799
    group by r.stageName, r.content.schema_name, r.content.schema_version
```
Sample output:
```json
[
    {
        "schema_name": "DEX HL7v2 STRUCTURE-VALIDATOR",
        "schema_version": "2.0.0",
        "counts": 12757,
        "stageName": "STRUCTURE-VALIDATOR"
    },
    {
        "schema_name": "DEX HL7v2 LAKE-SEGMENTS-TRANSFORMER",
        "schema_version": "2.0.0",
        "counts": 12624,
        "stageName": "LAKE-SEGMENTS-TRANSFORMER"
    },
    {
        "schema_name": "DEX HL7v2 HL7-JSON-LAKE-TRANSFORMER",
        "schema_version": "2.0.0",
        "counts": 12624,
        "stageName": "HL7-JSON-LAKE-TRANSFORMER"
    },
    {
        "schema_name": "DEX HL7v2 REDACTOR",
        "schema_version": "2.0.0",
        "counts": 12758,
        "stageName": "REDACTOR"
    },
    {
        "schema_name": "DEX HL7v2 RECEIVER",
        "schema_version": "2.0.0",
        "counts": 11600,
        "stageName": "RECEIVER"
    },
    {
        "schema_name": "upload",
        "schema_version": "1.0",
        "counts": 11598,
        "stageName": "dex-upload"
    }
]
```