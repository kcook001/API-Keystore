package apikeystore.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.Collections;
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
import org.springframework.data.domain.Page;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit4.SpringRunner;

import apikeystore.controller.KeyController;
import apikeystore.exception.ATExpiredException;
import apikeystore.exception.AddFailureException;
import apikeystore.exception.BadParameterException;
import apikeystore.exception.DoesNotExistException;
import apikeystore.exception.KeyExpiredException;
import apikeystore.exception.MissingRequiredParameterException;
import apikeystore.exception.RTExpiredException;
import apikeystore.exception.TokenDoesNotExistException;
import apikeystore.model.Key;
import apikeystore.model.KeyRequest;
import apikeystore.model.OAuth2AccessToken;
import apikeystore.model.OAuth2RefreshToken;
import apikeystore.model.Resource;

@RunWith(SpringRunner.class)
@SpringBootTest
public class KeyControllerTest {

	@Autowired(required = true)
	private KeyController controller;

	private long now = (new Date().getTime()) / 1000;
	private String uid = "Test_ID_" + now;
	private String[] verbs = { "GET", "POST", "PUT" };
	private Set<String> verbSet = new HashSet<>(Arrays.asList(verbs));
	private Resource scope1 = new Resource("samplePath/", verbSet);
	private Resource scope2 = new Resource("samplePath/subPath/", verbSet);;
	private Resource scope3 = new Resource("samplePath/{param}", verbSet);
	private Resource scopesArr[] = { scope1, scope2, scope3 };
	private Set<Resource> scopes = new HashSet<Resource>(Arrays.asList(scopesArr));

	private OAuth2AccessToken acc = new OAuth2AccessToken("Test_Access_Token", now + 300, scopes);
	private OAuth2AccessToken acc2 = new OAuth2AccessToken("Test_Access_Token_2", now + 300, scopes);
	private OAuth2AccessToken exAcc = new OAuth2AccessToken("EX_Test_Access_Token", 0, scopes);

	private OAuth2RefreshToken ref = new OAuth2RefreshToken("Test_Refresh_Token", now + 3000);
	private OAuth2RefreshToken exRef = new OAuth2RefreshToken("EX_Test_Refresh_Token", 0);

	private Map<String, String> atts = createMap("SampleAgencyCode");
	private Map<String, String> atts2 = createMap("OtherSampleAgencyCode");

	private HashMap<String, String> createMap(String agencyCode) {
		HashMap<String, String> temp = new HashMap<String, String>();
		temp.put("sample_Att_Key", "Sample_Att_Val");
		temp.put("agencyCode", agencyCode);
		return temp;
	}

	private Key testKey = new Key(acc, ref, uid, "Test_Client_ID", -1, atts);
	private Key testKey2 = new Key(acc2, ref, uid, "Test_Client_ID", -1, atts);
	private Key testKey3 = new Key(acc, ref, uid + "_3", "Test_Client_ID", -1, atts);
	private Key testKey4 = new Key(acc, ref, uid + "_4", "Test_Client_2_ID", -1, atts);
	private Key testKey5 = new Key(acc, ref, uid, "Test_Client_3_ID", -1, atts);
	private Key testKey6 = new Key(acc, ref, uid + "_6", "Test_Client_2_ID", -1, atts2);
	private Key testKey7 = new Key(acc, ref, uid + "_7", "Test_Client_2_ID", -1, atts2);
	private Key expTestKey = new Key(exAcc, exRef, "EX_" + uid, "EX_Test_Client_ID", -1, atts);
	private Key refTestKey = new Key(exAcc, ref, "Ref_" + uid, "Ref_Test_Client_ID", -1, atts);

	private String testJwt = new String(
			"eyJhbGciOiJIUzI1NiJ9.eyJhdXRoVG9rZW5WYWx1ZSI6IlRlc3RfSldUX1Rva2VuIiwiYXV0aFRva2VuRXhwaXJhdGlvbiI6MTYwMDAwMDAwMDAsImF1dGhUb2tlblNjb3BlIjpbeyJyZXNvdXJjZSI6InNhbXBsZVBhdGgvIiwidmVyYnMiOlsiR0VUIiwiUFVUIl19LHsicmVzb3VyY2UiOiJzYW1wbGVQYXRoMi8iLCJ2ZXJicyI6WyJERUxFVEUiLCJQT1NUIl19XSwiYXV0aFRva2VuVHlwZSI6IkJFQVJFUiIsImF1dGhUb2tlbkV4cGlyZWQiOmZhbHNlLCJyZWZUb2tlblZhbHVlIjoiVGVzdF9KV1RfUmVmX1Rva2VuIiwicmVmVG9rZW5FeHBpcmF0aW9uIjoxNjAwMDAwMDAwMCwicmVmVG9rZW5FeHBpcmVkIjpmYWxzZSwidXNlcklkIjoiVGVzdF9KV1RfVXNlcl9JRCIsImNsaWVudElkIjoiVGVzdF9KV1RfQ2xpZW50X0lEIiwiYXR0cmlidXRlcyI6eyJhbm90aGVyQXR0cmlidXRlIjoiV2hhdF9Hb2VzX0hlcmUiLCJhZ2VuY3lDb2RlIjoiU2FtcGxlX0FnZW5jeV9Db2RlIn0sImNyZWF0ZWQiOjE1MjEyMzM4NzgsIm1vZGlmaWVkIjoxNTIxMjMzODc4LCJqdGkiOiJUZXN0X0pXVF9Vc2VyX0lEIiwiaWF0IjoxNjAwMDAwMDAwLCJleHAiOjE2MDAwMDAwMDB9.1DTLk65S_ywKDrzi9L-WtnpLVVvSqX3ByUDZ-UYd8sg");
	private String testJwtResult;
	private String testJwtUid = "Test_JWT_User_ID";
	private String testJwtCid = "Test_JWT_Client_ID";

