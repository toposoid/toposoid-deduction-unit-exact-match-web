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

import com.ideal.linked.toposoid.common.{CLAIM, PREMISE, ToposoidUtils}
import com.ideal.linked.toposoid.knowledgebase.model.{KnowledgeBaseEdge, KnowledgeBaseNode}
import com.ideal.linked.toposoid.protocol.model.base.{AnalyzedSentenceObject, AnalyzedSentenceObjects, DeductionResult}
import com.ideal.linked.toposoid.protocol.model.neo4j.{Neo4jRecordMap, Neo4jRecords}
import com.typesafe.scalalogging.LazyLogging
import com.ideal.linked.toposoid.deduction.common.FacadeForAccessNeo4J.getCypherQueryResult

import javax.inject._
import play.api._
import play.api.libs.json.Json
import play.api.mvc._

import scala.util.{Failure, Success, Try}

/**
 * This controller creates an `Action` to determine if the entered text matches exactly with the knowledge graph
 */
@Singleton
class HomeController @Inject()(val controllerComponents: ControllerComponents) extends BaseController  with LazyLogging{

  /**
   * This function receives a parser's result as JSON,
   * checks whether it matches logically strictly with the knowledge database, and returns the result in JSON.
   */
  def execute()  = Action(parse.json) { request =>
    try {
      val json = request.body
      val analyzedSentenceObjects: AnalyzedSentenceObjects = Json.parse(json.toString).as[AnalyzedSentenceObjects]
      val claimAnalyzedSentenceObjects = analyzedSentenceObjects.analyzedSentenceObjects.filter(_.sentenceType == 1).map(analyze(_, true))
      if(claimAnalyzedSentenceObjects.filter(_.deductionResultMap.filter(_._2.status).size > 0).size == claimAnalyzedSentenceObjects.size){
        Ok(Json.toJson(AnalyzedSentenceObjects(claimAnalyzedSentenceObjects))).as(JSON)
      }else{
        val premiseAnalyzedSentenceObjects = analyzedSentenceObjects.analyzedSentenceObjects.filter(_.sentenceType == 0).map(analyze(_, true))
        if (premiseAnalyzedSentenceObjects.filter(x => x.deductionResultMap.filter(y => y._1.equals("0") && y._2.status).size > 0).size == premiseAnalyzedSentenceObjects.size){
          val allAnalyzedSentenceObjects = analyzedSentenceObjects.analyzedSentenceObjects.map(analyze(_, false))
          if(allAnalyzedSentenceObjects.filter(x => x.deductionResultMap.filter(y => y._2.status).size > 0).size == allAnalyzedSentenceObjects.size){
            Ok(Json.toJson(AnalyzedSentenceObjects(premiseAnalyzedSentenceObjects ++ allAnalyzedSentenceObjects.filter(_.sentenceType == 1)))).as(JSON)
          }else{
            Ok(Json.toJson(AnalyzedSentenceObjects(premiseAnalyzedSentenceObjects ++ claimAnalyzedSentenceObjects))).as(JSON)
          }
        }else{
          Ok(Json.toJson(AnalyzedSentenceObjects(premiseAnalyzedSentenceObjects ++ claimAnalyzedSentenceObjects))).as(JSON)
        }
      }
    }catch {
      case e: Exception => {
        logger.error(e.toString, e)
        BadRequest(Json.obj("status" -> "Error", "message" -> e.toString()))
      }
    }
  }


  /**
   * This function analyzes whether the entered text exactly matches.　
   * @param aso
   * @return
   */
  private def analyze(aso:AnalyzedSentenceObject, claimCheck:Boolean): AnalyzedSentenceObject ={
    val (searchResults, propositionIds) = aso.edgeList.foldLeft((List.empty[List[Neo4jRecordMap]], List.empty[String])){
      (acc, x) => analyzeGraphKnowledge(x, aso.nodeMap, aso.sentenceType, acc)
    }

    val targetPropositionIds =  claimCheck match {
      case true => propositionIds.filterNot(havePremiseNode(_))
      case _ => propositionIds
    }

    if(targetPropositionIds.size < aso.edgeList.size) return aso
      //Pick up the most frequent propositionId
    val maxFreqSize = targetPropositionIds.groupBy(identity).mapValues(_.size).maxBy(_._2)._2
    val propositionIdsHavingMaxFreq:List[String] = targetPropositionIds.groupBy(identity).mapValues(_.size).filter(_._2 == maxFreqSize).map(_._1).toList
    logger.debug(propositionIdsHavingMaxFreq.toString())
    //If the number of search results with this positionId and the number of edges are equal,
    //it is assumed that they match exactly. It is no longer a partial match.
    val coveredPropositionIds =  propositionIdsHavingMaxFreq.filter(x => searchResults.filter(y =>  existALlPropositionIdEqualId(x, y)).size ==  aso.edgeList.size)
    if(coveredPropositionIds.size == 0) return aso
    val status = true
    //selectedPropositions includes trivialClaimsPropositionIds
    val deductionResult:DeductionResult = new DeductionResult(status, coveredPropositionIds, "exact-match")
    val updateDeductionResultMap = aso.deductionResultMap.updated(aso.sentenceType.toString, deductionResult)
    AnalyzedSentenceObject(aso.nodeMap, aso.edgeList, aso.sentenceType, updateDeductionResultMap)

  }

