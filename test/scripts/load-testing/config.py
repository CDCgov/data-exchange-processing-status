import os
import logging

logger = logging.getLogger(__name__)

def load_config():
    """
    Load configuration from .env file.
    Handles format: KEY="value"
    """
    config = {}

    try:
        # Get the directory where config.py is located
        current_dir = os.path.dirname(os.path.abspath(__file__))
        # Go up one level to find .env

        with open("../.env") as envfile:
            for line in envfile:
                line = line.strip()

                # Skip empty lines and comments
                if not line or line.startswith('#'):
                    continue

                # Parse KEY="value" format
                if '=' in line:
                    key, value = line.split('=', 1)
                    key = key.strip()
                    value = value.strip()

                    # Remove quotes if present
                    if value.startswith('"') and value.endswith('"'):
                        value = value[1:-1]

                    config[key] = value

        logger.debug("Configuration loaded successfully")
        # Log config without sensitive information
        safe_config = {
            k: v if not any(x in k.lower() for x in ['password', 'key', 'secret', 'connection'])
            else '***' for k, v in config.items()
        }
        logger.debug(f"Configuration: {safe_config}")

        return config

    except Exception as e:
        logger.error(f"Failed to load configuration: {str(e)}", exc_info=True)
        raise

if __name__ == "__main__":
    # Set up logging
    logging.basicConfig(
        level=logging.DEBUG,
        format='%(asctime)s - %(levelname)s - %(message)s'
    )

    try:
        config = load_config()
        logger.info("Configuration loaded successfully")

    except Exception as e:
        logger.error(f"Configuration error: {str(e)}")