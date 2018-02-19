/*
 * Copyright [2017] [B2W Digital]
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
 *
 */
package org.marvin.executor.api

import actions.HealthCheckResponse.Status
import akka.actor.ActorRef
import akka.http.scaladsl.model.ContentTypes._
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, StatusCode, StatusCodes}
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.testkit.ScalatestRouteTest
import akka.testkit.TestProbe
import org.marvin.exception.MarvinEExecutorException
import org.marvin.executor.actions.BatchAction.{BatchExecute, BatchExecutionStatus, BatchHealthCheck, BatchReload}
import org.marvin.executor.actions.OnlineAction.{OnlineExecute, OnlineHealthCheck}
import org.marvin.executor.actions.PipelineAction.{PipelineExecute, PipelineExecutionStatus}
import org.marvin.executor.statemachine.Reload
import org.marvin.fixtures.MetadataMock
import org.marvin.model.EngineMetadata
import org.scalamock.scalatest.MockFactory
import org.scalatest.{Inside, Matchers, WordSpec}


class GenericAPITest extends WordSpec with ScalatestRouteTest with Matchers with Inside with MockFactory {

  "/predictor endpoint" should {

    val metadata = MetadataMock.simpleMockedMetadata()
    val actor = TestProbe()
    val defaultParams = ""
    val api = new GenericAPI(system, metadata, defaultParams, Map[String, ActorRef]("predictor" -> actor.ref))

    "interpret the input message and respond with media type json" in {

      val message = "testQuery"
      val params = "testParams"
      val response = "fooReply"

      val result = Post("/predictor", HttpEntity(`application/json`, s"""{"params":"$params","message":"$message"}""")) ~> api.routes ~> runRoute

      val expectedMessage = OnlineExecute(message, params)
      actor.expectMsg(expectedMessage)
      actor.reply(response)

      check {
        status shouldEqual StatusCode.int2StatusCode(200)
        contentType shouldEqual ContentTypes.`application/json`
        responseAs[String] shouldEqual s"""{"result":"$response"}"""
      }(result)
    }

    "gracefully fail when message is not informed" in {
      Post("/predictor", HttpEntity(`application/json`, s"""{"params":"testParams"}""")) ~> api.routes ~> check {
        status shouldEqual StatusCodes.BadRequest
        responseAs[String] shouldEqual s"""{"errorMessage":"requirement failed: The request payload must contain the attribute 'message'."}"""
      }
    }

    "use default params when no params is informed" in {

      val message = "testQuery"
      val response = "noParams"

      val result = Post("/predictor", HttpEntity(`application/json`, s"""{"message":"$message"}""")) ~> api.routes ~> runRoute

      val expectedMessage = OnlineExecute(message, defaultParams)
      actor.expectMsg(expectedMessage)
      actor.reply(response)

      check{
        status shouldEqual StatusCodes.OK
        responseAs[String] shouldEqual s"""{"result":"$response"}"""
      }(result)
    }

    "fail fast when the timeout is reached" in {

      val message = "testQuery"

      val metadata = EngineMetadata(
        name = "test",
        actions = null,
        artifactsRemotePath = "",
        artifactManagerType = "",
        s3BucketName = "",
        batchActionTimeout = 50,
        engineType = "",
        hdfsHost = "",
        healthCheckTimeout = 50,
        onlineActionTimeout = 50,
        pipelineActions = List(),
        reloadStateTimeout = Some(250),
        reloadTimeout = 50,
        version = "1"
      )

      val api = new GenericAPI(system, metadata, defaultParams, Map[String, ActorRef]("predictor" -> actor.ref))

      val result = Post("/predictor", HttpEntity(`application/json`, s"""{"message":"$message"}""")) ~> api.routes ~> runRoute

      val expectedMessage = OnlineExecute(message, defaultParams)
      actor.expectMsg(expectedMessage)

      check {
        status shouldEqual StatusCodes.InternalServerError
        responseAs[String] shouldEqual """{"errorMessage":"The engine was not able to provide a response within the specified timeout."}"""
      }(result)

    }
  }