	private Map<String, String> emptyParams = Collections.emptyMap();

	// Test status http return code
	@Test
	public void testStatus() {
		HttpEntity<String> control = new ResponseEntity<String>(HttpStatus.OK);
		HttpEntity<String> resp = controller.status();
		assertEquals(resp, control);
	}

	// Find non-existent key, get exception
	@Test(expected = DoesNotExistException.class)
	public void testFindBeforeAdd() {

		try {
			controller.find(testKey.getUserId(), testKey.getClientId());
		} catch (KeyExpiredException e) {
			fail("Unexpected KeyExpiredException thrown; there shouldn't be any result here.");
			e.printStackTrace();
		} catch (ATExpiredException e) {
			fail("Unexpected ATExpiredException thrown; there shouldn't be any result here.");
			e.printStackTrace();
		}
		fail("Should have thrown a DoesNotExist Exception.");
	}

	// Add key, find it by user and client id, remove it by user and client id,
	// search again and get exception
	@Test(expected = DoesNotExistException.class)
	public void testAddFindRemoveByUidCid() {

		try {
			controller.addKey(testKey);
		} catch (MissingRequiredParameterException e1) {
			fail("Missing required parameters exception; user ID and client ID should not have been null or empty.");
			e1.printStackTrace();
		} catch (AddFailureException e) {
			fail("Unknown error in add method.");
			e.printStackTrace();
		}

		HttpEntity<Key> result = null;

		try {
			result = controller.find(testKey.getUserId(), testKey.getClientId());
		} catch (DoesNotExistException e) {
			fail("DNE exception; key should exist.");
			e.printStackTrace();
		} catch (KeyExpiredException e) {
			fail("Exp exception; key should not be expired.");
			e.printStackTrace();
		} catch (ATExpiredException e) {
			fail("Unexpected ATExpiredException thrown; key AT should not be expired.");
			e.printStackTrace();
		}
		assertNotNull(result.getBody());
		assertEquals(result.getBody(), testKey);

		controller.delete(testKey.getUserId(), testKey.getClientId());

		try {
			result = controller.find(testKey.getUserId(), testKey.getClientId());
		} catch (KeyExpiredException e) {
			fail("Exp exception; key should have been removed.");
			e.printStackTrace();
		} catch (ATExpiredException e) {
			fail("Unexpected ATExpiredException thrown; key should not exist.");
			e.printStackTrace();
		}
		fail("Key should have been removed.");
	}

	// Add key, find it by auth token value, remove it by auth token value, search
	// again and get exception
	@Test(expected = TokenDoesNotExistException.class)
	public void testAddFindRemoveByAuthVal() {

		try {
			controller.addKey(testKey);
		} catch (MissingRequiredParameterException e1) {
			fail("Missing required parameters exception; user ID and client ID should not have been null or empty.");
			e1.printStackTrace();
		} catch (AddFailureException e) {
			fail("Unknown error in add method.");
			e.printStackTrace();
		}

		HttpEntity<Key> result = null;
		HttpEntity<String> jwtResult = null;

		try {
			result = controller.findToken(testKey.getAuthToken().getValue());
		} catch (TokenDoesNotExistException e) {
			fail("DNE exception; key should exist.");
			e.printStackTrace();
		} catch (KeyExpiredException e) {
			fail("Exp exception; key should not be expired.");
			e.printStackTrace();
		} catch (ATExpiredException e) {
			fail("Unexpected ATExpiredException thrown; key AT should not be expired.");
			e.printStackTrace();
		}
		assertNotNull(result.getBody());
		assertEquals(result.getBody(), testKey);

		try {
			jwtResult = controller.findTokenJwt(testKey.getAuthToken().getValue());
		} catch (TokenDoesNotExistException e) {
			fail("DNE exception; key should exist.");
			e.printStackTrace();
		} catch (KeyExpiredException e) {
			fail("Exp exception; key should not be expired.");
			e.printStackTrace();
		} catch (ATExpiredException e) {
			fail("Unexpected ATExpiredException thrown; key AT should not be expired.");
			e.printStackTrace();
		} catch (Exception e) {
			fail("Unexpected exception: something JWT-related went wrong.");
			e.printStackTrace();
		}

		assertNotNull(jwtResult.getBody());

		controller.deleteByAuthValue(testKey.getAuthToken().getValue());

		try {
			result = controller.findToken(testKey.getAuthToken().getValue());
		} catch (KeyExpiredException e) {
			fail("Exp exception; key should have been removed.");
			e.printStackTrace();
		} catch (ATExpiredException e) {
			fail("Unexpected ATExpiredException thrown; key should not exist.");
			e.printStackTrace();
		}
		fail("Key should have been removed.");
	}

