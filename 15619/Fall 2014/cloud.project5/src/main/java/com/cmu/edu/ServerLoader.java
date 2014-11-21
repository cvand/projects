package com.cmu.edu;

import org.vertx.java.core.Handler;
import org.vertx.java.core.http.HttpServer;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.http.RouteMatcher;
import org.vertx.java.platform.Verticle;

import com.cmu.edu.cloud.querries.Query1;
import com.cmu.edu.cloud.querries.Query2;

public class ServerLoader extends Verticle {

	@Override
	public void start() {
		HttpServer server = vertx.createHttpServer();

		RouteMatcher matcher = createMatcher();
		server.requestHandler(matcher).listen(8080);
	}

	private RouteMatcher createMatcher() {
		RouteMatcher matcher = new RouteMatcher();

		matcher.get("/q1", new Handler<HttpServerRequest>() {
			public void handle(HttpServerRequest req) {
				Query1 q1 = new Query1();
				String result = q1.execute(req.params().get("key"));
				req.response().headers().set("Content-Type", "text/plain; charset=UTF-8");
				req.response().end(result);
			};
		});

		matcher.get("/q2", new Handler<HttpServerRequest>() {
			@Override
			public void handle(HttpServerRequest req) {
				Query2 q2 = new Query2();
				String result = q2.execute(req.params().get("key"), req.params().get("userid"), req.params().get("tweet_time"));;
				req.response().headers().set("Content-Type", "text/plain; charset=UTF-8");
				req.response().end(result);
			}
		});
		return matcher;
	}
}
