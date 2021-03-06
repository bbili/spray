/*
 * Copyright © 2011-2013 the spray project <http://spray.io>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package spray.can.server

import spray.io._
import spray.http._
import HttpHeaders._

private object RemoteAddressHeaderSupport extends PipelineStage {
  def apply(context: PipelineContext, commandPL: CPL, eventPL: EPL): Pipelines =
    new Pipelines {
      val raHeader = `Remote-Address`(context.remoteAddress.getAddress)
      def appendHeader(request: HttpRequest): HttpRequest = request.mapHeaders(raHeader :: _)

      val commandPipeline = commandPL

      val eventPipeline: EPL = {
        case x: RequestParsing.HttpMessageStartEvent ⇒ eventPL {
          x.copy(
            messagePart = x.messagePart match {
              case request: HttpRequest         ⇒ appendHeader(request)
              case ChunkedRequestStart(request) ⇒ ChunkedRequestStart(appendHeader(request))
              case _                            ⇒ throw new IllegalStateException
            })
        }

        case ev ⇒ eventPL(ev)
      }
    }
}