  "/feedback endpoint" should {

    val metadata = MetadataMock.simpleMockedMetadata()
    val actor = TestProbe()
    val defaultParams = ""
    val api = new GenericAPI(system, metadata, defaultParams, Map[String, ActorRef]("feedback" -> actor.ref))


    "interpret the input message and respond with media type json" in {

      val message = "testQuery"
      val params = "testParams"
      val response = "fooReply"

      val result = Post("/feedback", HttpEntity(`application/json`, s"""{"params":"$params","message":"$message"}""")) ~> api.routes ~> runRoute

      val expectedMessage = OnlineExecute(message, params)
      actor.expectMsg(expectedMessage)
      actor.reply(response)

      check {
        status shouldEqual StatusCode.int2StatusCode(200)
        contentType shouldEqual ContentTypes.`application/json`
        responseAs[String] shouldEqual s"""{"result":"$response"}"""
      }(result)
    }

    "gracefully fail when message is not informed" in {
      Post("/feedback", HttpEntity(`application/json`, s"""{"params":"testParams"}""")) ~> api.routes ~> check {
        status shouldEqual StatusCodes.BadRequest
        responseAs[String] shouldEqual s"""{"errorMessage":"requirement failed: The request payload must contain the attribute 'message'."}"""
      }
    }

    "use default params when no params is informed" in {

      val message = "testQuery"
      val response = "noParams"

      val result = Post("/feedback", HttpEntity(`application/json`, s"""{"message":"$message"}""")) ~> api.routes ~> runRoute

      val expectedMessage = OnlineExecute(message, defaultParams)
      actor.expectMsg(expectedMessage)
      actor.reply(response)

      check{
        status shouldEqual StatusCodes.OK
        responseAs[String] shouldEqual s"""{"result":"$response"}"""
      }(result)
    }

    "fail fast when the timeout is reached" in {

      val message = "testQuery"

      val metadata = EngineMetadata(
        name = "test",
        actions = null,
        artifactsRemotePath = "",
        artifactManagerType = "",
        s3BucketName = "",
        batchActionTimeout = 50,
        engineType = "",
        hdfsHost = "",
        healthCheckTimeout = 50,
        onlineActionTimeout = 50,
        pipelineActions = List(),
        reloadStateTimeout = Some(250),
        reloadTimeout = 50,
        version = "1"
      )

      val api = new GenericAPI(system, metadata, defaultParams, Map[String, ActorRef]("feedback" -> actor.ref))

      val result = Post("/feedback", HttpEntity(`application/json`, s"""{"message":"$message"}""")) ~> api.routes ~> runRoute

      val expectedMessage = OnlineExecute(message, defaultParams)
      actor.expectMsg(expectedMessage)

      check {
        status shouldEqual StatusCodes.InternalServerError
        responseAs[String] shouldEqual """{"errorMessage":"The engine was not able to provide a response within the specified timeout."}"""
      }(result)

    }
  }

  "/acquisitor endpoint" should {

    val metadata = MetadataMock.simpleMockedMetadata()
    val actor = TestProbe()
    val defaultParams = ""
    val protocol = "acquisitor_mockedProtocol"

    val api = new GenericAPI(system, metadata, defaultParams, Map[String, ActorRef]("acquisitor" -> actor.ref)){
      override def generateProtocol(actionName: String):String = protocol
    }

    "interpret params and call BatchActor" in {

      val params = "testParams"

      val result = Post("/acquisitor", HttpEntity(`application/json`, s"""{"params": "$params"}""")) ~> api.routes ~> runRoute

      val expectedMessage = BatchExecute(protocol, params)
      actor.expectMsg(expectedMessage)

      check{
        status shouldEqual StatusCodes.OK
        responseAs[String] shouldEqual s"""{"result":"$protocol"}"""
      }(result)
    }

    "use default params when no params is informed" in {

      val result = Post("/acquisitor", HttpEntity(`application/json`, s"""{}""")) ~> api.routes ~> runRoute

      val expectedMessage = BatchExecute(protocol, defaultParams)
      actor.expectMsg(expectedMessage)

      check{
        status shouldEqual StatusCodes.OK
        responseAs[String] shouldEqual s"""{"result":"$protocol"}"""
      }(result)
    }
  }

  "/tpreparator endpoint" should {

    val metadata = MetadataMock.simpleMockedMetadata()
    val actor = TestProbe()
    val defaultParams = ""
    val protocol = "tpreparator_mockedProtocol"

    val api = new GenericAPI(system, metadata, defaultParams, Map[String, ActorRef]("tpreparator" -> actor.ref)){
      override def generateProtocol(actionName: String):String = protocol
    }

    "interpret params and call BatchActor" in {

      val params = "testParams"

      val result = Post("/tpreparator", HttpEntity(`application/json`, s"""{"params": "$params"}""")) ~> api.routes ~> runRoute

      val expectedMessage = BatchExecute(protocol, params)
      actor.expectMsg(expectedMessage)

      check{
        status shouldEqual StatusCodes.OK
        responseAs[String] shouldEqual s"""{"result":"$protocol"}"""
      }(result)
    }

    "use default params when no params is informed" in {

      val result = Post("/tpreparator", HttpEntity(`application/json`, s"""{}""")) ~> api.routes ~> runRoute

      val expectedMessage = BatchExecute(protocol, defaultParams)
      actor.expectMsg(expectedMessage)

      check{
        status shouldEqual StatusCodes.OK
        responseAs[String] shouldEqual s"""{"result":"$protocol"}"""
      }(result)
    }
  }

  "/trainer endpoint" should {

    val metadata = MetadataMock.simpleMockedMetadata()
    val actor = TestProbe()
    val defaultParams = ""
    val protocol = "trainer_mockedProtocol"

    val api = new GenericAPI(system, metadata, defaultParams, Map[String, ActorRef]("trainer" -> actor.ref)){
      override def generateProtocol(actionName: String):String = protocol
    }



    "interpret params and call BatchActor" in {

      val params = "testParams"

      val result = Post("/trainer", HttpEntity(`application/json`, s"""{"params": "$params"}""")) ~> api.routes ~> runRoute

      val expectedMessage = BatchExecute(protocol, params)
      actor.expectMsg(expectedMessage)

      check{
        status shouldEqual StatusCodes.OK
        responseAs[String] shouldEqual s"""{"result":"$protocol"}"""
      }(result)
    }

    "use default params when no params is informed" in {

      val result = Post("/trainer", HttpEntity(`application/json`, s"""{}""")) ~> api.routes ~> runRoute

      val expectedMessage = BatchExecute(protocol, defaultParams)
      actor.expectMsg(expectedMessage)

      check{
        status shouldEqual StatusCodes.OK
        responseAs[String] shouldEqual s"""{"result":"$protocol"}"""
      }(result)
    }
  }

