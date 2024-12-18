import boto3
import logging
from config import load_config  # Import the config loader

# Set up logging
logging.basicConfig(
    level=logging.DEBUG,
    format='%(asctime)s - %(levelname)s - %(message)s'
)
logger = logging.getLogger(__name__)


def test_aws_connection():
    try:
        # Load configuration
        config = load_config()

        # Create SQS client using config values
        sqs = boto3.client(
            'sqs',
            aws_access_key_id=config['aws_access_key_id'],
            aws_secret_access_key=config['aws_secret_access_key'],
            region_name=config['aws_region']
        )

        # Try to list queues as a test
        response = sqs.list_queues()
        logger.info("Successfully connected to AWS SQS")
        logger.info(f"Available queues: {response.get('QueueUrls', [])}")

        # If a specific queue URL is configured, check if it exists
        if 'aws_queue_url' in config:
            if config['aws_queue_url'] in response.get('QueueUrls', []):
                logger.info(f"Configured queue {config['aws_queue_url']} found")
            else:
                logger.warning(f"Configured queue {config['aws_queue_url']} not found in available queues")

    except Exception as e:
        logger.error(f"Error connecting to AWS: {str(e)}", exc_info=True)


if __name__ == "__main__":
    test_aws_connection()
