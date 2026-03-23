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

import com.ideal.linked.toposoid.common.{SentenceType, ScopeType,  FeatureType, TRANSVERSAL_STATE, ToposoidUtils, TransversalState}
import com.ideal.linked.toposoid.knowledgebase.model.{KnowledgeBaseEdge, KnowledgeBaseNode}
import com.ideal.linked.toposoid.protocol.model.base.{KnowledgeBaseSideInfo, _}
import com.ideal.linked.toposoid.protocol.model.neo4j.{Neo4jRecordMap, Neo4jRecords}
import com.typesafe.scalalogging.LazyLogging
import play.api.libs.json.Json
import play.api.mvc._
import play.api.libs.json.JsValue

import javax.inject._

sealed abstract class RelationMatchState(val index: Int)
case object MATCHED_SOURCE_NODE_ONLY extends RelationMatchState(0)
case object MATCHED_TARGET_NODE_ONLY extends RelationMatchState(1)
case object NOT_MATCHED extends RelationMatchState(2)

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
        (acc, x) => {
          acc :+ analyze(x, acc, "exact-match", List.empty[Int], transversalState)
          //acc
        }
      }
      logger.info(ToposoidUtils.formatMessageForLogger("deduction completed.", transversalState.userId))
      
      Ok(Json.toJson(AnalyzedSentenceObjects(result, analyzedSentenceObjects.deductionConfiguration))).as(JSON)
    }catch {
      case e: Exception => {
        logger.error(ToposoidUtils.formatMessageForLogger(e.toString, transversalState.userId), e)
        BadRequest(Json.obj("status" -> "Error", "message" -> e.toString()))
      }
    }
  }

  def getCoveredPropositionEdge(edge: KnowledgeBaseEdge, sourceAlias:String, destinationAlias:String, nodeMap:Map[String, KnowledgeBaseNode], neo4jRecords: Neo4jRecords):CoveredPropositionEdge = {
    //一旦どちらかのノードが埋まっていれば推論を進めるものとする。
    val sourceNodeSurface = nodeMap.get(edge.sourceId).get.asInstanceOf[KnowledgeBaseNode].predicateArgumentStructure.surface
    val destinationNodeSurface = nodeMap.get(edge.destinationId).get.asInstanceOf[KnowledgeBaseNode].predicateArgumentStructure.surface

    val sourceKnowledgeNodes:List[KnowledgeBaseNode] = neo4jRecords.records.map(x => x.filter(y => y.key == sourceAlias).map(z => z.value.localNode.get)).flatten
    val destinationKnowledgeNodes:List[KnowledgeBaseNode] = neo4jRecords.records.map(x => x.filter(y => y.key == destinationAlias).map(z => z.value.localNode.get)).flatten

    val sourceMatchedKnowledgeNodes:List[MatchedKnowledgeNode] = sourceAlias match {
      case "" => {
        List.empty[MatchedKnowledgeNode]
      }
      case _ => {
        sourceKnowledgeNodes.map(x => {
          MatchedKnowledgeNode(
              sentenceId = x.sentenceId,
              nodeId = x.nodeId,
              caseNameOnEdge = edge.caseStr,
              isDenialWord = x.predicateArgumentStructure.isDenialWord,
              nodeType = x.predicateArgumentStructure.nodeType
            )
        })
      }
    }
    val destinationMatchedKnowledgeNodes:List[MatchedKnowledgeNode] = destinationAlias match {
      case "" => {
        List.empty[MatchedKnowledgeNode]
      }
      case _ => {
        destinationKnowledgeNodes.map(x => {
          MatchedKnowledgeNode(
              sentenceId = x.sentenceId,
              nodeId = x.nodeId,
              caseNameOnEdge = edge.caseStr,
              isDenialWord = x.predicateArgumentStructure.isDenialWord,
              nodeType = x.predicateArgumentStructure.nodeType
            )
        })
      }
    }

    
    //val knowledgeBaseSideInfoList:List[KnowledgeBaseSideInfo] = List.empty[KnowledgeBaseSideInfo]
    val knowledgeBaseSideInfoList:List[KnowledgeBaseSideInfo] = (sourceKnowledgeNodes:::destinationKnowledgeNodes).map(x => {               
      KnowledgeBaseSideInfo(propositionId=x.propositionId  , sentenceId=x.sentenceId , featureInfoList = List.empty[MatchedFeatureInfo])
    }).distinct

    val sourceNode = CoveredPropositionNode(terminalId = edge.sourceId, terminalSurface = sourceNodeSurface, terminalUrl = "", matchedKnowledgeNodes=sourceMatchedKnowledgeNodes)
    val destinationNode = CoveredPropositionNode(terminalId = edge.destinationId, terminalSurface = destinationNodeSurface, terminalUrl = "", matchedKnowledgeNodes=destinationMatchedKnowledgeNodes)
    //val knowledgeBaseSideInfo = KnowledgeBaseSideInfo(propositionId = , sentenceId = , featureInfoList = List.empty[MatchedFeatureInfo])
    CoveredPropositionEdge(sourceNode = sourceNode, destinationNode = destinationNode, knowledgeBaseSideInfoList = knowledgeBaseSideInfoList)
  }


  /**
   * This function is a sub-function of analyze
   *
   * @param nodeMap
   * @param sentenceType
   * @param accParent
   * @return
   */
  
  def analyzeGraphKnowledge(edges: List[KnowledgeBaseEdge], aso:AnalyzedSentenceObject, transversalState:TransversalState):List[CoveredPropositionEdge] = {
    
    val nodeMap: Map[String, KnowledgeBaseNode] =  aso.nodeMap    
    edges.foldLeft(List.empty[CoveredPropositionEdge]){
      (acc, edge) => {        
        val sourceKey = edge.sourceId
        val targetKey = edge.destinationId
        val sourceAlias = "n1"
        val destinationAlias = "n2"
        val sourceNodeSurface = nodeMap.get(sourceKey).get.asInstanceOf[KnowledgeBaseNode].predicateArgumentStructure.surface
        val destinationNodeSurface = nodeMap.get(targetKey).get.asInstanceOf[KnowledgeBaseNode].predicateArgumentStructure.surface

        val nodeType: String = ToposoidUtils.getNodeType(SentenceType.CLAIM.index, ScopeType.LOCAL.index, FeatureType.PREDICATE_ARGUMENT.index)
        //エッジの両側ノードで厳格に一致するものがあるかどうか
        val query = "MATCH (n1:%s)-[e]-(n2:%s) WHERE n1.surface='%s' AND e.caseName='%s' AND n2.surface='%s' RETURN n1, e, n2".format(nodeType, nodeType, sourceNodeSurface, edge.caseStr, destinationNodeSurface)
        logger.debug(query)
        val jsonStr: String = getCypherQueryResult(query, "", transversalState)
        //If there is even one that does not match, it is useless to search further
        if (!jsonStr.equals("""{"records":[]}""")) {
          //ヒットするものがある場合
          val neo4jRecords: Neo4jRecords = Json.parse(jsonStr).as[Neo4jRecords]
          acc :+ getCoveredPropositionEdge(edge, sourceAlias, destinationAlias, nodeMap,  neo4jRecords)
        } 
        else {
          /*
          //ヒットするものがない場合
          //上記でヒットしない場合、エッジの片側ノード（Source）で厳格に一致するものがあるかどうか
          val querySourceOnly = "MATCH (n1:%s)-[e]-(n2:%s) WHERE n1.normalizedName='%s' AND n1.isDenialWord='%s' AND e.caseName='%s' RETURN n1, e, n2".format(nodeType, nodeType, sourceNode.predicateArgumentStructure.normalizedName, sourceNode.predicateArgumentStructure.isDenialWord, caseName)
          logger.debug(querySourceOnly)
          val querySourceOnlyResultJson: String = getCypherQueryResult(querySourceOnly, "", transversalState)
          if (!querySourceOnlyResultJson.equals("""{"records":[]}""")) {
            //TargetをSynonymに置き換えられる可能性あり
            checkNode(sourceNode, targetNode, caseName, MATCHED_SOURCE_NODE_ONLY, sentenceType, transversalState)
          } else {
            //上記でヒットしない場合、エッジの片側ノード（Target）で厳格に一致するものがあるかどうか
            val queryTargetOnly = "MATCH (n1:%s)-[e]-(n2:%s) WHERE e.caseName='%s' AND n2.normalizedName='%s' AND n2.isDenialWord='%s' RETURN n1, e, n2".format(nodeType, nodeType, caseName, targetNode.predicateArgumentStructure.normalizedName, targetNode.predicateArgumentStructure.isDenialWord)
            logger.debug(queryTargetOnly)
            val queryTargetOnlyResultJson: String = getCypherQueryResult(queryTargetOnly, "", transversalState)
            if (!queryTargetOnlyResultJson.equals("""{"records":[]}""")) {
              //SourceをSynonymに置き換えられる可能性あり
              checkNode(sourceNode, targetNode, caseName, MATCHED_TARGET_NODE_ONLY, sentenceType, transversalState)
            } else {
              //もしTargetとSourceをSynonymに置き換えられれば、OK
              checkNode(sourceNode, targetNode, caseName, NOT_MATCHED, sentenceType, transversalState)
            }
          } 
          */
          acc
        }
      }
    }
  }
}


