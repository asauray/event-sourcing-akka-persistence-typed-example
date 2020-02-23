package net.sauray.infrastructure;

import java.util.List;
import java.util.concurrent.ExecutionException;

import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import akka.persistence.query.EventEnvelope;
import dev.miku.r2dbc.mysql.MySqlConnectionConfiguration;
import dev.miku.r2dbc.mysql.MySqlConnectionFactory;
import io.r2dbc.spi.Connection;
import io.r2dbc.spi.ConnectionFactory;
import reactor.core.publisher.Mono;

/*
 * ReactiveSQL.java
 * Copyright (C) 2020 antoinesauray <sauray.antoine@gmail.com>
 *
 * Distributed under terms of the MIT license.
 */


public class ReactiveSQL
{

  public static Mono<Connection> reactiveConnection(String user, String password, String database, String host, int port) throws InterruptedException, ExecutionException {


      MySqlConnectionConfiguration configuration = MySqlConnectionConfiguration.builder()
        .username(user)
        .password(password)
        .host(host)
        .port(port)
        .database(database)
        .build();

      ConnectionFactory connectionFactory = MySqlConnectionFactory.from(configuration);

      Mono<Connection> connectionMono = Mono.from(connectionFactory.create());
      return connectionMono;
    }
}

