const net = require('net');
const Mam = require('@iota/mam');
const Converter = require('@iota/converter');

if (!process.argv[2]) return console.log('Missing Argument: A listening port for the service is requried!');
const listeningPort = process.argv[2];

if (!process.argv[3]) return console.log('Missing Argument: A polling interval in seconds for fetching from the MAM Stream is requried!');
const pollingIntervalMilliseconds = process.argv[3] * 1000;

if (!process.argv[4]) return console.log('Missing Argument: An URL for a public iota node provider is required to start the service!');
const provider = process.argv[4];

if (!process.argv[5]) return console.log('Missing Argument: The root address of the MAM stream to listen to is required!');
let nextRoot =  process.argv[5];

const mode = 'public';
const key = null;
const localhost = '127.0.0.1';

let lastSentMessage = null;
let newMessage = null;
	

function sleep(milliseconds) {
   return new Promise(resolve => setTimeout(resolve, milliseconds));
}

function convertToJsonString(message) {
	return JSON.stringify(JSON.parse(Converter.trytesToAscii(message)));
}

function fetchNewMessage(root) {
	console.log(`Fetch latest message from root ${root}`);
	const response = Mam.fetch(root, mode, key);
	
	response.then(resolve => {
		console.log('Resolve response');
		
		console.log(`nextRoot: ${resolve.nextRoot}`);	
		nextRoot = resolve.nextRoot;
		
		if (resolve.messages.length != 0) {
			console.log('Resolving messages');
			newMessage = convertToJsonString(resolve.messages[resolve.messages.length - 1]);
			console.log(`New message: ${newMessage}`);
		}
		else {
		    newMessage = null;
			console.log('No new message yet.');
		}
	}).catch(error => { 
		newMessage = null;
		console.log(`Resolve error: ${error}`);
	});
}

function publishNewMessage(client) {
	if (newMessage) {
	    temp = newMessage // Cache it because newMessage  can be written by async operation.
		console.log(`Publish message ${temp}`);
		client.write(`${temp}\r\n`);
		newMessage = null;
		lastSentMessage = temp;
	}
}

function publishLastSentMessageAgain(client) {
    console.log('Publish last sent message again');
    newMessage = lastSentMessage;
    publishNewMessage(client);
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
	
	publishLastSentMessageAgain(client);
		
	console.log('Start publishing');
	while (!stop) {
		publishNewMessage(client);
		fetchNewMessage(nextRoot);
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
