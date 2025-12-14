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

import com.ideal.linked.toposoid.common.{CLAIM, LOCAL, PREDICATE_ARGUMENT, PREMISE, TRANSVERSAL_STATE, ToposoidUtils, TransversalState}
import com.ideal.linked.toposoid.deduction.common.DeductionUnitController
import com.ideal.linked.toposoid.deduction.common.FacadeForAccessNeo4J.getCypherQueryResult
import com.ideal.linked.toposoid.knowledgebase.model.{KnowledgeBaseEdge, KnowledgeBaseNode}
import com.ideal.linked.toposoid.protocol.model.base.{KnowledgeBaseSideInfo, _}
import com.ideal.linked.toposoid.protocol.model.neo4j.{Neo4jRecordMap, Neo4jRecords}
import com.typesafe.scalalogging.LazyLogging
import play.api.libs.json.Json
import play.api.mvc._
import play.api.libs.json.JsValue


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
  def execute():Action[JsValue] = Action(parse.json[JsValue]) { request =>
    val transversalState = Json.parse(request.headers.get(TRANSVERSAL_STATE .str).get).as[TransversalState]
    try {
      val json = request.body
      val analyzedSentenceObjects: AnalyzedSentenceObjects = Json.parse(json.toString).as[AnalyzedSentenceObjects]
      val asos:List[AnalyzedSentenceObject] = analyzedSentenceObjects.analyzedSentenceObjects
      val result:List[AnalyzedSentenceObject] = asos.foldLeft(List.empty[AnalyzedSentenceObject]){
        (acc, x) => acc :+ analyze(x, acc, "exact-match", List.empty[Int], transversalState)
      }
      logger.info(ToposoidUtils.formatMessageForLogger("deduction completed.", transversalState.userId))
      Ok(Json.toJson(AnalyzedSentenceObjects(result))).as(JSON)
    }catch {
      case e: Exception => {
        logger.error(ToposoidUtils.formatMessageForLogger(e.toString, transversalState.userId), e)
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
  def analyzeGraphKnowledge(edge: KnowledgeBaseEdge, aso:AnalyzedSentenceObject, accParent: List[(KnowledgeBaseSideInfo, CoveredPropositionEdge)], transversalState:TransversalState): List[(KnowledgeBaseSideInfo, CoveredPropositionEdge)] = {

    val nodeMap: Map[String, KnowledgeBaseNode] =  aso.nodeMap
    val sourceKey = edge.sourceId
    val targetKey = edge.destinationId
    val sourceNodeSurface = nodeMap.get(sourceKey).get.asInstanceOf[KnowledgeBaseNode].predicateArgumentStructure.surface
    val destinationNodeSurface = nodeMap.get(targetKey).get.asInstanceOf[KnowledgeBaseNode].predicateArgumentStructure.surface

    val nodeType: String = ToposoidUtils.getNodeType(CLAIM.index, LOCAL.index, PREDICATE_ARGUMENT.index)
    val query = "MATCH (n1:%s)-[e]-(n2:%s) WHERE n1.surface='%s' AND e.caseName='%s' AND n2.surface='%s' RETURN n1, e, n2".format(nodeType, nodeType, sourceNodeSurface, edge.caseStr, destinationNodeSurface)
    logger.debug(query)
    val jsonStr: String = getCypherQueryResult(query, "", transversalState)
    //If there is even one that does not match, it is useless to search further
    if (jsonStr.equals("""{"records":[]}""")) return accParent
    val neo4jRecords: Neo4jRecords = Json.parse(jsonStr).as[Neo4jRecords]


    accParent
    neo4jRecords.records.foldLeft(accParent) {
      (acc, x) => {
        val sourceNode = CoveredPropositionNode(terminalId = sourceKey, terminalSurface = sourceNodeSurface, terminalUrl = "")
        val destinationNode = CoveredPropositionNode(terminalId = targetKey, terminalSurface = destinationNodeSurface, terminalUrl = "")
        val knowledgeBaseSideInfo = KnowledgeBaseSideInfo(propositionId = x.head.value.localNode.get.propositionId, sentenceId = x.head.value.localNode.get.sentenceId, featureInfoList = List.empty[MatchedFeatureInfo])
        val coveredPropositionEdge = CoveredPropositionEdge(sourceNode = sourceNode, destinationNode = destinationNode)
        acc :+ (knowledgeBaseSideInfo, coveredPropositionEdge)
      }
    }

  }


}

