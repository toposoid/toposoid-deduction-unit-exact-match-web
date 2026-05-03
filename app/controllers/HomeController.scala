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

import com.ideal.linked.toposoid.common.{SentenceType, ScopeType,  FeatureType, TRANSVERSAL_STATE, ToposoidUtils, TransversalState, RelationMatchState}
import com.ideal.linked.toposoid.knowledgebase.model.{KnowledgeBaseEdge, KnowledgeBaseNode}
import com.ideal.linked.toposoid.protocol.model.base.{KnowledgeBaseSideInfo, _}
import com.ideal.linked.toposoid.protocol.model.neo4j.{Neo4jRecordMap, Neo4jRecords}
import com.typesafe.scalalogging.LazyLogging
import play.api.libs.json.Json
import play.api.mvc._
import play.api.libs.json.JsValue

import javax.inject._
import scala.concurrent.Future
import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.util.{Failure, Success, Try}
import com.ideal.linked.common.DeploymentConverter.conf
import com.ideal.linked.toposoid.common.Neo4JUtilsImpl
import com.ideal.linked.toposoid.common.DeductionUtils
import play.api.libs.json.{Json, OWrites, Reads}
import org.checkerframework.checker.initialization.qual.NotOnlyInitialized
import com.ideal.linked.toposoid.common.DeductionQuery

//case class DeductionQuery(query:String,relationMatchState:RelationMatchState, sourceAlias:String, destinationAlias:String,isSourceConfirmed:Boolean, isDestinationConfirmed:Boolean)

/**
 * This controller creates an `Action` to determine if the entered text matches exactly with the knowledge graph
 */
