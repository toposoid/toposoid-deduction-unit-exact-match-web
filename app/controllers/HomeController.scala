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

  private def getQueryAndRelationMatchState(queryTemplate:String, sourceFilterPhrase:String, destinationFilterPhrase:String, expectedRelationMatchState:RelationMatchState, sourceNode:KnowledgeBaseNode, destinationNode:KnowledgeBaseNode):(String, RelationMatchState) = {
    val haveFeatureOnSource = sourceNode.localContext.knowledgeFeatureReferences.filter(x => List(FeatureType.IMAGE.index, FeatureType.TABLE.index).contains(x.featureType)).size > 0
    val haveFeatureOnDestination = destinationNode.localContext.knowledgeFeatureReferences.filter(x => List(FeatureType.IMAGE.index, FeatureType.TABLE.index).contains(x.featureType)).size > 0    
    //命題のFeatureNodeのペアをどう持つかで、仮に表層テキスト単位でマッチしても判断を先送りする必要がある。RelationMatchStateを指定している意味。
    val (haveDeterminerSource, haveDeterminerDestination) = DeductionUtils.getPassThroughNodeStatePair(sourceNode, destinationNode)
    (haveDeterminerSource, haveDeterminerDestination) match {
      case (true, true) => (queryTemplate.replace("__##SOURCE_FILTER_PHRASE##__", "").replace("__##DESTINATION_FILTER_PHRASE##__", ""), RelationMatchState.MATCHED_BOTH) //このケースはないと思うが念の為。 
      case (true, false) => haveFeatureOnDestination match {
        case true => (queryTemplate.replace("__##SOURCE_FILTER_PHRASE##__", "").replace("__##DESTINATION_FILTER_PHRASE##__", destinationFilterPhrase), RelationMatchState.MATCHED_SOURCE_NODE_ONLY)
        case _ => (queryTemplate.replace("__##SOURCE_FILTER_PHRASE##__", "").replace("__##DESTINATION_FILTER_PHRASE##__", destinationFilterPhrase), RelationMatchState.MATCHED_BOTH)
      }
      case (false, true) => haveFeatureOnSource match {
        case true => (queryTemplate.replace("__##SOURCE_FILTER_PHRASE##__", sourceFilterPhrase).replace("__##DESTINATION_FILTER_PHRASE##__", ""), RelationMatchState.MATCHED_TARGET_NODE_ONLY)
        case _ => (queryTemplate.replace("__##SOURCE_FILTER_PHRASE##__", sourceFilterPhrase).replace("__##DESTINATION_FILTER_PHRASE##__", ""), RelationMatchState.MATCHED_BOTH)
      }
      case (false, false) =>  {
        val query = queryTemplate.replace("__##SOURCE_FILTER_PHRASE##__", sourceFilterPhrase).replace("__##DESTINATION_FILTER_PHRASE##__", destinationFilterPhrase)
        //クエリでレコードが取れる状態は、haveFeatureOnSource, haveFeatureOnDestinationと完全に連動していない、、、　連動するとSynonymマッチの可能性を捨てることになる。
        (haveFeatureOnSource, haveFeatureOnDestination) match {
          case (false, false) => {   
            (query, expectedRelationMatchState)
          }     
          case (true, true) => {
            (query, RelationMatchState.NOT_MATCHED_BOTH)
          }
          case (true, false) => {
            (query, RelationMatchState.MATCHED_TARGET_NODE_ONLY)            
          }
          case (false, true) => {
            (query, RelationMatchState.MATCHED_SOURCE_NODE_ONLY)
          }
        }
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
    val sourcePas = sourceNode.predicateArgumentStructure
    val destinationPas = destinationNode.predicateArgumentStructure

    val nodeType: String = ToposoidUtils.getNodeType(SentenceType.CLAIM.index, ScopeType.LOCAL.index, FeatureType.PREDICATE_ARGUMENT.index)
    val (query1, relationMatchState1) = getQueryAndRelationMatchState(
      "MATCH (n1:%s)-[e]->(n2:%s) WHERE __##SOURCE_FILTER_PHRASE##__ AND e.caseName='%s' AND __##DESTINATION_FILTER_PHRASE##__ %s RETURN n1, e, n2".format(nodeType, nodeType, edge.caseStr, sentenceIdFilterQuery), 
      "n1.normalizedName=\"%s\" AND n1.isDenialWord='%s' AND n1.modalityType='%s'".format(sourcePas.normalizedName, sourcePas.isDenialWord, sourcePas.modalityType), 
      "n2.normalizedName=\"%s\" AND n2.isDenialWord='%s' AND n2.modalityType='%s'".format(destinationPas.normalizedName, destinationPas.isDenialWord, destinationPas.modalityType), 
      RelationMatchState.MATCHED_BOTH, sourceNode, destinationNode) 
      
    val (query2, relationMatchState2) = getQueryAndRelationMatchState(
      "MATCH (n1:%s)-[e]->(n2:%s)-[e2ext]-(n2ext) WHERE __##SOURCE_FILTER_PHRASE##__ AND e.caseName='%s' AND __##DESTINATION_FILTER_PHRASE##__ %s RETURN n1, e, n2".format(nodeType, nodeType, edge.caseStr, sentenceIdFilterQuery), 
      "n1.normalizedName=\"%s\" AND n1.isDenialWord='%s' AND n1.modalityType='%s'".format(sourcePas.normalizedName, sourcePas.isDenialWord, sourcePas.modalityType), 
      "Not e2ext:LocalEdge AND n2.isDenialWord='%s' AND n2.modalityType='%s'".format(destinationPas.isDenialWord, destinationPas.modalityType), 
      RelationMatchState.MATCHED_SOURCE_NODE_ONLY, sourceNode, destinationNode) 

    val (query3, relationMatchState3) = getQueryAndRelationMatchState(
      "MATCH (n1ext)-[e1ext]-(n1:%s)-[e]->(n2:%s) WHERE __##SOURCE_FILTER_PHRASE##__ AND e.caseName='%s' AND __##DESTINATION_FILTER_PHRASE##__ %s RETURN n1, e, n2".format(nodeType, nodeType, edge.caseStr, sentenceIdFilterQuery), 
      "Not e1ext:LocalEdge AND n1.isDenialWord='%s' AND n1.modalityType='%s'".format(sourcePas.isDenialWord, sourcePas.modalityType), 
      "n2.normalizedName=\"%s\" AND n2.isDenialWord='%s' AND n2.modalityType='%s'".format(destinationPas.normalizedName, destinationPas.isDenialWord, destinationPas.modalityType), 
      RelationMatchState.MATCHED_TARGET_NODE_ONLY, sourceNode, destinationNode) 

    val (query4, relationMatchState4) = getQueryAndRelationMatchState(
      "MATCH (n1ext)-[e1ext]-(n1:%s)-[e]->(n2:%s)-[e2ext]-(n2ext) WHERE __##SOURCE_FILTER_PHRASE##__ AND e.caseName='%s' AND __##DESTINATION_FILTER_PHRASE##__ %s RETURN n1, e, n2".format(nodeType, nodeType, edge.caseStr, sentenceIdFilterQuery), 
      "Not e1ext:LocalEdge AND n1.isDenialWord='%s' AND n1.modalityType='%s'".format(sourcePas.isDenialWord, sourcePas.modalityType), 
      "Not e2ext:LocalEdge AND n2.isDenialWord='%s' AND n2.modalityType='%s'".format(destinationPas.isDenialWord, destinationPas.modalityType), 
      RelationMatchState.NOT_MATCHED_BOTH, sourceNode, destinationNode) 

    List(
      DeductionQuery(query1, relationMatchState1, "n1", "n2", false, false),
      DeductionQuery(query2, relationMatchState2, "n1", "n2", false, false),
      DeductionQuery(query3, relationMatchState3, "n1", "n2", false, false),
      DeductionQuery(query4, relationMatchState4, "n1", "n2", false, false)
    )

    /*
      (haveDeterminerSource, haveDeterminerDestination) match {
      case (true, true) => "MATCH (n1:%s)-[e]->(n2:%s) %s WHERE n1.caseType='dt' AND n2.caseType='dt' RETURN n1, e, n2".format(nodeType, nodeType, sentenceIdFilterQuery) 
      case (true, false) => "MATCH (n1:%s)-[e]->(n2:%s) WHERE n1.caseType='dt' AND e.caseName='%s' AND n2.normalizedName=\"%s\" AND n2.isDenialWord='%s' AND n2.modalityType='%s' %s RETURN n1, e, n2".format(nodeType, nodeType, edge.caseStr, destinationNode.predicateArgumentStructure.normalizedName, destinationNode.predicateArgumentStructure.isDenialWord, destinationNode.predicateArgumentStructure.modalityType, sentenceIdFilterQuery) 
      case (false, true) => "MATCH (n1:%s)-[e]->(n2:%s) WHERE n1.normalizedName=\"%s\" AND n1.isDenialWord='%s' AND n1.modalityType='%s' AND e.caseName='%s' AND n2.caseType='dt' %s RETURN n1, e, n2".format(nodeType, nodeType, sourceNode.predicateArgumentStructure.normalizedName, sourceNode.predicateArgumentStructure.isDenialWord, sourceNode.predicateArgumentStructure.modalityType, edge.caseStr, sentenceIdFilterQuery) 
      case (false, false) => "MATCH (n1:%s)-[e]->(n2:%s) WHERE n1.normalizedName=\"%s\" AND n1.isDenialWord='%s' AND n1.modalityType='%s' AND e.caseName='%s' AND n2.normalizedName=\"%s\" AND n2.isDenialWord='%s' AND n2.modalityType='%s' %s RETURN n1, e, n2".format(nodeType, nodeType, sourceNode.predicateArgumentStructure.normalizedName, sourceNode.predicateArgumentStructure.isDenialWord, sourceNode.predicateArgumentStructure.modalityType, edge.caseStr, destinationNode.predicateArgumentStructure.normalizedName, destinationNode.predicateArgumentStructure.isDenialWord, destinationNode.predicateArgumentStructure.modalityType, sentenceIdFilterQuery) 
    }
                  
    val query2 = (haveDeterminerSource, haveDeterminerDestination) match {
      case (true, true) => "MATCH (n1:%s)-[e]->(n2:%s) %s WHERE n1.caseType='dt' AND n2.caseType='dt' RETURN n1, e, n2".format(nodeType, nodeType, sentenceIdFilterQuery) //このケースはないと思うが念の為。 
      case (true, false) => "" 
      case (false, true) => "" 
      case (false, false) => "MATCH (n1:%s)-[e]->(n2:%s)-[e2ext]-(n2ext) WHERE n1.normalizedName=\"%s\" AND n1.isDenialWord='%s' AND n1.modalityType='%s' AND e.caseName='%s' AND Not e2ext:LocalEdge AND n2.isDenialWord='%s' AND n2.modalityType='%s' %s RETURN n1, e, n2".format(nodeType, nodeType, sourceNode.predicateArgumentStructure.normalizedName, sourceNode.predicateArgumentStructure.isDenialWord, sourceNode.predicateArgumentStructure.modalityType, edge.caseStr, destinationNode.predicateArgumentStructure.isDenialWord, destinationNode.predicateArgumentStructure.modalityType, sentenceIdFilterQuery)
      
    }
      
      "MATCH (n1:%s)-[e]->(n2:%s)-[e2ext]-(n2ext) WHERE n1.normalizedName=\"%s\" AND n1.isDenialWord='%s' AND n1.modalityType='%s' AND e.caseName='%s' AND Not e2ext:LocalEdge AND n2.isDenialWord='%s' AND n2.modalityType='%s' %s RETURN n1, e, n2".format(nodeType, nodeType, sourceNode.predicateArgumentStructure.normalizedName, sourceNode.predicateArgumentStructure.isDenialWord, sourceNode.predicateArgumentStructure.modalityType, edge.caseStr, destinationNode.predicateArgumentStructure.isDenialWord, destinationNode.predicateArgumentStructure.modalityType, sentenceIdFilterQuery)
    val query3 = "MATCH (n1ext)-[e1ext]-(n1:%s)-[e]->(n2:%s) WHERE n2.normalizedName=\"%s\" AND n2.isDenialWord='%s' AND n2.modalityType='%s' AND e.caseName='%s' AND Not e1ext:LocalEdge AND n1.isDenialWord='%s' AND n1.modalityType='%s' %s RETURN n1, e, n2".format(nodeType, nodeType, destinationNode.predicateArgumentStructure.normalizedName, destinationNode.predicateArgumentStructure.isDenialWord, destinationNode.predicateArgumentStructure.modalityType, edge.caseStr, sourceNode.predicateArgumentStructure.isDenialWord, sourceNode.predicateArgumentStructure.modalityType, sentenceIdFilterQuery)
    val query4 = "MATCH (n1ext)-[e1ext]-(n1:%s)-[e]->(n2:%s)-[e2ext]-(n2ext) WHERE n1.modalityType='%s' AND n2.modalityType='%s' AND e.caseName='%s' AND Not e1ext:LocalEdge AND Not e2ext:LocalEdge AND n1.isDenialWord='%s' AND n2.isDenialWord='%s' %s RETURN n1, e, n2".format(nodeType, nodeType, sourceNode.predicateArgumentStructure.modalityType, destinationNode.predicateArgumentStructure.modalityType, edge.caseStr, sourceNode.predicateArgumentStructure.isDenialWord, destinationNode.predicateArgumentStructure.isDenialWord, sentenceIdFilterQuery)


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
      */
  }
}
