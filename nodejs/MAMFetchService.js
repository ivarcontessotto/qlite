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
let latestMessage = null;
	

function sleep(milliseconds) {
   return new Promise(resolve => setTimeout(resolve, milliseconds));
}

function convertToJsonString(message) {
	return JSON.stringify(JSON.parse(Converter.trytesToAscii(message)));
}

function fetchLatestMessage(root) {
	console.log(`Fetch latest message from root ${root}`);
	const response = Mam.fetch(root, mode, key);
	
	response.then(resolve => {
		console.log('Resolve response');
		
		console.log(`nextRoot: ${resolve.nextRoot}`);	
		nextRoot = resolve.nextRoot;
		
		if (resolve.messages.length != 0) {
			console.log('Resolving messages');
			latestMessage = convertToJsonString(resolve.messages[resolve.messages.length - 1]);
			console.log(`New latest message: ${latestMessage}`);			
		}
		else {
			console.log('No new messages yet.');
		}
	}).catch(error => { 
		console.log(`Resolve error: ${error}`);
	});
}

async function initializeLatestMessage() {
	lastSentMessage = null; // Reset this in case of a reconnect to receive the last sent message again.
	
	if (latestMessage) {
		console.log(`Latest message already known: ${latestMessage}`);
		return;
	}
	
	fetchLatestMessage(nextRoot);
	console.log('Wait for latest message to be initialized');
	while (!latestMessage) await sleep(500);
	console.log(`Latest message initalized: ${latestMessage}`);
}

function publishLatestMessage(client) {
	if (latestMessage && latestMessage != lastSentMessage) {
		console.log(`Publish latest message ${latestMessage}`);
		client.write(`${latestMessage}\r\n`);
		lastSentMessage = latestMessage;
	}
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
	
	await initializeLatestMessage();
		
	console.log('Start publishing');
	while (!stop) {
		publishLatestMessage(client);
		fetchLatestMessage(nextRoot);
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
