# toposoid-deduction-unit-exact-match-web
This is a WEB API that works as a microservice within the toposoid project.
Toposoid is a knowledge base construction platform.(see [Toposoid　Root Project](https://github.com/toposoid/toposoid.git))
This microservice provides the ability to determine if the text you enter matches the knowledge graph exactly. 

<img width="1094" alt="2021-09-27 18 34 51" src="https://user-images.githubusercontent.com/82787843/134884028-7e9ef59c-033a-44ce-92ab-cba45a7baea7.png">

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
                "75f3c079-848e-4f88-8155-4f198b2b68e2-2": {
                    "nodeId": "75f3c079-848e-4f88-8155-4f198b2b68e2-2",
                    "propositionId": "75f3c079-848e-4f88-8155-4f198b2b68e2",
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
                    "isDenial": false,
                    "isConditionalConnection": false,
                    "normalizedNameYomi": "やすい?易しい",
                    "surfaceYomi": "やすし。",
                    "modalityType": "-",
                    "logicType": "-",
                    "nodeType": 1,
                    "extentText": "{}"
                },
                "75f3c079-848e-4f88-8155-4f198b2b68e2-1": {
                    "nodeId": "75f3c079-848e-4f88-8155-4f198b2b68e2-1",
                    "propositionId": "75f3c079-848e-4f88-8155-4f198b2b68e2",
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
                    "isDenial": false,
                    "isConditionalConnection": false,
                    "normalizedNameYomi": "うむ",
                    "surfaceYomi": "うむが",
                    "modalityType": "-",
                    "logicType": "-",
                    "nodeType": 1,
                    "extentText": "{}"
                },
                "75f3c079-848e-4f88-8155-4f198b2b68e2-0": {
                    "nodeId": "75f3c079-848e-4f88-8155-4f198b2b68e2-0",
                    "propositionId": "75f3c079-848e-4f88-8155-4f198b2b68e2",
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
                    "isDenial": false,
                    "isConditionalConnection": false,
                    "normalizedNameYomi": "あんずる",
                    "surfaceYomi": "あんずるより",
                    "modalityType": "-",
                    "logicType": "-",
                    "nodeType": 1,
                    "extentText": "{}"
                }
            },
            "edgeList": [
                {
                    "sourceId": "75f3c079-848e-4f88-8155-4f198b2b68e2-1",
                    "destinationId": "75f3c079-848e-4f88-8155-4f198b2b68e2-2",
                    "caseStr": "連用",
                    "dependType": "D",
                    "logicType": "-"
                },
                {
                    "sourceId": "75f3c079-848e-4f88-8155-4f198b2b68e2-0",
                    "destinationId": "75f3c079-848e-4f88-8155-4f198b2b68e2-1",
                    "caseStr": "連用",
                    "dependType": "D",
                    "logicType": "-"
                }
            ],
            "sentenceType": 1,
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