  "/evaluator endpoint" should {

    val metadata = MetadataMock.simpleMockedMetadata()
    val actor = TestProbe()
    val defaultParams = ""
    val protocol = "evaluator_mockedProtocol"

    val api = new GenericAPI(system, metadata, defaultParams, Map[String, ActorRef]("evaluator" -> actor.ref)){
      override def generateProtocol(actionName: String):String = protocol
    }

    "interpret params and call BatchActor" in {

      val params = "testParams"

      val result = Post("/evaluator", HttpEntity(`application/json`, s"""{"params": "$params"}""")) ~> api.routes ~> runRoute

      val expectedMessage = BatchExecute(protocol, params)
      actor.expectMsg(expectedMessage)

      check{
        status shouldEqual StatusCodes.OK
        responseAs[String] shouldEqual s"""{"result":"$protocol"}"""
      }(result)
    }

    "use default params when no params is informed" in {

      val result = Post("/evaluator", HttpEntity(`application/json`, s"""{}""")) ~> api.routes ~> runRoute

      val expectedMessage = BatchExecute(protocol, defaultParams)
      actor.expectMsg(expectedMessage)

      check{
        status shouldEqual StatusCodes.OK
        responseAs[String] shouldEqual s"""{"result":"$protocol"}"""
      }(result)
    }
  }

  "/pipeline endpoint" should {

    val metadata = MetadataMock.simpleMockedMetadata()
    val actor = TestProbe()
    val defaultParams = ""
    val protocol = "pipeline_mockedProtocol"

    val api = new GenericAPI(system, metadata, defaultParams, Map[String, ActorRef]("pipeline" -> actor.ref)){
      override def generateProtocol(actionName: String):String = protocol
    }



    "interpret params and call PipelineActor" in {

      val params = "testParams"

      val result = Post("/pipeline", HttpEntity(`application/json`, s"""{"params": "$params"}""")) ~> api.routes ~> runRoute

      val expectedMessage = PipelineExecute(protocol, params)
      actor.expectMsg(expectedMessage)

      check{
        status shouldEqual StatusCodes.OK
        responseAs[String] shouldEqual s"""{"result":"$protocol"}"""
      }(result)
    }

    "use default params when no params is informed" in {

      val result = Post("/pipeline", HttpEntity(`application/json`, s"""{}""")) ~> api.routes ~> runRoute

      val expectedMessage = PipelineExecute(protocol, defaultParams)
      actor.expectMsg(expectedMessage)

      check{
        status shouldEqual StatusCodes.OK
        responseAs[String] shouldEqual s"""{"result":"$protocol"}"""
      }(result)
    }
  }

  "/predictor/reload endpoint" should {

    val metadata = MetadataMock.simpleMockedMetadata()
    val actor = TestProbe()
    val defaultParams = ""
    val api = new GenericAPI(system, metadata, defaultParams, Map[String, ActorRef]("predictor" -> actor.ref))

    "call OnlineReload" in {

      val protocol = "testProtocol"

      val result = Put(s"/predictor/reload?protocol=$protocol") ~> api.routes ~> runRoute

      val expectedMessage = Reload(protocol)
      actor.expectMsg(expectedMessage)

      check{
        status shouldEqual StatusCodes.OK
        responseAs[String] shouldEqual s"""{"result":"Work in progress...Thank you folk!"}"""
      }(result)
    }

    "fail gracefully when protocol is not informed" in {

      Put("/predictor/reload") ~> Route.seal(api.routes) ~> check {
        status shouldEqual StatusCodes.BadRequest
        responseAs[String] shouldEqual s"""{"errorMessage":"Missing query parameter. [protocol]"}"""
      }
    }
  }

  "/feedback/reload endpoint" should {

    val metadata = MetadataMock.simpleMockedMetadata()
    val actor = TestProbe()
    val defaultParams = ""
    val api = new GenericAPI(system, metadata, defaultParams, Map[String, ActorRef]("feedback" -> actor.ref))

    "call OnlineReload" in {

      val protocol = "testProtocol"

      val result = Put(s"/feedback/reload?protocol=$protocol") ~> api.routes ~> runRoute

      val expectedMessage = Reload(protocol)
      actor.expectMsg(expectedMessage)

      check{
        status shouldEqual StatusCodes.OK
        responseAs[String] shouldEqual s"""{"result":"Work in progress...Thank you folk!"}"""
      }(result)
    }

    "fail gracefully when protocol is not informed" in {

      Put("/predictor/reload") ~> Route.seal(api.routes) ~> check {
        status shouldEqual StatusCodes.BadRequest
        responseAs[String] shouldEqual s"""{"errorMessage":"Missing query parameter. [protocol]"}"""
      }
    }
  }

