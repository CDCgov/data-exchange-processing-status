import asyncio
import uuid
import time
from datetime import datetime, timezone
import pika
import reports
from config import load_config

############################################################################################
# -- INSTRUCTIONS --
# 1. Open the file named ".env" in the same folder as this Python script.
# 2. In the .env file, set the variables according to your environment and desired settings.
############################################################################################

class RabbitMQProducer:
    """Handles publishing messages to RabbitMQ"""
    def __init__(self, config: dict):
        self.config = config
        self.connection = None
        self.channel = None
        self.routing_keys = [
            "metadata.verify",
            "upload.started",
            "upload.status",
            "upload.completed",
            "upload.routed"
        ]

    def initialize(self):
        """Initialize RabbitMQ connection and channel"""
        try:
            # Set up RabbitMQ connection
            self.connection = pika.BlockingConnection(
                pika.ConnectionParameters(
                    host=self.config.get('rabbitmq_host', 'localhost'),
                    credentials=pika.PlainCredentials(
                        username=self.config.get('rabbitmq_user', 'guest'),
                        password=self.config.get('rabbitmq_password', 'guest')
                    ),
                    virtual_host=self.config.get('rabbitmq_vhost', '/')
                )
            )
            self.channel = self.connection.channel()

            # Declare exchange
            exchange_name = self.config.get('rabbitmq_exchange_name', 'upload_exchange')
            self.channel.exchange_declare(
                exchange=exchange_name,
                exchange_type='direct',
                durable=True
            )

            # Declare queue and bindings
            queue_name = self.config.get('queue_name', 'upload_queue')
            self.channel.queue_declare(queue=queue_name, durable=True)

            # Create bindings for all routing keys
            for routing_key in self.routing_keys:
                self.channel.queue_bind(
                    exchange=exchange_name,
                    queue=queue_name,
                    routing_key=routing_key
                )

            print(f"Successfully connected to RabbitMQ and set up exchange '{exchange_name}' and queue '{queue_name}'")

        except Exception as e:
            print(f"Failed to initialize RabbitMQ connection: {str(e)}")
            raise

    def publish_message(self, message: str, routing_key: str):
        """Publish a message to RabbitMQ"""
        try:
            self.channel.basic_publish(
                exchange=self.config.get('rabbitmq_exchange_name', 'upload_exchange'),
                routing_key=routing_key,
                body=message,
                properties=pika.BasicProperties(
                    delivery_mode=2,  # persistent
                    content_type='application/json'
                )
            )
            print(f"Published message to RabbitMQ with routing key: {routing_key}")
        except Exception as e:
            print(f"Error publishing message: {str(e)}")
            raise

    def close(self):
        """Close the RabbitMQ connection"""
        if self.connection and not self.connection.is_closed:
            self.connection.close()

async def simulate_upload(producer: RabbitMQProducer, upload_id: str, dex_ingest_datetime: str):
    """Simulate the upload process by sending various status messages"""
    print(f"Upload ID = {upload_id}")
    print("Sending simulated messages...")

    try:
        # Send metadata verify
        print("Sending METADATA-VERIFY report...")
        message = reports.create_metadata_verify(upload_id, dex_ingest_datetime)
        producer.publish_message(message, "metadata.verify")

        # Send upload started
        print("Sending UPLOAD-STARTED report...")
        message = reports.create_upload_started(upload_id, dex_ingest_datetime)
        producer.publish_message(message, "upload.started")

        # Send upload status messages
        num_chunks = 4
        size = 27472691
        for index in range(num_chunks):
            offset = int((index + 1) * size / num_chunks)
            print(f"Sending UPLOAD-STATUS ({offset} of {size} bytes) report...")
            message = reports.create_upload_status(
                upload_id, dex_ingest_datetime, offset, size
            )
            producer.publish_message(message, "upload.status")
            await asyncio.sleep(1)

        # Send upload completed
        print("Sending UPLOAD-COMPLETED report...")
        message = reports.create_upload_completed(upload_id, dex_ingest_datetime)
        producer.publish_message(message, "upload.completed")

        # Send routing message
        print("Sending UPLOAD-ROUTED report...")
        message = reports.create_routing(upload_id, dex_ingest_datetime)
        producer.publish_message(message, "upload.routed")

    except Exception as e:
        print(f"Error during simulation: {str(e)}")
        raise

async def main():
    """Main entry point"""
    try:
        # Load configuration
        config = load_config()

        # Initialize producer
        producer = RabbitMQProducer(config)
        producer.initialize()

        try:
            # Generate upload ID and timestamp
            upload_id = str(uuid.uuid4())
            dex_ingest_datetime = datetime.now(timezone.utc).replace(microsecond=0).isoformat().replace("+00:00", "Z")

            # Run simulation
            await simulate_upload(producer, upload_id, dex_ingest_datetime)
            print("Successfully completed message simulation")

        finally:
            producer.close()

    except Exception as e:
        print(f"Application error: {str(e)}")
        raise

if __name__ == "__main__":
    asyncio.run(main())