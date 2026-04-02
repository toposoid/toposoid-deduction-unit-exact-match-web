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

  private def getCoveredPropositionEdge(edge: KnowledgeBaseEdge, sourceAlias:String, destinationAlias:String, nodeMap:Map[String, KnowledgeBaseNode], neo4jRecords: Neo4jRecords, relationMatchState:RelationMatchState):CoveredPropositionEdge = {
    //一旦どちらかのノードが埋まっていれば推論を進めるものとする。
    val sourceNodeSurface = nodeMap.get(edge.sourceId).get.asInstanceOf[KnowledgeBaseNode].predicateArgumentStructure.surface
    val destinationNodeSurface = nodeMap.get(edge.destinationId).get.asInstanceOf[KnowledgeBaseNode].predicateArgumentStructure.surface

    val sourceKnowledgeNodes:List[KnowledgeBaseNode] = neo4jRecords.records.map(x => x.filter(y => y.key == sourceAlias).map(z => z.value.localNode.get)).flatten
    val destinationKnowledgeNodes:List[KnowledgeBaseNode] = neo4jRecords.records.map(x => x.filter(y => y.key == destinationAlias).map(z => z.value.localNode.get)).flatten

    val (isConfirmedSource, isConfirmedDestination)= relationMatchState match {
      case RelationMatchState.MATCHED_BOTH => (true, true)
      case RelationMatchState.MATCHED_SOURCE_NODE_ONLY => (true, false)
      case RelationMatchState.MATCHED_TARGET_NODE_ONLY => (false, true)
      case RelationMatchState.NOT_MATCHED_BOTH => (false, false)
    } 

    val sourceMatchedKnowledgeNodes:List[MatchedKnowledgeNode] = sourceAlias match {
      case "" => {
        List.empty[MatchedKnowledgeNode]
      }
      case _ => {
        sourceKnowledgeNodes.map(x => {
          MatchedKnowledgeNode(
              propositionId = x.propositionId,
              sentenceId = x.sentenceId,
              nodeId = x.nodeId,
              caseNameOnEdge = edge.caseStr,
              isDenialWord = x.predicateArgumentStructure.isDenialWord,
              nodeType = x.predicateArgumentStructure.nodeType,
              featureInfoList = List.empty[MatchedFeatureInfo]
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
              propositionId = x.propositionId,
              sentenceId = x.sentenceId,
              nodeId = x.nodeId,
              caseNameOnEdge = edge.caseStr,
              isDenialWord = x.predicateArgumentStructure.isDenialWord,
              nodeType = x.predicateArgumentStructure.nodeType,
              List.empty[MatchedFeatureInfo]

            )
        })
      }
    }

    
    //val knowledgeBaseSideInfoList:List[KnowledgeBaseSideInfo] = List.empty[KnowledgeBaseSideInfo]
    val knowledgeBaseSideInfoList:List[KnowledgeBaseSideInfo] = (sourceKnowledgeNodes:::destinationKnowledgeNodes).map(x => {   
      //TODO:すでにある deductionUnitsを追加しないといけない。           
      KnowledgeBaseSideInfo(propositionId=x.propositionId, sentenceId=x.sentenceId , featureInfoList = List.empty[MatchedFeatureInfo], deductionUnits = List("exact-match"))
    }).distinct

    //isConfirmed:Boolean, deductionUnit:String
    val sourceNode = CoveredPropositionNode(terminalId = edge.sourceId, terminalSurface = sourceNodeSurface, terminalUrl = "", matchedKnowledgeNodes=sourceMatchedKnowledgeNodes, isConfirmedSource, "exact-match")
    val destinationNode = CoveredPropositionNode(terminalId = edge.destinationId, terminalSurface = destinationNodeSurface, terminalUrl = "", matchedKnowledgeNodes=destinationMatchedKnowledgeNodes, isConfirmedDestination, "exact-match")
    //val knowledgeBaseSideInfo = KnowledgeBaseSideInfo(propositionId = , sentenceId = , featureInfoList = List.empty[MatchedFeatureInfo])
    CoveredPropositionEdge(sourceNode = sourceNode, destinationNode = destinationNode)
  }


  /**
   * This function is a sub-function of analyze
   *
   * @param nodeMap
   * @param sentenceType
   * @param accParent
   * @return
   */
  
    private def analyzeEdge(edge:KnowledgeBaseEdge, nodeMap: Map[String, KnowledgeBaseNode], transversalState:TransversalState):Option[CoveredPropositionEdge] = {
      val sourceKey = edge.sourceId
      val targetKey = edge.destinationId
      val sourceAlias = "n1"
      val destinationAlias = "n2"
      val sourceNode = nodeMap.get(sourceKey).get.asInstanceOf[KnowledgeBaseNode]
      val destinationNode = nodeMap.get(targetKey).get.asInstanceOf[KnowledgeBaseNode]

      val nodeType: String = ToposoidUtils.getNodeType(SentenceType.CLAIM.index, ScopeType.LOCAL.index, FeatureType.PREDICATE_ARGUMENT.index)
      //エッジの両側ノードで厳格に一致するものがあるかどうか
      val query = "MATCH (n1:%s)-[e]-(n2:%s) WHERE n1.surface='%s' AND e.caseName='%s' AND n2.surface='%s' RETURN n1, e, n2".format(nodeType, nodeType, sourceNode.predicateArgumentStructure.surface, edge.caseStr, destinationNode.predicateArgumentStructure.surface)      
      logger.debug(query)
      val jsonStr: String = getCypherQueryResult(query, "", transversalState)
      //If there is even one that does not match, it is useless to search further
      if (!jsonStr.equals("""{"records":[]}""")) {
        //ヒットするものがある場合
        val neo4jRecords: Neo4jRecords = Json.parse(jsonStr).as[Neo4jRecords]
        Option(getCoveredPropositionEdge(edge, sourceAlias, destinationAlias, nodeMap,  neo4jRecords, RelationMatchState.MATCHED_BOTH))
      }else{
        //ヒットするものがない場合
        //上記でヒットしない場合、エッジの片側ノード（Source）で厳格に一致するものがあるかどうか
      
        //val querySourceOnly = "MATCH (n1:%s)-[e]-(n2:%s) WHERE n1.normalizedName='%s' AND n1.isDenialWord='%s' AND e.caseName='%s' RETURN n1, e, n2".format(nodeType, nodeType, sourceNode.predicateArgumentStructure.normalizedName, sourceNode.predicateArgumentStructure.isDenialWord, caseName)
        val querySourceOnly = "MATCH (n1:%s)-[e]-(n2:%s)-[e2ext]-(n2ext) WHERE n1.surface='%s' AND e.caseName='%s' AND Not e2ext:LocalEdge AND n2.isDenialWord='%s' RETURN n1, e, n2".format(nodeType, nodeType, sourceNode.predicateArgumentStructure.surface, edge.caseStr, destinationNode.predicateArgumentStructure.isDenialWord)

        logger.debug(querySourceOnly)
        val querySourceOnlyResultJson: String = getCypherQueryResult(querySourceOnly, "", transversalState)
        if (!querySourceOnlyResultJson.equals("""{"records":[]}""")) {
          //Destinationを別ノードで置き換えられる可能性あり
          val neo4jRecords: Neo4jRecords = Json.parse(jsonStr).as[Neo4jRecords]
          Option(getCoveredPropositionEdge(edge, sourceAlias, destinationAlias, nodeMap,  neo4jRecords, RelationMatchState.MATCHED_SOURCE_NODE_ONLY))         
        } else {            
          //上記でヒットしない場合、エッジの片側ノード（Target）で厳格に一致するものがあるかどうか
          val queryTargetOnly = "MATCH (n1ext)-[e1ext]-(n1:%s)-[e]-(n2:%s) WHERE n2.surface='%s' AND e.caseName='%s' AND Not e1ext:LocalEdge AND n1.isDenialWord='%s' RETURN n1, e, n2".format(nodeType, nodeType, destinationNode.predicateArgumentStructure.normalizedName, edge.caseStr, sourceNode.predicateArgumentStructure.isDenialWord)
          logger.debug(queryTargetOnly)
          val queryTargetOnlyResultJson: String = getCypherQueryResult(queryTargetOnly, "", transversalState)
          if (!queryTargetOnlyResultJson.equals("""{"records":[]}""")) {
            //Sourceを別ノードで置き換えられる可能性あり
            val neo4jRecords: Neo4jRecords = Json.parse(jsonStr).as[Neo4jRecords]
            Option(getCoveredPropositionEdge(edge, sourceAlias, destinationAlias, nodeMap,  neo4jRecords, RelationMatchState.MATCHED_TARGET_NODE_ONLY))                       
          } else {
            //もしTargetとSourceを別ノードで置き換えられれば、OK
            val queryTargetOnly = "MATCH (n1ext)-[e1ext]-(n1:%s)-[e]-(n2:%s)-[e2ext]-(n2ext) WHERE e.caseName='%s' AND Not e1ext:LocalEdge AND Not e2ext:LocalEdge AND n1.isDenialWord='%s' AND n2.isDenialWord='%s' RETURN n1, e, n2".format(nodeType, nodeType, edge.caseStr, sourceNode.predicateArgumentStructure.isDenialWord, destinationNode.predicateArgumentStructure.isDenialWord)
            logger.debug(queryTargetOnly)
            val queryTargetOnlyResultJson: String = getCypherQueryResult(queryTargetOnly, "", transversalState)
            if (!queryTargetOnlyResultJson.equals("""{"records":[]}""")) {
              val neo4jRecords: Neo4jRecords = Json.parse(jsonStr).as[Neo4jRecords]
              Option(getCoveredPropositionEdge(edge, sourceAlias, destinationAlias, nodeMap,  neo4jRecords, RelationMatchState.NOT_MATCHED_BOTH))                          
            }else{
              //推論不能
              //TODO:どうやって呼び出し側で検知するか？　→ 渡したエッジを全て被覆できていなければそれで終了。
              None
            }            
          }            
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
  
  def analyzeGraphKnowledge(edges: List[KnowledgeBaseEdge], aso:AnalyzedSentenceObject, transversalState:TransversalState):List[CoveredPropositionEdge] = {
    
    val nodeMap: Map[String, KnowledgeBaseNode] =  aso.nodeMap    

    val futures: List[Future[Option[CoveredPropositionEdge]]] = edges.foldLeft(List.empty[Future[Option[CoveredPropositionEdge]]]){
      (acc, edge) => {
        acc :+ Future(analyzeEdge(edge:KnowledgeBaseEdge, nodeMap: Map[String, KnowledgeBaseNode], transversalState))
      }
    }
    
    val combinedFuture: Future[List[Option[CoveredPropositionEdge]]] = Future.sequence(futures)
    val result = Await.result(combinedFuture, Duration.Inf)
    result.flatten
    
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
