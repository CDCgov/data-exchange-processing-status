import random
import asyncio
import uuid
from datetime import datetime, timezone, timedelta
import pika
import boto3
from azure.servicebus.aio import ServiceBusClient
from azure.servicebus import ServiceBusMessage, TransportType
from abc import ABC, abstractmethod
import reports
from config import load_config
from pika.exceptions import (
    ChannelClosedByBroker
)

############################################################################################
# -- INSTRUCTIONS --
# 1. Open the file named ".env" in the same folder as this Python script.
# 2. In the .env file, set the variables according to your environment and desired settings.
############################################################################################

class MessageSystem(ABC):
    """Abstract base class for message systems"""

    @abstractmethod
    async def initialize(self):
        """Initialize the message system connection"""
        pass

    @abstractmethod
    async def send_message(self, message: str):
        """Send a message to the message system"""
        pass

    @abstractmethod
    async def close(self):
        """Close the message system connection"""
        pass


class RabbitMQSystem(MessageSystem):
    def __init__(self, config: dict):
        self.config = config
        self.connection = None
        self.channel = None
        self.queue_name = config.get('rabbitmq_queue_name')
        self.routing_key = config.get('rabbitmq_routing_key')
        self.exchange_name = config.get('rabbitmq_exchange_name')
        self.vhost = config.get('rabbitmq_vhost', '/')

    async def initialize(self):
        try:
            # Setup connection
            self.connection = pika.BlockingConnection(
                pika.ConnectionParameters(
                    host=self.config.get('rabbitmq_host', 'localhost'),
                    credentials=pika.PlainCredentials(
                        username=self.config.get('rabbitmq_user', 'guest'),
                        password=self.config.get('rabbitmq_password', 'guest')
                    ),
                    virtual_host=self.vhost
                )
            )
            self.channel = self.connection.channel()

            # Setup exchange
            try:
                # Check if exchange exists by declaring it passively
                self.channel.exchange_declare(
                    exchange=self.exchange_name,
                    exchange_type='direct',
                    passive=True
                )
                print(f"Exchange {self.exchange_name} already exists")
            except pika.exceptions.ChannelClosedByBroker:
                # Re-open channel as it was closed by the failed passive declaration
                self.channel = self.connection.channel()
                # Create exchange as it doesn't exist
                self.channel.exchange_declare(
                    exchange=self.exchange_name,
                    exchange_type='direct',
                    durable=True,
                    auto_delete=False
                )
                print(f"Created exchange {self.exchange_name}")

            # Setup queue
            try:
                # Check if queue exists by declaring it passively
                self.channel.queue_declare(
                    queue=self.queue_name,
                    passive=True
                )
                print(f"Queue {self.queue_name} already exists")
            except pika.exceptions.ChannelClosedByBroker:
                # Re-open channel as it was closed by the failed passive declaration
                self.channel = self.connection.channel()
                # Create queue as it doesn't exist
                self.channel.queue_declare(
                    queue=self.queue_name,
                    durable=True,
                    auto_delete=False,
                    arguments={
                        'x-queue-type': 'classic'
                    }
                )
                print(f"Created queue {self.queue_name}")

            # Ensure binding exists (idempotent operation, safe to repeat)
            self.channel.queue_bind(
                exchange=self.exchange_name,
                queue=self.queue_name,
                routing_key=self.routing_key
            )
            print(f"Ensured binding between exchange {self.exchange_name} and queue {self.queue_name}")

            # Set channel prefetch count (optional, for better load distribution)
            self.channel.basic_qos(prefetch_count=1)

        except Exception as e:
            print(f"Failed to initialize RabbitMQ: {str(e)}")
            if self.connection and not self.connection.is_closed:
                self.connection.close()
            raise

    async def send_message(self, message: str):
        try:
            if self.channel is None or self.channel.is_closed:
                print("Channel is closed, attempting to reconnect...")
                await self.initialize()

            self.channel.basic_publish(
                exchange=self.exchange_name,
                routing_key=self.routing_key,
                body=message,
                properties=pika.BasicProperties(
                    delivery_mode=2,  # Make message persistent
                    content_type='application/json'
                ),
                mandatory=True  # Ensure message is routable
            )
        except pika.exceptions.UnroutableError:
            print("Message was returned as unroutable")
            raise
        except pika.exceptions.AMQPConnectionError as e:
            print(f"Connection error while sending message: {str(e)}")
            await self.initialize()  # Try to reconnect
            raise
        except Exception as e:
            print(f"Failed to send message: {str(e)}")
            raise

    async def close(self):
        try:
            if self.channel and not self.channel.is_closed:
                try:
                    # Ensure all messages are delivered before closing
                    self.channel.close()
                except Exception as e:
                    print(f"Error closing channel: {str(e)}")

            if self.connection and not self.connection.is_closed:
                try:
                    self.connection.close()
                except Exception as e:
                    print(f"Error closing connection: {str(e)}")

            print("RabbitMQ connection closed")
        except Exception as e:
            print(f"Error during cleanup: {str(e)}")
            raise

    def check_connection(self) -> bool:
        """Check if connection is still valid"""
        return (
                self.connection is not None
                and not self.connection.is_closed
                and self.channel is not None
                and not self.channel.is_closed
        )

    async def ensure_connection(self):
        """Ensure connection is valid, reconnect if needed"""
        if not self.check_connection():
            print("Connection lost, reconnecting...")
            await self.initialize()


