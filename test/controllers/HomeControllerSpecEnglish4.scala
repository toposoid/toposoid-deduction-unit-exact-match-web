/*
 * Copyright 2021 Linked Ideal LLC.[https://linked-ideal.com/]
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

package controllers

import akka.util.Timeout
import com.ideal.linked.common.DeploymentConverter.conf
import com.ideal.linked.data.accessor.neo4j.Neo4JAccessor
import com.ideal.linked.toposoid.common.ToposoidUtils
import com.ideal.linked.toposoid.knowledgebase.regist.model.{Knowledge, KnowledgeSentenceSet, PropositionRelation}
import com.ideal.linked.toposoid.protocol.model.base.AnalyzedSentenceObjects
import com.ideal.linked.toposoid.protocol.model.parser.InputSentence
import com.ideal.linked.toposoid.sentence.transformer.neo4j.Sentence2Neo4jTransformer
import org.scalatest.{BeforeAndAfter, BeforeAndAfterAll}
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Play.materializer
import play.api.http.Status.OK
import play.api.libs.json.Json
import play.api.test.Helpers.{POST, contentType, status, _}
import play.api.test.{FakeRequest, _}
import io.jvm.uuid.UUID

import scala.concurrent.duration.DurationInt

class HomeControllerSpecEnglish4 extends PlaySpec with BeforeAndAfter with BeforeAndAfterAll with GuiceOneAppPerSuite  with DefaultAwaitTimeout with Injecting {

  before {
    Neo4JAccessor.delete()
  }

  override def beforeAll(): Unit = {
    Neo4JAccessor.delete()
  }

  override def afterAll(): Unit = {
    Neo4JAccessor.delete()
  }

  override implicit def defaultAwaitTimeout: Timeout = 600.seconds
  val controller: HomeController = inject[HomeController]

  "The specification31" should {
    "returns an appropriate response" in {
      Sentence2Neo4jTransformer.createGraphAuto(List(UUID.random.toString), List(Knowledge("Fear often exaggerates danger.","en_US", "{}", false)))
      Sentence2Neo4jTransformer.createGraphAuto(List(UUID.random.toString), List(Knowledge("Grasp Fortune by the forelock.","en_US", "{}", false)))
      val inputSentence = Json.toJson(InputSentence(List(Knowledge("Fear often exaggerates danger.","en_US", "{}", false), Knowledge("Grasp Fortune by the forelock.","en_US", "{}", false)), List(Knowledge("Time is money.","en_US", "{}", false), Knowledge("God helps those who help themselves.","en_US", "{}", false)))).toString()
      val json = ToposoidUtils.callComponent(inputSentence, conf.getString("SENTENCE_PARSER_EN_WEB_HOST"), "9007", "analyze")
      val fr = FakeRequest(POST, "/execute")
        .withHeaders("Content-type" -> "application/json")
        .withJsonBody(Json.parse(json))
      val result = call(controller.execute(), fr)
      status(result) mustBe OK
      contentType(result) mustBe Some("application/json")
      val jsonResult: String = contentAsJson(result).toString()
      val analyzedSentenceObjects: AnalyzedSentenceObjects = Json.parse(jsonResult).as[AnalyzedSentenceObjects]
      assert(analyzedSentenceObjects.analyzedSentenceObjects.filter(_.deductionResultMap.get("0").get.status).size == 2)
      assert(analyzedSentenceObjects.analyzedSentenceObjects.filter(_.deductionResultMap.get("1").get.status).size == 0)
    }
  }

  "The specification32" should {
    "returns an appropriate response" in {
      Sentence2Neo4jTransformer.createGraphAuto(List(UUID.random.toString), List(Knowledge("Time is money.","en_US", "{}", false)))
      Sentence2Neo4jTransformer.createGraphAuto(List(UUID.random.toString), List(Knowledge("God helps those who help themselves.","en_US", "{}", false)))
      val inputSentence = Json.toJson(InputSentence(List(Knowledge("Fear often exaggerates danger.","en_US", "{}", false), Knowledge("Grasp Fortune by the forelock.","en_US", "{}", false)), List(Knowledge("Time is money.","en_US", "{}", false), Knowledge("God helps those who help themselves.","en_US", "{}", false)))).toString()
      val json = ToposoidUtils.callComponent(inputSentence, conf.getString("SENTENCE_PARSER_EN_WEB_HOST"), "9007", "analyze")
      val fr = FakeRequest(POST, "/execute")
        .withHeaders("Content-type" -> "application/json")
        .withJsonBody(Json.parse(json))
      val result = call(controller.execute(), fr)
      status(result) mustBe OK
      contentType(result) mustBe Some("application/json")
      val jsonResult: String = contentAsJson(result).toString()
      val analyzedSentenceObjects: AnalyzedSentenceObjects = Json.parse(jsonResult).as[AnalyzedSentenceObjects]
      assert(analyzedSentenceObjects.analyzedSentenceObjects.filter(_.deductionResultMap.get("0").get.status).size == 0)
      assert(analyzedSentenceObjects.analyzedSentenceObjects.filter(_.deductionResultMap.get("1").get.status).size == 2)
    }
  }

  "The specification33" should {
    "returns an appropriate response" in {
      val knowledgeSentenceSet = KnowledgeSentenceSet(
        List(Knowledge("Fear often exaggerates danger.","en_US", "{}")),
        List.empty[PropositionRelation],
        List(Knowledge("Time is money.","en_US", "{}", false)),
        List.empty[PropositionRelation])
      Sentence2Neo4jTransformer.createGraph(UUID.random.toString, knowledgeSentenceSet)
      val inputSentence = Json.toJson(InputSentence(List(Knowledge("Fear often exaggerates danger.","en_US", "{}", false), Knowledge("Grasp Fortune by the forelock.","en_US", "{}", false)), List(Knowledge("Time is money.","en_US", "{}", false), Knowledge("God helps those who help themselves.","en_US", "{}", false)))).toString()
      val json = ToposoidUtils.callComponent(inputSentence, conf.getString("SENTENCE_PARSER_EN_WEB_HOST"), "9007", "analyze")
      val fr = FakeRequest(POST, "/execute")
        .withHeaders("Content-type" -> "application/json")
        .withJsonBody(Json.parse(json))
      val result = call(controller.execute(), fr)
      status(result) mustBe OK
      contentType(result) mustBe Some("application/json")
      val jsonResult: String = contentAsJson(result).toString()
      val analyzedSentenceObjects: AnalyzedSentenceObjects = Json.parse(jsonResult).as[AnalyzedSentenceObjects]
      assert(analyzedSentenceObjects.analyzedSentenceObjects.filter(_.deductionResultMap.get("0").get.status).size == 0)
      assert(analyzedSentenceObjects.analyzedSentenceObjects.filter(_.deductionResultMap.get("1").get.status).size == 0)
    }
  }

  "The specification34" should {
    "returns an appropriate response" in {
      val knowledgeSentenceSet = KnowledgeSentenceSet(
        List(Knowledge("Fear often exaggerates danger.","en_US", "{}"), Knowledge("Grasp Fortune by the forelock.","en_US", "{}")),
        List.empty[PropositionRelation],
        List(Knowledge("Time is money.","en_US", "{}", false)),
        List.empty[PropositionRelation])
      Sentence2Neo4jTransformer.createGraph(UUID.random.toString, knowledgeSentenceSet)
      val inputSentence = Json.toJson(InputSentence(List(Knowledge("Fear often exaggerates danger.","en_US", "{}", false), Knowledge("Grasp Fortune by the forelock.","en_US", "{}", false)), List(Knowledge("Time is money.","en_US", "{}", false), Knowledge("God helps those who help themselves.","en_US", "{}", false)))).toString()
      val json = ToposoidUtils.callComponent(inputSentence, conf.getString("SENTENCE_PARSER_EN_WEB_HOST"), "9007", "analyze")
      val fr = FakeRequest(POST, "/execute")
        .withHeaders("Content-type" -> "application/json")
        .withJsonBody(Json.parse(json))
      val result = call(controller.execute(), fr)
      status(result) mustBe OK
      contentType(result) mustBe Some("application/json")
      val jsonResult: String = contentAsJson(result).toString()
      val analyzedSentenceObjects: AnalyzedSentenceObjects = Json.parse(jsonResult).as[AnalyzedSentenceObjects]
      assert(analyzedSentenceObjects.analyzedSentenceObjects.filter(_.deductionResultMap.get("0").get.status).size == 0)
      assert(analyzedSentenceObjects.analyzedSentenceObjects.filter(_.deductionResultMap.get("1").get.status).size == 0)
    }
  }

  "The specification35" should {
    "returns an appropriate response" in {
      val knowledgeSentenceSet = KnowledgeSentenceSet(
        List(Knowledge("Fear often exaggerates danger.","en_US", "{}", false)),
        List.empty[PropositionRelation],
        List(Knowledge("Time is money.","en_US", "{}", false), Knowledge("God helps those who help themselves.","en_US", "{}", false)),
        List.empty[PropositionRelation])
      Sentence2Neo4jTransformer.createGraph(UUID.random.toString, knowledgeSentenceSet)
      val inputSentence = Json.toJson(InputSentence(List(Knowledge("Fear often exaggerates danger.","en_US", "{}", false), Knowledge("Grasp Fortune by the forelock.","en_US", "{}", false)), List(Knowledge("Time is money.","en_US", "{}", false), Knowledge("God helps those who help themselves.","en_US", "{}", false)))).toString()
      val json = ToposoidUtils.callComponent(inputSentence, conf.getString("SENTENCE_PARSER_EN_WEB_HOST"), "9007", "analyze")
      val fr = FakeRequest(POST, "/execute")
        .withHeaders("Content-type" -> "application/json")
        .withJsonBody(Json.parse(json))
      val result = call(controller.execute(), fr)
      status(result) mustBe OK
      contentType(result) mustBe Some("application/json")
      val jsonResult: String = contentAsJson(result).toString()
      val analyzedSentenceObjects: AnalyzedSentenceObjects = Json.parse(jsonResult).as[AnalyzedSentenceObjects]
      assert(analyzedSentenceObjects.analyzedSentenceObjects.filter(_.deductionResultMap.get("0").get.status).size == 0)
      assert(analyzedSentenceObjects.analyzedSentenceObjects.filter(_.deductionResultMap.get("1").get.status).size == 0)
    }
  }

  "The specification36" should {
    "returns an appropriate response" in {
      val knowledgeSentenceSet = KnowledgeSentenceSet(
        List(Knowledge("Fear often exaggerates danger.","en_US", "{}", false), Knowledge("Grasp Fortune by the forelock.","en_US", "{}", false)),
        List.empty[PropositionRelation],
        List(Knowledge("Time is money.","en_US", "{}", false), Knowledge("God helps those who help themselves.","en_US", "{}", false)),
        List.empty[PropositionRelation])
      Sentence2Neo4jTransformer.createGraph(UUID.random.toString, knowledgeSentenceSet)
      val inputSentence = Json.toJson(InputSentence(List(Knowledge("Fear often exaggerates danger.","en_US", "{}", false), Knowledge("Grasp Fortune by the forelock.","en_US", "{}", false)), List(Knowledge("Time is money.","en_US", "{}", false), Knowledge("God helps those who help themselves.","en_US", "{}", false)))).toString()
      val json = ToposoidUtils.callComponent(inputSentence, conf.getString("SENTENCE_PARSER_EN_WEB_HOST"), "9007", "analyze")
      val fr = FakeRequest(POST, "/execute")
        .withHeaders("Content-type" -> "application/json")
        .withJsonBody(Json.parse(json))
      val result = call(controller.execute(), fr)
      status(result) mustBe OK
      contentType(result) mustBe Some("application/json")
      val jsonResult: String = contentAsJson(result).toString()
      val analyzedSentenceObjects: AnalyzedSentenceObjects = Json.parse(jsonResult).as[AnalyzedSentenceObjects]
      assert(analyzedSentenceObjects.analyzedSentenceObjects.filter(_.deductionResultMap.get("0").get.status).size == 0)
      assert(analyzedSentenceObjects.analyzedSentenceObjects.filter(_.deductionResultMap.get("1").get.status).size == 0)
    }
  }

  "The specification37" should {
    "returns an appropriate response" in {
      Sentence2Neo4jTransformer.createGraphAuto(List(UUID.random.toString), List(Knowledge("Fear often exaggerates danger.","en_US", "{}", false)))
      Sentence2Neo4jTransformer.createGraphAuto(List(UUID.random.toString), List(Knowledge("Grasp Fortune by the forelock.","en_US", "{}", false)))
      val knowledgeSentenceSet = KnowledgeSentenceSet(
        List(Knowledge("Fear often exaggerates danger.","en_US", "{}", false), Knowledge("Grasp Fortune by the forelock.","en_US", "{}", false)),
        List.empty[PropositionRelation],
        List(Knowledge("Time is money.","en_US", "{}", false), Knowledge("God helps those who help themselves.","en_US", "{}", false)),
        List.empty[PropositionRelation])
      Sentence2Neo4jTransformer.createGraph(UUID.random.toString, knowledgeSentenceSet)
      val inputSentence = Json.toJson(InputSentence(List(Knowledge("Fear often exaggerates danger.","en_US", "{}", false), Knowledge("Grasp Fortune by the forelock.","en_US", "{}", false)), List(Knowledge("Time is money.","en_US", "{}", false), Knowledge("God helps those who help themselves.","en_US", "{}", false)))).toString()
      val json = ToposoidUtils.callComponent(inputSentence, conf.getString("SENTENCE_PARSER_EN_WEB_HOST"), "9007", "analyze")
      val fr = FakeRequest(POST, "/execute")
        .withHeaders("Content-type" -> "application/json")
        .withJsonBody(Json.parse(json))
      val result = call(controller.execute(), fr)
      status(result) mustBe OK
      contentType(result) mustBe Some("application/json")
      val jsonResult: String = contentAsJson(result).toString()
      val analyzedSentenceObjects: AnalyzedSentenceObjects = Json.parse(jsonResult).as[AnalyzedSentenceObjects]
      assert(analyzedSentenceObjects.analyzedSentenceObjects.filter(_.deductionResultMap.get("0").get.status).size == 2)
      assert(analyzedSentenceObjects.analyzedSentenceObjects.filter(_.deductionResultMap.get("1").get.status).size == 2)
    }
  }

  "The specification38" should {
    "returns an appropriate response" in {
      Sentence2Neo4jTransformer.createGraphAuto(List(UUID.random.toString), List(Knowledge("Fear often exaggerates danger.","en_US", "{}", false)))
      val knowledgeSentenceSet = KnowledgeSentenceSet(
        List(Knowledge("Fear often exaggerates danger.","en_US", "{}", false), Knowledge("Grasp Fortune by the forelock.","en_US", "{}", false)),
        List.empty[PropositionRelation],
        List(Knowledge("Time is money.","en_US", "{}", false), Knowledge("God helps those who help themselves.","en_US", "{}", false)),
        List.empty[PropositionRelation])
      Sentence2Neo4jTransformer.createGraph(UUID.random.toString, knowledgeSentenceSet)
      val inputSentence = Json.toJson(InputSentence(List(Knowledge("Fear often exaggerates danger.","en_US", "{}", false), Knowledge("Grasp Fortune by the forelock.","en_US", "{}", false)), List(Knowledge("Time is money.","en_US", "{}", false), Knowledge("God helps those who help themselves.","en_US", "{}", false)))).toString()
      val json = ToposoidUtils.callComponent(inputSentence, conf.getString("SENTENCE_PARSER_EN_WEB_HOST"), "9007", "analyze")
      val fr = FakeRequest(POST, "/execute")
        .withHeaders("Content-type" -> "application/json")
        .withJsonBody(Json.parse(json))
      val result = call(controller.execute(), fr)
      status(result) mustBe OK
      contentType(result) mustBe Some("application/json")
      val jsonResult: String = contentAsJson(result).toString()
      val analyzedSentenceObjects: AnalyzedSentenceObjects = Json.parse(jsonResult).as[AnalyzedSentenceObjects]
      assert(analyzedSentenceObjects.analyzedSentenceObjects.filter(_.deductionResultMap.get("0").get.status).size == 1)
      assert(analyzedSentenceObjects.analyzedSentenceObjects.filter(_.deductionResultMap.get("1").get.status).size == 0)
    }
  }

  "The specification39" should {
    "returns an appropriate response" in {
      Sentence2Neo4jTransformer.createGraphAuto(List(UUID.random.toString), List(Knowledge("Time is money.","en_US", "{}", false)))
      val knowledgeSentenceSet = KnowledgeSentenceSet(
        List(Knowledge("Fear often exaggerates danger.","en_US", "{}", false), Knowledge("Grasp Fortune by the forelock.","en_US", "{}", false)),
        List.empty[PropositionRelation],
        List(Knowledge("Time is money.","en_US", "{}", false), Knowledge("God helps those who help themselves.","en_US", "{}", false)),
        List.empty[PropositionRelation])
      Sentence2Neo4jTransformer.createGraph(UUID.random.toString, knowledgeSentenceSet)
      val inputSentence = Json.toJson(InputSentence(List(Knowledge("Fear often exaggerates danger.","en_US", "{}", false), Knowledge("Grasp Fortune by the forelock.","en_US", "{}", false)), List(Knowledge("Time is money.","en_US", "{}", false), Knowledge("God helps those who help themselves.","en_US", "{}", false)))).toString()
      val json = ToposoidUtils.callComponent(inputSentence, conf.getString("SENTENCE_PARSER_EN_WEB_HOST"), "9007", "analyze")
      val fr = FakeRequest(POST, "/execute")
        .withHeaders("Content-type" -> "application/json")
        .withJsonBody(Json.parse(json))
      val result = call(controller.execute(), fr)
      status(result) mustBe OK
      contentType(result) mustBe Some("application/json")
      val jsonResult: String = contentAsJson(result).toString()
      val analyzedSentenceObjects: AnalyzedSentenceObjects = Json.parse(jsonResult).as[AnalyzedSentenceObjects]
      assert(analyzedSentenceObjects.analyzedSentenceObjects.filter(_.deductionResultMap.get("0").get.status).size == 0)
      assert(analyzedSentenceObjects.analyzedSentenceObjects.filter(_.deductionResultMap.get("1").get.status).size == 1)
    }
  }

  "The specification40" should {
    "returns an appropriate response" in {
      Sentence2Neo4jTransformer.createGraphAuto(List(UUID.random.toString), List(Knowledge("Fear often exaggerates danger.","en_US", "{}", false)))
      Sentence2Neo4jTransformer.createGraphAuto(List(UUID.random.toString), List(Knowledge("Time is money.","en_US", "{}", false)))
      val knowledgeSentenceSet = KnowledgeSentenceSet(
        List(Knowledge("Fear often exaggerates danger.","en_US", "{}", false), Knowledge("Grasp Fortune by the forelock.","en_US", "{}", false)),
        List.empty[PropositionRelation],
        List(Knowledge("Time is money.","en_US", "{}", false), Knowledge("God helps those who help themselves.","en_US", "{}", false)),
        List.empty[PropositionRelation])
      Sentence2Neo4jTransformer.createGraph(UUID.random.toString, knowledgeSentenceSet)
      val inputSentence = Json.toJson(InputSentence(List(Knowledge("Fear often exaggerates danger.","en_US", "{}", false), Knowledge("Grasp Fortune by the forelock.","en_US", "{}", false)), List(Knowledge("Time is money.","en_US", "{}", false), Knowledge("God helps those who help themselves.","en_US", "{}", false)))).toString()
      val json = ToposoidUtils.callComponent(inputSentence, conf.getString("SENTENCE_PARSER_EN_WEB_HOST"), "9007", "analyze")
      val fr = FakeRequest(POST, "/execute")
        .withHeaders("Content-type" -> "application/json")
        .withJsonBody(Json.parse(json))
      val result = call(controller.execute(), fr)
      status(result) mustBe OK
      contentType(result) mustBe Some("application/json")
      val jsonResult: String = contentAsJson(result).toString()
      val analyzedSentenceObjects: AnalyzedSentenceObjects = Json.parse(jsonResult).as[AnalyzedSentenceObjects]
      assert(analyzedSentenceObjects.analyzedSentenceObjects.filter(_.deductionResultMap.get("0").get.status).size == 1)
      assert(analyzedSentenceObjects.analyzedSentenceObjects.filter(_.deductionResultMap.get("1").get.status).size == 1)
    }
  }

}