	// Add key as a JWT, find it by user and client id (returned as a JWT), remove
	// it, search again and get exception
	@Test(expected = DoesNotExistException.class)
	public void testAddFindRemoveJwt() {

		try {
			controller.addJwt(testJwt);
		} catch (Exception e) {
			fail("Sig exception; JWT Signature should match.");
		}

		HttpEntity<Key> resultObj = null;
		HttpEntity<String> result = null;

		try {
			resultObj = controller.find(testJwtUid, testJwtCid);
		} catch (DoesNotExistException e) {
			fail("DNE exception; key should exist.");
			e.printStackTrace();
		} catch (KeyExpiredException e) {
			fail("Exp exception; key should not be expired.");
			e.printStackTrace();
		} catch (ATExpiredException e) {
			fail("Unexpected ATExpiredException thrown; key AT should not be expired.");
			e.printStackTrace();
		}
		assertNotNull(resultObj.getBody());

		try {
			testJwtResult = controller.createJwt(resultObj.getBody());
		} catch (Exception e) {
			fail("Sig exception; JWT Signature should match.");
		}

		try {
			result = controller.findJwt(testJwtUid, testJwtCid);
		} catch (DoesNotExistException e) {
			fail("DNE exception; key should exist.");
			e.printStackTrace();
		} catch (KeyExpiredException e) {
			fail("Exp exception; key should not be expired.");
			e.printStackTrace();
		} catch (ATExpiredException e) {
			fail("Unexpected ATExpiredException thrown; key AT should not be expired.");
			e.printStackTrace();
		} catch (Exception e) {
			fail("Sig exception; JWT Signature should match.");
		}
		assertNotNull(result.getBody());
		assertEquals(result.getBody(), testJwtResult);

		controller.delete(testJwtUid, testJwtCid);

		try {
			result = controller.findJwt(testJwtUid, testJwtCid);
		} catch (KeyExpiredException e) {
			fail("Exp exception; key should have been removed.");
			e.printStackTrace();
		} catch (DoesNotExistException e) {
			throw e;
		} catch (ATExpiredException e) {
			fail("Unexpected ATExpiredException thrown; key should not exist.");
			e.printStackTrace();
		} catch (Exception e) {
			fail("Exception; unexpected exception.");
		}
		fail("Key should have been removed.");
	}

	// Add expired key, find it, remove it, search and get exception
	@Test(expected = KeyExpiredException.class)
	public void testAddExpiredFindRemove() throws KeyExpiredException {

		try {
			controller.addKey(expTestKey);
		} catch (MissingRequiredParameterException e1) {
			fail("Missing required parameters exception; user ID and client ID should not have been null or empty.");
			e1.printStackTrace();
		} catch (AddFailureException e) {
			fail("Unknown error in add method.");
			e.printStackTrace();
		}

		try {
			controller.find(expTestKey.getUserId(), expTestKey.getClientId());
		} catch (DoesNotExistException e) {
			fail("DNE exception; key should exist.");
			e.printStackTrace();
		} catch (ATExpiredException e) {
			fail("Unexpected ATExpiredException thrown; KeyExpiredException should have been thrown before this.");
			e.printStackTrace();
		}
		fail("Key should be expired.");

	}

	// Add key, see it in find all results, remove it, don't see it in find all
	@Test
	public void testAddFindallRemove() {

		try {
			controller.addKey(testKey);
		} catch (MissingRequiredParameterException e1) {
			fail("Missing required parameters exception; user ID and client ID should not have been null or empty.");
			e1.printStackTrace();
		} catch (AddFailureException e) {
			fail("Unknown error in add method.");
			e.printStackTrace();
		}

		List<Key> results = controller.findAll();
		assertTrue(results.contains(testKey));

		controller.delete(testKey.getUserId(), testKey.getClientId());

		results = controller.findAll();
		assertFalse(results.contains(testKey));
	}

	// Try to delete non-existent key, get exception
	@Test(expected = DoesNotExistException.class)
	public void testDeleteBeforeAdd() {

		controller.delete(testKey.getUserId(), testKey.getClientId());
		fail("Expected DoesNotExistException was not thrown.");
	}

