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

import com.ideal.linked.toposoid.common.{SentenceType, TransversalState, ToposoidUtils}
//import com.ideal.linked.toposoid.deduction.common.FacadeForAccessNeo4J.getCypherQueryResult
import com.ideal.linked.toposoid.knowledgebase.model.{KnowledgeBaseEdge, KnowledgeBaseSemiGlobalNode}
import com.ideal.linked.toposoid.protocol.model.base.{_}
import com.ideal.linked.toposoid.protocol.model.neo4j.Neo4jRecords
import com.typesafe.scalalogging.LazyLogging
import play.api.libs.json.{JsValue, Json}
import play.api.mvc._
import com.ideal.linked.toposoid.common.AuthenticityType

import scala.util.{Failure, Success, Try}
import com.ideal.linked.common.DeploymentConverter.conf


trait DeductionUnitController extends LazyLogging {
  protected def execute(): Action[JsValue]

  protected def analyzeGraphKnowledge(edges: List[KnowledgeBaseEdge], aso:AnalyzedSentenceObject, transversalState:TransversalState):List[CoveredPropositionEdge]
  //TODO: getMergedKnowledgeBaseSideInfoは、MatchedKnowledgeNodeから作成する。
  /*
  private def getMergedKnowledgeBaseSideInfo(coveredPropositionResults: List[CoveredPropositionEdge], confirmedCoveredPropositionResults:List[CoveredPropositionResult]):List[KnowledgeBaseSideInfo] = {
    val knowledgeBaseSideInfoList = coveredPropositionResults.map(x => x.knowledgeBaseSideInfoList).flatten
    val coveredPropositionEdges = confirmedCoveredPropositionResults.map(_.coveredPropositionEdges).flatten
    val confirmedKnowledgeBaseSideInfoList:List[KnowledgeBaseSideInfo] = coveredPropositionEdges.map(x => x.knowledgeBaseSideInfoList).flatten    
    knowledgeBaseSideInfoList ++ confirmedKnowledgeBaseSideInfoList
  }
  */

