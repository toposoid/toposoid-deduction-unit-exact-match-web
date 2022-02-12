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
import com.ideal.linked.toposoid.knowledgebase.regist.model.{Knowledge, KnowledgeSentenceSet, PropositionRelation}
import com.ideal.linked.toposoid.protocol.model.base.AnalyzedSentenceObjects
import com.ideal.linked.toposoid.sentence.transformer.neo4j.Sentence2Neo4jTransformer
import org.scalatest.{BeforeAndAfter, BeforeAndAfterAll}
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Play.materializer
import play.api.http.Status.OK
import play.api.libs.json.Json
import play.api.test.Helpers.{POST, contentType, defaultAwaitTimeout, status, _}
import play.api.test.{FakeRequest, _}

class HomeControllerSpecEnglish extends PlaySpec with BeforeAndAfter with BeforeAndAfterAll with GuiceOneAppPerSuite  with Injecting {

  override def beforeAll(): Unit = {
    Neo4JAccessor.delete()
    Sentence2Neo4jTransformer.createGraphAuto(List(Knowledge("Time is money.","en_US", "{}" )))
    val knowledgeSentenceSet = KnowledgeSentenceSet(
      List(Knowledge("Time is money.","en_US", "{}")),
      List.empty[PropositionRelation],
      List(Knowledge("Grasp Fortune by the forelock.","en_US", "{}")),
      List.empty[PropositionRelation])
    Sentence2Neo4jTransformer.createGraph(knowledgeSentenceSet)
  }

  override def afterAll(): Unit = {
    Neo4JAccessor.delete()
  }

  val controller: HomeController = inject[HomeController]