	// Add duplicate key (replace), see updated values
	@Test(expected = DoesNotExistException.class)
	public void testAddUpdate() {

		try {
			controller.addKey(testKey);
		} catch (MissingRequiredParameterException e1) {
			fail("Missing required parameters exception; user ID and client ID should not have been null or empty.");
			e1.printStackTrace();
		} catch (AddFailureException e) {
			fail("Unknown error in add method.");
			e.printStackTrace();
		}

		HttpEntity<Key> result = null;

		try {
			result = controller.find(testKey.getUserId(), testKey.getClientId());
		} catch (DoesNotExistException e) {
			e.printStackTrace();
			fail("DNE exception; key should exist.");
		} catch (KeyExpiredException e) {
			e.printStackTrace();
			fail("Exp exception; key should not be expired.");
		} catch (ATExpiredException e) {
			fail("Unexpected ATExpiredException thrown; key AT should not be expired.");
			e.printStackTrace();
		}
		assertNotNull(result.getBody());
		assertEquals(result.getBody(), testKey);

		try {
			controller.addKey(testKey2);
		} catch (MissingRequiredParameterException e1) {
			fail("Missing required parameters exception; user ID and client ID should not have been null or empty.");
			e1.printStackTrace();
		} catch (AddFailureException e) {
			fail("Unknown error in add method.");
			e.printStackTrace();
		}

		try {
			result = controller.find(testKey2.getUserId(), testKey2.getClientId());
		} catch (DoesNotExistException e) {
			e.printStackTrace();
			fail("DNE exception; key should exist.");
		} catch (KeyExpiredException e) {
			e.printStackTrace();
			fail("Exp exception; key should not be expired.");
		} catch (ATExpiredException e) {
			fail("Unexpected ATExpiredException thrown; key AT should not be expired.");
			e.printStackTrace();
		}
		assertNotNull(result.getBody());
		assertEquals(result.getBody(), testKey2);
		assertNotEquals(result.getBody(), testKey);

		controller.delete(testKey2.getUserId(), testKey2.getClientId());

		try {
			result = controller.find(testKey.getUserId(), testKey.getClientId());
		} catch (KeyExpiredException e) {
			e.printStackTrace();
			fail("Exp exception; key should have been removed.");
		} catch (ATExpiredException e) {
			fail("Unexpected ATExpiredException thrown; key should not exist.");
			e.printStackTrace();
		}
		fail("Key should have been removed.");

	}

	// Add key using Generate method, see it in find all results, remove it, don't
	// see it in find all
	@Test(expected = DoesNotExistException.class)
	public void testAddGeneratedFindRemove() {

		String cid = "Test_Client_ID";
		Set<String> verbs = new HashSet<>();
		verbs.add("GET");
		Resource scope = new Resource("samplePath/", verbs);
		Set<Resource> testScope = new HashSet<Resource>();
		testScope.add(scope);

		KeyRequest creds = new KeyRequest(uid, cid, testScope, atts);

		try {
			controller.generateAndAdd(creds);
		} catch (MissingRequiredParameterException e1) {
			fail("Missing required parameters exception; user ID and client ID should not have been null or empty.");
			e1.printStackTrace();
		} catch (AddFailureException e) {
			fail("Unknown error in add method.");
			e.printStackTrace();
		}

		HttpEntity<Key> resp = null;

		try {
			resp = controller.find(uid, cid);
		} catch (DoesNotExistException e) {
			e.printStackTrace();
			fail("DNE exception; key should exist.");
		} catch (KeyExpiredException e) {
			e.printStackTrace();
			fail("Exp exception; key should not be expired.");
		} catch (ATExpiredException e) {
			fail("Unexpected ATExpiredException thrown; key AT should not be expired.");
			e.printStackTrace();
		}

		Key result = resp.getBody();
		assertTrue(result.getUserId().equals(uid));
		assertTrue(result.getClientId().equals(cid));

		controller.delete(testKey.getUserId(), testKey.getClientId());

		try {
			resp = controller.find(uid, cid);
		} catch (KeyExpiredException e) {
			e.printStackTrace();
			fail("Exp exception; key should not exist.");
		} catch (ATExpiredException e) {
			fail("Unexpected ATExpiredException thrown; key should not exist.");
			e.printStackTrace();
		}
		fail("Key should have been removed.");
	}

	// Add three keys (two with the same client id), see them in find all results,
	// revoke by client id, don't see the keys for that client, remove the remaining
	// key.
	@SuppressWarnings("unchecked")
	@Test
	public void testAddFindallRevokeClient() {

		try {
			controller.addKey(testKey);
			controller.addKey(testKey3);
			controller.addKey(testKey4);
		} catch (MissingRequiredParameterException e1) {
			fail("Missing required parameters exception; user ID and client ID should not have been null or empty.");
			e1.printStackTrace();
		} catch (AddFailureException e) {
			fail("Unknown error in add method.");
			e.printStackTrace();
		}

		List<Key> results = controller.findAll();
		assertTrue(results.contains(testKey));
		assertTrue(results.contains(testKey3));
		assertTrue(results.contains(testKey4));

		try {
			results = controller.findAllFromClientID("Test_Client_ID", emptyParams).getContent();
		} catch (BadParameterException e) {
			fail("Bad parameters exception.");
			e.printStackTrace();
		}
		assertTrue(results.contains(testKey));
		assertTrue(results.contains(testKey3));
		assertFalse(results.contains(testKey4));

		controller.revokeClient("Test_Client_ID");

		results = controller.findAll();
		assertFalse(results.contains(testKey));
		assertFalse(results.contains(testKey3));
		assertTrue(results.contains(testKey4));

		controller.delete(testKey4.getUserId(), testKey4.getClientId());

		results = controller.findAll();
		assertFalse(results.contains(testKey));
		assertFalse(results.contains(testKey3));
		assertFalse(results.contains(testKey4));

		try {
			results = controller.findAllFromClientID(uid, emptyParams).getContent();
		} catch (BadParameterException e) {
			fail("Bad parameters exception.");
			e.printStackTrace();
		}

		assertTrue(results.isEmpty());
	}

