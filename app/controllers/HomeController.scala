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
import com.ideal.linked.toposoid.deduction.common.{AnalyzedSentenceObjectUtils, FacadeForAccessNeo4J}
import com.ideal.linked.toposoid.knowledgebase.model.{KnowledgeBaseEdge, KnowledgeBaseNode}
import com.ideal.linked.toposoid.protocol.model.base.{AnalyzedSentenceObject, AnalyzedSentenceObjects, DeductionResult, MatchedFeatureInfo, MatchedPropositionInfo}
import com.ideal.linked.toposoid.protocol.model.neo4j.{Neo4jRecordMap, Neo4jRecords}
import com.typesafe.scalalogging.LazyLogging
import com.ideal.linked.toposoid.deduction.common.FacadeForAccessNeo4J.{getCypherQueryResult, havePremiseNode}

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
      val asos:List[AnalyzedSentenceObject] = analyzedSentenceObjects.analyzedSentenceObjects
      val result:List[AnalyzedSentenceObject] = asos.foldLeft(List.empty[AnalyzedSentenceObject]){
        (acc, x) => acc :+ analyze(x, acc)
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
   * This function analyzes whether the entered text exactly matches.
   * @param aso
   * @param asos
   * @return
   */
  private def analyze(aso: AnalyzedSentenceObject, asos: List[AnalyzedSentenceObject]): AnalyzedSentenceObject = {

    val (searchResults, propositionIdInfoList) = aso.edgeList.foldLeft((List.empty[List[Neo4jRecordMap]], List.empty[MatchedPropositionInfo])) {
      (acc, x) => analyzeGraphKnowledge(x, aso.nodeMap, aso.knowledgeFeatureNode.sentenceType, acc)
    }
    if (propositionIdInfoList.size == 0) return aso
    val result = checkFinal(propositionIdInfoList, aso, searchResults)

    //This process requires that the Premise has already finished in calculating the DeductionResult
    if (aso.knowledgeFeatureNode.sentenceType == CLAIM.index) {
      val premiseDeductionResults: List[DeductionResult] = asos.map(x => x.deductionResultMap.get(PREMISE.index.toString).get)
      //If there is no deduction result that makes premise true, return the process.
      if(premiseDeductionResults.filter(_.status).size == 0) return result
      asos.filter(x => x.knowledgeFeatureNode.sentenceType == PREMISE.index).size match {
        case 0 => result
        case _ => {
          //val premiseDeductionResults: List[DeductionResult] = asos.map(x => x.deductionResultMap.get(PREMISE.index.toString).get)
          val matchedPropositionInfoList: List[MatchedPropositionInfo] = premiseDeductionResults.map(_.matchedPropositionInfoList).flatten
          val premisePropositionIds: Set[String] = matchedPropositionInfoList.map(_.propositionId).toSet
          val claimPropositionIds :Set[String] = result.deductionResultMap.get(CLAIM.index.toString).get.matchedPropositionInfoList.map(_.propositionId).toSet[String]

          //There must be at least one Claim that corresponds to at least one Premise proposition.
          (premisePropositionIds & claimPropositionIds).size - premisePropositionIds.size match {
            case 0 => {
              val originalDeductionResult: DeductionResult = result.deductionResultMap.get(CLAIM.index.toString).get
              val updateDeductionResult: DeductionResult = DeductionResult(
                status = originalDeductionResult.status,
                matchedPropositionInfoList = originalDeductionResult.matchedPropositionInfoList,
                deductionUnit = originalDeductionResult.deductionUnit,
                havePremiseInGivenProposition = true
              )
              AnalyzedSentenceObject(
                nodeMap = result.nodeMap,
                edgeList = result.edgeList,
                knowledgeFeatureNode = result.knowledgeFeatureNode,
                deductionResultMap = result.deductionResultMap.updated(CLAIM.index.toString, updateDeductionResult))
            }
            case _ =>  result
          }
        }
      }
    }else{
      result
    }
  }

  /**
   * final check
   * @param targetMatchedPropositionInfoList
   * @param aso
   * @param searchResults
   * @return
   */
  private def checkFinal(targetMatchedPropositionInfoList: List[MatchedPropositionInfo], aso: AnalyzedSentenceObject, searchResults: List[List[Neo4jRecordMap]]): AnalyzedSentenceObject = {
    //The targetMatchedPropositionInfoList contains duplicate propositionIds.
    if (targetMatchedPropositionInfoList.size < aso.edgeList.size) return aso
    //Pick up the most frequent propositionId
    val dupFreq = targetMatchedPropositionInfoList.groupBy(identity).filter(x => x._2.size >= aso.edgeList.size)
    if(dupFreq == 0) return aso

    val minFreqSize = dupFreq.mapValues(_.size).minBy(_._2)._2
    val propositionIdsHavingMinFreq: List[MatchedPropositionInfo] = targetMatchedPropositionInfoList.groupBy(identity).mapValues(_.size).filter(_._2 == minFreqSize).map(_._1).toList
    logger.debug(propositionIdsHavingMinFreq.toString())

    val coveredPropositionInfoList = propositionIdsHavingMinFreq
    //Does the chosen proposalId have a premise? T
    //he coveredPropositionInfoList contains a mixture of those that are established only by Claims and those that have Premise.
    val propositionInfoListHavingPremise:List[MatchedPropositionInfo] = coveredPropositionInfoList.filter(havePremiseNode(_))
    val propositionInfoListOnlyClaim :List[MatchedPropositionInfo] = coveredPropositionInfoList.filterNot(x => propositionInfoListHavingPremise.map(y => y.propositionId).contains(x.propositionId))

    val finalPropositionInfoList: List[MatchedPropositionInfo] = propositionInfoListHavingPremise.size match {
      case 0 => propositionInfoListOnlyClaim
      case _ => propositionInfoListOnlyClaim ::: checkClaimHavingPremise(propositionInfoListHavingPremise)
    }

    if(finalPropositionInfoList.size == 0) return aso

    val status = true
    //selectedPropositions includes trivialClaimsPropositionIds
    val deductionResult: DeductionResult = new DeductionResult(status, finalPropositionInfoList, "exact-match")
    val updateDeductionResultMap = aso.deductionResultMap.updated(aso.knowledgeFeatureNode.sentenceType.toString, deductionResult)
    AnalyzedSentenceObject(aso.nodeMap, aso.edgeList, aso.knowledgeFeatureNode, updateDeductionResultMap)

  }

  /**
   *
   * @param propositionId
   * @return
   */
  private def havePremise(propositionId: String): Boolean = {
    val query = "MATCH (n:PremiseNode)-[*]-(m:ClaimNode) WHERE m.propositionId ='%s'  RETURN (n)".format(propositionId)
    val jsonStr: String = getCypherQueryResult(query, "n")
    val neo4jRecords: Neo4jRecords = Json.parse(jsonStr).as[Neo4jRecords]
    neo4jRecords.records.size match {
      case 0 => false
      case _ => true
    }
  }

  /**
   *
   * @param targetMatchedPropositionInfoList
   * @return
   */
  private def checkClaimHavingPremise(targetMatchedPropositionInfoList: List[MatchedPropositionInfo]): List[MatchedPropositionInfo] = {
    //Pick up a node with the same surface layer as the Premise connected from Claim as x
    //Search for the one that has the corresponding ClaimId and has a premise
    targetMatchedPropositionInfoList.foldLeft(List.empty[MatchedPropositionInfo]) {
      (acc, x) => {
        val query = "MATCH (n1:PremiseNode)-[e:PremiseEdge]->(n2:PremiseNode) WHERE n1.propositionId='%s' AND n2.propositionId='%s' RETURN n1, e, n2".format(x.propositionId, x.propositionId)
        val jsonStr = FacadeForAccessNeo4J.getCypherQueryResult(query, "x")
        val neo4jRecords: Neo4jRecords = Json.parse(jsonStr).as[Neo4jRecords]
        val resultMatchedPropositionInfoList = neo4jRecords.records.size match {
          case 0 => List.empty[MatchedPropositionInfo]
          case _ => checkOnlyClaimNodes(neo4jRecords, targetMatchedPropositionInfoList)
        }
        acc ::: resultMatchedPropositionInfoList
      }
    }
  }

  /**
   *
   * @param neo4jRecords
   * @param targetMatchedPropositionInfoList
   * @return
   */
  private def checkOnlyClaimNodes(neo4jRecords: Neo4jRecords, targetMatchedPropositionInfoList: List[MatchedPropositionInfo]): List[MatchedPropositionInfo] = {

    val claimMatchedPropositionInfo: List[MatchedPropositionInfo] = neo4jRecords.records.foldLeft(List.empty[MatchedPropositionInfo]) {
      (acc, x) => {
        val surface1: String = x(0).value.logicNode.predicateArgumentStructure.surface
        val caseStr: String = x(1).value.logicEdge.caseStr
        val surface2: String = x(2).value.logicNode.predicateArgumentStructure.surface
        val query = "MATCH (n1:ClaimNode)-[e:ClaimEdge]->(n2:ClaimNode) WHERE n1.surface='%s' AND e.caseName='%s' AND n2.surface='%s' RETURN n1, e, n2".format(surface1, caseStr, surface2)
        val jsonStr: String = getCypherQueryResult(query, "")
        val neo4jRecordsForClaim: Neo4jRecords = Json.parse(jsonStr).as[Neo4jRecords]
        val additionalMatchedPropositionInfo = neo4jRecordsForClaim.records.foldLeft(List.empty[MatchedPropositionInfo]) {
          (acc2, x2) => {
            val propositionId = x2.head.value.logicNode.propositionId
            val sentenceId = x2.head.value.logicNode.sentenceId
            val matchedFeatureInfo = MatchedFeatureInfo(sentenceId, 1)
            acc2 :+ MatchedPropositionInfo(propositionId, List(matchedFeatureInfo))
          }
        }
        acc ::: additionalMatchedPropositionInfo
      }
    }
    //Checkpoint
    //・Are there all claims corresponding to premise?
    //・Does the obtained result have more propositionIds than the number of neo4jRecords records?得られてた結果でneo4jRecordsのレコード数と同数以上のpropositionIdを持つものが存在するかどうか？
    //・Multiple claims can guarantee one Premise, so it is not necessarily =, but there must be more Claims than the number of Premises.
    if (claimMatchedPropositionInfo.size < neo4jRecords.records.size) return List.empty[MatchedPropositionInfo]

    //val candidates: List[MatchedPropositionInfo] = claimMatchedPropositionInfo.groupBy(identity).mapValues(_.size).map(_._1).toList
    val candidates: List[MatchedPropositionInfo] = claimMatchedPropositionInfo.distinct
    //candidatesは、propositionId上の重複はない。
    if (candidates.size == 0) return List.empty[MatchedPropositionInfo]
    //ensure there are no Premise. only claim!
    val finalChoice:List[MatchedPropositionInfo] = candidates.filterNot(x => havePremise(x.propositionId))
    finalChoice.size match {
      case 0 => List.empty[MatchedPropositionInfo]
      case _ => finalChoice ::: targetMatchedPropositionInfoList
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
  private def analyzeGraphKnowledge(edge: KnowledgeBaseEdge, nodeMap: Map[String, KnowledgeBaseNode], sentenceType: Int, accParent: (List[List[Neo4jRecordMap]], List[MatchedPropositionInfo])): (List[List[Neo4jRecordMap]], List[MatchedPropositionInfo]) = {

    val sourceKey = edge.sourceId
    val targetKey = edge.destinationId
    val sourceNodeSurface = nodeMap.get(sourceKey).getOrElse().asInstanceOf[KnowledgeBaseNode].predicateArgumentStructure.surface
    val destinationNodeSurface = nodeMap.get(targetKey).getOrElse().asInstanceOf[KnowledgeBaseNode].predicateArgumentStructure.surface

    val nodeType: String = ToposoidUtils.getNodeType(CLAIM.index)
    val query = "MATCH (n1:%s)-[e]-(n2:%s) WHERE n1.surface='%s' AND e.caseName='%s' AND n2.surface='%s' RETURN n1, e, n2".format(nodeType, nodeType, sourceNodeSurface, edge.caseStr, destinationNodeSurface)
    logger.info(query)
    val jsonStr: String = getCypherQueryResult(query, "")
    //If there is even one that does not match, it is useless to search further
    if (jsonStr.equals("""{"records":[]}""")) return accParent
    val neo4jRecords: Neo4jRecords = Json.parse(jsonStr).as[Neo4jRecords]
    neo4jRecords.records.foldLeft(accParent) {
      (acc, x) => {
        (acc._1 :+ x, acc._2 :+ MatchedPropositionInfo(x.head.value.logicNode.propositionId, List(MatchedFeatureInfo(x.head.value.logicNode.sentenceId, 1))))
      }
    }
  }

}