class AWSSystem(MessageSystem):
    def __init__(self, config: dict):
        self.use_queue = config.get('use_queue', 'true').lower() == 'true'
        self.sqs = boto3.client(
            'sqs',
            region_name=config['aws_region']
        )
        self.sns = boto3.client(
            'sns',
            region_name=config['aws_region']
        )
        self.queue_url = config['aws_queue_url']
        self.topic_arn = config['aws_topic_arn']

    async def initialize(self):
        print(f"AWS initialized with {'queue' if self.use_queue else 'topic'}")

    async def send_message(self, message: str):
        if self.use_queue:
            self.sqs.send_message(
                QueueUrl=self.queue_url,
                MessageBody=message
            )
        else:
            self.sns.publish(
                TopicArn=self.topic_arn,
                Message=message
            )

    async def close(self):
        pass


class AzureSystem(MessageSystem):
    def __init__(self, config: dict):
        self.connection_str = config['azure_service_bus_connection_str']
        self.use_queue = config.get('use_queue', 'true').lower() == 'true'
        self.queue_name = config.get('azure_queue_name')
        self.topic_name = config.get('azure_topic_name')
        self.client = None
        self.sender = None
        self.messages_sent = 0

    async def initialize(self):
        try:
            # Create new client instance
            self.client = ServiceBusClient.from_connection_string(
                conn_str=self.connection_str,
                transport_type=TransportType.AmqpOverWebsocket
            )
            # Create sender at initialization
            if self.use_queue:
                self.sender = self.client.get_queue_sender(queue_name=self.queue_name)
            else:
                self.sender = self.client.get_topic_sender(topic_name=self.topic_name)

            print(f"Azure Service Bus initialized with {'queue' if self.use_queue else 'topic'}")

        except Exception as e:
            print(f"Failed to initialize Azure Service Bus: {str(e)}")
            raise

    async def send_message(self, message: str):
        try:
            # Create a new message object
            message_obj = ServiceBusMessage(message)

            # Send message using the existing sender
            await self.sender.send_messages(message_obj)

            self.messages_sent += 1
            print(f"Message {self.messages_sent} sent to {'queue' if self.use_queue else 'topic'}: "
                  f"{self.queue_name if self.use_queue else self.topic_name}")

        except Exception as e:
            print(f"Failed to send message {self.messages_sent + 1}: {str(e)}")
            # Try to reinitialize and resend
            try:
                await self.initialize()
                await self.sender.send_messages(ServiceBusMessage(message))
                self.messages_sent += 1
                print(f"Message {self.messages_sent} sent after reconnection")
            except Exception as retry_error:
                print(f"Failed to send message even after reconnection: {str(retry_error)}")
                raise

    async def close(self):
        try:
            if self.sender:
                await self.sender.close()
            if self.client:
                await self.client.close()
            print(f"Azure Service Bus connection closed. Total messages sent: {self.messages_sent}")
        except Exception as e:
            print(f"Error closing Azure Service Bus connection: {str(e)}")
            raise


