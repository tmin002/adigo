package kr.gachon.adigo.data.local.repository

import io.realm.kotlin.Realm
import io.realm.kotlin.RealmConfiguration
import io.realm.kotlin.query.RealmResults
import io.realm.kotlin.types.RealmObject
import kotlin.reflect.KClass

abstract class AbstractManagedModelDatabaseRepository<Entity: RealmObject>(
    private val entityClass: KClass<Entity>
) {
    fun getRealm(): Realm {
        val config = RealmConfiguration.Builder(
            schema = setOf(entityClass)
        ).build()
        return Realm.open(config)
    }

    fun get(id: String, realm: Realm = getRealm()): Entity? {
        return realm.query(entityClass, "id == $0", id).first().find()
    }
    fun getByQuery(queryString: String, realm: Realm = getRealm()): List<Entity> {
        val result: RealmResults<Entity> = realm.query(entityClass, queryString).find()
        return result.toList()
    }
    fun getAll(realm: Realm = getRealm()): List<Entity> {
        val result: RealmResults<Entity> = realm.query(entityClass).find()
        return result.toList()
    }

    suspend fun add(entity: Entity, realm: Realm = getRealm()) {
        realm.write {
            copyToRealm(entity)
        }
    }
    suspend fun delete(id: String, realm: Realm = getRealm()) {
        realm.write {
            val entity = query(entityClass, "id == $0", id).first().find()
            if (entity != null) {
                delete(entity)
            } else {
                throw EntityNotFoundException(id)
            }
        }
    }
}

class EntityNotFoundException(
    private val id: String
): Exception("Entity with ID '${id}' not found.");