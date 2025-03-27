package kr.gachon.adigo.data.model.global

interface IManagedModel<DTO: IDataTransfterObject> {
    val id: String
    fun getDTO(): DTO
}