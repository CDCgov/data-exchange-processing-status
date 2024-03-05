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
with open("../.env") as envfile:
    for line in envfile:
        name, var = line.partition("=")[::2]
        var = var.strip()
        if var.startswith('"') and var.endswith('"'):
            var = var[1:-1]
        env[name.strip()] = var

pstatus_base_url=env["pstatus_api_base_url"]

async def run():
    ### Test 1
    print('TEST 1: Attempting to a trace just created using trace_id')

    # Generate a unqiue upload ID
    upload_id = str(uuid.uuid4())
    print("Upload ID = " + upload_id)

    # Create the trace for this upload
    url = f'{pstatus_base_url}/api/trace?uploadId={upload_id}&destinationId=dex-testing&eventType=test-event1'
    response = requests.post(url)
    response_json = response.json()
    trace_id = response_json["trace_id"]
    print(f'trace_id = {trace_id}')

    # Immediately try to get the trace we just created
    start_time = time.time()
    url = f'{pstatus_base_url}/api/trace/traceId/{trace_id}'
    r = requests.get(url)
    print(f'status code = {r.status_code}')
    if r.status_code != 200:
        print(f'FAILED: bad response = {r.content}')
    else:
        json_object = json.loads(r.content)
        json_formatted_str = json.dumps(json_object, indent=2)
        print(f'SUCCESS: JSON response: {json_formatted_str}')
        print('Waited %s seconds' % (time.time() - start_time))

    ### Test 2
    print('TEST 2: Attempting to get a trace just created using upload_id')

    # Generate a unqiue upload ID
    upload_id = str(uuid.uuid4())
    print("Upload ID = " + upload_id)

    # Create the trace for this upload
    url = f'{pstatus_base_url}/api/trace?uploadId={upload_id}&destinationId=dex-testing&eventType=test-event1'
    requests.post(url)

    # Immediately try to get the trace we just created
    start_time = time.time()
    url = f'{pstatus_base_url}/api/trace/uploadId/{upload_id}'
    r = requests.get(url)
    print(f'status code = {r.status_code}')
    if r.status_code != 200:
        print(f'FAILED: bad response = {r.content}')
    else:
        json_object = json.loads(r.content)
        json_formatted_str = json.dumps(json_object, indent=2)
        print(f'SUCCESS: JSON response: {json_formatted_str}')
        print('Waited %s seconds' % (time.time() - start_time))

asyncio.run(run())