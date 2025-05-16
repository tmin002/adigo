package kr.gachon.adigo.data

import kr.gachon.adigo.data.model.global.BasedManagedModel

abstract class BasedManagedModelRepository<Model: BasedManagedModel<*>> {
    private val storage: MutableMap<Long, Model> = mutableMapOf();

    val size: Int get() = storage.size;
    val empty: Boolean get() = storage.isEmpty();

    fun get(id: Long): Model {
        val result = storage[id];
        if (result == null) {
            throw ModelNotFoundException(id);
        } else {
            return result;
        }
    }
    fun get(predicate: (Model) -> Boolean): Map<Long, Model> {
        return storage.filter { predicate(it.value) }
    }
    fun add(model: Model) {
        storage[model.id] = model;
    }
    fun delete(id: Long) {
        if (storage.contains(id)) {
            storage.remove(id);
        } else {
            throw ModelNotFoundException(id);
        }
    }
}

class ModelNotFoundException(
    private val id: Long
): Exception("Model with ID '${id}' not found.");