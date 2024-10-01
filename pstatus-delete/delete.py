import os
import sys
import os

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

    def delete_by_id(self, item_id):
        """
        Deletes items from the container with the specified id.

        :param item_id: The id of the items to delete.
        """
        try:
            # Query items with the specified id
            query = f"SELECT c.id,c.uploadId FROM c WHERE c.dataStreamId = @dataStreamId"
            parameters = [{"name": "@dataStreamId", "value": item_id}]
            items = list(self.container.query_items(query=query, parameters=parameters, enable_cross_partition_query=True))
            deleted_count = 0

            for item in items:
                print(f"Deleting item with id: {item['id']}")
                self.container.delete_item(item['id'], partition_key=item['uploadId'])
                deleted_count += 1  # Increment the counter


            if deleted_count > 0:
                print(f"Deleted {deleted_count} item(s) with id: {item_id}")
            else:
                print(f"No items found with id: {item_id}")

        except exceptions.CosmosHttpResponseError as e:
            print(f"An error occurred: {e.message}")
        except Exception as e:
            print(f"An unexpected error occurred: {str(e)}")


if __name__ == "__main__":

    if len(sys.argv) < 1:
        # Example usage
        print("Usage: python delete.py <item_id>")
    else:
      database_name ="ProcessingStatus"
      container_name = "Reports"
      item_id = sys.argv[1]
      deleter = CosmosDBDeleter(database_name, container_name)

      deleter.delete_by_id(item_id)