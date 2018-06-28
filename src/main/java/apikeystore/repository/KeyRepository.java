package apikeystore.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.querydsl.QueryDslPredicateExecutor;
import org.springframework.stereotype.Repository;

import com.querydsl.core.types.Predicate;

import apikeystore.model.Key;

@Repository
public interface KeyRepository
		extends MongoRepository<Key, String>, QueryDslPredicateExecutor<Key>, KeyRepositoryCustom {

	Key findById(String id);

	Key findByAuthTokenValue(String value);

	Key findByRefTokenValue(String value);

	Page<Key> findAllByUserId(String userId, Pageable pageable);

	Page<Key> findAllByClientId(String clientId, Pageable pageable);

	@Query("{ 'attributes.agencyCode' : ?0 }")
	Page<Key> findAllByAgencyCode(String agencyCode, Pageable pageable);

	List<Key> findAll();

	Page<Key> findAll(Predicate predicate, Pageable pageable);

	List<Key> findAllByUserId(String userId);

	List<Key> findAllByClientId(String clientId);

	@Query("{ 'attributes.agencyCode' : ?0 }")
	List<Key> findAllByAgencyCode(String agencyCode);

}
