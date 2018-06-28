package apikeystore.repository;

import apikeystore.model.Key;

public interface KeyRepositoryCustom {

	public void saveKey(Key key);

	public void removeKey(Key key);

	public void drop();
}
