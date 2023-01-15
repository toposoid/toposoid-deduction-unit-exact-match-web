# toposoid-deduction-unit-exact-match-web
This is a WEB API that works as a microservice within the toposoid project.
Toposoid is a knowledge base construction platform.(see [Toposoid　Root Project](https://github.com/toposoid/toposoid.git))
This microservice provides the ability to determine if the text you enter matches the knowledge graph exactly. 

[![Unit Test And Build Image Action](https://github.com/toposoid/toposoid-deduction-unit-exact-match-web/actions/workflows/action.yml/badge.svg?branch=main)](https://github.com/toposoid/toposoid-deduction-unit-exact-match-web/actions/workflows/action.yml)

<img width="1038" src="https://user-images.githubusercontent.com/82787843/212530242-bd6cdb20-d78b-488b-b697-a013b93c85d2.png">

## Requirements
* Docker version 20.10.x, or late
* docker-compose version 1.22.x

## Memory requirements
* Required: at least 8GB of RAM (The maximum heap memory size of the JVM is set to 6G (Application: 4G, Neo4J: 2G))
* Required: 30G or higher　of HDD


## Setup
```bssh
docker-compose up -d
```
It takes more than 20 minutes to pull the Docker image for the first time.
## Usage
```bash
curl -X POST -H "Content-Type: application/json" -d '{
    "analyzedSentenceObjects": [
        {
            "nodeMap": {
                "d1635630-dcaa-4996-afd4-765fb136b406-2": {
                    "nodeId": "d1635630-dcaa-4996-afd4-765fb136b406-2",
                    "propositionId": "b5aed0cb-ab43-4acd-8695-bda2bb65993b",
                    "currentId": 2,
                    "parentId": -1,
                    "isMainSection": true,
                    "surface": "易し。",
                    "normalizedName": "易い",
                    "dependType": "D",
                    "caseType": "文末",
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
                    "isDenialWord": false,
                    "isConditionalConnection": false,
                    "normalizedNameYomi": "やすい?易しい",
                    "surfaceYomi": "やすし。",
                    "modalityType": "-",
                    "logicType": "-",
                    "nodeType": 1,
                    "lang": "ja_JP",
                    "extentText": "{}"
                },
                "d1635630-dcaa-4996-afd4-765fb136b406-1": {
                    "nodeId": "d1635630-dcaa-4996-afd4-765fb136b406-1",
                    "propositionId": "b5aed0cb-ab43-4acd-8695-bda2bb65993b",
                    "currentId": 1,
                    "parentId": 2,
                    "isMainSection": false,
                    "surface": "産むが",
                    "normalizedName": "産む",
                    "dependType": "D",
                    "caseType": "連用",
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
                    "isDenialWord": false,
                    "isConditionalConnection": false,
                    "normalizedNameYomi": "うむ",
                    "surfaceYomi": "うむが",
                    "modalityType": "-",
                    "logicType": "-",
                    "nodeType": 1,
                    "lang": "ja_JP",
                    "extentText": "{}"
                },
                "d1635630-dcaa-4996-afd4-765fb136b406-0": {
                    "nodeId": "d1635630-dcaa-4996-afd4-765fb136b406-0",
                    "propositionId": "b5aed0cb-ab43-4acd-8695-bda2bb65993b",
                    "currentId": 0,
                    "parentId": 1,
                    "isMainSection": false,
                    "surface": "案ずるより",
                    "normalizedName": "案ずる",
                    "dependType": "D",
                    "caseType": "連用",
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
                    "isDenialWord": false,
                    "isConditionalConnection": false,
                    "normalizedNameYomi": "あんずる",
                    "surfaceYomi": "あんずるより",
                    "modalityType": "-",
                    "logicType": "-",
                    "nodeType": 1,
                    "lang": "ja_JP",
                    "extentText": "{}"
                }
            },
            "edgeList": [
                {
                    "sourceId": "d1635630-dcaa-4996-afd4-765fb136b406-1",
                    "destinationId": "d1635630-dcaa-4996-afd4-765fb136b406-2",
                    "caseStr": "連用",
                    "dependType": "D",
                    "logicType": "-",
                    "lang": "ja_JP"
                },
                {
                    "sourceId": "d1635630-dcaa-4996-afd4-765fb136b406-0",
                    "destinationId": "d1635630-dcaa-4996-afd4-765fb136b406-1",
                    "caseStr": "連用",
                    "dependType": "D",
                    "logicType": "-",
                    "lang": "ja_JP"
                }
            ],
            "sentenceType": 1,
            "sentenceId": "d1635630-dcaa-4996-afd4-765fb136b406",
            "lang": "ja_JP",
            "deductionResultMap": {
                "0": {
                    "status": false,
                    "matchedPropositionIds": [],
                    "deductionUnit": ""
                },
                "1": {
                    "status": false,
                    "matchedPropositionIds": [],
                    "deductionUnit": ""
                }
            }
        }
    ]
}' http://localhost:9101/execute
```

# Note
* This microservice uses 9101 as the default port.
* If you want to run in a remote environment or a virtual environment, change PRIVATE_IP_ADDRESS in docker-compose.yml according to your environment.

## License
toposoid/toposoid-deduction-unit-exact-match-web is Open Source software released under the [Apache 2.0 license](https://www.apache.org/licenses/LICENSE-2.0.html).

## Author
* Makoto Kubodera([Linked Ideal LLC.](https://linked-ideal.com/))

Thank you!