  private def getMergedKnowledgeBaseSideInfo(coveredPropositionEdges: List[CoveredPropositionEdge]):List[KnowledgeBaseSideInfo] = {
    
    coveredPropositionEdges.foldLeft(List.empty[KnowledgeBaseSideInfo]){
      (acc, x) => {
        
        //同一のsentenceIdを持っているものが対象なのでフィルターする。
        val sourceSentenceIds = x.sourceNode.isConfirmed match  {
          case true => x.sourceNode.matchedKnowledgeNodes.map(y => y.sentenceId).toSet
          case _ => Set()
        }        
        val destinationSentenceIds = x.destinationNode.isConfirmed match  {
          case true => x.destinationNode.matchedKnowledgeNodes.map(y => y.sentenceId).toSet
          case _ => Set()
        }        
        val confirmedSentenceIds  = sourceSentenceIds & destinationSentenceIds
        val distinctMatchedKnowledgeNodes = (x.sourceNode.matchedKnowledgeNodes:::x.destinationNode.matchedKnowledgeNodes).filter(y =>{
          confirmedSentenceIds.contains(y.sentenceId)
        }).distinct

        if(confirmedSentenceIds.size > 0){
          val confirmedKnowledgeBaseSideInfoList:List[KnowledgeBaseSideInfo] = distinctMatchedKnowledgeNodes.map(y => {
            KnowledgeBaseSideInfo(
              propositionId = y.propositionId,
              sentenceId = y.sentenceId,
              featureInfoList = y.featureInfoList,
              deductionUnits = List.empty[String]
            )
          })
          acc ::: confirmedKnowledgeBaseSideInfoList
        }else{
          acc
        }
      }
    }
  }
  /**
   * final check
   *
   * @param targetMatchedPropositionInfoList
   * @param aso
   * @param searchResults
   * @return
   */
  private def checkFinal(aso: AnalyzedSentenceObject, deductionUnitName:String, unsettledCoveredPropositionEdges:List[CoveredPropositionEdge], transversalState:TransversalState ): AnalyzedSentenceObject = {

    //The targetMatchedPropositionInfoList contains duplicate propositionIds.
    //Pick up the most frequent propositionId
    val mergedKnowledgeBaseSideInfo =  aso.deductionResult.evidenceKnowledgeList ::: getMergedKnowledgeBaseSideInfo(unsettledCoveredPropositionEdges)
    //val updatedCoveredPropositionResults = addCoveredPropositionResults(mergedKnowledgeBaseSideInfo, aso.deductionResult, unsettledCoveredPropositionResults, aso.knowledgeBaseSemiGlobalNode, deductionUnitName)
    val (updatedCoveredPropositionEdges, updatedKnowledgeBaseSideInfo) = updateCoveredPropositionEdges(mergedKnowledgeBaseSideInfo, aso, unsettledCoveredPropositionEdges, deductionUnitName, transversalState)
    /*
    val updateDeductionResult: DeductionResult = new DeductionResult(
      aso.deductionResult.status,
      aso.deductionResult.authenticityType,
      updatedCoveredPropositionEdges,
      mergedKnowledgeBaseSideInfo,
      aso.deductionResult.havePremiseInGivenProposition
    )
    val updateAso = AnalyzedSentenceObject(aso.nodeMap, aso.edgeList, aso.knowledgeBaseSemiGlobalNode, updateDeductionResult)

    val dupFreq = mergedKnowledgeBaseSideInfo.map(_.propositionId).groupBy(identity).filter(x => x._2.size >= aso.edgeList.size)
    if (dupFreq.size == 0) return updateAso

    //被覆サイズが最小のものを選ぶ。
    val minFreqSize = dupFreq.mapValues(_.size).minBy(_._2)._2
    val propositionIdsHavingMinFreq: List[String] = mergedKnowledgeBaseSideInfo.map(_.propositionId).groupBy(identity).mapValues(_.size).filter(_._2 == minFreqSize).map(_._1).toList
    logger.debug(propositionIdsHavingMinFreq.toString())

    val coveredPropositionInfoList = mergedKnowledgeBaseSideInfo.filter(x =>  propositionIdsHavingMinFreq.contains(x.propositionId))
    //Does the chosen proposalId have a premise? T
    //he coveredPropositionInfoList contains a mixture of those that are established only by Claims and those that have Premise.
    val propositionInfoListHavingPremise: List[KnowledgeBaseSideInfo] = coveredPropositionInfoList.filter(havePremise(_, transversalState))
    val propositionInfoListOnlyClaim: List[KnowledgeBaseSideInfo] = coveredPropositionInfoList.filterNot(x => propositionInfoListHavingPremise.map(y => y.propositionId).contains(x.propositionId))

    val finalPropositionInfoList: List[KnowledgeBaseSideInfo] = propositionInfoListHavingPremise.size match {
      case 0 => propositionInfoListOnlyClaim
      case _ => propositionInfoListOnlyClaim ::: checkClaimHavingPremise(propositionInfoListHavingPremise, transversalState)
    }
    */
    if (updatedKnowledgeBaseSideInfo.size == 0){
      val deductionResult: DeductionResult = new DeductionResult(aso.deductionResult.status, aso.deductionResult.authenticityType, updatedCoveredPropositionEdges, updatedKnowledgeBaseSideInfo)
      AnalyzedSentenceObject(aso.nodeMap, aso.edgeList, aso.knowledgeBaseSemiGlobalNode, deductionResult)
    }else{
      val status = true
      val deductionResult: DeductionResult = new DeductionResult(status,AuthenticityType.TRUE.index, updatedCoveredPropositionEdges, updatedKnowledgeBaseSideInfo)
      //val updateDeductionResult = aso.deductionResult.updated(aso.knowledgeBaseSemiGlobalNode.sentenceType.toString, deductionResult)
      AnalyzedSentenceObject(aso.nodeMap, aso.edgeList, aso.knowledgeBaseSemiGlobalNode, deductionResult)
    }
  
    
    //selectedPropositions includes trivialClaimsPropositionIds
    /*
    val updatedCoveredPropositionResults2 = updatedCoveredPropositionResults.foldLeft(List.empty[CoveredPropositionResult]){
      (acc, x) => {   
        //TODO: MatchedKnowledgeNodeから判定する。
        //if(x.deductionUnit.equals(deductionUnitName)){
        if(x.coveredPropositionEdges.filter(y => y.knowledgeBaseSideInfoList.filter(z => z.deductionUnits.contains(deductionUnitName)).size > 0).size > 0){
          //TODO:finalPropositionInfoListをcoveredPropositionEdgesのKnoledgeSideInfoに追加する必要がある。
          val updatedCoveredPropositionEdges =  x.coveredPropositionEdges.map(y => {
              CoveredPropositionEdge(
                sourceNode = y.sourceNode,
                destinationNode = y.destinationNode,              
              )
            })
          acc :+ CoveredPropositionResult(
            //deductionUnit = x.deductionUnit,
            propositionId = x.propositionId,
            sentenceId = x.sentenceId,
            coveredPropositionEdges = updatedCoveredPropositionEdges)
        }else{
          acc :+ x
        }
      }
    }
    */


    //val deductionResult: DeductionResult = new DeductionResult(status,AuthenticityType.TRUE.index, updatedCoveredPropositionResults2, mergedKnowledgeBaseSideInfo)
    /*
    val deductionResult: DeductionResult = new DeductionResult(status,AuthenticityType.TRUE.index, updatedCoveredPropositionEdges, finalPropositionInfoList)
    //val updateDeductionResult = aso.deductionResult.updated(aso.knowledgeBaseSemiGlobalNode.sentenceType.toString, deductionResult)
    AnalyzedSentenceObject(aso.nodeMap, aso.edgeList, aso.knowledgeBaseSemiGlobalNode, deductionResult)
    */
  }