  "/tpreparator/reload endpoint" should {

    val metadata = MetadataMock.simpleMockedMetadata()
    val actor = TestProbe()
    val defaultParams = ""
    val api = new GenericAPI(system, metadata, defaultParams, Map[String, ActorRef]("tpreparator" -> actor.ref))

    "call BatchReload" in {

      val protocol = "testProtocol"

      val result = Put(s"/tpreparator/reload?protocol=$protocol") ~> api.routes ~> runRoute

      val expectedMessage = BatchReload(protocol)
      actor.expectMsg(expectedMessage)

      check{
        status shouldEqual StatusCodes.OK
        responseAs[String] shouldEqual s"""{"result":"Work in progress...Thank you folk!"}"""
      }(result)
    }

    "fail gracefully when protocol is not informed" in {

      Put("/tpreparator/reload") ~> Route.seal(api.routes) ~> check {
        status shouldEqual StatusCodes.BadRequest
        responseAs[String] shouldEqual s"""{"errorMessage":"Missing query parameter. [protocol]"}"""
      }
    }
  }

  "/trainer/reload endpoint" should {

    val metadata = MetadataMock.simpleMockedMetadata()
    val actor = TestProbe()
    val defaultParams = ""
    val api = new GenericAPI(system, metadata, defaultParams, Map[String, ActorRef]("trainer" -> actor.ref))

    "call BatchReload" in {

      val protocol = "testProtocol"

      val result = Put(s"/trainer/reload?protocol=$protocol") ~> api.routes ~> runRoute

      val expectedMessage = BatchReload(protocol)
      actor.expectMsg(expectedMessage)

      check{
        status shouldEqual StatusCodes.OK
        responseAs[String] shouldEqual s"""{"result":"Work in progress...Thank you folk!"}"""
      }(result)
    }

    "fail gracefully when protocol is not informed" in {

      Put("/trainer/reload") ~> Route.seal(api.routes) ~> check {
        status shouldEqual StatusCodes.BadRequest
        responseAs[String] shouldEqual s"""{"errorMessage":"Missing query parameter. [protocol]"}"""
      }
    }
  }

  "/evaluator/reload endpoint" should {

    val metadata = MetadataMock.simpleMockedMetadata()
    val actor = TestProbe()
    val defaultParams = ""
    val api = new GenericAPI(system, metadata, defaultParams, Map[String, ActorRef]("evaluator" -> actor.ref))

    "call BatchReload" in {

      val protocol = "testProtocol"

      val result = Put(s"/evaluator/reload?protocol=$protocol") ~> api.routes ~> runRoute

      val expectedMessage = BatchReload(protocol)
      actor.expectMsg(expectedMessage)

      check{
        status shouldEqual StatusCodes.OK
        responseAs[String] shouldEqual s"""{"result":"Work in progress...Thank you folk!"}"""
      }(result)
    }

    "fail gracefully when protocol is not informed" in {

      Put("/evaluator/reload") ~> Route.seal(api.routes) ~> check {
        status shouldEqual StatusCodes.BadRequest
        responseAs[String] shouldEqual s"""{"errorMessage":"Missing query parameter. [protocol]"}"""
      }
    }
  }

  "/predictor/health endpoint" should {

    val metadata = MetadataMock.simpleMockedMetadata()
    val actor = TestProbe()
    val defaultParams = ""
    val api = new GenericAPI(system, metadata, defaultParams, Map[String, ActorRef]("predictor" -> actor.ref))

    "interpret and respond OK with media type json" in {

      val response = Status.OK

      val result = Get("/predictor/health") ~> api.routes ~> runRoute

      val expectedMessage = OnlineHealthCheck
      actor.expectMsg(expectedMessage)
      actor.reply(response)

      check {
        status shouldEqual StatusCode.int2StatusCode(200)
        contentType shouldEqual ContentTypes.`application/json`
        responseAs[String] shouldEqual s"""{"status":"$response","additionalMessage":""}"""
      }(result)
    }

    "interpret and respond NOK with media type json" in {

      val response = Status.NOK

      val result = Get("/predictor/health") ~> api.routes ~> runRoute

      val expectedMessage = OnlineHealthCheck
      actor.expectMsg(expectedMessage)
      actor.reply(response)

      check {
        status shouldEqual StatusCode.int2StatusCode(503)
        contentType shouldEqual ContentTypes.`application/json`
        responseAs[String] shouldEqual s"""{"status":"$response","additionalMessage":"Engine did not returned a healthy status."}"""
      }(result)
    }

    "fail fast when the timeout is reached" in {

      val metadata = EngineMetadata(
        name = "test",
        actions = null,
        artifactsRemotePath = "",
        artifactManagerType = "",
        s3BucketName = "",
        batchActionTimeout = 50,
        engineType = "",
        hdfsHost = "",
        healthCheckTimeout = 5,
        onlineActionTimeout = 50,
        pipelineActions = List(),
        reloadStateTimeout = Some(250),
        reloadTimeout = 50,
        version = "1"
      )

      val api = new GenericAPI(system, metadata, defaultParams, Map[String, ActorRef]("predictor" -> actor.ref))

      val result = Get("/predictor/health") ~> api.routes ~> runRoute

      val expectedMessage = OnlineHealthCheck
      actor.expectMsg(expectedMessage)

      check {
        status shouldEqual StatusCodes.InternalServerError
        responseAs[String] shouldEqual """{"errorMessage":"The engine was not able to provide a response within the specified timeout."}"""
      }(result)

    }
  }

