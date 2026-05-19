# toposoid-deduction-unit-exact-match-web
This is a WEB API that works as a microservice within the toposoid project.
Toposoid is a knowledge base construction platform.(see [Toposoid　Root Project](https://github.com/toposoid/toposoid.git))
This microservice provides the ability to determine if the text you enter matches the knowledge graph exactly. 

[![Unit Test And Build Image Action](https://github.com/toposoid/toposoid-deduction-unit-exact-match-web/actions/workflows/action.yml/badge.svg?branch=main)](https://github.com/toposoid/toposoid-deduction-unit-exact-match-web/actions/workflows/action.yml)
* API Image
  * Input
  * <img width="1167" src="https://github.com/toposoid/toposoid-deduction-unit-exact-match-web/assets/82787843/b2c37656-064a-4c00-91b0-52347666e997">
  * Output
  * <img width="1166" src="https://github.com/toposoid/toposoid-deduction-unit-exact-match-web/assets/82787843/34851da1-5555-47bd-9cf6-6a84dcc8a81a">

## Requirements
* Docker version 20.10.x, or late
* docker-compose version 1.22.x
* The following microservices must be running
  * scala-data-accessor-neo4j-web
  * neo4j

## Recommended Environment For Standalone
* Required: at least 16GB of RAM
* Required: at least 32G of HDD(Total required Docker Image size)
* Please understand that since we are dealing with large models such as LLM, the Dockerfile size is large and the required machine SPEC is high.


## Setup For Standalone
```bssh
docker-compose up
```
The first startup takes a long time until docker pull finishes.
## Usage
```bash
# Please refer to the following for information on registering data to try deduction.
# ref. https://github.com/toposoid/toposoid-knowledge-register-web
#for example
curl -X POST -H "Content-Type: application/json" -H 'X_TOPOSOID_TRANSVERSAL_STATE: {"userId":"test-user", "username":"guest", "roleId":0, "csrfToken":""}' -d '{
    "premiseList": [],
    "premiseLogicRelation": [],
    "claimList": [
        {
            "sentence": "案ずるより産むが易し。",
            "lang": "ja_JP",
            "extentInfoJson": "{}",
            "isNegativeSentence": false,
            "knowledgeForImages":[],
            "knowledgeForTables": [],
            "knowledgeForDocument": {
              "id": "",
              "filename": "",
              "url": "",
              "titleOfTopPage": ""
            },
            "documentPageReference": {
              "pageNo": -1,
              "references": [],
              "tableOfContents": [],
              "headlines": []                
            }}
    ],
    "claimLogicRelation": [
    ]
}
' http://localhost:9002/registerForManual


# Deduction
curl -X POST -H "Content-Type: application/json" -H 'X_TOPOSOID_TRANSVERSAL_STATE: {"userId":"test-user", "username":"guest", "roleId":0, "csrfToken":""}' -d '{
  "analyzedSentenceObjects": [
    {
      "nodeMap": {
        "4a2994a1-ec7a-438b-a290-0cfb563a5170-2": {
          "localContext": {
            "domains": {
              "": ""
            },
            "categories": {
              "": ""
            },
            "rangeExpressions": {
              "": {}
            },
            "namedEntities": {},
            "lang": "ja_JP",
            "properNouns": {},
            "knowledgeFeatureReferences": []
          },
          "predicateArgumentStructure": {
            "caseGroupType": 0,
            "surface": "易し。",
            "casePhrase": "",
            "casePhraseId": "",
            "modalityType": "-",
            "dependType": "D",
            "surfaceYomi": "やさし。",
            "normalizedNameYomi": "やさしい",
            "parentId": -1,
            "isDenialWord": false,
            "normalizedName": "易しい",
            "currentId": 2,
            "morphemes": [
              "形容詞,*,イ形容詞イ段,文語基本形",
              "特殊,句点,*,*"
            ],
            "isMainSection": true,
            "nodeType": 1,
            "parallelType": "-",
            "isConditionalConnection": false,
            "caseType": "文末"
          },
          "propositionId": "612bf3d6-bdb5-47b9-a3a6-185015c8c414",
          "nodeId": "4a2994a1-ec7a-438b-a290-0cfb563a5170-2",
          "sentenceId": "4a2994a1-ec7a-438b-a290-0cfb563a5170"
        },
        "4a2994a1-ec7a-438b-a290-0cfb563a5170-1": {
          "localContext": {
            "domains": {
              "産む": "家庭・暮らし"
            },
            "categories": {
              "": ""
            },
            "rangeExpressions": {
              "": {}
            },
            "namedEntities": {},
            "lang": "ja_JP",
            "properNouns": {},
            "knowledgeFeatureReferences": []
          },
          "predicateArgumentStructure": {
            "caseGroupType": 0,
            "surface": "産むが",
            "casePhrase": "",
            "casePhraseId": "",
            "modalityType": "-",
            "dependType": "D",
            "surfaceYomi": "うむが",
            "normalizedNameYomi": "うむ",
            "parentId": 2,
            "isDenialWord": false,
            "normalizedName": "産む",
            "currentId": 1,
            "morphemes": [
              "動詞,*,子音動詞マ行,基本形",
              "助詞,接続助詞,*,*"
            ],
            "isMainSection": false,
            "nodeType": 1,
            "parallelType": "-",
            "isConditionalConnection": false,
            "caseType": "連用"
          },
          "propositionId": "612bf3d6-bdb5-47b9-a3a6-185015c8c414",
          "nodeId": "4a2994a1-ec7a-438b-a290-0cfb563a5170-1",
          "sentenceId": "4a2994a1-ec7a-438b-a290-0cfb563a5170"
        },
        "4a2994a1-ec7a-438b-a290-0cfb563a5170-0": {
          "localContext": {
            "domains": {
              "": ""
            },
            "categories": {
              "": ""
            },
            "rangeExpressions": {
              "": {}
            },
            "namedEntities": {},
            "lang": "ja_JP",
            "properNouns": {},
            "knowledgeFeatureReferences": []
          },
          "predicateArgumentStructure": {
            "caseGroupType": 0,
            "surface": "案ずるより",
            "casePhrase": "",
            "casePhraseId": "",
            "modalityType": "-",
            "dependType": "D",
            "surfaceYomi": "あんずるより",
            "normalizedNameYomi": "あんずる",
            "parentId": 1,
            "isDenialWord": false,
            "normalizedName": "案ずる",
            "currentId": 0,
            "morphemes": [
              "動詞,*,ザ変動詞,基本形",
              "助詞,接続助詞,*,*"
            ],
            "isMainSection": false,
            "nodeType": 1,
            "parallelType": "-",
            "isConditionalConnection": false,
            "caseType": "連用"
          },
          "propositionId": "612bf3d6-bdb5-47b9-a3a6-185015c8c414",
          "nodeId": "4a2994a1-ec7a-438b-a290-0cfb563a5170-0",
          "sentenceId": "4a2994a1-ec7a-438b-a290-0cfb563a5170"
        }
      },
      "edgeList": [
        {
          "hasInclusion": false,
          "dependType": "D",
          "parallelType": "-",
          "destinationId": "4a2994a1-ec7a-438b-a290-0cfb563a5170-2",
          "sourceId": "4a2994a1-ec7a-438b-a290-0cfb563a5170-1",
          "logicType": "-",
          "caseStr": "連用"
        },
        {
          "hasInclusion": false,
          "dependType": "D",
          "parallelType": "-",
          "destinationId": "4a2994a1-ec7a-438b-a290-0cfb563a5170-1",
          "sourceId": "4a2994a1-ec7a-438b-a290-0cfb563a5170-0",
          "logicType": "-",
          "caseStr": "連用"
        }
      ],
      "knowledgeBaseSemiGlobalNode": {
        "propositionId": "612bf3d6-bdb5-47b9-a3a6-185015c8c414",
        "localContextForFeature": {
          "lang": "ja_JP",
          "knowledgeFeatureReferences": []
        },
        "sentenceType": 1,
        "documentId": "4a2994a1-ec7a-438b-a290-0cfb563a5170",
        "sentenceId": "4a2994a1-ec7a-438b-a290-0cfb563a5170",
        "sentence": "案ずるより産むが易し。"
      },
      "deductionResult": {
        "deductionPhaseType": 1,
        "havePremiseInGivenProposition": false,
        "coveredPropositionEdges": [],
        "status": false,
        "evidenceKnowledgeList": [],
        "authenticityType": 2
      }
    }
  ],
  "deductionConfiguration": {
    "actionModeType": 2,
    "llmModel": "",
    "llmModelHyperParameters": {},
    "maxTargetKnowledgeCount": 10
  }
}' http://localhost:9101/execute
```

## For details on Input Json 
see below.
* ref. https://github.com/toposoid/toposoid-deduction-admin-web?tab=readme-ov-file#json-details

# Note
* This microservice uses 9101 as the default port.
* If you want to run in a remote environment or a virtual environment, change PRIVATE_IP_ADDRESS in docker-compose.yml according to your environment.

## License
This program is offered under a commercial and under the AGPL license.
For commercial licensing, contact us at https://toposoid.com/contact.  For AGPL licensing, see below.

AGPL licensing:
This program is free software: you can redistribute it and/or modify
it under the terms of the GNU Affero General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU Affero General Public License for more details.

You should have received a copy of the GNU Affero General Public License
along with this program.  If not, see <https://www.gnu.org/licenses/>.


## Author
* Makoto Kubodera([Linked Ideal LLC.](https://linked-ideal.com/))

Thank you!