	// Add three keys (two with the same user id), see them in find all results,
	@SuppressWarnings("unchecked")
	// revoke by user id, don't see the keys for that user, remove the remaining
	// key.
	@Test
	public void testAddFindallRevokeUser() {

		try {
			controller.addKey(testKey);
			controller.addKey(testKey4);
			controller.addKey(testKey5);
		} catch (MissingRequiredParameterException e1) {
			fail("Missing required parameters exception; user ID and client ID should not have been null or empty.");
			e1.printStackTrace();
		} catch (AddFailureException e) {
			fail("Unknown error in add method.");
			e.printStackTrace();
		}

		List<Key> results = controller.findAll();
		assertTrue(results.contains(testKey));
		assertTrue(results.contains(testKey4));
		assertTrue(results.contains(testKey5));

		try {
			results = controller.findAllFromUserID(uid, emptyParams).getContent();
		} catch (BadParameterException e) {
			fail("Bad parameters exception.");
			e.printStackTrace();
		}
		assertTrue(results.contains(testKey));
		assertFalse(results.contains(testKey4));
		assertTrue(results.contains(testKey5));

		controller.revokeUser(uid);

		results = controller.findAll();
		assertFalse(results.contains(testKey));
		assertFalse(results.contains(testKey5));
		assertTrue(results.contains(testKey4));

		controller.delete(testKey4.getUserId(), testKey4.getClientId());

		results = controller.findAll();
		assertFalse(results.contains(testKey));
		assertFalse(results.contains(testKey3));
		assertFalse(results.contains(testKey4));

		try {
			results = controller.findAllFromUserID(uid, emptyParams).getContent();
		} catch (BadParameterException e) {
			fail("Bad parameters exception.");
			e.printStackTrace();
		}

		assertTrue(results.isEmpty());
	}

	// Add key with expired auth token and valid refresh token,
	// search for it and see that the tokens are refreshed.
	@Test(expected = DoesNotExistException.class)
	public void testRefresh() {
		HttpEntity<Key> result = null;

		try {
			controller.addKey(refTestKey);
		} catch (MissingRequiredParameterException e1) {
			fail("Missing required parameters exception; user ID and client ID should not have been null or empty.");
			e1.printStackTrace();
		} catch (AddFailureException e) {
			fail("Unknown error in add method.");
			e.printStackTrace();
		}

		List<Key> results = controller.findAll();
		assertTrue(results.contains(refTestKey));

		try {
			result = controller.forceRefresh(refTestKey.getRefToken().getValue());
		} catch (DoesNotExistException e) {
			e.printStackTrace();
			fail("DNE exception; key should exist.");
		} catch (KeyExpiredException e) {
			e.printStackTrace();
			fail("Exp exception; key should not be expired.");
		} catch (RTExpiredException e) {
			e.printStackTrace();
			fail("Exp exception; ref token should not be expired.");
		}

		Key updatedKey = result.getBody();

		// Check that the key has been updated (and not removed).
		assertNotNull(updatedKey);
		assertNotEquals(refTestKey, updatedKey);

		// UID, CID, attributes, created timestamp, auth token type and scope should be
		// the same.
		assertEquals(refTestKey.getUserId(), updatedKey.getUserId());
		assertEquals(refTestKey.getClientId(), updatedKey.getClientId());
		assertEquals(refTestKey.getAttributes(), updatedKey.getAttributes());
		assertEquals(refTestKey.getCreated(), updatedKey.getCreated());
		assertEquals(refTestKey.getAuthToken().getTokenType(), updatedKey.getAuthToken().getTokenType());
		// assertEquals(refTestKey.getAuthToken().getScope(),
		// updatedKey.getAuthToken().getScope());

		// Token values should be different.
		assertNotEquals(updatedKey.getAuthToken().getValue(), refTestKey.getAuthToken().getValue());
		assertNotEquals(updatedKey.getRefToken().getValue(), refTestKey.getRefToken().getValue());

		// Token expirations should be newer, modified timestamp may be newer or the
		// same (it's a fast test).
		assertTrue(updatedKey.getModified() >= refTestKey.getModified());
		assertTrue(updatedKey.getAuthToken().getExpiration() > refTestKey.getAuthToken().getExpiration());
		assertTrue(updatedKey.getRefToken().getExpiration() > refTestKey.getRefToken().getExpiration());

		controller.delete(refTestKey.getUserId(), refTestKey.getClientId());

		try {
			result = controller.find(refTestKey.getUserId(), refTestKey.getClientId());
		} catch (KeyExpiredException e) {
			e.printStackTrace();
			fail("Exp exception; key should not exist.");
		} catch (ATExpiredException e) {
			fail("Unexpected ATExpiredException thrown; key should not exist.");
			e.printStackTrace();
		}
		fail("Key should have been removed.");
	}

