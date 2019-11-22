const Mam = require('@iota/mam');
const IotaConverter = require('@iota/converter');

const provider = 'https://nodes.devnet.iota.org:443';
const root = 'QZIFJWSFOXPMWNDUXSFSOOAZFANHCNSOFWEVLYKMLUA9ZVSRLCQ99QYJ9PTUMTWPDTLALGIBHUNTZUAYN';
const mode = 'public';
const key = null;

try {
      Mam.init(provider);
	  
      const convertAndCollect = result => { 
	    let jsonArray = JSON.parse(IotaConverter.trytesToAscii(result));
		for (var i = 0; i < jsonArray.length; i++) {
			console.log('pritning array')
			console.log(jsonArray[i]);
		}
	  }
	  
      let response = Mam.fetch(root, mode, key, convertAndCollect);
	 

} catch (error) {
      console.log('MAM fetch error', error);
}