  "/feedback/health endpoint" should {

    val metadata = MetadataMock.simpleMockedMetadata()
    val actor = TestProbe()
    val defaultParams = ""
    val api = new GenericAPI(system, metadata, defaultParams, Map[String, ActorRef]("feedback" -> actor.ref))

    "interpret and respond OK with media type json" in {

      val response = Status.OK

      val result = Get("/feedback/health") ~> api.routes ~> runRoute

      val expectedMessage = OnlineHealthCheck
      actor.expectMsg(expectedMessage)
      actor.reply(response)

      check {
        status shouldEqual StatusCode.int2StatusCode(200)
        contentType shouldEqual ContentTypes.`application/json`
        responseAs[String] shouldEqual s"""{"status":"$response","additionalMessage":""}"""
      }(result)
    }

    "interpret and respond NOK with media type json" in {

      val response = Status.NOK

      val result = Get("/feedback/health") ~> api.routes ~> runRoute

      val expectedMessage = OnlineHealthCheck
      actor.expectMsg(expectedMessage)
      actor.reply(response)

      check {
        status shouldEqual StatusCode.int2StatusCode(503)
        contentType shouldEqual ContentTypes.`application/json`
        responseAs[String] shouldEqual s"""{"status":"$response","additionalMessage":"Engine did not returned a healthy status."}"""
      }(result)
    }

    "fail fast when the timeout is reached" in {

      val metadata = EngineMetadata(
        name = "test",
        actions = null,
        artifactsRemotePath = "",
        artifactManagerType = "",
        s3BucketName = "",
        batchActionTimeout = 50,
        engineType = "",
        hdfsHost = "",
        healthCheckTimeout = 5,
        onlineActionTimeout = 50,
        pipelineActions = List(),
        reloadStateTimeout = Some(250),
        reloadTimeout = 50,
        version = "1"
      )

      val api = new GenericAPI(system, metadata, defaultParams, Map[String, ActorRef]("feedback" -> actor.ref))

      val result = Get("/feedback/health") ~> api.routes ~> runRoute

      val expectedMessage = OnlineHealthCheck
      actor.expectMsg(expectedMessage)

      check {
        status shouldEqual StatusCodes.InternalServerError
        responseAs[String] shouldEqual """{"errorMessage":"The engine was not able to provide a response within the specified timeout."}"""
      }(result)

    }
  }

  "/acquisitor/health endpoint" should {

    val metadata = MetadataMock.simpleMockedMetadata()
    val actor = TestProbe()
    val defaultParams = ""
    val api = new GenericAPI(system, metadata, defaultParams, Map[String, ActorRef]("acquisitor" -> actor.ref))

    "interpret and respond OK with media type json" in {

      val response = Status.OK

      val result = Get("/acquisitor/health") ~> api.routes ~> runRoute

      val expectedMessage = BatchHealthCheck
      actor.expectMsg(expectedMessage)
      actor.reply(response)

      check {
        status shouldEqual StatusCode.int2StatusCode(200)
        contentType shouldEqual ContentTypes.`application/json`
        responseAs[String] shouldEqual s"""{"status":"$response","additionalMessage":""}"""
      }(result)
    }

    "interpret and respond NOK ith media type json" in {

      val response = Status.NOK

      val result = Get("/acquisitor/health") ~> api.routes ~> runRoute

      val expectedMessage = BatchHealthCheck
      actor.expectMsg(expectedMessage)
      actor.reply(response)

      check {
        status shouldEqual StatusCode.int2StatusCode(503)
        contentType shouldEqual ContentTypes.`application/json`
        responseAs[String] shouldEqual s"""{"status":"$response","additionalMessage":"Engine did not returned a healthy status."}"""
      }(result)
    }

    "fail fast when the timeout is reached" in {

      val metadata = EngineMetadata(
        name = "test",
        actions = null,
        artifactsRemotePath = "",
        artifactManagerType = "",
        s3BucketName = "",
        batchActionTimeout = 50,
        engineType = "",
        hdfsHost = "",
        healthCheckTimeout = 5,
        onlineActionTimeout = 50,
        pipelineActions = List(),
        reloadStateTimeout = Some(250),
        reloadTimeout = 50,
        version = "1"
      )

      val api = new GenericAPI(system, metadata, defaultParams, Map[String, ActorRef]("acquisitor" -> actor.ref))

      val result = Get("/acquisitor/health") ~> api.routes ~> runRoute

      val expectedMessage = BatchHealthCheck
      actor.expectMsg(expectedMessage)

      check {
        status shouldEqual StatusCodes.InternalServerError
        responseAs[String] shouldEqual """{"errorMessage":"The engine was not able to provide a response within the specified timeout."}"""
      }(result)

    }
  }

