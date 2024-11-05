import pika
import boto3
import asyncio
import logging
from azure.servicebus.aio import ServiceBusClient
from azure.servicebus import ServiceBusMessage, TransportType
from config import load_config

# Set up logging
logging.basicConfig(
    level=logging.DEBUG,
    format='%(asctime)s - %(levelname)s - %(message)s'
)
logger = logging.getLogger(__name__)

class MessageForwarder:
    def __init__(self, config: dict):
        self.config = config
        self.connection = None
        self.channel = None
        self.should_stop = False

        logger.info(f"Initializing MessageForwarder with target service: {config.get('target_service')}")

        # Initialize target handler based on configuration
        if config.get('target_service') == 'aws':
            logger.info("Setting up AWS client")
            if config.get('use_queue', 'true').lower() == 'true':
                self.sqs = boto3.client(
                    'sqs',
                    aws_access_key_id=config['aws_access_key_id'],
                    aws_secret_access_key=config['aws_secret_access_key'],
                    region_name=config['aws_region']
                )
                logger.info(f"AWS SQS Queue URL: {config.get('aws_queue_url')}")
            else:
                self.sns = boto3.client(
                    'sns',
                    aws_access_key_id=config['aws_access_key_id'],
                    aws_secret_access_key=config['aws_secret_access_key'],
                    region_name=config['aws_region']
                )
                logger.info(f"AWS SNS Topic ARN: {config.get('aws_topic_arn')}")
        elif config.get('target_service') == 'azure':
            logger.info("Setting up Azure Service Bus client")
            self.servicebus_client = ServiceBusClient.from_connection_string(
                conn_str=config['azure_service_bus_connection_str'],
                transport_type=TransportType.AmqpOverWebsocket,
                logging_enable=True
            )

    def connect(self):
        """Establish connection to RabbitMQ"""
        try:
            # Set up RabbitMQ connection with heartbeat and blocked connection timeout
            self.connection = pika.BlockingConnection(
                pika.ConnectionParameters(
                    host=self.config.get('rabbitmq_host', 'localhost'),
                    credentials=pika.PlainCredentials(
                        username=self.config.get('rabbitmq_user', 'guest'),
                        password=self.config.get('rabbitmq_password', 'guest')
                    ),
                    virtual_host=self.config.get('rabbitmq_vhost', '/'),
                    heartbeat=600,
                    blocked_connection_timeout=300
                )
            )
            self.channel = self.connection.channel()

            # Declare queue
            queue_name = self.config.get('queue_name', 'upload_queue')
            self.channel.queue_declare(queue=queue_name, durable=True)

            # Set QoS prefetch count to 1 to ensure fair dispatch
            self.channel.basic_qos(prefetch_count=1)

            logger.info(f"Successfully connected to RabbitMQ and declared queue: {queue_name}")
        except Exception as e:
            logger.error(f"Failed to connect to RabbitMQ: {str(e)}", exc_info=True)
            raise

    async def forward_to_aws(self, message_body: str, delivery_tag: int):
        """Forward message to AWS and handle RabbitMQ acknowledgment"""
        try:
            use_queue = self.config.get('use_queue', 'true').lower() == 'true'

            if use_queue:
                response = self.sqs.send_message(
                    QueueUrl=self.config['aws_queue_url'],
                    MessageBody=message_body,
                    MessageAttributes={
                        'Source': {'DataType': 'String', 'StringValue': 'RabbitMQ-Bridge'}
                    }
                )
                logger.info(f"Message sent to AWS SQS. MessageId: {response['MessageId']}")
            else:
                response = self.sns.publish(
                    TopicArn=self.config['aws_topic_arn'],
                    Message=message_body,
                    MessageAttributes={
                        'Source': {'DataType': 'String', 'StringValue': 'RabbitMQ-Bridge'}
                    }
                )
                logger.info(f"Message published to AWS SNS. MessageId: {response['MessageId']}")

            # Acknowledge and delete the message from RabbitMQ
            self.channel.basic_ack(delivery_tag=delivery_tag)
            logger.info("Message acknowledged and deleted from RabbitMQ")
            return True

        except Exception as e:
            logger.error(f"Error forwarding to AWS: {str(e)}", exc_info=True)
            # Negative acknowledge and requeue the message
            self.channel.basic_nack(delivery_tag=delivery_tag, requeue=True)
            logger.info("Message requeued due to forward failure")
            return False

    async def forward_to_azure(self, message_body: str, delivery_tag: int):
        """Forward message to Azure and handle RabbitMQ acknowledgment"""
        try:
            use_queue = self.config.get('use_queue', 'true').lower() == 'true'

            if use_queue:
                sender = self.servicebus_client.get_queue_sender(
                    queue_name=self.config['azure_queue_name']
                )
            else:
                sender = self.servicebus_client.get_topic_sender(
                    topic_name=self.config['azure_topic_name']
                )

            async with sender:
                await sender.send_messages(ServiceBusMessage(message_body))

            logger.info(f"Message sent to Azure {'Queue' if use_queue else 'Topic'}")

            # Acknowledge and delete the message from RabbitMQ
            self.channel.basic_ack(delivery_tag=delivery_tag)
            logger.info("Message acknowledged and deleted from RabbitMQ")
            return True

        except Exception as e:
            logger.error(f"Error forwarding to Azure: {str(e)}", exc_info=True)
            # Negative acknowledge and requeue the message
            self.channel.basic_nack(delivery_tag=delivery_tag, requeue=True)
            logger.info("Message requeued due to forward failure")
            return False

    def callback(self, ch, method, properties, body):
        """Process received messages and forward to target service"""
        try:
            message_body = body.decode('utf-8')
            logger.info(f"Received message from RabbitMQ with routing key: {method.routing_key}")

            # Create event loop for async operations
            loop = asyncio.new_event_loop()
            asyncio.set_event_loop(loop)

            success = False
            if self.config.get('target_service') == 'aws':
                success = loop.run_until_complete(
                    self.forward_to_aws(message_body, method.delivery_tag)
                )
            elif self.config.get('target_service') == 'azure':
                success = loop.run_until_complete(
                    self.forward_to_azure(message_body, method.delivery_tag)
                )

            loop.close()

            if not success:
                logger.warning("Failed to process message")

        except Exception as e:
            logger.error(f"Error in callback: {str(e)}", exc_info=True)
            # Negative acknowledge and requeue the message
            ch.basic_nack(delivery_tag=method.delivery_tag, requeue=True)
            logger.info("Message requeued due to processing error")

    def run(self):
        """Start consuming messages"""
        try:
            self.connect()
            queue_name = self.config.get('queue_name', 'upload_queue')

            logger.info(f"Starting to consume messages from queue: {queue_name}")
            self.channel.basic_consume(
                queue=queue_name,
                on_message_callback=self.callback
            )

            self.channel.start_consuming()

        except KeyboardInterrupt:
            logger.info("Received shutdown signal")
            self.stop()
        except Exception as e:
            logger.error(f"Error in consumer: {str(e)}", exc_info=True)
            self.stop()

    def stop(self):
        """Stop the consumer and close connections"""
        logger.info("Stopping consumer...")
        if self.channel and not self.channel.is_closed:
            self.channel.stop_consuming()
        if self.connection and not self.connection.is_closed:
            self.connection.close()
        logger.info("Consumer stopped")

def main():
    logger.info("Starting Message Forwarder")

    try:
        config = load_config()
        forwarder = MessageForwarder(config)
        forwarder.run()
    except KeyboardInterrupt:
        logger.info("Shutting down due to keyboard interrupt...")
    except Exception as e:
        logger.error(f"Application error: {str(e)}", exc_info=True)

if __name__ == "__main__":
    main()