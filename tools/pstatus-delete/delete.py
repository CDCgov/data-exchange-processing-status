import sys
import os
import concurrent.futures
from time import time, strftime, localtime

from azure.cosmos import CosmosClient, exceptions


class CosmosDBDeleter:
    def __init__(self, database_name, container_name):
        # Read from environment variables
        endpoint = os.getenv('COSMOS_ENDPOINT')
        key = os.getenv('COSMOS_KEY')

        if not endpoint or not key:
            raise ValueError("Please set the COSMOS_ENDPOINT and COSMOS_KEY environment variables.")

        self.client = CosmosClient(endpoint, key)
        self.database = self.client.get_database_client(database_name)
        self.container = self.database.get_container_client(container_name)

    def get_timestring(self, epoch = None):
        if epoch:
            return strftime('%Y-%m-%d %H:%M:%S', localtime(epoch))
        else:
            return strftime('%Y-%m-%d %H:%M:%S', localtime())

    def delete_by_id(self, item_id):
        try:
            query1 = "SELECT value count(1) FROM c WHERE c.dataStreamId = @dataStreamId"
            parameters = [{"name": "@dataStreamId", "value": item_id}]
            items1 = list(
                self.container.query_items(query=query1, parameters=parameters, enable_cross_partition_query=True)
            )

            if items1:
                print(f"Number of rows in run: {items1[0]}")
            else:
                print("Connection established but no items found in the container.")

            query = "SELECT top 5000 c.id, c.uploadId FROM c WHERE c.dataStreamId = @dataStreamId"
            parameters = [{"name": "@dataStreamId", "value": item_id}]
            deleted_count = 0

            while True:
                items = list(self.container.query_items(
                    query=query,
                    parameters=parameters,
                    enable_cross_partition_query=True,
                    #max_item_count=3000
                ))

                if not items:
                    break
                max_workers = 30
                # Use ThreadPoolExecutor for multithreading
                with concurrent.futures.ThreadPoolExecutor(max_workers=max_workers) as executor:
                    futures = {executor.submit(self.delete_item, item): item for item in items}

                    for future in concurrent.futures.as_completed(futures):
                        item = futures[future]
                        try:
                            future.result()  # Get the result to raise any exceptions
                            deleted_count += 1  # Increment the counter on successful deletion
                        except exceptions.CosmosHttpResponseError as e:
                            print(f"Failed to delete item with id {item['id']}: {e.message}")
                        except Exception as e:
                            print(f"An unexpected error occurred while deleting item {item['id']}: {str(e)}")

                print(f"Deleted {deleted_count} items so far.")

            print(f"Total deleted items: {deleted_count} for id: {item_id}")

        except exceptions.CosmosHttpResponseError as e:
            print(f"An error occurred: {e.message}")
        except Exception as e:
            print(f"An unexpected error occurred: {str(e)}")

    def delete_item(self, item):
        """Helper method to delete an item."""
        self.container.delete_item(item['id'], partition_key=item['uploadId'])

    def dry_run_delete(self, item_id):
        """
        Simulates deletion of items from the container with the specified id.
        """
        query1 = "SELECT value count(1) FROM c WHERE c.dataStreamId = @dataStreamId"
        parameters = [{"name": "@dataStreamId", "value": item_id}]
        items = list(
            self.container.query_items(query=query1, parameters=parameters, enable_cross_partition_query=True)
        )

        if items:
            print(f"Number of rows: {items[0]}")
        else:
            print("Connection established but no items found in the container.")

        query = "SELECT top 5000 c.id FROM c WHERE c.dataStreamId = @dataStreamId"
        parameters = [{"name": "@dataStreamId", "value": item_id}]
        deleted_count = 0

        while True:
            items = list(self.container.query_items(
                query=query,
                parameters=parameters,
                enable_cross_partition_query=True,
                #max_item_count=1000
            ))

            if not items:
                break

            for item in items:
                deleted_count += 1

            #if 'continuation' in items and items['continuation']:
             #   continuation_token = items['continuation']
            #else:
             #   break

        print(f"Would delete {deleted_count} item(s) with id: {item_id}")


if __name__ == "__main__":
    if len(sys.argv) < 2:
        # Example usage
        print("Usage: python delete.py <item_id> [dry-run]")
    else:
        database_name = "ProcessingStatus"
        container_name = "Reports"
        item_id = sys.argv[1]
        dry_run = "dry-run" in sys.argv  # Check if dry run flag is provided
        deleter = CosmosDBDeleter(database_name, container_name)

        if dry_run:
            deleter.dry_run_delete(item_id)
        else:
            start_time = time()

            print(f"Starting deleting of documents at {deleter.get_timestring(start_time)} . . .")
            deleter.delete_by_id(item_id)
            end_time = time()
            duration = end_time - start_time
            print(f"deleted documents in {duration} seconds")