  "/tpreparator/health endpoint" should {

    val metadata = MetadataMock.simpleMockedMetadata()
    val actor = TestProbe()
    val defaultParams = ""
    val api = new GenericAPI(system, metadata, defaultParams, Map[String, ActorRef]("tpreparator" -> actor.ref))

    "interpret and respond OK with media type json" in {

      val response = Status.OK

      val result = Get("/tpreparator/health") ~> api.routes ~> runRoute

      val expectedMessage = BatchHealthCheck
      actor.expectMsg(expectedMessage)
      actor.reply(response)

      check {
        status shouldEqual StatusCode.int2StatusCode(200)
        contentType shouldEqual ContentTypes.`application/json`
        responseAs[String] shouldEqual s"""{"status":"$response","additionalMessage":""}"""
      }(result)
    }

    "interpret and respond NOK with media type json" in {

      val response = Status.NOK

      val result = Get("/tpreparator/health") ~> api.routes ~> runRoute

      val expectedMessage = BatchHealthCheck
      actor.expectMsg(expectedMessage)
      actor.reply(response)

      check {
        status shouldEqual StatusCode.int2StatusCode(503)
        contentType shouldEqual ContentTypes.`application/json`
        responseAs[String] shouldEqual s"""{"status":"$response","additionalMessage":"Engine did not returned a healthy status."}"""
      }(result)
    }

    "fail fast when the timeout is reached" in {

      val metadata = EngineMetadata(
        name = "test",
        actions = null,
        artifactsRemotePath = "",
        artifactManagerType = "",
        s3BucketName = "",
        batchActionTimeout = 50,
        engineType = "",
        hdfsHost = "",
        healthCheckTimeout = 5,
        onlineActionTimeout = 50,
        pipelineActions = List(),
        reloadStateTimeout = Some(250),
        reloadTimeout = 50,
        version = "1"
      )

      val api = new GenericAPI(system, metadata, defaultParams, Map[String, ActorRef]("tpreparator" -> actor.ref))

      val result = Get("/tpreparator/health") ~> api.routes ~> runRoute

      val expectedMessage = BatchHealthCheck
      actor.expectMsg(expectedMessage)

      check {
        status shouldEqual StatusCodes.InternalServerError
        responseAs[String] shouldEqual """{"errorMessage":"The engine was not able to provide a response within the specified timeout."}"""
      }(result)

    }
  }

  "/trainer/health endpoint" should {

    val metadata = MetadataMock.simpleMockedMetadata()
    val actor = TestProbe()
    val defaultParams = ""
    val api = new GenericAPI(system, metadata, defaultParams, Map[String, ActorRef]("trainer" -> actor.ref))

    "interpret and respond OK with media type json" in {

      val response = Status.OK

      val result = Get("/trainer/health") ~> api.routes ~> runRoute

      val expectedMessage = BatchHealthCheck
      actor.expectMsg(expectedMessage)
      actor.reply(response)

      check {
        status shouldEqual StatusCode.int2StatusCode(200)
        contentType shouldEqual ContentTypes.`application/json`
        responseAs[String] shouldEqual s"""{"status":"$response","additionalMessage":""}"""
      }(result)
    }

    "interpret and respond NOK with media type json" in {

      val response = Status.NOK

      val result = Get("/trainer/health") ~> api.routes ~> runRoute

      val expectedMessage = BatchHealthCheck
      actor.expectMsg(expectedMessage)
      actor.reply(response)

      check {
        status shouldEqual StatusCode.int2StatusCode(503)
        contentType shouldEqual ContentTypes.`application/json`
        responseAs[String] shouldEqual s"""{"status":"$response","additionalMessage":"Engine did not returned a healthy status."}"""
      }(result)
    }

    "fail fast when the timeout is reached" in {

      val metadata = EngineMetadata(
        name = "test",
        actions = null,
        artifactsRemotePath = "",
        artifactManagerType = "",
        s3BucketName = "",
        batchActionTimeout = 50,
        engineType = "",
        hdfsHost = "",
        healthCheckTimeout = 5,
        onlineActionTimeout = 50,
        pipelineActions = List(),
        reloadStateTimeout = Some(250),
        reloadTimeout = 50,
        version = "1"
      )

      val api = new GenericAPI(system, metadata, defaultParams, Map[String, ActorRef]("trainer" -> actor.ref))

      val result = Get("/trainer/health") ~> api.routes ~> runRoute

      val expectedMessage = BatchHealthCheck
      actor.expectMsg(expectedMessage)

      check {
        status shouldEqual StatusCodes.InternalServerError
        responseAs[String] shouldEqual """{"errorMessage":"The engine was not able to provide a response within the specified timeout."}"""
      }(result)

    }

  }

