package apikeystore.repository;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import apikeystore.model.Key;
import apikeystore.model.OAuth2AccessToken;
import apikeystore.model.OAuth2RefreshToken;
import apikeystore.model.Resource;
import apikeystore.repository.KeyRepository;

@RunWith(SpringRunner.class)
@SpringBootTest
public class KeyRepositoryImplTest {

	@Autowired(required = true)
	private KeyRepository keyRepository;

	private long now = (new Date().getTime()) / 1000;
	private String uid = "Test_ID_" + now;
	private String cid = "Test_Client_ID";
	private String[] verbs = { "GET", "POST" };
	private Set<String> verbset = new HashSet<>(Arrays.asList(verbs));
	private Resource scope = new Resource("path/", verbset);
	private Set<Resource> scopes = new HashSet<Resource>(Arrays.asList(scope));
	private String atVal = "Test_Access_Token";
	private String rtVal = "Test_Refresh_Token";
	private OAuth2AccessToken acc = new OAuth2AccessToken(atVal, now, scopes);
	private OAuth2RefreshToken ref = new OAuth2RefreshToken(rtVal, now);
	private Map<String, String> atts = createMap();

	private HashMap<String, String> createMap() {
		HashMap<String, String> temp = new HashMap<String, String>();
		temp.put("Sample_Att_Key", "Sample_Att_Val");
		return temp;
	}

	private Key testKey = new Key(acc, ref, uid, cid, -1, atts);
	private Key testKey2 = new Key(acc, ref, uid + "2", cid, -1, atts);

	@Test
	public void testFindBeforeAdd() {
		Key result = keyRepository.findById(uid + "__" + cid);
		assertNull(result);
	}

	@Test
	public void testAddFindDeleteKey() {

		keyRepository.saveKey(testKey);

		Key result;
		result = keyRepository.findById(uid + "__" + cid);
		assertEquals(result, testKey);

		keyRepository.removeKey(testKey);

		result = keyRepository.findById(uid + "__" + cid);
		assertNull(result);
	}

	@Test
	public void testFindAll() {

		// Test key should NOT exist in results
		List<Key> results = keyRepository.findAll();
		assertFalse(results.contains(testKey));

		keyRepository.saveKey(testKey);

		// Test key should exist in results
		results = keyRepository.findAll();
		System.out.println(testKey.toString());
		for (Key key : results)
			System.out.println(key.toString());
		assertTrue(testKey.equals(testKey));
		assertTrue(results.contains(testKey));

		keyRepository.removeKey(testKey);

		// Test key should NOT exist in results
		results = keyRepository.findAll();
		assertFalse(results.contains(testKey));
	}

	@Test
	public void testFindAllByClientID() {
		// Test keys should NOT exist in results
		List<Key> results = keyRepository.findAll();
		assertFalse(results.contains(testKey));
		assertFalse(results.contains(testKey2));

		keyRepository.saveKey(testKey);
		keyRepository.saveKey(testKey2);

		// Test keys should exist in results
		results = keyRepository.findAllByClientId(cid);
		assertTrue(results.contains(testKey));
		assertTrue(results.contains(testKey2));

		keyRepository.removeKey(testKey);

		// Test key 1 should NOT exist in results, but test key 2 SHOULD
		results = keyRepository.findAll();
		assertFalse(results.contains(testKey));
		assertTrue(results.contains(testKey2));

		keyRepository.removeKey(testKey2);

		// Test keys should NOT exist in results
		results = keyRepository.findAll();
		assertFalse(results.contains(testKey));
		assertFalse(results.contains(testKey2));
	}

	@Test
	public void testFindByAuthTokenValue() {
		// Test key should NOT exist in results
		Key result = keyRepository.findByAuthTokenValue(atVal);
		assertNull(result);

		keyRepository.saveKey(testKey);

		//
		result = keyRepository.findByAuthTokenValue(atVal);
		assertNotNull(result);
		assertTrue(atVal.equals(result.getAuthToken().getValue()));

		keyRepository.removeKey(testKey);

		// Test key should NOT exist in results
		result = keyRepository.findByAuthTokenValue(atVal);
		assertNull(result);
	}

	@Test
	public void testFindByRefTokenValue() {
		// Test key should NOT exist in results
		Key result = keyRepository.findByRefTokenValue(rtVal);
		assertNull(result);

		keyRepository.saveKey(testKey);

		//
		result = keyRepository.findByRefTokenValue(rtVal);
		assertNotNull(result);
		assertTrue(rtVal.equals(result.getRefToken().getValue()));

		keyRepository.removeKey(testKey);

		// Test key should NOT exist in results
		result = keyRepository.findByRefTokenValue(rtVal);
		assertNull(result);
	}
}
