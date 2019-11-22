const net = require('net');
const Mam = require('@iota/mam');
const Converter = require('@iota/converter');

if (!process.argv[2]) return console.log('Missing Argument: A listening port for the service is requried!');
const listeningPort = process.argv[2];

if (!process.argv[3]) return console.log('Missing Argument: An URL for a public iota node provider is required to start the service!');
const provider = process.argv[3];

const localhost = '127.0.0.1';


const initializeMam = function() {    
	try {
		Mam.init(provider);	
	} catch (error) {
		console.log('MAM initialize error', error);
	}
};	

const fetchLastMessage = function(root, mode, key) {
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

const echo = function(socket) {
	console.log('echo');
	socket.write('Echo server\r\n');
}

const initializeServer = function() {
	try {
		return net.createServer(echo);
	} catch (error) {
		console.log('Servicer initialize error', error);
		return null;
	}
}

const server = initializeServer();
server.listen(listeningPort, localhost);
console.log('done');