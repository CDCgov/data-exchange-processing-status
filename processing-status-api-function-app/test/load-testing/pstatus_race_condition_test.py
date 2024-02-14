import asyncio
import uuid
import requests
import time
import json

############################################################################################
# -- INSTRUCTIONS --
# 1. Open the file named ".env" in the same folder as this Python script.
# 2. In the .env file, set the variables according to your environment and desired settings.
############################################################################################

env = {}
with open(".env") as envfile:
    for line in envfile:
        name, var = line.partition("=")[::2]
        var = var.strip()
        if var.startswith('"') and var.endswith('"'):
            var = var[1:-1]
        env[name.strip()] = var

pstatus_base_url=env["pstatus_api_base_url"]

def send_start_trace(trace_id, parent_span_id, stage_name):
    url = f'{pstatus_base_url}/api/trace/startSpan/{trace_id}/{parent_span_id}?stageName={stage_name}'
    r = requests.put(url)
    json_response = r.json()
    return json_response['span_id']

def send_stop_trace(trace_id, span_id):
    url = f'{pstatus_base_url}/api/trace/stopSpan/{trace_id}/{span_id}'
    requests.put(url)

async def run():
    # Generate a unqiue upload ID
    upload_id = str(uuid.uuid4())
    print("Upload ID = " + upload_id)

    # Create the trace for this upload
    url = f'{pstatus_base_url}/api/trace?uploadId={upload_id}&destinationId=dex-testing&eventType=test-event1'
    response = requests.post(url)
    response_json = response.json()
    trace_id = response_json["trace_id"]
    parent_span_id = response_json["span_id"]

    # Send the start trace
    span_id = send_start_trace(trace_id, parent_span_id, "dex-upload")

    # Immediately try to get the span we just created
    print('Attempting to get the span just created')
    start_time = time.time()

    url = f'{pstatus_base_url}/api/trace/span?uploadId={upload_id}&stageName=dex-upload'
    r = requests.get(url)
    print(f'status code = {r.status_code}')
    if r.status_code != 200:
        print(f'bad response = {r.content}')
        exit(1)
    json_object = json.loads(r.content)
    json_formatted_str = json.dumps(json_object, indent=2)
    print(f'JSON response: {json_formatted_str}')
    print('Waited %s seconds' % (time.time() - start_time))

    # Send the stop trace
    send_stop_trace(trace_id, span_id)

asyncio.run(run())
print("Done sending messages")