	// Add three keys (two with the same agency code), see them in find all results,
	// revoke by agency code, don't see the keys for that agency, remove the
	// remaining key.
	@SuppressWarnings("unchecked")
	@Test
	public void testAddFindallRevokeAgency() {

		try {
			controller.addKey(testKey4);
			controller.addKey(testKey6);
			controller.addKey(testKey7);
		} catch (MissingRequiredParameterException e1) {
			fail("Missing required parameters exception; user ID and client ID should not have been null or empty.");
			e1.printStackTrace();
		} catch (AddFailureException e) {
			fail("Unknown error in add method.");
			e.printStackTrace();
		}

		List<Key> results = controller.findAll();
		assertTrue(results.contains(testKey4));
		assertTrue(results.contains(testKey6));
		assertTrue(results.contains(testKey7));

		try {
			results = controller.findAllFromAgencyCode("OtherSampleAgencyCode", emptyParams).getContent();
		} catch (BadParameterException e) {
			fail("Bad parameters exception.");
			e.printStackTrace();
		}
		assertFalse(results.contains(testKey4));
		assertTrue(results.contains(testKey6));
		assertTrue(results.contains(testKey7));

		controller.revokeAgency("OtherSampleAgencyCode");

		results = controller.findAll();
		assertFalse(results.contains(testKey6));
		assertFalse(results.contains(testKey7));
		assertTrue(results.contains(testKey4));

		controller.delete(testKey4.getUserId(), testKey4.getClientId());

		results = controller.findAll();
		assertFalse(results.contains(testKey4));
		assertFalse(results.contains(testKey6));
		assertFalse(results.contains(testKey7));

		try {
			results = controller.findAllFromAgencyCode("OtherSampleAgencyCode", emptyParams).getContent();
		} catch (BadParameterException e) {
			fail("Bad parameters exception.");
			e.printStackTrace();
		}

		assertTrue(results.isEmpty());
	}

	// Add three keys, see them sorted correctly in find all results,
	// remove the keys.
	@SuppressWarnings("unchecked")
	@Test
	public void testAddFindallSorted() {

		Map<String, String> params = new HashMap<>();
		params.put("size", "3");
		params.put("page", "0");
		params.put("sortBy", "clientId");
		params.put("sortOrder", "ASC");
		params.put("clientId", "Test_Client_ID*");

		try {
			controller.addKey(testKey);
			controller.addKey(expTestKey);
			controller.addKey(refTestKey);
		} catch (MissingRequiredParameterException e1) {
			fail("Missing required parameters exception; user ID and client ID should not have been null or empty.");
			e1.printStackTrace();
		} catch (AddFailureException e) {
			fail("Unknown error in add method.");
			e.printStackTrace();
		}

		Page<Key> resultsPage = null;
		try {
			resultsPage = controller.findAll(params);
		} catch (BadParameterException e) {
			fail("Bad parameters exception");
			e.printStackTrace();
		}
		assertTrue(resultsPage.getNumberOfElements() == 3);
		assertTrue(resultsPage.getTotalElements() == 3);
		assertTrue(resultsPage.getTotalPages() == 1);
		assertTrue(resultsPage.getSort().getOrderFor("clientId").isAscending());
		List<Key> results = resultsPage.getContent();
		assertTrue(results.contains(testKey));
		assertTrue(results.contains(expTestKey));
		assertTrue(results.contains(refTestKey));

		params.put("sortOrder", "DESC");
		try {
			resultsPage = controller.findAll(params);
		} catch (BadParameterException e) {
			fail("Bad parameters exception");
			e.printStackTrace();
		}
		assertTrue(resultsPage.getNumberOfElements() == 3);
		assertTrue(resultsPage.getTotalElements() == 3);
		assertTrue(resultsPage.getTotalPages() == 1);
		assertTrue(resultsPage.getSort().getOrderFor("clientId").isDescending());
		results = resultsPage.getContent();
		assertTrue(results.contains(testKey));
		assertTrue(results.contains(expTestKey));
		assertTrue(results.contains(refTestKey));

		controller.delete(testKey.getUserId(), testKey.getClientId());
		controller.delete(expTestKey.getUserId(), expTestKey.getClientId());
		controller.delete(refTestKey.getUserId(), refTestKey.getClientId());

		results = controller.findAll();
		assertFalse(results.contains(testKey));
		assertFalse(results.contains(expTestKey));
		assertFalse(results.contains(refTestKey));

	}

