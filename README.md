# API Keystore
The API Keystore creates, stores, retrieves and updates OAuth tokens and credentials.  The application is designed to support an OAuth token-based authentication and authorization implementation for APIs.  The chief purpose of Keystore is to create and keep track of access token values, which can be used by an API gateway or other architecture to control client application access to specific resources.

Keystore is written in Java using the Spring framework, and maintains a MongoDB repository.
Swagger doc is located at /swagger-ui.html

## Keys and Tokens
A Key consists of a user ID, client application ID, OAuth access token, OAuth refresh token, and a mapping of additional attributes (e.g., agency code).  Each Key is uniquely identified in the repository by the combination of its user ID and client ID values; attempting to add a Key with the same user ID and client ID as an existing Key will overwrite the existing Key with the newer one.

An access token consists of a token value, and expiration timestamp and a set of scopes (the API paths and verbs to which the token bearer has access).  A refresh token is similar in structure, but only has a value and expiration timestamp.  Typically the expiry of an access token is set to be much sooner than that of a refresh token; in common usage, access tokens are meant to expire in minutes or hours, while refresh tokens can last for days or weeks.  In Keystore the token expiry can be set via the lifetimeSeconds static field in the access and refresh token classes.

## Endpoints
### Most Frequently Used / Primary Functionality
#### Finding all keys or adding a new key to the database
- /keys
  - GET
    - Returns a 200.OK status code and a paginated List of all Keys in the repository.
    - The results list can be sorted, filtered and paginated.

  - POST
    - Accepts a Key Request JSON object (see below section on object format), which is used to generate and add a new Key to the repository.
    - Returns a 201.Created and the new Key if successful.
    - Finding or removing/revoking individual keys
    - These endpoints return or delete (respectively) the individual key matching the supplied criteria (access token value, or user ID and client ID).

- /keys/{userId}/{clientId}
  - GET
    - Returns a 200.OK and the matching Key if successful.
    - Returns a 404.NotFound if the Key is not in the repository.
    - Returns a 401.Unauthorized if the Key is found but the access token is expired.
    - Returns a 410.Gone and removes the Key from the repository if the key is found but both the access and refresh tokens are expired.

  - DELETE
    - Returns a 200.OK and removes the matching Key from the repository if successful.
    - Returns a 404.NotFound if the Key is not in the repository.

- /keys/token/{authValue}
  - GET
    - Returns a 200.OK and the matching Key if successful.
    - Returns a 404.NotFound if the Key is not in the repository.
    - Returns a 401.Unauthorized if the Key is found but the access token is expired.
    - Returns a 410.Gone and removes the Key from the repository if the key is found but both the access and refresh tokens are expired.

  - DELETE
    - Returns a 200.OK and removes the matching Key from the repository if successful.
    - Returns a 404.NotFound if the Key is not in the repository.
    
#### Finding or removing/revoking groups of keys
These endpoints return or delete (respectively) all keys matching the supplied criteria (user ID, client ID or agency code).

- /keys/user/{userId}
  - GET
    - Returns a 200.OK and a paginated List of all Keys in the repository with the supplied user ID.
  - DELETE
    - Returns a 200.OK and removes all keys with the supplied user ID from the repository if successful.
    - Returns a 404.NotFound if no keys are found in the repository with the supplied user ID.

- /keys/client/{clientId}
  - GET
    - Returns a 200.OK and a paginated List of all Keys in the repository with the supplied client ID.
  - DELETE
    - Returns a 200.OK and removes all keys with the supplied client ID from the repository if successful.
    - Returns a 404.NotFound if no keys are found in the repository with the supplied client ID.

- /keys/agency/{agencyCode}
  - GET
    - Returns a 200.OK and a paginated List of all Keys in the repository with the supplied agency code.
  - DELETE
    - Returns a 200.OK and removes all keys with the supplied agency code from the repository if successful.
    - Returns a 404.NotFound if no keys are found in the repository with the supplied agency code.

#### OAuth functions
- /keys/auth/{authValue}
  - GET
    - Used to check the validity of a Key matching the supplied access token value without returning the full Key object.
    - Returns a 200.OK if the key is found and the access token is not expired.
    - Returns a 404.NotFound if the Key is not in the repository.
    - Returns a 401.Unauthorized if the Key is found but the access token is expired.
    - Returns a 410.Gone and removes the Key from the repository if the key is found but both the access and refresh tokens are expired.

- /keys/refresh
  - POST
    - Accepts a refresh token value as a plaintext string, and attempts to refresh the corresponding key by generating new access token and refresh token values and expirations.
    - Returns a 200.OK and the refreshed Key if successful.
    - Returns a 404.NotFound if the corresponding Key is not in the repository.
    - Returns a 403.Forbidden if the Key id found but the refresh token is expired.  The Key is still valid and usable until the access token is expired, but cannot be refreshed.  This shouldn't happen during normal use, but is possible when adding Keys from full Key objects (see below section).
    - Returns a 410.Gone and removes the Key from the repository if the key is found but both the access and refresh tokens are expired.

### Less Frequently Used and Testing / Debugging
- /keys/status
  - GET
    - Returns a 200.OK status code, used to quickly check that the application is running and available.

- /keys/obj
  - POST
    - Accepts a preconstructed JSON Key object (including access and refresh tokens) and adds it to the repository.
    - Returns a 201.Created and a copy of the added Key if successful.

#### JWT format support
These endpoints provide the same functionality of the find / remove operations above, but accept or return Keys encoded as JSON Web Tokens (JWTs).

- /keys/jwt
  - POST
    - Accepts a Key Request object encoded as a JWT, which is used to add a new Key to the repository.
    - Returns a 201.Created and the new Key as a JWT if successful.

- /keys/jwt/{authValue}
  - GET
    - Returns a 200.OK and the matching Key encoded as a JWT object if successful.
    - Returns a 404.NotFound if the Key is not in the repository.
    - Returns a 401.Unauthorized if the Key is found but the access token is expired.
    - Returns a 410.Gone and removes the Key from the repository if the key is found but both the access and refresh tokens are expired.

- /keys/jwt/{userId}/{clientId}
  - GET
    - Returns a 200.OK and the matching Key encoded as a JWT object if successful.
    - Returns a 404.NotFound if the Key is not in the repository.
    - Returns a 401.Unauthorized if the Key is found but the access token is expired.
    - Returns a 410.Gone and removes the Key from the repository if the key is found but both the access and refresh tokens are expired.
