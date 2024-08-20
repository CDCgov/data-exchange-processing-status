import asyncio
import uuid
import time
from datetime import datetime, timezone
import reports
from azure.servicebus.aio import ServiceBusClient
from azure.servicebus import ServiceBusMessage
from azure.servicebus import TransportType

############################################################################################
# -- INSTRUCTIONS --
# 1. Open the file named ".env" in the same folder as this Python script.
# 2. In the .env file, set the variables according to your environment and desired settings.
############################################################################################

# Queue name is always the same regardless of environment
QUEUE_NAME = "processing-status-cosmos-db-report-sink-queue"
TOPIC_NAME = "processing-status-cosmos-db-report-sink-topics"
USE_QUEUE = True

env = {}
with open("../.env") as envfile:
    for line in envfile:
        name, var = line.partition("=")[::2]
        var = var.strip()
        if var.startswith('"') and var.endswith('"'):
            var = var[1:-1]
        env[name.strip()] = var

async def send_single_message(sender, message):
    # Create a Service Bus message and send it to the queue
    message = ServiceBusMessage(message)
    await sender.send_messages(message)

async def run():
    # Generate a unqiue upload ID
    upload_id = str(uuid.uuid4())
    dex_ingest_datetime = datetime.now(timezone.utc).replace(microsecond=0).isoformat().replace("+00:00", "Z")
    print("Upload ID = " + upload_id)
    print("Sending simulated messages via the service bus...")

    # create a Service Bus client using the connection string
    async with ServiceBusClient.from_connection_string(
        conn_str=env["service_bus_connection_str"],
        transport_type=TransportType.AmqpOverWebsocket,
        logging_enable=False) as servicebus_client:
        if USE_QUEUE == True:
            # Get a Queue Sender object to send messages to the queue
            sender = servicebus_client.get_queue_sender(queue_name=QUEUE_NAME)
            async with sender:
                await simulate(sender, upload_id, dex_ingest_datetime)
        else:
            # Get a Topic Sender object to send messages to the queue
            sender = servicebus_client.get_topic_sender(topic_name=TOPIC_NAME)
            async with sender:
                await simulate(sender, upload_id, dex_ingest_datetime)

async def simulate(sender, upload_id, dex_ingest_datetime):
        # Send upload messages
        print("Sending simulated UPLOAD reports...")
        num_chunks = 4
        size = 27472691
        for index in range(num_chunks):
            offset = (index+1) * size / num_chunks
            message = reports.create_upload(upload_id, dex_ingest_datetime, offset, size)
            #print(f"Sending: {message}")
            await send_single_message(sender, message)
            time.sleep(1)

        # Send routing message
        print("Sending simulated ROUTING report...")
        message = reports.create_routing(upload_id, dex_ingest_datetime)
        #print(f"Sending: {message}")
        await send_single_message(sender, message)

        print("Sending simulated HL7 DEBATCH report...")
        message = reports.create_hl7_debatch_report(upload_id, dex_ingest_datetime)
        # print("Sending message: " + message)
        await send_single_message(sender, message)

        # Send hl7 validation messages
        print("Sending simulated HL7-VALIDATION reports...")
        batch_message = await sender.create_message_batch()
        num_lines = 1
        for index in range(num_lines):
            line = index + 1
            message = reports.create_hl7_validation(upload_id, dex_ingest_datetime, line)
            #print(f"Sending: {message}")
            batch_message.add_message(ServiceBusMessage(message))
        await sender.send_messages(batch_message)

asyncio.run(run())
print("Done sending messages")