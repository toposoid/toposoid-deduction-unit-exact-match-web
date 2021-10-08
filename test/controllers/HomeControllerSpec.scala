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

import com.ideal.linked.data.accessor.neo4j.Neo4JAccessor
import com.ideal.linked.toposoid.knowledgebase.regist.model.Knowledge
import com.ideal.linked.toposoid.protocol.model.base.{AnalyzedSentenceObject, AnalyzedSentenceObjects}
import com.ideal.linked.toposoid.sentence.transformer.neo4j.Sentence2Neo4jTransformer
import org.scalatest.{BeforeAndAfter, BeforeAndAfterAll}
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Play.materializer
import play.api.http.Status.OK
import play.api.libs.json.Json
import play.api.test.Helpers.{POST, contentAsString, contentType, defaultAwaitTimeout, status, _}
import play.api.test.{FakeRequest, _}

class HomeControllerSpec extends PlaySpec with BeforeAndAfter with BeforeAndAfterAll with GuiceOneAppPerSuite  with Injecting {

  override def beforeAll(): Unit = {
    Neo4JAccessor.delete()
    Sentence2Neo4jTransformer.createGraphAuto(List(Knowledge("案ずるより産むが易し。", "{}" )))
  }

  override def afterAll(): Unit = {
    Neo4JAccessor.delete()
  }

  val controller: HomeController = inject[HomeController]

  "The specification" should {
    "returns an appropriate response" in {
      val json =
        """{
          |    "analyzedSentenceObjects": [
          |        {
          |            "nodeMap": {
          |                "75f3c079-848e-4f88-8155-4f198b2b68e2-2": {
          |                    "nodeId": "75f3c079-848e-4f88-8155-4f198b2b68e2-2",
          |                    "propositionId": "75f3c079-848e-4f88-8155-4f198b2b68e2",
          |                    "currentId": 2,
          |                    "parentId": -1,
          |                    "isMainSection": true,
          |                    "surface": "易し。",
          |                    "normalizedName": "易い",
          |                    "dependType": "D",
          |                    "caseType": "文末",
          |                    "namedEntity": "",
          |                    "rangeExpressions": {
          |                        "": {}
          |                    },
          |                    "categories": {
          |                        "": ""
          |                    },
          |                    "domains": {
          |                        "": ""
          |                    },
          |                    "isDenial": false,
          |                    "isConditionalConnection": false,
          |                    "normalizedNameYomi": "やすい?易しい",
          |                    "surfaceYomi": "やすし。",
          |                    "modalityType": "-",
          |                    "logicType": "-",
          |                    "nodeType": 1,
          |                    "extentText": "{}"
          |                },
          |                "75f3c079-848e-4f88-8155-4f198b2b68e2-1": {
          |                    "nodeId": "75f3c079-848e-4f88-8155-4f198b2b68e2-1",
          |                    "propositionId": "75f3c079-848e-4f88-8155-4f198b2b68e2",
          |                    "currentId": 1,
          |                    "parentId": 2,
          |                    "isMainSection": false,
          |                    "surface": "産むが",
          |                    "normalizedName": "産む",
          |                    "dependType": "D",
          |                    "caseType": "連用",
          |                    "namedEntity": "",
          |                    "rangeExpressions": {
          |                        "": {}
          |                    },
          |                    "categories": {
          |                        "": ""
          |                    },
          |                    "domains": {
          |                        "産む": "家庭・暮らし"
          |                    },
          |                    "isDenial": false,
          |                    "isConditionalConnection": false,
          |                    "normalizedNameYomi": "うむ",
          |                    "surfaceYomi": "うむが",
          |                    "modalityType": "-",
          |                    "logicType": "-",
          |                    "nodeType": 1,
          |                    "extentText": "{}"
          |                },
          |                "75f3c079-848e-4f88-8155-4f198b2b68e2-0": {
          |                    "nodeId": "75f3c079-848e-4f88-8155-4f198b2b68e2-0",
          |                    "propositionId": "75f3c079-848e-4f88-8155-4f198b2b68e2",
          |                    "currentId": 0,
          |                    "parentId": 1,
          |                    "isMainSection": false,
          |                    "surface": "案ずるより",
          |                    "normalizedName": "案ずる",
          |                    "dependType": "D",
          |                    "caseType": "連用",
          |                    "namedEntity": "",
          |                    "rangeExpressions": {
          |                        "": {}
          |                    },
          |                    "categories": {
          |                        "": ""
          |                    },
          |                    "domains": {
          |                        "": ""
          |                    },
          |                    "isDenial": false,
          |                    "isConditionalConnection": false,
          |                    "normalizedNameYomi": "あんずる",
          |                    "surfaceYomi": "あんずるより",
          |                    "modalityType": "-",
          |                    "logicType": "-",
          |                    "nodeType": 1,
          |                    "extentText": "{}"
          |                }
          |            },
          |            "edgeList": [
          |                {
          |                    "sourceId": "75f3c079-848e-4f88-8155-4f198b2b68e2-1",
          |                    "destinationId": "75f3c079-848e-4f88-8155-4f198b2b68e2-2",
          |                    "caseStr": "連用",
          |                    "dependType": "D",
          |                    "logicType": "-"
          |                },
          |                {
          |                    "sourceId": "75f3c079-848e-4f88-8155-4f198b2b68e2-0",
          |                    "destinationId": "75f3c079-848e-4f88-8155-4f198b2b68e2-1",
          |                    "caseStr": "連用",
          |                    "dependType": "D",
          |                    "logicType": "-"
          |                }
          |            ],
          |            "sentenceType": 1,
          |            "deductionResultMap": {
          |                "0": {
          |                    "status": false,
          |                    "matchedPropositionIds": [],
          |                    "deductionUnit": ""
          |                },
          |                "1": {
          |                    "status": false,
          |                    "matchedPropositionIds": [],
          |                    "deductionUnit": ""
          |                }
          |            }
          |        }
          |    ]
          |}""".stripMargin

      val fr = FakeRequest(POST, "/execute")
        .withHeaders("Content-type" -> "application/json")
        .withJsonBody(Json.parse(json))

      val result = call(controller.execute(), fr)
      status(result) mustBe OK
      contentType(result) mustBe Some("application/json")

      val jsonResult: String = contentAsJson(result).toString()
      val analyzedSentenceObjects: AnalyzedSentenceObjects = Json.parse(jsonResult).as[AnalyzedSentenceObjects]
      assert(analyzedSentenceObjects.analyzedSentenceObjects.filter(_.deductionResultMap.get("1").get.status).size == 1)
    }
  }
}
