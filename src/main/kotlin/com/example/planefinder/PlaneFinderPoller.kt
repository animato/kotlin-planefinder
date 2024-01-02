package com.example.planefinder

import org.springframework.data.redis.connection.RedisConnectionFactory
import org.springframework.data.redis.core.RedisOperations
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToFlux

@EnableScheduling
@Component
class PlaneFinderPoller(
    val connectionFactory: RedisConnectionFactory,
    val redisOperations: RedisOperations<String, Aircraft>) {

    var client: WebClient = WebClient.create("http://localhost:7634/aircraft")

    @Scheduled(fixedRate = 1000)
    fun pollPlanes() {
        connectionFactory.connection.serverCommands().flushDb()

        client.get()
            .retrieve()
            .bodyToFlux<Aircraft>()
            .filter { p -> p.reg.isNotEmpty() }
            .toStream()
            .forEach { ac -> redisOperations.opsForValue().set(ac.reg, ac)}

        redisOperations.opsForValue()
            .operations
            .keys("*")
            ?.forEach { ac -> println(redisOperations.opsForValue().get(ac))}
    }
}