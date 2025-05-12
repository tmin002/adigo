package kr.gachon.adigo.data.model.global

interface BasedManagedModel<DTO: BasedDataTransfterObject> {
    val id: String
    fun getDTO(): DTO

}