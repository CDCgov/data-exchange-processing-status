import asyncio
import uuid
import time
import requests
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

pstatus_base_url=env["pstatus_api_base_url"]

async def send_single_message(sender, message):
    # Create a Service Bus message and send it to the queue
    message = ServiceBusMessage(message)
    await sender.send_messages(message)

def send_start_trace(trace_id, parent_span_id, stage_name):
    url = f'{pstatus_base_url}/api/trace/addSpan/{trace_id}/{parent_span_id}?stageName={stage_name}&spanMark=start'
    requests.put(url)

def send_stop_trace(trace_id, parent_span_id, stage_name):
    url = f'{pstatus_base_url}/api/trace/addSpan/{trace_id}/{parent_span_id}?stageName={stage_name}&spanMark=stop'
    requests.put(url)

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
            # Create the trace for this upload
            url = f'{pstatus_base_url}/api/trace?uploadId={upload_id}&destinationId=dex-testing&eventType=test-event1'
            response = requests.post(url)
            response_json = response.json()
            trace_id = response_json["trace_id"]
            parent_span_id = response_json["span_id"]

            # Send the start trace
            send_start_trace(trace_id, parent_span_id, "dex-upload")

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
            # Send the stop trace
            send_stop_trace(trace_id, parent_span_id, "dex-upload")

            # Send the start trace
            send_start_trace(trace_id, parent_span_id, "dex-routing")

            # Send routing message
            print("Sending simulated ROUTING report...")
            message = reports.create_routing(upload_id)
            #print(f"Sending: {message}")
            await send_single_message(sender, message)
            # Send the stop trace
            send_stop_trace(trace_id, parent_span_id, "dex-routing")

            # Send the start trace
            send_start_trace(trace_id, parent_span_id, "dex-hl7-validation")
            # Send hl7 validation messages
            print("Sending simulated HL7-VALIDATION reports...")
            num_lines = 2
            for index in range(num_lines):
                line = index + 1
                message = reports.create_hl7_validation(upload_id, line)
                #print(f"Sending: {message}")
                await send_single_message(sender, message)
            # Send the stop trace
            send_stop_trace(trace_id, parent_span_id, "dex-hl7-validation")

asyncio.run(run())
print("Done sending messages")