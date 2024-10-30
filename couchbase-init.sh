#!/bin/bash

#sh ./entrypoint.sh
#(exec "./entrypoint.sh")
#/bin/sh ./entrypoint.sh
#echo "Running original entrypoint.sh script..."
#. /entrypoint.sh
#/entrypoint.sh "$@"
#/entrypoint.sh couchbase-server &

# Wait for Couchbase Server to start
until curl -s -u admin:password http://couchbase:8091/pools >/dev/null; do
  echo "Waiting for Couchbase to start..."
  sleep 5
done

echo "Couchbase is up and running5."

if [ ! -e "/home/couchbase_initialized" ] ; then
  echo "Initializing the cluster..."
  couchbase-cli cluster-init --cluster "couchbase://couchbase" --cluster-name "couchbase" \
    --cluster-username "admin" --cluster-password "password" --services "data,index,query" \
    --cluster-ramsize 500 --cluster-index-ramsize 256 --index-storage-setting "memopt"

  echo "1"

  # Create a bucket if it doesn't exist
  cb_bucket_exists=$(curl -s -u admin:password http://couchbase:8091/pools/default/buckets | grep -c "ProcessingStatus")
  if [ "$cb_bucket_exists" -eq 0 ]; then
    echo "Creating bucket 'ProcessingStatus'..."
    couchbase-cli bucket-create -c couchbase:8091 -u admin -p password \
      --bucket=ProcessingStatus --bucket-type=couchbase --bucket-ramsize=100
  fi

  echo "2"

  # Add a scope
  echo "Adding scope 'data'..."
  curl -s -u admin:password -X POST \
    http://couchbase:8091/pools/default/buckets/ProcessingStatus/scopes \
    -d name=data

  # Add a collection under the scope
  echo "Adding collection 'Reports' under scope 'data'..."
  curl -s -u admin:password -X POST \
    http://couchbase:8091/pools/default/buckets/ProcessingStatus/scopes/data/collections \
    -d name=Reports

  # Add a collection under the scope
  echo "Adding collection 'Reports-DeadLetter' under scope 'data'..."
  curl -s -u admin:password -X POST \
    http://couchbase:8091/pools/default/buckets/ProcessingStatus/scopes/data/collections \
    -d name=Reports-DeadLetter

  # Done
  echo "Couchbase Server initialized."
  echo "Initialized `date +"%D %T"`" > /home/couchbase_initialized

  echo "Couchbase setup completed."
else
  echo "Couchbase Server already initialized."
fi
