const net = require('net');
const Mam = require('@iota/mam');
const Converter = require('@iota/converter');

if (!process.argv[2]) return console.log('Missing Argument: A listening port for the service is requried!');
const listeningPort = process.argv[2];

if (!process.argv[3]) return console.log('Missing Argument: An URL for a public iota node provider is required to start the service!');
const provider = process.argv[3];

if (!process.argv[4]) return console.log('Missing Argument: The root address of the MAM stream to listen to is required!');
const root = process.argv[4];

const mode = 'public';
const key = null;
const localhost = '127.0.0.1';

let latestMessage = null;	

function convertToJsonString(message) {
	return JSON.stringify(JSON.parse(Converter.trytesToAscii(message)));
}

function fetchLatestMessage() {
	const response = Mam.fetch(root, mode, key);
	response.then(resolve => {
		latestMessage = convertToJsonString(resolve.messages[resolve.messages.length - 1]);
	});
}

function sleep(milliseconds) {
   return new Promise(resolve => setTimeout(resolve, milliseconds));
}

async function publishLatestMessage(client) {
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
	
	while (!stop) {
		client.write(`${latestMessage}\r\n`); // Todo do this in mam callback
		await sleep(1000);
	}
}

try {
	console.log('init mam');
	Mam.init(provider);
	console.log('fetch latest message');
	fetchLatestMessage();
	// Todo subscribe to mam updates and save them to latest message variable.
	// Todo send latest message to client when changed

	const server = net.createServer(publishLatestMessage);
	server.listen(listeningPort, localhost, () => { 
		console.log(`Server listening for connection on port ${listeningPort}`);
	});

} catch (error) {
	console.log(`Unhanldled error: ${error}`);
}
