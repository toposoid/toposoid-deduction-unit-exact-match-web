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

import com.ideal.linked.toposoid.common.{CLAIM, LOCAL, PREDICATE_ARGUMENT, PREMISE, ToposoidUtils}
import com.ideal.linked.toposoid.deduction.common.DeductionUnitController
import com.ideal.linked.toposoid.deduction.common.FacadeForAccessNeo4J.getCypherQueryResult
import com.ideal.linked.toposoid.knowledgebase.model.{KnowledgeBaseEdge, KnowledgeBaseNode}
import com.ideal.linked.toposoid.protocol.model.base._
import com.ideal.linked.toposoid.protocol.model.neo4j.{Neo4jRecordMap, Neo4jRecords}
import com.typesafe.scalalogging.LazyLogging
import play.api.libs.json.Json
import play.api.mvc._

import javax.inject._

/**
 * This controller creates an `Action` to determine if the entered text matches exactly with the knowledge graph
 */
@Singleton
class HomeController @Inject()(val controllerComponents: ControllerComponents) extends BaseController with DeductionUnitController with LazyLogging {

  /**
   * This function receives a parser's result as JSON,
   * checks whether it matches logically strictly with the knowledge database, and returns the result in JSON.
   */
  def execute()  = Action(parse.json) { request =>
    try {
      val json = request.body
      val analyzedSentenceObjects: AnalyzedSentenceObjects = Json.parse(json.toString).as[AnalyzedSentenceObjects]
      val asos:List[AnalyzedSentenceObject] = analyzedSentenceObjects.analyzedSentenceObjects
      val result:List[AnalyzedSentenceObject] = asos.foldLeft(List.empty[AnalyzedSentenceObject]){
        (acc, x) => acc :+ analyze(x, acc, "exact-match")
      }
      Ok(Json.toJson(AnalyzedSentenceObjects(result))).as(JSON)
    }catch {
      case e: Exception => {
        logger.error(e.toString, e)
        BadRequest(Json.obj("status" -> "Error", "message" -> e.toString()))
      }
    }
  }

  /**
   * This function is a sub-function of analyze
   *
   * @param nodeMap
   * @param sentenceType
   * @param accParent
   * @return
   */
  def analyzeGraphKnowledge(edge: KnowledgeBaseEdge, nodeMap: Map[String, KnowledgeBaseNode], sentenceType: Int, accParent: (List[List[Neo4jRecordMap]], List[MatchedPropositionInfo], List[CoveredPropositionEdge])): (List[List[Neo4jRecordMap]], List[MatchedPropositionInfo], List[CoveredPropositionEdge]) = {

    val sourceKey = edge.sourceId
    val targetKey = edge.destinationId
    val sourceNodeSurface = nodeMap.get(sourceKey).getOrElse().asInstanceOf[KnowledgeBaseNode].predicateArgumentStructure.surface
    val destinationNodeSurface = nodeMap.get(targetKey).getOrElse().asInstanceOf[KnowledgeBaseNode].predicateArgumentStructure.surface

    val nodeType: String = ToposoidUtils.getNodeType(CLAIM.index, LOCAL.index, PREDICATE_ARGUMENT.index)
    val query = "MATCH (n1:%s)-[e]-(n2:%s) WHERE n1.surface='%s' AND e.caseName='%s' AND n2.surface='%s' RETURN n1, e, n2".format(nodeType, nodeType, sourceNodeSurface, edge.caseStr, destinationNodeSurface)
    logger.info(query)
    val jsonStr: String = getCypherQueryResult(query, "")
    //If there is even one that does not match, it is useless to search further
    if (jsonStr.equals("""{"records":[]}""")) return accParent
    val neo4jRecords: Neo4jRecords = Json.parse(jsonStr).as[Neo4jRecords]
    neo4jRecords.records.foldLeft(accParent) {
      (acc, x) => {
        val neo4jRecordMap = acc._1 :+ x
        val matchedPropositionInfoList = acc._2 :+ MatchedPropositionInfo(x.head.value.localNode.get.propositionId, List(MatchedFeatureInfo(x.head.value.localNode.get.sentenceId, 1)))
        val sourceNode = CoveredPropositionNode(terminalId = sourceKey, terminalSurface = sourceNodeSurface, terminalUrl = "")
        val destinationNode = CoveredPropositionNode(terminalId = targetKey, terminalSurface = destinationNodeSurface, terminalUrl = "")
        val coveredPropositionEdgeList = acc._3 :+ CoveredPropositionEdge(sourceNode = sourceNode, destinationNode = destinationNode)
        (neo4jRecordMap, matchedPropositionInfoList, coveredPropositionEdgeList)
      }
    }
  }


}