/*
    accParent
    neo4jRecords.records.foldLeft(accParent) {
      (acc, x) => {
        //sentenceId:String, nodeId:String, caseNameOnEdge:String, isDenialWord:Boolean, nodeType: Int
        val sourceNode = CoveredPropositionNode(terminalId = sourceKey, terminalSurface = sourceNodeSurface, terminalUrl = "")
        val destinationNode = CoveredPropositionNode(terminalId = targetKey, terminalSurface = destinationNodeSurface, terminalUrl = "")
        val knowledgeBaseSideInfo = KnowledgeBaseSideInfo(propositionId = x.head.value.localNode.get.propositionId, sentenceId = x.head.value.localNode.get.sentenceId, featureInfoList = List.empty[MatchedFeatureInfo])
        val coveredPropositionEdge = CoveredPropositionEdge(sourceNode = sourceNode, destinationNode = destinationNode)
        acc :+ (knowledgeBaseSideInfo, coveredPropositionEdge)
      
      }
    }


  }
  
  private def checkNode(sourceNode: KnowledgeBaseNode, targetNode: KnowledgeBaseNode, caseName: String, relationMatchState: RelationMatchState, sentenceType: Int, transversalState:TransversalState): List[(KnowledgeBaseSideInfo, CoveredPropositionEdge)] = {

    val nodeType: String = ToposoidUtils.getNodeType(sentenceType, LOCAL.index, PREDICATE_ARGUMENT.index)
    val query = relationMatchState match {
      case MATCHED_SOURCE_NODE_ONLY => {
        "MATCH (n1:%s)-[e]-(n2:%s)<-[se:SynonymEdge]-(sn2:SynonymNode) WHERE n1.normalizedName='%s' AND n1.isDenialWord='%s' AND e.caseName='%s' AND n2.isDenialWord='%s' AND sn2.nodeName='%s' RETURN n1, e, sn2".format(nodeType, nodeType, sourceNode.predicateArgumentStructure.normalizedName, sourceNode.predicateArgumentStructure.isDenialWord, caseName, targetNode.predicateArgumentStructure.isDenialWord, targetNode.predicateArgumentStructure.normalizedName)
      }
      case MATCHED_TARGET_NODE_ONLY => {
        "MATCH (sn1:SynonymNode)-[se:SynonymEdge]->(n1:%s)-[e]-(n2:%s) WHERE sn1.nodeName='%s' AND n1.isDenialWord='%s' AND e.caseName='%s' AND n2.normalizedName='%s' AND n2.isDenialWord='%s' RETURN sn1, e, n2".format(nodeType, nodeType, sourceNode.predicateArgumentStructure.normalizedName, sourceNode.predicateArgumentStructure.isDenialWord, caseName, targetNode.predicateArgumentStructure.normalizedName, targetNode.predicateArgumentStructure.isDenialWord)
      }
      case NOT_MATCHED => {
        "MATCH (sn1:SynonymNode)-[se1:SynonymEdge]->(n1:%s)-[e]-(n2:%s)<-[se2:SynonymEdge]-(sn2:SynonymNode) WHERE sn1.nodeName='%s' AND n1.isDenialWord='%s' AND e.caseName='%s' AND n2.isDenialWord='%s' AND sn2.nodeName='%s' RETURN sn1, e, sn2".format(nodeType, nodeType, sourceNode.predicateArgumentStructure.normalizedName, sourceNode.predicateArgumentStructure.isDenialWord, caseName, targetNode.predicateArgumentStructure.isDenialWord, targetNode.predicateArgumentStructure.normalizedName)
      }
    }
    val resultJson: String = getCypherQueryResult(query, "", transversalState)
    logger.debug(query)
    if (resultJson.equals("""{"records":[]}""")) {
      List.empty[(KnowledgeBaseSideInfo, CoveredPropositionEdge)]
    } else {
      getKnowledgeBaseSideInfo(Json.parse(resultJson).as[Neo4jRecords], sourceNode, targetNode)
    }
  }
*/