  /**
   *
   * @param propositionId
   * @return
   */
  private def havePremiseNode(propositionId:String):Boolean = {
    val query = "MATCH (m:PremiseNode)-[e:LogicEdge]-(n:ClaimNode) WHERE n.propositionId='%s' return m, e, n".format(propositionId)
    val jsonStr:String = getCypherQueryResult(query, "")
    if(jsonStr.equals("""{"records":[]}""")) false
    else true
  }

  /**
   * This function is a sub-function of analyze
   * @param nodeMap
   * @param sentenceType
   * @param accParent
   * @return
   */
  private def analyzeGraphKnowledge(edge:KnowledgeBaseEdge, nodeMap:Map[String, KnowledgeBaseNode], sentenceType:Int, accParent:(List[List[Neo4jRecordMap]], List[String])): (List[List[Neo4jRecordMap]], List[String]) = {

    val sourceKey = edge.sourceId
    val targetKey = edge.destinationId
    val sourceNodeSurface = nodeMap.get(sourceKey).getOrElse().asInstanceOf[KnowledgeBaseNode].surface
    val destinationNodeSurface = nodeMap.get(targetKey).getOrElse().asInstanceOf[KnowledgeBaseNode].surface
    val nodeType:String = ToposoidUtils.getNodeType(sentenceType)

    val initAcc = sentenceType match{
      case PREMISE.index => {
        val nodeType:String = ToposoidUtils.getNodeType(CLAIM.index)
        val query = "MATCH (n1:%s)-[e]-(n2:%s) WHERE n1.surface='%s' AND e.caseName='%s' AND n2.surface='%s' RETURN n1, e, n2".format(nodeType, nodeType, sourceNodeSurface, edge.caseStr, destinationNodeSurface)
        logger.info(query)
        val jsonStr:String = getCypherQueryResult(query, "")
        //If there is even one that does not match, it is useless to search further
        if(jsonStr.equals("""{"records":[]}""")) return (List.empty[List[Neo4jRecordMap]], List.empty[String])
        val neo4jRecords:Neo4jRecords = Json.parse(jsonStr).as[Neo4jRecords]
        neo4jRecords.records.foldLeft(accParent){
          (acc, x) => { (acc._1 :+ x, acc._2 :+ x.head.value.logicNode.propositionId)}
        }
      }
      case _ => accParent
    }

    val query = "MATCH (n1:%s)-[e]-(n2:%s) WHERE n1.surface='%s' AND e.caseName='%s' AND n2.surface='%s' RETURN n1, e, n2".format(nodeType, nodeType, sourceNodeSurface, edge.caseStr, destinationNodeSurface)
    logger.info(query)
    val jsonStr:String = getCypherQueryResult(query, "")
    //If there is even one that does not match, it is useless to search further
    if(jsonStr.equals("""{"records":[]}""")) return initAcc
    val neo4jRecords:Neo4jRecords = Json.parse(jsonStr).as[Neo4jRecords]
    neo4jRecords.records.foldLeft(initAcc){
      (acc, x) => { (acc._1 :+ x, acc._2 :+ x.head.value.logicNode.propositionId)}
    }
  }

  /**
   * This function checks if there is a result with only the specified ID
   * @param id
   * @param record
   * @return
   */
  private def existALlPropositionIdEqualId(id:String, record:List[Neo4jRecordMap]):Boolean = Try{
    if(record.size > 0){
      record.foreach { map: Neo4jRecordMap =>
        if (map.value.logicNode.propositionId.equals(id)) {
          return true
        }
      }
    }
    return false
  }match {
    case Failure(e) => throw e
  }



}
