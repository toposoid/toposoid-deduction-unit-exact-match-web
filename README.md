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

## Memory requirements For Standalone
* at least 3.21G of HDD(Total required Docker Image size)


## Setup For Standalone
```bssh
docker-compose up
```
The first startup takes a long time until docker pull finishes.
## Usage
```bash
# Please refer to the following for information on registering data to try searching.
# ref. https://github.com/toposoid/toposoid-knowledge-register-web
#for example
curl -X POST -H "Content-Type: application/json" -d '{
    "premiseList": [],
    "premiseLogicRelation": [],
    "claimList": [
        {
            "sentence": "案ずるより産むが易し。",
            "lang": "ja_JP",
            "extentInfoJson": "{}",
            "isNegativeSentence": false,
            "knowledgeForImages":[]
        }
    ],
    "claimLogicRelation": [
    ]
}
' http://localhost:9002/regist


# Deduction
curl -X POST -H "Content-Type: application/json" -d '{
    "analyzedSentenceObjects": [
        {
            "nodeMap": {
                "c1bec44a-ec99-436a-9354-22231d43a435-2": {
                    "nodeId": "c1bec44a-ec99-436a-9354-22231d43a435-2",
                    "propositionId": "58704341-49d8-43eb-ba4f-fa85e6b999e3",
                    "sentenceId": "c1bec44a-ec99-436a-9354-22231d43a435",
                    "predicateArgumentStructure": {
                        "currentId": 2,
                        "parentId": -1,
                        "isMainSection": true,
                        "surface": "易し。",
                        "normalizedName": "易い",
                        "dependType": "D",
                        "caseType": "文末",
                        "isDenialWord": false,
                        "isConditionalConnection": false,
                        "normalizedNameYomi": "やすい?易しい",
                        "surfaceYomi": "やすし。",
                        "modalityType": "-",
                        "parallelType": "-",
                        "nodeType": 1,
                        "morphemes": [
                            "形容詞,*,イ形容詞アウオ段,文語基本形",
                            "特殊,句点,*,*"
                        ]
                    },
                    "localContext": {
                        "lang": "ja_JP",
                        "namedEntity": "",
                        "rangeExpressions": {
                            "": {}
                        },
                        "categories": {
                            "": ""
                        },
                        "domains": {
                            "": ""
                        },
                        "knowledgeFeatureReferences": []
                    }
                },
                "c1bec44a-ec99-436a-9354-22231d43a435-1": {
                    "nodeId": "c1bec44a-ec99-436a-9354-22231d43a435-1",
                    "propositionId": "58704341-49d8-43eb-ba4f-fa85e6b999e3",
                    "sentenceId": "c1bec44a-ec99-436a-9354-22231d43a435",
                    "predicateArgumentStructure": {
                        "currentId": 1,
                        "parentId": 2,
                        "isMainSection": false,
                        "surface": "産むが",
                        "normalizedName": "産む",
                        "dependType": "D",
                        "caseType": "連用",
                        "isDenialWord": false,
                        "isConditionalConnection": false,
                        "normalizedNameYomi": "うむ",
                        "surfaceYomi": "うむが",
                        "modalityType": "-",
                        "parallelType": "-",
                        "nodeType": 1,
                        "morphemes": [
                            "動詞,*,子音動詞マ行,基本形",
                            "助詞,接続助詞,*,*"
                        ]
                    },
                    "localContext": {
                        "lang": "ja_JP",
                        "namedEntity": "",
                        "rangeExpressions": {
                            "": {}
                        },
                        "categories": {
                            "": ""
                        },
                        "domains": {
                            "産む": "家庭・暮らし"
                        },
                        "knowledgeFeatureReferences": []
                    }
                },
                "c1bec44a-ec99-436a-9354-22231d43a435-0": {
                    "nodeId": "c1bec44a-ec99-436a-9354-22231d43a435-0",
                    "propositionId": "58704341-49d8-43eb-ba4f-fa85e6b999e3",
                    "sentenceId": "c1bec44a-ec99-436a-9354-22231d43a435",
                    "predicateArgumentStructure": {
                        "currentId": 0,
                        "parentId": 1,
                        "isMainSection": false,
                        "surface": "案ずるより",
                        "normalizedName": "案ずる",
                        "dependType": "D",
                        "caseType": "連用",
                        "isDenialWord": false,
                        "isConditionalConnection": false,
                        "normalizedNameYomi": "あんずる",
                        "surfaceYomi": "あんずるより",
                        "modalityType": "-",
                        "parallelType": "-",
                        "nodeType": 1,
                        "morphemes": [
                            "動詞,*,ザ変動詞,基本形",
                            "助詞,接続助詞,*,*"
                        ]
                    },
                    "localContext": {
                        "lang": "ja_JP",
                        "namedEntity": "",
                        "rangeExpressions": {
                            "": {}
                        },
                        "categories": {
                            "": ""
                        },
                        "domains": {
                            "": ""
                        },
                        "knowledgeFeatureReferences": []
                    }
                }
            },
            "edgeList": [
                {
                    "sourceId": "c1bec44a-ec99-436a-9354-22231d43a435-1",
                    "destinationId": "c1bec44a-ec99-436a-9354-22231d43a435-2",
                    "caseStr": "連用",
                    "dependType": "D",
                    "parallelType": "-",
                    "hasInclusion": false,
                    "logicType": "-"
                },
                {
                    "sourceId": "c1bec44a-ec99-436a-9354-22231d43a435-0",
                    "destinationId": "c1bec44a-ec99-436a-9354-22231d43a435-1",
                    "caseStr": "連用",
                    "dependType": "D",
                    "parallelType": "-",
                    "hasInclusion": false,
                    "logicType": "-"
                }
            ],
            "knowledgeBaseSemiGlobalNode": {
                "nodeId": "c1bec44a-ec99-436a-9354-22231d43a435",
                "propositionId": "58704341-49d8-43eb-ba4f-fa85e6b999e3",
                "sentenceId": "c1bec44a-ec99-436a-9354-22231d43a435",
                "sentence": "案ずるより産むが易し。",
                "sentenceType": 1,
                "localContextForFeature": {
                    "lang": "ja_JP",
                    "knowledgeFeatureReferences": []
                }
            },
            "deductionResult": {
                "status": false,
                "coveredPropositionResults": [],
                "havePremiseInGivenProposition": false
            }
        }
    ]
}' http://localhost:9101/execute
```

## For details on Input Json 
see below.
* ref. https://github.com/toposoid/toposoid-deduction-admin-web?tab=readme-ov-file#json-details

# Note
* This microservice uses 9101 as the default port.
* If you want to run in a remote environment or a virtual environment, change PRIVATE_IP_ADDRESS in docker-compose.yml according to your environment.

## License
toposoid/toposoid-deduction-unit-exact-match-web is Open Source software released under the [Apache 2.0 license](https://www.apache.org/licenses/LICENSE-2.0.html).

## Author
* Makoto Kubodera([Linked Ideal LLC.](https://linked-ideal.com/))

Thank you!
