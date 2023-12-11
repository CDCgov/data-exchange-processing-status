import asyncio
import uuid
import time
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
QUEUE_NAME = "processing-status-cosmos-db-queue"

env = {}
with open(".env") as envfile:
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
    print("Upload ID = " + upload_id)
    print("Sending simulated messages via the service bus...")

    conn_str1=env["service_bus_connection_str"]
    print("sb str = " + conn_str1)

    # create a Service Bus client using the connection string
    async with ServiceBusClient.from_connection_string(
        conn_str=env["service_bus_connection_str"],
        transport_type=TransportType.AmqpOverWebsocket,
        logging_enable=False) as servicebus_client:
        # Get a Queue Sender object to send messages to the queue
        sender = servicebus_client.get_queue_sender(queue_name=QUEUE_NAME)
        async with sender:
            # Send upload messages
            print("Sending simulated UPLOAD reports...")
            num_chunks = 3
            size = 27472691
            for index in range(num_chunks):
                offset = (index+1) * size / num_chunks 
                message = reports.create_upload(upload_id, offset, size)
                #print(f"Sending: {message}")
                await send_single_message(sender, message)
                time.sleep(1)
            # Send routing message
            print("Sending simulated ROUTING report...")
            message = reports.create_routing(upload_id)
            #print(f"Sending: {message}")
            await send_single_message(sender, message)

            # Send hl7 validation messages
            print("Sending simulated HL7-VALIDATION reports...")
            num_lines = 100
            for index in range(num_lines):
                line = index + 1
                message = reports.create_hl7_validation(upload_id, line)
                #print(f"Sending: {message}")
                await send_single_message(sender, message)

asyncio.run(run())
print("Done sending messages")
