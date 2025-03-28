package kr.gachon.adigo.data.local.repository

import kr.adigo.adigo.database.entity.UserEntity

object UserDatabaseRepository:
BasedManagedModelDatabaseRepository<UserEntity>(UserEntity::class) {}