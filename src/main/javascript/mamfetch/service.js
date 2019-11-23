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


function initializeMam() {    
	try {
		Mam.init(provider);	
	} catch (error) {
		console.log('MAM initialize error', error);
	}
};	

function fetchLastMessage() {
	try {
		const response = Mam.fetch(root, mode, key);
			response.then(resolve => {
			return resolve.nextRoot;
		});
	} catch (error) {
		console.log('Mam fetch error', error);
		return null;
	}
}

function sleep(milliseconds) {
   return new Promise(resolve => setTimeout(resolve, milliseconds));
}

async function publishData(client) {
	console.log('A new connection has been established');
	
	client.on('end', function() {
		console.log('Closing connection with the client');
	});

	client.on('error', function(error) {
		console.log(`Error: ${error}`);
	})
	
	for (var i = 0; i < 10; i++) {
		client.write(`Hello ${i}\r\n`);
		await sleep(1000);
	}
}

const server = net.createServer(publishData);

server.listen(listeningPort, localhost, () => { 
	console.log(`Server listening for connection on port ${listeningPort}`);
});
