# Overview
The `pstatus_load_test.py` script enables us to send messages to the respective Messaging System based on the configurations. 

### Supported Messaging Systems
- RabbitMQ
- AWS (SQS Queue/ SNS Topic)
- Azure (Service Bus Queue/ Topic)

### Environment Variable Setup
Set the following environment variables
#### Configuration to specify which cloud provider to use
The message_system can either be `aws` or `azure` or `rabbitmq`.
- `message_system` - The cloud provider we will be using (Set to either 'aws' or 'azure' based on which service provider we want to connect to).

#### Configuration to specify whether to send to the queue or topic
The use_queue is true for queues(AWS/Azure) AND false for topics (AWS/Azure).
The use_queue is true for RabbitMQ.
- `use_queue` - A string value set to either 'true' or 'false' to indicate whether to use a queue or a topic. Default value is set to 'true'.

#### RabbbitMQ
For RabbitMQ(Local Runs) only,  set the following environment variables:
- `rabbitmq_host` - if not provided, `localhost` will be used.
- `rabbitmq_user` - if not provided, default `guest` will be used.
- `rabbitmq_password` - if not provided, default `guest` will be used.
- `rabbitmq_vhost` - if not provided, default virtual host `/` will be used.
- `rabbitmq_exchange_name` - Your RabbitMQ exchange name.
- `rabbitmq_queue_name` - Your RabbitMQ queue name bound to the desired exchange topic.
- `rabbitmq_routing_key` - Your RabbitMQ routing key.

#### Azure Service Bus
For Azure Service Bus only, set the following environment variables:
- `azure_service_bus_connection_str` - Your service bus connection string.
- `azure_queue_name` - Your service bus queue name.
- `azure_topic_name` - Your service bus topic name.
  
#### AWS 
For AWS SNS/SQS only, set the following environment variables:
- `aws_access_key_id` - The Access Key ID for an IAM user with permissions to receive and delete messages from specified SQS queue.
- `aws_secret_access_key` - The secret access key for an IAM user with permissions to receive and delete messages from the specified SQS queue. This key is used for authentication and secure access to the queue.
- `aws_region` - The AWS region where your SQS queue is located.
- `aws_queue_url` - URL of the Amazon Simple Queue Service(SQS) queue.
- `aws_topic_arn` - Value of the Amazon Simple Notification Service(SNS) topic.

### Steps to run the scripts
Steps to set up and send messages to AWS(SQS/SNS) or Azure Service Bus (Queue/Topic) from the Python Script files:
- Install Python.
- Please install all the dependencies from the requirements.txt file.
- Run the following command, 'python .\rabbitmq_consumer.py' in a Terminal or in Windows powershell.
