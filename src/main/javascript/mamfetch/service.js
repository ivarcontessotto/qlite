const Mam = require('@iota/mam');
const Converter = require('@iota/converter')

var fetchLastMessage = function(provider, root, mode, key) {
	
    try {
		Mam.init(provider);
		
		let response = Mam.fetch(root, mode, key);
		
		response.then(resolve => {
			return resolve.nextRoot;
		});
		
	} catch (error) {
		return 'MAM fetch error';
	}
};