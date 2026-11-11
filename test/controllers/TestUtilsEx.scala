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


import com.ideal.linked.toposoid.common.{FeatureType, DataEntryType, Neo4JUtilsImpl, ToposoidUtils, TransversalState}
import com.ideal.linked.toposoid.knowledgebase.regist.model.{ImageReference, Knowledge, KnowledgeForImage, PropositionRelation, Reference}
import com.ideal.linked.common.DeploymentConverter.conf
import com.ideal.linked.toposoid.protocol.model.neo4j.Neo4jRecords
import com.ideal.linked.toposoid.protocol.model.parser.{KnowledgeForParser, KnowledgeSentenceSetForParser}
import com.ideal.linked.toposoid.test.utils.TestUtils
import play.api.libs.json.Json
import com.ideal.linked.toposoid.protocol.model.base.VerifyingEdges
import com.ideal.linked.toposoid.knowledgebase.model.{KnowledgeBaseNode, KnowledgeFeatureReference, LocalContext}
import com.ideal.linked.toposoid.protocol.model.base.{AnalyzedSentenceObject, AnalyzedSentenceObjects}
//import com.ideal.linked.toposoid.knowledgebase.featurevector.model.RegistContentResult

//case class ImageBoxInfo(x:Int, y:Int, weight:Int, height:Int)

object TestUtilsEx {
  val neo4JUtils = new Neo4JUtilsImpl()
  def deleteNeo4JAllData(transversalState:TransversalState): Unit = {
    val query = "MATCH (n) OPTIONAL MATCH (n)-[r]-() DELETE n,r"
    neo4JUtils.executeQuery(query, transversalState)
  }

  def executeQueryAndReturn(query:String, transversalState:TransversalState): Neo4jRecords = {
    neo4JUtils.executeQueryAndReturn(query, transversalState)
  }

  def registerSingleClaim(knowledgeForParser:KnowledgeForParser, transversalState:TransversalState):Unit = {
    val knowledgeSentenceSetForParser = KnowledgeSentenceSetForParser(
      List.empty[KnowledgeForParser],
      List.empty[PropositionRelation],
      List(knowledgeForParser),
      List.empty[PropositionRelation])
    TestUtils.registerData(knowledgeSentenceSetForParser, transversalState, addVectorFlag = false)
  }

  var usedUuidList = List.empty[String]
  def getUUID(): String = {
    var uuid: String = java.util.UUID.randomUUID().toString
    while (usedUuidList.filter(_.equals(uuid)).size > 0) {
      uuid = java.util.UUID.randomUUID().toString
    }
    usedUuidList = usedUuidList :+ uuid
    //logger.info(uuid)
    uuid
  }
 
  def isPosiNegaMatch(sentenceId:String, verifyingEdgesList:List[VerifyingEdges]): Boolean = {

    val targetverifyingEdges = verifyingEdgesList.filter(x => x.sentenceId.equals(sentenceId))        
    targetverifyingEdges.foldLeft(true)((acc, x) => {
      x.coveredPropositionEdges.size match {
        case 0 => acc & false
        case _ => {
          x.coveredPropositionEdges.foldLeft(acc)((acc2, y) => {
            acc2 & (y.sourceNode.matchedKnowledgeNodes ::: y.destinationNode.matchedKnowledgeNodes).foldLeft(acc2)((acc3, z) => {
              acc3 & z.isNegativeMeaning 
            })
          })
        } 
      }
    })
  }
}