  "The specification1" should {
    "returns an appropriate response" in {
      val json =
        """{
          |    "analyzedSentenceObjects": [
          |        {
          |            "nodeMap": {
          |                "14205129-e188-4da2-a6da-773335494151-0": {
          |                    "nodeId": "14205129-e188-4da2-a6da-773335494151-0",
          |                    "propositionId": "14205129-e188-4da2-a6da-773335494151",
          |                    "currentId": 0,
          |                    "parentId": 1,
          |                    "isMainSection": true,
          |                    "surface": "Time",
          |                    "normalizedName": "time",
          |                    "dependType": "-",
          |                    "caseType": "nsubj",
          |                    "namedEntity": "",
          |                    "rangeExpressions": {
          |                        "": {}
          |                    },
          |                    "categories": {},
          |                    "domains": {},
          |                    "isDenial": false,
          |                    "isConditionalConnection": false,
          |                    "normalizedNameYomi": "",
          |                    "surfaceYomi": "",
          |                    "modalityType": "-",
          |                    "logicType": "-",
          |                    "nodeType": 1,
          |                    "lang": "en_US",
          |                    "extentText": "{}"
          |                },
          |                "14205129-e188-4da2-a6da-773335494151-1": {
          |                    "nodeId": "14205129-e188-4da2-a6da-773335494151-1",
          |                    "propositionId": "14205129-e188-4da2-a6da-773335494151",
          |                    "currentId": 1,
          |                    "parentId": 1,
          |                    "isMainSection": true,
          |                    "surface": "is",
          |                    "normalizedName": "be",
          |                    "dependType": "-",
          |                    "caseType": "ROOT",
          |                    "namedEntity": "",
          |                    "rangeExpressions": {
          |                        "": {}
          |                    },
          |                    "categories": {},
          |                    "domains": {},
          |                    "isDenial": false,
          |                    "isConditionalConnection": false,
          |                    "normalizedNameYomi": "",
          |                    "surfaceYomi": "",
          |                    "modalityType": "-",
          |                    "logicType": "-",
          |                    "nodeType": 1,
          |                    "lang": "en_US",
          |                    "extentText": "{}"
          |                },
          |                "14205129-e188-4da2-a6da-773335494151-2": {
          |                    "nodeId": "14205129-e188-4da2-a6da-773335494151-2",
          |                    "propositionId": "14205129-e188-4da2-a6da-773335494151",
          |                    "currentId": 2,
          |                    "parentId": 1,
          |                    "isMainSection": true,
          |                    "surface": "money",
          |                    "normalizedName": "money",
          |                    "dependType": "-",
          |                    "caseType": "attr",
          |                    "namedEntity": "",
          |                    "rangeExpressions": {
          |                        "": {}
          |                    },
          |                    "categories": {},
          |                    "domains": {},
          |                    "isDenial": false,
          |                    "isConditionalConnection": false,
          |                    "normalizedNameYomi": "",
          |                    "surfaceYomi": "",
          |                    "modalityType": "-",
          |                    "logicType": "-",
          |                    "nodeType": 1,
          |                    "lang": "en_US",
          |                    "extentText": "{}"
          |                },
          |                "14205129-e188-4da2-a6da-773335494151-3": {
          |                    "nodeId": "14205129-e188-4da2-a6da-773335494151-3",
          |                    "propositionId": "14205129-e188-4da2-a6da-773335494151",
          |                    "currentId": 3,
          |                    "parentId": 1,
          |                    "isMainSection": true,
          |                    "surface": ".",
          |                    "normalizedName": ".",
          |                    "dependType": "-",
          |                    "caseType": "punct",
          |                    "namedEntity": "",
          |                    "rangeExpressions": {
          |                        "": {}
          |                    },
          |                    "categories": {},
          |                    "domains": {},
          |                    "isDenial": false,
          |                    "isConditionalConnection": false,
          |                    "normalizedNameYomi": "",
          |                    "surfaceYomi": "",
          |                    "modalityType": "-",
          |                    "logicType": "-",
          |                    "nodeType": 1,
          |                    "lang": "en_US",
          |                    "extentText": "{}"
          |                }
          |            },
          |            "edgeList": [
          |                {
          |                    "sourceId": "14205129-e188-4da2-a6da-773335494151-0",
          |                    "destinationId": "14205129-e188-4da2-a6da-773335494151-1",
          |                    "caseStr": "nsubj",
          |                    "dependType": "-",
          |                    "logicType": "-",
          |                    "lang": "en_US"
          |                },
          |                {
          |                    "sourceId": "14205129-e188-4da2-a6da-773335494151-2",
          |                    "destinationId": "14205129-e188-4da2-a6da-773335494151-1",
          |                    "caseStr": "attr",
          |                    "dependType": "-",
          |                    "logicType": "-",
          |                    "lang": "en_US"
          |                },
          |                {
          |                    "sourceId": "14205129-e188-4da2-a6da-773335494151-3",
          |                    "destinationId": "14205129-e188-4da2-a6da-773335494151-1",
          |                    "caseStr": "punct",
          |                    "dependType": "-",
          |                    "logicType": "-",
          |                    "lang": "en_US"
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

  "The specification2" should {
    "returns an appropriate response" in {
      val json =
        """{
          |    "analyzedSentenceObjects": [
          |        {
          |            "nodeMap": {
          |                "14205129-e188-4da2-a6da-773335494151-0": {
          |                    "nodeId": "14205129-e188-4da2-a6da-773335494151-0",
          |                    "propositionId": "14205129-e188-4da2-a6da-773335494151",
          |                    "currentId": 0,
          |                    "parentId": 1,
          |                    "isMainSection": true,
          |                    "surface": "Time",
          |                    "normalizedName": "time",
          |                    "dependType": "-",
          |                    "caseType": "nsubj",
          |                    "namedEntity": "",
          |                    "rangeExpressions": {
          |                        "": {}
          |                    },
          |                    "categories": {},
          |                    "domains": {},
          |                    "isDenial": false,
          |                    "isConditionalConnection": false,
          |                    "normalizedNameYomi": "",
          |                    "surfaceYomi": "",
          |                    "modalityType": "-",
          |                    "logicType": "-",
          |                    "nodeType": 0,
          |                    "lang": "en_US",
          |                    "extentText": "{}"
          |                },
          |                "14205129-e188-4da2-a6da-773335494151-1": {
          |                    "nodeId": "14205129-e188-4da2-a6da-773335494151-1",
          |                    "propositionId": "14205129-e188-4da2-a6da-773335494151",
          |                    "currentId": 1,
          |                    "parentId": 1,
          |                    "isMainSection": true,
          |                    "surface": "is",
          |                    "normalizedName": "be",
          |                    "dependType": "-",
          |                    "caseType": "ROOT",
          |                    "namedEntity": "",
          |                    "rangeExpressions": {
          |                        "": {}
          |                    },
          |                    "categories": {},
          |                    "domains": {},
          |                    "isDenial": false,
          |                    "isConditionalConnection": false,
          |                    "normalizedNameYomi": "",
          |                    "surfaceYomi": "",
          |                    "modalityType": "-",
          |                    "logicType": "-",
          |                    "nodeType": 0,
          |                    "lang": "en_US",
          |                    "extentText": "{}"
          |                },
          |                "14205129-e188-4da2-a6da-773335494151-2": {
          |                    "nodeId": "14205129-e188-4da2-a6da-773335494151-2",
          |                    "propositionId": "14205129-e188-4da2-a6da-773335494151",
          |                    "currentId": 2,
          |                    "parentId": 1,
          |                    "isMainSection": true,
          |                    "surface": "money",
          |                    "normalizedName": "money",
          |                    "dependType": "-",
          |                    "caseType": "attr",
          |                    "namedEntity": "",
          |                    "rangeExpressions": {
          |                        "": {}
          |                    },
          |                    "categories": {},
          |                    "domains": {},
          |                    "isDenial": false,
          |                    "isConditionalConnection": false,
          |                    "normalizedNameYomi": "",
          |                    "surfaceYomi": "",
          |                    "modalityType": "-",
          |                    "logicType": "-",
          |                    "nodeType": 0,
          |                    "lang": "en_US",
          |                    "extentText": "{}"
          |                },
          |                "14205129-e188-4da2-a6da-773335494151-3": {
          |                    "nodeId": "14205129-e188-4da2-a6da-773335494151-3",
          |                    "propositionId": "14205129-e188-4da2-a6da-773335494151",
          |                    "currentId": 3,
          |                    "parentId": 1,
          |                    "isMainSection": true,
          |                    "surface": ".",
          |                    "normalizedName": ".",
          |                    "dependType": "-",
          |                    "caseType": "punct",
          |                    "namedEntity": "",
          |                    "rangeExpressions": {
          |                        "": {}
          |                    },
          |                    "categories": {},
          |                    "domains": {},
          |                    "isDenial": false,
          |                    "isConditionalConnection": false,
          |                    "normalizedNameYomi": "",
          |                    "surfaceYomi": "",
          |                    "modalityType": "-",
          |                    "logicType": "-",
          |                    "nodeType": 0,
          |                    "lang": "en_US",
          |                    "extentText": "{}"
          |                }
          |            },
          |            "edgeList": [
          |                {
          |                    "sourceId": "14205129-e188-4da2-a6da-773335494151-0",
          |                    "destinationId": "14205129-e188-4da2-a6da-773335494151-1",
          |                    "caseStr": "nsubj",
          |                    "dependType": "-",
          |                    "logicType": "-",
          |                    "lang": "en_US"
          |                },
          |                {
          |                    "sourceId": "14205129-e188-4da2-a6da-773335494151-2",
          |                    "destinationId": "14205129-e188-4da2-a6da-773335494151-1",
          |                    "caseStr": "attr",
          |                    "dependType": "-",
          |                    "logicType": "-",
          |                    "lang": "en_US"
          |                },
          |                {
          |                    "sourceId": "14205129-e188-4da2-a6da-773335494151-3",
          |                    "destinationId": "14205129-e188-4da2-a6da-773335494151-1",
          |                    "caseStr": "punct",
          |                    "dependType": "-",
          |                    "logicType": "-",
          |                    "lang": "en_US"
          |                }
          |            ],
          |            "sentenceType": 0,
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
      assert(analyzedSentenceObjects.analyzedSentenceObjects.filter(_.deductionResultMap.get("0").get.status).size == 1)
    }
  }

  "The specification3" should {
    "returns an appropriate response" in {
      Neo4JAccessor.delete()
      val knowledgeSentenceSet = KnowledgeSentenceSet(
        List(Knowledge("Time is money.","en_US", "{}")),
        List.empty[PropositionRelation],
        List(Knowledge("Grasp Fortune by the forelock.","en_US", "{}")),
        List.empty[PropositionRelation])
      Sentence2Neo4jTransformer.createGraph(knowledgeSentenceSet)

      val json =
        """{
          |    "analyzedSentenceObjects": [
          |        {
          |            "nodeMap": {
          |                "14205129-e188-4da2-a6da-773335494151-0": {
          |                    "nodeId": "14205129-e188-4da2-a6da-773335494151-0",
          |                    "propositionId": "14205129-e188-4da2-a6da-773335494151",
          |                    "currentId": 0,
          |                    "parentId": 1,
          |                    "isMainSection": true,
          |                    "surface": "Time",
          |                    "normalizedName": "time",
          |                    "dependType": "-",
          |                    "caseType": "nsubj",
          |                    "namedEntity": "",
          |                    "rangeExpressions": {
          |                        "": {}
          |                    },
          |                    "categories": {},
          |                    "domains": {},
          |                    "isDenial": false,
          |                    "isConditionalConnection": false,
          |                    "normalizedNameYomi": "",
          |                    "surfaceYomi": "",
          |                    "modalityType": "-",
          |                    "logicType": "-",
          |                    "nodeType": 0,
          |                    "lang": "en_US",
          |                    "extentText": "{}"
          |                },
          |                "14205129-e188-4da2-a6da-773335494151-1": {
          |                    "nodeId": "14205129-e188-4da2-a6da-773335494151-1",
          |                    "propositionId": "14205129-e188-4da2-a6da-773335494151",
          |                    "currentId": 1,
          |                    "parentId": 1,
          |                    "isMainSection": true,
          |                    "surface": "is",
          |                    "normalizedName": "be",
          |                    "dependType": "-",
          |                    "caseType": "ROOT",
          |                    "namedEntity": "",
          |                    "rangeExpressions": {
          |                        "": {}
          |                    },
          |                    "categories": {},
          |                    "domains": {},
          |                    "isDenial": false,
          |                    "isConditionalConnection": false,
          |                    "normalizedNameYomi": "",
          |                    "surfaceYomi": "",
          |                    "modalityType": "-",
          |                    "logicType": "-",
          |                    "nodeType": 0,
          |                    "lang": "en_US",
          |                    "extentText": "{}"
          |                },
          |                "14205129-e188-4da2-a6da-773335494151-2": {
          |                    "nodeId": "14205129-e188-4da2-a6da-773335494151-2",
          |                    "propositionId": "14205129-e188-4da2-a6da-773335494151",
          |                    "currentId": 2,
          |                    "parentId": 1,
          |                    "isMainSection": true,
          |                    "surface": "money",
          |                    "normalizedName": "money",
          |                    "dependType": "-",
          |                    "caseType": "attr",
          |                    "namedEntity": "",
          |                    "rangeExpressions": {
          |                        "": {}
          |                    },
          |                    "categories": {},
          |                    "domains": {},
          |                    "isDenial": false,
          |                    "isConditionalConnection": false,
          |                    "normalizedNameYomi": "",
          |                    "surfaceYomi": "",
          |                    "modalityType": "-",
          |                    "logicType": "-",
          |                    "nodeType": 0,
          |                    "lang": "en_US",
          |                    "extentText": "{}"
          |                },
          |                "14205129-e188-4da2-a6da-773335494151-3": {
          |                    "nodeId": "14205129-e188-4da2-a6da-773335494151-3",
          |                    "propositionId": "14205129-e188-4da2-a6da-773335494151",
          |                    "currentId": 3,
          |                    "parentId": 1,
          |                    "isMainSection": true,
          |                    "surface": ".",
          |                    "normalizedName": ".",
          |                    "dependType": "-",
          |                    "caseType": "punct",
          |                    "namedEntity": "",
          |                    "rangeExpressions": {
          |                        "": {}
          |                    },
          |                    "categories": {},
          |                    "domains": {},
          |                    "isDenial": false,
          |                    "isConditionalConnection": false,
          |                    "normalizedNameYomi": "",
          |                    "surfaceYomi": "",
          |                    "modalityType": "-",
          |                    "logicType": "-",
          |                    "nodeType": 0,
          |                    "lang": "en_US",
          |                    "extentText": "{}"
          |                }
          |            },
          |            "edgeList": [
          |                {
          |                    "sourceId": "14205129-e188-4da2-a6da-773335494151-0",
          |                    "destinationId": "14205129-e188-4da2-a6da-773335494151-1",
          |                    "caseStr": "nsubj",
          |                    "dependType": "-",
          |                    "logicType": "-",
          |                    "lang": "en_US"
          |                },
          |                {
          |                    "sourceId": "14205129-e188-4da2-a6da-773335494151-2",
          |                    "destinationId": "14205129-e188-4da2-a6da-773335494151-1",
          |                    "caseStr": "attr",
          |                    "dependType": "-",
          |                    "logicType": "-",
          |                    "lang": "en_US"
          |                },
          |                {
          |                    "sourceId": "14205129-e188-4da2-a6da-773335494151-3",
          |                    "destinationId": "14205129-e188-4da2-a6da-773335494151-1",
          |                    "caseStr": "punct",
          |                    "dependType": "-",
          |                    "logicType": "-",
          |                    "lang": "en_US"
          |                }
          |            ],
          |            "sentenceType": 0,
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
      assert(analyzedSentenceObjects.analyzedSentenceObjects.filter(_.deductionResultMap.get("0").get.status).size == 0)
    }
  }
}
