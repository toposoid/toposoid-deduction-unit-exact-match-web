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
import com.ideal.linked.toposoid.protocol.model.base.{AnalyzedSentenceObject, AnalyzedSentenceObjects, DeductionResult}
import com.ideal.linked.toposoid.protocol.model.neo4j.{Neo4jRecordMap, Neo4jRecords}
import com.typesafe.scalalogging.LazyLogging
import com.ideal.linked.toposoid.deduction.common.FacadeForAccessNeo4J.{existALlPropositionIdEqualId, getCypherQueryResult, havePremiseNode, neo4JData2AnalyzedSentenceObjectByPropositionId}

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
      Ok(Json.toJson(AnalyzedSentenceObjects(analyzedSentenceObjects.analyzedSentenceObjects.map(analyze(_))))).as(JSON)
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
  private def analyze(aso:AnalyzedSentenceObject): AnalyzedSentenceObject ={

    val (searchResults, propositionIds) = aso.edgeList.foldLeft((List.empty[List[Neo4jRecordMap]], List.empty[String])){
      (acc, x) => analyzeGraphKnowledge(x, aso.nodeMap, aso.sentenceType, acc)
    }
    if(propositionIds.size == 0) return aso

    if(aso.sentenceType == 0){
      //f the proposition is premise, check only if the same proposition exists as claim
      checkFinal(propositionIds, aso, searchResults)
    }else if(aso.sentenceType == 1){
      //If the proposition is a claim, check whether the proposition holds only as a claim or through premise.
      val onlyClaimPropositionIds = propositionIds.filterNot(havePremiseNode(_))
      if (onlyClaimPropositionIds.size > 0){
        //A case where a proposition (claim) can be true only by claim in the knowledge base
        checkFinal(onlyClaimPropositionIds, aso, searchResults)
      }else{
        //The case where the proposition (claim) becomes true via premis in knowledge base
        val claimHavingPremisePropositionIds = propositionIds.filter(havePremiseNode(_))
        val checkedPremiseAso =  checkClaimHavingPremise(claimHavingPremisePropositionIds.distinct, aso)
        if(checkedPremiseAso.deductionResultMap.get(aso.sentenceType.toString).get.matchedPropositionIds.size > 0){
          checkFinal(claimHavingPremisePropositionIds, checkedPremiseAso, searchResults)
        }else{
          aso
        }
      }
    }else{
      aso
    }
  }

  /**
   * A function that checks whether a proposition holds a claim via premise
   * @param targetPropositionIds
   * @param aso
   * @return
   */
  private def checkClaimHavingPremise(targetPropositionIds:List[String], aso:AnalyzedSentenceObject):AnalyzedSentenceObject ={
    for(propositionId <- targetPropositionIds){
      val updateAso = checkClaimHavingPremiseImpl(propositionId, aso)
      if(updateAso.deductionResultMap.get(aso.sentenceType.toString).get.matchedPropositionIds.size > 0) return updateAso
    }
    aso
  }

  /**
   * Concrete implementation of checkClaimHavingPremise
   * @param propositionId
   * @param aso
   * @return
   */
  private def checkClaimHavingPremiseImpl(targetPropositionId:String, aso:AnalyzedSentenceObject): AnalyzedSentenceObject = {
    //Pick up a node with the same surface layer as the Premise connected from Claim as x
    val query = "MATCH (n:PremiseNode)-[*]-(m:ClaimNode), (x:ClaimNode) WHERE m.propositionId ='%s' AND x.surface=n.surface  RETURN (n), (x)".format(targetPropositionId)
    val jsonStr = FacadeForAccessNeo4J.getCypherQueryResult(query, "x")
    val neo4jRecords:Neo4jRecords = Json.parse(jsonStr).as[Neo4jRecords]

    if(neo4jRecords.records.size > 0){
      val targetPropositionId1Set = neo4jRecords.records.map(_.filter(_.key.equals("x")).map(_.value.logicNode.propositionId)).flatten.toSet
      val targetAnalyzedSentenceObjectsFromNeo4j:List[AnalyzedSentenceObject] = FacadeForAccessNeo4J.neo4JData2AnalyzedSentenceObjectByPropositionId(targetPropositionId, 0).analyzedSentenceObjects

      val checkedAso:Set[AnalyzedSentenceObject] = targetPropositionId1Set.map(targetPropositionId1 =>{
        val sentenceInfo1 = AnalyzedSentenceObjectUtils.makeSentence(FacadeForAccessNeo4J.neo4JData2AnalyzedSentenceObjectByPropositionId(targetPropositionId1, 1).analyzedSentenceObjects.head)
        //Acquired information x from Neo4j contains multiple pieces of text information (for example, partially matching items, etc.), and it is necessary to compare each of them.
        targetAnalyzedSentenceObjectsFromNeo4j.foldLeft(aso){
          (acc, x) => {
            val sentenceInfo2 = AnalyzedSentenceObjectUtils.makeSentence(x)
            if (sentenceInfo1.get(1).get.sentence.equals(sentenceInfo2.get(0).get.sentence)) {
              val coveredPropositionIds = List(sentenceInfo1.get(1).get.propositionId, sentenceInfo2.get(0).get.propositionId)
              //Here, only the proposalId is added without outputting the final result. Leave the final decision to the checkFinal function
              val deductionResult: DeductionResult = new DeductionResult(false, aso.deductionResultMap.get(aso.sentenceType.toString).get.matchedPropositionIds ::: coveredPropositionIds, "")
              val updateDeductionResultMap = aso.deductionResultMap.updated(aso.sentenceType.toString, deductionResult)
              AnalyzedSentenceObject(aso.nodeMap, aso.edgeList, aso.sentenceType, aso.sentenceId, aso.lang, updateDeductionResultMap)
            }else{
              acc
          }
        }}
      })
      //If there are multiple premises, all corresponding Claims are required
      if(checkedAso.filter(_.deductionResultMap(aso.sentenceType.toString).matchedPropositionIds.size > 0).size == targetAnalyzedSentenceObjectsFromNeo4j.size){
        checkedAso.filter(_.deductionResultMap(aso.sentenceType.toString).matchedPropositionIds.size > 0).head
      }else{
        aso
      }

    }else{
      aso
    }
  }

  /**
   *　final check
   * @param targetPropositionIds
   * @param aso
   * @param searchResults
   * @return
   */
  private def checkFinal(targetPropositionIds:List[String], aso:AnalyzedSentenceObject, searchResults:List[List[Neo4jRecordMap]]): AnalyzedSentenceObject ={
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
    val additionalPropositionIds = aso.deductionResultMap.get(aso.sentenceType.toString).get.matchedPropositionIds
    val deductionResult:DeductionResult = new DeductionResult(status, coveredPropositionIds:::additionalPropositionIds, "exact-match")
    val updateDeductionResultMap = aso.deductionResultMap.updated(aso.sentenceType.toString, deductionResult)
    AnalyzedSentenceObject(aso.nodeMap, aso.edgeList, aso.sentenceType, aso.sentenceId, aso.lang, updateDeductionResultMap)

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
    val sourceNodeSurface = nodeMap.get(sourceKey).getOrElse().asInstanceOf[KnowledgeBaseNode].predicateArgumentStructure.surface
    val destinationNodeSurface = nodeMap.get(targetKey).getOrElse().asInstanceOf[KnowledgeBaseNode].predicateArgumentStructure.surface
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
}