	// Add three keys, see them sorted correctly in find all results,
	// remove the keys.
	@SuppressWarnings("unchecked")
	@Test
	public void testAddFindallMultiSort() {

		Map<String, String> params = new HashMap<>();
		params.put("size", "3");
		params.put("page", "0");
		params.put("sortBy", "clientId");
		params.put("sortOrder", "ASC");
		params.put("clientId", "Test_Client_ID*");

		try {
			controller.addKey(testKey);
			controller.addKey(expTestKey);
			controller.addKey(refTestKey);
		} catch (MissingRequiredParameterException e1) {
			fail("Missing required parameters exception; user ID and client ID should not have been null or empty.");
			e1.printStackTrace();
		} catch (AddFailureException e) {
			fail("Unknown error in add method.");
			e.printStackTrace();
		}

		Page<Key> resultsPage = null;
		try {
			resultsPage = controller.findAll(params);
		} catch (BadParameterException e) {
			fail("Bad parameters exception");
			e.printStackTrace();
		}
		assertTrue(resultsPage.getNumberOfElements() == 3);
		assertTrue(resultsPage.getTotalElements() == 3);
		assertTrue(resultsPage.getTotalPages() == 1);
		assertTrue(resultsPage.getSort().getOrderFor("clientId").isAscending());
		List<Key> results = resultsPage.getContent();
		assertTrue(results.contains(testKey));
		assertTrue(results.contains(expTestKey));
		assertTrue(results.contains(refTestKey));

		params.put("sortBy", "userId,clientId,authTokenValue");
		params.put("sortOrder", "DESC,ASC,DESC");
		try {
			resultsPage = controller.findAll(params);
		} catch (BadParameterException e) {
			fail("Bad parameters exception");
			e.printStackTrace();
		}
		assertTrue(resultsPage.getNumberOfElements() == 3);
		assertTrue(resultsPage.getTotalElements() == 3);
		assertTrue(resultsPage.getTotalPages() == 1);
		assertTrue(resultsPage.getSort().getOrderFor("userId").isDescending());
		assertTrue(resultsPage.getSort().getOrderFor("clientId").isAscending());
		assertTrue(resultsPage.getSort().getOrderFor("authToken.value").isDescending());
		results = resultsPage.getContent();
		assertTrue(results.contains(testKey));
		assertTrue(results.contains(expTestKey));
		assertTrue(results.contains(refTestKey));

		controller.delete(testKey.getUserId(), testKey.getClientId());
		controller.delete(expTestKey.getUserId(), expTestKey.getClientId());
		controller.delete(refTestKey.getUserId(), refTestKey.getClientId());

		results = controller.findAll();
		assertFalse(results.contains(testKey));
		assertFalse(results.contains(expTestKey));
		assertFalse(results.contains(refTestKey));

	}

	// Add three keys, see them filtered correctly in find all results,
	// remove the keys.
	@SuppressWarnings("unchecked")
	@Test
	public void testAddFindallFiltered() {

		Map<String, String> params = new HashMap<>();
		params.put("clientId", "Test_Client_ID*");
		params.put("userId", "$in:" + uid + ",EX_" + uid + ",Ref_" + uid);
		params.put("created", "$gt:0");
		params.put("modified", "$lt:200000000000");
		params.put("authTokenValue", "$exists:true");
		params.put("agencyCode", "SampleAgencyCode");
		params.put("refTokenExpiration", "$exists:true");

		try {
			controller.addKey(testKey);
			controller.addKey(expTestKey);
			controller.addKey(refTestKey);
		} catch (MissingRequiredParameterException e1) {
			fail("Missing required parameters exception; user ID and client ID should not have been null or empty.");
			e1.printStackTrace();
		} catch (AddFailureException e) {
			fail("Unknown error in add method.");
			e.printStackTrace();
		}

		Page<Key> resultsPage = null;
		try {
			resultsPage = controller.findAll(params);
		} catch (BadParameterException e) {
			fail("Bad parameters exception");
			e.printStackTrace();
		}
		List<Key> results = resultsPage.getContent();
		assertTrue(results.contains(testKey));
		assertTrue(results.contains(expTestKey));
		assertTrue(results.contains(refTestKey));

		params.put("clientId", "Test_Client_ID");
		try {
			resultsPage = controller.findAll(params);
		} catch (BadParameterException e) {
			fail("Bad parameters exception");
			e.printStackTrace();
		}
		results = resultsPage.getContent();
		assertTrue(results.contains(testKey));
		assertFalse(results.contains(expTestKey));
		assertFalse(results.contains(refTestKey));

		params.remove("clientId");
		params.put("authTokenExpired", "true");
		try {
			resultsPage = controller.findAll(params);
		} catch (BadParameterException e) {
			fail("Bad parameters exception");
			e.printStackTrace();
		}
		results = resultsPage.getContent();
		assertFalse(results.contains(testKey));
		assertTrue(results.contains(expTestKey));
		assertTrue(results.contains(refTestKey));

		params.remove("authTokenExpired");
		params.put("attributesSample_Att_Key", "$in:Sample_Att_Val,Sample_Bat_Val,Sample_Cat_Val,Jim");
		try {
			resultsPage = controller.findAll(params);
		} catch (BadParameterException e) {
			fail("Bad parameters exception");
			e.printStackTrace();
		}
		results = resultsPage.getContent();
		assertTrue(results.contains(testKey));
		assertTrue(results.contains(expTestKey));
		assertTrue(results.contains(refTestKey));

		params.put("refTokenExpired", "true");
		try {
			resultsPage = controller.findAll(params);
		} catch (BadParameterException e) {
			fail("Bad parameters exception");
			e.printStackTrace();
		}
		results = resultsPage.getContent();
		assertFalse(results.contains(testKey));
		assertTrue(results.contains(expTestKey));
		assertFalse(results.contains(refTestKey));

		params.put("refTokenValue", "$exists:false");
		try {
			resultsPage = controller.findAll(params);
		} catch (BadParameterException e) {
			fail("Bad parameters exception");
			e.printStackTrace();
		}
		results = resultsPage.getContent();
		assertTrue(results.isEmpty());

		params.remove("refTokenExpired");
		params.remove("refTokenValue");
		params.put("authTokenScope", "Scope 3");
		try {
			resultsPage = controller.findAll(params);
		} catch (BadParameterException e) {
			fail("Bad parameters exception");
			e.printStackTrace();
		}
		results = resultsPage.getContent();
		assertTrue(results.contains(testKey));
		assertTrue(results.contains(expTestKey));
		assertTrue(results.contains(refTestKey));

		// Clean-up
		controller.delete(testKey.getUserId(), testKey.getClientId());
		controller.delete(expTestKey.getUserId(), expTestKey.getClientId());
		controller.delete(refTestKey.getUserId(), refTestKey.getClientId());

		results = controller.findAll();
		assertFalse(results.contains(testKey));
		assertFalse(results.contains(expTestKey));
		assertFalse(results.contains(refTestKey));

	}