  "/evaluator/health endpoint" should {

    val metadata = MetadataMock.simpleMockedMetadata()
    val actor = TestProbe()
    val defaultParams = ""
    val api = new GenericAPI(system, metadata, defaultParams, Map[String, ActorRef]("evaluator" -> actor.ref))

    "interpret and respond OK with media type json" in {

      val response = Status.OK

      val result = Get("/evaluator/health") ~> api.routes ~> runRoute

      val expectedMessage = BatchHealthCheck
      actor.expectMsg(expectedMessage)
      actor.reply(response)

      check {
        status shouldEqual StatusCode.int2StatusCode(200)
        contentType shouldEqual ContentTypes.`application/json`
        responseAs[String] shouldEqual s"""{"status":"$response","additionalMessage":""}"""
      }(result)
    }

    "interpret and respond NOK with media type json" in {

      val response = Status.NOK

      val result = Get("/evaluator/health") ~> api.routes ~> runRoute

      val expectedMessage = BatchHealthCheck
      actor.expectMsg(expectedMessage)
      actor.reply(response)

      check {
        status shouldEqual StatusCode.int2StatusCode(503)
        contentType shouldEqual ContentTypes.`application/json`
        responseAs[String] shouldEqual s"""{"status":"$response","additionalMessage":"Engine did not returned a healthy status."}"""
      }(result)
    }

    "fail fast when the timeout is reached" in {

      val metadata = EngineMetadata(
        name = "test",
        actions = null,
        artifactsRemotePath = "",
        artifactManagerType = "",
        s3BucketName = "",
        batchActionTimeout = 50,
        engineType = "",
        hdfsHost = "",
        healthCheckTimeout = 5,
        onlineActionTimeout = 50,
        pipelineActions = List(),
        reloadStateTimeout = Some(250),
        reloadTimeout = 50,
        version = "1"
      )

      val api = new GenericAPI(system, metadata, defaultParams, Map[String, ActorRef]("evaluator" -> actor.ref))

      val result = Get("/evaluator/health") ~> api.routes ~> runRoute

      val expectedMessage = BatchHealthCheck
      actor.expectMsg(expectedMessage)

      check {
        status shouldEqual StatusCodes.InternalServerError
        responseAs[String] shouldEqual """{"errorMessage":"The engine was not able to provide a response within the specified timeout."}"""
      }(result)

    }
  }

  "/acquisitor/status endpoint" should {

    val metadata = MetadataMock.simpleMockedMetadata()
    val actor = TestProbe()
    val defaultParams = ""
    val api = new GenericAPI(system, metadata, defaultParams, Map[String, ActorRef]("acquisitor" -> actor.ref))

    "be requested with a valid protocol" in {
      val protocol = "testProtocol"
      val result = Get(s"/acquisitor/status?protocol=$protocol") ~> api.routes ~> runRoute

      val response = "foi"
      val expectedMessage = BatchExecutionStatus(protocol)
      actor.expectMsg(expectedMessage)
      actor.reply(response)

      check {
        status shouldEqual StatusCode.int2StatusCode(200)
        contentType shouldEqual ContentTypes.`application/json`
        responseAs[String] shouldEqual s"""{"result":"$response"}"""
      }(result)
    }

    "be requested with a invalid protocol" in {
      val protocol = "testProtocol"
      val result = Get(s"/acquisitor/status?protocol=$protocol") ~> api.routes ~> runRoute

      val expectedMessage = BatchExecutionStatus(protocol)
      actor.expectMsg(expectedMessage)
      actor.reply(akka.actor.Status.Failure(new MarvinEExecutorException(s"Protocol $protocol not found!")))

      check {
        status shouldEqual StatusCode.int2StatusCode(503)
        contentType shouldEqual ContentTypes.`application/json`
        responseAs[String] shouldEqual s"""{"errorMessage":"Protocol testProtocol not found!"}"""
      }(result)
    }
  }

  "/tpreparator/status endpoint" should {

    val metadata = MetadataMock.simpleMockedMetadata()
    val actor = TestProbe()
    val defaultParams = ""
    val api = new GenericAPI(system, metadata, defaultParams, Map[String, ActorRef]("tpreparator" -> actor.ref))

    "be requested with a valid protocol" in {
      val protocol = "testProtocol"
      val result = Get(s"/tpreparator/status?protocol=$protocol") ~> api.routes ~> runRoute

      val response = "foi"
      val expectedMessage = BatchExecutionStatus(protocol)
      actor.expectMsg(expectedMessage)
      actor.reply(response)

      check {
        status shouldEqual StatusCode.int2StatusCode(200)
        contentType shouldEqual ContentTypes.`application/json`
        responseAs[String] shouldEqual s"""{"result":"$response"}"""
      }(result)
    }

    "be requested with a invalid protocol" in {
      val protocol = "testProtocol"
      val result = Get(s"/tpreparator/status?protocol=$protocol") ~> api.routes ~> runRoute

      val expectedMessage = BatchExecutionStatus(protocol)
      actor.expectMsg(expectedMessage)
      actor.reply(akka.actor.Status.Failure(new MarvinEExecutorException(s"Protocol $protocol not found!")))

      check {
        status shouldEqual StatusCode.int2StatusCode(503)
        contentType shouldEqual ContentTypes.`application/json`
        responseAs[String] shouldEqual s"""{"errorMessage":"Protocol testProtocol not found!"}"""
      }(result)
    }
  }

