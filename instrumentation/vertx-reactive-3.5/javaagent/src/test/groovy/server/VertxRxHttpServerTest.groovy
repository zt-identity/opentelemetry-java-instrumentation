/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package server

import static io.opentelemetry.instrumentation.test.base.HttpServerTest.ServerEndpoint.ERROR
import static io.opentelemetry.instrumentation.test.base.HttpServerTest.ServerEndpoint.EXCEPTION
import static io.opentelemetry.instrumentation.test.base.HttpServerTest.ServerEndpoint.PATH_PARAM
import static io.opentelemetry.instrumentation.test.base.HttpServerTest.ServerEndpoint.QUERY_PARAM
import static io.opentelemetry.instrumentation.test.base.HttpServerTest.ServerEndpoint.REDIRECT
import static io.opentelemetry.instrumentation.test.base.HttpServerTest.ServerEndpoint.SUCCESS

import io.opentelemetry.instrumentation.test.AgentTestTrait
import io.opentelemetry.instrumentation.test.base.HttpServerTest
import io.vertx.core.DeploymentOptions
import io.vertx.core.Future
import io.vertx.core.Vertx
import io.vertx.core.VertxOptions
import io.vertx.core.json.JsonObject
import io.vertx.reactivex.core.AbstractVerticle
import io.vertx.reactivex.ext.web.Router
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit

class VertxRxHttpServerTest extends HttpServerTest<Vertx> implements AgentTestTrait {
  public static final String CONFIG_HTTP_SERVER_PORT = "http.server.port"

  @Override
  Vertx startServer(int port) {
    Vertx server = Vertx.vertx(new VertxOptions()
    // Useful for debugging:
    // .setBlockedThreadCheckInterval(Integer.MAX_VALUE)
      .setClusterPort(port))
    CompletableFuture<Void> future = new CompletableFuture<>()
    server.deployVerticle(verticle().getName(),
      new DeploymentOptions()
        .setConfig(new JsonObject().put(CONFIG_HTTP_SERVER_PORT, port))
        .setInstances(3)) { res ->
      if (!res.succeeded()) {
        throw new RuntimeException("Cannot deploy server Verticle", res.cause())
      }
      future.complete(null)
    }

    future.get(30, TimeUnit.SECONDS)
    return server
  }

  @Override
  void stopServer(Vertx server) {
    server.close()
  }

  @Override
  boolean testException() {
    // https://github.com/open-telemetry/opentelemetry-java-instrumentation/issues/807
    return false
  }

  @Override
  boolean testPathParam() {
    return true
  }

  @Override
  boolean testNotFound() {
    return false
  }

  @Override
  String expectedServerSpanName(ServerEndpoint endpoint) {
    return endpoint == PATH_PARAM ? "/path/:id/param" : endpoint.getPath()
  }

  protected Class<AbstractVerticle> verticle() {
    return VertxReactiveWebServer
  }

  static class VertxReactiveWebServer extends AbstractVerticle {

    @Override
    void start(final Future<Void> startFuture) {
      int port = config().getInteger(CONFIG_HTTP_SERVER_PORT)
      Router router = Router.router(super.@vertx)

      router.route(SUCCESS.path).handler { ctx ->
        controller(SUCCESS) {
          ctx.response().setStatusCode(SUCCESS.status).end(SUCCESS.body)
        }
      }
      router.route(QUERY_PARAM.path).handler { ctx ->
        controller(QUERY_PARAM) {
          ctx.response().setStatusCode(QUERY_PARAM.status).end(ctx.request().query())
        }
      }
      router.route(REDIRECT.path).handler { ctx ->
        controller(REDIRECT) {
          ctx.response().setStatusCode(REDIRECT.status).putHeader("location", REDIRECT.body).end()
        }
      }
      router.route(ERROR.path).handler { ctx ->
        controller(ERROR) {
          ctx.response().setStatusCode(ERROR.status).end(ERROR.body)
        }
      }
      router.route(EXCEPTION.path).handler { ctx ->
        controller(EXCEPTION) {
          throw new Exception(EXCEPTION.body)
        }
      }
      router.route("/path/:id/param").handler { ctx ->
        controller(PATH_PARAM) {
          ctx.response().setStatusCode(PATH_PARAM.status).end(ctx.request().getParam("id"))
        }
      }


      super.@vertx.createHttpServer()
        .requestHandler { router.accept(it) }
        .listen(port) { startFuture.complete() }
    }
  }
}
