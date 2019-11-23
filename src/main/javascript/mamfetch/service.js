const net = require('net');
const Mam = require('@iota/mam');
const Converter = require('@iota/converter');

if (!process.argv[2]) return console.log('Missing Argument: A listening port for the service is requried!');
const listeningPort = process.argv[2];

if (!process.argv[3]) return console.log('Missing Argument: An URL for a public iota node provider is required to start the service!');
const provider = process.argv[3];

if (!process.argv[4]) return console.log('Missing Argument: The root address of the MAM stream to listen to is required!');
let nextRoot = process.argv[4];

const mode = 'public';
const key = null;
const localhost = '127.0.0.1';

const pollingIntervallMilliseconds = 5 * 1000;
let latestMessage = null;	

function sleep(milliseconds) {
   return new Promise(resolve => setTimeout(resolve, milliseconds));
}

function convertToJson(message) {
	return JSON.parse(Converter.trytesToAscii(message));
}

function fetchLatestMessage(root) {
	console.log(`Fetch latest message from root ${root}`);
	const response = Mam.fetch(root, mode, key);
	
	response.then(resolve => {
		console.log('Resolve response');
		nextRoot = resolve.nextRoot;
		latestMessage = convertToJson(resolve.messages[resolve.messages.length - 1]);
	}).catch(error => { 
		console.log(`Fetch error: ${error}`);
		latestMessage = null;
	});
}

async function waitForLatestMessageToInitialize() {
	console.log('Wait for latest message to be initialized');
	while (!latestMessage) await sleep(500);
	console.log(`Latest message initalized: ${latestMessage}`);
}

function publishLatestMessage(client) {
	if (latestMessage) {
		message = JSON.stringify(latestMessage);
		console.log(`Publish latest message ${message}`);
		client.write(`${message}\r\n`); 
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
	
	fetchLatestMessage(nextRoot);
	await waitForLatestMessageToInitialize();
		
	console.log('Start publishing');
	while (!stop) {
		publishLatestMessage(client);
		fetchLatestMessage(nextRoot);
		await sleep(pollingIntervallMilliseconds);
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
