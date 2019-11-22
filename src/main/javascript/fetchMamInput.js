const Mam = require('@iota/mam');
const IotaConverter = require('@iota/converter');

const provider = 'https://nodes.devnet.iota.org:443';
const root = 'QZIFJWSFOXPMWNDUXSFSOOAZFANHCNSOFWEVLYKMLUA9ZVSRLCQ99QYJ9PTUMTWPDTLALGIBHUNTZUAYN';
const mode = 'public';
const key = null;

try {
	
	Mam.init(provider);
	
	let response = Mam.fetch(root, mode, key);
	
	response.then(resolve => {
		
		console.log('Next Root: ' + resolve.nextRoot);
		console.log('')
		
		for (var i = 0; i < resolve.messages.length; i++) {
			console.log('Message ' + i)
			console.log('')
			
			let jsonArray = JSON.parse(IotaConverter.trytesToAscii(resolve.messages[i]));
			jsonArray.forEach(json => {
				console.log(json)
				console.log('')
			})
		}
	})
	

} catch (error) {
	console.log('MAM fetch error', error);
}