@Singleton
class HomeController @Inject()(val controllerComponents: ControllerComponents) extends BaseController with LazyLogging {

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
      val result:List[VerifyingEdges] = asos.foldLeft(List.empty[VerifyingEdges]){
        (acc, aso) => {    
          acc :+ VerifyingEdges(            
            propositionId = aso.knowledgeBaseSemiGlobalNode.propositionId,
            sentenceId = aso.knowledgeBaseSemiGlobalNode.sentenceId,
            coveredPropositionEdges = DeductionUtils.analyzeGraphKnowledge(getQeuries, aso, transversalState)
          )
        }
      }
      logger.info(ToposoidUtils.formatMessageForLogger("Basic edge analysis completed.", transversalState.userId))      
      Ok(Json.toJson(result)).as(JSON)
    }catch {
      case e: Exception => {
        logger.error(ToposoidUtils.formatMessageForLogger(e.toString, transversalState.userId), e)
        BadRequest(Json.obj("status" -> "Error", "message" -> e.toString()))
      }
    }
  }
  
  private def getQeuries(edge:KnowledgeBaseEdge, aso:AnalyzedSentenceObject, transversalState:TransversalState):List[DeductionQuery] = {
    
    //すでにsentenceIdの候補がある場合、それらのsenteneIdでフィルタリングをかける。
    val sentenceIds = aso.deductionResult.evidenceKnowledgeList.map(x => "'" + x.sentenceId + "'").distinct
    val sentenceIdFilterQuery = sentenceIds.size match {
      case 0 => ""
      case _ => "AND n1.sentenceId IN [%s]".format(sentenceIds.mkString(","))
    }
    val sourceKey = edge.sourceId
    val targetKey = edge.destinationId
    val sourceNode = aso.nodeMap.get(sourceKey).get.asInstanceOf[KnowledgeBaseNode]
    val destinationNode = aso.nodeMap.get(targetKey).get.asInstanceOf[KnowledgeBaseNode]
    val haveDeterminerSource = sourceNode.localContext.lang match  {
      case "en_US" => {
        if(sourceNode.predicateArgumentStructure.caseType.equals("dt")) true
        else false
      }
      case _ => false
    }
    val haveDeterminerDestination = destinationNode.localContext.lang match  {
      case "en_US" => {
        if(destinationNode.predicateArgumentStructure.caseType.equals("dt")) true
        else false
      }
      case _ => false
    }

    val nodeType: String = ToposoidUtils.getNodeType(SentenceType.CLAIM.index, ScopeType.LOCAL.index, FeatureType.PREDICATE_ARGUMENT.index)
    val query1 = "MATCH (n1:%s)-[e]->(n2:%s) WHERE n1.surface=\"%s\" AND e.caseName='%s' AND n2.surface=\"%s\" %s RETURN n1, e, n2".format(nodeType, nodeType, sourceNode.predicateArgumentStructure.surface, edge.caseStr, destinationNode.predicateArgumentStructure.surface, sentenceIdFilterQuery) 
    val query2 = "MATCH (n1:%s)-[e]->(n2:%s)-[e2ext]-(n2ext) WHERE n1.surface=\"%s\" AND e.caseName='%s' AND Not e2ext:LocalEdge AND n2.isDenialWord='%s' %s RETURN n1, e, n2".format(nodeType, nodeType, sourceNode.predicateArgumentStructure.surface, edge.caseStr, destinationNode.predicateArgumentStructure.isDenialWord, sentenceIdFilterQuery)
    val query3 = "MATCH (n1ext)-[e1ext]-(n1:%s)-[e]->(n2:%s) WHERE n2.surface=\"%s\" AND e.caseName='%s' AND Not e1ext:LocalEdge AND n1.isDenialWord='%s' %s RETURN n1, e, n2".format(nodeType, nodeType, destinationNode.predicateArgumentStructure.surface, edge.caseStr, sourceNode.predicateArgumentStructure.isDenialWord, sentenceIdFilterQuery)
    val query4 = "MATCH (n1ext)-[e1ext]-(n1:%s)-[e]->(n2:%s)-[e2ext]-(n2ext) WHERE e.caseName='%s' AND Not e1ext:LocalEdge AND Not e2ext:LocalEdge AND n1.isDenialWord='%s' AND n2.isDenialWord='%s' %s RETURN n1, e, n2".format(nodeType, nodeType, edge.caseStr, sourceNode.predicateArgumentStructure.isDenialWord, destinationNode.predicateArgumentStructure.isDenialWord, sentenceIdFilterQuery)

    val haveFeatureOnSource = sourceNode.localContext.knowledgeFeatureReferences.filter(x => List(FeatureType.IMAGE.index, FeatureType.TABLE.index).contains(x.featureType)).size > 0
    val haveFeatureOnDestination = destinationNode.localContext.knowledgeFeatureReferences.filter(x => List(FeatureType.IMAGE.index, FeatureType.TABLE.index).contains(x.featureType)).size > 0

    //命題のFeatureNodeのペアをどう持つかで、仮に表層テキスト単位でマッチしても判断を先送りする必要がある。RelationMatchStateを指定している意味。
    (haveFeatureOnSource, haveFeatureOnDestination) match
      case (false, false) => {
        List(
          DeductionQuery(query1, RelationMatchState.MATCHED_BOTH, "n1", "n2", false, false),
          DeductionQuery(query2, RelationMatchState.MATCHED_SOURCE_NODE_ONLY, "n1", "n2", false, false),
          DeductionQuery(query3, RelationMatchState.MATCHED_TARGET_NODE_ONLY, "n1", "n2", false, false),
          DeductionQuery(query4, RelationMatchState.NOT_MATCHED_BOTH, "n1", "n2", false, false)
        )      
      }
      case (true, true) => {
        List(
          DeductionQuery(query1, RelationMatchState.NOT_MATCHED_BOTH, "n1", "n2", false, false),
          DeductionQuery(query2, RelationMatchState.NOT_MATCHED_BOTH, "n1", "n2", false, false),
          DeductionQuery(query3, RelationMatchState.NOT_MATCHED_BOTH, "n1", "n2", false, false),
          DeductionQuery(query4, RelationMatchState.NOT_MATCHED_BOTH, "n1", "n2", false, false)
        )      
      }
      case (true, false) => {
        List(
          DeductionQuery(query1, RelationMatchState.MATCHED_TARGET_NODE_ONLY, "n1", "n2", false, false),
          DeductionQuery(query2, RelationMatchState.NOT_MATCHED_BOTH, "n1", "n2", false, false),
          DeductionQuery(query3, RelationMatchState.MATCHED_TARGET_NODE_ONLY, "n1", "n2", false, false),
          DeductionQuery(query4, RelationMatchState.NOT_MATCHED_BOTH, "n1", "n2", false, false)
        )      
      }
      case (false, true) => {
        List(
          DeductionQuery(query1, RelationMatchState.MATCHED_SOURCE_NODE_ONLY, "n1", "n2", false, false),
          DeductionQuery(query2, RelationMatchState.MATCHED_SOURCE_NODE_ONLY, "n1", "n2", false, false),
          DeductionQuery(query3, RelationMatchState.NOT_MATCHED_BOTH, "n1", "n2", false, false),
          DeductionQuery(query4, RelationMatchState.NOT_MATCHED_BOTH, "n1", "n2", false, false)
        )      
      }
  }
}