  private def updateCoveredPropositionEdges(mergedKnowledgeBaseSideInfo:List[KnowledgeBaseSideInfo], aso: AnalyzedSentenceObject, unsettledCoveredPropositionEdges:List[CoveredPropositionEdge], deductionUnitName:String, transversalState:TransversalState) :(List[CoveredPropositionEdge],List[KnowledgeBaseSideInfo])  ={
    //TODO:もっと良い方法がないか見直し
    //もし同じ表層かつ同じ関係性も持つエッジが一つの文章で重複して存在する場合、単純にpropositionIdをユニークにしても命題のエッジの数を被覆したとは言えない。重複も含めてカウントする必要がある。
    //val dupFreq = mergedKnowledgeBaseSideInfo.map(_.propositionId).groupBy(identity).filter(x => x._2.size > deductionResult.coveredPropositionEdges.size)
    //全てのエッジが何か対応があるという条件がないとダメっていうのは良いんだっけか？ 部分的に一致しているという情報は残さない？
    val dupFreq = mergedKnowledgeBaseSideInfo.map(_.propositionId).groupBy(identity).filter(x => x._2.size >= aso.nodeMap.size)
    if(dupFreq.size == 0) return (aso.deductionResult.coveredPropositionEdges, aso.deductionResult.evidenceKnowledgeList)
    val minFreqSize = dupFreq.mapValues(_.size).minBy(_._2)._2
  
    val propositionIdsHavingMinFreq: List[String] = mergedKnowledgeBaseSideInfo.map(_.propositionId).groupBy(identity).mapValues(_.size).filter(_._2 == minFreqSize).map(_._1).toList
    val filteredKnowledgeBaseSideInfo = mergedKnowledgeBaseSideInfo.filter(x =>  propositionIdsHavingMinFreq.contains(x.propositionId))
    
    val filteredCoveredPropositionEdges:List[CoveredPropositionEdge] = unsettledCoveredPropositionEdges.filter(
      x => {
        if(x.sourceNode.isConfirmed && x.destinationNode.isConfirmed) {
          val confirmedSourceNodeSize = x.sourceNode.matchedKnowledgeNodes.filter(y => propositionIdsHavingMinFreq.contains(y.propositionId)).size
          val confirmedDestinationNodeSize = x.destinationNode.matchedKnowledgeNodes.filter(y => propositionIdsHavingMinFreq.contains(y.propositionId)).size
          confirmedSourceNodeSize > 0 && confirmedDestinationNodeSize > 0
        }else{
          false
        }
        //x.knowledgeBaseSideInfoList.filter(y => propositionIdsHavingMinFreq.contains(y.propositionId)).size > 0
        //propositionIdsHavingMinFreq.contains(x._1.propositionId)
      })


    val coveredPropositionInfoList = mergedKnowledgeBaseSideInfo.filter(x =>  propositionIdsHavingMinFreq.contains(x.propositionId))
    //Does the chosen proposalId have a premise? T
    //he coveredPropositionInfoList contains a mixture of those that are established only by Claims and those that have Premise.
    val propositionInfoListHavingPremise: List[KnowledgeBaseSideInfo] = coveredPropositionInfoList.filter(havePremise(_, transversalState))
    val propositionInfoListOnlyClaim: List[KnowledgeBaseSideInfo] = coveredPropositionInfoList.filterNot(x => propositionInfoListHavingPremise.map(y => y.propositionId).contains(x.propositionId))

    val finalPropositionInfoList: List[KnowledgeBaseSideInfo] = propositionInfoListHavingPremise.size match {
      case 0 => propositionInfoListOnlyClaim
      case _ => propositionInfoListOnlyClaim ::: checkClaimHavingPremise(propositionInfoListHavingPremise, transversalState)
    }
    (filteredCoveredPropositionEdges, finalPropositionInfoList)
    /*
    if(finalPropositionInfoList.size == 0){
      (filteredCoveredPropositionEdges, filteredKnowledgeBaseSideInfo)
    }else{
      (filteredCoveredPropositionEdges, finalPropositionInfoList)
    }
    */
    //(List.empty[CoveredPropositionEdge], List.empty[KnowledgeBaseSideInfo])
  }
  /*
  private def addCoveredPropositionResults(mergedKnowledgeBaseSideInfo:List[KnowledgeBaseSideInfo] , deductionResult:DeductionResult, unsettledCoveredPropositionResults:List[CoveredPropositionEdge], knowledgeBaseSemiGlobalNode:KnowledgeBaseSemiGlobalNode, deductionUnitName:String): List[CoveredPropositionResult] = {

    //TODO:もっと良い方法がないか見直し
    val dupFreq = mergedKnowledgeBaseSideInfo.map(_.propositionId).groupBy(identity).filter(x => x._2.size > deductionResult.coveredPropositionResults.size)
    if(dupFreq.size == 0) return deductionResult.coveredPropositionResults
    val minFreqSize = dupFreq.mapValues(_.size).minBy(_._2)._2

    val propositionIdsHavingMinFreq: List[String] = mergedKnowledgeBaseSideInfo.map(_.propositionId).groupBy(identity).mapValues(_.size).filter(_._2 == minFreqSize).map(_._1).toList
    //val filteredKnowledgeBaseSideInfo = mergedKnowledgeBaseSideInfo.filter(x =>  propositionIdsHavingMinFreq.contains(x.propositionId))

    val filteredCoveredPropositionEdges:List[CoveredPropositionEdge] = unsettledCoveredPropositionResults.filter(
      x => {
        if(x.sourceNode.isConfirmed && x.destinationNode.isConfirmed) {
          val confirmedSourceNodeSize = x.sourceNode.matchedKnowledgeNodes.filter(y => propositionIdsHavingMinFreq.contains(y.propositionId)).size
          val confirmedDestinationNodeSize = x.destinationNode.matchedKnowledgeNodes.filter(y => propositionIdsHavingMinFreq.contains(y.propositionId)).size
          confirmedSourceNodeSize > 0 && confirmedDestinationNodeSize > 0
        }else{
          false
        }
        //x.knowledgeBaseSideInfoList.filter(y => propositionIdsHavingMinFreq.contains(y.propositionId)).size > 0
        //propositionIdsHavingMinFreq.contains(x._1.propositionId)
      })

    val coveredPropositionResult = CoveredPropositionResult(
      propositionId = knowledgeBaseSemiGlobalNode.propositionId,
      sentenceId = knowledgeBaseSemiGlobalNode.sentenceId,
      coveredPropositionEdges = filteredCoveredPropositionEdges
      )

    deductionResult.coveredPropositionResults :+ coveredPropositionResult
  }
  */
  /**
   *
   * @param matchedPropositionInfo
   * @return
   */
  private def havePremise(matchedPropositionInfo: KnowledgeBaseSideInfo, transversalState:TransversalState): Boolean = {
    val query = "MATCH (n:PremiseNode)-[*]-(m:ClaimNode) WHERE m.propositionId ='%s'  RETURN (n)".format(matchedPropositionInfo.propositionId)
    val jsonStr: String = getCypherQueryResult(query, "n", transversalState)
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
  private def checkClaimHavingPremise(targetMatchedPropositionInfoList: List[KnowledgeBaseSideInfo], transversalState:TransversalState): List[KnowledgeBaseSideInfo] = {
    //Pick up a node with the same surface layer as the Premise connected from Claim as x
    //Search for the one that has the corresponding ClaimId and has a premise
    targetMatchedPropositionInfoList.foldLeft(List.empty[KnowledgeBaseSideInfo]) {
      (acc, x) => {
        val query = "MATCH (n1:PremiseNode)-[e:LocalEdge{logicType:'-'}]->(n2:PremiseNode) WHERE n1.propositionId='%s' AND n2.propositionId='%s' RETURN n1, e, n2".format(x.propositionId, x.propositionId)
        val jsonStr = getCypherQueryResult(query, "x", transversalState)
        val neo4jRecords: Neo4jRecords = Json.parse(jsonStr).as[Neo4jRecords]
        val resultMatchedPropositionInfoList = neo4jRecords.records.size match {
          case 0 => List.empty[KnowledgeBaseSideInfo]
          case _ => checkOnlyClaimNodes(neo4jRecords, targetMatchedPropositionInfoList, transversalState)
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
  private def checkOnlyClaimNodes(neo4jRecords: Neo4jRecords, targetMatchedPropositionInfoList: List[KnowledgeBaseSideInfo], transversalState:TransversalState): List[KnowledgeBaseSideInfo] = {

    val claimMatchedPropositionInfo: List[KnowledgeBaseSideInfo] = neo4jRecords.records.foldLeft(List.empty[KnowledgeBaseSideInfo]) {
      (acc, x) => {
        val surface1: String = x(0).value.localNode.get.predicateArgumentStructure.surface
        val caseStr: String = x(1).value.localEdge.get.caseStr
        val surface2: String = x(2).value.localNode.get.predicateArgumentStructure.surface
        val query = "MATCH (n1:ClaimNode)-[e:LocalEdge]->(n2:ClaimNode) WHERE n1.surface='%s' AND e.caseName='%s' AND n2.surface='%s' RETURN n1, e, n2".format(surface1, caseStr, surface2)
        val jsonStr: String = getCypherQueryResult(query, "", transversalState)
        val neo4jRecordsForClaim: Neo4jRecords = Json.parse(jsonStr).as[Neo4jRecords]
        val additionalMatchedPropositionInfo = neo4jRecordsForClaim.records.foldLeft(List.empty[KnowledgeBaseSideInfo]) {
          (acc2, x2) => {
            val propositionId = x2.head.value.localNode.get.propositionId
            val sentenceId = x2.head.value.localNode.get.sentenceId
            val matchedFeatureInfo = MatchedFeatureInfo(sentenceId, 1)
            acc2 :+ KnowledgeBaseSideInfo(propositionId, sentenceId, List(matchedFeatureInfo), List("exact-match"))
          }
        }
        acc ::: additionalMatchedPropositionInfo
      }
    }
    //Checkpoint
    //・Are there all claims corresponding to premise?
    //・Does the obtained result have more propositionIds than the number of neo4jRecords records?得られてた結果でneo4jRecordsのレコード数と同数以上のpropositionIdを持つものが存在するかどうか？
    //・Multiple claims can guarantee one Premise, so it is not necessarily =, but there must be more Claims than the number of Premises.
    if (claimMatchedPropositionInfo.size < neo4jRecords.records.size) return List.empty[KnowledgeBaseSideInfo]

    //val candidates: List[MatchedPropositionInfo] = claimMatchedPropositionInfo.groupBy(identity).mapValues(_.size).map(_._1).toList
    val candidates: List[KnowledgeBaseSideInfo] = claimMatchedPropositionInfo.distinct
    //candidatesは、propositionId上の重複はない。
    if (candidates.size == 0) return List.empty[KnowledgeBaseSideInfo]
    //ensure there are no Premise. only claim!
    val finalChoice: List[KnowledgeBaseSideInfo] = candidates.filterNot(x => this.havePremise(x, transversalState))
    finalChoice.size match {
      case 0 => List.empty[KnowledgeBaseSideInfo]
      case _ => finalChoice ::: targetMatchedPropositionInfoList
    }

  }

  /**
   *
   * @param aso
   * @return
   */
  private def getUnsettledEdges(aso:AnalyzedSentenceObject): List[KnowledgeBaseEdge] = {
    val pairSetList = aso.deductionResult.coveredPropositionEdges.foldLeft(List.empty[Set[String]]){
        (acc, x) => {
          acc :+ Set(x.sourceNode.terminalId, x.destinationNode.terminalId)
        }
      }
    aso.edgeList.filterNot(x => {
      val targetLink = Set(x.sourceId, x.destinationId)
      pairSetList.contains(targetLink)
    })
  }

/*
  private def getUnsettledEdges(aso:AnalyzedSentenceObject): List[KnowledgeBaseEdge] = {
    val pairSetList = aso.deductionResult.coveredPropositionResults.foldLeft(List.empty[Set[String]]){
        (acc, x) => {
          acc ++ x.coveredPropositionEdges.foldLeft(List.empty[Set[String]]) {
            (acc2, y) => {
              acc2 :+ Set(y.sourceNode.terminalId, y.destinationNode.terminalId)
            }
          }
        }
      }
    aso.edgeList.filterNot(x => {
      val targetLink = Set(x.sourceId, x.destinationId)
      pairSetList.contains(targetLink)
    })
  }
*/
  /**
   *
   * @param edge
   * @param aso
   * @param deductionUnitFeatureTypes
   * @return
   */
  private def haveFeatureTypeToProcess(edge: KnowledgeBaseEdge, aso: AnalyzedSentenceObject, deductionUnitFeatureTypes:List[Int]): Boolean = {
    val sourceKnowledgeFeatureReferences = aso.nodeMap.get(edge.sourceId).get.localContext.knowledgeFeatureReferences
    val destinationKnowledgeFeatureReferences = aso.nodeMap.get(edge.destinationId).get.localContext.knowledgeFeatureReferences
    val isSourceSideOk = sourceKnowledgeFeatureReferences.size match {
      case 0 =>  true
      case _ => {
        sourceKnowledgeFeatureReferences.filter(x => deductionUnitFeatureTypes.contains(x.featureType)).size > 0
      }
    }
    val isDestinationSideOk = destinationKnowledgeFeatureReferences.size match {
      case 0 => true
      case _ => {
        destinationKnowledgeFeatureReferences.filter(x => deductionUnitFeatureTypes.contains(x.featureType)).size > 0
      }
    }
    isSourceSideOk && isDestinationSideOk
  }

  /**
   * This function analyzes whether the entered text exactly matches.
   *
   * @param aso
   * @param asos
   * @return
   */
  def analyze(aso: AnalyzedSentenceObject, asos: List[AnalyzedSentenceObject], deductionUnitName:String, deductionUnitFeatureTypes:List[Int], transversalState:TransversalState): AnalyzedSentenceObject = {
    //Excluding those for which the existence of links has already been confirmed in edgeList
    

    val coveredPropositionResults:List[CoveredPropositionEdge] = analyzeGraphKnowledge(getUnsettledEdges(aso), aso, transversalState)
    /*
    val coveredPropositionResults = getUnsettledEdges(aso).foldLeft(List.empty[CoveredPropositionEdge]) {
      (acc, x) => {
        //If the feature does not match, it cannot be evaluated and will be skipped.
        if (haveFeatureTypeToProcess(x, aso, deductionUnitFeatureTypes)) {
          analyzeGraphKnowledge(x, aso, acc, transversalState)
        } else{
          acc
        }
      }
    }
    */

    if (coveredPropositionResults.size == 0) return aso
    val result = checkFinal(aso, deductionUnitName, coveredPropositionResults, transversalState)
    if(!result.deductionResult.status) return result
    //This process requires that the Premise has already finished in calculating the DeductionResult
    if (aso.knowledgeBaseSemiGlobalNode.sentenceType == SentenceType.CLAIM.index) {

      //val premiseDeductionResults: List[DeductionResult] = asos.map(x => x.deductionResultMap.get(PREMISE.index.toString).get)
      val premiseDeductionResults: List[DeductionResult] = asos.filter(x => x.knowledgeBaseSemiGlobalNode.sentenceType == SentenceType.PREMISE.index).map(y => y.deductionResult)
      //If there is no deduction result that makes premise true, return the process.
      if (premiseDeductionResults.filter(_.status).size == 0) return result
      asos.filter(x => x.knowledgeBaseSemiGlobalNode.sentenceType == SentenceType.PREMISE.index).size match {
        case 0 => result
        case _ => {
          val knowledgeBaseSideInfoList = premiseDeductionResults.map(y => y.evidenceKnowledgeList).flatten
          val premisePropositionIds: Set[String] = knowledgeBaseSideInfoList.map(_.propositionId).toSet
          //Depending on the conditions, the result is claim information.
          val claimPropositionIds:Set[String] = result.deductionResult.evidenceKnowledgeList.map(_.propositionId).toSet
          //There must be at least one Claim that corresponds to at least one Premise proposition.
          (premisePropositionIds & claimPropositionIds).size - premisePropositionIds.size match {
            case 0 => {
              //val originalDeductionResult: DeductionResult = result.deductionResultMap.get(CLAIM.index.toString).get
              val originalDeductionResult: DeductionResult = result.deductionResult
              val updateDeductionResult: DeductionResult = DeductionResult(
                status = originalDeductionResult.status,
                authenticityType = AuthenticityType.TRUE.index,
                coveredPropositionEdges = originalDeductionResult.coveredPropositionEdges,
                //coveredPropositionResults = originalDeductionResult.coveredPropositionResults,
                evidenceKnowledgeList = knowledgeBaseSideInfoList,
                havePremiseInGivenProposition = true
              )
              AnalyzedSentenceObject(
                nodeMap = result.nodeMap,
                edgeList = result.edgeList,
                knowledgeBaseSemiGlobalNode = result.knowledgeBaseSemiGlobalNode,
                deductionResult = updateDeductionResult
              )
            }
            case _ => result
          }
        }
      }
    } else {
      result
    }
  }

  def getCypherQueryResult(query:String, target:String, transversalState:TransversalState): String = Try{
    val retryNum =  conf.getInt("retryCallMicroserviceNum") -1
    for (i <- 0 to retryNum) {
      //val result:String  = this.getCypherQueryResultImpl(query, target, transversalState)
      val json = """{ "query":"%s", "target":"%s" }""".format(query, target)
      val result:String  = ToposoidUtils.callComponent(
        json,
        conf.getString("TOPOSOID_GRAPHDB_WEB_HOST"),
        conf.getString("TOPOSOID_GRAPHDB_WEB_PORT"),
        "getQueryFormattedResult",
        transversalState
      )
      if (result != "{}") {
        return result
      }
      if(i == retryNum) throw new Exception("Results were not returned properly")
    }
    ""
  }match {
    case Success(s) => s
    case Failure(e) => throw e
  }
 
}
