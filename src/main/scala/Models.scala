package io.kensu.frbayart.models

case class Tweet(
        id: Long,
        userId: Long,
        createdAt: String,
        name: String,
        message: String
)
