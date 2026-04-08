/*
 * Copyright (C) 2025  Linked Ideal LLC.[https://linked-ideal.com/]
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package controllers

import org.apache.pekko.util.Timeout
import com.ideal.linked.common.DeploymentConverter.conf
import com.ideal.linked.toposoid.common.{SentenceType, TRANSVERSAL_STATE, ToposoidUtils, TransversalState, ActionModeType}
import com.ideal.linked.toposoid.knowledgebase.regist.model.{Knowledge, PropositionRelation}
import com.ideal.linked.toposoid.protocol.model.base.AnalyzedSentenceObjects
import com.ideal.linked.toposoid.protocol.model.parser.{InputSentenceForParser, KnowledgeForParser, KnowledgeSentenceSetForParser}
import com.ideal.linked.toposoid.sentence.transformer.neo4j.Sentence2Neo4jTransformer
import com.ideal.linked.toposoid.test.utils.TestUtils
import org.scalatest.{BeforeAndAfter, BeforeAndAfterAll}
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Play.materializer
import play.api.http.Status.OK
import play.api.libs.json.Json
import play.api.test.Helpers.{POST, contentType, status, _}
import play.api.test.{FakeRequest, _}
//import io.jvm.uuid.UUID

import scala.concurrent.duration.DurationInt
import com.ideal.linked.toposoid.protocol.model.base.VerifyingEdges

class HomeControllerSpecEnglish extends PlaySpec with BeforeAndAfter with BeforeAndAfterAll with GuiceOneAppPerSuite  with DefaultAwaitTimeout with Injecting {

  val transversalState:TransversalState = TransversalState(userId="test-user", username="guest", roleId=0, csrfToken = "")
  val transversalStateJson:String = Json.toJson(transversalState).toString()

  before {
    TestUtilsEx.deleteNeo4JAllData(transversalState)
  }

  override def beforeAll(): Unit = {
    TestUtilsEx.deleteNeo4JAllData(transversalState)
  }

  override def afterAll(): Unit = {
    TestUtilsEx.deleteNeo4JAllData(transversalState)
  }

  override implicit def defaultAwaitTimeout: Timeout = 600.seconds
  val controller: HomeController = inject[HomeController]
  
  "The specification1" should {
    val sentence1 = "Mark has overcome many problems."
    val paraphrase1 = "Mark has overcome many problems." 

    "returns an appropriate response" in {
      val propositionId1 = java.util.UUID.randomUUID().toString
      val sentenceId1 = java.util.UUID.randomUUID().toString
      val knowledge1 = Knowledge(sentence1,"en_US", "{}", false)
      val paraphraseKnowledge1 = Knowledge(paraphrase1,"en_US", "{}", false)
      TestUtilsEx.registerSingleClaim(KnowledgeForParser(propositionId1, sentenceId1, knowledge1), transversalState)
      val propositionIdForInference1 = java.util.UUID.randomUUID().toString
      val sentenceIdForInference1 = java.util.UUID.randomUUID().toString
      val premiseKnowledge = List.empty[KnowledgeForParser]
      val claimKnowledge = List(KnowledgeForParser(propositionIdForInference1, sentenceIdForInference1, paraphraseKnowledge1))
      val inputSentence = Json.toJson(InputSentenceForParser(premiseKnowledge, claimKnowledge, ActionModeType.DEDUCTION_MODE.index)).toString()
      val json = ToposoidUtils.callComponent(inputSentence, conf.getString("TOPOSOID_SENTENCE_PARSER_EN_WEB_HOST"), conf.getString("TOPOSOID_SENTENCE_PARSER_EN_WEB_PORT"), "analyze", transversalState)
      val fr = FakeRequest(POST, "/execute")
        .withHeaders("Content-type" -> "application/json", TRANSVERSAL_STATE.str -> transversalStateJson)
        .withJsonBody(Json.parse(json))
      val result = call(controller.execute(), fr)
      status(result) mustBe OK
      contentType(result) mustBe Some("application/json")
      val jsonResult: String = contentAsJson(result).toString()

      val verifyingEdgesList: List[VerifyingEdges] = Json.parse(jsonResult).as[List[VerifyingEdges]]
      assert(verifyingEdgesList.size == 1)

      TestUtilsEx.checkMatchedBothSide(json=json, sentenceId = sentenceIdForInference1, verifyingEdgesList=verifyingEdgesList, correctSize=5)
    }
  }

  "The specification2" should {
    val sentence1 = "Mark has overcome many problems."
    val paraphrase1 = "Also Mark has overcome many problems." 

    "returns an appropriate response" in {
      val propositionId1 = java.util.UUID.randomUUID().toString
      val sentenceId1 = java.util.UUID.randomUUID().toString
      val knowledge1 = Knowledge(sentence1,"en_US", "{}", false)
      val paraphraseKnowledge1 = Knowledge(paraphrase1,"en_US", "{}", false)
      TestUtilsEx.registerSingleClaim(KnowledgeForParser(propositionId1, sentenceId1, knowledge1), transversalState)
      val propositionIdForInference1 = java.util.UUID.randomUUID().toString
      val sentenceIdForInference1 = java.util.UUID.randomUUID().toString
      val premiseKnowledge = List.empty[KnowledgeForParser]
      val claimKnowledge = List(KnowledgeForParser(propositionIdForInference1, sentenceIdForInference1, paraphraseKnowledge1))
      val inputSentence = Json.toJson(InputSentenceForParser(premiseKnowledge, claimKnowledge, ActionModeType.DEDUCTION_MODE.index)).toString()
      val json = ToposoidUtils.callComponent(inputSentence, conf.getString("TOPOSOID_SENTENCE_PARSER_EN_WEB_HOST"), conf.getString("TOPOSOID_SENTENCE_PARSER_EN_WEB_PORT"), "analyze", transversalState)
      val fr = FakeRequest(POST, "/execute")
        .withHeaders("Content-type" -> "application/json", TRANSVERSAL_STATE.str -> transversalStateJson)
        .withJsonBody(Json.parse(json))
      val result = call(controller.execute(), fr)
      status(result) mustBe OK
      contentType(result) mustBe Some("application/json")
      val jsonResult: String = contentAsJson(result).toString()

      val verifyingEdgesList: List[VerifyingEdges] = Json.parse(jsonResult).as[List[VerifyingEdges]]
      assert(verifyingEdgesList.size == 1)

      TestUtilsEx.checkMatchedBothSide(json=json, sentenceId = sentenceIdForInference1, verifyingEdgesList=verifyingEdgesList, correctSize=5)
    }
  }

  "The specification3" should {
    val sentence1 = "Also Mark has overcome many problems."
    val paraphrase1 = "Mark has overcome many problems." 

    "returns an appropriate response" in {
      val propositionId1 = java.util.UUID.randomUUID().toString
      val sentenceId1 = java.util.UUID.randomUUID().toString
      val knowledge1 = Knowledge(sentence1,"en_US", "{}", false)
      val paraphraseKnowledge1 = Knowledge(paraphrase1,"en_US", "{}", false)
      TestUtilsEx.registerSingleClaim(KnowledgeForParser(propositionId1, sentenceId1, knowledge1), transversalState)
      val propositionIdForInference1 = java.util.UUID.randomUUID().toString
      val sentenceIdForInference1 = java.util.UUID.randomUUID().toString
      val premiseKnowledge = List.empty[KnowledgeForParser]
      val claimKnowledge = List(KnowledgeForParser(propositionIdForInference1, sentenceIdForInference1, paraphraseKnowledge1))
      val inputSentence = Json.toJson(InputSentenceForParser(premiseKnowledge, claimKnowledge, ActionModeType.DEDUCTION_MODE.index)).toString()
      val json = ToposoidUtils.callComponent(inputSentence, conf.getString("TOPOSOID_SENTENCE_PARSER_EN_WEB_HOST"), conf.getString("TOPOSOID_SENTENCE_PARSER_EN_WEB_PORT"), "analyze", transversalState)
      val fr = FakeRequest(POST, "/execute")
        .withHeaders("Content-type" -> "application/json", TRANSVERSAL_STATE.str -> transversalStateJson)
        .withJsonBody(Json.parse(json))
      val result = call(controller.execute(), fr)
      status(result) mustBe OK
      contentType(result) mustBe Some("application/json")
      val jsonResult: String = contentAsJson(result).toString()

      val verifyingEdgesList: List[VerifyingEdges] = Json.parse(jsonResult).as[List[VerifyingEdges]]
      assert(verifyingEdgesList.size == 1)

      TestUtilsEx.checkMatchedBothSide(json=json, sentenceId = sentenceIdForInference1, verifyingEdgesList=verifyingEdgesList, correctSize=5)
    }
  }

  "The specification4" should {
    val sentence1 = "Also absolutely Mark has overcome many problems."
    val paraphrase1 = "Mark has overcome many problems." 

    "returns an appropriate response" in {
      val propositionId1 = java.util.UUID.randomUUID().toString
      val sentenceId1 = java.util.UUID.randomUUID().toString
      val knowledge1 = Knowledge(sentence1,"en_US", "{}", false)
      val paraphraseKnowledge1 = Knowledge(paraphrase1,"en_US", "{}", false)
      TestUtilsEx.registerSingleClaim(KnowledgeForParser(propositionId1, sentenceId1, knowledge1), transversalState)
      val propositionIdForInference1 = java.util.UUID.randomUUID().toString
      val sentenceIdForInference1 = java.util.UUID.randomUUID().toString
      val premiseKnowledge = List.empty[KnowledgeForParser]
      val claimKnowledge = List(KnowledgeForParser(propositionIdForInference1, sentenceIdForInference1, paraphraseKnowledge1))
      val inputSentence = Json.toJson(InputSentenceForParser(premiseKnowledge, claimKnowledge, ActionModeType.DEDUCTION_MODE.index)).toString()
      val json = ToposoidUtils.callComponent(inputSentence, conf.getString("TOPOSOID_SENTENCE_PARSER_EN_WEB_HOST"), conf.getString("TOPOSOID_SENTENCE_PARSER_EN_WEB_PORT"), "analyze", transversalState)
      val fr = FakeRequest(POST, "/execute")
        .withHeaders("Content-type" -> "application/json", TRANSVERSAL_STATE.str -> transversalStateJson)
        .withJsonBody(Json.parse(json))
      val result = call(controller.execute(), fr)
      status(result) mustBe OK
      contentType(result) mustBe Some("application/json")
      val jsonResult: String = contentAsJson(result).toString()

      val verifyingEdgesList: List[VerifyingEdges] = Json.parse(jsonResult).as[List[VerifyingEdges]]
      assert(verifyingEdgesList.size == 1)

      TestUtilsEx.checkMatchedBothSide(json=json, sentenceId = sentenceIdForInference1, verifyingEdgesList=verifyingEdgesList, correctSize=5)
    }
  }

  "The specification5" should {
    val sentence1 = "Mark has overcome many problems."
    val paraphrase1 = "Also absolutely Mark has overcome many problems." 

    "returns an appropriate response" in {
      val propositionId1 = java.util.UUID.randomUUID().toString
      val sentenceId1 = java.util.UUID.randomUUID().toString
      val knowledge1 = Knowledge(sentence1,"en_US", "{}", false)
      val paraphraseKnowledge1 = Knowledge(paraphrase1,"en_US", "{}", false)
      TestUtilsEx.registerSingleClaim(KnowledgeForParser(propositionId1, sentenceId1, knowledge1), transversalState)
      val propositionIdForInference1 = java.util.UUID.randomUUID().toString
      val sentenceIdForInference1 = java.util.UUID.randomUUID().toString
      val premiseKnowledge = List.empty[KnowledgeForParser]
      val claimKnowledge = List(KnowledgeForParser(propositionIdForInference1, sentenceIdForInference1, paraphraseKnowledge1))
      val inputSentence = Json.toJson(InputSentenceForParser(premiseKnowledge, claimKnowledge, ActionModeType.DEDUCTION_MODE.index)).toString()
      val json = ToposoidUtils.callComponent(inputSentence, conf.getString("TOPOSOID_SENTENCE_PARSER_EN_WEB_HOST"), conf.getString("TOPOSOID_SENTENCE_PARSER_EN_WEB_PORT"), "analyze", transversalState)
      val fr = FakeRequest(POST, "/execute")
        .withHeaders("Content-type" -> "application/json", TRANSVERSAL_STATE.str -> transversalStateJson)
        .withJsonBody(Json.parse(json))
      val result = call(controller.execute(), fr)
      status(result) mustBe OK
      contentType(result) mustBe Some("application/json")
      val jsonResult: String = contentAsJson(result).toString()

      val verifyingEdgesList: List[VerifyingEdges] = Json.parse(jsonResult).as[List[VerifyingEdges]]
      assert(verifyingEdgesList.size == 1)

      TestUtilsEx.checkMatchedBothSide(json=json, sentenceId = sentenceIdForInference1, verifyingEdgesList=verifyingEdgesList, correctSize=5)
    }
  }

  "The specification6" should {
    val sentence1 = "Mark has overcome many problems."
    val paraphrase1 = "Mark has overcome many troubles." 

    "returns an appropriate response" in {
      val propositionId1 = java.util.UUID.randomUUID().toString
      val sentenceId1 = java.util.UUID.randomUUID().toString
      val knowledge1 = Knowledge(sentence1,"en_US", "{}", false)
      val paraphraseKnowledge1 = Knowledge(paraphrase1,"en_US", "{}", false)
      TestUtilsEx.registerSingleClaim(KnowledgeForParser(propositionId1, sentenceId1, knowledge1), transversalState)
      val propositionIdForInference1 = java.util.UUID.randomUUID().toString
      val sentenceIdForInference1 = java.util.UUID.randomUUID().toString
      val premiseKnowledge = List.empty[KnowledgeForParser]
      val claimKnowledge = List(KnowledgeForParser(propositionIdForInference1, sentenceIdForInference1, paraphraseKnowledge1))
      val inputSentence = Json.toJson(InputSentenceForParser(premiseKnowledge, claimKnowledge, ActionModeType.DEDUCTION_MODE.index)).toString()
      val json = ToposoidUtils.callComponent(inputSentence, conf.getString("TOPOSOID_SENTENCE_PARSER_EN_WEB_HOST"), conf.getString("TOPOSOID_SENTENCE_PARSER_EN_WEB_PORT"), "analyze", transversalState)
      val fr = FakeRequest(POST, "/execute")
        .withHeaders("Content-type" -> "application/json", TRANSVERSAL_STATE.str -> transversalStateJson)
        .withJsonBody(Json.parse(json))
      val result = call(controller.execute(), fr)
      status(result) mustBe OK
      contentType(result) mustBe Some("application/json")
      val jsonResult: String = contentAsJson(result).toString()

      val verifyingEdgesList: List[VerifyingEdges] = Json.parse(jsonResult).as[List[VerifyingEdges]]
      assert(verifyingEdgesList.size == 1)

      TestUtilsEx.checkMatchedOneSide(json=json, sentenceId = sentenceIdForInference1, verifyingEdgesList=verifyingEdgesList, correctSize=5)
    }
  }

  "The specification7" should {
    val sentence1 = "Also Mark has overcome many problems."
    val paraphrase1 = "Mark has overcome many troubles." 

    "returns an appropriate response" in {
      val propositionId1 = java.util.UUID.randomUUID().toString
      val sentenceId1 = java.util.UUID.randomUUID().toString
      val knowledge1 = Knowledge(sentence1,"en_US", "{}", false)
      val paraphraseKnowledge1 = Knowledge(paraphrase1,"en_US", "{}", false)
      TestUtilsEx.registerSingleClaim(KnowledgeForParser(propositionId1, sentenceId1, knowledge1), transversalState)
      val propositionIdForInference1 = java.util.UUID.randomUUID().toString
      val sentenceIdForInference1 = java.util.UUID.randomUUID().toString
      val premiseKnowledge = List.empty[KnowledgeForParser]
      val claimKnowledge = List(KnowledgeForParser(propositionIdForInference1, sentenceIdForInference1, paraphraseKnowledge1))
      val inputSentence = Json.toJson(InputSentenceForParser(premiseKnowledge, claimKnowledge, ActionModeType.DEDUCTION_MODE.index)).toString()
      val json = ToposoidUtils.callComponent(inputSentence, conf.getString("TOPOSOID_SENTENCE_PARSER_EN_WEB_HOST"), conf.getString("TOPOSOID_SENTENCE_PARSER_EN_WEB_PORT"), "analyze", transversalState)
      val fr = FakeRequest(POST, "/execute")
        .withHeaders("Content-type" -> "application/json", TRANSVERSAL_STATE.str -> transversalStateJson)
        .withJsonBody(Json.parse(json))
      val result = call(controller.execute(), fr)
      status(result) mustBe OK
      contentType(result) mustBe Some("application/json")
      val jsonResult: String = contentAsJson(result).toString()

      val verifyingEdgesList: List[VerifyingEdges] = Json.parse(jsonResult).as[List[VerifyingEdges]]
      assert(verifyingEdgesList.size == 1)

      TestUtilsEx.checkMatchedOneSide(json=json, sentenceId = sentenceIdForInference1, verifyingEdgesList=verifyingEdgesList, correctSize=5)
    }
  }

  "The specification8" should {
    val sentence1 = "Mark has overcome many problems."
    val paraphrase1 = "Also Mark has overcome many troubles." 

    "returns an appropriate response" in {
      val propositionId1 = java.util.UUID.randomUUID().toString
      val sentenceId1 = java.util.UUID.randomUUID().toString
      val knowledge1 = Knowledge(sentence1,"en_US", "{}", false)
      val paraphraseKnowledge1 = Knowledge(paraphrase1,"en_US", "{}", false)
      TestUtilsEx.registerSingleClaim(KnowledgeForParser(propositionId1, sentenceId1, knowledge1), transversalState)
      val propositionIdForInference1 = java.util.UUID.randomUUID().toString
      val sentenceIdForInference1 = java.util.UUID.randomUUID().toString
      val premiseKnowledge = List.empty[KnowledgeForParser]
      val claimKnowledge = List(KnowledgeForParser(propositionIdForInference1, sentenceIdForInference1, paraphraseKnowledge1))
      val inputSentence = Json.toJson(InputSentenceForParser(premiseKnowledge, claimKnowledge, ActionModeType.DEDUCTION_MODE.index)).toString()
      val json = ToposoidUtils.callComponent(inputSentence, conf.getString("TOPOSOID_SENTENCE_PARSER_EN_WEB_HOST"), conf.getString("TOPOSOID_SENTENCE_PARSER_EN_WEB_PORT"), "analyze", transversalState)
      val fr = FakeRequest(POST, "/execute")
        .withHeaders("Content-type" -> "application/json", TRANSVERSAL_STATE.str -> transversalStateJson)
        .withJsonBody(Json.parse(json))
      val result = call(controller.execute(), fr)
      status(result) mustBe OK
      contentType(result) mustBe Some("application/json")
      val jsonResult: String = contentAsJson(result).toString()

      val verifyingEdgesList: List[VerifyingEdges] = Json.parse(jsonResult).as[List[VerifyingEdges]]
      assert(verifyingEdgesList.size == 1)

      TestUtilsEx.checkMatchedOneSide(json=json, sentenceId = sentenceIdForInference1, verifyingEdgesList=verifyingEdgesList, correctSize=5)
    }
  }

  "The specification9" should {
    val sentence1 = "Also absolutely Mark has overcome many problems."
    val paraphrase1 = "Mark has overcome many troubles." 

    "returns an appropriate response" in {
      val propositionId1 = java.util.UUID.randomUUID().toString
      val sentenceId1 = java.util.UUID.randomUUID().toString
      val knowledge1 = Knowledge(sentence1,"en_US", "{}", false)
      val paraphraseKnowledge1 = Knowledge(paraphrase1,"en_US", "{}", false)
      TestUtilsEx.registerSingleClaim(KnowledgeForParser(propositionId1, sentenceId1, knowledge1), transversalState)
      val propositionIdForInference1 = java.util.UUID.randomUUID().toString
      val sentenceIdForInference1 = java.util.UUID.randomUUID().toString
      val premiseKnowledge = List.empty[KnowledgeForParser]
      val claimKnowledge = List(KnowledgeForParser(propositionIdForInference1, sentenceIdForInference1, paraphraseKnowledge1))
      val inputSentence = Json.toJson(InputSentenceForParser(premiseKnowledge, claimKnowledge, ActionModeType.DEDUCTION_MODE.index)).toString()
      val json = ToposoidUtils.callComponent(inputSentence, conf.getString("TOPOSOID_SENTENCE_PARSER_EN_WEB_HOST"), conf.getString("TOPOSOID_SENTENCE_PARSER_EN_WEB_PORT"), "analyze", transversalState)
      val fr = FakeRequest(POST, "/execute")
        .withHeaders("Content-type" -> "application/json", TRANSVERSAL_STATE.str -> transversalStateJson)
        .withJsonBody(Json.parse(json))
      val result = call(controller.execute(), fr)
      status(result) mustBe OK
      contentType(result) mustBe Some("application/json")
      val jsonResult: String = contentAsJson(result).toString()

      val verifyingEdgesList: List[VerifyingEdges] = Json.parse(jsonResult).as[List[VerifyingEdges]]
      assert(verifyingEdgesList.size == 1)

      TestUtilsEx.checkMatchedOneSide(json=json, sentenceId = sentenceIdForInference1, verifyingEdgesList=verifyingEdgesList, correctSize=5)
    }
  }
  
  "The specification10" should {
    val sentence1 = "Mark has overcome many problems."
    val paraphrase1 = "Also absolutely Mark has overcome many troubles." 

    "returns an appropriate response" in {
      val propositionId1 = java.util.UUID.randomUUID().toString
      val sentenceId1 = java.util.UUID.randomUUID().toString
      val knowledge1 = Knowledge(sentence1,"en_US", "{}", false)
      val paraphraseKnowledge1 = Knowledge(paraphrase1,"en_US", "{}", false)
      TestUtilsEx.registerSingleClaim(KnowledgeForParser(propositionId1, sentenceId1, knowledge1), transversalState)
      val propositionIdForInference1 = java.util.UUID.randomUUID().toString
      val sentenceIdForInference1 = java.util.UUID.randomUUID().toString
      val premiseKnowledge = List.empty[KnowledgeForParser]
      val claimKnowledge = List(KnowledgeForParser(propositionIdForInference1, sentenceIdForInference1, paraphraseKnowledge1))
      val inputSentence = Json.toJson(InputSentenceForParser(premiseKnowledge, claimKnowledge, ActionModeType.DEDUCTION_MODE.index)).toString()
      val json = ToposoidUtils.callComponent(inputSentence, conf.getString("TOPOSOID_SENTENCE_PARSER_EN_WEB_HOST"), conf.getString("TOPOSOID_SENTENCE_PARSER_EN_WEB_PORT"), "analyze", transversalState)
      val fr = FakeRequest(POST, "/execute")
        .withHeaders("Content-type" -> "application/json", TRANSVERSAL_STATE.str -> transversalStateJson)
        .withJsonBody(Json.parse(json))
      val result = call(controller.execute(), fr)
      status(result) mustBe OK
      contentType(result) mustBe Some("application/json")
      val jsonResult: String = contentAsJson(result).toString()

      val verifyingEdgesList: List[VerifyingEdges] = Json.parse(jsonResult).as[List[VerifyingEdges]]
      assert(verifyingEdgesList.size == 1)

      TestUtilsEx.checkMatchedOneSide(json=json, sentenceId = sentenceIdForInference1, verifyingEdgesList=verifyingEdgesList, correctSize=5)
    }
  }

  "The specification11" should {
    val sentence1 = "Whatever happens, Mark has overcome many problems."    
    val paraphrase1 = "Whatever occurs, Mark has overcome many troubles." 

    "returns an appropriate response" in {
      val propositionId1 = java.util.UUID.randomUUID().toString
      val sentenceId1 = java.util.UUID.randomUUID().toString
      val knowledge1 = Knowledge(sentence1,"en_US", "{}", false)
      val paraphraseKnowledge1 = Knowledge(paraphrase1,"en_US", "{}", false)
      TestUtilsEx.registerSingleClaim(KnowledgeForParser(propositionId1, sentenceId1, knowledge1), transversalState)
      val propositionIdForInference1 = java.util.UUID.randomUUID().toString
      val sentenceIdForInference1 = java.util.UUID.randomUUID().toString
      val premiseKnowledge = List.empty[KnowledgeForParser]
      val claimKnowledge = List(KnowledgeForParser(propositionIdForInference1, sentenceIdForInference1, paraphraseKnowledge1))
      val inputSentence = Json.toJson(InputSentenceForParser(premiseKnowledge, claimKnowledge, ActionModeType.DEDUCTION_MODE.index)).toString()
      val json = ToposoidUtils.callComponent(inputSentence, conf.getString("TOPOSOID_SENTENCE_PARSER_EN_WEB_HOST"), conf.getString("TOPOSOID_SENTENCE_PARSER_EN_WEB_PORT"), "analyze", transversalState)
      val fr = FakeRequest(POST, "/execute")
        .withHeaders("Content-type" -> "application/json", TRANSVERSAL_STATE.str -> transversalStateJson)
        .withJsonBody(Json.parse(json))
      val result = call(controller.execute(), fr)
      status(result) mustBe OK
      contentType(result) mustBe Some("application/json")
      val jsonResult: String = contentAsJson(result).toString()

      val verifyingEdgesList: List[VerifyingEdges] = Json.parse(jsonResult).as[List[VerifyingEdges]]
      assert(verifyingEdgesList.size == 1)

      TestUtilsEx.checkMatchedOneSide(json=json, sentenceId = sentenceIdForInference1, verifyingEdgesList=verifyingEdgesList, correctSize=8)
    }
  }  

  "The specification12" should {
    val sentence1 = "Union will be strength." 
    val paraphrase1 = "Knowledge is power." 

    "returns an appropriate response" in {
      val propositionId1 = java.util.UUID.randomUUID().toString
      val sentenceId1 = java.util.UUID.randomUUID().toString
      val knowledge1 = Knowledge(sentence1,"en_US", "{}", false)
      val paraphraseKnowledge1 = Knowledge(paraphrase1,"en_US", "{}", false)
      TestUtilsEx.registerSingleClaim(KnowledgeForParser(propositionId1, sentenceId1, knowledge1), transversalState)
      val propositionIdForInference1 = java.util.UUID.randomUUID().toString
      val sentenceIdForInference1 = java.util.UUID.randomUUID().toString
      val premiseKnowledge = List.empty[KnowledgeForParser]
      val claimKnowledge = List(KnowledgeForParser(propositionIdForInference1, sentenceIdForInference1, paraphraseKnowledge1))
      val inputSentence = Json.toJson(InputSentenceForParser(premiseKnowledge, claimKnowledge, ActionModeType.DEDUCTION_MODE.index)).toString()
      val json = ToposoidUtils.callComponent(inputSentence, conf.getString("TOPOSOID_SENTENCE_PARSER_EN_WEB_HOST"), conf.getString("TOPOSOID_SENTENCE_PARSER_EN_WEB_PORT"), "analyze", transversalState)
      val fr = FakeRequest(POST, "/execute")
        .withHeaders("Content-type" -> "application/json", TRANSVERSAL_STATE.str -> transversalStateJson)
        .withJsonBody(Json.parse(json))
      val result = call(controller.execute(), fr)
      status(result) mustBe OK
      contentType(result) mustBe Some("application/json")
      val jsonResult: String = contentAsJson(result).toString()

      val verifyingEdgesList: List[VerifyingEdges] = Json.parse(jsonResult).as[List[VerifyingEdges]]
      assert(verifyingEdgesList.size == 1)

      TestUtilsEx.checkMatchedFuzzy(json=json, sentenceId = sentenceIdForInference1, verifyingEdgesList=verifyingEdgesList, correctSize=1)
    }
  }  

}