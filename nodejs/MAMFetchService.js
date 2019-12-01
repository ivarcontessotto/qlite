const net = require('net');
const Mam = require('@iota/mam');
const Converter = require('@iota/converter');

if (!process.argv[2]) return console.log('Missing Argument: A listening port for the service is requried!');
const listeningPort = process.argv[2];

if (!process.argv[3]) return console.log('Missing Argument: A polling interval in seconds for fetching from the MAM Stream is requried!');
const pollingIntervalMilliseconds = process.argv[3] * 1000;

if (!process.argv[4]) return console.log('Missing Argument: An URL for a public iota node provider is required to start the service!');
const provider = process.argv[4];

if (!process.argv[5]) return console.log('Missing Argument: The mode is required! (public, private, restricted)');
let mode =  process.argv[5];

if (!process.argv[6]) return console.log('Missing Argument: The root address of the MAM stream to listen to is required!');
let nextRoot =  process.argv[6];

let encryptionKey = null;
if (mode == 'restricted') {
    if (!process.argv[7]) return console.log('Missing Argument: For restricted mode an encryption key is required!');
    encryptionKey = process.argv[7];
}

const localhost = '127.0.0.1';

let lastPublishedMessage = null;

function sleep(milliseconds) {
   return new Promise(resolve => setTimeout(resolve, milliseconds));
}

function convertToJsonString(message) {
	return JSON.stringify(JSON.parse(Converter.trytesToAscii(message)));
}

function publishMessage(client, message) {
	if (message) {
		console.log(`Publish message ${message}`);
		client.write(`${message}\r\n`);
		lastPublishedMessage = message;
	}
}

function publishLastPublishedMessageAgain(client) {
    console.log('Publish last sent message again');
    publishMessage(client, lastPublishedMessage);
}

function fetchNewMessageAndPublish(root, client) {
	console.log(`Fetch latest message from root ${root}`);
	const response = Mam.fetch(root, mode, encryptionKey);
	
	response.then(resolve => {
		console.log('Resolve response');
		
		console.log(`nextRoot: ${resolve.nextRoot}`);	
		nextRoot = resolve.nextRoot;
		
		if (resolve.messages.length != 0) {
			console.log('Resolving messages');
			newMessage = convertToJsonString(resolve.messages[resolve.messages.length - 1]);
			console.log(`New message found: ${newMessage}`);
			publishMessage(client, newMessage);
		}
		else {
			console.log('No new message yet.');
		}
	});

	response.catch(error => {
		console.log(`Resolve error: ${error}`);
	});
}

async function onClientConnection(client) {
	console.log('Client connection established');
	
	let stop = false;
	
	client.on('end', function() {
		console.log('Client connection closed');
		stop = true;
	});

	client.on('error', function(error) {
		console.log(`Client connection error: ${error}`);
		stop = true;
	})
	
	publishLastPublishedMessageAgain(client);
		
	console.log('Start publishing');
	while (!stop) {
		fetchNewMessageAndPublish(nextRoot, client);
		await sleep(pollingIntervalMilliseconds);
	}
}

console.log('Init mam state');
Mam.init(provider);

console.log('Init server');
const server = net.createServer(onClientConnection);

server.on('error', function(error) { console.log(`Unhandled error: ${error}`) })

server.listen(listeningPort, localhost, () => { 
	console.log(`Server listening for connection on port ${listeningPort}`);
});
