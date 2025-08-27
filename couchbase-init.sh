#!/bin/bash

CBQ_HOST="http://couchbase:8093"
CB_ADMIN="admin"
CB_PASSWORD="password"
BUCKET_NAME="ProcessingStatus"

# Wait for Couchbase Server to start
until curl -s -u $CB_ADMIN:$CB_PASSWORD http://couchbase:8091/pools >/dev/null; do
  echo "Waiting for Couchbase to start..."
  sleep 5
done

echo "Couchbase is up and running."

# Function to wait until a bucket is healthy
wait_for_bucket() {
  local bucket=$1
  local max_retries=30
  local count=0

  echo "Waiting for bucket '$bucket' to be healthy..."
  until curl -s -u $CB_ADMIN:$CB_PASSWORD http://couchbase:8091/pools/default/buckets/$bucket \
    | grep -q '"healthy"'; do

    count=$((count+1))
    if [ "$count" -ge "$max_retries" ]; then
      echo "ERROR: Bucket '$bucket' did not become healthy after $((max_retries*2)) seconds."
      exit 1
    fi

    echo "Bucket not healthy yet. Retrying $count/$max_retries..."
    sleep 2
  done

  echo "Bucket '$bucket' is healthy."
}

# Function to build deferred indexes with retries
build_indexes() {
  local max_retries=10
  local count=0
  local success=0

  while [ $count -lt $max_retries ]; do
    echo "Attempt $((count+1)) to build indexes..."
    cbq -e $CBQ_HOST -u $CB_ADMIN -p $CB_PASSWORD -s \
      "BUILD INDEX ON \`$BUCKET_NAME\`.\`data\`.\`Reports\` (primary_index, upload_id_index, composite_data_stream_index);" && success=1

    if [ $success -eq 1 ]; then
      echo "Indexes built successfully."
      return 0
    fi

    count=$((count+1))
    echo "Index build failed (rebalance?), retrying in 10s..."
    sleep 10
  done

  echo "ERROR: Failed to build indexes after $max_retries attempts."
  exit 1
}

if [ ! -e "/home/couchbase_initialized" ] ; then
  echo "Initializing the cluster..."
  couchbase-cli cluster-init --cluster "couchbase://couchbase" --cluster-name "couchbase" \
    --cluster-username "$CB_ADMIN" --cluster-password "$CB_PASSWORD" --services "data,index,query" \
    --cluster-ramsize 500 --cluster-index-ramsize 256 --index-storage-setting "memopt"

  # Create bucket if it doesn't exist
  cb_bucket_exists=$(curl -s -u $CB_ADMIN:$CB_PASSWORD http://couchbase:8091/pools/default/buckets | grep -c "$BUCKET_NAME")
  if [ "$cb_bucket_exists" -eq 0 ]; then
    echo "Creating bucket '$BUCKET_NAME'..."
    couchbase-cli bucket-create -c couchbase:8091 -u $CB_ADMIN -p $CB_PASSWORD \
      --bucket=$BUCKET_NAME --bucket-type=couchbase --bucket-ramsize=100
  fi

  # Wait for the bucket to be healthy
  wait_for_bucket "$BUCKET_NAME"

  # Add a scope
  echo "Adding scope 'data'..."
  curl -s -u $CB_ADMIN:$CB_PASSWORD -X POST \
    http://couchbase:8091/pools/default/buckets/$BUCKET_NAME/scopes \
    -d name=data

  # Add collections under the scope
  echo "Adding collection 'Reports' under scope 'data'..."
  curl -s -u $CB_ADMIN:$CB_PASSWORD -X POST \
    http://couchbase:8091/pools/default/buckets/$BUCKET_NAME/scopes/data/collections \
    -d name=Reports

  echo "Adding collection 'Reports-DeadLetter' under scope 'data'..."
  curl -s -u $CB_ADMIN:$CB_PASSWORD -X POST \
    http://couchbase:8091/pools/default/buckets/$BUCKET_NAME/scopes/data/collections \
    -d name=Reports-DeadLetter

  echo "Adding collection 'NotificationSubscriptions' under scope 'data'..."
  curl -s -u $CB_ADMIN:$CB_PASSWORD -X POST \
    http://couchbase:8091/pools/default/buckets/$BUCKET_NAME/scopes/data/collections \
    -d name=NotificationSubscriptions

  # Wait for Query service to register the bucket/collections
  echo "Waiting 30 seconds for index service to initialize..."
  sleep 30

  # Create deferred indexes
  echo "Creating deferred indexes..."
  cbq -e $CBQ_HOST -u $CB_ADMIN -p $CB_PASSWORD -s \
    "CREATE PRIMARY INDEX primary_index ON \`$BUCKET_NAME\`.\`data\`.\`Reports\` WITH {'defer_build':true};"
  cbq -e $CBQ_HOST -u $CB_ADMIN -p $CB_PASSWORD -s \
    "CREATE INDEX upload_id_index ON \`$BUCKET_NAME\`.\`data\`.\`Reports\`(uploadId) WITH {'defer_build':true};"
  cbq -e $CBQ_HOST -u $CB_ADMIN -p $CB_PASSWORD -s \
    "CREATE INDEX composite_data_stream_index ON \`$BUCKET_NAME\`.\`data\`.\`Reports\`(dataStreamId,dataStreamRoute) WITH {'defer_build':true};"

  # Wait a few seconds before building
  sleep 10

  # Build deferred indexes with retry
  build_indexes

  # Done
  echo "Couchbase Server initialized with indexes."
  echo "Initialized" > /home/couchbase_initialized

  echo "Couchbase setup completed."
else
  echo "Couchbase Server already initialized."
fi
