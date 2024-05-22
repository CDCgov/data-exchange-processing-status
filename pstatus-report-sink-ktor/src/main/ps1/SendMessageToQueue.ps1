# Set the parameters for the Service Bus
$connectionString = "Endpoint=sb://ocio-ede-dev-processingstatus-testing.servicebus.windows.net/;SharedAccessKeyName=RootManageSharedAccessKey;SharedAccessKey=EkH3H/i66rBE+xTmTdIqMFmlxzYmsteP7+ASbJMhc5w="
$queueName = "reports-notifications-queue"

# Import the necessary assemblies
Add-Type -AssemblyName "System.ServiceModel"
Add-Type -Path (Join-Path -Path (Split-Path $env:PSModulePath -Parent) -ChildPath 'Azure.ServiceBus\2.1.1\lib\netstandard2.0\Microsoft.Azure.ServiceBus.dll')

# Create a queue client
$queueClient = [Microsoft.Azure.ServiceBus.QueueClient]::new($connectionString, $queueName)

# Create a message
$messageBody = "Hello, Azure Service Bus!"
$message = [Microsoft.Azure.ServiceBus.Message]::new([System.Text.Encoding]::UTF8.GetBytes($messageBody))

# Send the message to the queue
$queueClient.SendAsync($message).GetAwaiter().GetResult()

# Print confirmation
Write-Output "Sent message: $messageBody"

# Close the client
$queueClient.CloseAsync().GetAwaiter().GetResult()
