package apikeystore.repository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;

import apikeystore.model.Key;

public class KeyRepositoryImpl implements KeyRepositoryCustom {

	@Autowired
	MongoTemplate mongoOps;

	@Override
	public void saveKey(Key key) {
		mongoOps.insert(key);
		return;
	}

	@Override
	public void removeKey(Key key) {
		mongoOps.remove(key);
	}

	@Override
	public void drop() {

		mongoOps.dropCollection("apikeystore");
	}
}