def random_datetime_in_future(relative_datetime):
    random_milliseconds = random.randint(100, 3000)
    # Generate a future datetime
    future_datetime = relative_datetime + timedelta(milliseconds=random_milliseconds)
    # print(f'random_datetime_in_future: relative_datetime: {relative_datetime}, future_datetime: {future_datetime}')
    return future_datetime

async def simulate(message_system: MessageSystem):
    # Generate upload ID and timestamp
    upload_id = str(uuid.uuid4())
    start_datetime = datetime.now(timezone.utc)
    dex_ingest_datetime = start_datetime.replace(microsecond=0).isoformat().replace("+00:00", "Z")
    print(f"Upload ID = {upload_id}")

    print("Sending simulated reports...")

    # Send metadata verify
    print("Sending METADATA-VERIFY report...")
    end_datetime = random_datetime_in_future(start_datetime)
    message = reports.create_metadata_verify(upload_id, dex_ingest_datetime, start_datetime, end_datetime)
    await message_system.send_message(message)

    # Send upload started
    print("Sending UPLOAD-STARTED report...")
    start_datetime = end_datetime
    end_datetime = random_datetime_in_future(start_datetime)
    message = reports.create_upload_started(upload_id, dex_ingest_datetime, start_datetime, end_datetime)
    await message_system.send_message(message)

    # Send upload status messages
    num_chunks = 4
    size = 27472691
    for index in range(num_chunks):
        offset = int((index + 1) * size / num_chunks)
        print(f"Sending UPLOAD-STATUS ({offset} of {size} bytes) report...")
        start_datetime = end_datetime
        end_datetime = random_datetime_in_future(start_datetime)
        message = reports.create_upload_status(upload_id, dex_ingest_datetime, start_datetime, end_datetime, offset, size)
        await message_system.send_message(message)
        await asyncio.sleep(0.1)

    # Send upload completed
    print("Sending UPLOAD-COMPLETED report...")
    start_datetime = end_datetime
    end_datetime = random_datetime_in_future(start_datetime)
    message = reports.create_upload_completed(upload_id, dex_ingest_datetime, start_datetime, end_datetime)
    await message_system.send_message(message)

    # Send routing message
    print("Sending UPLOAD-ROUTED report...")
    start_datetime = end_datetime
    end_datetime = random_datetime_in_future(start_datetime)
    message = reports.create_routing(upload_id, dex_ingest_datetime, start_datetime, end_datetime)
    await message_system.send_message(message)


async def run():
    # Load configuration using the config module
    config = load_config()

    # Create appropriate message system based on configuration
    message_system = None
    system_type = config.get('message_system', 'azure').lower()

    try:
        if system_type == 'rabbitmq':
            message_system = RabbitMQSystem(config)
        elif system_type == 'aws':
            message_system = AWSSystem(config)
        elif system_type == 'azure':
            message_system = AzureSystem(config)
        else:
            raise ValueError(f"Unsupported message system: {system_type}")

        # Initialize the message system
        await message_system.initialize()

        print(f"Using message system: {system_type}")

        # Run simulation
        await simulate(message_system)

    finally:
        if message_system:
            await message_system.close()


asyncio.run(run())
print("Done sending messages")
