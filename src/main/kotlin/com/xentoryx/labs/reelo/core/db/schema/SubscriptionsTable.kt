@file:OptIn(kotlin.uuid.ExperimentalUuidApi::class)
package com.xentoryx.labs.reelo.core.db.schema

import org.jetbrains.exposed.v1.core.*
import org.jetbrains.exposed.v1.javatime.*

object SubscriptionsTable : Table("subscriptions") {
    val id = uuid("id")
    val subscriberId = uuid("subscriber_id").references(UsersTable.id, onDelete = ReferenceOption.CASCADE)
    val channelId = uuid("channel_id").references(UsersTable.id, onDelete = ReferenceOption.CASCADE)
    val createdAt = datetime("created_at")

    override val primaryKey = PrimaryKey(id)

    init {
        uniqueIndex("idx_subscriber_channel", subscriberId, channelId)
    }
}
