package kr.gachon.adigo.data.local.transformer
import io.realm.kotlin.types.RealmObject
import kr.gachon.adigo.data.model.global.BasedManagedModel

interface BasedTransformer<Model: BasedManagedModel<*>, Entity: RealmObject> {
    fun modelToEntity(model: Model): Entity
    fun entityToModel(entity: Entity): Model
}