	// Add three keys, see the projected fields correctly in find all results,
	// remove the keys.
	// TODO Not working with the new scopes change, need to fix
	/*
	 * @SuppressWarnings({ "rawtypes" })
	 * 
	 * @Test public void testAddFindallProjected() {
	 * 
	 * // Prepare the paging/sorting/filtering/projection parameters. Map<String,
	 * String> params = new HashMap<>(); params.put("sortBy", "clientId");
	 * params.put("sortOrder", "ASC"); params.put("clientId", "Test_Client_ID*");
	 * params.put("fields", "userId,clientId");
	 * 
	 * // Add the test keys. try { controller.addKey(testKey);
	 * controller.addKey(expTestKey); controller.addKey(refTestKey); } catch
	 * (MissingRequiredParameterException e1) {
	 * fail("Missing required parameters exception; user ID and client ID should not have been null or empty."
	 * ); e1.printStackTrace(); } catch (AddFailureException e) {
	 * fail("Unknown error in add method."); e.printStackTrace(); }
	 * 
	 * // Prepare the expected results maps. Map<String, Object> expected1 = new
	 * HashMap<>(); expected1.put("userId", testKey.getUserId());
	 * expected1.put("clientId", testKey.getClientId()); Map<String, Object>
	 * expected2 = new HashMap<>(); expected2.put("userId", expTestKey.getUserId());
	 * expected2.put("clientId", expTestKey.getClientId()); Map<String, Object>
	 * expected3 = new HashMap<>(); expected3.put("userId", refTestKey.getUserId());
	 * expected3.put("clientId", refTestKey.getClientId());
	 * 
	 * // Search again with field projection. Page resultsPage = null; try {
	 * resultsPage = controller.findAll(params); } catch (BadParameterException e) {
	 * fail("Bad parameters exception"); e.printStackTrace(); }
	 * assertTrue(resultsPage.getSort().getOrderFor("clientId").isAscending()); List
	 * results = resultsPage.getContent(); assertTrue(results.contains(expected1));
	 * assertTrue(results.contains(expected2));
	 * assertTrue(results.contains(expected3));
	 * 
	 * // Prepare the new expected results maps. Map<String, Object> sc = new
	 * HashMap<>(); sc.put("scope", scopes);
	 * 
	 * Map<String, Object> ag = new HashMap<>(); ag.put("agencyCode",
	 * "SampleAgencyCode");
	 * 
	 * expected1.remove("clientId"); expected1.put("authToken", sc);
	 * expected1.put("attributes", ag);
	 * 
	 * expected2.remove("clientId"); expected2.put("authToken", sc);
	 * expected2.put("attributes", ag);
	 * 
	 * expected3.remove("clientId"); expected3.put("authToken", sc);
	 * expected3.put("attributes", ag);
	 * 
	 * // Search again with different field projection. params.put("fields",
	 * "userId,authTokenScope,agencyCode"); try { resultsPage =
	 * controller.findAll(params); } catch (BadParameterException e) {
	 * fail("Bad parameters exception"); e.printStackTrace(); } results =
	 * resultsPage.getContent(); System.out.println(expected3.toString()); for
	 * (Object key : results) System.out.println(key.toString());
	 * assertTrue(results.contains(expected1));
	 * assertTrue(results.contains(expected2));
	 * assertTrue(results.contains(expected3));
	 * 
	 * // Clean-up controller.delete(testKey.getUserId(), testKey.getClientId());
	 * controller.delete(expTestKey.getUserId(), expTestKey.getClientId());
	 * controller.delete(refTestKey.getUserId(), refTestKey.getClientId());
	 * 
	 * // Check for remnants. results = controller.findAll();
	 * assertFalse(results.contains(testKey));
	 * assertFalse(results.contains(expTestKey));
	 * assertFalse(results.contains(refTestKey));
	 * 
	 * }
	 */
}
