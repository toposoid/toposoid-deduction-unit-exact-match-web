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

import com.ideal.linked.toposoid.common.ToposoidUtils
import com.ideal.linked.toposoid.knowledgebase.model.KnowledgeBaseNode
import com.ideal.linked.toposoid.protocol.model.base.{AnalyzedSentenceObject, AnalyzedSentenceObjects, DeductionResult}
import com.ideal.linked.toposoid.protocol.model.neo4j.{Neo4jRecodeUnit, Neo4jRecordMap, Neo4jRecords}
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
   * This function receives the predicate argument structure analysis result of a Japanese sentence as JSON,
   * checks whether it matches logically strictly with the knowledge database, and returns the result in JSON.
   */
  def execute()  = Action(parse.json) { request =>
    try {
      val json = request.body
      val analyzedSentenceObjects: AnalyzedSentenceObjects = Json.parse(json.toString).as[AnalyzedSentenceObjects]
      val convertAnalyzedSentenceObjects = analyzedSentenceObjects.analyzedSentenceObjects.map(analyze)
      Ok(Json.toJson(AnalyzedSentenceObjects(convertAnalyzedSentenceObjects))).as(JSON)

    }catch {
      case e: Exception => {
        logger.error(e.toString, e)
        BadRequest(Json.obj("status" -> "Error", "message" -> e.toString()))
      }
    }
  }

  /**
   *　This function analyzes whether the entered text exactly matches.　
   * @param aso
   */
  private def analyze(aso:AnalyzedSentenceObject): AnalyzedSentenceObject = Try{

    var propositionIds = List.empty[String]
    var searchResults= List.empty[List[Neo4jRecordMap]]

    for(edge <- aso.edgeList) {
      val sourceKey = edge.sourceId
      val targetKey = edge.destinationId
      val sourceNodeSurface = aso.nodeMap.get(sourceKey).getOrElse().asInstanceOf[KnowledgeBaseNode].surface
      val destinationNodeSurface = aso.nodeMap.get(targetKey).getOrElse().asInstanceOf[KnowledgeBaseNode].surface

      val nodeType:String = ToposoidUtils.getNodeType(aso.sentenceType)

      val query = "MATCH (n1:%s)-[e]-(n2:%s) WHERE n1.surface='%s' AND e.caseName='%s' AND n2.surface='%s' RETURN n1, e, n2".format(nodeType, nodeType, sourceNodeSurface, edge.caseStr, destinationNodeSurface)
      logger.info(query)
      val jsonStr:String = getCypherQueryResult(query, "")
      //一つでも合致しないものがあったらそれ以上検索しても無駄
      if(jsonStr.equals(""""{"records":[]}"""")) return aso
      val neo4jRecords:Neo4jRecords = Json.parse(jsonStr).as[Neo4jRecords]
      neo4jRecords.records.foreach( record => {
        searchResults = searchResults :+ record
        record.foreach { map =>
          logger.debug(map.key, map.value)
          if(map.key.equals("n1")){
            val unit:Neo4jRecodeUnit = map.value
            propositionIds = propositionIds :+ unit.logicNode.propositionId
          }
        }
      })
    }

    if(propositionIds.size < aso.edgeList.size){
      return aso
    }else{
      //Pick up the most frequent propositionId
      val axiomIdHavingMaxFreq = propositionIds.groupBy(identity).mapValues(_.size).maxBy(_._2)._1
      logger.debug(axiomIdHavingMaxFreq)
      //このpropositionIdを持つ検索結果の数とエッジの数が等しければ厳格に一致するとする。部分一致ではなくなる。
      val selectedList =  searchResults.filter(existALlPropositionIdEqualId(axiomIdHavingMaxFreq, _))
      if(selectedList.size == aso.edgeList.size){
        val deductionResult:DeductionResult = new DeductionResult(true, List(axiomIdHavingMaxFreq), "exact-match")
        val updateDeductionResultMap = aso.deductionResultMap.updated(aso.sentenceType.toString, deductionResult)
        return new AnalyzedSentenceObject(aso.nodeMap, aso.edgeList, aso.sentenceType, updateDeductionResultMap)
      }
    }
    return aso

  }match {
    case Failure(e) => throw e
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