  "/trainer/status endpoint" should {

    val metadata = MetadataMock.simpleMockedMetadata()
    val actor = TestProbe()
    val defaultParams = ""
    val api = new GenericAPI(system, metadata, defaultParams, Map[String, ActorRef]("trainer" -> actor.ref))

    "be requested with a valid protocol" in {
      val protocol = "testProtocol"
      val result = Get(s"/trainer/status?protocol=$protocol") ~> api.routes ~> runRoute

      val response = "foi"
      val expectedMessage = BatchExecutionStatus(protocol)
      actor.expectMsg(expectedMessage)
      actor.reply(response)

      check {
        status shouldEqual StatusCode.int2StatusCode(200)
        contentType shouldEqual ContentTypes.`application/json`
        responseAs[String] shouldEqual s"""{"result":"$response"}"""
      }(result)
    }

    "be requested with a invalid protocol" in {
      val protocol = "testProtocol"
      val result = Get(s"/trainer/status?protocol=$protocol") ~> api.routes ~> runRoute

      val expectedMessage = BatchExecutionStatus(protocol)
      actor.expectMsg(expectedMessage)
      actor.reply(akka.actor.Status.Failure(new MarvinEExecutorException(s"Protocol $protocol not found!")))

      check {
        status shouldEqual StatusCode.int2StatusCode(503)
        contentType shouldEqual ContentTypes.`application/json`
        responseAs[String] shouldEqual s"""{"errorMessage":"Protocol testProtocol not found!"}"""
      }(result)
    }
  }

  "/evaluator/status endpoint" should {

    val metadata = MetadataMock.simpleMockedMetadata()
    val actor = TestProbe()
    val defaultParams = ""
    val api = new GenericAPI(system, metadata, defaultParams, Map[String, ActorRef]("evaluator" -> actor.ref))

    "be requested with a valid protocol" in {
      val protocol = "testProtocol"
      val result = Get(s"/evaluator/status?protocol=$protocol") ~> api.routes ~> runRoute

      val response = "foi"
      val expectedMessage = BatchExecutionStatus(protocol)
      actor.expectMsg(expectedMessage)
      actor.reply(response)

      check {
        status shouldEqual StatusCode.int2StatusCode(200)
        contentType shouldEqual ContentTypes.`application/json`
        responseAs[String] shouldEqual s"""{"result":"$response"}"""
      }(result)
    }

    "be requested with a invalid protocol" in {
      val protocol = "testProtocol"
      val result = Get(s"/evaluator/status?protocol=$protocol") ~> api.routes ~> runRoute

      val expectedMessage = BatchExecutionStatus(protocol)
      actor.expectMsg(expectedMessage)
      actor.reply(akka.actor.Status.Failure(new MarvinEExecutorException(s"Protocol $protocol not found!")))

      check {
        status shouldEqual StatusCode.int2StatusCode(503)
        contentType shouldEqual ContentTypes.`application/json`
        responseAs[String] shouldEqual s"""{"errorMessage":"Protocol testProtocol not found!"}"""
      }(result)
    }
  }

  "/pipeline/status endpoint" should {

    val metadata = MetadataMock.simpleMockedMetadata()
    val actor = TestProbe()
    val defaultParams = ""
    val api = new GenericAPI(system, metadata, defaultParams, Map[String, ActorRef]("pipeline" -> actor.ref))

    "be requested with a valid protocol" in {
      val protocol = "testProtocol"
      val result = Get(s"/pipeline/status?protocol=$protocol") ~> api.routes ~> runRoute

      val response = "foi"
      val expectedMessage = PipelineExecutionStatus(protocol)
      actor.expectMsg(expectedMessage)
      actor.reply(response)

      check {
        status shouldEqual StatusCode.int2StatusCode(200)
        contentType shouldEqual ContentTypes.`application/json`
        responseAs[String] shouldEqual s"""{"result":"$response"}"""
      }(result)
    }

    "be requested with a invalid protocol" in {
      val protocol = "testProtocol"
      val result = Get(s"/pipeline/status?protocol=$protocol") ~> api.routes ~> runRoute

      val expectedMessage = PipelineExecutionStatus(protocol)
      actor.expectMsg(expectedMessage)
      actor.reply(akka.actor.Status.Failure(new MarvinEExecutorException(s"Protocol $protocol not found!")))

      check {
        status shouldEqual StatusCode.int2StatusCode(503)
        contentType shouldEqual ContentTypes.`application/json`
        responseAs[String] shouldEqual s"""{"errorMessage":"Protocol testProtocol not found!"}"""
      }(result)
    }
  }

  "getMetadata method" should {
    "return right value" in {
      val metadata = MetadataMock.simpleMockedMetadata()
      val api = new GenericAPI(system, metadata, "", null)

      api.getMetadata shouldEqual metadata
    }
  }

  "getSystem method" should {
    "return right value" in {
      val metadata = MetadataMock.simpleMockedMetadata()
      val api = new GenericAPI(system, metadata, "", null)

      api.getSystem shouldEqual system
    }
  }

  "getEngineParams method" should {
    "return right value" in {
      val metadata = MetadataMock.simpleMockedMetadata()
      val params = "params"
      val api = new GenericAPI(system, metadata, params, null)

      api.getEngineParams shouldEqual params
    }
  }

  "manageableActors method" should {
    "return right value" in {
      val metadata = MetadataMock.simpleMockedMetadata()
      val params = "params"
      val actor1 = TestProbe()
      val actors = Map[String, ActorRef]("actor1" -> actor1.ref)
      val api = new GenericAPI(system, metadata, params, actors)

      api.manageableActors shouldEqual actors
      api.manageableActors("actor1") shouldEqual actor1.ref
    }
  }
}