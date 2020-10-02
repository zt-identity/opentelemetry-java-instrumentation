/*
 * Copyright The OpenTelemetry Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.opentelemetry.instrumentation.auto.spring.webflux.server;

import io.opentelemetry.instrumentation.api.tracer.BaseTracer;

public class SpringWebfluxHttpServerTracer extends BaseTracer {
  public static final SpringWebfluxHttpServerTracer TRACER = new SpringWebfluxHttpServerTracer();

  @Override
  protected String getInstrumentationName() {
    return "io.opentelemetry.auto.spring-webflux-5.0";
  }
}